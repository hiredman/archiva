package org.apache.maven.repository.indexing;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;


/**
 * @author Edwin Punzalan
 */
public interface RepositoryIndexingFactory
{
    String ROLE = RepositoryIndexingFactory.class.getName();

    /**
     * Method to create an instance of the ArtifactRepositoryIndex
     *
     * @param indexPath  the path where the index will be created/updated
     * @param repository the repository where the indexed artifacts are located
     * @return the ArtifactRepositoryIndex instance
     * @throws RepositoryIndexException
     */
    ArtifactRepositoryIndex createArtifactRepositoryIndex( String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException;

    /**
     * Method to create an instance of the PomRepositoryIndex
     *
     * @param indexPath  the path where the index will be created/updated
     * @param repository the repository where the indexed poms are located
     * @return the PomRepositoryIndex instance
     * @throws RepositoryIndexException
     */
    PomRepositoryIndex createPomRepositoryIndex( String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException;

    /**
     * Method to create instance of the MetadataRepositoryIndex
     *
     * @param indexPath  the path where the index will be created/updated
     * @param repository the repository where the indexed metadata are located
     * @return the MetadataRepositoryIndex instance
     * @throws RepositoryIndexException
     */
    MetadataRepositoryIndex createMetadataRepositoryIndex( String indexPath, ArtifactRepository repository )
        throws RepositoryIndexException;

    /**
     * Method to create an instance of RepositoryIndexSearchLayer
     *
     * @param index the RepositoryIndex object where the query string will be searched
     * @return the RepositoryIndexSearchLayer instance
     */
    RepositoryIndexSearchLayer createRepositoryIndexSearchLayer( RepositoryIndex index );

    /**
     * Method to create an instance of DefaultRepositoryIndexSearcher
     *
     * @param index the RepositoryIndex object where the query string will be searched
     * @return the DefaultRepositoryIndexSearcher instance
     */
    DefaultRepositoryIndexSearcher createDefaultRepositoryIndexSearcher( RepositoryIndex index );
}
