package org.apache.maven.archiva.web.action;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.RepositorySearchException;
import org.apache.archiva.indexer.search.SearchFields;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.struts2.ServletActionContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Search all indexed fields by the given criteria.
 *
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="searchAction" instantiation-strategy="per-lookup"
 */
public class SearchAction
    extends AbstractRepositoryBasedAction
    implements Preparable
{
    /**
     * Query string.
     */

    private ArchivaConfiguration archivaConfiguration;

    private String q;

    /**
     * The Search Results.
     */
    private SearchResults results;

    private static final String RESULTS = "results";

    private static final String ARTIFACT = "artifact";

    private List<ArtifactMetadata> databaseResults;

    private int currentPage = 0;

    private int totalPages;

    private boolean searchResultsOnly;

    private String completeQueryString;

    private static final String COMPLETE_QUERY_STRING_SEPARATOR = ";";

    private List<String> managedRepositoryList;

    private String groupId;

    private String artifactId;

    private String version;

    private String className;

    private int rowCount = 30;

    private String repositoryId;

    private boolean fromFilterSearch;

    private boolean filterSearch = false;

    private boolean fromResultsPage;

    private RepositorySearch nexusSearch;

    private Map<String, String> searchFields;

    private String infoMessage;

    public boolean isFromResultsPage()
    {
        return fromResultsPage;
    }

    public void setFromResultsPage( boolean fromResultsPage )
    {
        this.fromResultsPage = fromResultsPage;
    }

    public boolean isFromFilterSearch()
    {
        return fromFilterSearch;
    }

    public void setFromFilterSearch( boolean fromFilterSearch )
    {
        this.fromFilterSearch = fromFilterSearch;
    }

    public void prepare()
    {
        managedRepositoryList = new ArrayList<String>();
        managedRepositoryList = getObservableRepos();

        if ( managedRepositoryList.size() > 0 )
        {
            managedRepositoryList.add( "all" );
        }

        searchFields = new LinkedHashMap<String, String>();
        searchFields.put( "groupId", "Group ID" );
        searchFields.put( "artifactId", "Artifact ID" );
        searchFields.put( "version", "Version" );
        searchFields.put( "className", "Class/Package Name" );
        searchFields.put( "rowCount", "Row Count" );

        super.clearErrorsAndMessages();
        clearSearchFields();
    }

    private void clearSearchFields()
    {
        repositoryId = "";
        artifactId = "";
        groupId = "";
        version = "";
        className = "";
        rowCount = 30;
        currentPage = 0;
    }

    // advanced search MRM-90 -- filtered search
    public String filteredSearch()
        throws MalformedURLException
    {
        if ( ( groupId == null || "".equals( groupId ) ) && ( artifactId == null || "".equals( artifactId ) ) &&
            ( className == null || "".equals( className ) ) && ( version == null || "".equals( version ) ) )
        {
            addActionError( "Advanced Search - At least one search criteria must be provided." );
            return INPUT;
        }

        fromFilterSearch = true;

        if ( CollectionUtils.isEmpty( managedRepositoryList ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        SearchResultLimits limits = new SearchResultLimits( currentPage );
        limits.setPageSize( rowCount );
        List<String> selectedRepos = new ArrayList<String>();

        if ( repositoryId == null || StringUtils.isBlank( repositoryId ) || "all".equals( StringUtils.stripToEmpty(
            repositoryId ) ) )
        {
            selectedRepos = getObservableRepos();
        }
        else
        {
            selectedRepos.add( repositoryId );
        }

        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        SearchFields searchFields = new SearchFields( groupId, artifactId, version, null, className, selectedRepos );

        // TODO: add packaging in the list of fields for advanced search (UI)?
        try
        {
            results = getNexusSearch().search( getPrincipal(), searchFields, limits );
        }
        catch ( RepositorySearchException e )
        {
            addActionError( e.getMessage() );
            return ERROR;
        }

        if ( results.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }

        totalPages = results.getTotalHits() / limits.getPageSize();

        if ( ( results.getTotalHits() % limits.getPageSize() ) != 0 )
        {
            totalPages = totalPages + 1;
        }

        for ( SearchResultHit hit : results.getHits() )
        {
            final String version = hit.getVersion();
            if ( version != null )
            {
                hit.setVersion( VersionUtil.getBaseVersion( version ) );
            }
        }

        return SUCCESS;
    }

    @SuppressWarnings( "unchecked" )
    public String quickSearch()
        throws MalformedURLException
    {
        /* TODO: give action message if indexing is in progress.
         * This should be based off a count of 'unprocessed' artifacts.
         * This (yet to be written) routine could tell the user that X (unprocessed) artifacts are not yet
         * present in the full text search.
         */

        assert q != null && q.length() != 0;

        fromFilterSearch = false;

        SearchResultLimits limits = new SearchResultLimits( currentPage );

        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty( selectedRepos ) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        try
        {
            if ( searchResultsOnly && !completeQueryString.equals( "" ) )
            {
                results = getNexusSearch().search( getPrincipal(), selectedRepos, q, limits,
                                                   parseCompleteQueryString() );
            }
            else
            {
                completeQueryString = "";
                results = getNexusSearch().search( getPrincipal(), selectedRepos, q, limits, null );
            }
        }
        catch ( RepositorySearchException e )
        {
            addActionError( e.getMessage() );
            return ERROR;
        }

        if ( results.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }

        totalPages = results.getTotalHits() / limits.getPageSize();

        if ( ( results.getTotalHits() % limits.getPageSize() ) != 0 )
        {
            totalPages = totalPages + 1;
        }

        if ( !isEqualToPreviousSearchTerm( q ) )
        {
            buildCompleteQueryString( q );
        }

        return SUCCESS;
    }

    public String findArtifact()
        throws Exception
    {
        // TODO: give action message if indexing is in progress

        if ( StringUtils.isBlank( q ) )
        {
            addActionError( "Unable to search for a blank checksum" );
            return INPUT;
        }

        databaseResults = new ArrayList<ArtifactMetadata>();
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            for ( String repoId : getObservableRepos() )
            {
                databaseResults.addAll( metadataRepository.getArtifactsByChecksum( repoId, q ) );
            }
        }
        finally
        {
            repositorySession.close();
        }

        if ( databaseResults.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }

        if ( databaseResults.size() == 1 )
        {
            // 1 hit? return it's information directly!
            return ARTIFACT;
        }

        return RESULTS;
    }

    public String doInput()
    {
        return INPUT;
    }

    private void buildCompleteQueryString( String searchTerm )
    {
        if ( searchTerm.indexOf( COMPLETE_QUERY_STRING_SEPARATOR ) != -1 )
        {
            searchTerm = StringUtils.remove( searchTerm, COMPLETE_QUERY_STRING_SEPARATOR );
        }

        if ( completeQueryString == null || "".equals( completeQueryString ) )
        {
            completeQueryString = searchTerm;
        }
        else
        {
            completeQueryString = completeQueryString + COMPLETE_QUERY_STRING_SEPARATOR + searchTerm;
        }
    }

    private List<String> parseCompleteQueryString()
    {
        List<String> parsedCompleteQueryString = new ArrayList<String>();
        String[] parsed = StringUtils.split( completeQueryString, COMPLETE_QUERY_STRING_SEPARATOR );
        CollectionUtils.addAll( parsedCompleteQueryString, parsed );

        return parsedCompleteQueryString;
    }

    private boolean isEqualToPreviousSearchTerm( String searchTerm )
    {
        if ( !"".equals( completeQueryString ) )
        {
            String[] parsed = StringUtils.split( completeQueryString, COMPLETE_QUERY_STRING_SEPARATOR );
            if ( StringUtils.equalsIgnoreCase( searchTerm, parsed[parsed.length - 1] ) )
            {
                return true;
            }
        }

        return false;
    }

    public String getQ()
    {
        return q;
    }

    public void setQ( String q )
    {
        this.q = q;
    }

    public SearchResults getResults()
    {
        return results;
    }

    public List<ArtifactMetadata> getDatabaseResults()
    {
        return databaseResults;
    }

    public void setCurrentPage( int page )
    {
        this.currentPage = page;
    }

    public int getCurrentPage()
    {
        return currentPage;
    }

    public int getTotalPages()
    {
        return totalPages;
    }

    public void setTotalPages( int totalPages )
    {
        this.totalPages = totalPages;
    }

    public boolean isSearchResultsOnly()
    {
        return searchResultsOnly;
    }

    public void setSearchResultsOnly( boolean searchResultsOnly )
    {
        this.searchResultsOnly = searchResultsOnly;
    }

    public String getCompleteQueryString()
    {
        return completeQueryString;
    }

    public void setCompleteQueryString( String completeQueryString )
    {
        this.completeQueryString = completeQueryString;
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public Map<String, ManagedRepositoryConfiguration> getManagedRepositories()
    {
        return getArchivaConfiguration().getConfiguration().getManagedRepositoriesAsMap();
    }

    public void setManagedRepositories( Map<String, ManagedRepositoryConfiguration> managedRepositories )
    {
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public int getRowCount()
    {
        return rowCount;
    }

    public void setRowCount( int rowCount )
    {
        this.rowCount = rowCount;
    }

    public boolean isFilterSearch()
    {
        return filterSearch;
    }

    public void setFilterSearch( boolean filterSearch )
    {
        this.filterSearch = filterSearch;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public List<String> getManagedRepositoryList()
    {
        return managedRepositoryList;
    }

    public void setManagedRepositoryList( List<String> managedRepositoryList )
    {
        this.managedRepositoryList = managedRepositoryList;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName( String className )
    {
        this.className = className;
    }

    public RepositorySearch getNexusSearch()
    {
        // no need to do this when wiring is already in spring
        if ( nexusSearch == null )
        {
            WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(
                ServletActionContext.getServletContext() );
            nexusSearch = (RepositorySearch) wac.getBean( "nexusSearch" );
        }
        return nexusSearch;
    }

    public void setNexusSearch( RepositorySearch nexusSearch )
    {
        this.nexusSearch = nexusSearch;
    }

    public Map<String, String> getSearchFields()
    {
        return searchFields;
    }

    public void setSearchFields( Map<String, String> searchFields )
    {
        this.searchFields = searchFields;
    }

    public String getInfoMessage()
    {
        return infoMessage;
    }

    public void setInfoMessage( String infoMessage )
    {
        this.infoMessage = infoMessage;
    }
}
