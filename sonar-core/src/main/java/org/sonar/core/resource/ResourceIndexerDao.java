/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.resource;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.persistence.MyBatis;

public class ResourceIndexerDao {

  public static final int MINIMUM_KEY_SIZE = 3;

  // The scopes and qualifiers that are not in the following constants are not indexed at all.
  // Directories and packages are explicitly excluded.
  private static final String[] RENAMABLE_QUALIFIERS = {Qualifiers.PROJECT, Qualifiers.MODULE, Qualifiers.VIEW, Qualifiers.SUBVIEW};
  private static final String[] RENAMABLE_SCOPES = {Scopes.PROJECT};
  private static final String[] NOT_RENAMABLE_QUALIFIERS = {Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE, Qualifiers.CLASS};
  private static final String[] NOT_RENAMABLE_SCOPES = {Scopes.FILE};

  private final MyBatis mybatis;

  public ResourceIndexerDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  /**
   * This method is reentrant. It can be executed even if the project is already indexed.
   */
  public ResourceIndexerDao indexProject(final int rootProjectId) {
    SqlSession session = mybatis.openBatchSession();
    try {
      ResourceIndexerMapper mapper = session.getMapper(ResourceIndexerMapper.class);
      doIndexProject(rootProjectId, session, mapper);
      session.commit();
      return this;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * This method is reentrant. It can be executed even if some projects are already indexed.
   */
  public ResourceIndexerDao indexProjects() {
    final SqlSession session = mybatis.openBatchSession();
    try {
      final ResourceIndexerMapper mapper = session.getMapper(ResourceIndexerMapper.class);
      session.select("org.sonar.core.resource.ResourceIndexerMapper.selectRootProjectIds", /* workaround to get booleans */ResourceIndexerQuery.create(), new ResultHandler() {
        public void handleResult(ResultContext context) {
          Integer rootProjectId = (Integer) context.getResultObject();
          doIndexProject(rootProjectId, session, mapper);
          session.commit();
        }
      });
      return this;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void doIndexProject(int rootProjectId, SqlSession session, final ResourceIndexerMapper mapper) {
    // non indexed resources
    ResourceIndexerQuery query = ResourceIndexerQuery.create()
        .setNonIndexedOnly(true)
        .setQualifiers(NOT_RENAMABLE_QUALIFIERS)
        .setScopes(NOT_RENAMABLE_SCOPES)
        .setRootProjectId(rootProjectId);

    session.select("org.sonar.core.resource.ResourceIndexerMapper.selectResources", query, new ResultHandler() {
      public void handleResult(ResultContext context) {
        ResourceDto resource = (ResourceDto) context.getResultObject();
        doIndex(resource, mapper);
      }
    });

    // some resources can be renamed, so index must be regenerated
    // -> delete existing rows and create them again
    query = ResourceIndexerQuery.create()
        .setNonIndexedOnly(false)
        .setQualifiers(RENAMABLE_QUALIFIERS)
        .setScopes(RENAMABLE_SCOPES)
        .setRootProjectId(rootProjectId);

    session.select("org.sonar.core.resource.ResourceIndexerMapper.selectResources", query, new ResultHandler() {
      public void handleResult(ResultContext context) {
        ResourceDto resource = (ResourceDto) context.getResultObject();

        mapper.deleteByResourceId(resource.getId());
        doIndex(resource, mapper);
      }
    });
  }


  void doIndex(ResourceDto resource, ResourceIndexerMapper mapper) {
    String key = nameToKey(resource.getName());
    if (key.length() >= MINIMUM_KEY_SIZE) {
      ResourceIndexDto dto = new ResourceIndexDto()
          .setResourceId(resource.getId())
          .setQualifier(resource.getQualifier())
          .setRootProjectId(resource.getRootId())
          .setNameSize(resource.getName().length());

      for (int position = 0; position <= key.length() - MINIMUM_KEY_SIZE; position++) {
        dto.setPosition(position);
        dto.setKey(StringUtils.substring(key, position));
        mapper.insert(dto);
      }
    }
  }

  public boolean indexResource(long id) {
    boolean indexed = false;
    SqlSession session = mybatis.openSession();
    try {
      ResourceIndexerMapper mapper = session.getMapper(ResourceIndexerMapper.class);
      ResourceDto resource = mapper.selectResourceToIndex(id);
      if (resource != null) {
        Integer rootId = resource.getRootId();
        if (rootId == null) {
          rootId = resource.getId().intValue();
        }
        indexed = indexResource(resource.getId().intValue(), resource.getName(), resource.getQualifier(), rootId, session, mapper);
      }
      return indexed;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public boolean indexResource(int id, String name, String qualifier, int rootId) {
    boolean indexed = false;
    if (isIndexableQualifier(qualifier)) {
      SqlSession session = mybatis.openSession();
      ResourceIndexerMapper mapper = session.getMapper(ResourceIndexerMapper.class);
      try {
        indexed = indexResource(id, name, qualifier, rootId, session, mapper);
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
    return indexed;
  }

  private boolean indexResource(int id, String name, String qualifier, int rootId, SqlSession session, ResourceIndexerMapper mapper) {
    boolean indexed = false;
    String key = nameToKey(name);
    if (key.length() >= MINIMUM_KEY_SIZE) {
      indexed = true;
      boolean toBeIndexed = sanitizeIndex(id, key, mapper);
      if (toBeIndexed) {
        ResourceIndexDto dto = new ResourceIndexDto()
            .setResourceId(id)
            .setQualifier(qualifier)
            .setRootProjectId(rootId)
            .setNameSize(name.length());

        for (int position = 0; position <= key.length() - MINIMUM_KEY_SIZE; position++) {
          dto.setPosition(position);
          dto.setKey(StringUtils.substring(key, position));
          mapper.insert(dto);
        }
        session.commit();
      }
    }
    return indexed;
  }


  /**
   * Return true if the resource must be indexed, false if the resource is already indexed.
   * If the resource is indexed with a different key, then this index is dropped and the
   * resource must be indexed again.
   */
  private boolean sanitizeIndex(int resourceId, String key, ResourceIndexerMapper mapper) {
    ResourceIndexDto masterIndex = mapper.selectMasterIndexByResourceId(resourceId);
    if (masterIndex != null && !StringUtils.equals(key, masterIndex.getKey())) {
      // resource has been renamed -> drop existing indexes
      mapper.deleteByResourceId(resourceId);
      masterIndex = null;
    }
    return masterIndex == null;
  }

  static String nameToKey(String input) {
    return StringUtils.lowerCase(StringUtils.trimToEmpty(input));
  }

  static boolean isIndexableQualifier(String qualifier) {
    return ArrayUtils.contains(RENAMABLE_QUALIFIERS, qualifier) || ArrayUtils.contains(NOT_RENAMABLE_QUALIFIERS, qualifier);
  }
}
