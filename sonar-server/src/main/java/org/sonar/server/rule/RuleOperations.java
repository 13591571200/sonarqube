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

package org.sonar.server.rule;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.check.Cardinality;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.rule.RuleRuleTagDto;
import org.sonar.core.rule.RuleTagDao;
import org.sonar.core.rule.RuleTagType;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.ESActiveRule;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

public class RuleOperations implements ServerComponent {

  private final MyBatis myBatis;
  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final RuleTagDao ruleTagDao;
  private final CharacteristicDao characteristicDao;
  private final RuleTagOperations ruleTagOperations;
  private final ESActiveRule esActiveRule;
  private final RuleRegistry ruleRegistry;

  private final System2 system;

  public RuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, RuleTagDao ruleTagDao, CharacteristicDao characteristicDao,
                        RuleTagOperations ruleTagOperations, ESActiveRule esActiveRule, RuleRegistry ruleRegistry) {
    this(myBatis, activeRuleDao, ruleDao, ruleTagDao, characteristicDao, ruleTagOperations, esActiveRule, ruleRegistry, System2.INSTANCE);
  }

  @VisibleForTesting
  RuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, RuleTagDao ruleTagDao, CharacteristicDao characteristicDao, RuleTagOperations ruleTagOperations,
                 ESActiveRule esActiveRule, RuleRegistry ruleRegistry, System2 system) {
    this.myBatis = myBatis;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.ruleTagDao = ruleTagDao;
    this.characteristicDao = characteristicDao;
    this.ruleTagOperations = ruleTagOperations;
    this.esActiveRule = esActiveRule;
    this.ruleRegistry = ruleRegistry;
    this.system = system;
  }

  public void updateRuleNote(RuleDto rule, String note, UserSession userSession) {
    checkPermission(userSession);
    Date now = new Date(system.now());

    SqlSession session = myBatis.openSession(false);
    try {
      if (rule.getNoteData() == null) {
        rule.setNoteCreatedAt(now);
        rule.setNoteUserLogin(getLoggedLogin(userSession));
      }
      rule.setNoteUpdatedAt(now);
      rule.setNoteData(note);
      ruleDao.update(rule);
      session.commit();

      reindexRule(rule, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteRuleNote(RuleDto rule, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession(false);
    try {
      rule.setNoteData(null);
      rule.setNoteUserLogin(null);
      rule.setNoteCreatedAt(null);
      rule.setNoteUpdatedAt(null);
      ruleDao.update(rule);
      session.commit();

      reindexRule(rule, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public RuleDto createCustomRule(RuleDto templateRule, String name, String severity, String description, Map<String, String> paramsByKey,
                                  UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      RuleDto rule = new RuleDto()
        .setParentId(templateRule.getId())
        .setName(name)
        .setDescription(description)
        .setSeverity(severity)
        .setRepositoryKey(templateRule.getRepositoryKey())
        .setConfigKey(templateRule.getConfigKey())
        .setRuleKey(templateRule.getRuleKey() + "_" + system.now())
        .setCardinality(Cardinality.SINGLE)
        .setStatus(Rule.STATUS_READY)
        .setLanguage(templateRule.getLanguage())
        .setDefaultSubCharacteristicId(templateRule.getDefaultSubCharacteristicId())
        .setDefaultRemediationFunction(templateRule.getDefaultRemediationFunction())
        .setDefaultRemediationCoefficient(templateRule.getDefaultRemediationCoefficient())
        .setDefaultRemediationOffset(templateRule.getDefaultRemediationOffset())
        .setCreatedAt(new Date(system.now()))
        .setUpdatedAt(new Date(system.now()));
      ruleDao.insert(rule, session);

      List<RuleParamDto> templateRuleParams = ruleDao.selectParametersByRuleId(templateRule.getId(), session);
      for (RuleParamDto templateRuleParam : templateRuleParams) {
        String key = templateRuleParam.getName();
        String value = paramsByKey.get(key);

        RuleParamDto param = new RuleParamDto()
          .setRuleId(rule.getId())
          .setName(key)
          .setDescription(templateRuleParam.getDescription())
          .setType(templateRuleParam.getType())
          .setDefaultValue(Strings.emptyToNull(value));
        ruleDao.insert(param, session);
      }

      List<RuleRuleTagDto> templateRuleTags = ruleDao.selectTagsByRuleIds(templateRule.getId(), session);
      for (RuleRuleTagDto tag : templateRuleTags) {
        RuleRuleTagDto newTag = new RuleRuleTagDto()
          .setRuleId(rule.getId())
          .setTagId(tag.getTagId())
          .setTag(tag.getTag())
          .setType(tag.getType());
        ruleDao.insert(newTag, session);
      }

      session.commit();
      reindexRule(rule, session);
      return rule;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateCustomRule(RuleDto rule, String name, String severity, String description, Map<String, String> paramsByKey,
                               UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      rule.setName(name)
        .setDescription(description)
        .setSeverity(severity)
        .setUpdatedAt(new Date(system.now()));
      ruleDao.update(rule, session);

      List<RuleParamDto> ruleParams = ruleDao.selectParametersByRuleId(rule.getId(), session);
      for (RuleParamDto ruleParam : ruleParams) {
        String value = paramsByKey.get(ruleParam.getName());
        ruleParam.setDefaultValue(Strings.emptyToNull(value));
        ruleDao.update(ruleParam, session);
      }
      session.commit();
      reindexRule(rule, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteCustomRule(RuleDto rule, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      // Set status REMOVED on rule
      rule.setStatus(Rule.STATUS_REMOVED)
        .setUpdatedAt(new Date(system.now()));
      ruleDao.update(rule, session);
      session.commit();
      reindexRule(rule, session);

      // Delete all active rules and active rule params linked to the rule
      List<ActiveRuleDto> activeRules = activeRuleDao.selectByRuleId(rule.getId());
      for (ActiveRuleDto activeRule : activeRules) {
        activeRuleDao.deleteParameters(activeRule.getId(), session);
      }
      activeRuleDao.deleteFromRule(rule.getId(), session);
      session.commit();
      esActiveRule.deleteActiveRules(newArrayList(Iterables.transform(activeRules, new Function<ActiveRuleDto, Integer>() {
        @Override
        public Integer apply(ActiveRuleDto input) {
          return input.getId();
        }
      })));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateRuleTags(RuleDto rule, List<String> newTags, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      Map<String, Long> neededTagIds = validateAndGetTagIds(newTags, session);

      final Integer ruleId = rule.getId();

      boolean ruleChanged = synchronizeTags(ruleId, newTags, neededTagIds, session);
      if (ruleChanged) {
        rule.setUpdatedAt(new Date(system.now()));
        ruleDao.update(rule, session);
        session.commit();
        reindexRule(rule, session);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateRule(RuleChange ruleChange, UserSession userSession) {
    checkPermission(userSession);
    DbSession session = myBatis.openSession(false);
    try {
      RuleDto ruleDto = ruleDao.selectByKey(ruleChange.ruleKey(), session);
      if (ruleDto == null) {
        throw new NotFoundException(String.format("Unknown rule '%s'", ruleChange.ruleKey()));
      }
      String subCharacteristicKey = ruleChange.debtCharacteristicKey();
      CharacteristicDto subCharacteristic = null;

      // A sub-characteristic is given -> update rule debt if given values are different from overridden ones and from default ones
      if (!Strings.isNullOrEmpty(subCharacteristicKey)) {
        subCharacteristic = characteristicDao.selectByKey(subCharacteristicKey, session);
        if (subCharacteristic == null) {
          throw new NotFoundException(String.format("Unknown sub characteristic '%s'", ruleChange.debtCharacteristicKey()));
        }
      }

      boolean needUpdate = updateRule(ruleDto, subCharacteristic, ruleChange.debtRemediationFunction(), ruleChange.debtRemediationCoefficient(), ruleChange.debtRemediationOffset(),
        new Date(system.now()), session);
      if (needUpdate) {
        session.commit();
        reindexRule(ruleDto, session);
      }
    } catch (IllegalArgumentException e) {
      throw BadRequestException.of(e.getMessage());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public boolean updateRule(RuleDto ruleDto, @Nullable CharacteristicDto newSubCharacteristic, @Nullable String newFunction,
                            @Nullable String newCoefficient, @Nullable String newOffset, Date updateDate, DbSession session) {
    boolean needUpdate = false;

    // A sub-characteristic and a remediation function is given -> update rule debt
    if (newSubCharacteristic != null && newFunction != null) {
      // New values are the same as the default values -> set overridden values to null
      if (isRuleDebtSameAsDefaultValues(ruleDto, newSubCharacteristic, newFunction, newCoefficient, newOffset)) {
        ruleDto.setSubCharacteristicId(null);
        ruleDto.setRemediationFunction(null);
        ruleDto.setRemediationCoefficient(null);
        ruleDto.setRemediationOffset(null);
        needUpdate = true;

        // New values are not the same as the overridden values -> update overridden values with new values
      } else if (!isRuleDebtSameAsOverriddenValues(ruleDto, newSubCharacteristic, newFunction, newCoefficient, newOffset)) {
        ruleDto.setSubCharacteristicId(newSubCharacteristic.getId());

        DefaultDebtRemediationFunction debtRemediationFunction = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.valueOf(newFunction), newCoefficient, newOffset);
        ruleDto.setRemediationFunction(debtRemediationFunction.type().name());
        ruleDto.setRemediationCoefficient(debtRemediationFunction.coefficient());
        ruleDto.setRemediationOffset(debtRemediationFunction.offset());
        needUpdate = true;
      }

      // No sub-characteristic is given -> disable rule debt if not already disabled
    } else {
      // Rule characteristic is not already disabled -> update it
      if (ruleDto.getSubCharacteristicId() == null || !RuleDto.DISABLED_CHARACTERISTIC_ID.equals(ruleDto.getSubCharacteristicId())) {
        // If default characteristic is not defined, set the overridden characteristic to null in order to be able to track debt plugin update
        ruleDto.setSubCharacteristicId(ruleDto.getDefaultSubCharacteristicId() != null ? RuleDto.DISABLED_CHARACTERISTIC_ID : null);
        ruleDto.setRemediationFunction(null);
        ruleDto.setRemediationCoefficient(null);
        ruleDto.setRemediationOffset(null);
        needUpdate = true;
      }
    }

    if (needUpdate) {
      ruleDto.setUpdatedAt(updateDate);
      ruleDao.update(ruleDto, session);
    }
    return needUpdate;
  }

  private static boolean isRuleDebtSameAsDefaultValues(RuleDto ruleDto, CharacteristicDto newSubCharacteristic, @Nullable String newFunction,
                                              @Nullable String newCoefficient, @Nullable String newOffset) {
    return newSubCharacteristic.getId().equals(ruleDto.getDefaultSubCharacteristicId()) &&
      isSameRemediationFunction(newFunction, newCoefficient, newOffset, ruleDto.getDefaultRemediationFunction(), ruleDto.getDefaultRemediationCoefficient(),
        ruleDto.getDefaultRemediationOffset());
  }

  private static boolean isRuleDebtSameAsOverriddenValues(RuleDto ruleDto, CharacteristicDto newSubCharacteristic, @Nullable String newFunction,
                                                 @Nullable String newCoefficient, @Nullable String newOffset) {
    return newSubCharacteristic.getId().equals(ruleDto.getSubCharacteristicId())
      && isSameRemediationFunction(newFunction, newCoefficient, newOffset, ruleDto.getRemediationFunction(), ruleDto.getRemediationCoefficient(), ruleDto.getRemediationOffset());
  }

  private static boolean isSameRemediationFunction(@Nullable String newFunction, @Nullable String newCoefficient, @Nullable String newOffset,
                                                   String oldFunction, @Nullable String oldCoefficient, @Nullable String oldOffset) {
    return new EqualsBuilder()
      .append(oldFunction, newFunction)
      .append(oldCoefficient, newCoefficient)
      .append(oldOffset, newOffset)
      .isEquals();
  }

  private Map<String, Long> validateAndGetTagIds(List<String> newTags, SqlSession session) {
    Map<String, Long> neededTagIds = Maps.newHashMap();
    Set<String> unknownTags = Sets.newHashSet();
    for (String tag : newTags) {
      Long tagId = ruleTagDao.selectId(tag, session);
      if (tagId == null) {
        unknownTags.add(tag);
      } else {
        neededTagIds.put(tag, tagId);
      }
    }
    if (!unknownTags.isEmpty()) {
      throw new NotFoundException("The following tags are unknown and must be created before association: "
        + StringUtils.join(unknownTags, ", "));
    }
    return neededTagIds;
  }

  private boolean synchronizeTags(final Integer ruleId, List<String> newTags, Map<String, Long> neededTagIds, SqlSession session) {
    boolean ruleChanged = false;

    Set<String> tagsToKeep = Sets.newHashSet();
    List<RuleRuleTagDto> currentTags = ruleDao.selectTagsByRuleIds(ruleId, session);
    for (RuleRuleTagDto existingTag : currentTags) {
      if (existingTag.getType() == RuleTagType.ADMIN && !newTags.contains(existingTag.getTag())) {
        ruleDao.deleteTag(existingTag, session);
        ruleChanged = true;
      } else {
        tagsToKeep.add(existingTag.getTag());
      }
    }

    for (String tag : newTags) {
      if (!tagsToKeep.contains(tag)) {
        ruleDao.insert(new RuleRuleTagDto().setRuleId(ruleId).setTagId(neededTagIds.get(tag)).setType(RuleTagType.ADMIN), session);
        ruleChanged = true;
      }
    }

    ruleTagOperations.deleteUnusedTags(session);

    return ruleChanged;
  }

  private void reindexRule(RuleDto rule, SqlSession session) {
    ruleRegistry.reindex(rule, session);
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private String getLoggedLogin(UserSession userSession) {
    String login = userSession.login();
    if (Strings.isNullOrEmpty(login)) {
      throw new BadRequestException("User login can't be null");
    }
    return login;
  }

  public static class RuleChange {
    private RuleKey ruleKey;
    private String debtCharacteristicKey;
    private String debtRemediationFunction;
    private String debtRemediationCoefficient;
    private String debtRemediationOffset;

    public RuleKey ruleKey() {
      return ruleKey;
    }

    public RuleChange setRuleKey(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    @CheckForNull
    public String debtCharacteristicKey() {
      return debtCharacteristicKey;
    }

    public RuleChange setDebtCharacteristicKey(@Nullable String debtCharacteristicKey) {
      this.debtCharacteristicKey = debtCharacteristicKey;
      return this;
    }

    @CheckForNull
    public String debtRemediationFunction() {
      return debtRemediationFunction;
    }

    public RuleChange setDebtRemediationFunction(@Nullable String debtRemediationFunction) {
      this.debtRemediationFunction = debtRemediationFunction;
      return this;
    }

    @CheckForNull
    public String debtRemediationCoefficient() {
      return debtRemediationCoefficient;
    }

    public RuleChange setDebtRemediationCoefficient(@Nullable String debtRemediationCoefficient) {
      this.debtRemediationCoefficient = debtRemediationCoefficient;
      return this;
    }

    @CheckForNull
    public String debtRemediationOffset() {
      return debtRemediationOffset;
    }

    public RuleChange setDebtRemediationOffset(@Nullable String debtRemediationOffset) {
      this.debtRemediationOffset = debtRemediationOffset;
      return this;
    }
  }
}
