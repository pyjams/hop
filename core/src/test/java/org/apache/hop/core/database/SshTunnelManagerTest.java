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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.variables.IVariables;
import org.junit.Before;
import org.junit.Test;

public class SshTunnelManagerTest {

  private SshTunnelManager tunnelManager;
  private ILogChannel log;

  @Before
  public void setUp() {
    tunnelManager = new SshTunnelManager();
    log = mock(ILogChannel.class);
  }

  @Test
  public void testInitialState() {
    assertFalse(tunnelManager.isOpen());
    assertEquals(0, tunnelManager.getLocalPort());
  }

  @Test
  public void testCloseTunnelWhenNotOpen() {
    // Should not throw when closing a tunnel that was never opened
    tunnelManager.closeTunnel(log);
    assertFalse(tunnelManager.isOpen());
  }

  @Test
  public void testCloseTunnelMultipleTimes() {
    // Should be safe to close multiple times
    tunnelManager.closeTunnel(log);
    tunnelManager.closeTunnel(log);
    assertFalse(tunnelManager.isOpen());
  }

  @Test(expected = org.apache.hop.core.exception.HopDatabaseException.class)
  public void testOpenTunnelWithInvalidHost() throws Exception {
    IVariables variables = mock(IVariables.class);
    when(variables.resolve(anyString())).thenAnswer(inv -> inv.getArgument(0));

    DatabaseMeta databaseMeta = mock(DatabaseMeta.class);
    when(databaseMeta.getSshTunnelHost()).thenReturn("invalid-host-that-does-not-exist");
    when(databaseMeta.getSshTunnelPort()).thenReturn("22");
    when(databaseMeta.getSshTunnelUsername()).thenReturn("testuser");
    when(databaseMeta.getSshTunnelPassword()).thenReturn("testpass");
    when(databaseMeta.getHostname()).thenReturn("localhost");
    when(databaseMeta.getPort()).thenReturn("3306");
    when(databaseMeta.getDefaultDatabasePort()).thenReturn(3306);
    when(databaseMeta.isSshTunnelUsePrivateKey()).thenReturn(false);

    // Should throw HopDatabaseException because host is unreachable
    tunnelManager.openTunnel(variables, databaseMeta, log);
  }

  @Test
  public void testOpenTunnelWithInvalidHostCleansTunnel() {
    IVariables variables = mock(IVariables.class);
    when(variables.resolve(anyString())).thenAnswer(inv -> inv.getArgument(0));

    DatabaseMeta databaseMeta = mock(DatabaseMeta.class);
    when(databaseMeta.getSshTunnelHost()).thenReturn("invalid-host-that-does-not-exist");
    when(databaseMeta.getSshTunnelPort()).thenReturn("22");
    when(databaseMeta.getSshTunnelUsername()).thenReturn("testuser");
    when(databaseMeta.getSshTunnelPassword()).thenReturn("testpass");
    when(databaseMeta.getHostname()).thenReturn("localhost");
    when(databaseMeta.getPort()).thenReturn("3306");
    when(databaseMeta.getDefaultDatabasePort()).thenReturn(3306);
    when(databaseMeta.isSshTunnelUsePrivateKey()).thenReturn(false);

    try {
      tunnelManager.openTunnel(variables, databaseMeta, log);
    } catch (Exception e) {
      // Expected
    }

    // After a failed open, the tunnel should be cleaned up
    assertFalse(tunnelManager.isOpen());
  }
}
