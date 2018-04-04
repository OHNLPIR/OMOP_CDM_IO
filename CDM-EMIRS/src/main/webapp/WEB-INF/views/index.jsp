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
<body ng-app="EMIRSApp" ng-controller="EMIRSCtrl as EMIRS" class="mt-0"
      style="background-color: rgb(226,226,226); height:100%">
<div class="row" id="page-header">
    <div class="col-xs-1 " id="logo" style="z-index: 100">
        <a href="<c:url value="/"/>">
            <img src="<c:url value="img/MC_stack_wht.png"/>" height="100px" alt="MC Logo"
                 style="float: left; padding: 15px; margin-top:0"/>
        </a>
    </div>
    <div class="col-sm-12" id="header-text">
        <h2 class="text-center"
            style="line-height:40px; height: 100px; padding-bottom: 30px;color: #ffffff; padding-top: 30px; margin:0 auto">
            Electronic
            Medical Information Retrieval
            System</h2>
    </div>
</div>
<!-- Navbar -->
<div class="navbar navbar-default">
    <div class="container-fluid">
        <ul class="nav navbar-nav" ng-if="EMIRS.model.completed === true ">
            <li ng-if="EMIRS.displayFilters"><a href="#" ng-click="EMIRS.displayFilters = false"><span
                    class="glyphicon glyphicon-th"></span>&nbsp;Hide Filter Options</a></li>
            <li ng-if="!EMIRS.displayFilters"><a href="#" ng-click="EMIRS.displayFilters = true"><span
                    class="glyphicon glyphicon-th"></span>&nbsp;Show Filter Options</a></li>
            <li><a href="#" data-toggle="modal" data-target="#search_modal"><span
                    class="glyphicon glyphicon-pencil"></span>&nbsp;Edit Current Query</a></li>
            <li ng-if="!EMIRS.model.isJudging"><a href="#" ng-click="EMIRS.model.isJudging = true"><span
                    class="glyphicon glyphicon-check"></span>&nbsp;Enter Relevance Judgement Mode</a></li>
            <li ng-if="EMIRS.model.isJudging"><a href="#" ng-click="EMIRS.model.isJudging = false"><span
                    class="glyphicon glyphicon-list-alt"></span>&nbsp;Exit Relevance Judgement Mode</a></li>
            <li><a href="#" ng-click="EMIRS.save()"><span class="glyphicon glyphicon-floppy-save"></span>&nbsp;Save
                Current Query</a></li>
        </ul>
        <ul class="nav navbar-nav navbar-right">
            <li ng-if="!EMIRS.model.loggedIn"><span class="glyphicon glyphicon-user"></span>&nbsp;<a href="#">Login</a>
            </li>
            <li class="dropdown" ng-if="EMIRS.model.loggedIn">
                <a class="dropdown-toggle" data-toggle="dropdown" href="#"><span
                        class="glyphicon glyphicon-user"></span>&nbsp;{{EMIRS.model.loggedIn}}<span
                        class="caret"></span></a>
                <ul class="dropdown-menu">
                    <li><a href="<c:url value="/"/>"><span class="glyphicon glyphicon-search"></span>&nbsp;&nbsp;New
                        Search</a></li>
                    <li><a href="#" data-toggle="modal" data-target="#load_save_modal"
                           ng-click="EMIRS.loadSaveList()"><span class="glyphicon glyphicon-folder-open"></span>&nbsp;&nbsp;Load
                        Saved
                        Search</a></li>
                    <li><a href="#"><span class="glyphicon glyphicon-log-out"></span>&nbsp;&nbsp;Logout</a></li>
                </ul>
            </li>
        </ul>
    </div>
</div>
<div id="search" ng-if="EMIRS.model.loggedIn && !(EMIRS.model.submitted || EMIRS.model.completed)">
    <!-- TODO: need server side validation of this instead -->
    <h2 class="text-center" style="padding-top:10px">Start a New Search</h2>
    <div
            style="padding-top: 5px; padding-bottom: 20px; border-bottom: 1px solid #808080">
        <!-- Search -->
        <div style="padding-left: 15px; padding-right: 15px;">
            <form name="search" ng-submit="EMIRS.submitQuery()">
                <jsp:include page="search_inline.jsp"/>
                <div class="row text-center">
                    <button type="submit" class="btn btn-default">Submit</button>
                </div>
            </form>
        </div>
    </div>
