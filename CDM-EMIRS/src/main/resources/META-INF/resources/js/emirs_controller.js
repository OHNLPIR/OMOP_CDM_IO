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
function Clause(type) {
    this.type = "Should";
    this.recordType = type;
    this.field = '';
    this.content = '';
}
function Query(unstructured, cdmQuery) {
    /**
     * @type {String}
     */
    this.unstructured = unstructured;
    /**
     * @type {Array.<Clause>}
     */
    this.structured = [];
    this.lastRefresh = (' ' + unstructured).slice(1); // Force a string copy because chrome retains reference otherwise
    this.cdmQuery = cdmQuery;
    this.addCDMItem = function ($item) {
        this.cdmQuery.push($item)
    };
    this.removeCDMItem = function ($toRemove) {
        this.cdmQuery.splice($toRemove, 1);
    };

    this.addStructuredItem = function ($type) {
        this.structured.push(new Clause($type));
    };
    this.removeStructuredItem = function ($index) {
        this.structured.splice($index, 1);
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
            structured: model.query.structured,
            cdmQuery: model.query.cdmQuery
        }).then(function (resp) {
            if (resp.data != null) {
                model.query.unstructured = resp.data.query.unstructured;
                model.query.structured = resp.data.query.structured;
                model.query.cdmQuery = resp.data.query.cdmQuery;
                model.hits = resp.data.hits;
                model.patientHits = resp.data.patientHits;
                model.completed = true;
                model.submitted = false;
                model.docJudgements = {};
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
    this.refresh = function ($http, force, callback, model, filter) {
        if (this.unstructured === null || this.unstructured.length === 0) {
            this.cdmQuery = [];
            if (callback) {
                callback($http, model, filter);
            }
        } else {
            if (force || this.unstructured !== this.lastRefresh) {
                var state = this;
                this.lastRefresh = this.unstructured;
                $http.post('/_cdm', this.unstructured).then(function (resp) {
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
    };

    /**
     * @param {MappingDefinition} mappings
     * @param {String} item
     */
    this.newCDM = function (mappings, item) {
        var mappingConfig = mappings.mappings[item];
        var newObj = {
            model_type: item
        };
        for (var key in mappingConfig.properties) {
            if (!mappingConfig.properties.hasOwnProperty(key)) {
                continue;
            }
            if (key !== 'date' && key !== 'type') {
                newObj[key] = '';
            }
        }
        this.cdmQuery.push(newObj);
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
 * @param {Patient} patient
 * @param {Array.<SearchHit>} docs
 * @param {float} score
 * @constructor
 */
function PatientHit(patient, docs, score) {
    this.patient = patient;
    this.docs = docs;
    this.score = score;
}

/**
 *
 * @param {Query} query
 * @param {Array.<SearchHit>} hits
 * @param {Array.<PatientHit>} patientHits
 * @constructor
 */
function Model(query, hits, patientHits) {
    // -- Model Attributes
    this.query = query;
    this.hits = hits;
    this.patientHits = patientHits;
    this.docFilter = new Filter();
    this.docJudgements = {};
    this.patientJudgements = {};
    // -- Status
    this.completed = false;
    this.submitted = false;
    // -- Pagination
    this.currentPage = 0;
    this.pageSize = 50;
    this.currPageCount = 0;
    this.currNumPagesArr = [];
    // -- Functions (Document View)
    this.numberOfPages = function () {
        return Math.ceil(this.getHits().length / this.pageSize);
    };

    this.getHits = function () {
        var ret = [];
        for (var i = 0; i < this.hits.length; i++) {
            if (this.docFilter.shouldDisplay(this.hits[i])) {
                ret.push(this.hits[i]);
            }
        }
        return ret;
    };

    /**
     * @returns {Array<PatientHit>|*}
     */
    this.getPatientHits = function () {
        return this.patientHits;
    };

    this.getNumPagesAsArr = function () { // Hack to allow for ng-repeat on set integer value
        var limit = this.numberOfPages();
        if (limit === this.currPageCount) {
            return this.currNumPagesArr;
        } else {
            var ret = [];
            for (var i = 0; i < limit; i++) {
                ret.push(i);
            }
            this.currNumPagesArr = ret;
            this.currPageCount = limit;
            return ret;
        }
    };
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
    this.shouldDisplay = function (hit) {
        return has(this.patients, hit.patient.id) && has(this.sections, hit.doc.sectionID);
    };
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
    this.initOpts = function () {
        scope.structOptions.push("Person");
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
    // Global Constants
    this.CLAUSE_TYPES = ["Must", "Should", "Should Not", "Must Not"];
    this.VIEW_TYPES = ["Document", "Patient"];
    // Model and functions
    this.currView = "Document";
    // Patient view pagination TODO really messy...
    this.currPatientDocHits = [];
    this.currentPage = 0;
    this.pageSize = 15;
    this.currPageCount = 0;
    this.currNumPagesArr = [];
    // Patient view pagination support TODO this needs refactoring
    this.getHitsFor = function (hits) {
        var ret = [];
        for (var i = 0; i < hits.length; i++) {
            if (this.model.docFilter.shouldDisplay(hits[i])) {
                ret.push(hits[i]);
            }
        }
        return ret;
    };
    this.getHits = function () {
        var ret = [];
        for (var i = 0; i < this.currPatientDocHits.length; i++) {
            if (this.model.docFilter.shouldDisplay(this.currPatientDocHits[i])) {
                ret.push(this.currPatientDocHits[i]);
            }
        }
        return ret;
    };
    /**
     * @param {PatientHit} hit
     */
    this.showPatientSpecificResults = function(hit) {
        this.currentPage = 0;
        this.currPatientDocHits = hit.docs;
        this.currPatientDocHits = this.getHits();
    };
    this.numberOfPages = function () {
        return Math.ceil(this.getHits().length / this.pageSize);
    };
    this.getNumPagesAsArr = function () { // Hack to allow for ng-repeat on set integer value
        var limit = this.numberOfPages();
        if (limit === this.currPageCount) {
            return this.currNumPagesArr;
        } else {
            var ret = [];
            for (var i = 0; i < limit; i++) {
                ret.push({});
            }
            this.currNumPagesArr = ret;
            this.currPageCount = limit;
            return ret;
        }
    };


    this.model = new Model(new Query('', []), [], []);
    this.structuredReferenceBar = false;
    var scope = this;
    this.mappingInit = function () {
        return $http.get('/_mappings').then(function (resp) {
            scope.mappings = new MappingDefinition(resp.data);
        });
    };
    this.mappingInit();

    this.submitQuery = function () {
        this.model.submitted = true;
        this.model.completed = false;
        this.model.query.refresh($http, false, this.model.query.submit, this.model, this.model.docFilter);
    };

    this.refresh = function (force) {
        this.model.query.refresh($http, force);
    };
});
// Used for pagination
app.filter('startFrom', function () {
    return function (input, start) {
        start = +start; //parse to int
        return input.slice(start);
    }
});
// TODO from stack overflow, understand this code and cleanup/modify
app.filter('range', function() {
    return function(val, limit, current) {
        var arr = [];
        if (current < 6) {

            for (var i = 0; i < 8; i++)
                arr.push(i);
            arr.push("...");
            arr.push(val - 1)
        } else if (current > (val - 5)) {
            arr.push(0);
            arr.push("...");
            for (var i = val - 7; i < val; i++)
                arr.push(i);
        } else {
            arr.push(0);
            arr.push("...");
            arr.push(current - 3);
            arr.push(current - 2);
            arr.push(current - 1);
            arr.push(current);
            arr.push(current + 1);
            arr.push("....");
            arr.push(val - 1)
        }
        return arr;
    }
}).filter('slice', function() {
    return function(arr, end, start) {
        start = start || 0;
        return (arr || []).slice(start, start + end);
    };
}).filter('isNum', function() {
    return function(val) {
        return !isNaN(val)
    };
});

