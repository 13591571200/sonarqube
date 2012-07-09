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

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

public class ResourceDao {
  private MyBatis mybatis;

  public ResourceDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<ResourceDto> getResources(ResourceQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(ResourceMapper.class).selectResources(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<Long> getResourceIds(ResourceQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(ResourceMapper.class).selectResourceIds(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ResourceDto getResource(long projectId) {
    SqlSession session = mybatis.openSession();
    try {
      return getResource(projectId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ResourceDto getResource(long projectId, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectResource(projectId);
  }

  public List<ResourceDto> getDescendantProjects(long projectId) {
    SqlSession session = mybatis.openSession();
    try {
      return getDescendantProjects(projectId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ResourceDto> getDescendantProjects(long projectId, SqlSession session) {
    ResourceMapper mapper = session.getMapper(ResourceMapper.class);
    List<ResourceDto> resources = Lists.newArrayList();
    appendChildProjects(projectId, mapper, resources);
    return resources;
  }

  private void appendChildProjects(long projectId, ResourceMapper mapper, List<ResourceDto> resources) {
    List<ResourceDto> subProjects = mapper.selectDescendantProjects(projectId);
    for (ResourceDto subProject : subProjects) {
      resources.add(subProject);
      appendChildProjects(subProject.getId(), mapper, resources);
    }
  }

  public ResourceDao insertOrUpdate(ResourceDto... resources) {
    SqlSession session = mybatis.openSession();
    ResourceMapper mapper = session.getMapper(ResourceMapper.class);
    try {
      for (ResourceDto resource : resources) {
        if (resource.getId()==null) {
          mapper.insert(resource);
        } else {
          mapper.update(resource);
        }
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return this;
  }
}
