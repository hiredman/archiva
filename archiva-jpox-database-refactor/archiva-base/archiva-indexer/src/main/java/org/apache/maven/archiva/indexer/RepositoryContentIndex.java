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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.lucene.queryParser.QueryParser;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.indexer.query.Query;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Common access methods for a Repository Content index.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface RepositoryContentIndex
{
    /**
     * Indexes the records.
     *
     * @param records list of {@link LuceneRepositoryContentRecord} objects.
     * @throws RepositoryIndexException if there is a problem indexing the records.
     */
    void indexRecords( Collection records )
        throws RepositoryIndexException;

    /**
     * Modify (potentially) existing records in the index.
     * 
     * @param records the collection of {@link LuceneRepositoryContentRecord} objects to modify in the index.
     * @throws RepositoryIndexException if there is a problem modifying the records.
     */
    public void modifyRecords( Collection records )
        throws RepositoryIndexException;

    /**
     * Modify an existing (potential) record in the index.
     *  
     * @param record the record to modify.
     * @throws RepositoryIndexException if there is a problem modifying the record.
     */
    public void modifyRecord( LuceneRepositoryContentRecord record )
        throws RepositoryIndexException;

    /**
     * Search the index based on the search criteria specified. Returns a list of index records.
     *
     * @param query The query that contains the search criteria
     * @return the index records found
     * @throws RepositoryIndexSearchException if there is a problem searching
     * @todo should it return "SearchResult" instances that contain the index record and other search data (like score?)
     */
    List search( Query query )
        throws RepositoryIndexSearchException;

    /**
     * Check if the index already exists.
     *
     * @return true if the index already exists
     * @throws RepositoryIndexException if the index location is not valid
     */
    boolean exists()
        throws RepositoryIndexException;

    /**
     * Delete records from the index. Simply ignore the request any did not exist.
     *
     * @param records the records to delete
     * @throws RepositoryIndexException if there is a problem removing the record
     */
    void deleteRecords( Collection records )
        throws RepositoryIndexException;

    /**
     * Retrieve all records in the index.
     *
     * @return the collection of {@link LuceneRepositoryContentRecord} objects.
     * @throws RepositoryIndexSearchException if there was an error searching the index
     */
    Collection getAllRecords()
        throws RepositoryIndexSearchException;

    /**
     * Retrieve all primary keys of records in the index.
     *
     * @return the keys
     * @throws RepositoryIndexException if there was an error searching the index
     */
    Collection getAllRecordKeys()
        throws RepositoryIndexException;

    /**
     * Get the index directory.
     * 
     * @return the index directory.
     */
    File getIndexDirectory();

    /**
     * Get the {@link QueryParser} appropriate for searches within this index.
     * 
     * @return the query parser;
     */
    QueryParser getQueryParser();

    /**
     * Get the id of index.
     * 
     * @return the id of index.
     */
    String getId();
}
