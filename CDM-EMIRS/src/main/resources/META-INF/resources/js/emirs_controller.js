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
        $http.post('/_search', {
            unstructured: model.query.unstructured,
            cdmQuery: model.query.cdmQuery
        }).then(function(resp) {
            if (resp.data != null) {
                model.query.unstructured = resp.data.query.unstructured;
                model.query.cdmQuery = resp.data.query.cdmQuery;
                model.hits = resp.data.hits;
                model.completed = true;
                model.submitted = false;
                filter.patients = [];
                filter.patientOptions = [];
                for (var i = 0; i < resp.data.patients.length; i++) {
                    if (!has(filter.patients, resp.data.patients[i].id)) {
                        filter.patients.push(resp.data.patients[i].id);
                        filter.patientOptions.push(resp.data.patients[i].id);
                    }
                }
                filter.sections = [];
                filter.sectionOptions = [];
                for (var j = 0; j < resp.data.hits.length; j++) {
                    if (!has(filter.sections, resp.data.hits[j].doc.sectionID)) {
                        filter.sections.push(resp.data.hits[j].doc.sectionID);
                        filter.sectionOptions.push({
                           id: resp.data.hits[j].doc.sectionID,
                            name: resp.data.hits[j].doc.sectionName
                        });
                    }
                }
            }
        });
    };
    this.refresh = function($http, force, callback, model, filter) {
        if (this.unstructured === null || this.unstructured.length === 0) {
            this.cdmQuery = [];
            if (callback) {
                callback($http, model, filter);
            }
        } else {
            if (force || this.unstructured !== this.lastRefresh) {
                var state = this;
                this.lastRefresh = this.unstructured;
                $http.post('/_cdm', this.unstructured).then(function(resp){
                    if (resp.data != null) {
                        state.cdmQuery = resp.data;
                    }
                    if (callback) {
                        callback($http, model, filter);
                    }
                });
            } else {
                if (callback) {
                    callback($http, model, filter);
                }
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

function ObjectMapping() {
    this.parent = '';
    this.properties = {}
}

function MappingDefinition(mapping) {
    /**
     * @type {Object.<String,ObjectMapping>}
     */
    this.mappings = mapping['index']['mappings'];
    this.cdmOptions = [];
    this.structOptions = [];
    var scope = this;
    this.initOpts = function() {
        for (var key in scope.mappings) {
            if (!scope.mappings.hasOwnProperty(key)) {
                continue;
            }
            if (scope.mappings[key].parent === "Document") {
                scope.cdmOptions.push(key);
            }
            if (scope.mappings[key].parent === "Person") {
                scope.structOptions.push(key);
            }
        }
    };
    this.initOpts();
}

var app = angular.module("EMIRSApp", []);
app.controller("EMIRSCtrl", function ($scope, $http) {
    this.model = new Model(new Query('', []), []);
    this.filter = new Filter();
    var scope = this;
    this.mappingInit = function() {
        return $http.get('/_mappings').then(function(resp) {
            scope.mappings = new MappingDefinition(resp.data);
        });
    };
    this.mappingInit();

    this.submitQuery = function () {
        this.model.submitted = true;
        this.model.completed = false;
        this.model.query.refresh($http, false, this.model.query.submit, this.model, this.filter);
    };

    this.refresh = function(force) {
        this.model.query.refresh($http, force);
    };
});
