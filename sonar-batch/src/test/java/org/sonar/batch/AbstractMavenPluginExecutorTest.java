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
package org.sonar.batch;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class AbstractMavenPluginExecutorTest {

  @Test
  public void pluginVersionIsOptional() {
    assertThat(AbstractMavenPluginExecutor.getGoal("group", "artifact", null, "goal"), is("group:artifact::goal"));
  }

  /**
   * The maven plugin sometimes changes the project structure (for example mvn build-helper:add-source). These changes
   * must be applied to the internal structure.
   */
  @Test
  public void shouldUpdateProjectAfterExecution() {
    AbstractMavenPluginExecutor executor = new AbstractMavenPluginExecutor() {
      @Override
      public void concreteExecute(MavenProject pom, String goal) throws Exception {
        pom.addCompileSourceRoot("src/java");
      }
    };
    MavenProject pom = new MavenProject();
    pom.setFile(new File("target/AbstractMavenPluginExecutorTest/pom.xml"));
    pom.getBuild().setDirectory("target");
    Project foo = new Project("foo");
    foo.setPom(pom);
    ProjectDefinition definition = ProjectDefinition.create();
    executor.execute(foo, definition, new AddSourceMavenPluginHandler());

    assertThat(definition.getSourceDirs(), hasItem("src/java"));
  }

  static class AddSourceMavenPluginHandler implements MavenPluginHandler {
    public String getGroupId() {
      return "fake";
    }

    public String getArtifactId() {
      return "fake";
    }

    public String getVersion() {
      return "2.2";
    }

    public boolean isFixedVersion() {
      return false;
    }

    public String[] getGoals() {
      return new String[] { "fake" };
    }

    public void configure(Project project, MavenPlugin plugin) {
    }
  }

}
