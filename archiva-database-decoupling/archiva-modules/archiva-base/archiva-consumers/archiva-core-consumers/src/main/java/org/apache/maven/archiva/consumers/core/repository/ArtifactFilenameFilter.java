package org.apache.maven.archiva.consumers.core.repository;

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

import java.io.FilenameFilter;
import java.io.File;

/**
 * Filename filter for getting all the files related to a specific artifact.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class ArtifactFilenameFilter
    implements FilenameFilter
{
    private String filename;

    public ArtifactFilenameFilter()
    {

    }

    public ArtifactFilenameFilter( String filename )
    {
        this.filename = filename;
    }
    
    public boolean accept( File dir, String name )
    {
        return ( name.startsWith( filename ) );
    }
}