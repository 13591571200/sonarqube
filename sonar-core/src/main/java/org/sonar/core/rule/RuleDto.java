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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Cardinality;
import org.sonar.core.db.Dto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

public final class RuleDto implements Dto<RuleKey> {

  public static final Integer DISABLED_CHARACTERISTIC_ID = -1;

  private Integer id;
  private String repositoryKey;
  private String ruleKey;
  private String description;
  private String status;
  private String name;
  private String configKey;
  private Integer severity;
  private Cardinality cardinality;
  private String language;
  private Integer parentId;
  private String noteData;
  private String noteUserLogin;
  private Date noteCreatedAt;
  private Date noteUpdatedAt;
  private Integer subCharacteristicId;
  private Integer defaultSubCharacteristicId;
  private String remediationFunction;
  private String defaultRemediationFunction;
  private String remediationCoefficient;
  private String defaultRemediationCoefficient;
  private String remediationOffset;
  private String defaultRemediationOffset;
  private String effortToFixDescription;
  private String tags;
  private String systemTags;
  private Date createdAt;
  private Date updatedAt;

  private transient RuleKey key;

  @Override
  public RuleKey getKey() {
    if (key == null) {
      key = RuleKey.of(getRepositoryKey(), getRuleKey());
    }
    return key;
  }

  public Integer getId() {
    return id;
  }

  public RuleDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public RuleDto setRepositoryKey(String repositoryKey) {
    this.repositoryKey = repositoryKey;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public RuleDto setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public RuleDto setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public RuleDto setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getName() {
    return name;
  }

  public RuleDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getConfigKey() {
    return configKey;
  }

  public RuleDto setConfigKey(String configKey) {
    this.configKey = configKey;
    return this;
  }

  public Integer getSeverity() {
    return severity;
  }

  public String getSeverityString() {
    return SeverityUtil.getSeverityFromOrdinal(severity);
  }

  public RuleDto setSeverity(String severity) {
    this.severity = SeverityUtil.getOrdinalFromSeverity(severity);
    return this;
  }

  public RuleDto setSeverity(Integer severity) {
    this.severity = severity;
    return this;
  }

  public Cardinality getCardinality() {
    return cardinality;
  }

  public RuleDto setCardinality(Cardinality cardinality) {
    this.cardinality = cardinality;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public RuleDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  @CheckForNull
  public Integer getParentId() {
    return parentId;
  }

  public RuleDto setParentId(@Nullable Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  public String getNoteData() {
    return noteData;
  }

  public RuleDto setNoteData(String noteData) {
    this.noteData = noteData;
    return this;
  }

  public String getNoteUserLogin() {
    return noteUserLogin;
  }

  public RuleDto setNoteUserLogin(String noteUserLogin) {
    this.noteUserLogin = noteUserLogin;
    return this;
  }

  public Date getNoteCreatedAt() {
    return noteCreatedAt;
  }

  public RuleDto setNoteCreatedAt(Date noteCreatedAt) {
    this.noteCreatedAt = noteCreatedAt;
    return this;
  }

  public Date getNoteUpdatedAt() {
    return noteUpdatedAt;
  }

  public RuleDto setNoteUpdatedAt(Date noteUpdatedAt) {
    this.noteUpdatedAt = noteUpdatedAt;
    return this;
  }

  @CheckForNull
  public Integer getSubCharacteristicId() {
    return subCharacteristicId;
  }

  public RuleDto setSubCharacteristicId(@Nullable Integer subCharacteristicId) {
    this.subCharacteristicId = subCharacteristicId;
    return this;
  }

  @CheckForNull
  public Integer getDefaultSubCharacteristicId() {
    return defaultSubCharacteristicId;
  }

  public RuleDto setDefaultSubCharacteristicId(@Nullable Integer defaultSubCharacteristicId) {
    this.defaultSubCharacteristicId = defaultSubCharacteristicId;
    return this;
  }

  @CheckForNull
  public String getRemediationFunction() {
    return remediationFunction;
  }

  public RuleDto setRemediationFunction(@Nullable String remediationFunction) {
    this.remediationFunction = remediationFunction;
    return this;
  }

  @CheckForNull
  public String getDefaultRemediationFunction() {
    return defaultRemediationFunction;
  }

  public RuleDto setDefaultRemediationFunction(@Nullable String defaultRemediationFunction) {
    this.defaultRemediationFunction = defaultRemediationFunction;
    return this;
  }

  @CheckForNull
  public String getRemediationCoefficient() {
    return remediationCoefficient;
  }

  public RuleDto setRemediationCoefficient(@Nullable String remediationCoefficient) {
    this.remediationCoefficient = remediationCoefficient;
    return this;
  }

  @CheckForNull
  public String getDefaultRemediationCoefficient() {
    return defaultRemediationCoefficient;
  }

  public RuleDto setDefaultRemediationCoefficient(@Nullable String defaultRemediationCoefficient) {
    this.defaultRemediationCoefficient = defaultRemediationCoefficient;
    return this;
  }

  @CheckForNull
  public String getRemediationOffset() {
    return remediationOffset;
  }

  public RuleDto setRemediationOffset(@Nullable String remediationOffset) {
    this.remediationOffset = remediationOffset;
    return this;
  }

  @CheckForNull
  public String getDefaultRemediationOffset() {
    return defaultRemediationOffset;
  }

  public RuleDto setDefaultRemediationOffset(@Nullable String defaultRemediationOffset) {
    this.defaultRemediationOffset = defaultRemediationOffset;
    return this;
  }

  @CheckForNull
  public String getEffortToFixDescription() {
    return effortToFixDescription;
  }

  public RuleDto setEffortToFixDescription(@Nullable String effortToFixDescription) {
    this.effortToFixDescription = effortToFixDescription;
    return this;
  }

  public Set<String> getTags() {
    return tags == null ?
      Collections.EMPTY_SET :
      new TreeSet<String>(Arrays.asList(StringUtils.split(tags, ',')));
  }

  public Set<String> getSystemTags() {
    return systemTags == null ?
      Collections.EMPTY_SET :
      new TreeSet<String>(Arrays.asList(StringUtils.split(systemTags, ',')));
  }

  private String getTagsField() {
    return tags;
  }

  private String getSystemTagsField() {
    return systemTags;
  }

  private void setTagsField(String s) {
    tags = s;
  }

  private void setSystemTagsField(String s) {
    systemTags = s;
  }

  public RuleDto setTags(Set<String> tags) {
    this.tags = StringUtils.join(tags, ',');
    return this;
  }

  public RuleDto setSystemTags(Set<String> tags) {
    this.systemTags = StringUtils.join(tags, ',');
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public RuleDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public RuleDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RuleDto)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RuleDto other = (RuleDto) obj;
    return new EqualsBuilder()
      .append(repositoryKey, other.getRepositoryKey())
      .append(ruleKey, other.getRuleKey())
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(repositoryKey)
      .append(ruleKey)
      .toHashCode();
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
