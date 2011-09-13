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
package org.sonar.server.plugins;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Test;
import org.picocontainer.containers.TransientPicoContainer;
import org.sonar.api.*;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.batch.Batch;
import org.sonar.core.plugins.DefaultPluginMetadata;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServerPluginRepositoryTest {

  private DefaultServerPluginRepository repository;

  @After
  public void stop() {
    if (repository != null) {
      repository.stop();
    }
  }

  @Test
  public void testStart() {
    PluginDeployer deployer = mock(PluginDeployer.class);
    File pluginFile = FileUtils.toFile(getClass().getResource("/org/sonar/server/plugins/DefaultServerPluginRepositoryTest/sonar-artifact-size-plugin-0.2.jar"));
    PluginMetadata plugin = DefaultPluginMetadata.create(pluginFile)
        .setKey("artifactsize")
        .setMainClass("org.sonar.plugins.artifactsize.ArtifactSizePlugin")
        .addDeployedFile(pluginFile);
    when(deployer.getMetadata()).thenReturn(Arrays.asList(plugin));

    repository = new DefaultServerPluginRepository(deployer);
    repository.start();

    assertThat(repository.getPlugins().size(), Is.is(1));
    assertThat(repository.getPlugin("artifactsize"), not(nullValue()));
    assertThat(repository.getClassloader("artifactsize"), not(nullValue()));
    assertThat(repository.getClass("artifactsize", "org.sonar.plugins.artifactsize.ArtifactSizeMetrics"), not(nullValue()));
    assertThat(repository.getClass("artifactsize", "org.Unknown"), nullValue());
    assertThat(repository.getClass("other", "org.sonar.plugins.artifactsize.ArtifactSizeMetrics"), nullValue());
  }

  @Test
  public void shouldRegisterServerExtensions() {
    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(mock(PluginDeployer.class));

    TransientPicoContainer container = new TransientPicoContainer();
    repository.registerExtensions(container, Arrays.<Plugin>asList(new FakePlugin(Arrays.<Class>asList(FakeBatchExtension.class, FakeServerExtension.class))));

    assertThat(container.getComponents(Extension.class).size(), is(1));
    assertThat(container.getComponents(FakeServerExtension.class).size(), is(1));
    assertThat(container.getComponents(FakeBatchExtension.class).size(), is(0));
  }

  @Test
  public void shouldInvokeServerExtensionProviders() {
    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(mock(PluginDeployer.class));

    TransientPicoContainer container = new TransientPicoContainer();
    repository.registerExtensions(container, Arrays.<Plugin>asList(new FakePlugin(Arrays.<Class>asList(FakeExtensionProvider.class))));

    assertThat(container.getComponents(Extension.class).size(), is(2));// provider + FakeServerExtension
    assertThat(container.getComponents(FakeServerExtension.class).size(), is(1));
    assertThat(container.getComponents(FakeBatchExtension.class).size(), is(0));
  }

  @Test
  public void shouldNotSupportProvidersOfProviders() {
    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(mock(PluginDeployer.class));

    TransientPicoContainer container = new TransientPicoContainer();
    repository.registerExtensions(container, Arrays.<Plugin>asList(new FakePlugin(Arrays.<Class>asList(SuperExtensionProvider.class))));

    assertThat(container.getComponents(FakeBatchExtension.class).size(), is(0));
    assertThat(container.getComponents(FakeServerExtension.class).size(), is(0));
  }

  @Test
  public void shouldDisablePlugin() {
    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(mock(PluginDeployer.class));
    repository.disable("checkstyle");

    assertTrue(repository.isDisabled("checkstyle"));
    assertFalse(repository.isDisabled("sqale"));
  }

  @Test
  public void shouldDisableDependentPlugins() {
    PluginDeployer deployer = mock(PluginDeployer.class);
    List<PluginMetadata> metadata = Arrays.asList(
        newMetadata("checkstyle", null),
        newMetadata("checkstyle-extensions", "checkstyle"),
        newMetadata("sqale", null)
    );
    when(deployer.getMetadata()).thenReturn(metadata);
    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(deployer);

    repository.disable("checkstyle");

    assertTrue(repository.isDisabled("checkstyle"));
    assertTrue(repository.isDisabled("checkstyle-extensions"));
    assertFalse(repository.isDisabled("sqale"));
  }

  @Test
  public void shouldNotDisableBasePlugin() {
    PluginDeployer deployer = mock(PluginDeployer.class);
    List<PluginMetadata> metadata = Arrays.asList(
        newMetadata("checkstyle", null),
        newMetadata("checkstyle-extensions", "checkstyle"),
        newMetadata("sqale", null)
    );
    when(deployer.getMetadata()).thenReturn(metadata);
    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(deployer);

    repository.disable("checkstyle-extensions");

    assertFalse(repository.isDisabled("checkstyle"));
    assertTrue(repository.isDisabled("checkstyle-extensions"));
  }

  private PluginMetadata newMetadata(String pluginKey, String basePluginKey) {
    PluginMetadata plugin = mock(PluginMetadata.class);
    when(plugin.getKey()).thenReturn(pluginKey);
    when(plugin.getBasePlugin()).thenReturn(basePluginKey);
    return plugin;
  }

  public static class FakePlugin extends SonarPlugin {
    private List<Class> extensions;

    public FakePlugin(List<Class> extensions) {
      this.extensions = extensions;
    }

    public List getExtensions() {
      return extensions;
    }
  }

  public static class FakeBatchExtension implements BatchExtension {

  }

  public static class FakeServerExtension implements ServerExtension {

  }

  public static class FakeExtensionProvider extends ExtensionProvider implements ServerExtension {

    @Override
    public Object provide() {
      return Arrays.asList(FakeBatchExtension.class, FakeServerExtension.class);
    }
  }

  public static class SuperExtensionProvider extends ExtensionProvider implements ServerExtension {

    @Override
    public Object provide() {
      return FakeExtensionProvider.class;
    }
  }
}
