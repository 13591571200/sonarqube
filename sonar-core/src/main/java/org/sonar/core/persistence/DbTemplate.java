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
package org.sonar.core.persistence;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.SonarException;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class DbTemplate implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DbTemplate.class);

  public DbTemplate copyTable(DataSource source, DataSource dest, String table, String... whereClauses) {
    LOG.debug("Copy table %s", table);

    String selectQuery = "select * from " + table;
    if (whereClauses.length > 0) {
      List<String> clauses = Lists.newArrayList();
      for (String whereClause : whereClauses) {
        clauses.add('(' + whereClause + ')');
      }

      selectQuery += " WHERE " + Joiner.on(" AND ").join(clauses);
    }

    truncate(dest, table);

    Connection sourceConnection = null;
    Statement sourceStatement = null;
    ResultSet sourceResultSet = null;
    Connection destConnection = null;
    ResultSet destResultSet = null;
    PreparedStatement destStatement = null;
    try {
      sourceConnection = source.getConnection();
      sourceStatement = sourceConnection.createStatement();
      sourceResultSet = sourceStatement.executeQuery(selectQuery);

      if (sourceResultSet.next()) {
        int colCount = sourceResultSet.getMetaData().getColumnCount();

        destConnection = dest.getConnection();
        destConnection.setAutoCommit(false);

        destStatement = destConnection.prepareStatement("INSERT INTO " + table + " VALUES(" + StringUtils.repeat("?", ",", colCount) + ")");
        do {
          for (int col = 1; col <= colCount; col++) {
            Object value = sourceResultSet.getObject(col);
            destStatement.setObject(col, value);
          }
          destStatement.addBatch();
        } while (sourceResultSet.next());

        destStatement.executeBatch();
        destConnection.commit();
      }
    } catch (SQLException e) {
      LOG.error("Fail to copy table " + table, e);
      throw new SonarException("Fail to copy table " + table, e);
    } finally {
      DatabaseUtils.closeQuietly(destStatement);
      DatabaseUtils.closeQuietly(destResultSet);
      DatabaseUtils.closeQuietly(destConnection);
      DatabaseUtils.closeQuietly(sourceResultSet);
      DatabaseUtils.closeQuietly(sourceStatement);
      DatabaseUtils.closeQuietly(sourceConnection);
    }

    return this;
  }

  public int getRowCount(DataSource dataSource, String table) {
    Connection connection = null;
    Statement statement = null;
    ResultSet resultSet = null;
    try {
      connection = dataSource.getConnection();
      statement = connection.createStatement();
      resultSet = statement.executeQuery("SELECT count(*) from " + table);

      return resultSet.next() ? resultSet.getInt(1) : 0;
    } catch (SQLException e) {
      LOG.error("Fail to get row count for table " + table, e);
      throw new SonarException("Fail to get row count for table " + table, e);
    } finally {
      DatabaseUtils.closeQuietly(resultSet);
      DatabaseUtils.closeQuietly(statement);
      DatabaseUtils.closeQuietly(connection);
    }
  }

  public DbTemplate truncate(DataSource dataSource, String table) {
    Connection connection = null;
    Statement statement = null;
    try {
      connection = dataSource.getConnection();
      statement = connection.createStatement();
      statement.executeUpdate("TRUNCATE TABLE " + table);
    } catch (SQLException e) {
      LOG.error("Fail to truncate table " + table, e);
      throw new SonarException("Fail to truncate table " + table, e);
    } finally {
      DatabaseUtils.closeQuietly(statement);
      DatabaseUtils.closeQuietly(connection);
    }

    return this;
  }

  public BasicDataSource dataSource(String driver, String user, String password, String url) {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName(driver);
    dataSource.setUsername(user);
    dataSource.setPassword(password);
    dataSource.setUrl(url);
    return dataSource;
  }

  public DbTemplate createSchema(DataSource dataSource, String dialect) {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      DdlUtils.createSchema(connection, dialect);
    } catch (SQLException e) {
      LOG.error("Fail to createSchema local database schema", e);
      throw new SonarException("Fail to createSchema local database schema", e);
    } finally {
      DatabaseUtils.closeQuietly(connection);
    }

    return this;
  }
}
