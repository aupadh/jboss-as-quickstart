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
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;

/**
 * test whether GC will destroy the client context.
 * check how many threads are opend if a lookup run in a loop.
 * 
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
public class TestClient {
  private static final Logger LOGGER = Logger.getLogger(TestClient.class.getName());
  private static final String REMOTE_NAME = "ejb:apptwo/ejb//AppTwoBean!" + AppTwo.class.getName();

  
  private final InitialContext scopedContext;
  private final AppTwo appTwoProxy;
  
  TestClient() throws Exception {
    this.scopedContext = createContextApp2A();
    InitialContext createContextApp2B = createContextApp2B();
    this.appTwoProxy = (AppTwo) createContextApp2B.lookup(REMOTE_NAME);
    createContextApp2B.close();
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

  private static InitialContext createContextApp2B() throws NamingException {
    Properties p = new Properties();
    p.put("endpoint.name", "client-endpoint-B");
    p.put("org.jboss.ejb.client.scoped.context", true);
    p.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
    p.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
    p.put("remote.connections", "appTwo");
    p.put("remote.connection.appTwo.port", String.valueOf(5247));
    p.put("remote.connection.appTwo.host", "localhost");
    p.put("remote.connection.appTwo.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
    p.put("remote.connection.appTwo.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
    p.put("remote.connection.appTwo.username", "quickuser2");
    p.put("remote.connection.appTwo.password", "quick+123");

    return new InitialContext(p);
  }


  private static void createContext() {
    Properties p = new Properties();
    // The endpoint.name is only use at client side and not necessary to set
    p.put("endpoint.name", "nonClustered-client-endpoint");
    p.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");

    p.put("remote.connections", "appTwoB");
    p.put("remote.connection.appTwoA.port", String.valueOf(4647));
    p.put("remote.connection.appTwoA.host", "localhost");
    p.put("remote.connection.appTwoB.port", String.valueOf(5247));
    p.put("remote.connection.appTwoB.host", "localhost");

    p.put("remote.connection.appTwoA.username", "quickuser2");
    p.put("remote.connection.appTwoA.password", "quick+123");
    p.put("remote.connection.appTwoB.username", "quickuser2");
    p.put("remote.connection.appTwoB.password", "quick+123");

    // create the client configuration based on the given properties
    EJBClientConfiguration cc = new PropertiesBasedEJBClientConfiguration(p);
    // create and set the selector
    ContextSelector<EJBClientContext> selector = new ConfigBasedEJBClientContextSelector(cc);
    EJBClientContext.setSelector(selector);
  }
  
  private void startTest() {
//    createContext();
    try {
      LOGGER.info("RUN test invocation XX");
      ((AppTwo)createContextApp2B().lookup(REMOTE_NAME)).invoke("Test Invocation XX");

      LOGGER.info("RUN test invocation");
      ((AppTwo)scopedContext.lookup(REMOTE_NAME)).invoke("Test Invocation");
      LOGGER.info("RUN test invocation proxy");
      appTwoProxy.invoke("Test Invocation");
      LOGGER.info("Test invocation succeeds");
    } catch (NamingException e1) {
      LOGGER.log(Level.SEVERE,"ERROR",e1);
      return;
    }
    Thread tP = new Thread(new Runnable() {
      
      @Override
      public void run() {
        try {
          while (true) {
            appTwoProxy.invoke("invokeWithProxy");
            Thread.sleep(1000);
          }
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "invokeWithProxy failed",e);
        }
      }
    });
    tP.start();
    
    Thread tC = new Thread(new Runnable() {
      
      @Override
      public void run() {
        try {
          while (true) {
            ((AppTwo)scopedContext.lookup(REMOTE_NAME)).invoke("invokeWithContext");
            Thread.sleep(1000);
          }
        } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "invokeWithContext failed",e);
        }
      }
    });
    
    tC.start();

    // force GC's
    LOGGER.info("start creating garbage");
    while(true) {
      for (int i = 0; i < 100; i++) {
        StringBuilder x = new StringBuilder(10000);
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
        x.append("ajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlziajhdfklahfkladkjahdfklahfklahlfkhaklsdfhaklhfdklahflkahfknvlkjdashfirawenvcikashdfaiowjfnawiuehtaowiehfnlzi");
      }
      System.gc();
    }
  }

  
  private void invokeWithProxy() throws InterruptedException {
    while(true) {
      appTwoProxy.invoke("invokeWithProxy");
      Thread.sleep(1000);
    }
  }

  private void invokeWithGlobalContext() throws InterruptedException, NamingException {
    createContext();
    
    Properties p= new Properties();
    p.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
    InitialContext ic = new InitialContext(p);
    while(true) {
      //((AppTwo)scopedContext.lookup(REMOTE_NAME)).invoke("invokeWithContext");
      ((AppTwo)ic.lookup(REMOTE_NAME)).invoke("invokeWithContext");
      Thread.sleep(10000);
    }
  }

  private void invokeWithContext() throws InterruptedException, NamingException {
    while(true) {
      ((AppTwo)scopedContext.lookup(REMOTE_NAME)).invoke("invokeWithContext");
      Thread.sleep(10000);
    }
  }

  /**
   * @param argsj
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    LOGGER.info("START");
    Thread.sleep(10000);
    LOGGER.info("create context");
//    InitialContext icA = createContextApp2A();
    InitialContext icB = createContextApp2B();
    while(true) {
      Thread.sleep(1000);
      LOGGER.info("INVOKE");
//      ((AppTwo)icA.lookup(REMOTE_NAME)).invoke("invokeWithContext A");
      ((AppTwo)icB.lookup(REMOTE_NAME)).invoke("invokeWithContext B");
    }
    
//    TestClient tc = new TestClient();
    
    //tc.startTest();
//    tc.invokeWithProxy();
//    tc.invokeWithContext();
    
//    InitialContext ic = createContextApp2();
//    
//    final String rcal = "ejb:apptwo/ejb//AppTwoBean!" + AppTwo.class.getName();
//    final AppTwo remote = (AppTwo) ic.lookup(rcal);
//    
//    LOGGER.info("AppTwo Proxy created : "+remote);
//    
//    ObjectOutput out = new ObjectOutputStream(new FileOutputStream("AppTwo.serial"));
//    out.writeObject(remote);
//    out.close();
//    ObjectInputStream in = new ObjectInputStream(new FileInputStream("AppTwo.serial"));
//    AppTwo serializedRemote = (AppTwo) in.readObject();
//    in.close();
//    new File("AppTwo.serial").delete();
//
//    LOGGER.info("AppTwo Proxy serialized : "+serializedRemote); 
//
//    remote.invoke("REMOTE");
//    serializedRemote.invoke("SERIALIZED");
  }
}
