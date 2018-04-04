<div class="tab-content" id="search-tabcontent">
    <div class="tab-pane fade in active" id="textqueryeditor" role="tabpanel"
         aria-labelledby="text-tab">
        <div class="form-group">
                                       <textarea class="form-control" rows="20" name="textQuery" title="Query"
                                                 ng-model="EMIRS.model.query.unstructured">
                                       </textarea>
        </div>
    </div>
    <div class="tab-pane fade" id="structurededitor" role="tabpanel"
         aria-labelledby="structured-tab">
        <div class="form-group">
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
            <div class="row">
                <div class="col-xs-12">
                    <button type="button" class="btn btn-default pull-left"
                            ng-click="EMIRS.structuredReferenceBar = !EMIRS.structuredReferenceBar">
                        {{!EMIRS.structuredReferenceBar ? "Show" : "Hide"}} Query Syntax Reference
                    </button>
                </div>
            </div>
        </div>
    </div>
    <div class="tab-pane fade" id="cdmeditor" role="tabpanel"
         aria-labelledby="cdm-tab">
        <div class="row">
            <div class="col-xs-12">
                <div class="form-group">
                    <p ng-if="EMIRS.model.query.cdmQuery.length == 0">No currently used CDM objects: add a text query or add a CDM object to get started</p>
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
            </div>
        </div>
        <div class="row">
            <div class="col-xs-12">
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
            </div>
        </div>
    </div>
</div>