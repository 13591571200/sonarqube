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
package org.sonar.batch.bootstrap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

/**
 * TODO extends Server when removing the deprecated org.sonar.batch.ServerMetadata
 *
 * @since 3.4
 */
public class ServerClient implements BatchComponent {
  private BootstrapSettings settings;
  private HttpDownloader.BaseHttpDownloader downloader;

  public ServerClient(BootstrapSettings settings, EnvironmentInformation env) {
    this.settings = settings;
    this.downloader = new HttpDownloader.BaseHttpDownloader(settings.getProperties(), env.toString());
  }

  public String getURL() {
    return StringUtils.removeEnd(settings.getProperty("sonar.host.url", "http://localhost:9000"), "/");
  }

  public void download(String pathStartingWithSlash, File toFile) {
    try {
      InputSupplier<InputStream> inputSupplier = doRequest(pathStartingWithSlash);
      Files.copy(inputSupplier, toFile);
    } catch (HttpDownloader.HttpException he) {
      throw handleHttpException(he);
    } catch (Exception e) {
      throw new SonarException(String.format("Unable to download '%s' to: %s", pathStartingWithSlash, toFile), e);
    }
  }

  public String request(String pathStartingWithSlash) {
    InputSupplier<InputStream> inputSupplier = doRequest(pathStartingWithSlash);
    try {
      return IOUtils.toString(inputSupplier.getInput(), "UTF-8");
    } catch (HttpDownloader.HttpException he) {
      throw handleHttpException(he);
    } catch (Exception e) {
      throw new SonarException(String.format("Unable to request: %s", pathStartingWithSlash), e);
    }
  }

  private InputSupplier<InputStream> doRequest(String pathStartingWithSlash) {
    Preconditions.checkArgument(pathStartingWithSlash.startsWith("/"), "Path must start with slash /");

    URI uri = URI.create(getURL() + pathStartingWithSlash);
    String login = settings.getProperty(CoreProperties.LOGIN);

    try {
      InputSupplier<InputStream> inputSupplier;
      if (Strings.isNullOrEmpty(login)) {
        inputSupplier = downloader.newInputSupplier(uri);
      } else {
        inputSupplier = downloader.newInputSupplier(uri, login, settings.getProperty(CoreProperties.PASSWORD));
      }
      return inputSupplier;
    } catch (Exception e) {
      throw new SonarException(String.format("Unable to request: %s", uri), e);
    }
  }

  private SonarException handleHttpException(HttpDownloader.HttpException he) {
    if (he.getResponseCode() == 401) {
      throw new SonarException(String.format("Not authorized. Please check the properties %s and %s.", CoreProperties.LOGIN, CoreProperties.PASSWORD));
    }
    throw new SonarException(String.format("Fail to execute request [code=%s, url=%s]", he.getResponseCode(), he.getUri()), he);
  }

}
