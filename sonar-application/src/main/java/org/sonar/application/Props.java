/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Properties;

class Props {

  private final Properties props;

  Props(Properties props) {
    this.props = props;
  }

  String of(String key) {
    return props.getProperty(key);
  }

  String of(String key, @Nullable String defaultValue) {
    String s = of(key);
    return s == null ? defaultValue : s;
  }

  boolean booleanOf(String key) {
    String s = of(key);
    return s != null && Boolean.parseBoolean(s);
  }

  boolean booleanOf(String key, boolean defaultValue) {
    String s = of(key);
    return s != null ? Boolean.parseBoolean(s) : defaultValue;
  }

  Integer intOf(String key) {
    String s = of(key);
    if (s != null && !"".equals(s)) {
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Value of property " + key + " is not an integer: " + s, e);
      }
    }
    return null;
  }

  int intOf(String key, int defaultValue) {
    Integer i = intOf(key);
    return i == null ? defaultValue : i;
  }

  static Props create(Env env) {
    File propsFile = env.file("conf/sonar.properties");
    Properties p = new Properties();
    FileReader reader = null;
    try {
      reader = new FileReader(propsFile);

      // order is important : the last override the first
      p.load(reader);
      p.putAll(System.getenv());
      p.putAll(System.getProperties());

      p = ConfigurationUtils.interpolateEnvVariables(p);
      p = decrypt(p);

      // Set all properties as system properties to pass them to PlatformServletContextListener
      System.setProperties(p);

      return new Props(p);

    } catch (Exception e) {
      throw new IllegalStateException("File does not exist or can't be open: " + propsFile, e);

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  static Properties decrypt(Properties properties) {
    Encryption encryption = new Encryption(null);
    Properties result = new Properties();

    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if (encryption.isEncrypted(value)) {
        value = encryption.decrypt(value);
      }
      result.setProperty(key, value);
    }
    return result;
  }
}
