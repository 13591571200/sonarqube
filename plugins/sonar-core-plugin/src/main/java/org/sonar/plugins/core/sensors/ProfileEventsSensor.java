/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.sonar.api.batch.*;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;

import java.util.List;

public class ProfileEventsSensor implements Sensor {

  private final RulesProfile profile;
  private final TimeMachine timeMachine;

  public ProfileEventsSensor(RulesProfile profile, TimeMachine timeMachine) {
    this.profile = profile;
    this.timeMachine = timeMachine;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext context) {
    if (profile == null) {
      return;
    }

    Measure pastProfileMeasure = getPreviousMeasure(project, CoreMetrics.PROFILE);
    if (pastProfileMeasure == null) {
      return; // first analysis
    }
    int pastProfileId = pastProfileMeasure.getIntValue();
    Measure pastProfileVersionMeasure = getPreviousMeasure(project, CoreMetrics.PROFILE_VERSION);
    final int pastProfileVersion;
    if (pastProfileVersionMeasure == null) { // first analysis with versions
      pastProfileVersion = 1;
    } else {
      pastProfileVersion = pastProfileVersionMeasure.getIntValue();
    }
    String pastProfile = formatProfileDescription(pastProfileMeasure.getData(), pastProfileVersion);

    int currentProfileId = profile.getId();
    int currentProfileVersion = profile.getVersion();
    String currentProfile = formatProfileDescription(profile.getName(), currentProfileVersion);

    if ((pastProfileId != currentProfileId) || (pastProfileVersion != currentProfileVersion)) {
      // A different profile is used for this project or new version of same profile
      context.createEvent(project, currentProfile, currentProfile + " is used instead of " + pastProfile, Event.CATEGORY_PROFILE, null);
    }
  }

  private static String formatProfileDescription(String name, int version) {
    return name + " version " + version;
  }

  private Measure getPreviousMeasure(Project project, Metric metric) {
    TimeMachineQuery query = new TimeMachineQuery(project)
        .setOnlyLastAnalysis(true)
        .setMetrics(metric);
    List<Measure> measures = timeMachine.getMeasures(query);
    if (measures.isEmpty()) {
      return null;
    }
    return measures.get(0);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