</div>
<div class="row" id="searching-for-results" ng-if="EMIRS.model.submitted === true && EMIRS.model.completed === false">
    <p class="col-xs-12 text-center">Searching...</p>
</div>
<div class="row" id="empty-results"
     ng-if="EMIRS.model.completed === true && EMIRS.model.loggedIn && EMIRS.model.hits.length === 0">
    <p class="col-xs-12 text-center">No Results Found</p>
</div>
<div class="row" id="results"
     ng-if="EMIRS.model.hits.length > 0 && EMIRS.model.completed === true && EMIRS.model.loggedIn"
     style="padding-left: 15px; padding-right: 15px">
    <div id="sidebar" class="col-sm-2 pull-left"
         ng-if="EMIRS.displayFilters">
        <label style="padding-top: 10px; padding-left: 10px; width:90%">
            Current Model View: <br/>
            <select class="input form-control"
                    ng-model="EMIRS.currView"
                    title="field"
                    ng-change="EMIRS.model.currentPage = 0">
                <option ng-repeat="view in EMIRS.VIEW_TYPES"
                        value="{{view}}">{{view}}
                </option>
            </select>
        </label>
        <!-- Patient ID Filter -->
        <label style="padding-top: 10px; padding-left: 10px; width: 90%">
            Patient ID({{EMIRS.model.docFilter.patientOptions.length}}): <br/>

            <select multiple class="form-control" ng-multiple="true" ng-model="EMIRS.model.docFilter.patients"
                    ng-options="id
                    for
                    id in EMIRS.model.docFilter.patientOptions">
            </select>
        </label>
        <!-- Section Type Filter -->
        <label style="padding-top: 10px; padding-left: 10px; width: 90%">
            Section Type: <br/>

            <select multiple size="{{EMIRS.model.docFilter.sectionOptions.length}}" class="form-control"
                    ng-multiple="true"
                    ng-model="EMIRS.model.docFilter.sections"
                    ng-options="section.id as section.name
                    for
                    section in EMIRS.model.docFilter.sectionOptions">
            </select>
        </label>
    </div>
    <div id="content" ng-class="EMIRS.displayFilters ? 'col-xs-10' : 'col-xs-12'"
         style="float:right; padding-top: 10px; border-left: 1px solid #808080">
        <!-- Results -->
        <!-- Group by Document -->
        <div class="panel-group" ng-if="EMIRS.currView === 'Document'">
            <div class="panel panel-default"
                 ng-repeat="hit in EMIRS.model.getHits() | startFrom:EMIRS.model.currentPage*EMIRS.model.pageSize | limitTo:EMIRS.model.pageSize">
                <div class="panel-heading clearfix">
                    <div class="panel-title pull-left h4">
                        <a data-toggle="collapse"
                           href="\#{{hit.doc.indexDocID}}"
                           ng-click="EMIRS.checkLoaded(hit.doc)">
                            {{hit.doc.sectionName}}
                            - MRN: {{hit.patient.id}}
                            - Id: {{hit.doc.docLinkId}}v{{hit.doc.revision}}
                            - Type: {{hit.doc.docType}}
                        </a>
                    </div>
                    <div class="panel-title pull-right">
                        <div class="pull-left" ng-if="EMIRS.model.isJudging == false">
                            Score: {{hit.score}}
                        </div>
                        <div class="btn-group pull-right" style="padding-left: 10px" data-toggle="buttons"
                             ng-if="EMIRS.model.isJudging == true">
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID] === 0}"
                                 ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = 0"
                                 title="Relevant">
                                &#10004;
                            </div>
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID] === 1}"
                                 ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = 1"
                                 title="Partially Relevant">
                                ~
                            </div>
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID] === 2}"
                                 ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = 2"
                                 title="Not Relevant">
                                &#10006;
                            </div>
                        </div>
                    </div>
                    <div class="clearfix"></div>
                </div>
                <div id="{{hit.doc.indexDocID}}"
                     class="panel-collapse collapse">
                    <div class="panel-body" style="white-space: pre-wrap;">{{hit.doc.text}}</div>
                </div>
            </div>
        </div>
        <!-- Group by Patient -->
        <div class="panel-group" ng-if="EMIRS.currView === 'Patient'">
            <div class="panel panel-default"
                 ng-repeat="hit in EMIRS.model.getPatientHits() | startFrom:EMIRS.model.currentPage*EMIRS.model.pageSize | limitTo:EMIRS.model.pageSize">
                <div class="panel-heading">
                    <div class="panel-title pull-left h4">
                        MRN: {{hit.patient.id}}
                    </div>
                    <div class="panel-title pull-right">
                        <div class="pull-left" ng-if="EMIRS.model.isJudging == false">
                            Documents: {{EMIRS.getHitsFor(hit.docs).length}} | Score:{{hit.score}}
                        </div>

                        <div class="btn-group pull-right" style="padding-left: 10px" data-toggle="buttons"
                             ng-if="EMIRS.model.isJudging == true">
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.patientJudgements[hit.patient.id] === 0}"
                                 ng-click="EMIRS.model.patientJudgements[hit.patient.id] = 0"
                                 title="Relevant">
                                &#10004;
                            </div>
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.patientJudgements[hit.patient.id] === 1}"
                                 ng-click="EMIRS.model.patientJudgements[hit.patient.id] = 1"
                                 title="Partially Relevant">
                                ~
                            </div>
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.patientJudgements[hit.patient.id] === 2}"
                                 ng-click="EMIRS.model.patientJudgements[hit.patient.id] = 2"
                                 title="Not Relevant">
                                &#10006;
                            </div>
                        </div>
                    </div>
                    <div class="clearfix"></div>
                </div>
                <div id="patient-{{hit.patient.id}}">
                    <div class="panel-body">
                        <ul class="list-unstyled">
                            <li>Gender: {{hit.patient.gender}}</li>
                            <li>Date of Birth: {{hit.patient.dob | date:'MM/dd/yyyy'}}</li>
                            <li>Ethnicity: {{hit.patient.ethnicity}}</li>
                            <li>Race: {{hit.patient.race}}</li>
                            <li>City: {{hit.patient.city}}</li>
                            <li><a href="#" data-toggle="modal" data-target="#doc_results"
                                   ng-click="EMIRS.showPatientSpecificResults(hit);">Show matched documents</a></li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
        <br/>
    </div>
