package org.apache.maven.archiva.repository.scanner.functors;

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

import org.apache.commons.collections.Closure;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.codehaus.plexus.logging.Logger;

/**
 * TriggerBeginScanClosure 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class TriggerBeginScanClosure
    implements Closure
{
    private ArchivaRepository repository;

    private Logger logger;

    public TriggerBeginScanClosure( ArchivaRepository repository, Logger logger )
    {
        this.repository = repository;
        this.logger = logger;
    }

    public void execute( Object input )
    {
        if ( input instanceof RepositoryContentConsumer )
        {
            RepositoryContentConsumer consumer = (RepositoryContentConsumer) input;

            try
            {
                consumer.beginScan( repository );
            }
            catch ( ConsumerException e )
            {
                logger.warn( "Consumer [" + consumer.getId() + "] cannot begin: " + e.getMessage(), e );
            }
        }
    }
}