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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.quickstarts.ejb.multi.server.app.AppOne;
import org.jboss.as.quickstarts.ejb.multi.server.app.AppTwo;

public class SecuredAppClient {
    private static final Logger LOGGER = Logger.getLogger(SecuredAppClient.class.getName());

    private static final String remoteBeanIdentifierApp1 = "ejb:appone/ejb//AppOneBean!" + AppOne.class.getName();
    private static final String remoteBeanIdentifierApp2 = "ejb:apptwo/ejb//AppTwoBean!" + AppTwo.class.getName();
    
    private static final String SERVER_NAME_APPONE = "master:app-one";
    private static final String SERVER_NAME_APPTWO= "master:app-two";

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final EJBClientConnectionConfiguration server_AppOne_Conf = new EJBClientConnectionConfiguration();
        server_AppOne_Conf.addServer("localhost", 4547, "app1user", "app1+123");
        final EJBClientConnectionConfiguration server_AppTwo_Conf = new EJBClientConnectionConfiguration();
        server_AppTwo_Conf.addServer("localhost", 4647, "app2user", "app2+123");
        
        AppOne serverAppOne_App1;
        AppTwo serverAppTwo_App2;
        
        try {
            // prepare proxy for serverOne
            InitialContext ic = new InitialContext(server_AppOne_Conf.getEJBClientConfiguration());
            serverAppOne_App1 = (AppOne) ic.lookup(remoteBeanIdentifierApp1);
            ic.close();
        } catch (NamingException e) {
            throw new RuntimeException("Can not initialize proxies for server one!",e);
        }


        try {
            // prepare proxy for serverTwo
            InitialContext ic = new InitialContext(server_AppTwo_Conf.getEJBClientConfiguration());
            serverAppTwo_App2 = (AppTwo) ic.lookup(remoteBeanIdentifierApp2);
            ic.close();
        } catch (NamingException e) {
            throw new RuntimeException("Can not initialize proxies for server two!",e);
        }

        boolean resultOk = true;
        
        resultOk &= checkServerNodeName(serverAppOne_App1.invokeSecured("Secured call from client"), SERVER_NAME_APPONE);
        resultOk &= checkServerNodeName(serverAppOne_App1.invoke("Unsecured call from client"), SERVER_NAME_APPONE);
        resultOk &= checkServerNodeName(serverAppTwo_App2.invoke("Unsecured call from client"), SERVER_NAME_APPTWO);
        resultOk &= checkServerNodeName(serverAppTwo_App2.invokeSecured("Secured call from client"), SERVER_NAME_APPTWO);
        
        if(!resultOk) {
            throw new Exception("Not all invocations reach the expected server!");
        }
    }
    
    private static boolean checkServerNodeName(String current, String ...expected) {
        List<String> servers = Arrays.asList(expected);
        if(servers.contains(current)) {
            if(servers.size() == 1) {
                LOGGER.info("Call successfully reached the server '"+current+"'");
            } else {
                LOGGER.info("Call successfully reached the server '"+current+"' out of "+servers);
            }
            return true;
        } else {
            LOGGER.severe("Wrong jboss.node.name, expect " + (servers.size()==1 ? "'"+servers.get(0)+"'" : servers) + " but got '"+current+"'. Looks that the wrong server was selected!");
        }
        return false;
    }
}
