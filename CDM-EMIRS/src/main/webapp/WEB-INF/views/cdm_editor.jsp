<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="results" scope="request" type="org.ohnlp.ir.emirs.model.QueryResult"/>

<script src="webjars/ng-tags-input/3.2.0/ng-tags-input.min.js"></script>
<script src="js/search_controller.js"></script>
<link href="css/search.css" rel="stylesheet">

<!-- First load CDM objects into the model from the supplied query -->
<script>
    app.value("$cdm", ${results.query.cdmQuery});
</script>
<div class="modal fade" id="cdm_query_editor" role="dialog" ng-app="cdm_editor" ng-controller="CDMTagsCtrl as cdm">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">CDM Query Object Editor</h4>
            </div>
            <div class="modal-body">
                <!-- Render the output objects-->
                <ul>
                    <li class="cdm_object_editable_container" ng-repeat="cdm_object in cdm.cdmQuery">
                        <div class="cdm_object_fields pull-left">
                            <ul style="list-style: none; padding: 3px;">
                                <li ng-repeat="(field, value) in cdm_object" ng-if="field != 'date' && field != 'model_type' && value.length > 0">
                                    <div class="cdm_field_content">
                                        <span>{{field}}: <span class="cdm_field_content_editable" contenteditable="true">{{value}}</span></span>
                                    </div>
                                </li>
                            </ul>
                        </div>
                        <div class="cdm_object_remove pull-right">
                            <span ng-click="cdm.removeItem($index)">&times;</span>
                        </div>
                    </li>
                </ul>
                <div class="clearfix"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default pull-left" ng-click="alert(cdm.cdmQuery)">Reset from Text Query</button>
                <button type="button" class="btn btn-default pull-left" ng-click="cdm.resetSoft()">Reset to Last Executed</button>
                <button type="button" class="btn btn-default pull-right" data-dismiss="modal">Save and Close</button>
            </div>
        </div>
    </div>
</div>


