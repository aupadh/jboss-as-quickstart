/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.ejb.clients;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.quickstarts.ejb.multi.server.app.AppTwo;

/**
 * Test whether a EJBCLIENT000016 message is shown.
 * This is not if only java.util.logging is use but when log4j is configured ????????
 * 
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
public class TestCloseMessagesClient {
  private static final Logger LOGGER = Logger.getLogger(TestCloseMessagesClient.class.getName());
  private static final String REMOTE_NAME = "ejb:apptwo/ejb//AppTwoBean!" + AppTwo.class.getName();

  
  private final InitialContext scopedContext;
  
  TestCloseMessagesClient() throws Exception {
    this.scopedContext = createContextApp2A();
    LOGGER.info("Context created!");
  }

  private static InitialContext createContextApp2A() throws NamingException {
    Properties p = new Properties();
    // The endpoint.name is only use at client side and not necessary to set
    p.put("endpoint.name", "client-endpoint-A");
    p.put("org.jboss.ejb.client.scoped.context", true);
    p.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
    p.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
    p.put("remote.connections", "appTwo");
    p.put("remote.connection.appTwo.port", String.valueOf(4647));
    p.put("remote.connection.appTwo.host", "localhost");
    p.put("remote.connection.appTwo.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
    p.put("remote.connection.appTwo.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
    p.put("remote.connection.appTwo.username", "quickuser");
    p.put("remote.connection.appTwo.password", "quick-123");

    return new InitialContext(p);
  }

  
  private void startTest() throws NamingException {
      final AppTwo proxy = (AppTwo) this.scopedContext.lookup(REMOTE_NAME);
      final int executions = 2;
      final String callMessage = "TestInvocation";
      final long sleeptime = 1000;
      
      LOGGER.info("STARTING  "+callMessage);
      for (int i = 1; i <= executions; i++) {
        proxy.invoke(callMessage+" #"+i);
        try {
            Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
        }
      }
      LOGGER.info("FINISHED  "+callMessage);

    LOGGER.info("CLOSING Context!");
    this.scopedContext.close();
    LOGGER.info("Context CLOSED!");
  }

  
  /**
   * @param argsj
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    LOGGER.info("START");
    
    Logger.getLogger("").setLevel(Level.INFO);
    Logger.getLogger("org.jboss.ejb.client.remoting").setLevel(Level.INFO);
    Logger.getLogger("org.xnio").setLevel(Level.INFO);
    Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
    
    TestCloseMessagesClient tc = new TestCloseMessagesClient();
    
    tc.startTest();
  }
}
