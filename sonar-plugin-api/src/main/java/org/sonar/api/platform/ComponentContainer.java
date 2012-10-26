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

import org.picocontainer.Characteristics;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.behaviors.OptInCaching;
import org.picocontainer.lifecycle.ReflectionLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.PropertyDefinitions;

import javax.annotation.Nullable;

/**
 * @since 2.12
 */
public class ComponentContainer implements BatchComponent, ServerComponent {

  ComponentContainer parent, child; // no need for multiple children
  MutablePicoContainer pico;
  PropertyDefinitions propertyDefinitions;

  /**
   * Create root container
   */
  public ComponentContainer() {
    this.parent = null;
    this.child = null;
    this.pico = createPicoContainer();
    propertyDefinitions = new PropertyDefinitions();
    addSingleton(propertyDefinitions);
    addSingleton(this);
  }

  /**
   * Create child container
   */
  private ComponentContainer(ComponentContainer parent) {
    this.parent = parent;
    this.pico = parent.pico.makeChildContainer();
    this.parent.child = this;
    this.propertyDefinitions = parent.propertyDefinitions;
    addSingleton(this);
  }

  /**
   * This method MUST NOT be renamed start() because the container is registered itself in picocontainer. Starting
   * a component twice is not authorized.
   */
  public final ComponentContainer startComponents() {
    pico.start();
    return this;
  }

  /**
   * This method MUST NOT be renamed stop() because the container is registered itself in picocontainer. Starting
   * a component twice is not authorized.
   */
  public final ComponentContainer stopComponents() {
    pico.stop();
    return this;
  }

  public final ComponentContainer addSingleton(Object component) {
    return addComponent(component, true);
  }

  /**
   * @param singleton return always the same instance if true, else a new instance
   *                  is returned each time the component is requested
   */
  public final ComponentContainer addComponent(Object component, boolean singleton) {
    pico.as(singleton ? Characteristics.CACHE : Characteristics.NO_CACHE).addComponent(getComponentKey(component), component);
    declareExtension(null, component);
    return this;
  }

  public final ComponentContainer addExtension(@Nullable PluginMetadata plugin, Object extension) {
    pico.as(Characteristics.CACHE).addComponent(getComponentKey(extension), extension);
    declareExtension(plugin, extension);
    return this;
  }

  public final void declareExtension(@Nullable PluginMetadata plugin, Object extension) {
    propertyDefinitions.addComponent(extension, plugin!=null ? plugin.getName() : "");
  }

  public final ComponentContainer addPicoAdapter(ComponentAdapter adapter) {
    pico.addAdapter(adapter);
    return this;
  }

  public final <T> T getComponentByType(Class<T> tClass) {
    return pico.getComponent(tClass);
  }

  public final Object getComponentByKey(Object key) {
    return pico.getComponent(key);
  }

  public final <T> java.util.List<T> getComponentsByType(java.lang.Class<T> tClass) {
    return pico.getComponents(tClass);
  }

  public final ComponentContainer removeChild() {
    if (child != null) {
      pico.removeChildContainer(child.pico);
      child = null;
    }
    return this;
  }

  public final ComponentContainer createChild() {
    return new ComponentContainer(this);
  }

  static MutablePicoContainer createPicoContainer() {
    ReflectionLifecycleStrategy lifecycleStrategy = new ReflectionLifecycleStrategy(new NullComponentMonitor(), "start", "stop", "dispose");
    return new DefaultPicoContainer(new OptInCaching(), lifecycleStrategy, null);
  }

  static Object getComponentKey(Object component) {
    if (component instanceof Class) {
      return component;
    }
    return new StringBuilder().append(component.getClass().getCanonicalName()).append("-").append(component.toString()).toString();
  }

  public ComponentContainer getParent() {
    return parent;
  }

  public ComponentContainer getChild() {
    return child;
  }
}
