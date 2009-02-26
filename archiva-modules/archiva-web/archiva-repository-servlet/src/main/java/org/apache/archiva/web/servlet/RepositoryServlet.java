package org.apache.archiva.web.servlet;

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

import javax.servlet.ServletConfig;
import org.apache.archiva.repository.api.RepositoryContext;
import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerFactory;
import org.apache.archiva.repository.api.ResourceContext;
import org.apache.archiva.repository.api.Status;
import org.apache.archiva.repository.api.interceptor.PostRepositoryInterceptor;
import org.apache.archiva.repository.api.interceptor.PreRepositoryInterceptor;
import org.apache.archiva.repository.api.interceptor.RepositoryInterceptorFactory;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.archiva.repository.api.InvalidOperationException;
import org.apache.archiva.repository.api.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class RepositoryServlet extends HttpServlet
{
    private static final Logger log = LoggerFactory.getLogger(RepositoryServlet.class);

    private RepositoryInterceptorFactory<PreRepositoryInterceptor> preRepositoryInterceptorFactory;

    private RepositoryInterceptorFactory<PostRepositoryInterceptor> postRepositoryInterceptorFactory;

    private RepositoryManagerFactory repositoryManagerFactory;

    private static final String MKCOL_METHOD = "MKCOL";

    private static final String LAST_MODIFIED = "last-modified";

    public static final String REPOSITORY_MANAGER_FACTORY = "repositoryManagerFactoryName";

    public static final String PREREPOSITORY_INTERCEPTOR_FACTORY = "preRepositoryInterceptorFactoryName";

    public static final String POSTREPOSITORY_INTERCEPTOR_FACTORY = "postRepositoryInterceptorFactoryName";

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        final String repositoryManagerFactoryName = config.getInitParameter(REPOSITORY_MANAGER_FACTORY);
        if (repositoryManagerFactoryName == null)
        {
            throw new ServletException(REPOSITORY_MANAGER_FACTORY + " cannot be null");
        }

        final String preRepositoryInterceptorFactoryName = config.getInitParameter(PREREPOSITORY_INTERCEPTOR_FACTORY);
        if (preRepositoryInterceptorFactoryName == null)
        {
            throw new ServletException(PREREPOSITORY_INTERCEPTOR_FACTORY + " cannot be null");
        }

        final String postRepositoryInterceptorFactoryName = config.getInitParameter(POSTREPOSITORY_INTERCEPTOR_FACTORY);
        if (postRepositoryInterceptorFactoryName == null)
        {
            throw new ServletException(POSTREPOSITORY_INTERCEPTOR_FACTORY + " cannot be null");
        }

        super.init(config);
        final ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
        repositoryManagerFactory = (RepositoryManagerFactory)applicationContext.getBean(repositoryManagerFactoryName);
        preRepositoryInterceptorFactory = (RepositoryInterceptorFactory<PreRepositoryInterceptor>)applicationContext.getBean(preRepositoryInterceptorFactoryName);
        postRepositoryInterceptorFactory = (RepositoryInterceptorFactory<PostRepositoryInterceptor>)applicationContext.getBean(postRepositoryInterceptorFactoryName);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.setHeader("Server", "Apache Archiva");
        resp.setDateHeader("Date", new Date().getTime());

        //Backwards compatability with the weddav wagon
        if (MKCOL_METHOD.equals(req.getMethod()))
        {
            resp.setStatus(HttpServletResponse.SC_CREATED);
            return;
        }
        super.service(req, resp);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        handleRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        handleRequest(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        handleRequest(req, resp);
    }

    private void handleRequest(final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException, ServletException
    {
        log.debug("Request started");
        HttpRepositoryContext context = new HttpRepositoryContext(req, resp);
        log.debug("Running PreRepositoryInterceptors");
        executePreRepositoryInterceptors(context);
        log.debug("Executing Repository Manager");
        try
        {
            runRepositoryManager(context, req, resp);
        }
        catch (InvalidOperationException e)
        {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, e.getMessage());
        }
        log.debug("Running PostRepositoryInterceptors");
        executePostRepositoryInterceptors(context);
        log.debug("Request Completed");
    }

    private void executePreRepositoryInterceptors(RepositoryContext context)
    {
        for (final PreRepositoryInterceptor interceptor : preRepositoryInterceptorFactory.getRepositoryInterceptors())
        {
            interceptor.intercept(context);
        }
    }

    private void executePostRepositoryInterceptors(RepositoryContext context)
    {
        for (final PostRepositoryInterceptor interceptor : postRepositoryInterceptorFactory.getRepositoryInterceptors())
        {
            interceptor.intercept(context);
        }
    }

    private void runRepositoryManager(final RepositoryContext context, final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException
    {
        for (final RepositoryManager manager : repositoryManagerFactory.getRepositoryManagers())
        {
            final ResourceContext resourceContext = manager.handles(context);
            if (resourceContext != null)
            {
                log.debug("Request handled by " + manager.toString());
                doContent(manager, resourceContext, req, resp);
                break;
            }
        }
    }

    private void doContent(final RepositoryManager repositoryManager, final ResourceContext context, final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException
    {
        if ("PUT".equals(req.getMethod()))
        {
            repositoryManager.write(context, req.getInputStream());
            resp.setStatus(HttpServletResponse.SC_CREATED);
            return;
        }

        final List<Status> results = repositoryManager.stat(context);

        if (!results.isEmpty())
        {
            final boolean withBody = !"HEAD".equals(req.getMethod());
            final Status status = results.get(0);
            if (ResourceType.Collection.equals(status.getResourceType()))
            {
                //If does not end with slash we should redirect
                if (!req.getRequestURI().endsWith("/" ))
                {
                    resp.sendRedirect(req.getRequestURI() + "/");
                    return;
                }

                Status collectionStatus = results.get(0);
                resp.setDateHeader(LAST_MODIFIED, collectionStatus.getLastModified());
                resp.setStatus(HttpServletResponse.SC_OK);

                IndexWriter.write(results, context, resp, withBody);
            }
            else
            {
                resp.setStatus(HttpServletResponse.SC_OK);

                if (status.getContentLength() >= 0)
                {
                    resp.setContentLength((int)status.getContentLength());
                }
                resp.setContentType(status.getContentType());
                resp.setDateHeader(LAST_MODIFIED, status.getLastModified());

                if (withBody)
                {
                    if (!repositoryManager.read(context, resp.getOutputStream()))
                    {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                }
            }
        }
        else
        {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find " + req.getPathInfo());
        }
    }
}
