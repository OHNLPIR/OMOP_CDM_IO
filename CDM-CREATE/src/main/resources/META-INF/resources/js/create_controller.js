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

    this.submit = function ($http, model, filter) {
        this.submit($http, model, filter, null);
    };
    /**
     *
     * @param $http
     * @param {Model} model
     * @param {Filter} filter
     * @param  callback
     */
    this.submit = function ($http, model, filter, callback) {
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
                model.patientJudgements = {};
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
                if (callback) {
                    callback();
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
    this.loggedIn = null;
    this.isJudging = false;
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

var app = angular.module("CREATEApp", []);
app.controller("CREATECtrl", function ($scope, $http) {
    // Global Constants
    this.CLAUSE_TYPES = ["Must", "Should", "Should Not", "Must Not"];
    this.VIEW_TYPES = ["Patient", "Document"];
    this.TASK_TYPES = ["Cohort Discovery", "Document Search", "Continue Previous Saved Task"];
    // Model and functions
    this.currView = "Patient";
    this.displayFilters = true;
    this.saves = {};
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
    this.showPatientSpecificResults = function (hit) {
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

    this.currFile = null;
    // Handle file selection
    var currController = this;
    $scope.$on("fileSelected", function (event, args) {
        $scope.$apply(function () {
            //add the file object to the scope's files collection
            currController.currFile = args.file;
        });
    });

    this.loadUploaded = function () {
        if (this.currFile === null) {
            return;
        }
        var fr = new FileReader();
        var currController = this;
        fr.onload = function (e) {
            // Deep copy so that we don't overwrite function methods
            var contents = e.target.result;
            var obj = JSON.parse(contents);

            // - Copy Query
            currController.model.query.unstructured = obj.model.query.unstructured;
            currController.model.query.structured = obj.model.query.structured;
            currController.model.query.cdmQuery = obj.model.query.cdmQuery;
            currController.model.query.lastRefresh = obj.model.query.lastRefresh;
            // - Copy Hits
            currController.model.hits = obj.model.hits;
            currController.model.patientHits = obj.model.patientHits;
            // - Copy Filters
            currController.model.docFilter.patients = obj.model.docFilter.patients;
            currController.model.docFilter.patientOptions = obj.model.docFilter.patientOptions;
            currController.model.docFilter.sections = obj.model.docFilter.sections;
            currController.model.docFilter.sectionOptions = obj.model.docFilter.sectionOptions;
            // - Copy Judgements
            currController.model.docJudgements = obj.model.docJudgements;
            currController.model.patientJudgements = obj.model.patientJudgements;
            // - Restore View State
            currController.currView = obj.currView;
            currController.model.completed = obj.model.completed;
            currController.model.submitted = obj.model.submitted;
            currController.model.currentPage = obj.model.currentPage;
            currController.model.pageSize = obj.model.pageSize;
            currController.model.currPageCount = obj.model.currPageCount;
            currController.model.currNumPagesArr = obj.model.currNumPagesArr;
            // - TODO reset pagination for patient view modals
            // Apply to angular (so that it redraws)
            $scope.$apply();
        };
        fr.readAsText(this.currFile);
    };

    this.getUser = function() {
        var scope = this;
        $http.post("/_user").then(function (resp) {
            if (resp.data && resp.data.length > 0) {
                scope.model.loggedIn = resp.data[0];
            }
        })
    };

    this.getUser();

    this.save = function() {
        var saveName = prompt("Please input a name for this query", "Query Name");
        var unstructured = this.model.query.unstructured;
        var structured = this.model.query.structured;
        var cdm = this.model.query.cdmQuery;
        var docjudgements = this.model.docJudgements;
        var patientJudgements = this.model.patientJudgements;
        var request = {
            "username": this.model.loggedIn,
            "queryName": saveName,
            "unstructured": unstructured,
            "structured": structured,
            "cdm": cdm,
            "docJudgements": docjudgements,
            "patientJudgements": patientJudgements
        };
        $http.post("/_save", request).then(function (resp) {});
    };

    this.loadSaveList = function() {
        var currscope = this;
        $http.post("/_savelist").then(function (resp) {
            currscope.saves = resp.data;
        });

    };

    this.loadSave = function (name) {
        var object = {
            "username": this.model.loggedIn,
            "queryName": name
        };
        var scope = this;
        this.model.submitted = true;
        this.model.completed = false;
        $http.post("/_load", object).then(function (resp) {
            if (resp.data != null) {
                scope.model.query.unstructured = resp.data.unstructured;
                scope.model.query.structured = resp.data.structured;
                scope.model.query.cdmQuery = resp.data.cdm;
                var docJudgements = {};
                var patientJudgements = {};
                var judgements = resp.data.hits;
                var len = judgements.length;
                for (var i = 0; i < len; i++) {
                    var judgement = judgements[i];
                    var isDocType = judgement.docJudgement;
                    if (isDocType) {
                        docJudgements[judgement.document] = judgement.relevance;
                    } else {
                        patientJudgements[judgement.document] = judgement.relevance;
                    }
                }
                var upd_callback = function () {
                    scope.model.docJudgements = docJudgements;
                    scope.model.patientJudgements = patientJudgements;
                };
                scope.model.query.submit($http, scope.model, scope.model.docFilter, upd_callback);

            }
        });
    };

    this.deleteSave = function (name) {
        var currscope = this;
        $http.post("/_delete", name).then(function (resp) {
            currscope.saves = resp.data;
        });
    };

    // checks if we need to load text
    this.checkLoaded = function (doc) {
        if (!doc.text) {
            this.loadText(doc.indexDocID, doc)
        }
    };
    // get document text
    this.loadText = function (docID, doc) {
        $http.post('/_text', docID).then(function (resp) {
                doc.text = resp.data.text;
            }
        );
    }
});
// Used for pagination
app.filter('startFrom', function () {
    return function (input, start) {
        start = +start; //parse to int
        return input.slice(start);
    }
});

// TODO from stack overflow, understand this code and cleanup/modify
app.filter('range', function () {
    return function (val, limit, current) {
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
}).filter('slice', function () {
    return function (arr, end, start) {
        start = start || 0;
        return (arr || []).slice(start, start + end);
    };
}).filter('isNum', function () {
    return function (val) {
        return !isNaN(val)
    };
});
app.directive('fileUpload', function () {
    return {
        scope: true,        //create a new scope
        link: function (scope, el, attrs) {
            el.bind('change', function (event) {
                var files = event.target.files;
                //iterate files since 'multiple' may be specified on the element
                for (var i = 0; i < files.length; i++) {
                    //emit event upward
                    scope.$emit("fileSelected", {file: files[i]});
                }
            });
        }
    };
});


