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
package org.sonar.core.persistence;

/**
 * @since 2.13
 */
public final class DatabaseUtils {
  /**
   * Oracle: number of elements in IN statements is limited.
   */
  public static final int MAX_IN_ELEMENTS = 1000;

  private DatabaseUtils() {
  }

  /**
   * List of all the tables.
   * This list is hardcoded because we didn't succeed in using java.sql.DatabaseMetaData#getTables() in the same way
   * for all the supported databases, particularly due to Oracle results.
   */
  static final String[] TABLE_NAMES = {
    "action_plans",
    "action_plans_reviews",
    "active_dashboards",
    "active_rules",
    "active_rule_changes",
    "active_rule_parameters",
    "active_rule_param_changes",
    "alerts",
    "authors",
    "characteristics",
    "characteristic_edges",
    "characteristic_properties",
    "criteria",
    "dashboards",
    "dependencies",
    "duplications_index",
    "events",
    "filters",
    "filter_columns",
    "groups",
    "groups_users",
    "group_roles",
    "loaded_templates",
    "manual_measures",
    "measure_data",
    "metrics",
    "notifications",
    "projects",
    "project_links",
    "project_measures",
    "properties",
    "quality_models",
    "resource_index",
    "reviews",
    "review_comments",
    "rules",
    "rules_parameters",
    "rules_profiles",
    "rule_failures",
    "schema_migrations",
    "snapshots",
    "snapshot_sources",
    "users",
    "user_roles",
    "widgets",
    "widget_properties"};
}
