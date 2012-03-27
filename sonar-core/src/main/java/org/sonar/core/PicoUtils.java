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
package org.sonar.core;

import com.google.common.base.Throwables;
import org.picocontainer.PicoLifecycleException;

public final class PicoUtils {

  private PicoUtils() {
  }

  public static Throwable sanitize(Throwable t) {
    Throwable result = t;
    Throwable cause = t.getCause();
    if (t instanceof PicoLifecycleException && cause != null) {
      if ("wrapper".equals(cause.getMessage()) && cause.getCause() != null) {
        result = cause.getCause();
      } else {
        result = cause;
      }

    }
    return result;
  }

  public static void propagateStartupException(Throwable t) {
    throw Throwables.propagate(sanitize(t));
  }
}
