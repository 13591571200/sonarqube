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
package org.sonar.core.review.workflow;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.core.review.workflow.condition.Condition;
import org.sonar.core.review.workflow.function.Function;
import org.sonar.core.review.workflow.review.*;
import org.sonar.core.review.workflow.screen.Screen;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class WorkflowEngine implements ServerComponent {

  private final Workflow workflow;
  private final ReviewStore store;
  private final Settings settings;

  public WorkflowEngine(Workflow workflow, ReviewStore store, Settings settings) {
    this.workflow = workflow;
    this.store = store;
    this.settings = settings;
  }

  /**
   * @return non-null list of screens per review#violationId
   */
  public ListMultimap<Long, Screen> listAvailableScreens(Review[] reviews, DefaultWorkflowContext context, boolean verifyConditions) {
    ListMultimap<Long, Screen> result = ArrayListMultimap.create();

    completeProjectSettings(context);

    for (Map.Entry<String, Screen> entry : workflow.getScreensByCommand().entrySet()) {
      String commandKey = entry.getKey();
      if (!verifyConditions || verifyConditionsQuietly(null, context, workflow.getContextConditions(commandKey))) {
        for (Review review : reviews) {
          if (!verifyConditions || verifyConditionsQuietly(review, context, workflow.getReviewConditions(commandKey))) {
            result.put(review.getViolationId(), entry.getValue());
          }
        }
      }
    }
    return result;
  }

  public List<Screen> listAvailableScreens(Review review, DefaultWorkflowContext context, boolean verifyConditions) {
    List<Screen> result = Lists.newArrayList();
    completeProjectSettings(context);
    for (Map.Entry<String, Screen> entry : workflow.getScreensByCommand().entrySet()) {
      String commandKey = entry.getKey();
      if (!verifyConditions || verifyConditionsQuietly(review, context, workflow.getConditions(commandKey))) {
        result.add(entry.getValue());

      }
    }
    return result;
  }

  /**
   * @return the optional (nullable) screen associated to the command
   */
  public Screen getScreen(String commandKey) {
    return workflow.getScreen(commandKey);
  }

  public void execute(String commandKey, MutableReview review, DefaultWorkflowContext context, Map<String, String> parameters) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(commandKey), "Missing command");
    Preconditions.checkArgument(workflow.hasCommand(commandKey), "Unknown command: " + commandKey);

    completeProjectSettings(context);

    verifyConditions(review, context, workflow.getConditions(commandKey));

    Map<String, String> immutableParameters = ImmutableMap.copyOf(parameters);

    // TODO execute functions are change state before functions that consume state (like "create-jira-issue")
    Review initialReview = ((DefaultReview) review).cloneImmutable();
    for (Function function : workflow.getFunctions(commandKey)) {
      function.doExecute(review, initialReview, context, immutableParameters);
    }

    // should it be extracted to a core function ?
    store.store(review);

    // TODO notify listeners
  }

  private boolean verifyConditionsQuietly(@Nullable Review review, WorkflowContext context, List<Condition> conditions) {
      for (Condition condition : conditions) {
        if (!condition.doVerify(review, context)) {
          return false;
        }
      }
      return true;
    }

  private void verifyConditions(@Nullable Review review, WorkflowContext context, List<Condition> conditions) {
    for (Condition condition : conditions) {
      if (!condition.doVerify(review, context)) {
        throw new IllegalStateException("Condition is not respected: " + condition.toString());
      }
    }
  }

  private void completeProjectSettings(DefaultWorkflowContext context) {
    Settings projectSettings = new Settings(settings);
    List<String> propertyKeys = workflow.getProjectPropertyKeys();
    store.completeProjectSettings(context.getProjectId(), projectSettings, propertyKeys);
    context.setSettings(projectSettings);
  }
}
