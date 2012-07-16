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
package org.sonar.server.notifications;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.notification.NotificationQueueElement;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @since 2.10
 */
@Properties({
  @Property(
    key = NotificationService.PROPERTY_DELAY,
    defaultValue = "60",
    name = "Delay of notifications, in seconds",
    project = false,
    global = false)
})
public class NotificationService implements ServerComponent {
  public static final String PROPERTY_DELAY = "sonar.notifications.delay";

  private static final TimeProfiler TIME_PROFILER = new TimeProfiler(Logs.INFO).setLevelToDebug();

  private final long delayInSeconds;
  private final DefaultNotificationManager manager;
  private final NotificationChannel[] channels;
  private final NotificationDispatcher[] dispatchers;

  private ScheduledExecutorService executorService;
  private boolean stopping = false;

  /**
   * Default constructor when no channels.
   */
  public NotificationService(Settings settings, DefaultNotificationManager manager, NotificationDispatcher[] dispatchers) {
    this(settings, manager, dispatchers, new NotificationChannel[0]);
    Logs.INFO.warn("There is no channels - all notifications will be ignored!");
  }

  public NotificationService(Settings settings, DefaultNotificationManager manager, NotificationDispatcher[] dispatchers, NotificationChannel[] channels) {
    delayInSeconds = settings.getLong(PROPERTY_DELAY);
    this.manager = manager;
    this.channels = channels;
    this.dispatchers = dispatchers;
  }

  public void start() {
    executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleWithFixedDelay(new Runnable() {
      public void run() {
        processQueue();
      }
    }, 0, delayInSeconds, TimeUnit.SECONDS);
    Logs.INFO.info("Notification service started (delay {} sec.)", delayInSeconds);
  }

  public void stop() {
    try {
      stopping = true;
      executorService.shutdown();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Logs.INFO.error("Error during stop of notification service", e);
    }
    Logs.INFO.info("Notification service stopped");
  }

  @VisibleForTesting
  synchronized void processQueue() {
    TIME_PROFILER.start("Processing notifications queue");

    NotificationQueueElement queueElement = manager.getFromQueue();
    while (queueElement != null) {
      deliver(queueElement.getNotification());
      if (stopping) {
        break;
      }
      queueElement = manager.getFromQueue();
    }

    TIME_PROFILER.stop();
  }

  private void deliver(Notification notification) {
    Logs.INFO.debug("Delivering notification " + notification);
    SetMultimap<String, NotificationChannel> recipients = HashMultimap.create();
    for (NotificationChannel channel : channels) {
      for (NotificationDispatcher dispatcher : dispatchers) {
        final Set<String> possibleRecipients = Sets.newHashSet();
        NotificationDispatcher.Context context = new NotificationDispatcher.Context() {
          public void addUser(String username) {
            if (username != null) {
              possibleRecipients.add(username);
            }
          }
        };
        try {
          dispatcher.dispatch(notification, context);
        } catch (Exception e) { // catch all exceptions in order to dispatch using other dispatchers
          Logs.INFO.warn("Unable to dispatch notification " + notification + " using " + dispatcher, e);
        }
        for (String username : possibleRecipients) {
          if (manager.isEnabled(username, channel.getKey(), dispatcher.getKey())) {
            recipients.put(username, channel);
          }
        }
      }
    }
    dispatch(notification, recipients);
  }

  private void dispatch(Notification notification, SetMultimap<String, NotificationChannel> recipients) {
    for (Map.Entry<String, Collection<NotificationChannel>> entry : recipients.asMap().entrySet()) {
      String username = entry.getKey();
      Collection<NotificationChannel> userChannels = entry.getValue();
      Logs.INFO.debug("For user {} via {}", username, userChannels);
      for (NotificationChannel channel : userChannels) {
        try {
          channel.deliver(notification, username);
        } catch (Exception e) { // catch all exceptions in order to deliver via other channels
          Logs.INFO.warn("Unable to deliver notification " + notification + " for user " + username + " via " + channel, e);
        }
      }
    }
  }

  public List<NotificationDispatcher> getDispatchers() {
    return Arrays.asList(dispatchers);
  }

  public List<NotificationChannel> getChannels() {
    return Arrays.asList(channels);
  }

}
