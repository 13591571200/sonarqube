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

package org.sonar.api.measures;

import org.sonar.api.resources.ResourceUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Formula used to compute an average for a given metric A, which is the result of the sum of measures of this metric (A) divided by another metric (B).
 * <p/>
 * For example: to compute the metric "complexity by file", the main metric (A) is "complexity" and the other metric (B) is "file".
 *
 * @since 3.0
 */
public class AverageFormula implements Formula {

  private Metric mainMetric;
  private Metric byMetric;

  /**
   * This method should be private but it kep package-protected because of AverageComplexityFormula.
   */
  AverageFormula(Metric mainMetric, Metric byMetric) {
    this.mainMetric = mainMetric;
    this.byMetric = byMetric;
  }

  /**
   * Creates a new {@link AverageFormula} class.
   *
   * @param main The metric on which average should be calculated (ex.: "complexity")
   * @param by   The metric used to divide the main metric to compute average (ex.: "file" for "complexity by file")
   */
  public static AverageFormula create(Metric main, Metric by) {
    return new AverageFormula(main, by);
  }

  /**
   * {@inheritDoc}
   */
  public List<Metric> dependsUponMetrics() {
    return Arrays.asList(mainMetric, byMetric);
  }

  /**
   * {@inheritDoc}
   */
  public Measure calculate(FormulaData data, FormulaContext context) {
    if (!shouldDecorateResource(data, context)) {
      return null;
    }

    Measure result;
    if (ResourceUtils.isFile(context.getResource())) {
      result = calculateForFile(data, context);
    } else {
      result = calculateOnChildren(data, context);
    }
    return result;
  }

  private Measure calculateOnChildren(FormulaData data, FormulaContext context) {
    Measure result = null;

    double totalByMeasure = 0;
    double totalMainMeasure = 0;
    boolean hasApplicableChildren = false;

    for (FormulaData childrenData : data.getChildren()) {
      Double childrenByMeasure = MeasureUtils.getValue(childrenData.getMeasure(byMetric), null);
      Double childrenMainMeasure = MeasureUtils.getValue(childrenData.getMeasure(mainMetric), null);
      if (childrenMainMeasure != null && childrenByMeasure != null && childrenByMeasure > 0.0) {
        totalByMeasure += childrenByMeasure;
        totalMainMeasure += childrenMainMeasure;
        hasApplicableChildren = true;
      }
    }
    if (hasApplicableChildren) {
      result = new Measure(context.getTargetMetric(), (totalMainMeasure / totalByMeasure));
    }
    return result;
  }

  private Measure calculateForFile(FormulaData data, FormulaContext context) {
    Measure result = null;

    Double byMeasure = MeasureUtils.getValue(data.getMeasure(byMetric), null);
    Double mainMeasure = MeasureUtils.getValue(data.getMeasure(mainMetric), null);
    if (mainMeasure != null && byMeasure != null && byMeasure > 0.0) {
      result = new Measure(context.getTargetMetric(), (mainMeasure / byMeasure));
    }

    return result;
  }

  private boolean shouldDecorateResource(FormulaData data, FormulaContext context) {
    return !MeasureUtils.hasValue(data.getMeasure(context.getTargetMetric()));
  }
}
