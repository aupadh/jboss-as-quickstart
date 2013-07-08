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

import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.quickstarts.ejb.multi.server.app.AppOne;
import org.jboss.as.quickstarts.ejb.multi.server.app.AppTwo;

/**
 * A simple EJB client to demonstrate how to use the scoped-context to connect a EJB.<br/>
 * Via command line the following interactions are possible:
 * <ul>
 * <li> -u -p  the user and password will be set</li>
 * <li> -L     credentials will be set to quickuser/quick-123 (the default of the multi-server project) if the client is not running on the same machine</li>
 * <li> -s     additional a secured method will be invoked</li>
 * 
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 *
 */
public class SimpleScopedContextClusterClient {
    private static final Logger LOGGER = Logger.getLogger(SimpleScopedContextClusterClient.class.getName());
    final Boolean secured;
    final boolean local;
    final String user;
    final String password;
    private InitialContext context;
    
    public SimpleScopedContextClusterClient(Boolean secured, boolean local, String user, String password, Boolean debug) throws NamingException {
        this.secured = secured;
        this.local = local;
        this.user = user;
        this.password = password;

        Level l = debug == null ? Level.OFF : debug.booleanValue() ? Level.ALL : Level.INFO;
        Logger.getLogger("").setLevel(l);
        Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
        LOGGER.setLevel(Boolean.TRUE.equals(debug) ? Level.FINEST : Level.INFO);
    }


    /**
     * Create a scoped context
     * @param name name of the client-endpoint
     * @param host hostname or IP
     * @param port port number fro connection
     * @param user Security credentials or <code>null</code>
     * @param passwd
     * @return
     * @throws NamingException
     */
    private static InitialContext createContext(String name, String host, int port, String user, String passwd) throws NamingException {
        Properties p = new Properties();

        p.put("endpoint.name", name == null ? "client-endpoint-SC" : name);
        p.put("org.jboss.ejb.client.scoped.context", true);
        p.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        p.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
        p.put("remote.connections", "one");
        p.put("remote.connection.one.port", String.valueOf(port));
        p.put("remote.connection.one.host", host);
        p.put("remote.connection.one.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
        p.put("remote.connection.one.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");

        final String clusterName = "ejb";
        p.put("remote.clusters", clusterName);
        p.put("remote.cluster."+clusterName+".connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
        p.put("remote.cluster."+clusterName+".connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");

        if(user != null && user.length() != 0) {
          p.put("remote.connection.one.username", user);
          p.put("remote.connection.one.password", passwd);
          p.put("remote.cluster."+clusterName+".username", user);
          p.put("remote.cluster."+clusterName+".password", passwd);
        }else if(user != null) {
          p.put("remote.connection.one.username", "quickuser");
          p.put("remote.connection.one.password", "quick-123");
          p.put("remote.cluster."+clusterName+".username", "quickuser");
          p.put("remote.cluster."+clusterName+".password", "quick-123");
        }
        LOGGER.info("PARAMS : "+p);

        return new InitialContext(p);
    }
    
    private void createContext(String name) throws NamingException {
      this.context = createContext(name, "localhost", 4547, this.user != null ? this.user : this.local ? null : "", this.password);
    }
    private void close() throws NamingException {
      saveCloseContext(context);
      this.context = null;
    }
    private static void saveCloseContext(Context context) throws NamingException {
      if(context != null) {
        LOGGER.info("try to close context!");
        context.close();
        LOGGER.info("context closed!");
      }
    }
    
    
    private void invokeAppOne() throws NamingException {
        final String rcal = "ejb:appone/ejb//AppOneBean!" + AppOne.class.getName();
        final AppOne remote = (AppOne) context.lookup(rcal);
        
        // invoke the unsecured method if needed
        if(this.secured == null || Boolean.FALSE.equals(this.secured)) {
            final String result = remote.invoke("Client call at "+new Date());
            LOGGER.info("The unsecured EJB call returns : "+result);
        }
        
        // invoke the secured method if wanted
        if(this.secured != null) {
            LOGGER.info("The secured EJB call returns : "+remote.invokeSecured("Client call secured at "+new Date()));
        }
    }
    
    /**
     * Invoke the appOne bean several times by using the remote-naming approach.
     * 
     * @param args it is possible to change the user/password and whether secured methods should be called
     *             <ul>
     *             <li>-u &lt;username&gt;</li>
     *             <li>-p &lt;password&gt;</li>
     *             <li>-s flag, if given the secured method is called in addition to the unsecured</li>
     *             <li>-S flag, if given only the secured method is called</li>
     *             <li>-l suppress the use of default credentials (ejb-multi-server project), will only work if the server is local</li>
     *             </ul>
     *             
     * @throws NamingException problem with InitialContext creation or lookup
     */
    public static void main(String[] args) throws NamingException {
        Boolean secured = null;
        boolean local = false;
        String user=null, passwd=null;
        Boolean debug = true;
        
        for (int i = 0; i < args.length; i++) {
            if(args[i].equals("-s")) {
                if(secured != null && secured.booleanValue() != false) {
                    throw new IllegalArgumentException("Only one of -s or -S can be set!");
                }
                secured = Boolean.FALSE;
            }else if(args[i].equals("-S")) {
                if(secured != null && secured.booleanValue() != true) {
                    throw new IllegalArgumentException("Only one of -s or -S can be set!");
                }
                secured = Boolean.TRUE;
            }else if(args[i].equals("-L")) {
                local = true;
            }else if(args[i].equals("-u")) {
                i++;
                user = args[i];
            }else if(args[i].equals("-p")) {
                i++;
                passwd = args[i];
            }else if(args[i].equals("-d")) {
                debug = debug==null ? Boolean.FALSE : Boolean.TRUE;
            }else if(args[i].equals("-D")) {
                debug = Boolean.TRUE;
            }
        }
        SimpleScopedContextClusterClient client = new SimpleScopedContextClusterClient(secured, local, user, passwd, debug);
        client.createContext("first");
        client.invokeAppOne();
        client.close();

        client.createContext("second");
        client.invokeAppOne();
    }
}
