<div class="row">
    <!-- Top Tabs -->
    <div class="col-xs-2">
        <ul class="nav nav-stacked nav-pills" role="tablist">
            <li class="nav-item active">
                <a class="nav-link active" id="text-tab" data-toggle="tab" href="#textqueryeditor"
                   role="tab" aria-controls="textqueryeditor" aria-selected="true" ng-click="CREATE.refresh(false)">Text</a></li>
            <li class="nav-item">
                <a class="nav-link" id="structured-tab" data-toggle="tab" href="#structurededitor"
                   role="tab" aria-controls="structurededitor" aria-selected="false" ng-click="CREATE.refresh(false)">Structured Data</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" id="cdm-tab" data-toggle="tab"
                   href="#cdmeditor" role="tab" aria-controls="cdmeditor"
                   aria-selected="false" ng-click="CREATE.refresh(false)">OHDSI CDM Objects</a>
            </li>
        </ul>
    </div>
    <!-- Content|Editors -->
    <div class="col-xs-10">
       <jsp:include page="search_forms.jsp"/>
    </div>
</div>