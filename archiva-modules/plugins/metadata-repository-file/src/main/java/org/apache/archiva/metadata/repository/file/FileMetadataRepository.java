package org.apache.archiva.metadata.repository.file;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.model.Scm;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.MetadataRepository"
 */
public class FileMetadataRepository
    implements MetadataRepository
{
    /**
     * TODO: this isn't suitable for production use
     *
     * @plexus.configuration
     */
    private File directory = new File( System.getProperty( "user.home" ), ".archiva-metadata" );

    /**
     * @plexus.requirement role="org.apache.archiva.metadata.model.MetadataFacetFactory"
     */
    private Map<String, MetadataFacetFactory> metadataFacetFactories;

    private static final Logger log = LoggerFactory.getLogger( FileMetadataRepository.class );

    private static final String PROJECT_METADATA_KEY = "project-metadata";

    private static final String PROJECT_VERSION_METADATA_KEY = "version-metadata";

    private static final String NAMESPACE_METADATA_KEY = "namespace-metadata";

    private static final String METADATA_KEY = "metadata";

    public void updateProject( String repoId, ProjectMetadata project )
    {
        updateProject( repoId, project.getNamespace(), project.getId() );
    }

    private void updateProject( String repoId, String namespace, String id )
    {
        // TODO: this is a more braindead implementation than we would normally expect, for prototyping purposes
        updateNamespace( repoId, namespace );

        try
        {
            File namespaceDirectory = new File( this.directory, repoId + "/" + namespace );
            Properties properties = new Properties();
            properties.setProperty( "namespace", namespace );
            properties.setProperty( "id", id );
            writeProperties( properties, new File( namespaceDirectory, id ), PROJECT_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO!
            e.printStackTrace();
        }
    }

    public void updateProjectVersion( String repoId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
    {
        updateProject( repoId, namespace, projectId );

        File directory =
            new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + versionMetadata.getId() );

        Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );
        // remove properties that are not references or artifacts
        for ( String name : properties.stringPropertyNames() )
        {
            if ( !name.contains( ":" ) && !name.equals( "facetIds" ) )
            {
                properties.remove( name );
            }
        }
        properties.setProperty( "id", versionMetadata.getId() );
        setProperty( properties, "name", versionMetadata.getName() );
        setProperty( properties, "description", versionMetadata.getDescription() );
        setProperty( properties, "url", versionMetadata.getUrl() );
        if ( versionMetadata.getScm() != null )
        {
            setProperty( properties, "scm.connection", versionMetadata.getScm().getConnection() );
            setProperty( properties, "scm.developerConnection", versionMetadata.getScm().getDeveloperConnection() );
            setProperty( properties, "scm.url", versionMetadata.getScm().getUrl() );
        }
        if ( versionMetadata.getCiManagement() != null )
        {
            setProperty( properties, "ci.system", versionMetadata.getCiManagement().getSystem() );
            setProperty( properties, "ci.url", versionMetadata.getCiManagement().getUrl() );
        }
        if ( versionMetadata.getIssueManagement() != null )
        {
            setProperty( properties, "issue.system", versionMetadata.getIssueManagement().getSystem() );
            setProperty( properties, "issue.url", versionMetadata.getIssueManagement().getUrl() );
        }
        if ( versionMetadata.getOrganization() != null )
        {
            setProperty( properties, "org.name", versionMetadata.getOrganization().getName() );
            setProperty( properties, "org.url", versionMetadata.getOrganization().getUrl() );
        }
        int i = 0;
        for ( License license : versionMetadata.getLicenses() )
        {
            setProperty( properties, "license." + i + ".name", license.getName() );
            setProperty( properties, "license." + i + ".url", license.getUrl() );
            i++;
        }
        i = 0;
        for ( MailingList mailingList : versionMetadata.getMailingLists() )
        {
            setProperty( properties, "mailingList." + i + ".archive", mailingList.getMainArchiveUrl() );
            setProperty( properties, "mailingList." + i + ".name", mailingList.getName() );
            setProperty( properties, "mailingList." + i + ".post", mailingList.getPostAddress() );
            setProperty( properties, "mailingList." + i + ".unsubscribe", mailingList.getUnsubscribeAddress() );
            setProperty( properties, "mailingList." + i + ".subscribe", mailingList.getSubscribeAddress() );
            setProperty( properties, "mailingList." + i + ".otherArchives", join( mailingList.getOtherArchives() ) );
            i++;
        }
        i = 0;
        for ( Dependency dependency : versionMetadata.getDependencies() )
        {
            setProperty( properties, "dependency." + i + ".classifier", dependency.getClassifier() );
            setProperty( properties, "dependency." + i + ".scope", dependency.getScope() );
            setProperty( properties, "dependency." + i + ".systemPath", dependency.getSystemPath() );
            setProperty( properties, "dependency." + i + ".artifactId", dependency.getArtifactId() );
            setProperty( properties, "dependency." + i + ".groupId", dependency.getGroupId() );
            setProperty( properties, "dependency." + i + ".version", dependency.getVersion() );
            setProperty( properties, "dependency." + i + ".type", dependency.getType() );
            i++;
        }
        Set<String> facetIds = new LinkedHashSet<String>( versionMetadata.getFacetIds() );
        facetIds.addAll( Arrays.asList( properties.getProperty( "facetIds", "" ).split( "," ) ) );
        properties.setProperty( "facetIds", join( facetIds ) );

        for ( MetadataFacet facet : versionMetadata.getFacetList() )
        {
            properties.putAll( facet.toProperties() );
        }

        try
        {
            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void updateProjectReference( String repoId, String namespace, String projectId, String projectVersion,
                                        ProjectVersionReference reference )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );
        int i = Integer.valueOf( properties.getProperty( "ref:lastReferenceNum", "-1" ) ) + 1;
        setProperty( properties, "ref:lastReferenceNum", Integer.toString( i ) );
        setProperty( properties, "ref:reference." + i + ".namespace", reference.getNamespace() );
        setProperty( properties, "ref:reference." + i + ".projectId", reference.getProjectId() );
        setProperty( properties, "ref:reference." + i + ".projectVersion", reference.getProjectVersion() );
        setProperty( properties, "ref:reference." + i + ".referenceType", reference.getReferenceType().toString() );

        try
        {
            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void updateNamespace( String repoId, String namespace )
    {
        try
        {
            File namespaceDirectory = new File( this.directory, repoId + "/" + namespace );
            Properties properties = new Properties();
            properties.setProperty( "namespace", namespace );
            writeProperties( properties, namespaceDirectory, NAMESPACE_METADATA_KEY );

        }
        catch ( IOException e )
        {
            // TODO!
            e.printStackTrace();
        }
    }

    public List<String> getMetadataFacets( String repoId, String facetId )
    {
        File directory = getMetadataDirectory( repoId, facetId );
        List<String> facets = new ArrayList<String>();
        recurse( facets, "", directory );
        return facets;
    }

    private void recurse( List<String> facets, String prefix, File directory )
    {
        File[] list = directory.listFiles();
        if ( list != null )
        {
            for ( File dir : list )
            {
                if ( dir.isDirectory() )
                {
                    recurse( facets, prefix + "/" + dir.getName(), dir );
                }
                else if ( dir.getName().equals( METADATA_KEY + ".properties" ) )
                {
                    facets.add( prefix.substring( 1 ) );
                }
            }
        }
    }

    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
    {
        Properties properties;
        try
        {
            properties =
                readProperties( new File( getMetadataDirectory( repositoryId, facetId ), name ), METADATA_KEY );
        }
        catch ( FileNotFoundException e )
        {
            return null;
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
        MetadataFacet metadataFacet = null;
        MetadataFacetFactory metadataFacetFactory = metadataFacetFactories.get( facetId );
        if ( metadataFacetFactory != null )
        {
            metadataFacet = metadataFacetFactory.createMetadataFacet();
            Map<String, String> map = new HashMap<String, String>();
            for ( String key : properties.stringPropertyNames() )
            {
                map.put( key, properties.getProperty( key ) );
            }
            metadataFacet.fromProperties( map );
        }
        return metadataFacet;
    }

    public void addMetadataFacet( String repositoryId, String facetId, MetadataFacet metadataFacet )
    {
        Properties properties = new Properties();
        properties.putAll( metadataFacet.toProperties() );

        try
        {
            writeProperties( properties,
                             new File( getMetadataDirectory( repositoryId, facetId ), metadataFacet.getName() ),
                             METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO!
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void removeMetadataFacets( String repositoryId, String facetId )
    {
        try
        {
            FileUtils.deleteDirectory( getMetadataDirectory( repositoryId, facetId ) );
        }
        catch ( IOException e )
        {
            // TODO!
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void removeMetadataFacet( String repoId, String facetId, String name )
    {
        File dir = new File( getMetadataDirectory( repoId, facetId ), name );
        try
        {
            FileUtils.deleteDirectory( dir );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date startTime, Date endTime )
    {
        // TODO: this is quite slow - if we are to persist with this repository implementation we should build an index
        //  of this information (eg. in Lucene, as before)

        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();
        for ( String ns : getRootNamespaces( repoId ) )
        {
            getArtifactsByDateRange( artifacts, repoId, ns, startTime, endTime );
        }
        return artifacts;
    }

    private void getArtifactsByDateRange( List<ArtifactMetadata> artifacts, String repoId, String ns, Date startTime,
                                          Date endTime )
    {
        for ( String namespace : getNamespaces( repoId, ns ) )
        {
            getArtifactsByDateRange( artifacts, repoId, ns + "." + namespace, startTime, endTime );
        }

        for ( String project : getProjects( repoId, ns ) )
        {
            for ( String version : getProjectVersions( repoId, ns, project ) )
            {
                for ( ArtifactMetadata artifact : getArtifacts( repoId, ns, project, version ) )
                {
                    if ( startTime == null || startTime.before( artifact.getWhenGathered() ) )
                    {
                        if ( endTime == null || endTime.after( artifact.getWhenGathered() ) )
                        {
                            artifacts.add( artifact );
                        }
                    }
                }
            }
        }
    }

    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                      String projectVersion )
    {
        Map<String, ArtifactMetadata> artifacts = new HashMap<String, ArtifactMetadata>();

        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );

        for ( Map.Entry entry : properties.entrySet() )
        {
            String name = (String) entry.getKey();
            StringTokenizer tok = new StringTokenizer( name, ":" );
            if ( tok.hasMoreTokens() && "artifact".equals( tok.nextToken() ) )
            {
                String field = tok.nextToken();
                String id = tok.nextToken();

                ArtifactMetadata artifact = artifacts.get( id );
                if ( artifact == null )
                {
                    artifact = new ArtifactMetadata();
                    artifact.setRepositoryId( repoId );
                    artifact.setNamespace( namespace );
                    artifact.setProject( projectId );
                    artifact.setVersion( projectVersion );
                    artifact.setId( id );
                    artifacts.put( id, artifact );
                }

                String value = (String) entry.getValue();
                if ( "updated".equals( field ) )
                {
                    artifact.setFileLastModified( Long.valueOf( value ) );
                }
                else if ( "size".equals( field ) )
                {
                    artifact.setSize( Long.valueOf( value ) );
                }
                else if ( "whenGathered".equals( field ) )
                {
                    artifact.setWhenGathered( new Date( Long.valueOf( value ) ) );
                }
                else if ( "version".equals( field ) )
                {
                    artifact.setVersion( value );
                }
                else if ( "md5".equals( field ) )
                {
                    artifact.setMd5( value );
                }
                else if ( "sha1".equals( field ) )
                {
                    artifact.setSha1( value );
                }
            }
        }
        return artifacts.values();
    }

    public Collection<String> getRepositories()
    {
        String[] repoIds = this.directory.list();
        return repoIds != null ? Arrays.asList( repoIds ) : Collections.<String>emptyList();
    }

    public List<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum )
    {
        // TODO: this is quite slow - if we are to persist with this repository implementation we should build an index
        //  of this information (eg. in Lucene, as before)
        // alternatively, we could build a referential tree in the content repository, however it would need some levels
        // of depth to avoid being too broad to be useful (eg. /repository/checksums/a/ab/abcdef1234567)

        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();
        for ( String ns : getRootNamespaces( repositoryId ) )
        {
            getArtifactsByChecksum( artifacts, repositoryId, ns, checksum );
        }
        return artifacts;
    }

    private void getArtifactsByChecksum( List<ArtifactMetadata> artifacts, String repositoryId, String ns,
                                         String checksum )
    {
        for ( String namespace : getNamespaces( repositoryId, ns ) )
        {
            getArtifactsByChecksum( artifacts, repositoryId, ns + "." + namespace, checksum );
        }

        for ( String project : getProjects( repositoryId, ns ) )
        {
            for ( String version : getProjectVersions( repositoryId, ns, project ) )
            {
                for ( ArtifactMetadata artifact : getArtifacts( repositoryId, ns, project, version ) )
                {
                    if ( checksum.equals( artifact.getMd5() ) || checksum.equals( artifact.getSha1() ) )
                    {
                        artifacts.add( artifact );
                    }
                }
            }
        }
    }

    private File getMetadataDirectory( String repositoryId, String facetId )
    {
        return new File( this.directory, repositoryId + "/.meta/" + facetId );
    }

    private String join( Collection<String> ids )
    {
        if ( !ids.isEmpty() )
        {
            StringBuilder s = new StringBuilder();
            for ( String id : ids )
            {
                s.append( id );
                s.append( "," );
            }
            return s.substring( 0, s.length() - 1 );
        }
        return "";
    }

    private void setProperty( Properties properties, String name, String value )
    {
        if ( value != null )
        {
            properties.setProperty( name, value );
        }
    }

    public void updateArtifact( String repoId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifact )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );

        properties.setProperty( "artifact:updated:" + artifact.getId(),
                                Long.toString( artifact.getFileLastModified().getTime() ) );
        properties.setProperty( "artifact:whenGathered:" + artifact.getId(),
                                Long.toString( artifact.getWhenGathered().getTime() ) );
        properties.setProperty( "artifact:size:" + artifact.getId(), Long.toString( artifact.getSize() ) );
        if ( artifact.getMd5() != null )
        {
            properties.setProperty( "artifact:md5:" + artifact.getId(), artifact.getMd5() );
        }
        if ( artifact.getSha1() != null )
        {
            properties.setProperty( "artifact:sha1:" + artifact.getId(), artifact.getSha1() );
        }
        properties.setProperty( "artifact:version:" + artifact.getId(), artifact.getVersion() );

        try
        {
            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private Properties readOrCreateProperties( File directory, String propertiesKey )
    {
        try
        {
            return readProperties( directory, propertiesKey );
        }
        catch ( FileNotFoundException e )
        {
            // ignore and return new properties
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return new Properties();
    }

    private Properties readProperties( File directory, String propertiesKey )
        throws IOException
    {
        Properties properties = new Properties();
        FileInputStream in = null;
        try
        {
            in = new FileInputStream( new File( directory, propertiesKey + ".properties" ) );
            properties.load( in );
        }
        finally
        {
            IOUtils.closeQuietly( in );
        }
        return properties;
    }

    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId );

        Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );

        ProjectMetadata project = new ProjectMetadata();
        project.setNamespace( properties.getProperty( "namespace" ) );
        project.setId( properties.getProperty( "id" ) );
        return project;
    }

    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );
        String id = properties.getProperty( "id" );
        ProjectVersionMetadata versionMetadata = null;
        if ( id != null )
        {
            versionMetadata = new ProjectVersionMetadata();
            versionMetadata.setId( id );
            versionMetadata.setName( properties.getProperty( "name" ) );
            versionMetadata.setDescription( properties.getProperty( "description" ) );
            versionMetadata.setUrl( properties.getProperty( "url" ) );

            String scmConnection = properties.getProperty( "scm.connection" );
            String scmDeveloperConnection = properties.getProperty( "scm.developerConnection" );
            String scmUrl = properties.getProperty( "scm.url" );
            if ( scmConnection != null || scmDeveloperConnection != null || scmUrl != null )
            {
                Scm scm = new Scm();
                scm.setConnection( scmConnection );
                scm.setDeveloperConnection( scmDeveloperConnection );
                scm.setUrl( scmUrl );
                versionMetadata.setScm( scm );
            }

            String ciSystem = properties.getProperty( "ci.system" );
            String ciUrl = properties.getProperty( "ci.url" );
            if ( ciSystem != null || ciUrl != null )
            {
                CiManagement ci = new CiManagement();
                ci.setSystem( ciSystem );
                ci.setUrl( ciUrl );
                versionMetadata.setCiManagement( ci );
            }

            String issueSystem = properties.getProperty( "issue.system" );
            String issueUrl = properties.getProperty( "issue.url" );
            if ( issueSystem != null || issueUrl != null )
            {
                IssueManagement issueManagement = new IssueManagement();
                issueManagement.setSystem( issueSystem );
                issueManagement.setUrl( issueUrl );
                versionMetadata.setIssueManagement( issueManagement );
            }

            String orgName = properties.getProperty( "org.name" );
            String orgUrl = properties.getProperty( "org.url" );
            if ( orgName != null || orgUrl != null )
            {
                Organization org = new Organization();
                org.setName( orgName );
                org.setUrl( orgUrl );
                versionMetadata.setOrganization( org );
            }

            boolean done = false;
            int i = 0;
            while ( !done )
            {
                String licenseName = properties.getProperty( "license." + i + ".name" );
                String licenseUrl = properties.getProperty( "license." + i + ".url" );
                if ( licenseName != null || licenseUrl != null )
                {
                    License license = new License();
                    license.setName( licenseName );
                    license.setUrl( licenseUrl );
                    versionMetadata.addLicense( license );
                }
                else
                {
                    done = true;
                }
                i++;
            }

            done = false;
            i = 0;
            while ( !done )
            {
                String mailingListName = properties.getProperty( "mailingList." + i + ".name" );
                if ( mailingListName != null )
                {
                    MailingList mailingList = new MailingList();
                    mailingList.setName( mailingListName );
                    mailingList.setMainArchiveUrl( properties.getProperty( "mailingList." + i + ".archive" ) );
                    mailingList.setOtherArchives(
                        Arrays.asList( properties.getProperty( "mailingList." + i + ".otherArchives" ).split( "," ) ) );
                    mailingList.setPostAddress( properties.getProperty( "mailingList." + i + ".post" ) );
                    mailingList.setSubscribeAddress( properties.getProperty( "mailingList." + i + ".subscribe" ) );
                    mailingList.setUnsubscribeAddress( properties.getProperty( "mailingList." + i + ".unsubscribe" ) );
                    versionMetadata.addMailingList( mailingList );
                }
                else
                {
                    done = true;
                }
                i++;
            }

            done = false;
            i = 0;
            while ( !done )
            {
                String dependencyArtifactId = properties.getProperty( "dependency." + i + ".artifactId" );
                if ( dependencyArtifactId != null )
                {
                    Dependency dependency = new Dependency();
                    dependency.setArtifactId( dependencyArtifactId );
                    dependency.setGroupId( properties.getProperty( "dependency." + i + ".groupId" ) );
                    dependency.setClassifier( properties.getProperty( "dependency." + i + ".classifier" ) );
                    dependency.setOptional(
                        Boolean.valueOf( properties.getProperty( "dependency." + i + ".optional" ) ) );
                    dependency.setScope( properties.getProperty( "dependency." + i + ".scope" ) );
                    dependency.setSystemPath( properties.getProperty( "dependency." + i + ".systemPath" ) );
                    dependency.setType( properties.getProperty( "dependency." + i + ".type" ) );
                    dependency.setVersion( properties.getProperty( "dependency." + i + ".version" ) );
                    versionMetadata.addDependency( dependency );
                }
                else
                {
                    done = true;
                }
                i++;
            }

            for ( String facetId : properties.getProperty( "facetIds" ).split( "," ) )
            {
                MetadataFacetFactory factory = metadataFacetFactories.get( facetId );
                if ( factory == null )
                {
                    log.error( "Attempted to load unknown metadata facet: " + facetId );
                }
                else
                {
                    MetadataFacet facet = factory.createMetadataFacet();
                    Map<String, String> map = new HashMap<String, String>();
                    for ( String key : properties.stringPropertyNames() )
                    {
                        if ( key.startsWith( facet.getFacetId() ) )
                        {
                            map.put( key, properties.getProperty( key ) );
                        }
                    }
                    facet.fromProperties( map );
                    versionMetadata.addFacet( facet );
                }
            }

            for ( MetadataFacet facet : versionMetadata.getFacetList() )
            {
                properties.putAll( facet.toProperties() );
            }
        }
        return versionMetadata;
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );

        Set<String> versions = new HashSet<String>();
        for ( Map.Entry entry : properties.entrySet() )
        {
            String name = (String) entry.getKey();
            if ( name.startsWith( "artifact:version:" ) )
            {
                versions.add( (String) entry.getValue() );
            }
        }
        return versions;
    }

    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readOrCreateProperties( directory, PROJECT_VERSION_METADATA_KEY );
        int numberOfRefs = Integer.valueOf( properties.getProperty( "ref:lastReferenceNum", "-1" ) ) + 1;

        List<ProjectVersionReference> references = new ArrayList<ProjectVersionReference>();
        for ( int i = 0; i < numberOfRefs; i++ )
        {
            ProjectVersionReference reference = new ProjectVersionReference();
            reference.setProjectId( properties.getProperty( "ref:reference." + i + ".projectId" ) );
            reference.setNamespace( properties.getProperty( "ref:reference." + i + ".namespace" ) );
            reference.setProjectVersion( properties.getProperty( "ref:reference." + i + ".projectVersion" ) );
            reference.setReferenceType( ProjectVersionReference.ReferenceType.valueOf(
                properties.getProperty( "ref:reference." + i + ".referenceType" ) ) );
            references.add( reference );
        }
        return references;
    }

    public Collection<String> getRootNamespaces( String repoId )
    {
        return getNamespaces( repoId, null );
    }

    public Collection<String> getNamespaces( String repoId, String baseNamespace )
    {
        List<String> allNamespaces = new ArrayList<String>();
        File directory = new File( this.directory, repoId );
        File[] files = directory.listFiles();
        if ( files != null )
        {
            for ( File namespace : files )
            {
                if ( new File( namespace, NAMESPACE_METADATA_KEY + ".properties" ).exists() )
                {
                    allNamespaces.add( namespace.getName() );
                }
            }
        }

        Set<String> namespaces = new LinkedHashSet<String>();
        int fromIndex = baseNamespace != null ? baseNamespace.length() + 1 : 0;
        for ( String namespace : allNamespaces )
        {
            if ( baseNamespace == null || namespace.startsWith( baseNamespace + "." ) )
            {
                int i = namespace.indexOf( '.', fromIndex );
                if ( i >= 0 )
                {
                    namespaces.add( namespace.substring( fromIndex, i ) );
                }
                else
                {
                    namespaces.add( namespace.substring( fromIndex ) );
                }
            }
        }
        return new ArrayList<String>( namespaces );
    }

    public Collection<String> getProjects( String repoId, String namespace )
    {
        List<String> projects = new ArrayList<String>();
        File directory = new File( this.directory, repoId + "/" + namespace );
        File[] files = directory.listFiles();
        if ( files != null )
        {
            for ( File project : files )
            {
                if ( new File( project, PROJECT_METADATA_KEY + ".properties" ).exists() )
                {
                    projects.add( project.getName() );
                }
            }
        }
        return projects;
    }

    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
    {
        List<String> projectVersions = new ArrayList<String>();
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId );
        File[] files = directory.listFiles();
        if ( files != null )
        {
            for ( File projectVersion : files )
            {
                if ( new File( projectVersion, PROJECT_VERSION_METADATA_KEY + ".properties" ).exists() )
                {
                    projectVersions.add( projectVersion.getName() );
                }
            }
        }
        return projectVersions;
    }

    private void writeProperties( Properties properties, File directory, String propertiesKey )
        throws IOException
    {
        directory.mkdirs();
        FileOutputStream os = new FileOutputStream( new File( directory, propertiesKey + ".properties" ) );
        try
        {
            properties.store( os, null );
        }
        finally
        {
            IOUtils.closeQuietly( os );
        }
    }

    public void setDirectory( File directory )
    {
        this.directory = directory;
    }

    public void setMetadataFacetFactories( Map<String, MetadataFacetFactory> metadataFacetFactories )
    {
        this.metadataFacetFactories = metadataFacetFactories;
    }
}