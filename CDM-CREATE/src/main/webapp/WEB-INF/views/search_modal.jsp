<!-- Top Tabs -->
<ul class="nav nav-tabs" role="tablist">
    <li class="nav-item active">
        <a class="nav-link" id="text-tab" data-toggle="tab" href="#textqueryeditor"
           role="tab" aria-controls="textqueryeditor" aria-selected="true" ng-click="CREATE.refresh(false)">Text</a></li>
    <li class="nav-item">
        <a class="nav-link" id="structured-tab" data-toggle="tab" href="#structurededitor"
           role="tab" aria-controls="structurededitor" aria-selected="false" ng-click="CREATE.refresh(false)">Structured
            Data</a>
    </li>
    <li class="nav-item">
        <a class="nav-link" id="cdm-tab" data-toggle="tab"
           href="#cdmeditor" role="tab" aria-controls="cdmeditor"
           aria-selected="false" ng-click="CREATE.refresh(false)">OHDSI CDM Objects</a>
    </li>
</ul>
<!-- Content|Editors -->
<div class="row">
    <div class="col-xs-12" style="padding-top:10px;">
        <jsp:include page="search_forms.jsp"/>
    </div>
</div>