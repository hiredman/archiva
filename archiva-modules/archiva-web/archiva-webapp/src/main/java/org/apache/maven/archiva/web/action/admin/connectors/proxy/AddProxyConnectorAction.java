package org.apache.maven.archiva.web.action.admin.connectors.proxy;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;

/**
 * AddProxyConnectorAction 
 *
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="addProxyConnectorAction" instantiation-strategy="per-lookup"
 */
public class AddProxyConnectorAction
    extends AbstractProxyConnectorFormAction
{
    @Override
    public void prepare()
    {
        super.prepare();
        connector = new ProxyConnectorConfiguration();
    }
    
    @Override
    public String input()
    {
        if( connector != null )
        {
         // MRM-1135
            connector.setBlackListPatterns( escapePatterns( connector.getBlackListPatterns() ) );
            connector.setWhiteListPatterns( escapePatterns( connector.getWhiteListPatterns() ) );
        }
        
        return INPUT;
    }

    public String commit()
    {
        /* Too complex for webwork's ${Action}-validation.xml techniques.
         * Not appropriate for use with webwork's implements Validatable, as that validates regardless of
         * the request method, such as .addProperty() or .addWhiteList().
         * 
         * This validation is ultimately only useful on this one request method.
         */
        String sourceId = connector.getSourceRepoId();
        String targetId = connector.getTargetRepoId();

        ProxyConnectorConfiguration otherConnector = findProxyConnector( sourceId, targetId );
        if ( otherConnector != null )
        {
            addActionError( "Unable to add proxy connector, as one already exists with source repository id ["
                + sourceId + "] and target repository id [" + targetId + "]." );
        }
        
        validateConnector();

        if ( hasActionErrors() )
        {
            return INPUT;
        }
        
        if( StringUtils.equals( DIRECT_CONNECTION, connector.getProxyId() ) )
        {
            connector.setProxyId( null );
        }

        // MRM-1135
        connector.setBlackListPatterns( unescapePatterns( connector.getBlackListPatterns() ) );
        connector.setWhiteListPatterns( unescapePatterns( connector.getWhiteListPatterns() ) );
        
        addProxyConnector( connector );
        return saveConfiguration();
    }
}
