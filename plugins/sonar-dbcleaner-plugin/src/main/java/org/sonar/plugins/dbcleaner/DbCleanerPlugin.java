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
package org.sonar.plugins.dbcleaner;

import com.google.common.collect.ImmutableList;
import org.sonar.api.BatchExtension;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.dbcleaner.api.DbCleanerConstants;
import org.sonar.plugins.dbcleaner.period.DefaultPeriodCleaner;

import java.util.List;

@Properties({
  @Property(key = DbCleanerConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_WEEK, defaultValue = "4",
    name = "Number of weeks before starting to keep only one snapshot by week",
    description = "After this number of weeks, if there are several snapshots during the same week, "
      + "the DbCleaner keeps the first one and fully delete the other ones.",
    global = true,
    project = true,
    type = PropertyType.INTEGER),
  @Property(key = DbCleanerConstants.WEEKS_BEFORE_KEEPING_ONLY_ONE_SNAPSHOT_BY_MONTH, defaultValue = "52",
    name = "Number of weeks before starting to keep only one snapshot by month",
    description = "After this number of weeks, if there are several snapshots during the same month, "
      + "the DbCleaner keeps the first one and fully delete the other ones.",
    global = true,
    project = true,
    type = PropertyType.INTEGER),
  @Property(key = DbCleanerConstants.WEEKS_BEFORE_DELETING_ALL_SNAPSHOTS, defaultValue = "260",
    name = "Number of weeks before starting to delete all remaining snapshots",
    description = "After this number of weeks, all snapshots are fully deleted.",
    global = true,
    project = true,
    type = PropertyType.INTEGER)
})
public final class DbCleanerPlugin extends SonarPlugin {

  public List<Class<? extends BatchExtension>> getExtensions() {
    return ImmutableList.of(
        DefaultPeriodCleaner.class,
        DefaultPurgeTask.class,
        ProjectPurgePostJob.class);
  }
}
