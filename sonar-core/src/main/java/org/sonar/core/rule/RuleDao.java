/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.rule;

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class RuleDao implements BatchComponent, ServerComponent {

  private MyBatis mybatis;

  public RuleDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<RuleDto> selectAll() {
    SqlSession session = mybatis.openSession();
    try {
      return selectAll(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectAll(SqlSession session) {
    return getMapper(session).selectAll();
  }

  public List<RuleDto> selectEnablesAndNonManual() {
    SqlSession session = mybatis.openSession();
    try {
      return selectEnablesAndNonManual(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectEnablesAndNonManual(SqlSession session) {
    return getMapper(session).selectEnablesAndNonManual();
  }

  public List<RuleDto> selectNonManual(SqlSession session) {
    return getMapper(session).selectNonManual();
  }

  public List<RuleDto> selectBySubCharacteristicId(Integer characteristicOrSubCharacteristicId) {
    SqlSession session = mybatis.openSession();
    try {
      return selectBySubCharacteristicId(characteristicOrSubCharacteristicId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Return all rules (even the REMOVED ones) linked on to a sub characteristic
   */
  public List<RuleDto> selectBySubCharacteristicId(Integer subCharacteristicId, SqlSession session) {
    return getMapper(session).selectBySubCharacteristicId(subCharacteristicId);
  }

  @CheckForNull
  public RuleDto selectById(Integer id, SqlSession session) {
    return getMapper(session).selectById(id);
  }

  @CheckForNull
  public RuleDto selectById(Integer id) {
    SqlSession session = mybatis.openSession();
    try {
      return selectById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleDto selectByKey(RuleKey ruleKey, SqlSession session) {
    return getMapper(session).selectByKey(ruleKey);
  }

  @CheckForNull
  public RuleDto selectByKey(RuleKey ruleKey) {
    SqlSession session = mybatis.openSession();
    try {
      return selectByKey(ruleKey, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleDto selectByName(String name) {
    SqlSession session = mybatis.openSession();
    try {
      return getMapper(session).selectByName(name);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(RuleDto rule, SqlSession session) {
    getMapper(session).update(rule);
  }

  public void update(RuleDto rule) {
    SqlSession session = mybatis.openSession();
    try {
      update(rule, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(RuleDto ruleToInsert, SqlSession session) {
    getMapper(session).insert(ruleToInsert);
  }

  public void insert(RuleDto ruleToInsert) {
    SqlSession session = mybatis.openSession();
    try {
      insert(ruleToInsert, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(Collection<RuleDto> rules) {
    SqlSession session = mybatis.openBatchSession();
    try {
      for (RuleDto rule : rules) {
        getMapper(session).batchInsert(rule);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  //******************************
  // Methods for Rule Parameters
  //******************************

  public List<RuleParamDto> selectParameters() {
    SqlSession session = mybatis.openSession();
    try {
      return selectParameters(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParameters(SqlSession session) {
    return getMapper(session).selectAllParams();
  }

  public List<RuleParamDto> selectParametersByRuleId(Integer ruleId) {
    SqlSession session = mybatis.openSession();
    try {
      return selectParametersByRuleId(ruleId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParametersByRuleId(Integer ruleId, SqlSession session) {
    return selectParametersByRuleIds(newArrayList(ruleId));
  }

  public List<RuleParamDto> selectParametersByRuleIds(List<Integer> ruleIds) {
    SqlSession session = mybatis.openSession();
    try {
      return selectParametersByRuleIds(ruleIds, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParametersByRuleIds(List<Integer> ruleIds, SqlSession session) {
    List<RuleParamDto> dtos = newArrayList();
    List<List<Integer>> partitionList = Lists.partition(newArrayList(ruleIds), 1000);
    for (List<Integer> partition : partitionList) {
      dtos.addAll(getMapper(session).selectParamsByRuleIds(partition));
    }
    return dtos;
  }

  public void insert(RuleParamDto param, SqlSession session) {
    getMapper(session).insertParameter(param);
  }

  public void insert(RuleParamDto param) {
    SqlSession session = mybatis.openSession();
    try {
      insert(param, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(RuleParamDto param, SqlSession session) {
    getMapper(session).updateParameter(param);
  }

  public void update(RuleParamDto param) {
    SqlSession session = mybatis.openSession();
    try {
      update(param, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleParamDto selectParamByRuleAndKey(Integer ruleId, String key, SqlSession session) {
    return getMapper(session).selectParamByRuleAndKey(ruleId, key);
  }

  private RuleMapper getMapper(SqlSession session) {
    return session.getMapper(RuleMapper.class);
  }
}
