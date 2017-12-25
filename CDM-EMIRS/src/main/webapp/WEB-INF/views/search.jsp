<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<jsp:useBean id="results" scope="request" type="org.ohnlp.ir.emirs.model.QueryResult"/>

<script src="webjars/angularjs/1.6.6/angular-resource.min.js"></script>
<script src="webjars/ng-tags-input/3.2.0/ng-tags-input.min.js"></script>
<script src="js/search_controller.js"></script>
<link href="css/search.css" rel="stylesheet">

<script>
    <c:choose>
        <c:when test="${not empty results.query}">
            var unstructuredLoad = '${results.query.unstructured}';
            var cdmLoad = ${results.query.cdmQuery};
        </c:when>
        <c:otherwise>
            var unstructuredLoad = '';
            var cdmLoad = [];
        </c:otherwise>
    </c:choose>
    <%--app.value("unstructured", unstructured);--%>
    <%--app.value("cdm", ${results.query.cdmQuery});--%>
    <%--app.value("$qb.cdm", ${results.query.cdmQuery});--%>
    <%--app.value("$qb.unstructured", ${results.query.unstructured});--%>
</script>
<div ng-app="querybuilder" class="container">
    <div ng-controller="QueryBuilderController as qb">
        <form name="search" novalidate ng-submit="qb.submit()">
            <label>
                Query
                <input type="text" title="Query" value="${results.query.unstructured}" ng-model="qb.unstructured"/>
            </label>
            <button type="submit">Submit</button>
            <ul class="list-inline">
                <li><a href="#" data-toggle="modal" data-target="#cdm_query_editor">Edit CDM Objects</a></li>
            </ul>
            <!-- CDM Query Editing -->
            <div class="modal fade" id="cdm_query_editor" role="dialog">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <button type="button" class="close" data-dismiss="modal">&times;</button>
                            <h4 class="modal-title">CDM Query Object Editor</h4>
                        </div>
                        <div class="modal-body">
                            <!-- Render the output objects-->
                            <ul>
                                <li class="cdm_object_editable_container" ng-repeat="cdm_object in qb.cdmQuery">
                                    <div class="cdm_object_fields pull-left">
                                        <ul style="list-style: none; padding: 3px;">
                                            <li ng-repeat="(field, value) in cdm_object"
                                                ng-if="field != 'date' && field != 'model_type' && value.length > 0">
                                                <div class="cdm_field_content">
                                                    <span>{{field}}: <span class="cdm_field_content_editable"
                                                                           contenteditable="true">{{value}}</span></span>
                                                </div>
                                            </li>
                                        </ul>
                                    </div>
                                    <div class="cdm_object_remove pull-right">
                                        <span ng-click="qb.removeItem($index)">&times;</span>
                                    </div>
                                </li>
                            </ul>
                            <div class="clearfix"></div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-default pull-left" ng-click="alert(qb.cdmQuery)">Reset
                                from Text Query
                            </button>
                            <button type="button" class="btn btn-default pull-left" ng-click="qb.resetSoft()">Reset to
                                Last Executed
                            </button>
                            <button type="button" class="btn btn-default pull-right" data-dismiss="modal">Save and
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>
    <form:form method="POST" action="/" modelAttribute="query">
        <form:label path="unstructured">Query</form:label>
        <form:input path="unstructured"/>
        <input type="submit" value="Submit"/>
    </form:form>
</div>

