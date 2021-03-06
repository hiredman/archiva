package org.apache.archiva.scheduler.indexing;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.maven.archiva.common.utils.ArchivaNexusIndexerUtil;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.IndexerEngine;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.packer.IndexPacker;

/**
 * ArchivaIndexingTaskExecutorTest
 */
public class ArchivaIndexingTaskExecutorTest
    extends PlexusInSpringTestCase
{
    private ArchivaIndexingTaskExecutor indexingExecutor;

    private IndexerEngine indexerEngine;

    private IndexPacker indexPacker;

    private ManagedRepositoryConfiguration repositoryConfig;

    private Configuration configuration;

    private NexusIndexer indexer;

    private IndexingContext context;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        indexingExecutor = new ArchivaIndexingTaskExecutor();
        indexingExecutor.initialize();

        repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( "test-repo" );
        repositoryConfig.setLocation( getBasedir() + "/target/test-classes/test-repo" );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( "Test Repository" );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );

        configuration = new Configuration();
        configuration.addManagedRepository( repositoryConfig );

        indexer = (NexusIndexer) lookup( NexusIndexer.class );
        indexerEngine = (IndexerEngine) lookup( IndexerEngine.class );
        indexPacker = (IndexPacker) lookup( IndexPacker.class );

        indexingExecutor.setIndexerEngine( indexerEngine );
        indexingExecutor.setIndexPacker( indexPacker );

        context = ArtifactIndexingTask.createContext( repositoryConfig );
    }

    protected void tearDown()
        throws Exception
    {
        context.close( true );
        indexer.removeIndexingContext( context, true );

        // delete created index in the repository
        File indexDir = new File( repositoryConfig.getLocation(), ".indexer" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );

        indexDir = new File( repositoryConfig.getLocation(), ".index" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );

        super.tearDown();
    }

    public void testAddArtifactToIndex()
        throws Exception
    {
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD, context );

        indexingExecutor.executeTask( task );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );

        IndexingContext context =
            indexer.addIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                        new File( repositoryConfig.getLocation() ),
                                        new File( repositoryConfig.getLocation(), ".indexer" ), null, null,
                                        ArchivaNexusIndexerUtil.FULL_INDEX );
        context.setSearchable( true );

        FlatSearchRequest request = new FlatSearchRequest( q );
        FlatSearchResponse response = indexer.searchFlat( request );

        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertFalse( new File( repositoryConfig.getLocation(), ".index" ).exists() );
        assertEquals( 1, response.getTotalHits() );

        Set<ArtifactInfo> results = response.getResults();

        ArtifactInfo artifactInfo = (ArtifactInfo) results.iterator().next();
        assertEquals( "org.apache.archiva", artifactInfo.groupId );
        assertEquals( "archiva-index-methods-jar-test", artifactInfo.artifactId );
        assertEquals( "test-repo", artifactInfo.repository );

        context.close( true );
    }

    public void testUpdateArtifactInIndex()
        throws Exception
    {
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD, context );

        indexingExecutor.executeTask( task );
        indexingExecutor.executeTask( task );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );

        IndexSearcher searcher = new IndexSearcher( repositoryConfig.getLocation() + "/.indexer" );
        TopDocs topDocs = searcher.search( q, null, 10 );

        searcher.close();

        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertFalse( new File( repositoryConfig.getLocation(), ".index" ).exists() );

        // should only return 1 hit!
        assertEquals( 1, topDocs.totalHits );
    }

    public void testRemoveArtifactFromIndex()
        throws Exception
    {
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD, context );

        // remove artifact from index
        indexingExecutor.executeTask( task );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );

        IndexSearcher searcher = new IndexSearcher( repositoryConfig.getLocation() + "/.indexer" );
        TopDocs topDocs = searcher.search( q, null, 10 );

        searcher.close();

        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertFalse( new File( repositoryConfig.getLocation(), ".index" ).exists() );

        // should return 1 hit
        assertEquals( 1, topDocs.totalHits );

        searcher.close();

        context = ArtifactIndexingTask.createContext( repositoryConfig );

        // remove added artifact from index
        task = new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.DELETE, context );
        indexingExecutor.executeTask( task );

        task = new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.FINISH, context );
        indexingExecutor.executeTask( task );

        q = new BooleanQuery();
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );

        searcher = new IndexSearcher( repositoryConfig.getLocation() + "/.indexer" );
        topDocs = searcher.search( q, null, 10 );

        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertTrue( new File( repositoryConfig.getLocation(), ".index" ).exists() );

        // artifact should have been removed from the index!
        assertEquals( 0, topDocs.totalHits );

        context.close( true );
        searcher.close();
        // TODO: test it was removed from the packaged index also
    }

    public void testPackagedIndex()
        throws Exception
    {
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD, context );

        indexingExecutor.executeTask( task );

        task = new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.FINISH, context );

        indexingExecutor.executeTask( task );

        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertTrue( new File( repositoryConfig.getLocation(), ".index" ).exists() );

        // unpack .zip index
        File destDir = new File( repositoryConfig.getLocation(), ".index/tmp" );
        unzipIndex( new File( repositoryConfig.getLocation(), ".index" ).getPath(), destDir.getPath() );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );

        IndexingContext context =
            indexer.addIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                        new File( repositoryConfig.getLocation() ), destDir, null, null,
                                        ArchivaNexusIndexerUtil.FULL_INDEX );
        context.setSearchable( true );

        FlatSearchRequest request = new FlatSearchRequest( q );
        FlatSearchResponse response = indexer.searchFlat( request );

        assertEquals( 1, response.getTotalHits() );

        Set<ArtifactInfo> results = response.getResults();

        ArtifactInfo artifactInfo = (ArtifactInfo) results.iterator().next();
        assertEquals( "org.apache.archiva", artifactInfo.groupId );
        assertEquals( "archiva-index-methods-jar-test", artifactInfo.artifactId );
        assertEquals( "test-repo", artifactInfo.repository );

        context.close( true );
    }

    private void unzipIndex( String indexDir, String destDir )
        throws FileNotFoundException, IOException
    {
        final int buff = 2048;

        new File( destDir ).mkdirs();

        BufferedOutputStream out = null;
        FileInputStream fin = new FileInputStream( new File( indexDir, "nexus-maven-repository-index.zip" ) );
        ZipInputStream in = new ZipInputStream( new BufferedInputStream( fin ) );
        ZipEntry entry;

        while ( ( entry = in.getNextEntry() ) != null )
        {
            int count;
            byte data[] = new byte[buff];
            FileOutputStream fout = new FileOutputStream( new File( destDir, entry.getName() ) );
            out = new BufferedOutputStream( fout, buff );

            while ( ( count = in.read( data, 0, buff ) ) != -1 )
            {
                out.write( data, 0, count );
            }
            out.flush();
            out.close();
        }

        in.close();
    }
}
