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
package org.sonar.api.config;

import org.sonar.api.ServerExtension;

import javax.annotation.Nullable;

/**
 * Observe changes of global properties done from web application. It does not support :
 * <ul>
 * <li>changes done by end-users from the page "Project Settings"</li>
 * <li>changes done programmatically on the component org.sonar.api.config.Settings</li>
 * </ul>
 *
 * @since 2.15
 */
public abstract class GlobalPropertyChangeHandler implements ServerExtension {

  public static final class PropertyChange {
    private String key;
    private String newValue;

    private PropertyChange(String key, @Nullable String newValue) {
      this.key = key;
      this.newValue = newValue;
    }

    public static PropertyChange create(String key, @Nullable String newValue) {
      return new PropertyChange(key, newValue);
    }

    public String getKey() {
      return key;
    }

    public String getNewValue() {
      return newValue;
    }

    @Override
    public String toString() {
      return String.format("[key=%s, newValue=%s]", key, newValue);
    }
  }

  /**
   * This method gets called when a property is changed.
   */
  public abstract void onChange(PropertyChange change);
}
