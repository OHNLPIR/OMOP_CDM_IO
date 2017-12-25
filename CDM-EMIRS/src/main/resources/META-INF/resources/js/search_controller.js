var app = angular.module("querybuilder", ['ngResource']);
app.controller("QueryBuilderController", function ($scope, $resource) {
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
    this.resetSoft = function () {
        this.cdmQuery = this.initObjects;
    };
    this.submit = function () {
        var post = $resource('/');
        var result = post.save(this).$promise;
        result.then(function(data) {
            console.log(data)
        })

    }
});
