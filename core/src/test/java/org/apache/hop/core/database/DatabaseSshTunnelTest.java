/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.core.database;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.apache.hop.core.HopClientEnvironment;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.ILoggingObject;
import org.apache.hop.core.logging.LogLevel;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.junit.rules.RestoreHopEnvironment;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class DatabaseSshTunnelTest {

  @ClassRule public static RestoreHopEnvironment env = new RestoreHopEnvironment();

  private final DatabaseMeta meta = mock(DatabaseMeta.class);
  private final ILoggingObject log = mock(ILoggingObject.class);
  private IVariables variables;

  @BeforeClass
  public static void setUpClass() throws Exception {
    HopClientEnvironment.init();
  }

  @Before
  public void setUp() {
    when(log.getLogLevel()).thenReturn(LogLevel.NOTHING);
    variables = new Variables();
  }

  @Test
  public void testCloseConnectionOnlyClosesSshTunnel() throws Exception {
    Connection conn = mockConnection();
    SshTunnelManager tunnelManager = mock(SshTunnelManager.class);

    Database db = new Database(log, variables, meta);
    db.setConnection(conn);
    setSshTunnelManager(db, tunnelManager);

    db.closeConnectionOnly();

    // Verify tunnel was closed
    verify(tunnelManager).closeTunnel(any(ILogChannel.class));
    // Verify JDBC connection was closed
    verify(conn).close();
  }

  @Test
  public void testCloseConnectionOnlyWithoutTunnel() throws Exception {
    Connection conn = mockConnection();

    Database db = new Database(log, variables, meta);
    db.setConnection(conn);

    // Should not throw when no tunnel is configured
    db.closeConnectionOnly();

    verify(conn).close();
  }

  @Test
  public void testDisconnectClosesSshTunnelViaCloseConnectionOnly() throws Exception {
    Connection conn = mockConnection();
    SshTunnelManager tunnelManager = mock(SshTunnelManager.class);

    Database db = new Database(log, variables, meta);
    db.setConnection(conn);
    setSshTunnelManager(db, tunnelManager);

    db.disconnect();

    // Verify tunnel was closed (via closeConnectionOnly called from disconnect)
    verify(tunnelManager).closeTunnel(any(ILogChannel.class));
  }

  @Test
  public void testTunnelManagerIsNulledAfterClose() throws Exception {
    Connection conn = mockConnection();
    SshTunnelManager tunnelManager = mock(SshTunnelManager.class);

    Database db = new Database(log, variables, meta);
    db.setConnection(conn);
    setSshTunnelManager(db, tunnelManager);

    db.closeConnectionOnly();

    // Verify sshTunnelManager field is nulled after close
    Field field = Database.class.getDeclaredField("sshTunnelManager");
    field.setAccessible(true);
    assertNull(field.get(db));
  }

  @Test
  public void testNoTunnelOpenedWhenSshDisabled() throws Exception {
    when(meta.isSshTunnelEnabled()).thenReturn(false);

    Database db = new Database(log, variables, meta);

    // Verify no tunnel manager is set when SSH is not enabled
    Field field = Database.class.getDeclaredField("sshTunnelManager");
    field.setAccessible(true);
    assertNull(field.get(db));
  }

  private Connection mockConnection() throws Exception {
    Connection conn = mock(Connection.class);
    DatabaseMetaData dbMetaData = mock(DatabaseMetaData.class);
    when(conn.getMetaData()).thenReturn(dbMetaData);
    return conn;
  }

  private void setSshTunnelManager(Database db, SshTunnelManager manager)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = Database.class.getDeclaredField("sshTunnelManager");
    field.setAccessible(true);
    field.set(db, manager);
  }
}
