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

import org.jboss.as.quickstarts.ejb.clients.server.app.StatefulSession;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;

/**
 * A simple EJB client to demonstrate how to use the native ejb-client API to call unclustered EJB's.<br/>
 * 
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 * 
 */
public class StatefulSessionJBossApiClient {
  private static final Logger LOGGER = Logger.getLogger(StatefulSessionJBossApiClient.class.getName());
  final Boolean secured;
  final boolean useDefaultCredentials;
  final String user;
  final String password;

  private StatefulSession remoteBean;

  public StatefulSessionJBossApiClient(Boolean secured, boolean useDefaultUser, String user, String password, Boolean debug) {
    this.secured = secured;
    this.useDefaultCredentials = useDefaultUser;
    this.user = user;
    this.password = password;

    Level l = debug == null ? Level.SEVERE : debug.booleanValue() ? Level.ALL : Level.INFO;
    Logger.getLogger("").setLevel(l);
    Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
    Logger.getLogger(AppOneClusterJBossApiClient.class.getPackage().getName()).setLevel(
        debug != null ? Level.FINEST : Level.INFO);
  }

  private void createContext() {
    Properties p = new Properties();
    // The endpoint.name is only use at client side and not necessary to set
    p.put("endpoint.name", "StatefulSession-endpoint");
    p.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");

    p.put("remote.connections", "default");
    p.put("remote.connection.default.port", String.valueOf(4447));
    p.put("remote.connection.default.host", "localhost");

    if (this.user != null) {
      // If the username is given add to properties
      p.put("remote.connection.default.username", this.user);
      p.put("remote.connection.default.password", this.password);
    } else if (!this.useDefaultCredentials) {
      p.put("remote.connection.default.username", "quickuser");
      p.put("remote.connection.default.password", "quick-123");
    }

    LOGGER.info("PARAMS : " + p);

    // create the client configuration based on the given properties
    EJBClientConfiguration cc = new PropertiesBasedEJBClientConfiguration(p);
    // create and set the selector
    ContextSelector<EJBClientContext> selector = new ConfigBasedEJBClientContextSelector(cc);
    EJBClientContext.setSelector(selector);
  }

  private void createSFSB() throws NamingException {
    Properties props = new Properties();
    props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
    InitialContext context = new InitialContext(props);

    final String rcal = "ejb:appclient/ejb//StatefulSessionBean!" + StatefulSession.class.getName() + "?stateful";
    this.remoteBean = (StatefulSession) context.lookup(rcal);
    System.out.println("Created");
    System.out.println(" Initial counter value=" + this.remoteBean.getCounter());
  }

  private void invoke() throws NamingException {
    System.out.println("Start using");
    System.out.println(" Initial counter value=" + this.remoteBean.getCounter());

    System.out.println("Increment [" + this.remoteBean.incCount() + "]");
    System.out.println("Increment [" + this.remoteBean.incCount() + "]");
    System.out.println("Increment [" + this.remoteBean.incCount() + "]");

    System.out.println("Final counter value=" + this.remoteBean.getCounter());
  }
  
  private void destroy() {
    System.out.println("Remove Counter value=" + remoteBean.getCounter());
    this.remoteBean.remove();
  }

  /**
   * @param args it is possible to change the user/password and whether secured methods should be called. The logging behavior
   *        can be changed. if no debug parameter is given only this package is verbose in INFO mode.
   * 
   *        <ul>
   *        <li>-u &lt;username&gt;</li>
   *        <li>-p &lt;password&gt;</li>
   *        <li>-U use default credentials of the ejb-multi-server project (if no -u given)</li>
   *        <li>-d flag, whether all classes should be in INFO mode.</li>
   *        <li>-D flag, whether all classes should be in verbose TRACE mode</li>
   *        </ul>
   * 
   * @throws NamingException problem with InitialContext creation or lookup
   * @throws InterruptedException 
   */
  public static void main(String[] args) throws NamingException, InterruptedException {
    Boolean secured = null;
    boolean local = true;
    String user = null, passwd = null;
    Boolean debug = false;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-U")) {
        local = true;
      } else if (args[i].equals("-u")) {
        i++;
        user = args[i];
      } else if (args[i].equals("-p")) {
        i++;
        passwd = args[i];
      } else if (args[i].equals("-d")) {
        debug = debug == null ? Boolean.FALSE : Boolean.TRUE;
      } else if (args[i].equals("-D")) {
        debug = Boolean.TRUE;
      }
    }
    StatefulSessionJBossApiClient client = new StatefulSessionJBossApiClient(secured, local, user, passwd, debug);
    client.createContext();
    
    client.createSFSB();
    Thread.sleep(500);
    client.invoke();
    client.destroy();
    Thread.sleep(1000);
    client.createSFSB();
    client.invoke();
    client.invoke();
    client.destroy();
  }
}
