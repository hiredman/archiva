package org.apache.maven.archiva.indexer.lucene;

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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.QueryParser;

/**
 * The important bits and pieces for handling a specific lucene index    
 *
 * @version $Id$
 */
public interface LuceneIndexHandlers
{
    /**
     * Get the converter to use with this index.
     * 
     * @return the converter to use.
     */
    public LuceneEntryConverter getConverter();
    
    /**
     * Get the analyzer to user with this index. 
     * 
     * @return the analzer to use.
     */
    public Analyzer getAnalyzer();
    
    /**
     * Get the {@link QueryParser} appropriate for searches within this index.
     * 
     * @return the query parser.
     */
    public QueryParser getQueryParser();

    /**
     * Get the id of the index handler.
     * 
     * @return the id of the index handler.
     */
    public String getId();
}