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
package org.sonar.core.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class PluginInstallerTest {

  private PluginInstaller extractor = new PluginInstaller();

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void shouldExtractMetadata() {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFile("sonar-checkstyle-plugin-2.8.jar"), true);

    assertThat(metadata.getKey()).isEqualTo("checkstyle");
    assertThat(metadata.getBasePlugin()).isNull();
    assertThat(metadata.getName()).isEqualTo("Checkstyle");
    assertThat(metadata.isCore()).isEqualTo(true);
    assertThat(metadata.getFile().getName()).isEqualTo("sonar-checkstyle-plugin-2.8.jar");
    assertThat(metadata.getVersion()).isEqualTo("2.8");
  }

  @Test
  public void should_read_sonar_version() {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFile("sonar-switch-off-violations-plugin-1.1.jar"), false);

    assertThat(metadata.getVersion()).isEqualTo("1.1");
    assertThat(metadata.getSonarVersion()).isEqualTo("2.5");
  }

  @Test
  public void shouldExtractDeprecatedMetadata() {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFile("sonar-emma-plugin-0.3.jar"), false);

    assertThat(metadata.getKey()).isEqualTo("emma");
    assertThat(metadata.getBasePlugin()).isNull();
    assertThat(metadata.getName()).isEqualTo("Emma");
  }

  @Test
  public void shouldExtractExtensionMetadata() {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFile("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"), true);

    assertThat(metadata.getKey()).isEqualTo("checkstyleextensions");
    assertThat(metadata.getBasePlugin()).isEqualTo("checkstyle");
  }

  @Test
  public void shouldCopyAndExtractDependencies() throws IOException {
    File toDir = temporaryFolder.newFolder();

    DefaultPluginMetadata metadata = extractor.install(getFile("sonar-checkstyle-plugin-2.8.jar"), true, null, toDir);

    assertThat(metadata.getKey()).isEqualTo("checkstyle");
    assertThat(new File(toDir, "sonar-checkstyle-plugin-2.8.jar")).exists();
    assertThat(new File(toDir, "META-INF/lib/checkstyle-5.1.jar")).exists();
  }

  @Test
  public void shouldExtractOnlyDependencies() throws IOException {
    File toDir = temporaryFolder.newFolder();

    extractor.install(getFile("sonar-checkstyle-plugin-2.8.jar"), true, null, toDir);

    assertThat(new File(toDir, "sonar-checkstyle-plugin-2.8.jar")).exists();
    assertThat(new File(toDir, "META-INF/MANIFEST.MF")).doesNotExist();
    assertThat(new File(toDir, "org/sonar/plugins/checkstyle/CheckstyleVersion.class")).doesNotExist();
  }

  @Test
  public void shouldCopyRuleExtensionsOnServerSide() throws IOException {
    File toDir = temporaryFolder.newFolder();

    DefaultPluginMetadata metadata = DefaultPluginMetadata.create(getFile("sonar-checkstyle-plugin-2.8.jar"))
        .setKey("checkstyle")
        .addDeprecatedExtension(getFile("checkstyle-extension.xml"));
    extractor.install(metadata, toDir);

    assertThat(new File(toDir, "sonar-checkstyle-plugin-2.8.jar")).exists();
    assertThat(new File(toDir, "checkstyle-extension.xml")).exists();
  }

  static File getFile(String filename) {
    return FileUtils.toFile(PluginInstallerTest.class.getResource("/org/sonar/core/plugins/" + filename));
  }
}
