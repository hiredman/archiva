package org.apache.archiva.metadata.model;

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

import java.util.ArrayList;
import java.util.List;

public class ProjectVersionMetadata
    extends FacetedMetadata
{
    private String id;

    private String url;

    private String name;

    private String description;

    private Organization organization;

    private IssueManagement issueManagement;

    private Scm scm;

    private CiManagement ciManagement;

    private List<License> licenses = new ArrayList<License>();

    private List<MailingList> mailingLists = new ArrayList<MailingList>();

    private List<Dependency> dependencies = new ArrayList<Dependency>();

    private boolean incomplete;

    public String getId()
    {
        return id;
    }

    public String getVersion()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public String getUrl()
    {
        return url;
    }

    public String getName()
    {
        return name;
    }

    public Organization getOrganization()
    {
        return organization;
    }

    public void setOrganization( Organization organization )
    {
        this.organization = organization;
    }

    public IssueManagement getIssueManagement()
    {
        return issueManagement;
    }

    public void setIssueManagement( IssueManagement issueManagement )
    {
        this.issueManagement = issueManagement;
    }

    public Scm getScm()
    {
        return scm;
    }

    public void setScm( Scm scm )
    {
        this.scm = scm;
    }

    public CiManagement getCiManagement()
    {
        return ciManagement;
    }

    public void setCiManagement( CiManagement ciManagement )
    {
        this.ciManagement = ciManagement;
    }

    public List<License> getLicenses()
    {
        return licenses;
    }

    public void setLicenses( List<License> licenses )
    {
        this.licenses = licenses;
    }

    public void addLicense( License license )
    {
        this.licenses.add( license );
    }

    public void setMailingLists( List<MailingList> mailingLists )
    {
        this.mailingLists = mailingLists;
    }

    public List<MailingList> getMailingLists()
    {
        return mailingLists;
    }

    public void addMailingList( MailingList mailingList )
    {
        this.mailingLists.add( mailingList );
    }

    public void setDependencies( List<Dependency> dependencies )
    {
        this.dependencies = dependencies;
    }

    public List<Dependency> getDependencies()
    {
        return dependencies;
    }

    public void addDependency( Dependency dependency )
    {
        this.dependencies.add( dependency );
    }

    public boolean isIncomplete()
    {
        return incomplete;
    }

    public void setIncomplete( boolean incomplete )
    {
        this.incomplete = incomplete;
    }
}
