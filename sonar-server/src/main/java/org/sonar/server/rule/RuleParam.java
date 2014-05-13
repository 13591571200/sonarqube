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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.server.rule.RuleParamType;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @deprecated to be dropped in 4.4
 */
@Deprecated
public class RuleParam {

  private final String key;
  private final String description;
  private final String defaultValue;
  private final RuleParamType type;

  public RuleParam(String key, String description, @Nullable String defaultValue, RuleParamType type) {
    this.key = key;
    this.description = description;
    this.defaultValue = defaultValue;
    this.type = type;
  }

  public String key() {
    return key;
  }

  public String description() {
    return description;
  }

  @CheckForNull
  public String defaultValue() {
    return defaultValue;
  }

  public RuleParamType type() {
    return type;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this).toString();
  }
}
