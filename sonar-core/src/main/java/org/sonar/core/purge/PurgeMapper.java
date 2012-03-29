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
package org.sonar.core.purge;

import java.util.List;

public interface PurgeMapper {

  List<Long> selectSnapshotIds(PurgeSnapshotQuery query);

  List<Long> selectProjectIdsByRootId(long rootResourceId);

  void deleteSnapshot(long snapshotId);

  void deleteSnapshotDependencies(long snapshotId);

  void deleteSnapshotDuplications(long snapshotId);

  void deleteSnapshotEvents(long snapshotId);

  void deleteSnapshotMeasures(long snapshotId);

  void deleteSnapshotMeasureData(long snapshotId);

  void deleteSnapshotSource(long snapshotId);

  void deleteSnapshotViolations(long snapshotId);

  void deleteSnapshotWastedMeasures(long snapshotId);

  void deleteSnapshotMeasuresOnQualityModelRequirements(long snapshotId);

  void updatePurgeStatusToOne(long snapshotId);

  void disableResource(long resourceId);

  void deleteResourceIndex(long resourceId);

  void deleteEvent(long eventId);

  void setSnapshotIsLastToFalse(long resourceId);

  void deleteResourceLinks(long resourceId);

  void deleteResourceProperties(long resourceId);

  void deleteResource(long resourceId);

  void deleteResourceGroupRoles(long resourceId);

  void deleteResourceUserRoles(long resourceId);

  void deleteResourceManualMeasures(long resourceId);

  void deleteResourceReviews(long resourceId);

  void deleteResourceEvents(long resourceId);

  void deleteResourceActionPlans(long resourceId);

  void deleteAuthors(long developerId);

  void closeResourceReviews(long resourceId);

  List<PurgeableSnapshotDto> selectPurgeableSnapshotsWithEvents(long resourceId);

  List<PurgeableSnapshotDto> selectPurgeableSnapshotsWithoutEvents(long resourceId);

  List<Long> selectResourceIdsByRootId(long rootProjectId);
}
