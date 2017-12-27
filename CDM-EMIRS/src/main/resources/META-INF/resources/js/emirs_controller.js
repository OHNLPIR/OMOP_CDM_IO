// Utility method
/**
 * @param {Array} arr
 * @param element
 */
function has(arr, element) {
    for (var i = 0; i < arr.length; i++) {
        if (arr[i] === element) {
            return true;
        }
    }
    return false;
}
// Declare Types
function Query(unstructured, cdmQuery) {
    /**
     * @type {String}
     */
    this.unstructured = unstructured;
    this.lastRefresh = (' ' + unstructured).slice(1); // Force a string copy because chrome retains reference otherwise
    this.cdmQuery = cdmQuery;
    this.addItem = function () {
        this.cdmQuery.push($scope.item)
    };
    this.removeItem = function ($toRemove) {
        this.cdmQuery.splice($toRemove, 1);
    };
    /**
     *
     * @param $http
     * @param {Model} model
     * @param {Filter} filter
     */
    this.submit = function ($http, model, filter) {
        $http.post('/_search', this).then(function(data) {
            if (data.data != null) {
                model.query.unstructured = data.data.query.unstructured;
                model.query.cdmQuery = data.data.query.cdmQuery;
                model.hits = data.data.hits;
                model.completed = true;
                model.submitted = false;
                filter.patients = [];
                filter.patientOptions = [];
                for (var i = 0; i < data.data.patients.length; i++) {
                    if (!has(filter.patients, data.data.patients[i].id)) {
                        filter.patients.push(data.data.patients[i].id);
                        filter.patientOptions.push(data.data.patients[i].id);
                    }
                }
                filter.sections = [];
                filter.sectionOptions = [];
                for (var j = 0; j < data.data.hits.length; j++) {
                    if (!has(filter.sections, data.data.hits[j].doc.sectionID)) {
                        filter.sections.push(data.data.hits[j].doc.sectionID);
                        filter.sectionOptions.push({
                           id: data.data.hits[j].doc.sectionID,
                            name: data.data.hits[j].doc.sectionName
                        });
                    }
                }
            }
        });
    };
    this.refresh = function($http, force) {
        if (this.unstructured === null || this.unstructured.length === 0) {
            this.cdmQuery = [];
        } else {
            if (force || this.unstructured !== this.lastRefresh) {
                var state = this;
                this.lastRefresh = this.unstructured;
                $http.post('/_cdm', this.unstructured).then(function(data){
                    if (data.data != null) {
                        state.cdmQuery = data.data;
                    }
                });
            }
        }
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
    this.completed = false;
    this.submitted = false;
}

function Filter() {
    /**
     * @type {Array}
     */
    this.patients = [];
    this.patientOptions = [];

    this.sections = [];
    this.sectionOptions = [];
    /**
     * @param {SearchHit} hit
     */
    this.shouldDisplay = function(hit) {
        return has(this.patients, hit.patient.id) && has(this.sections, hit.doc.sectionID);
    }
}

var app = angular.module("EMIRSApp", []);
app.controller("EMIRSCtrl", function ($scope, $http) {
    this.model = new Model(new Query('', []), []);
    this.filter = new Filter();
    this.submitQuery = function () {
        this.model.submitted = true;
        this.model.completed = false;
        this.model.query.refresh($http, false);
        this.model.query.submit($http, this.model, this.filter);
    };

    this.refresh = function(force) {
        this.model.query.refresh($http, force);
    };
});
