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
package org.sonar.api.batch.rule.internal;

import com.google.common.collect.Maps;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;

import java.util.Map;

/**
 * Builds instances of {@link org.sonar.api.batch.rule.ActiveRules}.
 * <b>For unit testing and internal use only</b>.
 *
 * @since 4.2
 */
public class ActiveRulesBuilder {

  private final Map<RuleKey, NewActiveRule> map = Maps.newHashMap();

  public NewActiveRule activate(RuleKey ruleKey) {
    if (map.containsKey(ruleKey)) {
      throw new IllegalStateException(String.format("Rule '%s' is already activated", ruleKey));
    }
    NewActiveRule newActiveRule = new NewActiveRule(this, ruleKey);
    map.put(ruleKey, newActiveRule);
    return newActiveRule;
  }

  public ActiveRules build() {
    return new DefaultActiveRules(map.values());
  }
}
