/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq.impl.splunk;

import net.hydromatic.avatica.DriverVersion;

import net.hydromatic.linq4j.Enumerator;

import net.hydromatic.optiq.SchemaPlus;
import net.hydromatic.optiq.impl.splunk.search.*;
import net.hydromatic.optiq.jdbc.*;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * JDBC driver for Splunk.
 *
 * <p>It accepts connect strings that start with "jdbc:splunk:".</p>
 */
public class SplunkDriver extends net.hydromatic.optiq.jdbc.Driver {
  protected SplunkDriver() {
    super();
  }

  static {
    new SplunkDriver().register();
  }

  protected String getConnectStringPrefix() {
    return "jdbc:splunk:";
  }

  protected DriverVersion createDriverVersion() {
    return new SplunkDriverVersion();
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    Connection connection = super.connect(url, info);
    OptiqConnection optiqConnection = (OptiqConnection) connection;
    SplunkConnection splunkConnection;
    try {
      String url1 = info.getProperty("url");
      if (url1 == null) {
        throw new IllegalArgumentException(
            "Must specify 'url' property");
      }
      if (url1.equals("mock")) {
        splunkConnection = new MockSplunkConnection();
      } else {
        String user = info.getProperty("user");
        if (user == null) {
          throw new IllegalArgumentException(
              "Must specify 'user' property");
        }
        String password = info.getProperty("password");
        if (password == null) {
          throw new IllegalArgumentException(
              "Must specify 'password' property");
        }
        URL url2 = new URL(url1);
        splunkConnection = new SplunkConnectionImpl(url2, user, password);
      }
    } catch (Exception e) {
      throw new SQLException("Cannot connect", e);
    }
    final SchemaPlus rootSchema = optiqConnection.getRootSchema();
    rootSchema.add("splunk", new SplunkSchema(splunkConnection));

    return connection;
  }

  /** Connection that looks up responses from a static map. */
  private static class MockSplunkConnection implements SplunkConnection {
    public Enumerator<Object> getSearchResultEnumerator(String search,
        Map<String, String> otherArgs, List<String> fieldList) {
      throw null;
    }

    public void getSearchResults(String search, Map<String, String> otherArgs,
        List<String> fieldList, SearchResultListener srl) {
      throw new UnsupportedOperationException();
    }
  }

  /** Connection that records requests and responses. */
  private static class WrappingSplunkConnection implements SplunkConnection {
    private final SplunkConnection connection;

    public WrappingSplunkConnection(SplunkConnection connection) {
      this.connection = connection;
    }

    public void getSearchResults(String search, Map<String, String> otherArgs,
        List<String> fieldList, SearchResultListener srl) {
      System.out.println("search='" + search
          + "', otherArgs=" + otherArgs
          + ", fieldList='" + fieldList);
    }

    public Enumerator<Object> getSearchResultEnumerator(String search,
        Map<String, String> otherArgs, List<String> fieldList) {
      throw new UnsupportedOperationException();
    }
  }
}

// End SplunkDriver.java
