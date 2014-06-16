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
package org.sonar.batch.rule;

import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.test.IsMeasure;
import org.sonar.batch.api.rules.QProfile;
import org.sonar.batch.rules.QProfileWithId;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.qualityprofile.db.QualityProfileDao;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QProfileSensorTest extends AbstractDaoTestCase {

  ModuleQProfiles moduleQProfiles = mock(ModuleQProfiles.class);
  Project project = mock(Project.class);
  SensorContext sensorContext = mock(SensorContext.class);
  DefaultFileSystem fs = new DefaultFileSystem();

  @Test
  public void to_string() throws Exception {
    QualityProfileDao dao = mock(QualityProfileDao.class);
    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, dao);
    assertThat(sensor.toString()).isEqualTo("QProfileSensor");
  }

  @Test
  public void no_qprofiles() throws Exception {
    setupData("shared");
    QualityProfileDao dao = new QualityProfileDao(getMyBatis());
    when(moduleQProfiles.findAll()).thenReturn(Collections.<QProfile>emptyList());

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, dao);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    // measures are not saved
    verify(sensorContext).saveMeasure(argThat(new IsMeasure(CoreMetrics.QUALITY_PROFILES, "[]")));
  }

  @Test
  public void mark_profiles_as_used() throws Exception {
    setupData("shared");

    QualityProfileDao dao = new QualityProfileDao(getMyBatis());
    when(moduleQProfiles.findByLanguage("java")).thenReturn(new QProfileWithId(2, "Java Two", "java", 20));
    when(moduleQProfiles.findByLanguage("php")).thenReturn(new QProfileWithId(3, "Php One", "php", 30));
    when(moduleQProfiles.findByLanguage("abap")).thenReturn(null);
    fs.addLanguages("java", "php", "abap");

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, dao);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    checkTable("mark_profiles_as_used", "rules_profiles");
  }

  @Test
  public void store_measures_on_single_lang_module() throws Exception {
    setupData("shared");

    QualityProfileDao dao = new QualityProfileDao(getMyBatis());
    when(moduleQProfiles.findByLanguage("java")).thenReturn(new QProfileWithId(2, "Java Two", "java", 20));
    when(moduleQProfiles.findByLanguage("php")).thenReturn(new QProfileWithId(3, "Php One", "php", 30));
    when(moduleQProfiles.findByLanguage("abap")).thenReturn(null);
    fs.addLanguages("java");

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, dao);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    verify(sensorContext).saveMeasure(argThat(new IsMeasure(CoreMetrics.PROFILE, "Java Two")));
    verify(sensorContext).saveMeasure(argThat(new IsMeasure(CoreMetrics.PROFILE_VERSION, 20.0)));
    verify(sensorContext).saveMeasure(
      argThat(new IsMeasure(CoreMetrics.QUALITY_PROFILES, "[{\"id\":2,\"name\":\"Java Two\",\"version\":20,\"language\":\"java\"}]")));
  }

  @Test
  public void store_measures_on_multi_lang_module() throws Exception {
    setupData("shared");

    QualityProfileDao dao = new QualityProfileDao(getMyBatis());
    when(moduleQProfiles.findByLanguage("java")).thenReturn(new QProfileWithId(2, "Java Two", "java", 20));
    when(moduleQProfiles.findByLanguage("php")).thenReturn(new QProfileWithId(3, "Php One", "php", 30));
    when(moduleQProfiles.findByLanguage("abap")).thenReturn(null);
    fs.addLanguages("java", "php");

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, fs, dao);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    verify(sensorContext).saveMeasure(
      argThat(new IsMeasure(CoreMetrics.QUALITY_PROFILES,
        "[{\"id\":2,\"name\":\"Java Two\",\"version\":20,\"language\":\"java\"},{\"id\":3,\"name\":\"Php One\",\"version\":30,\"language\":\"php\"}]")));
  }
}
