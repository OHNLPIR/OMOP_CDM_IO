<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<div class="container">
    <div ng-app="cdm_editor" ng-controller="CDMTagsCtrl">
        <ng-form name=""
    </div>
    <form:form method="POST" action="/" modelAttribute="query">
        <form:label path="unstructured">Query</form:label>
        <form:input path="unstructured"/>
        <input type = "submit" value = "Submit" />
        <ul class="list-inline">
            <li><a href="#" data-toggle="modal" data-target="#cdm_query_editor">Edit CDM Objects</a></li>
        </ul>
    </form:form>
    <jsp:include page="cdm_editor.jsp"/>
</div>

