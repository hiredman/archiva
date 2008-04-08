<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>

<html>
<head>
  <title>Admin: Delete Network Proxy</title>
  <ww:head/>
</head>

<body>

<h1>Admin: Delete Network Proxy</h1>

<ww:actionerror/>

<div id="contentArea">

  <h2>Delete Network Proxy</h2>

  <blockquote>
    <strong><span class="statusFailed">WARNING:</span> This operation can not be undone.</strong>
  </blockquote>

  <p>
    Are you sure you want to delete network proxy <code>${proxyid}</code> ?
  </p>

  <ww:form method="post" action="deleteNetworkProxy!delete" namespace="/admin" validate="true">
    <ww:hidden name="proxyid"/>
    <ww:submit value="Delete"/>
  </ww:form>
</div>

</body>
</html>