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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensymphony.xwork2.Action;
import org.apache.archiva.audit.AuditListener;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.apache.maven.archiva.web.validator.utils.ValidatorUtil;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;

/**
 * AddManagedRepositoryActionTest 
 *
 * @version $Id$
 */
public class AddManagedRepositoryActionTest
    extends AbstractManagedRepositoryActionTest
{
    private AddManagedRepositoryAction action;

    private RoleManager roleManager;

    private MockControl roleManagerControl;

    private MockControl archivaConfigurationControl;

    private Registry registry;

    private MockControl registryControl;

    private ArchivaConfiguration archivaConfiguration;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = new AddManagedRepositoryAction();
        
        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );

        roleManagerControl = MockControl.createControl( RoleManager.class );
        roleManager = (RoleManager) roleManagerControl.getMock();
        action.setRoleManager( roleManager );

        registryControl = MockControl.createControl( Registry.class );
        registry = (Registry) registryControl.getMock();
        action.setRegistry( registry );

        location = getTestFile( "target/test/location" );
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

    public void testAddRepositoryInitialPage()
        throws Exception
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        ManagedRepositoryConfiguration configuration = action.getRepository();
        assertNotNull( configuration );
        assertNull( configuration.getId() );
        // check all booleans are false
        assertFalse( configuration.isDeleteReleasedSnapshots() );
        assertFalse( configuration.isScanned() );
        assertFalse( configuration.isReleases() );
        assertFalse( configuration.isSnapshots() );

        String status = action.input();
        assertEquals( Action.INPUT, status );

        // check defaults
        assertFalse( configuration.isDeleteReleasedSnapshots() );
        assertTrue( configuration.isScanned() );
        assertTrue( configuration.isReleases() );
        assertFalse( configuration.isSnapshots() );
    }

    public void testAddRepository()
        throws Exception
    {
        FileUtils.deleteDirectory( location );

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setVoidCallable();
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setVoidCallable();

        roleManagerControl.replay();

        registry.getString( "appserver.base", "${appserver.base}" );
        registryControl.setReturnValue( "target/test" );
        registry.getString( "appserver.home", "${appserver.home}" );
        registryControl.setReturnValue( "target/test" );

        registryControl.replay();

        Configuration configuration = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.prepare();
        ManagedRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );

        assertFalse( location.exists() );
        String status = action.commit();
        assertEquals( Action.SUCCESS, status );
        assertTrue( location.exists() );
        assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );
        assertEquals( location.getCanonicalPath(), new File( repository.getLocation() ).getCanonicalPath() );

        roleManagerControl.verify();
        archivaConfigurationControl.verify();
        registryControl.verify();
    }
    
    
    public void testAddRepositoryExistingLocation()
        throws Exception
    {
        if( !location.exists() )
        {
            location.mkdirs();
        }

        registry.getString( "appserver.base", "${appserver.base}" );
        registryControl.setReturnValue( "target/test" );
        registry.getString( "appserver.home", "${appserver.home}" );
        registryControl.setReturnValue( "target/test" );

        registryControl.replay();

        action.prepare();
        ManagedRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );
    
        assertTrue( location.exists() );
        String status = action.commit();
        assertEquals( AddManagedRepositoryAction.CONFIRM, status );
        assertEquals( location.getCanonicalPath(), new File( repository.getLocation() ).getCanonicalPath() );
        registryControl.verify();
    }

    public void testStruts2ValidationFrameworkWithNullInputs() throws Exception
    {
        // prep
        // 0 is the default value for primitive int; null for objects
        ManagedRepositoryConfiguration managedRepositoryConfiguration = createManagedRepositoryConfiguration(null, null, null, null);
        action.setRepository(managedRepositoryConfiguration);

        // test
        actionValidatorManager.validate(action, EMPTY_STRING);

        // verify
        assertTrue(action.hasFieldErrors());

        Map<String, List<String>> fieldErrors = action.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a repository identifier.");
        expectedFieldErrors.put("repository.id", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a directory.");
        expectedFieldErrors.put("repository.location", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a repository name.");
        expectedFieldErrors.put("repository.name", expectedErrorMessages);

        ValidatorUtil.assertFieldErrors(expectedFieldErrors, fieldErrors);
    }

    public void testStruts2ValidationFrameworkWithBlankInputs() throws Exception
    {
        // prep
        // 0 is the default value for primitive int
        ManagedRepositoryConfiguration managedRepositoryConfiguration = createManagedRepositoryConfiguration(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);
        action.setRepository(managedRepositoryConfiguration);

        // test
        actionValidatorManager.validate(action, EMPTY_STRING);

        // verify
        assertTrue(action.hasFieldErrors());

        Map<String, List<String>> fieldErrors = action.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a repository identifier.");
        expectedFieldErrors.put("repository.id", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a directory.");
        expectedFieldErrors.put("repository.location", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("You must enter a repository name.");
        expectedFieldErrors.put("repository.name", expectedErrorMessages);

        ValidatorUtil.assertFieldErrors(expectedFieldErrors, fieldErrors);
    }

    public void testStruts2ValidationFrameworkWithInvalidInputs() throws Exception
    {
        // prep
        ManagedRepositoryConfiguration managedRepositoryConfiguration = createManagedRepositoryConfiguration(REPOSITORY_ID_INVALID_INPUT, REPOSITORY_NAME_INVALID_INPUT, REPOSITORY_LOCATION_INVALID_INPUT, REPOSITORY_INDEX_DIR_INVALID_INPUT, REPOSITORY_DAYS_OLDER_INVALID_INPUT, REPOSITORY_RETENTION_COUNT_INVALID_INPUT);
        action.setRepository(managedRepositoryConfiguration);

        // test
        actionValidatorManager.validate(action, EMPTY_STRING);

        // verify
        assertTrue(action.hasFieldErrors());

        Map<String, List<String>> fieldErrors = action.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-).");
        expectedFieldErrors.put("repository.id", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-).");
        expectedFieldErrors.put("repository.location", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'),  underscores(_), dots(.), and dashes(-).");
        expectedFieldErrors.put("repository.name", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-).");
        expectedFieldErrors.put("repository.indexDir", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Repository Purge By Retention Count needs to be between 1 and 100.");
        expectedFieldErrors.put("repository.retentionCount", expectedErrorMessages);

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add("Repository Purge By Days Older Than needs to be larger than 0.");
        expectedFieldErrors.put("repository.daysOlder", expectedErrorMessages);

        ValidatorUtil.assertFieldErrors(expectedFieldErrors, fieldErrors);
    }

    public void testStruts2ValidationFrameworkWithValidInputs() throws Exception
    {
        // prep
        ManagedRepositoryConfiguration managedRepositoryConfiguration = createManagedRepositoryConfiguration(REPOSITORY_ID_VALID_INPUT, REPOSITORY_NAME_VALID_INPUT, REPOSITORY_LOCATION_VALID_INPUT, REPOSITORY_INDEX_DIR_VALID_INPUT, REPOSITORY_DAYS_OLDER_VALID_INPUT, REPOSITORY_RETENTION_COUNT_VALID_INPUT);
        action.setRepository(managedRepositoryConfiguration);

        // test
        actionValidatorManager.validate(action, EMPTY_STRING);

        // verify
        assertFalse(action.hasFieldErrors());
    }


    // TODO: test errors during add, other actions
}
