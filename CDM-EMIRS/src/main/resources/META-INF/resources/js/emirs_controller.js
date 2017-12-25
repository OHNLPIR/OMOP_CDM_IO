// Declare Types
function Query(unstructured, cdmQuery) {
    this.unstructured = unstructured;
    this.cdmQuery = cdmQuery;
    this.addItem = function () {
        this.cdmQuery.push($scope.item)
    };
    this.removeItem = function ($toRemove) {
        this.cdmQuery.splice($toRemove, 1);
    };
    this.resetSoft = function () {
        this.cdmQuery = this.initObjects;
    };

    this.submit = function ($http, model) {
        $http.post('/', this).then(function(data) {
            console.log(data.data);
            if (data.data != null) {
                model.query = data.data.query;
                model.hits = data.data.hits;
            }
        });
    }
}

/**
 *
 * @param {Query} query
 * @param {Array.<SearchHit>} hits
 * @param {Array.<Patient>} patients
 * @constructor
 */
function SearchResult(query, hits, patients) {
    this.query = query;
    this.hits = hits;
    this.patients = patients;
}

function Patient(id, gender, ethnicity, race, city, dob, hits) {
    this.id = id;
    this.gender = gender;
    this.ethnicity = ethnicity;
    this.race = race;
    this.city = city;
    this.dob = dob;
    this.hits = hits;
}

/**
 * @param patient
 * @param encounter
 * @param doc
 * @param score
 * @constructor
 */
function SearchHit(patient, encounter, doc, score) {
    this.patient = patient;
    this.encounter = encounter;
    this.doc = doc;
    this.score = score;
}

/**
 *
 * @param {Query} query
 * @param {Array.<SearchHit>} hits
 * @constructor
 */
function Model(query, hits) {
    this.query = query;
    this.hits = hits;
}

var app = angular.module("EMIRSApp", []);
app.controller("EMIRSCtrl", function ($scope, $http) {
    this.model = new Model(new Query('', []), []);

    this.submitQuery = function () {
        this.model.query.submit($http, this.model);
    }
});
