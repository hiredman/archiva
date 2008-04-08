package org.apache.maven.archiva.layer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Gets the preferred implementation of a repository query layer for the given repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id:RepositoryQueryLayerFactory.java 437105 2006-08-26 17:22:22 +1000 (Sat, 26 Aug 2006) brett $
 */
public interface RepositoryQueryLayerFactory
{
    String ROLE = RepositoryQueryLayerFactory.class.getName();

    /**
     * Create or obtain a query interface.
     *
     * @param repository the repository to query
     * @return the obtained query layer
     */
    RepositoryQueryLayer createRepositoryQueryLayer( ArtifactRepository repository );
}