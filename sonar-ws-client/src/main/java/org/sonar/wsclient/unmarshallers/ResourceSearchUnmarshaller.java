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
package org.sonar.wsclient.unmarshallers;

import org.json.simple.JSONObject;
import org.sonar.wsclient.services.ResourceSearchResult;
import org.sonar.wsclient.services.WSUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 3.4
 */
public class ResourceSearchUnmarshaller extends AbstractUnmarshaller<ResourceSearchResult> {


  @Override
  protected ResourceSearchResult parse(Object json) {
    System.out.println("JSON: " + json);
    WSUtils utils = WSUtils.getINSTANCE();
    ResourceSearchResult result = new ResourceSearchResult();
    result.setPage(utils.getInteger(json, "page"));
    result.setPageSize(utils.getInteger(json, "page_size"));
    result.setTotal(utils.getInteger(json, "total"));

    List<ResourceSearchResult.Resource> resources = new ArrayList<ResourceSearchResult.Resource>();
    for (Object jsonResource : JsonUtils.getArray((JSONObject) json, "data")) {
      ResourceSearchResult.Resource resource = new ResourceSearchResult.Resource();
      resource.setKey(JsonUtils.getString((JSONObject) jsonResource, "key"));
      resource.setName(JsonUtils.getString((JSONObject) jsonResource, "nm"));
      resource.setQualifier(JsonUtils.getString((JSONObject) jsonResource, "q"));
      resources.add(resource);
    }
    result.setResources(resources);
    return result;
  }


}
