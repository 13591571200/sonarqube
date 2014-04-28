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
package org.sonar.server.exceptions;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Arrays;

public class ServerException extends RuntimeException {
  private final int httpCode;

  private final String l10nKey;
  private final Object[] l10nParams;

  public ServerException(int httpCode) {
    this.httpCode = httpCode;
    this.l10nKey = null;
    this.l10nParams = null;
  }

  public ServerException(int httpCode, String message) {
    super(message);
    this.httpCode = httpCode;
    this.l10nKey = null;
    this.l10nParams = null;
  }

  public ServerException(int httpCode, @Nullable String message, @Nullable String l10nKey, @Nullable Object[] l10nParams) {
    super(message);
    this.httpCode = httpCode;
    this.l10nKey = l10nKey;
    this.l10nParams = l10nParams;
  }

  public int httpCode() {
    return httpCode;
  }

  @CheckForNull
  public String l10nKey() {
    return l10nKey;
  }

  @CheckForNull
  public Object[] l10nParams() {
    if (l10nParams == null) {
      return new Object[0];
    } else {
      return Arrays.copyOf(l10nParams, l10nParams.length);
    }
  }
}
