<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="results" scope="request" type="org.ohnlp.ir.emirs.model.QueryResult"/>

<script src="webjars/ng-tags-input/3.2.0/ng-tags-input.min.js"></script>
<script src="js/cdm_editor_controllers.js"></script>
<link href="webjars/ng-tags-input/3.2.0/ng-tags-input.min.css" rel="stylesheet" media="screen">
<link href="webjars/ng-tags-input/3.2.0/ng-tags-input.bootstrap.min.css" rel="stylesheet" media="screen">

<div class="col-xs-12 " id="cdm_query_editor" ng-app="cdm_editor" ng-controller="CDMTagsCtrl">
    <!-- First load CDM objects into the model from the supplied query -->
    {{loadTags(${results.query.cdmQuery})}}
    <!-- Render the output objects-->
    <ul>
        <li ng-repeat="cdm_object in objects">
            <div class="cdm_object_fields">
                <ul>
                    <li ng-repeat="(field, value) in cdm_object">
                        <div class="cdm_field_content">
                            <span>{{field}}: <span contenteditable="true">{{value}}</span></span>
                        </div>
                    </li>
                </ul>
            </div>
            <div class="cdm_object_remove">
                <span ng-click="removeItem($index)">&times;</span>
            </div>
        </li>
    </ul>
</div>

