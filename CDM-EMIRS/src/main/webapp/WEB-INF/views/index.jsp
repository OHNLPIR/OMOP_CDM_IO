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
                        <li><a href="#" data-toggle="modal" data-target="#structured_query_editor">Edit Structured
                            Data</a></li>
                        <li><a href="#" data-toggle="modal" data-target="#cdm_query_editor"
                               ng-click="EMIRS.refresh(false);">Edit CDM Objects</a></li>
                    </ul>
                    <!-- Structured Query Editing -->
                    <div class="modal fade" id="structured_query_editor" role="dialog">
                        <div class="modal-dialog modal-lg" style="width:1200px">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                                    <h4 class="modal-title">Structured Data Query Editor</h4>
                                </div>
                                <div class="modal-body">
                                    <div class="panel-group">
                                        <!-- One panel per structured data type -->
                                        <div class="row">
                                            <div class="col-xs-3 text-left" ng-if="EMIRS.structuredReferenceBar">
                                                <h3>Syntax Reference</h3>
                                                <ul class="list-unstyled">
                                                    <li></li>
                                                    <li class="h4">Boolean Logic</li>
                                                    <li>(X OR Y OR Z) &rArr; [x, y, z]</li>
                                                    <li>At least N of x,y,z &rArr; [x, y, z]^N</li>
                                                    <li></li>
                                                    <li class="h4">Ranges</li>
                                                    <li>Range x to y, inclusive &rArr; R[x, y]</li>
                                                    <li>Range x to y, exclusive &rArr; R(x, y)</li>
                                                    <li>? > x &rArr; R(x,)</li>
                                                    <li>? >= x &rArr; R[x,)</li>
                                                    <li>? < x &rArr; R(,x)</li>
                                                    <li>? <= x &rArr; R(,x]</li>
                                                    <li></li>
                                                    <li class="h4">Dates</li>
                                                    <li>Can be Represented Via (YYYY-MM-DD) including the parentheses
                                                    </li>
                                                    <li>Can be used with range and boolean syntax</li>
                                                </ul>
                                            </div>
                                            <div ng-class="EMIRS.structuredReferenceBar ? 'col-xs-9' : 'col-xs-12'"
                                                 ng-style="EMIRS.structuredReferenceBar ? {'border-left': '1px solid #e5e5e5'} : {}">
                                                <div class="panel panel-default"
                                                     ng-repeat="rType in EMIRS.mappings.structOptions">
                                                    <div class="panel-heading">
                                                        <div class="panel-title pull-left h4">{{rType === "Person" ?
                                                            "Demographics" : rType}}
                                                        </div>
                                                        <div class="clearfix"></div>
                                                    </div>
                                                    <div class="panel-body text-left">
                                                        <ul class="list-unstyled">
                                                            <li
                                                                    ng-repeat="clause in EMIRS.model.query.structured track by $index"
                                                                    ng-show="clause.recordType === rType"
                                                                    ng-init="clauseIdx = $index">
                                                                <div class="input-group col-xs-12">
                                                                    <select class="input-small"
                                                                            ng-model="EMIRS.model.query.structured[clauseIdx].type"
                                                                            title="filter">
                                                                        <option ng-repeat="type in EMIRS.CLAUSE_TYPES"
                                                                                value="{{type}}">{{type}}
                                                                        </option>
                                                                    </select>
                                                                    <select class="input-small"
                                                                            ng-model="EMIRS.model.query.structured[clauseIdx].field"
                                                                            title="field">
                                                                        <option value="" disabled selected hidden>Choose
                                                                            a
                                                                            Field...
                                                                        </option>
                                                                        <option ng-repeat="(field, ignored) in EMIRS.mappings.mappings[rType].properties"
                                                                                value="{{field}}">{{field}}
                                                                        </option>
                                                                    </select>
                                                                    <!-- TODO do fancy stuff with input types here -->
                                                                    <input type="text" class="input-small"
                                                                           ng-model="EMIRS.model.query.structured[clauseIdx].content"
                                                                           title="content" placeholder="Search Query"/>
                                                                    <div class="btn-group" style="padding-left: 5px;">
                                                                        <button type="button" class="btn btn-secondary"
                                                                                ng-click="EMIRS.model.query.addStructuredItem(rType);">
                                                                            &plus;
                                                                        </button>
                                                                        <button type="button" class="btn btn-secondary"
                                                                                ng-click="EMIRS.model.query.removeStructuredItem($index)">
                                                                            &minus;
                                                                        </button>
                                                                    </div>
                                                                </div>
                                                            </li>
                                                        </ul>
                                                        <div ng-if="(EMIRS.model.query.structured | filter: {recordType: rType}).length === 0">
                                                            <button type="button" class="btn btn-btn-default"
                                                                    ng-click="EMIRS.model.query.addStructuredItem(rType);">
                                                                Add
                                                                new clause
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-default pull-left"
                                            ng-click="EMIRS.structuredReferenceBar = !EMIRS.structuredReferenceBar">
                                        {{!EMIRS.structuredReferenceBar ? "Show" : "Hide"}} Query Syntax Reference
                                    </button>
                                    <button type="button" class="btn btn-default pull-right" data-dismiss="modal">Save
                                        and
                                        Close
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <!-- CDM Query Editing -->
                    <div class="modal fade" id="cdm_query_editor" role="dialog">
                        <div class="modal-dialog modal-lg" style="width:1200px">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal">&times;</button>
                                    <h4 class="modal-title">CDM Query Object Editor</h4>
                                </div>
                                <div class="modal-body">
                                    <!-- Render the output objects-->
                                    <ul style="padding-left: 20px;">
                                        <li class="cdm_object_editable_container"
                                            ng-repeat="cdm_object in EMIRS.model.query.cdmQuery track by $index"
                                            ng-init="objectIdx = $index">
                                            <div class="cdm_object_fields pull-left">
                                                <ul style="list-style: none; padding: 3px;">
                                                    <li ng-repeat="(field, value) in cdm_object"
                                                        ng-if="field != 'date' && field != 'model_type'">
                                                        <div class="cdm_field_content">
                                                            <label>
                                                                {{field}}:
                                                                <input type="text" class="cdm_field_content_editable"
                                                                       ng-model="EMIRS.model.query.cdmQuery[objectIdx][field]"/>
                                                            </label>
                                                        </div>
                                                    </li>
                                                </ul>
                                            </div>
                                            <div class="cdm_object_remove pull-right">
                                                <span ng-click="EMIRS.model.query.removeCDMItem($index)">&times;</span>
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
                                    <div class="dropdown pull-left" style="padding-left: 3px;">
                                        <button class="btn btn-primary dropdown-toggle" type="button"
                                                data-toggle="dropdown">Add
                                            CDM Object
                                            <span class="caret"></span></button>
                                        <ul class="dropdown-menu">
                                            <li ng-repeat="item in EMIRS.mappings.cdmOptions">
                                                <a href="#"
                                                   ng-click="EMIRS.model.query.newCDM(EMIRS.mappings, item)">{{item}}</a>
                                            </li>
                                        </ul>
                                    </div>
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
    <div id="content" class="col-xs-10"
         style="float:right; padding-top: 10px; border-left: 1px solid #808080">
        <!-- Results -->
        <!-- Group by Document -->
        <div class="panel-group" ng-if="EMIRS.currView === 'Document'">
            <div class="panel panel-default"
                 ng-repeat="hit in EMIRS.model.getHits() | startFrom:EMIRS.model.currentPage*EMIRS.model.pageSize | limitTo:EMIRS.model.pageSize">
                <div class="panel-heading clearfix">
                    <div class="panel-title pull-left h4">
                        <a data-toggle="collapse"
                           href="\#{{hit.doc.indexDocID}}">{{hit.doc.sectionName}}
                            - MRN: {{hit.patient.id}}
                            - Id: {{hit.doc.docLinkId}}v{{hit.doc.revision}}
                            - Type: {{hit.doc.docType}}
                        </a>
                    </div>
                    <div class="panel-title pull-right">
                        <div class="pull-left">
                            Score: {{hit.score}}
                        </div>
                        <div class="btn-group pull-right" style="padding-left: 10px" data-toggle="buttons">
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID]}"
                                 ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = true">
                                &#10004;
                            </div>
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID] === false}"
                                 ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = false">
                                &#10006;
                            </div>
                        </div>
                    </div>
                    <div class="clearfix"></div>
                </div>
                <div id="{{hit.doc.indexDocID}}"
                     class="panel-collapse collapse">
                    <div class="panel-body">{{hit.doc.text}}</div>
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
                        <div class="pull-left">
                            Documents: {{EMIRS.getHitsFor(hit.docs).length}} | Score:{{hit.score}}
                        </div>

                        <div class="btn-group pull-right" style="padding-left: 10px" data-toggle="buttons">
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.patientJudgements[hit.patient.id]}"
                                 ng-click="EMIRS.model.patientJudgements[hit.patient.id] = true">
                                &#10004;
                            </div>
                            <div class="btn btn-primary"
                                 ng-class="{active: EMIRS.model.patientJudgements[hit.patient.id] === false}"
                                 ng-click="EMIRS.model.patientJudgements[hit.patient.id] = false">
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
<div class="row" id="footer" style="border-top: 1px solid #808080; padding-top: 10px;">
    <div class="col-xs-10 text-center" ng-if="EMIRS.model.hits.length > 0 && EMIRS.model.completed === true">
        Search Statistics | {{EMIRS.model.hits.length}} Documents | {{EMIRS.model.docFilter.patientOptions.length}}
        Patients
    </div>
    <div class="col-xs-2 pull-right">
        <ul class="list-inline">
            <li><a href="#" data-toggle="modal" data-target="#query_load_modal">Load Saved Query</a></li>
            <li><a href="#" ng-click="EMIRS.exportToFile()">Save Current Query</a></li>
        </ul>

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
                                   href="#personview-{{hit.doc.docLinkId}}v{{hit.doc.revision}}s{{hit.doc.sectionID}}">{{hit.doc.sectionName}}
                                    - MRN: {{hit.patient.id}}
                                    - Id: {{hit.doc.docLinkId}}v{{hit.doc.revision}}
                                    - Type: {{hit.doc.docType}}
                                </a>
                            </div>
                            <div class="panel-title pull-right">
                                <div class="pull-left">
                                    Score: {{hit.score}}
                                </div>
                                <div class="btn-group pull-right" style="padding-left: 10px" data-toggle="buttons">
                                    <div class="btn btn-primary"
                                         ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID]}"
                                         ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = true">
                                        &#10004;
                                    </div>
                                    <div class="btn btn-primary"
                                         ng-class="{active: EMIRS.model.docJudgements[hit.doc.indexDocID] === false}"
                                         ng-click="EMIRS.model.docJudgements[hit.doc.indexDocID] = false">
                                        &#10006;
                                    </div>
                                </div>
                            </div>
                            <div class="clearfix"></div>
                        </div>
                        <div id="personview-{{hit.doc.docLinkId}}v{{hit.doc.revision}}s{{hit.doc.sectionID}}"
                             class="panel-collapse collapse">
                            <div class="panel-body">{{hit.doc.text}}</div>
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
<!-- File Upload Modal -->
<!-- Patient Document Results Modal -->
<div class="modal fade" id="query_load_modal" role="dialog">
    <div class="modal-dialog" style="width:1200px">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Upload Query</h4>
            </div>
            <div class="modal-body">
                <label>
                    Saved query file:
                    <input type="file" file-upload>
                </label>
            </div>
            <div class="modal-footer">
                <button type="submit" data-dismiss="modal" ng-click="EMIRS.loadUploaded()">Submit</button>
            </div>
        </div>
    </div>
</div>
</body>
</html>