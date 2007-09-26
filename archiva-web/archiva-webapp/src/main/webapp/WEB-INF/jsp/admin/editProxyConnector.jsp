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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:url var="iconDeleteUrl" value="/images/icons/delete.gif"/>
<c:url var="iconCreateUrl" value="/images/icons/create.png"/>

<c:choose>
  <c:when test="${mode == 'edit'}">
    <c:set var="addedit" value="Edit"/>
  </c:when>
  <c:otherwise>
    <c:set var="addedit" value="Add"/>
  </c:otherwise>
</c:choose>

<html>
<head>
  <title>Admin : ${addedit} Proxy Connector</title>
  <ww:head/>
</head>

<body>

<h1>Admin : ${addedit} Proxy Connector</h1>

<div id="contentArea">

<ww:actionerror/>
<ww:actionmessage/>

<ww:form name="saveProxyConnector" method="post" action="saveProxyConnector" namespace="/admin">
<ww:hidden name="mode"/>

<input type="hidden" name="pattern"/>
<ww:select name="connector.proxyId" list="proxyIdOptions" label="Network Proxy" required="true"/>
<ww:select name="connector.sourceRepoId" list="managedRepoIdList"
           label="Managed Repository" required="true"/>
<ww:select name="connector.targetRepoId" list="remoteRepoIdList"
           label="Remote Repository" required="true"/>

<tr>
  <td valign="top"><label>Policies:</label>
  </td>
  <td>
    <table>
      <c:forEach items="${policyMap}" var="policy" varStatus="i">
        <tr>
          <td>
            <ww:label for="policy_${policy.key}" required="true"
                      theme="simple">${policy.key}:
            </ww:label>
          </td>
          <td>
            <ww:select name="connector.policies['${policy.key}']"
                       list="policyMap['${policy.key}'].options"
                       value="connector.policies['${policy.key}']"
                       id="policy_${policy.key}"
                       theme="simple"
                       cssStyle="width: 10em"/>
          </td>
        </tr>
      </c:forEach>
    </table>
  </td>
</tr>

<tr class="seperator">
  <td valign="top">
    <label for="propertiesEntry">Properties:</label>
  </td>
  <td>
    <ww:textfield name="propertyKey" size="15" id="propertiesEntry" theme="simple"
                  onkeypress="submitenter(event, 'editProxyConnector!addProperty.action')"/>
    :
    <ww:textfield name="propertyValue" size="15" id="propertiesValue" theme="simple"
                  onkeypress="submitenter(event, 'editProxyConnector!addProperty.action')"/>
    <ww:submit name="action:editProxyConnector!addProperty" value="Add Property" theme="simple"/>
  </td>
</tr>

<tr>
  <td>
  </td>
  <td>
    <c:choose>
      <c:when test="${empty(connector.properties)}">
        <i>No properties have been set.</i>
      </c:when>
      <c:otherwise>
        <ww:url id="removePropertyUrl"
                action="editProxyConnector"
                method="removeProperty"/>
        <table>
          <c:forEach items="${connector.properties}" var="property" varStatus="i">
            <tr>
              <td>
                <ww:label for="property_${property.key}"
                          theme="simple">${property.key}</ww:label>
              </td>
              <td>
                <ww:textfield name="connector.properties['${property.key}']"
                              size="15"
                              id="property_${property.key}"
                              theme="simple"/>
              </td>
              <td>
                <ww:a href="#" title="Remove [${property.key}] Property"
                      onclick="setAndSubmit('propertyKey', '${property.key}', '%{removePropertyUrl}')"
                      theme="simple">
                  <img src="${iconDeleteUrl}"/></ww:a>

              </td>
            </tr>
          </c:forEach>
        </table>
      </c:otherwise>
    </c:choose>
  </td>
</tr>

<tr class="seperator">
  <td valign="top">
    <label for="blackListEntry">Black List:</label>
  </td>
  <td>
    <ww:textfield name="blackListPattern" size="30" id="blackListEntry" theme="simple"
                  onkeypress="submitenter(event, 'editProxyConnector!addBlackListPattern.action')"/>
    <ww:submit name="action:editProxyConnector!addBlackListPattern" value="Add Pattern" theme="simple"/>
  </td>
</tr>

<tr>
  <td>
  </td>
  <td>
    <ww:url id="removeBlackListPatternUrl"
            action="editProxyConnector"
            method="removeBlackListPattern"/>
    <c:choose>
      <c:when test="${empty(connector.blackListPatterns)}">
        <i>No black list patterns have been set.</i>
      </c:when>
      <c:otherwise>
        <table>
          <c:forEach items="${connector.blackListPatterns}" var="pattern" varStatus="i">
            <tr>
              <td>
                <ww:hidden name="connector.blackListPatterns" value="${pattern}"/>
                <code>"${pattern}"</code>
              </td>
              <td>
                <ww:a href="#" title="Remove [${pattern}] Pattern"
                      onclick="setAndSubmit('pattern', '${pattern}', '%{removeBlackListPatternUrl}')"
                      theme="simple">
                  <img src="${iconDeleteUrl}"/></ww:a>
              </td>
            </tr>
          </c:forEach>
        </table>
      </c:otherwise>
    </c:choose>
  </td>
</tr>

<tr class="seperator">
  <td valign="top">
    <label for="whiteListEntry">White List:</label>
  </td>
  <td>
    <ww:textfield name="whiteListPattern" size="30" id="whiteListEntry" theme="simple"
                  onkeypress="submitenter(event, 'editProxyConnector!addWhiteListPattern.action')"/>
    <ww:submit name="action:editProxyConnector!addWhiteListPattern" value="Add Pattern" theme="simple"/>
  </td>
</tr>
<tr>
  <td>
  </td>
  <td>
    <ww:url id="removeWhiteListPatternUrl"
            action="editProxyConnector"
            method="removeWhiteListPattern"/>
    <c:choose>
      <c:when test="${empty(connector.whiteListPatterns)}">
        <i>No white list patterns have been set.</i>
      </c:when>
      <c:otherwise>
        <table>
          <c:forEach items="${connector.whiteListPatterns}" var="pattern" varStatus="i">
            <tr>
              <td>
                <ww:hidden name="connector.whiteListPatterns" value="${pattern}"/>
                <code>"${pattern}"</code>
              </td>
              <td>
                <ww:a href="#" title="Remove [${pattern}] Pattern"
                      onclick="setAndSubmit('pattern', '${pattern}', '%{removeWhiteListPatternUrl}')"
                      theme="simple">
                  <img src="${iconDeleteUrl}"/></ww:a>
              </td>
            </tr>
          </c:forEach>
        </table>
      </c:otherwise>
    </c:choose>
  </td>
</tr>


<ww:submit value="Save Proxy Connector"/>
</ww:form>

<script type="text/javascript">
  <!--
  document.getElementById("saveProxyConnector_proxyId").focus();

  function setAndSubmit( id, value, action )
  {
    var f = document.forms['saveProxyConnector'];

    f.action = action;
    f.elements[id].value = value;
    f.submit();
  }

  function submitForm( action )
  {
    var f = document.forms['saveProxyConnector'];

    f.action = action;
    f.submit();
  }

  function submitenter( e, action )
  {
    var keycode;

    if ( window.event )
    {
      keycode = window.event.keyCode;
    }
    else if ( e )
    {
      keycode = e.which;
    }
    else
    {
      return true;
    }

    if ( keycode == 13 )
    {
      submitForm(action);
      return false;
    }
    else
    {
      return true;
    }
  }

  //-->
</script>

</div>

</body>
</html>