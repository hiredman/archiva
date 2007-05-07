package org.apache.maven.archiva.web.action.admin.networkproxies;

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

import com.opensymphony.xwork.Preparable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.configuration.util.NetworkProxySelectionPredicate;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.IOException;

/**
 * ConfigureNetworkProxyAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureNetworkProxyAction"
 */
public class ConfigureNetworkProxyAction
    extends PlexusActionSupport
    implements SecureAction, Preparable
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private String mode;

    private String proxyid;

    private NetworkProxyConfiguration proxy;

    public String add()
    {
        this.mode = "add";
        return INPUT;
    }

    public String confirm()
    {
        return INPUT;
    }

    public String delete()
    {
        return INPUT;
    }

    public String edit()
    {
        this.mode = "edit";
        return INPUT;
    }

    public String getMode()
    {
        return mode;
    }

    public NetworkProxyConfiguration getProxy()
    {
        return proxy;
    }
    
    public String getProxyid()
    {
        return proxyid;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public String input()
    {
        return INPUT;
    }

    public void prepare()
        throws Exception
    {
        String id = getProxyid();

        if ( StringUtils.isNotBlank( id ) )
        {
            proxy = findNetworkProxy( id );
        }

        if ( proxy == null )
        {
            proxy = new NetworkProxyConfiguration();
        }
    }

    public String save()
    {
        String mode = getMode();

        String id = getProxy().getId();

        if ( StringUtils.equalsIgnoreCase( "edit", mode ) )
        {
            removeNetworkProxy( id );
        }

        try
        {
            addNetworkProxy( getProxy() );
            saveConfiguration();
        }
        catch ( IOException e )
        {
            addActionError( "I/O Exception: " + e.getMessage() );
        }
        catch ( InvalidConfigurationException e )
        {
            addActionError( "Invalid Configuration Exception: " + e.getMessage() );
        }
        catch ( RegistryException e )
        {
            addActionError( "Configuration Registry Exception: " + e.getMessage() );
        }

        return SUCCESS;
    }

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    public void setProxy( NetworkProxyConfiguration proxy )
    {
        this.proxy = proxy;
    }

    public void setProxyid( String proxyid )
    {
        this.proxyid = proxyid;
    }

    private void addNetworkProxy( NetworkProxyConfiguration proxy )
    {
        archivaConfiguration.getConfiguration().addNetworkProxy( proxy );
    }

    private NetworkProxyConfiguration findNetworkProxy( String id )
    {
        Configuration config = archivaConfiguration.getConfiguration();

        NetworkProxySelectionPredicate selectedProxy = new NetworkProxySelectionPredicate( id );

        return (NetworkProxyConfiguration) CollectionUtils.find( config.getNetworkProxies(), selectedProxy );
    }

    private void removeNetworkProxy( String id )
    {
        NetworkProxySelectionPredicate selectedProxy = new NetworkProxySelectionPredicate( id );
        NotPredicate notSelectedProxy = new NotPredicate( selectedProxy );
        CollectionUtils.filter( archivaConfiguration.getConfiguration().getNetworkProxies(), notSelectedProxy );
    }

    private String saveConfiguration()
        throws IOException, InvalidConfigurationException, RegistryException
    {
        archivaConfiguration.save( archivaConfiguration.getConfiguration() );

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }
}
