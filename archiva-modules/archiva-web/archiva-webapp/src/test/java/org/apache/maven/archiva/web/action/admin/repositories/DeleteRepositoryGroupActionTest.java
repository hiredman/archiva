package org.apache.maven.archiva.web.action.admin.repositories;

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

import java.util.Collections;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.web.action.AbstractActionTestCase;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;

import com.opensymphony.xwork2.Action;

/**
 * DeleteRepositoryGroupActionTest
 * 
 * @version
 */
public class DeleteRepositoryGroupActionTest 
    extends AbstractActionTestCase
{
    private static final String REPO_GROUP_ID = "repo-group-ident";

    private DeleteRepositoryGroupAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        action = (DeleteRepositoryGroupAction) lookup ( Action.class.getName(), "deleteRepositoryGroupAction" );
        
        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );
    }
    
    public void testSecureActionBundle()
        throws SecureActionException
	{
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();
	
        action.prepare();
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    public void testDeleteRepositoryGroupConfirmation()
        throws Exception
    {
        RepositoryGroupConfiguration origRepoGroup = createRepositoryGroup();
        Configuration configuration = createConfigurationForEditing( origRepoGroup );
    
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.replay();
        
        action.setRepoGroupId( REPO_GROUP_ID );

        action.prepare();
        assertEquals( REPO_GROUP_ID, action.getRepoGroupId() );
        RepositoryGroupConfiguration repoGroup = action.getRepositoryGroup();
        assertNotNull( repoGroup );
        assertEquals( repoGroup.getId(), action.getRepoGroupId() );
        assertEquals( Collections.singletonList( origRepoGroup ), configuration.getRepositoryGroups() );
    }

    public void testDeleteRepositoryGroup()
        throws Exception
    {
        Configuration configuration = createConfigurationForEditing( createRepositoryGroup() );
    	
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 3 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();
    	
        action.setRepoGroupId( REPO_GROUP_ID );
    	
        action.prepare();
        assertEquals( REPO_GROUP_ID, action.getRepoGroupId() );
        RepositoryGroupConfiguration repoGroup = action.getRepositoryGroup();
        assertNotNull( repoGroup );
        assertEquals( Collections.singletonList( repoGroup ), configuration.getRepositoryGroups() );
    	
        String status = action.delete();
        assertEquals( Action.SUCCESS, status );
        assertTrue( configuration.getRepositoryGroups().isEmpty() );
    }

    public void testDeleteRepositoryGroupCancelled()
        throws Exception
    {
        RepositoryGroupConfiguration origRepoGroup = createRepositoryGroup();
        Configuration configuration = createConfigurationForEditing ( origRepoGroup );
    	
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 2 );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();
        
        action.setRepoGroupId( REPO_GROUP_ID );
        
        action.prepare();
        assertEquals( REPO_GROUP_ID, action.getRepoGroupId() );
        RepositoryGroupConfiguration repoGroup = action.getRepositoryGroup();
        assertNotNull( repoGroup );
        
        String status = action.execute();
        assertEquals( Action.SUCCESS, status );
        assertEquals( Collections.singletonList( repoGroup ), configuration.getRepositoryGroups() );
    }

    private Configuration createConfigurationForEditing( RepositoryGroupConfiguration repoGroup )
    {
        Configuration configuration = new Configuration();
        configuration.addRepositoryGroup( repoGroup );
        return configuration;
    }
    
    private RepositoryGroupConfiguration createRepositoryGroup()
    {
        RepositoryGroupConfiguration repoGroup = new RepositoryGroupConfiguration();
        repoGroup.setId( REPO_GROUP_ID );
    	
        return repoGroup;
    }
}
