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
package org.sonar.plugins.core.widgets;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.web.*;

@WidgetProperties({
  @WidgetProperty(key = "heightInPercents", type= WidgetPropertyType.INTEGER, defaultValue = "50", description = "Height in percents of width"),
  @WidgetProperty(key = "sizeMetric", type= WidgetPropertyType.METRIC, defaultValue = CoreMetrics.NCLOC_KEY, description = "Default metric for size"),
  @WidgetProperty(key = "colorMetric", type= WidgetPropertyType.METRIC, defaultValue = CoreMetrics.VIOLATIONS_DENSITY_KEY, description = "Default metric for color")
})
public class TreemapWidget extends AbstractRubyTemplate implements RubyRailsWidget {
  public String getId() {
    // do not use the id "treemap" to avoid conflict with the same CSS class
    return "treemap-widget";
  }

  public String getTitle() {
    return "Treemap of components";
  }

  @Override
  protected String getTemplatePath() {
    return "/org/sonar/plugins/core/widgets/treemap.html.erb";
  }
}