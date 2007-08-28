package org.apache.maven.archiva.repository.project.filters;

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

import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Individual;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.DefaultBidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;
import org.apache.maven.archiva.repository.project.ProjectModelResolverFactory;
import org.apache.maven.archiva.repository.project.readers.ProjectModel400Reader;
import org.apache.maven.archiva.repository.project.resolvers.RepositoryProjectResolver;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * EffectiveProjectModelFilterTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class EffectiveProjectModelFilterTest
    extends PlexusTestCase
{
    private static final String DEFAULT_REPOSITORY = "src/test/repositories/default-repository";

    private EffectiveProjectModelFilter lookupEffective()
        throws Exception
    {
        return (EffectiveProjectModelFilter) lookup( ProjectModelFilter.class, "effective" );
    }

    private ArchivaProjectModel createArchivaProjectModel( String path )
        throws ProjectModelException
    {
        ProjectModelReader reader = new ProjectModel400Reader();

        File pomFile = new File( getBasedir(), path );

        return reader.read( pomFile );
    }

    private ProjectModelResolver createDefaultRepositoryResolver()
    {
        File defaultRepoDir = new File( getBasedir(), DEFAULT_REPOSITORY );

        ArchivaRepository repo = new ArchivaRepository( "defaultTestRepo", "Default Test Repo", "file://"
            + defaultRepoDir.getAbsolutePath() );

        ProjectModelReader reader = new ProjectModel400Reader();
        BidirectionalRepositoryLayout layout = new DefaultBidirectionalRepositoryLayout();
        RepositoryProjectResolver resolver = new RepositoryProjectResolver( repo, reader, layout );

        return resolver;
    }

    public void testBuildEffectiveProject()
        throws Exception
    {
        initTestResolverFactory();
        EffectiveProjectModelFilter filter = lookupEffective();

        ArchivaProjectModel startModel = createArchivaProjectModel( DEFAULT_REPOSITORY
            + "/org/apache/maven/archiva/archiva-model/1.0-SNAPSHOT/archiva-model-1.0-SNAPSHOT.pom" );

        ArchivaProjectModel effectiveModel = filter.filter( startModel );

        ArchivaProjectModel expectedModel = createArchivaProjectModel( "src/test/effective-poms/"
            + "/archiva-model-effective.pom" );

        assertModel( expectedModel, effectiveModel );
    }

    private ProjectModelResolverFactory initTestResolverFactory()
        throws Exception
    {
        ProjectModelResolverFactory resolverFactory = (ProjectModelResolverFactory) lookup( ProjectModelResolverFactory.class );

        resolverFactory.getCurrentResolverStack().clearResolvers();
        resolverFactory.getCurrentResolverStack().addProjectModelResolver( createDefaultRepositoryResolver() );

        return resolverFactory;
    }

    private void assertModel( ArchivaProjectModel expectedModel, ArchivaProjectModel effectiveModel )
    {
        assertEquals( "Equivalent Models", expectedModel, effectiveModel );

        assertContainsSameIndividuals( "Individuals", expectedModel.getIndividuals(), effectiveModel.getIndividuals() );
        dumpDependencyList( "Expected", expectedModel.getDependencies() );
        dumpDependencyList( "Effective", effectiveModel.getDependencies() );
        assertContainsSameDependencies( "Dependencies", expectedModel.getDependencies(), effectiveModel
            .getDependencies() );
        assertContainsSameDependencies( "DependencyManagement", expectedModel.getDependencyManagement(), effectiveModel
            .getDependencyManagement() );
    }

    private void dumpDependencyList( String type, List deps )
    {
        if ( deps == null )
        {
            System.out.println( " Dependencies [" + type + "] is null." );
            return;
        }

        if ( deps.isEmpty() )
        {
            System.out.println( " Dependencies [" + type + "] dependency list is empty." );
            return;
        }

        System.out.println( ".\\ [" + type + "] Dependency List (size:" + deps.size() + ") \\.________________" );
        Iterator it = deps.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();
            System.out.println( "  " + Dependency.toKey( dep ) );
        }
        System.out.println( "" );
    }

    private void assertEquivalentLists( String listId, List expectedList, List effectiveList )
    {
        if ( ( expectedList == null ) && ( effectiveList == null ) )
        {
            return;
        }

        if ( ( expectedList == null ) && ( effectiveList != null ) )
        {
            fail( "Effective [" + listId + "] List is instantiated, while expected List is null." );
        }

        if ( ( expectedList != null ) && ( effectiveList == null ) )
        {
            fail( "Effective [" + listId + "] List is null, while expected List is instantiated." );
        }

        assertEquals( "[" + listId + "] List Size", expectedList.size(), expectedList.size() );
    }

    private void assertContainsSameIndividuals( String listId, List expectedList, List effectiveList )
    {
        assertEquivalentLists( listId, expectedList, effectiveList );

        Map expectedMap = getIndividualsMap( expectedList );
        Map effectiveMap = getIndividualsMap( effectiveList );

        Iterator it = expectedMap.keySet().iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();

            assertTrue( "Should exist in Effective [" + listId + "] list: " + key, effectiveMap.containsKey( key ) );
        }
    }

    private void assertContainsSameDependencies( String listId, List expectedList, List effectiveList )
    {
        assertEquivalentLists( listId, expectedList, effectiveList );

        Map expectedMap = getDependencyMap( expectedList );
        Map effectiveMap = getDependencyMap( effectiveList );

        Iterator it = expectedMap.keySet().iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();

            assertTrue( "Should exist in Effective [" + listId + "] list: " + key, effectiveMap.containsKey( key ) );
        }
    }

    private Map getIndividualsMap( List deps )
    {
        Map map = new HashMap();
        Iterator it = deps.iterator();
        while ( it.hasNext() )
        {
            Object o = it.next();
            assertTrue( "List contains Individual entries. (found " + o.getClass().getName() + " instead)",
                        o instanceof Individual );
            Individual individual = (Individual) o;
            String key = individual.getEmail();
            map.put( key, individual );
        }
        return map;
    }

    private Map getDependencyMap( List deps )
    {
        Map map = new HashMap();
        Iterator it = deps.iterator();
        while ( it.hasNext() )
        {
            Object o = it.next();
            assertTrue( "List contains Dependency entries. (found " + o.getClass().getName() + " instead)",
                        o instanceof Dependency );
            Dependency dep = (Dependency) o;
            String key = Dependency.toVersionlessKey( dep );
            map.put( key, dep );
        }
        return map;
    }
}