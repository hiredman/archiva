package org.apache.maven.archiva.consumers;

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

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

import java.util.Date;
import java.util.List;

/**
 * A consumer of content (files) in the repository. 
 *
 * @version $Id$
 */
public interface RepositoryContentConsumer extends Consumer
{
    /**
     * Get the list of included file patterns for this consumer.
     * 
     * @return the list of {@link String} patterns. (example: <code>"**<span />/*.pom"</code>)
     */
    public List<String> getIncludes();
    
    /**
     * Get the list of excluded file patterns for this consumer.
     * 
     * @return the list of {@link String} patterns. (example: <code>"**<span />/*.pom"</code>) - (can be null for no exclusions)
     */
    public List<String> getExcludes();

    /**
     * <p>
     * Event that triggers at the beginning of a scan.
     * </p>
     * 
     * <p>
     * NOTE: This would be a good place to initialize the consumer, to lock any resources, and to
     * generally start tracking the scan as a whole.
     * </p>
     * 
     * @param repository the repository that this consumer is being used for.
     * @param whenGathered the start of the repository scan
     * @throws ConsumerException if there was a problem with using the provided repository with the consumer.
     */
    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered ) throws ConsumerException;

    /**
     * <p>
     * Event that triggers at the beginning of a scan, where you can also indicate whether the consumers will be
     * executed on an entire repository or on a specific resource.
     * </p>
     *
     * @see RepositoryContentConsumer#beginScan(org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration, java.util.Date )
     *
     * @param repository the repository that this consumer is being used for.
     * @param whenGathered the start of the repository scan
     * @param executeOnEntireRepo flags whether the consumer will be executed on an entire repository or just on a specific resource
     * @throws ConsumerException if there was a problem with using the provided repository with the consumer.
     */
    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException;

    /**
     * <p>
     * Event indicating a file is to be processed by this consumer.
     * </p> 
     * 
     * <p>
     * NOTE: The consumer does not need to process the file immediately, can can opt to queue and/or track
     * the files to be processed in batch.  Just be sure to complete the processing by the {@link #completeScan()} 
     * event.
     * </p>
     * 
     * @param path the relative file path (in the repository) to process.
     * @throws ConsumerException if there was a problem processing this file.
     */
    public void processFile( String path ) throws ConsumerException;

    /**
     *
     * @param path
     * @param executeOnEntireRepo
     * @throws Exception
     */
    public void processFile( String path, boolean executeOnEntireRepo ) throws Exception;
    
    /**
     * <p>
     * Event that triggers on the completion of a scan.
     * </p>
     * 
     * <p>
     * NOTE: If the consumer opted to batch up processing requests in the {@link #processFile(String)} event
     * this would be the last opportunity to drain any processing queue's.
     * </p>
     */
    public void completeScan();

    /**
     * 
     * @param executeOnEntireRepo
     * @throws Exception
     */
    public void completeScan( boolean executeOnEntireRepo );

    /**
     * Whether the consumer should process files that have not been modified since the time passed in to the scan
     * method.
     * @return whether to process the unmodified files
     */
    boolean isProcessUnmodified();        
}
