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
package org.sonar.core.issue;

import org.junit.Test;
import org.sonar.api.issue.internal.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuesBySeverityTest {

  IssuesBySeverity sut;

  @Test
  public void add_issue() {
    sut = new IssuesBySeverity();

    sut.add(new DefaultIssue().setSeverity("MINOR"));

    assertThat(sut.isEmpty()).isFalse();
    assertThat(sut.size()).isEqualTo(1);
  }

  @Test
  public void get_issues_by_severity() {
    sut = new IssuesBySeverity();

    sut.add(new DefaultIssue().setSeverity("MINOR"));
    sut.add(new DefaultIssue().setSeverity("MINOR"));
    sut.add(new DefaultIssue().setSeverity("MAJOR"));

    assertThat(sut.issues("MINOR")).isEqualTo(2);
    assertThat(sut.issues("MAJOR")).isEqualTo(1);
  }

  @Test
  public void get_zero_issues_on_empty_severity() {
    sut = new IssuesBySeverity();

    sut.add(new DefaultIssue().setSeverity("MAJOR"));

    assertThat(sut.issues("MINOR")).isEqualTo(0);
  }

  @Test
  public void is_empty() throws Exception {
    sut = new IssuesBySeverity();

    assertThat(sut.isEmpty()).isTrue();
  }
}
