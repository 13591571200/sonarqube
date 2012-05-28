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
package org.sonar.api.workflow.condition;

import com.google.common.annotations.Beta;

/**
 * Static utility methods pertaining to {@link Condition} instances.
 *
 * @since 3.1
 */
@Beta
public final class Conditions {

  private Conditions() {
  }

  public static Condition not(Condition c) {
    return new NotCondition(c);
  }

  public static Condition hasReviewProperty(String propertyKey) {
    return new HasReviewPropertyCondition(propertyKey);
  }

  public static Condition hasProjectProperty(String propertyKey) {
    return new HasProjectPropertyCondition(propertyKey);
  }

  public static Condition hasAdminRole() {
    return new AdminRoleCondition();
  }

  public static Condition statuses(String... statuses) {
    return new StatusCondition(statuses);
  }

  public static Condition resolutions(String... resolutions) {
    return new ResolutionCondition(resolutions);
  }

}
