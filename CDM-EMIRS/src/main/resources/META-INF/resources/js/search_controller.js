var app = angular.module("querybuilder", []);
app.controller("QueryBuilderController", ['$scope', function ($scope) {
    // Dirty fix, circumvent loading problems by pulling directly from previously loaded
    this.cdmQuery = cdmLoad;
    this.initObjects = JSON.parse(JSON.stringify(cdmLoad)); // Make a copy so edits don't affect initial value
    this.unstructured = unstructuredLoad;
    this.addItem = function () {
        this.cdmQuery.push($scope.item)
    };
    this.removeItem = function ($toRemove) {
        this.cdmQuery.splice($toRemove, 1);
    };
    this.resetSoft = function() {
        this.cdmQuery = this.initObjects;
    };
    this.submit = function() {
        // TODO
    }
}]);
