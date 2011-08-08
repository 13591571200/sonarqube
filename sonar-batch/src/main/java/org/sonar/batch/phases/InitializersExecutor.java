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
package org.sonar.batch.phases;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.MavenPluginExecutor;

import java.util.Collection;

public class InitializersExecutor {

  private static final Logger logger = LoggerFactory.getLogger(SensorsExecutor.class);

  private MavenPluginExecutor mavenExecutor;

  private ProjectDefinition projectDef;
  private Project project;
  private BatchExtensionDictionnary selector;

  public InitializersExecutor(BatchExtensionDictionnary selector, Project project, ProjectDefinition projectDef, MavenPluginExecutor mavenExecutor) {
    this.selector = selector;
    this.mavenExecutor = mavenExecutor;
    this.project = project;
    this.projectDef = projectDef;
  }

  public void execute() {
    Collection<Initializer> initializers = selector.select(Initializer.class, project, true);
    if (logger.isDebugEnabled()) {
      logger.debug("Initializers : {}", StringUtils.join(initializers, " -> "));
    }

    for (Initializer initializer : initializers) {
      executeMavenPlugin(initializer);

      TimeProfiler profiler = new TimeProfiler(logger).start("Initializer " + initializer);
      initializer.execute(project);
      profiler.stop();
    }
  }

  private void executeMavenPlugin(Initializer sensor) {
    if (sensor instanceof DependsUponMavenPlugin) {
      MavenPluginHandler handler = ((DependsUponMavenPlugin) sensor).getMavenPluginHandler(project);
      if (handler != null) {
        TimeProfiler profiler = new TimeProfiler(logger).start("Execute maven plugin " + handler.getArtifactId());
        mavenExecutor.execute(project, projectDef, handler);
        profiler.stop();
      }
    }
  }

}
