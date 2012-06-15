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
package org.sonar.api.platform;

import org.junit.Test;
import org.sonar.api.Property;
import org.sonar.api.config.PropertyDefinitions;

import static junit.framework.Assert.assertTrue;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ComponentContainerTest {

  @Test
  public void shouldRegisterItself() {
    ComponentContainer container = new ComponentContainer();

    assertThat(container.getComponentByType(ComponentContainer.class)).isSameAs(container);
  }

  @Test
  public void testStartAndStop() {
    ComponentContainer container = new ComponentContainer();
    container.addSingleton(StartableComponent.class);
    container.startComponents();

    assertThat(container.getComponentByType(StartableComponent.class).started).isTrue();
    assertThat(container.getComponentByType(StartableComponent.class).stopped).isFalse();

    container.stopComponents();
    assertThat(container.getComponentByType(StartableComponent.class).stopped).isTrue();
  }

  @Test
  public void testChild() {
    ComponentContainer parent = new ComponentContainer();
    parent.startComponents();

    ComponentContainer child = parent.createChild();
    child.addSingleton(StartableComponent.class);
    child.startComponents();

    assertThat(child.getParent()).isSameAs(parent);
    assertThat(parent.getChild()).isSameAs(child);
    assertThat(child.getComponentByType(ComponentContainer.class)).isSameAs(child);
    assertThat(parent.getComponentByType(ComponentContainer.class)).isSameAs(parent);
    assertThat(child.getComponentByType(StartableComponent.class)).isNotNull();
    assertThat(parent.getComponentByType(StartableComponent.class)).isNull();

    parent.stopComponents();
  }

  @Test
  public void testRemoveChild() {
    ComponentContainer parent = new ComponentContainer();
    parent.startComponents();

    ComponentContainer child = parent.createChild();
    assertThat(parent.getChild()).isSameAs(child);

    parent.removeChild();
    assertThat(parent.getChild()).isNull();
  }

  @Test
  public void shouldForwardStartAndStopToDescendants() {
    ComponentContainer grandParent = new ComponentContainer();
    ComponentContainer parent = grandParent.createChild();
    ComponentContainer child = parent.createChild();
    child.addSingleton(StartableComponent.class);

    grandParent.startComponents();

    StartableComponent component = child.getComponentByType(StartableComponent.class);
    assertTrue(component.started);

    parent.stopComponents();
    assertTrue(component.stopped);
  }

  @Test
  public void shouldDeclareComponentProperties() {
    ComponentContainer container = new ComponentContainer();
    container.addSingleton(ComponentWithProperty.class);

    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    assertThat(propertyDefinitions.get("foo")).isNotNull();
    assertThat(propertyDefinitions.get("foo").getDefaultValue()).isEqualTo("bar");
  }

  @Test
  public void shouldDeclareExtensionWithoutAddingIt() {
    ComponentContainer container = new ComponentContainer();
    PluginMetadata plugin = mock(PluginMetadata.class);
    container.declareExtension(plugin, ComponentWithProperty.class);

    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    assertThat(propertyDefinitions.get("foo")).isNotNull();
    assertThat(container.getComponentByType(ComponentWithProperty.class)).isNull();
  }

  @Test
  public void shouldDeclareExtensionWhenAdding() {
    ComponentContainer container = new ComponentContainer();
    PluginMetadata plugin = mock(PluginMetadata.class);
    container.addExtension(plugin, ComponentWithProperty.class);

    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    assertThat(propertyDefinitions.get("foo")).isNotNull();
    assertThat(container.getComponentByType(ComponentWithProperty.class)).isNotNull();
  }

  public static class StartableComponent {
    public boolean started = false, stopped = false;

    public void start() {
      started = true;
    }

    public void stop() {
      stopped = true;
    }
  }

  @Property(key = "foo", defaultValue = "bar", name = "Foo")
  public static class ComponentWithProperty {

  }
}
