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
package org.sonar.plugins.core.dashboards;

import org.sonar.api.web.Dashboard;
import org.sonar.api.web.DashboardLayout;
import org.sonar.api.web.DashboardTemplate;

/**
 * Reviews dashboard for Sonar
 *
 * @since 2.14
 */
public final class ReviewsDashboard extends DashboardTemplate {

  @Override
  public String getName() {
    return "Reviews";
  }

  @Override
  public Dashboard createDashboard() {
    Dashboard dashboard = Dashboard.create();
    dashboard.setLayout(DashboardLayout.TWO_COLUMNS);
    addFirstColumn(dashboard);
    addSecondColumn(dashboard);
    return dashboard;
  }

  private void addFirstColumn(Dashboard dashboard) {
    dashboard.addWidget("reviews_metrics", 1);
    dashboard.addWidget("action_plans", 1);
    dashboard.addWidget("planned_reviews", 1);
    dashboard.addWidget("unplanned_reviews", 1);
  }

  private void addSecondColumn(Dashboard dashboard) {
    dashboard.addWidget("reviews_per_developer", 2);
    dashboard.addWidget("my_reviews", 2);
    dashboard.addWidget("project_reviews", 2);
    dashboard.addWidget("false_positive_reviews", 2);
  }

}