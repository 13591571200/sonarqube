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
package org.sonar.plugins.core.security;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultResourcePermissionsTest extends AbstractDaoTestCase {

  private Resource project = new Project("project").setId(123);

  @Test
  public void grantGroupRole() {
    setupData("grantGroupRole");

    DefaultResourcePermissions permissions = new DefaultResourcePermissions(new Settings(), getMyBatis());
    permissions.grantGroupRole(project, "sonar-administrators", "admin");

    // do not insert duplicated rows
    permissions.grantGroupRole(project, "sonar-administrators", "admin");

    checkTables("grantGroupRole", new String[] {"id"}, "group_roles");
  }

  @Test
  public void grantGroupRole_anyone() {
    setupData("grantGroupRole_anyone");

    DefaultResourcePermissions permissions = new DefaultResourcePermissions(new Settings(), getMyBatis());
    permissions.grantGroupRole(project, DefaultGroups.ANYONE, "admin");

    checkTables("grantGroupRole_anyone", "group_roles");
  }

  @Test
  public void grantGroupRole_ignore_if_group_not_found() {
    setupData("grantGroupRole_ignore_if_group_not_found");

    DefaultResourcePermissions permissions = new DefaultResourcePermissions(new Settings(), getMyBatis());
    permissions.grantGroupRole(project, "not_found", "admin");

    checkTables("grantGroupRole_ignore_if_group_not_found", "group_roles");
  }

  @Test
  public void grantGroupRole_ignore_if_not_persisted() {
    setupData("grantGroupRole_ignore_if_not_persisted");

    DefaultResourcePermissions permissions = new DefaultResourcePermissions(new Settings(), getMyBatis());
    Project resourceWithoutId = new Project("");
    permissions.grantGroupRole(resourceWithoutId, "sonar-users", "admin");

    checkTables("grantGroupRole_ignore_if_not_persisted", "group_roles");
  }

  @Test
  public void grantUserRole() {
    setupData("grantUserRole");

    DefaultResourcePermissions permissions = new DefaultResourcePermissions(new Settings(), getMyBatis());
    permissions.grantUserRole(project, "marius", "admin");

    // do not insert duplicated rows
    permissions.grantUserRole(project, "marius", "admin");

    checkTables("grantUserRole", new String[] {"id"}, "user_roles");
  }

  @Test
  public void grantDefaultRoles() {
    setupData("grantDefaultRoles");

    Settings settings = new Settings();
    settings.setProperty("sonar.role.admin.TRK.defaultGroups", "sonar-administrators");
    settings.setProperty("sonar.role.admin.TRK.defaultUsers", "");
    settings.setProperty("sonar.role.user.TRK.defaultGroups", "Anyone,sonar-users");
    settings.setProperty("sonar.role.user.TRK.defaultUsers", "");
    settings.setProperty("sonar.role.codeviewer.TRK.defaultGroups", "Anyone,sonar-users");
    settings.setProperty("sonar.role.codeviewer.TRK.defaultUsers", "");
    DefaultResourcePermissions permissions = new DefaultResourcePermissions(settings, getMyBatis());

    permissions.grantDefaultRoles(project);

    checkTables("grantDefaultRoles", "user_roles", "group_roles");
  }

  @Test
  public void grantDefaultRoles_unknown_group() {
    setupData("grantDefaultRoles_unknown_group");

    Settings settings = new Settings();
    settings.setProperty("sonar.role.admin.TRK.defaultGroups", "sonar-administrators,unknown");
    DefaultResourcePermissions permissions = new DefaultResourcePermissions(settings, getMyBatis());
    permissions.grantDefaultRoles(project);

    checkTables("grantDefaultRoles_unknown_group", "group_roles");
  }

  @Test
  public void grantDefaultRoles_users() {
    setupData("grantDefaultRoles_users");

    Settings settings = new Settings();
    settings.setProperty("sonar.role.admin.TRK.defaultUsers", "marius,disabled,notfound");
    DefaultResourcePermissions permissions = new DefaultResourcePermissions(settings, getMyBatis());
    permissions.grantDefaultRoles(project);

    checkTables("grantDefaultRoles_users", "user_roles");
  }

  @Test
  public void hasRoles() {
    setupData("hasRoles");
    DefaultResourcePermissions permissions = new DefaultResourcePermissions(new Settings(), getMyBatis());

    // no groups and at least one user
    assertThat(permissions.hasRoles(new Project("only_users").setId(1))).isTrue();

    // no users and at least one group
    assertThat(permissions.hasRoles(new Project("only_groups").setId(2))).isTrue();

    // groups and users
    assertThat(permissions.hasRoles(new Project("groups_and_users").setId(3))).isTrue();

    // no groups, no users
    assertThat(permissions.hasRoles(new Project("no_groups_no_users").setId(4))).isFalse();

    // does not exist
    assertThat(permissions.hasRoles(new Project("not_found"))).isFalse();
  }
}
