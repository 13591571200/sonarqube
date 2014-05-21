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

package org.sonar.server.qualityprofile;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class QProfilesTest {

  @Mock
  QProfileProjectOperations projectOperations;

  @Mock
  QProfileProjectLookup projectLookup;

  @Mock
  QProfileLookup profileLookup;

  @Mock
  QProfileOperations profileOperations;

  QProfiles qProfiles;

  @Before
  public void setUp() throws Exception {
    qProfiles = new QProfiles(projectOperations, projectLookup, profileLookup, profileOperations);
  }

  @Test
  public void search_profile_by_id() throws Exception {
    qProfiles.profile(1);
    verify(profileLookup).profile(1);
  }

  @Test
  public void search_profile_by_name_and_language() throws Exception {
    qProfiles.profile("Default", "java");
    verify(profileLookup).profile("Default", "java");
  }

  @Test
  public void search_profiles() throws Exception {
    qProfiles.allProfiles();
    verify(profileLookup).allProfiles();
  }

  @Test
  public void search_profiles_by_language() throws Exception {
    qProfiles.profilesByLanguage("java");
    verify(profileLookup).profiles("java");
  }

  @Test
  public void search_default_profile_by_language() throws Exception {
    qProfiles.defaultProfile("java");
    verify(profileLookup).defaultProfile("java");
  }

  @Test
  public void search_parent_profile() throws Exception {
    QProfile profile = new QProfile().setId(1).setParent("Parent").setLanguage("java");
    qProfiles.parent(profile);
    verify(profileLookup).parent(profile);
  }

  @Test
  public void search_children() throws Exception {
    QProfile profile = new QProfile();
    qProfiles.children(profile);
    verify(profileLookup).children(profile);
  }

  @Test
  public void search_ancestors() throws Exception {
    QProfile profile = new QProfile();
    qProfiles.ancestors(profile);
    verify(profileLookup).ancestors(profile);
  }

  @Test
  public void count_children() throws Exception {
    QProfile profile = new QProfile();
    qProfiles.countChildren(profile);
    verify(profileLookup).countChildren(profile);
  }

  @Test
  public void create_new_profile() throws Exception {
    Map<String, String> xmlProfilesByPlugin = newHashMap();
    qProfiles.newProfile("Default", "java", xmlProfilesByPlugin);
    verify(profileOperations).newProfile(eq("Default"), eq("java"), eq(xmlProfilesByPlugin), any(UserSession.class));
  }

  @Test
  public void fail_to_create_profile_without_name() throws Exception {
    try {
      qProfiles.newProfile("", "java", Maps.<String, String>newHashMap());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void rename_profile() throws Exception {
    qProfiles.renameProfile(1, "Default profile");
    verify(profileOperations).renameProfile(eq(1), eq("Default profile"), any(UserSession.class));
  }

  @Test
  public void delete_profile() throws Exception {
    qProfiles.deleteProfile(1);
    verify(profileOperations).deleteProfile(eq(1), any(UserSession.class));
  }

  @Test
  public void update_default_profile() throws Exception {
    qProfiles.setDefaultProfile(1);
    verify(profileOperations).setDefaultProfile(eq(1), any(UserSession.class));
  }

  @Test
  public void copy_profile() throws Exception {
    qProfiles.copyProfile(1, "Copy Profile");
    verify(profileOperations).copyProfile(eq(1), eq("Copy Profile"), any(UserSession.class));
  }

  @Test
  public void update_parent_profile() throws Exception {
    qProfiles.updateParentProfile(1, 2);
    verify(profileOperations).updateParentProfile(eq(1), eq(2), any(UserSession.class));
  }

  @Test
  public void projects() throws Exception {
    qProfiles.projects(1);
    verify(projectLookup).projects(1);
  }

  @Test
  public void count_projects() throws Exception {
    QProfile profile = new QProfile();
    qProfiles.countProjects(profile);
    verify(projectLookup).countProjects(profile);
  }

  @Test
  public void get_profiles_from_project_and_language() throws Exception {
    qProfiles.findProfileByProjectAndLanguage(1, "java");
    verify(projectLookup).findProfileByProjectAndLanguage(1, "java");
  }

  @Test
  public void add_project() throws Exception {
    qProfiles.addProject(1, 10L);
    verify(projectOperations).addProject(eq(1), eq(10L), any(UserSession.class));
  }

  @Test
  public void remove_project_by_quality_profile_id() throws Exception {
    qProfiles.removeProject(1, 10L);
    verify(projectOperations).removeProject(eq(1), eq(10L), any(UserSession.class));
  }

  @Test
  public void remove_project_by_language() throws Exception {
    qProfiles.removeProjectByLanguage("java", 10L);
    verify(projectOperations).removeProject(eq("java"), eq(10L), any(UserSession.class));
  }

  @Test
  public void remove_all_projects() throws Exception {
    qProfiles.removeAllProjects(1);
    verify(projectOperations).removeAllProjects(eq(1), any(UserSession.class));
  }
}
