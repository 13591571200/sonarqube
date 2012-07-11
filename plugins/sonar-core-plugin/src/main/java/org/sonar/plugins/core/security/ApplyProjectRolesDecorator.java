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
package org.sonar.plugins.core.security;

import com.google.common.collect.ImmutableSet;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.ResourcePermissions;

import java.util.Set;

public class ApplyProjectRolesDecorator implements Decorator {

  private final ResourcePermissions resourcePermissions;
  private final Set<String> QUALIFIERS = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.SUBVIEW);

  public ApplyProjectRolesDecorator(ResourcePermissions resourcePermissions) {
    this.resourcePermissions = resourcePermissions;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource)) {
      LoggerFactory.getLogger(ApplyProjectRolesDecorator.class).info("Grant default permissions to {}", resource.getKey());
      resourcePermissions.grantDefaultRoles(resource);
    }
  }

  private boolean shouldDecorateResource(Resource resource) {
    return resource.getId() != null && QUALIFIERS.contains(resource.getQualifier()) && !resourcePermissions.hasRoles(resource);
  }

}
