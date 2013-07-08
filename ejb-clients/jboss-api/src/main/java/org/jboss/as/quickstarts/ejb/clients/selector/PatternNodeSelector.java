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
package org.jboss.as.quickstarts.ejb.clients.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.ejb.client.DeploymentNodeSelector;

/**
 * <p>A simple example that shows how the node selection can be influenced in a thread save basis.<br/>
 * Here a pattern for the node name can be set to select only nodes where the name match.
 * If there is more than one node found one of the sub-selected will be picked randomly.</p>
 * 
 * @author <a href="mailto:wfink@redhat.com">Wolf-Dieter Fink</a>
 */
public class PatternNodeSelector implements DeploymentNodeSelector {
  private static final Logger LOGGER = Logger.getLogger(PatternNodeSelector.class.getName());
  private static ThreadLocal<String> pattern = new ThreadLocal<String>();
  private static ThreadLocal<Random> random = new ThreadLocal<Random>();
  
  /**
   * Set a name pattern for node selection for the actual Thread.
   * 
   * @param pattern a regular expression used for {@link String#matches(String)} to select nodes
   */
  public static void setPattern(String pattern) {
    PatternNodeSelector.pattern.set(pattern);
    PatternNodeSelector.random.set(new Random());
  }
  /**
   * Clear the name pattern for this Thread. 
   */
  public static void clearPattern() {
    PatternNodeSelector.pattern.set(null);
    PatternNodeSelector.random.set(null);
  }

  /**
   * Simple check of all eligible nodes.
   * For a productive use it might be optimized that the list is not calculated every time
   * or better if the base list is not changed.
   * 
   * @param eligibleNodes
   * @return all nodes which match the patter or all if there is no pattern set
   */
  private static String[] getUsableNodes(String[] eligibleNodes) {
    final String pattern = PatternNodeSelector.pattern.get();
    if(pattern == null) {
      return eligibleNodes;
    }
    
    ArrayList<String> nodes = new ArrayList<String>();
    
    for (String node : eligibleNodes) {
      if(node.matches(pattern)) {
        nodes.add(node);
      }
    }
    
    return (String[])nodes.toArray();
  }
  
  
  @Override
  public String selectNode(String[] eligibleNodes, String appName, String moduleName, String distinctName) {
    if (LOGGER.isLoggable(Level.FINER)) {
      LOGGER.finer("INSTANCE " + this + " : nodes:" + Arrays.deepToString(eligibleNodes) + " appName:" + appName + " moduleName:" + moduleName
          + " distinctName:" + distinctName);
    }

    String[] nodes = getUsableNodes(eligibleNodes);
    int nodeNo = 0;
    if (nodes.length == 0) {
      throw new IllegalStateException("Not possible to select a node from list "+nodes+" with pattern '"+PatternNodeSelector.pattern.get()+"'");
    }else if (nodes.length > 1) {
      // if there is only one there is no sense to choice
      // otherwise use random
      nodeNo = PatternNodeSelector.random.get().nextInt(nodes.length);
    }
    
    return nodes[nodeNo];
  }
}
