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
package org.sonar.batch.scan;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.core.source.db.SnapshotSourceDao;

import java.net.URI;
import java.net.URISyntaxException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class LastSnapshotsTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TestDatabase db = new TestDatabase();

  private AnalysisMode mode;

  @Before
  public void before() {
    mode = mock(AnalysisMode.class);
    when(mode.getPreviewReadTimeoutSec()).thenReturn(30);
  }

  @Test
  public void should_get_source_of_last_snapshot() {
    db.prepareDbUnit(getClass(), "last_snapshot.xml");

    ServerClient server = mock(ServerClient.class);
    LastSnapshots lastSnapshots = new LastSnapshots(mode, new SnapshotSourceDao(db.myBatis()), server);

    assertThat(lastSnapshots.getSource(newFile())).isEqualTo("this is bar");
    verifyZeroInteractions(server);
  }

  @Test
  public void should_return_empty_source_if_no_last_snapshot() {
    db.prepareDbUnit(getClass(), "no_last_snapshot.xml");
    ServerClient server = mock(ServerClient.class);

    LastSnapshots lastSnapshots = new LastSnapshots(mode, new SnapshotSourceDao(db.myBatis()), server);

    assertThat(lastSnapshots.getSource(newFile())).isEqualTo("");
    verifyZeroInteractions(server);
  }

  @Test
  public void should_download_source_from_ws_if_preview_mode() {
    db.prepareDbUnit(getClass(), "last_snapshot.xml");
    ServerClient server = mock(ServerClient.class);
    when(server.request(anyString(), eq(false), eq(30 * 1000))).thenReturn("downloaded source of Bar.c");

    when(mode.isPreview()).thenReturn(true);
    LastSnapshots lastSnapshots = new LastSnapshots(mode, new SnapshotSourceDao(db.myBatis()), server);

    String source = lastSnapshots.getSource(newFile());
    assertThat(source).isEqualTo("downloaded source of Bar.c");
    verify(server).request("/api/sources?resource=myproject:org/foo/Bar.c&format=txt", false, 30 * 1000);
  }

  @Test
  public void should_fail_to_download_source_from_ws() throws URISyntaxException {
    db.prepareDbUnit(getClass(), "last_snapshot.xml");
    ServerClient server = mock(ServerClient.class);
    when(server.request(anyString(), eq(false), eq(30 * 1000))).thenThrow(new HttpDownloader.HttpException(new URI(""), 500));

    when(mode.isPreview()).thenReturn(true);
    LastSnapshots lastSnapshots = new LastSnapshots(mode, new SnapshotSourceDao(db.myBatis()), server);

    thrown.expect(HttpDownloader.HttpException.class);
    lastSnapshots.getSource(newFile());
  }

  @Test
  public void should_return_empty_source_if_preview_mode_and_no_last_snapshot() throws URISyntaxException {
    db.prepareDbUnit(getClass(), "last_snapshot.xml");
    ServerClient server = mock(ServerClient.class);
    when(server.request(anyString(), eq(false), eq(30 * 1000))).thenThrow(new HttpDownloader.HttpException(new URI(""), 404));

    when(mode.isPreview()).thenReturn(true);
    LastSnapshots lastSnapshots = new LastSnapshots(mode, new SnapshotSourceDao(db.myBatis()), server);

    String source = lastSnapshots.getSource(newFile());
    assertThat(source).isEqualTo("");
    verify(server).request("/api/sources?resource=myproject:org/foo/Bar.c&format=txt", false, 30 * 1000);
  }

  @Test
  public void should_not_load_source_of_non_files() throws URISyntaxException {
    db.prepareDbUnit(getClass(), "last_snapshot.xml");
    ServerClient server = mock(ServerClient.class);

    LastSnapshots lastSnapshots = new LastSnapshots(mode, new SnapshotSourceDao(db.myBatis()), server);

    String source = lastSnapshots.getSource(new Project("my-project"));
    assertThat(source).isEqualTo("");
  }

  private File newFile() {
    File file = new File("org/foo", "Bar.c");
    file.setEffectiveKey("myproject:org/foo/Bar.c");
    return file;
  }
}
