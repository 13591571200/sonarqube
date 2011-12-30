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
package org.sonar.plugins.core.batch;

import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.core.resource.ResourceIndexerDao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class IndexProjectPostJobTest {
  @Test
  public void shouldIndexProject() {
    ResourceIndexerDao indexer = mock(ResourceIndexerDao.class);
    IndexProjectPostJob job = new IndexProjectPostJob(indexer);
    Project project = new Project("foo");
    project.setId(123);

    job.executeOn(project, null);

    verify(indexer).indexProject(123);
  }

  @Test
  public void shouldNotIndexProjectIfMissingId() {
    ResourceIndexerDao indexer = mock(ResourceIndexerDao.class);
    IndexProjectPostJob job = new IndexProjectPostJob(indexer);

    job.executeOn(new Project("foo"), null);

    verifyZeroInteractions(indexer);
  }

}
