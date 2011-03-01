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
package org.sonar.api.measures;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class MetricTest {

  @Test
  public void shouldCreateMetric() {
    Metric metric = new Metric.Builder("foo", "Foo", Metric.ValueType.INT)
        .setDomain("my domain")
        .create();

    assertThat(metric.getKey(), is("foo"));
    assertThat(metric.getName(), is("Foo"));
    assertThat(metric.getDomain(), is("my domain"));
  }

  @Test
  public void shouldCreateMetricWithDefaultValues() {
    Metric metric = new Metric.Builder("foo", "Foo", Metric.ValueType.INT)
        .create();

    assertThat(metric.getBestValue(), nullValue());
    assertThat(metric.getDescription(), nullValue());
    assertThat(metric.getWorstValue(), nullValue());
    assertThat(metric.getDirection(), is(Metric.DIRECTION_NONE));
    assertThat(metric.getEnabled(), is(true));
    assertThat(metric.getOrigin(), is(Metric.Origin.JAV));
    assertThat(metric.getFormula(), nullValue());
    assertThat(metric.getId(), nullValue());
    assertThat(metric.getUserManaged(), is(false));
    assertThat(metric.isHidden(), is(false));
    assertThat(metric.isOptimizedBestValue(), is(false));
  }
}
