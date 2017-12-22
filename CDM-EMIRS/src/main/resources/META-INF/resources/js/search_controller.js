var app = angular.module("cdm_editor", []);
app.controller("CDMTagsCtrl", function ($scope, $obj) {
    this.cdmData = $obj;
    this.initObjects = $obj;
    this.addItem = function () {
        this.cdmData.push($scope.item)
    };
    this.removeItem = function ($toRemove) {
        this.cdmData.splice($toRemove, 1);
    };
    this.resetSoft = function() {
        this.cdmData = this.initObjects;
    }
});
