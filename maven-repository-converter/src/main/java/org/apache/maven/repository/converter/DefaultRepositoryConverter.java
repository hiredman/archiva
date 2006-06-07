package org.apache.maven.repository.converter;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.converter.ArtifactPomRewriter;
import org.apache.maven.model.converter.ModelConverter;
import org.apache.maven.model.converter.PomTranslationException;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.converter.transaction.FileTransaction;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.reporting.ArtifactReporter;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * Implementation of repository conversion class.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.repository.converter.RepositoryConverter" role-hint="default"
 */
public class DefaultRepositoryConverter
    implements RepositoryConverter
{
    /**
     * @plexus.requirement
     */
    private Digester digester;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private ArtifactPomRewriter rewriter;

    /**
     * @plexus.requirement
     */
    private ModelConverter translator;

    /**
     * @plexus.configuration default-value="false"
     */
    private boolean force;

    /**
     * @plexus.configuration default-value="false"
     */
    private boolean dryrun;

    /**
     * @plexus.requirement
     */
    private I18N i18n;

    public void convert( Artifact artifact, ArtifactRepository targetRepository, ArtifactReporter reporter )
        throws RepositoryConversionException
    {
        if ( artifact.getRepository().getUrl().equals( targetRepository.getUrl() ) )
        {
            throw new RepositoryConversionException( getI18NString( "exception.repositories.match" ) );
        }

        if ( validateMetadata( artifact, reporter ) )
        {
            FileTransaction transaction = new FileTransaction();

            if ( copyPom( artifact, targetRepository, reporter, transaction ) )
            {
                if ( copyArtifact( artifact, targetRepository, reporter, transaction ) )
                {
                    Metadata metadata = createBaseMetadata( artifact );
                    Versioning versioning = new Versioning();
                    versioning.addVersion( artifact.getBaseVersion() );
                    metadata.setVersioning( versioning );
                    updateMetadata( new ArtifactRepositoryMetadata( artifact ), targetRepository, metadata,
                                    transaction );

                    metadata = createBaseMetadata( artifact );
                    metadata.setVersion( artifact.getBaseVersion() );
                    versioning = new Versioning();

                    Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( artifact.getVersion() );
                    if ( matcher.matches() )
                    {
                        Snapshot snapshot = new Snapshot();
                        snapshot.setBuildNumber( Integer.valueOf( matcher.group( 3 ) ).intValue() );
                        snapshot.setTimestamp( matcher.group( 2 ) );
                        versioning.setSnapshot( snapshot );
                    }

                    // TODO: merge latest/release/snapshot from source instead
                    metadata.setVersioning( versioning );
                    updateMetadata( new SnapshotArtifactRepositoryMetadata( artifact ), targetRepository, metadata,
                                    transaction );

                    if ( !dryrun )
                    {
                        transaction.commit();
                    }
                    reporter.addSuccess( artifact );
                }
            }
        }
    }

    private static Metadata createBaseMetadata( Artifact artifact )
    {
        Metadata metadata = new Metadata();
        metadata.setArtifactId( artifact.getArtifactId() );
        metadata.setGroupId( artifact.getGroupId() );
        return metadata;
    }

    private void updateMetadata( RepositoryMetadata artifactMetadata, ArtifactRepository targetRepository,
                                 Metadata newMetadata, FileTransaction transaction )
        throws RepositoryConversionException
    {
        File file = new File( targetRepository.getBasedir(),
                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );

        Metadata metadata;
        boolean changed;

        if ( file.exists() )
        {
            metadata = readMetadata( file );
            changed = metadata.merge( newMetadata );
        }
        else
        {
            changed = true;
            metadata = newMetadata;
        }

        if ( changed )
        {
            StringWriter writer = null;
            try
            {
                writer = new StringWriter();

                MetadataXpp3Writer mappingWriter = new MetadataXpp3Writer();

                mappingWriter.write( writer, metadata );

                transaction.createFile( writer.toString(), file );
            }
            catch ( IOException e )
            {
                throw new RepositoryConversionException( "Error writing target metadata", e );
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
    }

    private Metadata readMetadata( File file )
        throws RepositoryConversionException
    {
        Metadata metadata;
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        FileReader fileReader = null;
        try
        {
            fileReader = new FileReader( file );
            metadata = reader.read( fileReader );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryConversionException( "Error reading target metadata", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryConversionException( "Error reading target metadata", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryConversionException( "Error reading target metadata", e );
        }
        finally
        {
            IOUtil.close( fileReader );
        }
        return metadata;
    }

    private boolean validateMetadata( Artifact artifact, ArtifactReporter reporter )
        throws RepositoryConversionException
    {
        ArtifactRepository repository = artifact.getRepository();

        boolean result = true;

        RepositoryMetadata repositoryMetadata = new ArtifactRepositoryMetadata( artifact );
        File file =
            new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repositoryMetadata ) );
        if ( file.exists() )
        {
            Metadata metadata = readMetadata( file );
            result = validateMetadata( metadata, repositoryMetadata, artifact, reporter );
        }

        repositoryMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        file = new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repositoryMetadata ) );
        if ( file.exists() )
        {
            Metadata metadata = readMetadata( file );
            result = result && validateMetadata( metadata, repositoryMetadata, artifact, reporter );
        }

        return result;
    }

    private boolean validateMetadata( Metadata metadata, RepositoryMetadata repositoryMetadata, Artifact artifact,
                                      ArtifactReporter reporter )
    {
        String key = "failure.incorrect.";

        if ( repositoryMetadata.storedInGroupDirectory() )
        {
            key += "groupMetadata.";
        }
        else if ( repositoryMetadata.storedInArtifactVersionDirectory() )
        {
            key += "snapshotMetadata.";
        }
        else
        {
            key += "artifactMetadata.";
        }

        boolean result = true;

        if ( !metadata.getGroupId().equals( artifact.getGroupId() ) )
        {
            reporter.addFailure( artifact, getI18NString( key + "groupId" ) );
            result = false;
        }
        if ( !repositoryMetadata.storedInGroupDirectory() )
        {
            if ( !metadata.getArtifactId().equals( artifact.getArtifactId() ) )
            {
                reporter.addFailure( artifact, getI18NString( key + "artifactId" ) );
                result = false;
            }
            if ( !repositoryMetadata.storedInArtifactVersionDirectory() )
            {
                // artifact metadata

                boolean foundVersion = false;
                if ( metadata.getVersioning() != null )
                {
                    for ( Iterator i = metadata.getVersioning().getVersions().iterator();
                          i.hasNext() && !foundVersion; )
                    {
                        String version = (String) i.next();
                        if ( version.equals( artifact.getBaseVersion() ) )
                        {
                            foundVersion = true;
                        }
                    }
                }

                if ( !foundVersion )
                {
                    reporter.addFailure( artifact, getI18NString( key + "versions" ) );
                    result = false;
                }
            }
            else
            {
                // snapshot metadata
                if ( !artifact.getBaseVersion().equals( metadata.getVersion() ) )
                {
                    reporter.addFailure( artifact, getI18NString( key + "version" ) );
                    result = false;
                }

                if ( artifact.isSnapshot() )
                {
                    Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( artifact.getVersion() );
                    if ( matcher.matches() )
                    {
                        boolean correct = false;
                        if ( metadata.getVersioning() != null && metadata.getVersioning().getSnapshot() != null )
                        {
                            Snapshot snapshot = metadata.getVersioning().getSnapshot();
                            int build = Integer.valueOf( matcher.group( 3 ) ).intValue();
                            String ts = matcher.group( 2 );
                            if ( build == snapshot.getBuildNumber() && ts.equals( snapshot.getTimestamp() ) )
                            {
                                correct = true;
                            }
                        }

                        if ( !correct )
                        {
                            reporter.addFailure( artifact, getI18NString( key + "snapshot" ) );
                            result = false;
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean copyPom( Artifact artifact, ArtifactRepository targetRepository, ArtifactReporter reporter,
                             FileTransaction transaction )
        throws RepositoryConversionException
    {
        Artifact pom = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                              artifact.getVersion() );
        pom.setBaseVersion( artifact.getBaseVersion() );
        ArtifactRepository repository = artifact.getRepository();
        File file = new File( repository.getBasedir(), repository.pathOf( pom ) );

        boolean result = true;
        if ( file.exists() )
        {
            // TODO: utility methods in the model converter
            File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pom ) );

            String contents = null;
            boolean checksumsValid = false;
            try
            {
                if ( testChecksums( artifact, file, reporter ) )
                {
                    checksumsValid = true;
                    contents = FileUtils.fileRead( file );
                }
            }
            catch ( IOException e )
            {
                throw new RepositoryConversionException( "Unable to read source POM: " + e.getMessage(), e );
            }

            if ( checksumsValid && contents.indexOf( "modelVersion" ) >= 0 )
            {
                // v4 POM
                try
                {
                    boolean matching = false;
                    if ( !force && targetFile.exists() )
                    {
                        String targetContents = FileUtils.fileRead( targetFile );
                        matching = targetContents.equals( contents );
                    }
                    if ( force || !matching )
                    {
                        transaction.createFile( contents, targetFile );
                    }
                }
                catch ( IOException e )
                {
                    throw new RepositoryConversionException( "Unable to write target POM: " + e.getMessage(), e );
                }
            }
            else
            {
                // v3 POM
                StringReader stringReader = new StringReader( contents );
                StringWriter writer = null;
                try
                {
                    MavenXpp3Reader v3Reader = new MavenXpp3Reader();
                    org.apache.maven.model.v3_0_0.Model v3Model = v3Reader.read( stringReader );

                    if ( doRelocation( artifact, v3Model, targetRepository, transaction ) )
                    {
                        Artifact relocatedPom = artifactFactory.createProjectArtifact( artifact.getGroupId(),
                                                                                       artifact.getArtifactId(),
                                                                                       artifact.getVersion() );
                        targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( relocatedPom ) );
                    }

                    Model v4Model = translator.translate( v3Model );

                    translator.validateV4Basics( v4Model, v3Model.getGroupId(), v3Model.getArtifactId(),
                                                 v3Model.getVersion(), v3Model.getPackage() );

                    writer = new StringWriter();
                    MavenXpp3Writer Xpp3Writer = new MavenXpp3Writer();
                    Xpp3Writer.write( writer, v4Model );

                    transaction.createFile( writer.toString(), targetFile );

                    List warnings = translator.getWarnings();

                    for ( Iterator i = warnings.iterator(); i.hasNext(); )
                    {
                        String message = (String) i.next();
                        reporter.addWarning( artifact, message );
                    }
                }
                catch ( XmlPullParserException e )
                {
                    reporter.addFailure( artifact, getI18NString( "failure.invalid.source.pom", e.getMessage() ) );
                    result = false;
                }
                catch ( IOException e )
                {
                    throw new RepositoryConversionException( "Unable to write converted POM", e );
                }
                catch ( PomTranslationException e )
                {
                    // TODO! check handling, fix error message
                    reporter.addFailure( artifact, getI18NString( "failure.invalid.source.pom", e.getMessage() ) );
                    result = false;
                }
                finally
                {
                    IOUtil.close( writer );
                }
            }
        }
        else
        {
            reporter.addWarning( artifact, getI18NString( "warning.missing.pom" ) );
        }
        return result;
    }

    private boolean doRelocation( Artifact artifact, org.apache.maven.model.v3_0_0.Model v3Model,
                                  ArtifactRepository repository, FileTransaction transaction )
        throws IOException
    {
        Properties properties = v3Model.getProperties();
        if ( properties.containsKey( "relocated.groupId" ) || properties.containsKey( "relocated.artifactId" ) ||
            properties.containsKey( "relocated.version" ) )
        {
            String newGroupId = v3Model.getGroupId();
            if ( properties.containsKey( "relocated.groupId" ) )
            {
                newGroupId = properties.getProperty( "relocated.groupId" );
                properties.remove( "relocated.groupId" );
            }

            String newArtifactId = v3Model.getArtifactId();
            if ( properties.containsKey( "relocated.artifactId" ) )
            {
                newArtifactId = properties.getProperty( "relocated.artifactId" );
                properties.remove( "relocated.artifactId" );
            }

            String newVersion = v3Model.getVersion();
            if ( properties.containsKey( "relocated.version" ) )
            {
                newVersion = properties.getProperty( "relocated.version" );
                properties.remove( "relocated.version" );
            }

            String message = "";
            if ( properties.containsKey( "relocated.message" ) )
            {
                message = properties.getProperty( "relocated.message" );
                properties.remove( "relocated.message" );
            }

            if ( properties.isEmpty() )
            {
                v3Model.setProperties( null );
            }

            writeRelocationPom( v3Model.getGroupId(), v3Model.getArtifactId(), v3Model.getVersion(), newGroupId,
                                newArtifactId, newVersion, message, repository, transaction );

            v3Model.setGroupId( newGroupId );
            v3Model.setArtifactId( newArtifactId );
            v3Model.setVersion( newVersion );

            artifact.setGroupId( newGroupId );
            artifact.setArtifactId( newArtifactId );
            artifact.setVersion( newVersion );

            return true;
        }
        else
        {
            return false;
        }
    }

    private void writeRelocationPom( String groupId, String artifactId, String version, String newGroupId,
                                     String newArtifactId, String newVersion, String message,
                                     ArtifactRepository repository, FileTransaction transaction )
        throws IOException
    {
        Model pom = new Model();
        pom.setGroupId( groupId );
        pom.setArtifactId( artifactId );
        pom.setVersion( version );

        DistributionManagement dMngt = new DistributionManagement();

        Relocation relocation = new Relocation();
        relocation.setGroupId( newGroupId );
        relocation.setArtifactId( newArtifactId );
        relocation.setVersion( newVersion );
        if ( message != null && message.length() > 0 )
        {
            relocation.setMessage( message );
        }

        dMngt.setRelocation( relocation );

        pom.setDistributionManagement( dMngt );

        Artifact artifact = artifactFactory.createBuildArtifact( groupId, artifactId, version, "pom" );
        File pomFile = new File( repository.getBasedir(), repository.pathOf( artifact ) );

        StringWriter strWriter = new StringWriter();
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();
        pomWriter.write( strWriter, pom );

        transaction.createFile( strWriter.toString(), pomFile );
    }

    private String getI18NString( String key, String arg0 )
    {
        return i18n.format( getClass().getName(), Locale.getDefault(), key, arg0 );
    }

    private String getI18NString( String key )
    {
        return i18n.getString( getClass().getName(), Locale.getDefault(), key );
    }

    private boolean testChecksums( Artifact artifact, File file, ArtifactReporter reporter )
        throws IOException, RepositoryConversionException
    {
        boolean result = true;

        try
        {
            File md5 = new File( file.getParentFile(), file.getName() + ".md5" );
            if ( md5.exists() )
            {
                String checksum = FileUtils.fileRead( md5 );
                if ( !digester.verifyChecksum( file, checksum, Digester.MD5 ) )
                {
                    reporter.addFailure( artifact, getI18NString( "failure.incorrect.md5" ) );
                    result = false;
                }
            }

            File sha1 = new File( file.getParentFile(), file.getName() + ".sha1" );
            if ( sha1.exists() )
            {
                String checksum = FileUtils.fileRead( sha1 );
                if ( !digester.verifyChecksum( file, checksum, Digester.SHA1 ) )
                {
                    reporter.addFailure( artifact, getI18NString( "failure.incorrect.sha1" ) );
                    result = false;
                }
            }
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RepositoryConversionException( "Error copying artifact: " + e.getMessage(), e );
        }
        return result;
    }

    private boolean copyArtifact( Artifact artifact, ArtifactRepository targetRepository, ArtifactReporter reporter,
                                  FileTransaction transaction )
        throws RepositoryConversionException
    {
        File sourceFile = artifact.getFile();

        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );

        boolean result = true;
        try
        {
            boolean matching = false;
            if ( !force && targetFile.exists() )
            {
                matching = FileUtils.contentEquals( sourceFile, targetFile );
                if ( !matching )
                {
                    reporter.addFailure( artifact, getI18NString( "failure.target.already.exists" ) );
                    result = false;
                }
            }
            if ( result )
            {
                if ( force || !matching )
                {
                    if ( testChecksums( artifact, sourceFile, reporter ) )
                    {
                        transaction.copyFile( sourceFile, targetFile );
                    }
                    else
                    {
                        result = false;
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryConversionException( "Error copying artifact", e );
        }
        return result;
    }

    public void convert( List artifacts, ArtifactRepository targetRepository, ArtifactReporter reporter )
        throws RepositoryConversionException
    {
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            convert( artifact, targetRepository, reporter );
        }
    }
}
