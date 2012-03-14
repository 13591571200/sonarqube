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
package org.sonar.diff;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TextComparatorTest {

  @Test
  public void testEqualsWithoutWhitespace() {
    TextComparator cmp = TextComparator.DEFAULT;

    Text a = new Text("abc\nabc\na bc".getBytes());
    Text b = new Text("abc\nabc d\nab c".getBytes());

    assertThat("abc == abc", cmp.equals(a, 0, b, 0), is(true));
    assertThat("abc != abc d", cmp.equals(a, 1, b, 1), is(false));
    assertThat("a bc == ab c", cmp.equals(a, 2, b, 2), is(false));
    assertThat(cmp.hash(a, 0), equalTo(cmp.hash(b, 0)));
  }

  @Test
  public void testEqualsWithWhitespace() {
    TextComparator cmp = TextComparator.IGNORE_WHITESPACE;

    Text a = new Text("abc\nabc\na bc".getBytes());
    Text b = new Text("abc\nabc d\nab c".getBytes());

    assertThat("abc == abc", cmp.equals(a, 0, b, 0), is(true));
    assertThat("abc != abc d", cmp.equals(a, 1, b, 1), is(false));
    assertThat("a bc == ab c", cmp.equals(a, 2, b, 2), is(true));
    assertThat(cmp.hash(a, 0), equalTo(cmp.hash(b, 0)));
    assertThat(cmp.hash(a, 2), equalTo(cmp.hash(b, 2)));
  }

}
