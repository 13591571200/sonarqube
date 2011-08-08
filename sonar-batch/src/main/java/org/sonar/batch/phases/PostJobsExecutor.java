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
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.batch.MavenPluginExecutor;

import java.util.Collection;
import java.util.List;

public class PostJobsExecutor implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(PostJobsExecutor.class);

  private MavenPluginExecutor mavenExecutor;
  private ProjectDefinition projectDefinition;
  private Project project;
  private BatchExtensionDictionnary selector;

  public PostJobsExecutor(BatchExtensionDictionnary selector, Project project, ProjectDefinition projectDefinition, MavenPluginExecutor mavenExecutor) {
    this.selector = selector;
    this.mavenExecutor = mavenExecutor;
    this.project = project;
    this.projectDefinition = projectDefinition;
  }

  public void execute(SensorContext context) {
    Collection<PostJob> postJobs = selector.select(PostJob.class, project, true);
    execute(context, postJobs);
  }

  void execute(SensorContext context, Collection<PostJob> postJobs) {
    logPostJobs(postJobs);

    for (PostJob postJob : postJobs) {
      LOG.info("Executing post-job {}", postJob.getClass());
      executeMavenPlugin(postJob);
      postJob.executeOn(project, context);
    }
  }

  private void logPostJobs(Collection<PostJob> postJobs) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Post-jobs : {}", StringUtils.join(postJobs, " -> "));
    }
  }

  private void executeMavenPlugin(PostJob job) {
    if (job instanceof DependsUponMavenPlugin) {
      MavenPluginHandler handler = ((DependsUponMavenPlugin) job).getMavenPluginHandler(project);
      if (handler != null) {
        mavenExecutor.execute(project, projectDefinition, handler);
      }
    }
  }
}
