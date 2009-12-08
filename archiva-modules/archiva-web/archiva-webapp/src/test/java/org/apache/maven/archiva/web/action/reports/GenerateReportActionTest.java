package org.apache.maven.archiva.web.action.reports;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import com.opensymphony.xwork2.Action;
import org.apache.commons.io.IOUtils;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.RepositoryContentStatisticsDAO;
import org.apache.maven.archiva.database.RepositoryProblemDAO;
import org.apache.maven.archiva.database.constraints.RangeConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryContentStatisticsByRepositoryConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemByGroupIdConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemByRepositoryIdConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemConstraint;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.model.RepositoryProblemReport;
import org.apache.maven.archiva.web.action.admin.repositories.ArchivaDAOStub;
import org.apache.maven.archiva.web.action.admin.repositories.RepositoryContentStatisticsDAOStub;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;

/**
 * Test the GenerationReportAction. Note that we are testing for <i>current</i> behaviour, however there are several
 * instances below where other behaviour may actually be more appropriate (eg the error handling, download stats should
 * never forward to HTML page, etc). This is also missing tests for various combinations of paging at this point.
 */
public class GenerateReportActionTest
    extends PlexusInSpringTestCase
{
    private GenerateReportAction action;

    private static final String SNAPSHOTS = "snapshots";

    private static final String INTERNAL = "internal";

    private RepositoryProblemDAO repositoryProblemDAO;

    private MockControl repositoryProblemDAOControl;

    private static final String GROUP_ID = "groupId";

    private static final String URL = "http://localhost/reports/generateReport.action";

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaDAOStub archivaDAOStub = (ArchivaDAOStub) lookup( ArchivaDAO.class, "jdo" );
        archivaDAOStub.setRepositoryIds( Arrays.asList( "repo1", "repo2" ) );

        repositoryProblemDAOControl = MockControl.createControl( RepositoryProblemDAO.class );
        repositoryProblemDAO = (RepositoryProblemDAO) repositoryProblemDAOControl.getMock();
        archivaDAOStub.setRepositoryProblemDAO( repositoryProblemDAO );

        action = (GenerateReportAction) lookup( Action.class, "generateReport" );
    }

    private void prepareAction( List<String> selectedRepositories, List<String> availableRepositories )
    {
        action.setSelectedRepositories( selectedRepositories );
        action.prepare();

        assertEquals( Arrays.asList( GenerateReportAction.ALL_REPOSITORIES, "repo1", "repo2" ),
                      action.getRepositoryIds() );
        assertEquals( availableRepositories, action.getAvailableRepositories() );
    }

    public void testGenerateStatisticsInvalidRowCount()
    {
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        action.setRowCount( 0 );
        String result = action.generateStatistics();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
    }

    public void testGenerateStatisticsInvalidEndDate()
    {
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        action.setStartDate( "2009/12/12" );
        action.setEndDate( "2008/11/11" );
        String result = action.generateStatistics();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
    }

    public void testGenerateStatisticsMalformedEndDate()
    {
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        action.setEndDate( "This is not a date" );
        String result = action.generateStatistics();

        // TODO: should be an input error
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
    }

    public void testGenerateStatisticsInvalidEndDateMultiRepo()
    {
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setStartDate( "2009/12/12" );
        action.setEndDate( "2008/11/11" );
        String result = action.generateStatistics();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
    }

    public void testGenerateStatisticsMalformedEndDateMultiRepo()
    {
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setEndDate( "This is not a date" );
        String result = action.generateStatistics();

        // TODO: should be an input error
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
    }

    public void testGenerateStatisticsNoRepos()
    {
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.generateStatistics();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
    }

    public void testGenerateStatisticsSingleRepo()
    {
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        String result = action.generateStatistics();
        assertSuccessResult( result );
    }

    public void testGenerateStatisticsSingleRepoNoStats()
    {
        RepositoryContentStatisticsDAOStub dao =
            (RepositoryContentStatisticsDAOStub) lookup( RepositoryContentStatisticsDAO.class, "jdo" );
        dao.setStats( Collections.<RepositoryContentStatistics>emptyList() );
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        String result = action.generateStatistics();
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
    }

    public void testGenerateStatisticsOvershotPages()
    {
        action.setPage( 2 );
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        String result = action.generateStatistics();
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
    }

    public void testGenerateStatisticsMultipleRepoNoResults()
    {
        RepositoryContentStatisticsDAOStub dao =
            (RepositoryContentStatisticsDAOStub) lookup( RepositoryContentStatisticsDAO.class, "jdo" );
        dao.setStats( Collections.<RepositoryContentStatistics>emptyList() );
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        String result = action.generateStatistics();
        assertEquals( GenerateReportAction.BLANK, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasActionMessages() );
        assertFalse( action.hasFieldErrors() );
    }

    public void testGenerateStatisticsMultipleRepo()
    {
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        String result = action.generateStatistics();
        assertSuccessResult( result );
    }

    public void testDownloadStatisticsSingleRepo()
        throws IOException, ArchivaDatabaseException
    {
        prepareAction( Arrays.asList( SNAPSHOTS ), Arrays.asList( INTERNAL ) );

        String result = action.downloadStatisticsReport();
        assertEquals( GenerateReportAction.SEND_FILE, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );

        RepositoryContentStatisticsDAOStub dao =
            (RepositoryContentStatisticsDAOStub) lookup( RepositoryContentStatisticsDAO.class, "jdo" );
        RepositoryContentStatistics stats = dao.queryRepositoryContentStatistics(
            new RepositoryContentStatisticsByRepositoryConstraint( SNAPSHOTS ) ).get( 0 );

        assertEquals(
            "Date of Scan,Total File Count,Total Size,Artifact Count,Group Count,Project Count,Plugins,Archetypes,Jars,Wars,Deployments,Downloads\n" +
                stats.getWhenGathered() + ",0,0,0,0,0,1,0,1,1,0,0\n", IOUtils.toString( action.getInputStream() ) );
    }

    public void testDownloadStatisticsMultipleRepos()
        throws IOException, ArchivaDatabaseException
    {
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        String result = action.downloadStatisticsReport();
        assertEquals( GenerateReportAction.SEND_FILE, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );

        assertMultiRepoCsvResult();
    }

    private void assertMultiRepoCsvResult()
        throws IOException
    {
        assertEquals(
            "Repository,Total File Count,Total Size,Artifact Count,Group Count,Project Count,Plugins,Archetypes,Jars,Wars,Deployments,Downloads\n" +
                "snapshots,0,0,0,0,0,1,0,1,1,0,0\n" + "internal,0,0,0,0,0,1,0,1,1,0,0\n",
            IOUtils.toString( action.getInputStream() ) );
    }

    public void testDownloadStatisticsMalformedEndDateMultiRepo()
    {
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setEndDate( "This is not a date" );
        String result = action.downloadStatisticsReport();

        // TODO: should be an input error
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
    }

    public void testDownloadStatisticsInvalidEndDateMultiRepo()
    {
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setStartDate( "2009/12/12" );
        action.setEndDate( "2008/11/11" );
        String result = action.downloadStatisticsReport();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
    }

    public void testDownloadStatisticsInvalidEndDateSingleRepo()
    {
        prepareAction( Arrays.asList( SNAPSHOTS ), Arrays.asList( INTERNAL ) );

        action.setStartDate( "2009/12/12" );
        action.setEndDate( "2008/11/11" );
        String result = action.downloadStatisticsReport();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
    }

    public void testDownloadStatisticsSingleRepoNoStats()
    {
        RepositoryContentStatisticsDAOStub dao =
            (RepositoryContentStatisticsDAOStub) lookup( RepositoryContentStatisticsDAO.class, "jdo" );
        dao.setStats( Collections.<RepositoryContentStatistics>emptyList() );
        prepareAction( Collections.singletonList( INTERNAL ), Collections.singletonList( SNAPSHOTS ) );

        String result = action.downloadStatisticsReport();
        assertEquals( Action.ERROR, result );
        assertTrue( action.hasActionErrors() );
    }

    public void testDownloadStatisticsNoRepos()
    {
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.downloadStatisticsReport();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
    }

    public void testDownloadStatisticsMultipleRepoNoResults()
    {
        RepositoryContentStatisticsDAOStub dao =
            (RepositoryContentStatisticsDAOStub) lookup( RepositoryContentStatisticsDAO.class, "jdo" );
        dao.setStats( Collections.<RepositoryContentStatistics>emptyList() );
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        String result = action.downloadStatisticsReport();
        assertEquals( GenerateReportAction.BLANK, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasActionMessages() );
        assertFalse( action.hasFieldErrors() );
    }

    public void testDownloadStatisticsMultipleRepoInStrutsFormat()
        throws IOException
    {
        prepareAction( Arrays.asList( SNAPSHOTS, INTERNAL ), Collections.<String>emptyList() );

        action.setSelectedRepositories( Collections.singletonList( "[" + SNAPSHOTS + "],[" + INTERNAL + "]" ) );
        String result = action.downloadStatisticsReport();
        assertEquals( GenerateReportAction.SEND_FILE, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );

        assertMultiRepoCsvResult();
    }

    public void testHealthReportSingleRepo()
        throws Exception
    {
        RepositoryProblem problem1 = createProblem( GROUP_ID, "artifactId", INTERNAL );
        RepositoryProblem problem2 = createProblem( GROUP_ID, "artifactId-2", INTERNAL );
        repositoryProblemDAOControl.expectAndReturn( repositoryProblemDAO.queryRepositoryProblems(
            new RepositoryProblemByRepositoryIdConstraint( new int[]{0, 101}, INTERNAL ) ),
                                                     Arrays.asList( problem1, problem2 ) );
        repositoryProblemDAOControl.replay();

        action.setRepositoryId( INTERNAL );
        ServletRunner sr = new ServletRunner();
        ServletUnitClient sc = sr.newClient();

        action.setServletRequest( sc.newInvocation( URL ).getRequest() );
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertSuccessResult( result );

        RepositoryProblemReport problemReport1 = createProblemReport( problem1 );
        RepositoryProblemReport problemReport2 = createProblemReport( problem2 );
        assertEquals( Collections.singleton( INTERNAL ), action.getRepositoriesMap().keySet() );
        assertEquals( Arrays.asList( problemReport1, problemReport2 ), action.getRepositoriesMap().get( INTERNAL ) );

        repositoryProblemDAOControl.verify();
    }

    public void testHealthReportInvalidRowCount()
        throws Exception
    {
        repositoryProblemDAOControl.replay();

        action.setRowCount( 0 );
        action.setRepositoryId( INTERNAL );
        ServletRunner sr = new ServletRunner();
        ServletUnitClient sc = sr.newClient();

        action.setServletRequest( sc.newInvocation( URL ).getRequest() );
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertEquals( Action.INPUT, result );
        assertFalse( action.hasActionErrors() );
        assertTrue( action.hasFieldErrors() );

        repositoryProblemDAOControl.verify();
    }

    public void testHealthReportAllRepos()
        throws Exception
    {
        RepositoryProblem problem1 = createProblem( GROUP_ID, "artifactId", INTERNAL );
        RepositoryProblem problem2 = createProblem( GROUP_ID, "artifactId-2", SNAPSHOTS );
        repositoryProblemDAOControl.expectAndReturn(
            repositoryProblemDAO.queryRepositoryProblems( new RangeConstraint( new int[]{0, 101} ) ),
            Arrays.asList( problem1, problem2 ) );
        repositoryProblemDAOControl.replay();

        action.setRepositoryId( GenerateReportAction.ALL_REPOSITORIES );
        ServletRunner sr = new ServletRunner();
        ServletUnitClient sc = sr.newClient();

        action.setServletRequest( sc.newInvocation( URL ).getRequest() );
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertSuccessResult( result );

        RepositoryProblemReport problemReport1 = createProblemReport( problem1 );
        RepositoryProblemReport problemReport2 = createProblemReport( problem2 );
        assertEquals( Arrays.asList( INTERNAL, SNAPSHOTS ),
                      new ArrayList<String>( action.getRepositoriesMap().keySet() ) );
        assertEquals( Arrays.asList( problemReport1 ), action.getRepositoriesMap().get( INTERNAL ) );
        assertEquals( Arrays.asList( problemReport2 ), action.getRepositoriesMap().get( SNAPSHOTS ) );

        repositoryProblemDAOControl.verify();
    }

    public void testHealthReportSingleRepoByCorrectGroupId()
        throws Exception
    {
        RepositoryProblem problem1 = createProblem( GROUP_ID, "artifactId", INTERNAL );
        RepositoryProblem problem2 = createProblem( GROUP_ID, "artifactId-2", INTERNAL );
        repositoryProblemDAOControl.expectAndReturn( repositoryProblemDAO.queryRepositoryProblems(
            new RepositoryProblemConstraint( new int[]{0, 101}, GROUP_ID, INTERNAL ) ),
                                                     Arrays.asList( problem1, problem2 ) );
        repositoryProblemDAOControl.replay();

        action.setGroupId( GROUP_ID );
        action.setRepositoryId( INTERNAL );
        ServletRunner sr = new ServletRunner();
        ServletUnitClient sc = sr.newClient();

        action.setServletRequest( sc.newInvocation( URL ).getRequest() );
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertSuccessResult( result );

        RepositoryProblemReport problemReport1 = createProblemReport( problem1 );
        RepositoryProblemReport problemReport2 = createProblemReport( problem2 );
        assertEquals( Collections.singleton( INTERNAL ), action.getRepositoriesMap().keySet() );
        assertEquals( Arrays.asList( problemReport1, problemReport2 ), action.getRepositoriesMap().get( INTERNAL ) );

        repositoryProblemDAOControl.verify();
    }

    public void testHealthReportSingleRepoByCorrectGroupIdAllRepositories()
        throws Exception
    {
        RepositoryProblem problem1 = createProblem( GROUP_ID, "artifactId", INTERNAL );
        RepositoryProblem problem2 = createProblem( GROUP_ID, "artifactId-2", SNAPSHOTS );
        repositoryProblemDAOControl.expectAndReturn( repositoryProblemDAO.queryRepositoryProblems(
            new RepositoryProblemByGroupIdConstraint( new int[]{0, 101}, GROUP_ID ) ),
                                                     Arrays.asList( problem1, problem2 ) );
        repositoryProblemDAOControl.replay();

        action.setGroupId( GROUP_ID );
        action.setRepositoryId( GenerateReportAction.ALL_REPOSITORIES );
        ServletRunner sr = new ServletRunner();
        ServletUnitClient sc = sr.newClient();

        action.setServletRequest( sc.newInvocation( URL ).getRequest() );
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertSuccessResult( result );

        RepositoryProblemReport problemReport1 = createProblemReport( problem1 );
        RepositoryProblemReport problemReport2 = createProblemReport( problem2 );
        assertEquals( Arrays.asList( INTERNAL, SNAPSHOTS ),
                      new ArrayList<String>( action.getRepositoriesMap().keySet() ) );
        assertEquals( Arrays.asList( problemReport1 ), action.getRepositoriesMap().get( INTERNAL ) );
        assertEquals( Arrays.asList( problemReport2 ), action.getRepositoriesMap().get( SNAPSHOTS ) );

        repositoryProblemDAOControl.verify();
    }

    public void testHealthReportSingleRepoByIncorrectGroupId()
        throws Exception
    {
        repositoryProblemDAOControl.expectAndReturn( repositoryProblemDAO.queryRepositoryProblems(
            new RepositoryProblemConstraint( new int[]{0, 101}, "not.it", INTERNAL ) ),
                                                     Collections.<Object>emptyList() );
        repositoryProblemDAOControl.replay();

        action.setGroupId( "not.it" );
        action.setRepositoryId( INTERNAL );
        ServletRunner sr = new ServletRunner();
        ServletUnitClient sc = sr.newClient();

        action.setServletRequest( sc.newInvocation( URL ).getRequest() );
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertEquals( GenerateReportAction.BLANK, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );

        repositoryProblemDAOControl.verify();
    }

    private RepositoryProblemReport createProblemReport( RepositoryProblem problem )
    {
        RepositoryProblemReport problemReport = new RepositoryProblemReport( problem );
        problemReport.setGroupURL( "http://localhost/browse/" + problem.getGroupId() );
        problemReport.setArtifactURL( problemReport.getGroupURL() + "/" + problem.getArtifactId() );
        return problemReport;
    }

    private RepositoryProblem createProblem( String groupId, String artifactId, String repoId )
    {
        RepositoryProblem problem = new RepositoryProblem();
        problem.setRepositoryId( repoId );
        problem.setGroupId( groupId );
        problem.setArtifactId( artifactId );
        return problem;
    }

    public void testHealthReportNoRepositoryId()
        throws Exception
    {
        prepareAction( Collections.<String>emptyList(), Arrays.asList( SNAPSHOTS, INTERNAL ) );

        String result = action.execute();
        assertEquals( Action.INPUT, result );
        assertTrue( action.hasFieldErrors() );
    }

    private void assertSuccessResult( String result )
    {
        assertEquals( Action.SUCCESS, result );
        assertFalse( action.hasActionErrors() );
        assertFalse( action.hasFieldErrors() );
    }
}
