var app = angular.module("cdm_editor", []);
app.controller("CDMTagsCtrl", function ($scope) {
    $scope.objects = [];
    $scope.addItem = function () {
        $scope.objects.push($scope.item)
    };
    $scope.removeItem = function ($toRemove) {
        $scope.objects.splice($toRemove, 1);
    };
    $scope.loadTags = function ($json) {
        $scope.objects = $json
    }
});