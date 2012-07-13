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
package org.sonar.batch.components;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.fest.assertions.Assertions.assertThat;

public class PastSnapshotFinderByPreviousVersionTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldFindByPreviousVersion() {
    setupData("with-previous-version");
    PastSnapshotFinderByPreviousVersion finder = new PastSnapshotFinderByPreviousVersion(getSession());

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshot foundSnapshot = finder.findByPreviousVersion(currentProjectSnapshot);
    assertThat(foundSnapshot.getProjectSnapshotId()).isEqualTo(1009);
    assertThat(foundSnapshot.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(foundSnapshot.getModeParameter()).isEqualTo("1.1");

    // and test also another version to verify that unprocessed snapshots are ignored
    currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1009);
    assertThat(finder.findByPreviousVersion(currentProjectSnapshot).getProjectSnapshotId()).isEqualTo(1003);
  }

  @Test
  public void testWithNoPreviousVersion() {
    setupData("no-previous-version");
    PastSnapshotFinderByPreviousVersion finder = new PastSnapshotFinderByPreviousVersion(getSession());

    Snapshot currentProjectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1003);
    PastSnapshot foundSnapshot = finder.findByPreviousVersion(currentProjectSnapshot);
    assertThat(foundSnapshot.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(foundSnapshot.getProjectSnapshot()).isNull();
    assertThat(foundSnapshot.getModeParameter()).isNull();
  }

}
