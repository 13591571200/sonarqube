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
package org.sonar.wsclient.services;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ReviewCreateQueryTest extends QueryTestCase {

  @Test
  public void testCreateReview() {
    ReviewCreateQuery query = new ReviewCreateQuery()
        .setViolationId(13L)
        .setComment("Hello World!");
    assertThat(query.getUrl(), is("/api/reviews?violation_id=13&"));
    assertThat(query.getBody(), is("Hello World!"));
    assertThat(query.getModelClass().getName(), is(Review.class.getName()));
  }

  @Test
  public void testCreateAssignedReview() {
    ReviewCreateQuery query = new ReviewCreateQuery()
        .setViolationId(13L)
        .setAssignee("fabrice")
        .setComment("Hello World!");
    assertThat(query.getUrl(), is("/api/reviews?violation_id=13&assignee=fabrice&"));
    assertThat(query.getBody(), is("Hello World!"));
  }

  @Test
  public void testCreateResolvedReview() {
    ReviewCreateQuery query = new ReviewCreateQuery()
        .setViolationId(13L)
        .setStatus("RESOLVED")
        .setResolution("FALSE-POSITIVE")
        .setComment("Hello World!");
    assertThat(query.getUrl(), is("/api/reviews?violation_id=13&status=RESOLVED&resolution=FALSE-POSITIVE&"));
    assertThat(query.getBody(), is("Hello World!"));
  }

}