</div>
<div class="row" id="pager-document-view" style="border-top: 1px solid #808080"
     ng-if="EMIRS.model.hits.length > 0 && EMIRS.model.completed === true && EMIRS.currView === 'Document'">
    <div class="col-xs-12 text-center">
        <ul class="pagination">
            <li ng-class="{disabled: EMIRS.model.currentPage === 0}">
                <a href="#"
                   ng-click="EMIRS.model.currentPage !== 0 ? EMIRS.model.currentPage = EMIRS.model.currentPage - 1 : ''">
                    &larr;
                </a>
            </li>
            <li ng-repeat="count in EMIRS.model.numberOfPages() | range:EMIRS.model.pageSize:EMIRS.model.currentPage+1"
                ng-class="{active: EMIRS.model.currentPage === count, disabled: !(count|isNum)}">
                <a href="#" ng-if="count|isNum" ng-click="EMIRS.model.currentPage = count">{{count+1}}</a>
                <a href="#" ng-if="!(count|isNum)">{{count}}</a>
            </li>
            <li ng-class="{disabled: EMIRS.model.currentPage === EMIRS.model.numberOfPages() - 1}">
                <a href="#"
                   ng-click="EMIRS.model.currentPage !== EMIRS.model.numberOfPages() - 1 ? EMIRS.model.currentPage = EMIRS.model.currentPage + 1 : ''">
                    &rarr;
                </a>
            </li>
        </ul>
    </div>
</div>
<div class="row" id="footer" style="border-top: 1px solid #808080; padding-top: 10px;"
     ng-if="EMIRS.model.loggedIn">
    <div class="col-xs-12 text-center" ng-if="EMIRS.model.hits.length > 0 && EMIRS.model.completed === true">
        Search Statistics | {{EMIRS.model.hits.length}} Documents | {{EMIRS.model.docFilter.patientOptions.length}}
        Patients
    </div>
