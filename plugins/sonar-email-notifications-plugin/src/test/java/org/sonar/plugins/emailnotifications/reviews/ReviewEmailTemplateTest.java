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
package org.sonar.plugins.emailnotifications.reviews;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.User;
import org.sonar.api.notifications.Notification;
import org.sonar.api.security.UserFinder;
import org.sonar.plugins.emailnotifications.EmailConfiguration;
import org.sonar.plugins.emailnotifications.api.EmailMessage;

public class ReviewEmailTemplateTest {

  private ReviewEmailTemplate template;

  @Before
  public void setUp() {
    EmailConfiguration configuration = mock(EmailConfiguration.class);
    when(configuration.getServerBaseURL()).thenReturn("http://nemo.sonarsource.org");
    UserFinder userFinder = mock(UserFinder.class);
    when(userFinder.findByLogin(eq("freddy.mallet"))).thenReturn(new User().setName("Freddy Mallet"));
    when(userFinder.findByLogin(eq("simon.brandhof"))).thenReturn(new User().setName("Simon Brandhof"));
    when(userFinder.findByLogin(eq("evgeny.mandrikov"))).thenReturn(new User().setName("Evgeny Mandrikov"));
    template = new ReviewEmailTemplate(configuration, userFinder);
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Comment:
   *   This is my first comment
   * 
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatCommentAdded() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.comment", null)
        .setFieldValue("new.comment", "This is my first comment");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), is("Freddy Mallet"));
    assertThat(message.getMessage(), is("Comment:\n  This is my first comment\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Comment:
   *   This is another comment
   * Was:
   *   This is my first comment
   * 
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatCommentEdited() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.comment", "This is my first comment")
        .setFieldValue("new.comment", "This is another comment");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), is("Freddy Mallet"));
    assertThat(message.getMessage(), is("Comment:\n  This is another comment\nWas:\n  This is my first comment\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Comment deleted, was:
   *   This is deleted comment
   *   
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatCommentDeleted() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("old.comment", "This is deleted comment")
        .setFieldValue("new.comment", null)
        .setFieldValue("author", "freddy.mallet");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), is("Freddy Mallet"));
    assertThat(message.getMessage(), is("Comment deleted, was:\n  This is deleted comment\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Assignee: Evgeny Mandrikov
   * 
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatAssigneed() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.assignee", null)
        .setFieldValue("new.assignee", "evgeny.mandrikov");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), is("Freddy Mallet"));
    assertThat(message.getMessage(), is("Assignee: Evgeny Mandrikov\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Assignee: Simon Brandhof (was Evgeny Mandrikov)
   * 
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatAssigneedToAnotherPerson() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.assignee", "evgeny.mandrikov")
        .setFieldValue("new.assignee", "simon.brandhof");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), is("Freddy Mallet"));
    assertThat(message.getMessage(), is("Assignee: Simon Brandhof (was Evgeny Mandrikov)\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Freddy Mallet
   * 
   * Assignee: (was Simon Brandhof)
   * 
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatUnassigned() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.assignee", "simon.brandhof")
        .setFieldValue("new.assignee", null);
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), is("Freddy Mallet"));
    assertThat(message.getMessage(), is("Assignee:  (was Simon Brandhof)\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Sonar
   * 
   * Status: CLOSED (was OPEN)
   * 
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatClosed() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("old.status", "OPEN")
        .setFieldValue("new.status", "CLOSED");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), nullValue());
    assertThat(message.getMessage(), is("Status: CLOSED (was OPEN)\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Simon Brandhof
   * 
   * Status: REOPENED (was RESOLVED)
   * Resolution: (was FIXED)
   * 
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatReopened() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("old.resolution", "FIXED")
        .setFieldValue("new.resolution", null)
        .setFieldValue("old.status", "RESOLVED")
        .setFieldValue("new.status", "REOPENED");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), nullValue());
    assertThat(message.getMessage(), is("Status: REOPENED (was RESOLVED)\nResolution:  (was FIXED)\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Simon Brandhof
   * 
   * Status: RESOLVED (was OPEN)
   * Resolution: FIXED
   * 
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatResolvedAsFixed() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("author", "simon.brandhof")
        .setFieldValue("old.status", "OPEN")
        .setFieldValue("old.resolution", null)
        .setFieldValue("new.status", "RESOLVED")
        .setFieldValue("new.resolution", "FIXED");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), is("Simon Brandhof"));
    assertThat(message.getMessage(), is("Status: RESOLVED (was OPEN)\nResolution: FIXED\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  /**
   * <pre>
   * Subject: Review #1
   * From: Simon Brandhof
   * 
   * Status: RESOLVED (was REOPENED)
   * Resolution: FALSE-POSITIVE
   * Comment:
   *   Because!
   * 
   * --
   * See it in Sonar: http://nemo.sonarsource.org/review/view/1
   * </pre>
   */
  @Test
  public void shouldFormatResolvedAsFalsePositive() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("reviewId", "1")
        .setFieldValue("author", "freddy.mallet")
        .setFieldValue("old.status", "REOPENED")
        .setFieldValue("old.resolution", null)
        .setFieldValue("new.status", "RESOLVED")
        .setFieldValue("new.resolution", "FALSE-POSITIVE")
        .setFieldValue("new.comment", "Because!");
    EmailMessage message = template.format(notification);
    assertThat(message.getMessageId(), is("review/1"));
    assertThat(message.getSubject(), is("Review #1"));
    assertThat(message.getFrom(), is("Freddy Mallet"));
    assertThat(message.getMessage(), is("Status: RESOLVED (was REOPENED)\nResolution: FALSE-POSITIVE\nComment:\n  Because!\n\n--\nSee it in Sonar: http://nemo.sonarsource.org/review/view/1\n"));
  }

  @Test
  public void shouldNotFormat() {
    Notification notification = new Notification("other");
    EmailMessage message = template.format(notification);
    assertThat(message, nullValue());
  }

  @Test
  public void shouldReturnFullNameOrLogin() {
    assertThat(template.getUserFullName("freddy.mallet"), is("Freddy Mallet"));
    assertThat(template.getUserFullName("deleted"), is("deleted"));
  }

}
