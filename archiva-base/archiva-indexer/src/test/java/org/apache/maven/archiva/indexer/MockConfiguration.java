package org.apache.maven.archiva.indexer;

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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.registry.RegistryListener;
import org.easymock.MockControl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MockConfiguration 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.configuration.ArchivaConfiguration"
 *                   role-hint="mock"
 */
public class MockConfiguration implements ArchivaConfiguration
{
    private Configuration configuration = new Configuration();

    private List listeners = new ArrayList();

    private MockControl registryControl;

    private Registry registryMock;

    public MockConfiguration()
    {
        registryControl = MockControl.createNiceControl( Registry.class );
        registryMock = (Registry) registryControl.getMock();
    }

    public void addChangeListener( RegistryListener listener )
    {
        listeners.add( listener );
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public void save( Configuration configuration )
        throws RegistryException
    {
        /* do nothing */
    }

    public void triggerChange( String name, String value )
    {
        Iterator it = listeners.iterator();
        while ( it.hasNext() )
        {
            RegistryListener listener = (RegistryListener) it.next();
            try
            {
                listener.afterConfigurationChange( registryMock, name, value );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }
}