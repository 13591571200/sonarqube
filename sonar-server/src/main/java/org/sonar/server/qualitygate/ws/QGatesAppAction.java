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
package org.sonar.server.qualitygate.ws;

import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.qualitygate.QualityGates;

import java.util.Locale;

public class QGatesAppAction implements RequestHandler {

  private static final String[] MESSAGE_KEYS = {
      "add_verb",
      "alerts.error_tooltip",
      "alerts.notes.error",
      "alerts.notes.ok",
      "alerts.notes.warn",
      "alerts.select_metric",
      "alerts.warning_tooltip",
      "are_you_sure",
      "cancel",
      "copy",
      "create",
      "default",
      "delete",
      "deprecated",
      "more",
      "name",
      "quality_gates.add_condition",
      "quality_gates.conditions",
      "quality_gates.copy",
      "quality_gates.create",
      "quality_gates.delete",
      "quality_gates.health_icons",
      "quality_gates.introduction",
      "quality_gates.no_conditions",
      "quality_gates.delete_condition",
      "quality_gates.noQualityGates",
      "quality_gates.operator.LT",
      "quality_gates.operator.GT",
      "quality_gates.operator.EQ",
      "quality_gates.operator.NE",
      "quality_gates.page",
      "quality_gates.projects",
      "quality_gates.projects.all",
      "quality_gates.projects.deselect_hint",
      "quality_gates.projects.noResults",
      "quality_gates.projects.select_hint",
      "quality_gates.projects.with",
      "quality_gates.projects.without",
      "quality_gates.projects_for_default",
      "quality_gates.projects_for_default.edit",
      "quality_gates.rename",
      "quality_gates.delete.confirm.message",
      "quality_gates.delete.confirm.default",
      "quality_gates.delete_condition.confirm.message",
      "quality_gates.project_period",
      "rename",
      "save",
      "set_as_default",
      "unset_as_default",
      "update_verb",
      "value",
      "work_duration.x_days",
      "work_duration.x_hours",
      "work_duration.x_minutes",
  };

  private final QualityGates qualityGates;

  private final Periods periods;

  private final I18n i18n;

  public QGatesAppAction(QualityGates qualityGates, Periods periods, I18n i18n) {
    this.qualityGates = qualityGates;
    this.periods = periods;
    this.i18n = i18n;
  }

  void define(WebService.NewController controller) {
    controller.createAction("app")
      .setInternal(true)
      .setDescription("Get initialization items for the admin UI. For internal use")
      .setSince("4.3")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    JsonWriter writer = response.newJsonWriter().beginObject();
    addPermissions(writer);
    addPeriods(writer);
    addMetrics(writer);
    addMessages(writer);
    writer.endObject().close();
  }

  private void addPermissions(JsonWriter writer) {
    writer.prop("edit", qualityGates.currentUserHasWritePermission());
  }

  private void addPeriods(JsonWriter writer) {
    writer.name("periods").beginArray();
    for (int i=0; i < 3; i ++) {
      writer.beginObject().prop("key", i + 1).prop("text", periods.label(i + 1)).endObject();
    }
    addProjectPeriod(4, writer);
    addProjectPeriod(5, writer);
    writer.endArray();
  }

  private void addProjectPeriod(int periodIndex, JsonWriter writer) {
    writer.beginObject().prop("key", periodIndex).prop("text",
      i18n.message(Locale.getDefault(), "quality_gates.project_period", "Period " + periodIndex, periodIndex)
    ).endObject();
  }

  private void addMessages(JsonWriter writer) {
    writer.name("messages").beginObject();
    for (String message: MESSAGE_KEYS) {
      writer.prop(message, i18n.message(Locale.getDefault(), message, message));
    }
    writer.endObject();
  }

  private void addMetrics(JsonWriter writer) {
    writer.name("metrics").beginArray();
    for (Metric metric: qualityGates.gateMetrics()) {
      writer.beginObject()
        .prop("id", metric.getId())
        .prop("key", metric.getKey())
        .prop("name", metric.getName())
        .prop("type", metric.getType().toString())
        .prop("domain", metric.getDomain())
        .prop("hidden", BooleanUtils.isNotFalse(metric.isHidden()))
      .endObject();
    }
    writer.endArray();
  }


}
