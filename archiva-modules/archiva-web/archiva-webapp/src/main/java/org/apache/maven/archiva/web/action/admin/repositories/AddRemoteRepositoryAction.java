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

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.audit.AuditEvent;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManagerException;

import java.io.IOException;

/**
 * AddRemoteRepositoryAction 
 *
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="addRemoteRepositoryAction" instantiation-strategy="per-lookup"
 */
public class AddRemoteRepositoryAction
    extends AbstractRemoteRepositoriesAction
    implements Preparable, Validateable
{
    /**
     * The model for this action.
     */
    private RemoteRepositoryConfiguration repository;    
    
    public void prepare()
    {
        this.repository = new RemoteRepositoryConfiguration();
    }

    public String input()
    {
        return INPUT;
    }

    public String commit()
    {
        Configuration configuration = archivaConfiguration.getConfiguration();
        
        //MRM-752 - url needs trimming
        repository.setUrl(repository.getUrl().trim());
        
        // Save the repository configuration.
        String result;
        try
        {
            addRepository( repository, configuration );
            triggerAuditEvent( repository.getId(), null, AuditEvent.ADD_REMOTE_REPO );
            result = saveConfiguration( configuration );
        }
        catch ( IOException e )
        {
            addActionError( "I/O Exception: " + e.getMessage() );
            result = INPUT;
        }
        catch ( RoleManagerException e )
        {
            addActionError( "Role Manager Exception: " + e.getMessage() );
            result = INPUT;
        }

        return result;
    }
    
    @Override
    public void validate()
    {
        Configuration config = archivaConfiguration.getConfiguration();
        
        String repoId = repository.getId();
        
        if ( config.getManagedRepositoriesAsMap().containsKey( repoId ) )
        {
            addFieldError( "repository.id", "Unable to add new repository with id [" + repoId
                + "], that id already exists as a managed repository." );
        }
        else if ( config.getRemoteRepositoriesAsMap().containsKey( repoId ) )
        {
            addFieldError( "repository.id", "Unable to add new repository with id [" + repoId
                + "], that id already exists as a remote repository." );
        }
        else if( config.getRepositoryGroupsAsMap().containsKey( repoId ) )
        {
            addFieldError( "repository.id", "Unable to add new repository with id [" + repoId
               + "], that id already exists as a repository group." );
        }
    }
    
    public RemoteRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void setRepository( RemoteRepositoryConfiguration repository )
    {
        this.repository = repository;
    }
}
