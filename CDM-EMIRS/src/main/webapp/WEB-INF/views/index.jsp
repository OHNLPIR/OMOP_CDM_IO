<!DOCTYPE html>
<jsp:useBean id="results" scope="request" class="org.ohnlp.ir.emirs.model.QueryResult"/>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:choose>
        <c:when test="${not empty param.title}">
            <title>${param.title} - EMIRS</title>
        </c:when>
        <c:otherwise>
            <title>EMIRS - Electronic Medical Information Retrieval System</title>
        </c:otherwise>
    </c:choose>

    <script src="webjars/jquery/3.2.1/jquery.min.js"></script>

    <link rel="stylesheet" href="webjars/bootstrap/3.3.7/css/bootstrap.min.css">
    <link rel="stylesheet" href="webjars/bootstrap/3.3.7/css/bootstrap-theme.min.css">
    <script src="webjars/bootstrap/3.3.7/js/bootstrap.min.js"></script>
    <script src="webjars/angularjs/1.6.6/angular.min.js"></script>
    <link rel="shortcut icon" href="<c:url value="img/favicon.ico"/>"/>
    <script src="js/emirs_controller.js"></script>
    <link href="css/search.css" rel="stylesheet">

</head>
<body ng-app="EMIRSApp" ng-controller="EMIRSCtrl as EMIRS" class="mt-0" style="background-color: #bebebe; height:100%">
<div class="row" id="header" style="background-color:#003DA5;height: 100px; margin:0; padding:0;">
    <div class="col-xs-1 " id="logo">
        <a href="<c:url value="/"/>">
            <img src="<c:url value="img/MC_stack_wht.png"/>" height="75px" alt="MC Logo"
                 style="float: left; padding-left: 15px; padding-top: 15px; margin-top:0"/>
        </a>
    </div>
    <div class="col-sm-11" id="header-text">
        <h2 class="text-center" style="padding-bottom: 30px;color: #ffffff; padding-top: 30px; margin:0 auto">Electronic
            Medical Information Retrieval
            System</h2>
    </div>
</div>
<div class="row" id="search">
    <div class="col-xs-12 text-center"
         style="padding-top: 20px; padding-bottom: 20px; border-bottom: 1px solid #808080">
        <!-- Search -->
        <div class="container">
            <div>
                <form name="search" ng-submit="EMIRS.submitQuery()">
                    <label>
                        Query
                        <input type="text" name="textQuery" title="Query" ng-model="EMIRS.model.query.unstructured"/>
                    </label>
                    <button type="submit">Submit</button>
                    <ul class="list-inline">
                        <li><a href="#" data-toggle="modal" data-target="#cdm_query_editor"
                               ng-click="EMIRS.refresh(false);">Edit CDM Objects</a></li>
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
                                        <li class="cdm_object_editable_container"
                                            ng-repeat="cdm_object in EMIRS.model.query.cdmQuery">
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
                                                <span ng-click="EMIRS.model.query.removeItem($index)">&times;</span>
                                            </div>
                                        </li>
                                    </ul>
                                    <div class="clearfix"></div>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-default pull-left"
                                            ng-click="EMIRS.refresh(true);">Reset
                                        from Text Query
                                    </button>
                                    <button type="button" class="btn btn-default pull-right" data-dismiss="modal">Save
                                        and
                                        Close
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<div class="row" id="searching-for-results" ng-if="EMIRS.model.submitted === true && EMIRS.model.completed === false">
    <p class="col-xs-12 text-center">Searching...</p>
</div>
<div class="row" id="empty-results" ng-if="EMIRS.model.completed === true && EMIRS.model.hits.length === 0">
    <p class="col-xs-12 text-center">No Results Found</p>
</div>
<div class="row" id="results" ng-if="EMIRS.model.hits.length > 0 && EMIRS.model.completed === true">
    <div id="sidebar" class="col-sm-2"
         style="float:left;">
        <!-- Sidebar -->
        <label style="padding-top: 10px; padding-left: 10px; width: 90%">
            Patient IDs: <br/>

            <select multiple class="form-control" ng-multiple="true" ng-model="EMIRS.filter.patients"
                    ng-options="id for id in EMIRS.filter.patientOptions">
            </select>
        </label>
    </div>
    <div id="content" class="col-xs-10"
         style="float:right; padding-top: 10px; border-left: 1px solid #808080">
        <!-- Results -->
        <div class="panel-group">
            <div class="panel panel-default" ng-repeat="hit in EMIRS.model.hits"
                 ng-if="EMIRS.filter.shouldDisplay(hit)">
                <div class="panel-heading">
                    <div class="panel-title pull-left h4">
                        <a data-toggle="collapse"
                           href="\#{{hit.doc.docLinkId}}v{{hit.doc.revision}}s{{hit.doc.sectionID}}">{{hit.doc.sectionName}}
                            - MRN: {{hit.patient.id}}
                            - Id: {{hit.doc.docLinkId}}v{{hit.doc.revision}}
                            - Type: {{hit.doc.docType}}
                        </a>
                    </div>
                    <div class="panel-title pull-right">Score: {{hit.score}}</div>
                    <div class="clearfix"></div>
                </div>
                <div id="{{hit.doc.docLinkId}}v{{hit.doc.revision}}s{{hit.doc.sectionID}}"
                     class="panel-collapse collapse">
                    <div class="panel-body">{{hit.doc.text}}</div>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="row" id="footer" style="border-top: 1px solid #808080">

</div>

</body>
</html>