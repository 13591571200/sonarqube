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
package org.sonar.plugins.core.sensors;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ProfileEventsSensorTest {

  private Project project;
  private SensorContext context;

  @Before
  public void prepare() {
    project = mock(Project.class);
    context = mock(SensorContext.class);
  }

  @Test
  public void shouldExecute() {
    ProfileEventsSensor sensor = new ProfileEventsSensor(null, null);

    assertThat(sensor.shouldExecuteOnProject(project), is(true));
    verifyZeroInteractions(project);
  }

  @Test
  public void shouldDoNothingIfNoProfile() {
    ProfileEventsSensor sensor = new ProfileEventsSensor(null, null);

    sensor.analyse(project, context);

    verify(context, never()).createEvent((Resource) anyObject(), anyString(), anyString(), anyString(), (Date) anyObject());
  }

  @Test
  public void shouldDoNothingIfNoProfileChange() {
    RulesProfile profile = mockProfileWithVersion(1);
    TimeMachine timeMachine = mockTM(project, 22.0, "Foo", 1.0); // Same profile, same version
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verifyZeroInteractions(context);
  }

  @Test
  public void shouldCreateEventIfProfileChange() {
    RulesProfile profile = mockProfileWithVersion(1);
    TimeMachine timeMachine = mockTM(project, 21.0, "Bar", 1.0); // Different profile
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project),
        eq("Foo version 1"),
        eq("Foo version 1 is used instead of Bar version 1"),
        same(Event.CATEGORY_PROFILE), (Date) anyObject());
  }

  @Test
  public void shouldCreateEventIfProfileVersionChange() {
    RulesProfile profile = mockProfileWithVersion(2);
    TimeMachine timeMachine = mockTM(project, 22.0, "Foo", 1.0); // Same profile, different version
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project),
        eq("Foo version 2"),
        eq("Foo version 2 is used instead of Foo version 1"),
        same(Event.CATEGORY_PROFILE), (Date) anyObject());
  }

  @Test
  public void shouldNotCreateEventIfFirstAnalysis() {
    RulesProfile profile = mockProfileWithVersion(2);
    TimeMachine timeMachine = mockTM(project, null, null);
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verifyZeroInteractions(context);
  }

  @Test
  public void shouldCreateEventIfFirstAnalysisWithVersionsAndVersionMoreThan1() {
    RulesProfile profile = mockProfileWithVersion(2);
    TimeMachine timeMachine = mockTM(project, 22.0, "Foo", null);
    ProfileEventsSensor sensor = new ProfileEventsSensor(profile, timeMachine);

    sensor.analyse(project, context);

    verify(context).createEvent(same(project),
        eq("Foo version 2"),
        eq("Foo version 2 is used instead of Foo version 1"),
        same(Event.CATEGORY_PROFILE), (Date) anyObject());
  }

  private RulesProfile mockProfileWithVersion(int version) {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getId()).thenReturn(22);
    when(profile.getName()).thenReturn("Foo");
    when(profile.getVersion()).thenReturn(version);
    return profile;
  }

  private TimeMachine mockTM(Project project, double profileId, String profileName, Double versionValue) {
    return mockTM(project, new Measure(CoreMetrics.PROFILE, profileId, profileName), versionValue == null ? null : new Measure(CoreMetrics.PROFILE_VERSION, versionValue));
  }

  private TimeMachine mockTM(Project project, Measure result1, Measure result2) {
    TimeMachine timeMachine = mock(TimeMachine.class);

    when(timeMachine.getMeasures(any(TimeMachineQuery.class)))
        .thenReturn(result1 == null ? Collections.<Measure> emptyList() : Arrays.asList(result1))
        .thenReturn(result2 == null ? Collections.<Measure> emptyList() : Arrays.asList(result2));

    return timeMachine;
  }

}
