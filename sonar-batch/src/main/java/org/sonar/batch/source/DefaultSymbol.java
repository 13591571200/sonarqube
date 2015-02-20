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

package org.sonar.batch.source;

import com.google.common.base.Objects;

import java.io.Serializable;

public class DefaultSymbol implements org.sonar.api.source.Symbol, Serializable {

  private final int declarationStartOffset;
  private final int declarationEndOffset;

  public DefaultSymbol(int startOffset, int endOffset) {
    this.declarationStartOffset = startOffset;
    this.declarationEndOffset = endOffset;
  }

  @Override
  public int getDeclarationStartOffset() {
    return declarationStartOffset;
  }

  @Override
  public int getDeclarationEndOffset() {
    return declarationEndOffset;
  }

  @Override
  public String getFullyQualifiedName() {
    return null;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper("Symbol")
      .add("offset", String.format("%d-%d", declarationStartOffset, declarationEndOffset))
      .toString();
  }
}
