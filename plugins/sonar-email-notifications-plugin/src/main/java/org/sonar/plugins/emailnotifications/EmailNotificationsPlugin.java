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
package org.sonar.plugins.emailnotifications;

import com.google.common.collect.ImmutableList;
import org.sonar.api.ServerExtension;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.emailnotifications.newviolations.NewViolationsEmailTemplate;
import org.sonar.plugins.emailnotifications.newviolations.NewViolationsOnMyFavouriteProject;
import org.sonar.plugins.emailnotifications.reviews.ChangesInReviewAssignedToMeOrCreatedByMe;
import org.sonar.plugins.emailnotifications.reviews.ReviewEmailTemplate;

import java.util.List;

public class EmailNotificationsPlugin extends SonarPlugin {

  public List<Class<? extends ServerExtension>> getExtensions() {
    return ImmutableList.of(
        EmailConfiguration.class,
        EmailNotificationChannel.class,

        ReviewEmailTemplate.class,
        ChangesInReviewAssignedToMeOrCreatedByMe.class,

        NewViolationsEmailTemplate.class,
        NewViolationsOnMyFavouriteProject.class);
  }

}
