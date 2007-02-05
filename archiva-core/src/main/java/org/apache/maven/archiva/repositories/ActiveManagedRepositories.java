package org.apache.maven.archiva.repositories;

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

import org.apache.maven.archiva.artifact.ManagedArtifact;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

/**
 * ActiveManagedRepositories 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface ActiveManagedRepositories
{
    public static final String ROLE = ActiveManagedRepositories.class.getName();

    /**
     * Obtain the ArtifactRepository for the specified Repository ID.
     * 
     * @param id the ID of the repository.
     * @return the ArtifactRepository associated with the provided ID, or null if none found.
     */
    public ArtifactRepository getArtifactRepository( String id );

    public RepositoryConfiguration getRepositoryConfiguration( String id );

    public MavenProject findProject( String groupId, String artifactId, String version )
        throws ProjectBuildingException;

    public ManagedArtifact findArtifact( String groupId, String artifactId, String version )
        throws ProjectBuildingException;

    public ManagedArtifact findArtifact( String groupId, String artifactId, String version, String type );

    public ManagedArtifact findArtifact( Artifact artifact );
}