</div>
<!-- Patient Document Results Modal -->
<div class="modal fade" id="doc_results" role="dialog">
    <div class="modal-dialog modal-lg" style="width:1200px">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Matched Documents</h4>
            </div>
            <div class="modal-body">
                <div class="panel-group" ng-if="EMIRS.currView === 'Patient'">
                    <div class="panel panel-default"
                         ng-repeat="hit in EMIRS.currPatientDocHits | startFrom:EMIRS.currentPage*EMIRS.pageSize | limitTo:EMIRS.pageSize">
                        <div class="panel-heading">
                            <div class="panel-title pull-left h4">
                                <a data-toggle="collapse"
                                   href="#personview-{{hit.doc.docLinkId}}v{{hit.doc.revision}}s{{hit.doc.sectionID}}"
                                   ng-click="EMIRS.checkLoaded(hit.doc)">
                                    {{hit.doc.sectionName}}
                                    - MRN: {{hit.patient.id}}
                                    - Id: {{hit.doc.docLinkId}}v{{hit.doc.revision}}
                                    - Type: {{hit.doc.docType}}
                                </a>
                            </div>
                            <div class="panel-title pull-right">
                                <div class="pull-left" ng-if="EMIRS.model.isJudging == false">
                                    Score: {{hit.score}}
                                </div>
                                <div class="btn-group pull-right" style="padding-left: 10px" data-toggle="buttons"
                                     ng-if="EMIRS.model.isJudging == true">
                                    <div class="btn btn-primary"
                                         ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID] === 0}"
                                         ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = 0"
                                         title="Relevant">
                                        &#10004;
                                    </div>
                                    <div class="btn btn-primary"
                                         ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID] === 1}"
                                         ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = 1"
                                         title="Partially Relevant">
                                        ~
                                    </div>
                                    <div class="btn btn-primary"
                                         ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID] === 2}"
                                         ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = 2"
                                         title="Not Relevant">
                                        &#10006;
                                    </div>
                                </div>
                            </div>
                            <div class="clearfix"></div>
                        </div>
                        <div id="personview-{{hit.doc.docLinkId}}v{{hit.doc.revision}}s{{hit.doc.sectionID}}"
                             class="panel-collapse collapse">
                            <div class="panel-body" style="white-space: pre-wrap;">{{hit.doc.text}}</div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <div class="row" id="modal-pager-patient-view" style="border-top: 1px solid #808080"
                     ng-if="EMIRS.currPatientDocHits.length > 0 && EMIRS.model.completed === true">
                    <div class="col-xs-12 text-center">
                        <ul class="pagination">
                            <li ng-class="{disabled: EMIRS.currentPage === 0}">
                                <a href="#"
                                   ng-click="EMIRS.currentPage !== 0 ? EMIRS.currentPage = EMIRS.currentPage - 1 : ''">
                                    &larr;
                                </a>
                            </li>
                            <li ng-repeat="count in EMIRS.numberOfPages() | range:EMIRS.pageSize:EMIRS.currentPage+1"
                                ng-class="{active: EMIRS.currentPage === count, disabled: !(count|isNum)}">
                                <a href="#" ng-if="count|isNum" ng-click="EMIRS.currentPage = count">{{count+1}}</a>
                                <a href="#" ng-if="!(count|isNum)">{{count}}</a>
                            </li>
                            <li ng-class="{disabled: EMIRS.currentPage === EMIRS.numberOfPages() - 1}">
                                <a href="#"
                                   ng-click="EMIRS.currentPage !== EMIRS.numberOfPages() - 1 ? EMIRS.currentPage = EMIRS.currentPage + 1 : ''">
                                    &rarr;
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<!-- Patient Document Results Modal -->
<div class="modal fade" id="search_modal" role="dialog">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Query Editor</h4>
            </div>
            <form name="search-editor" ng-submit="EMIRS.submitQuery()">
                <div class="modal-body">
                    <jsp:include page="search_modal.jsp"/>
                </div>
                <div class="modal-footer">
                    <button type="submit" class="btn btn-default" ng-click="EMIRS.submitQuery()" data-dismiss="modal">Submit</button>
                </div>
            </form>
        </div>
    </div>
</div>
<div class="modal fade" id="load_save_modal" role="dialog">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Load Saved Queries and Judgements</h4>
            </div>
            <div class="modal-body">
                <div class="row">
                    <div class="col-xs-12">
                        <table class="table table-striped">
                            <tr>
                                <th>Query Name</th>
                                <th>Query Text</th>
                                <th>Actions</th>
                            </tr>
                            <tr ng-repeat="(name,text) in EMIRS.saves">
                                <td>{{name}}</td>
                                <td>{{text}}</td>
                                <td><button class="btn btn-default" type="button" ng-click="EMIRS.loadSave(name)" data-dismiss="modal">Load</button><button class="btn btn-default" type="button" ng-click="EMIRS.deleteSave(name)">Delete</button></td>
                            </tr>
                        </table>
                    </div>
                </div>
            </div>
            <div class="modal-footer"></div>
        </div>
    </div>
</div>
</body>
</html>