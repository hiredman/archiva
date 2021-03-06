package org.apache.maven.archiva.web.action.admin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.codehaus.plexus.cache.Cache;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;

import java.util.Map;

/**
 * Shows system status information for the administrator.
 *
 * @version $Id$
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="systemStatus" instantiation-strategy="per-lookup"
 */
public class SystemStatusAction
    extends PlexusActionSupport
    implements SecureAction
{
    /**
     * @plexus.requirement role="org.codehaus.plexus.taskqueue.TaskQueue"
     */
    private Map<String,TaskQueue> queues;

    /**
     * @plexus.requirement role="org.codehaus.plexus.cache.Cache"
     */
    private Map<String,Cache> caches;

    /**
     * @plexus.requirement
     */
    private RepositoryScanner scanner;

    private String memoryStatus;

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public String execute()
    {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long total = runtime.totalMemory();
        long used = total - runtime.freeMemory();
        long max = runtime.maxMemory();
        memoryStatus = formatMemory(used) + "/" + formatMemory(total) + " (Max: " + formatMemory(max) + ")";
        
        return SUCCESS;
    }

    private static String formatMemory( long l )
    {
      return  l / ( 1024 * 1024 ) + "M";
    }

    public String getMemoryStatus()
    {
        return memoryStatus;
    }

    public RepositoryScanner getScanner()
    {
        return scanner;
    }

    public Map<String, Cache> getCaches()
    {
        return caches;
    }

    public Map<String, TaskQueue> getQueues()
    {
        return queues;
    }
}
