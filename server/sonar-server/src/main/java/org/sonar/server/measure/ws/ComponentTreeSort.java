/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.measure.ws;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.db.component.ComponentDtoWithSnapshotId;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricDtoFunctions;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.NAME_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.PATH_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.QUALIFIER_SORT;

class ComponentTreeSort {

  private ComponentTreeSort() {
    // static method only
  }

  static List<ComponentDtoWithSnapshotId> sortComponents(List<ComponentDtoWithSnapshotId> components, ComponentTreeWsRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
    List<String> sortParameters = wsRequest.getSort();
    if (sortParameters == null || sortParameters.isEmpty()) {
      return components;
    }
    boolean isAscending = wsRequest.getAsc();
    Map<String, Ordering<ComponentDtoWithSnapshotId>> orderingsBySortField = ImmutableMap.<String, Ordering<ComponentDtoWithSnapshotId>>builder()
      .put(NAME_SORT, componentNameOrdering(isAscending))
      .put(QUALIFIER_SORT, componentQualifierOrdering(isAscending))
      .put(PATH_SORT, componentPathOrdering(isAscending))
      .put(METRIC_SORT, metricOrdering(wsRequest, metrics, measuresByComponentUuidAndMetric))
      .build();

    String firstSortParameter = sortParameters.get(0);
    Ordering<ComponentDtoWithSnapshotId> primaryOrdering = orderingsBySortField.get(firstSortParameter);
    if (sortParameters.size() > 1) {
      for (int i = 1; i < sortParameters.size(); i++) {
        String secondarySortParameter = sortParameters.get(i);
        Ordering<ComponentDtoWithSnapshotId> secondaryOrdering = orderingsBySortField.get(secondarySortParameter);
        primaryOrdering = primaryOrdering.compound(secondaryOrdering);
      }
    }

    return primaryOrdering.immutableSortedCopy(components);
  }

  private static Ordering<ComponentDtoWithSnapshotId> componentNameOrdering(boolean isAscending) {
    return stringOrdering(isAscending, ComponentDtoWithSnapshotIdToName.INSTANCE);
  }

  private static Ordering<ComponentDtoWithSnapshotId> componentQualifierOrdering(boolean isAscending) {
    return stringOrdering(isAscending, ComponentDtoWithSnapshotIdToQualifier.INSTANCE);
  }

  private static Ordering<ComponentDtoWithSnapshotId> componentPathOrdering(boolean isAscending) {
    return stringOrdering(isAscending, ComponentDtoWithSnapshotIdToPath.INSTANCE);
  }

  private static Ordering<ComponentDtoWithSnapshotId> stringOrdering(boolean isAscending, Function<ComponentDtoWithSnapshotId, String> function) {
    Ordering<String> ordering = Ordering.from(CASE_INSENSITIVE_ORDER)
      .nullsLast();
    if (!isAscending) {
      ordering = ordering.reverse();
    }

    return ordering.onResultOf(function);
  }

  /**
   * Order by measure value, taking the metric direction into account
   */
  private static Ordering<ComponentDtoWithSnapshotId> metricOrdering(ComponentTreeWsRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
    if (wsRequest.getMetricSort() == null) {
      return componentNameOrdering(wsRequest.getAsc());
    }
    Map<String, MetricDto> metricsByKey = Maps.uniqueIndex(metrics, MetricDtoFunctions.toKey());
    MetricDto metric = metricsByKey.get(wsRequest.getMetricSort());

    boolean isAscending = wsRequest.getAsc();
    switch (ValueType.valueOf(metric.getValueType())) {
      case BOOL:
      case INT:
      case MILLISEC:
      case WORK_DUR:
      case FLOAT:
      case PERCENT:
      case RATING:
        return numericalMetricOrdering(isAscending, metric, measuresByComponentUuidAndMetric);
      case DATA:
      case DISTRIB:
      case LEVEL:
      case STRING:
        return stringOrdering(isAscending, new ComponentDtoWithSnapshotIdToTextualMeasureValue(metric, measuresByComponentUuidAndMetric));
      default:
        throw new IllegalStateException("Unrecognized metric value type: " + metric.getValueType());
    }
  }

  private static Ordering<ComponentDtoWithSnapshotId> numericalMetricOrdering(boolean isAscending, @Nullable MetricDto metric,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
    Ordering<Double> ordering = Ordering.natural()
      .nullsLast();

    if (!isAscending) {
      ordering = ordering.reverse();
    }

    return ordering.onResultOf(new ComponentDtoWithSnapshotIdToNumericalMeasureValue(metric, measuresByComponentUuidAndMetric));
  }

  private static class ComponentDtoWithSnapshotIdToNumericalMeasureValue implements Function<ComponentDtoWithSnapshotId, Double> {
    private final MetricDto metric;
    private final Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;

    private ComponentDtoWithSnapshotIdToNumericalMeasureValue(@Nullable MetricDto metric,
      Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
    }

    @Override
    public Double apply(@Nonnull ComponentDtoWithSnapshotId input) {
      MeasureDto measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      if (measure == null || measure.getValue() == null) {
        return null;
      }

      return measure.getValue();
    }
  }

  private static class ComponentDtoWithSnapshotIdToTextualMeasureValue implements Function<ComponentDtoWithSnapshotId, String> {
    private final MetricDto metric;
    private final Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;

    private ComponentDtoWithSnapshotIdToTextualMeasureValue(@Nullable MetricDto metric,
      Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
    }

    @Override
    public String apply(@Nonnull ComponentDtoWithSnapshotId input) {
      MeasureDto measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      if (measure == null || measure.getData() == null) {
        return null;
      }

      return measure.getData();
    }
  }

  private enum ComponentDtoWithSnapshotIdToName implements Function<ComponentDtoWithSnapshotId, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull ComponentDtoWithSnapshotId input) {
      return input.name();
    }
  }

  private enum ComponentDtoWithSnapshotIdToQualifier implements Function<ComponentDtoWithSnapshotId, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull ComponentDtoWithSnapshotId input) {
      return input.qualifier();
    }
  }

  private enum ComponentDtoWithSnapshotIdToPath implements Function<ComponentDtoWithSnapshotId, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull ComponentDtoWithSnapshotId input) {
      return input.path();
    }
  }
}
