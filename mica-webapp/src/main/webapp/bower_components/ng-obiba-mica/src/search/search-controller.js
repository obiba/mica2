/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/* global CRITERIA_ITEM_EVENT */
/* global QUERY_TARGETS */
/* global QUERY_TYPES */
/* global RQL_NODE */

function targetToType(target) {
  switch (target.toLocaleString()) {
    case QUERY_TARGETS.NETWORK:
      return QUERY_TYPES.NETWORKS;
    case QUERY_TARGETS.STUDY:
      return QUERY_TYPES.STUDIES;
    case QUERY_TARGETS.DATASET:
      return QUERY_TYPES.DATASETS;
    case QUERY_TARGETS.VARIABLE:
      return QUERY_TYPES.VARIABLES;
  }

  throw new Error('Invalid target: ' + target);
}

/**
 * State shared between Criterion DropDown and its content directives
 *
 * @constructor
 */
function CriterionState() {
  var onOpenCallbacks = [];
  var onCloseCallbacks = [];

  this.dirty = false;
  this.open = false;

  this.addOnOpen = function(callback) {
    onOpenCallbacks.push(callback);
  };

  this.addOnClose = function(callback) {
    onCloseCallbacks.push(callback);
  };

  this.onOpen =function() {
    onOpenCallbacks.forEach(function(callback) {
      callback();
    });
  };

  this.onClose = function() {
    onCloseCallbacks.forEach(function(callback) {
      callback();
    });
  };
}

var DISPLAY_TYPES = {
  LIST: 'list',
  COVERAGE: 'coverage',
  GRAPHICS: 'graphics'
};

angular.module('obiba.mica.search')

  .controller('SearchController', [
    '$scope',
    '$timeout',
    '$routeParams',
    '$location',
    'TaxonomiesSearchResource',
    'TaxonomiesResource',
    'TaxonomyResource',
    'VocabularyResource',
    'ngObibaMicaSearchTemplateUrl',
    'JoinQuerySearchResource',
    'JoinQueryCoverageResource',
    'AlertService',
    'ServerErrorUtils',
    'LocalizedValues',
    'ObibaSearchConfig',
    'RqlQueryService',
    'RqlQueryUtils',
    function ($scope,
              $timeout,
              $routeParams,
              $location,
              TaxonomiesSearchResource,
              TaxonomiesResource,
              TaxonomyResource,
              VocabularyResource,
              ngObibaMicaSearchTemplateUrl,
              JoinQuerySearchResource,
              JoinQueryCoverageResource,
              AlertService,
              ServerErrorUtils,
              LocalizedValues,
              ObibaSearchConfig,
              RqlQueryService,
              RqlQueryUtils) {

      $scope.settingsDisplay = ObibaSearchConfig.getOptions();

      function onError(response) {
        AlertService.alert({
          id: 'SearchController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response),
          delay: 5000
        });
      }

      function validateType(type) {
        if (!type || !QUERY_TYPES[type.toUpperCase()]) {
          throw new Error('Invalid type: ' + type);
        }
      }

      function validateDisplay(display) {
        if (!display || !DISPLAY_TYPES[display.toUpperCase()]) {
          throw new Error('Invalid display: ' + display);
        }
      }

      function getDefaultQueryType() {
        if ($scope.settingsDisplay.variables.showSearchTab) {
          return QUERY_TYPES.VARIABLES;
        }
        else {
          var result = Object.keys($scope.settingsDisplay).filter(function (key) {
            return $scope.settingsDisplay[key].showSearchTab === 1;
          });
          console.log(result);
          return result[result.length - 1];
        }
      }

      function validateQueryData() {
        try {
          var search = $location.search();
          var type = search.type || getDefaultQueryType();
          var display = search.display || DISPLAY_TYPES.LIST;
          var query = search.query || '';
          validateType(type);
          validateDisplay(display);

          $scope.search.type = type;
          $scope.search.display = display;
          $scope.search.query = query;
          $scope.search.rqlQuery = new RqlParser().parse(query);
          return true;

        } catch (e) {
          AlertService.alert({
            id: 'SearchController',
            type: 'danger',
            msg: e.message,
            delay: 5000
          });
        }

        return false;
      }

      function executeSearchQuery() {
        if (validateQueryData()) {
          // build the criteria UI
          RqlQueryService.createCriteria($scope.search.rqlQuery, $scope.lang).then(function (rootItem) {
            // criteria UI is updated here
            $scope.search.criteria = rootItem;
          });

          var localizedRqlQuery = angular.copy($scope.search.rqlQuery);
          RqlQueryUtils.addLocaleQuery(localizedRqlQuery, $scope.lang);
          var localizedQuery = new RqlQuery().serializeArgs(localizedRqlQuery.args);

          $scope.search.loading = true;
          switch ($scope.search.display) {
            case DISPLAY_TYPES.LIST:
              JoinQuerySearchResource[$scope.search.type]({query: localizedQuery},
                function onSuccess(response) {
                  $scope.search.result.list = response;
                  $scope.search.loading = false;
                },
                onError);
              break;
            case DISPLAY_TYPES.COVERAGE:
              JoinQueryCoverageResource.get({query: RqlQueryService.prepareCoverageQuery(localizedQuery, ['studyIds'])},
                function onSuccess(response) {
                  $scope.search.result.coverage = response;
                  $scope.search.loading = false;
                },
                onError);
              break;
            case DISPLAY_TYPES.GRAPHICS:
              JoinQuerySearchResource.studies({
                  query: RqlQueryService.prepareGraphicsQuery(localizedQuery,
                    ['methods.designs', 'populations.selectionCriteria.countriesIso', 'populations.dataCollectionEvents.bioSamples', 'numberOfParticipants.participant.number'])
                },
                function onSuccess(response) {
                  $scope.search.result.graphics = response;
                  $scope.search.loading = false;
                },
                onError);
              break;
          }
        }
      }

      var closeTaxonomies = function () {
        angular.element('#taxonomies').collapse('hide');
      };

      var filterTaxonomies = function (query) {
        $scope.taxonomies.search.active = true;
        if (query && query.length === 1) {
          $scope.taxonomies.search.active = false;
          return;
        }
        // taxonomy filter
        if ($scope.taxonomies.taxonomy) {
          if ($scope.taxonomies.vocabulary) {
            VocabularyResource.get({
              target: $scope.taxonomies.target,
              taxonomy: $scope.taxonomies.taxonomy.name,
              vocabulary: $scope.taxonomies.vocabulary.name,
              query: query
            }, function onSuccess(response) {
              $scope.taxonomies.vocabulary.terms = response.terms;
              $scope.taxonomies.search.active = false;
            });
          } else {
            TaxonomyResource.get({
              target: $scope.taxonomies.target,
              taxonomy: $scope.taxonomies.taxonomy.name,
              query: query
            }, function onSuccess(response) {
              $scope.taxonomies.taxonomy.vocabularies = response.vocabularies;
              $scope.taxonomies.search.active = false;
            });
          }
        } else {
          TaxonomiesResource.get({
            target: $scope.taxonomies.target,
            query: query
          }, function onSuccess(response) {
            $scope.taxonomies.all = response;
            $scope.taxonomies.search.active = false;
          });
        }
      };

      var selectTaxonomyTarget = function (target) {
        if (!$scope.taxonomiesShown) {
          angular.element('#taxonomies').collapse('show');
        } else if ($scope.taxonomies.target === target) {
          closeTaxonomies();
        }
        if ($scope.taxonomies.target !== target) {
          $scope.taxonomies.target = target;
          $scope.taxonomies.taxonomy = null;
          $scope.taxonomies.vocabulary = null;
          filterTaxonomies($scope.taxonomies.search.text);
        }
      };

      var clearFilterTaxonomies = function () {
        $scope.taxonomies.search.text = null;
        $scope.taxonomies.search.active = false;
        filterTaxonomies(null);
      };

      var filterTaxonomiesKeyUp = function (event) {
        switch (event.keyCode) {
          case 27: // ESC
            if (!$scope.taxonomies.search.active) {
              clearFilterTaxonomies();
            }
            break;

          case 13: // Enter
            filterTaxonomies($scope.taxonomies.search.text);
            break;
        }
      };

      /**
       * Updates the URL location triggering a query execution
       */
      var refreshQuery = function() {
        var query = new RqlQuery().serializeArgs($scope.search.rqlQuery.args);
        var search = $location.search();
        // TODO bug when there other queries such as locale etc
        if ('' === query) {
          delete search.query;
        } else {
          search.query = query;
        }
        $location.search(search).replace();
      };

      var clearSearch = function () {
        $scope.documents.search.text = null;
        $scope.documents.search.active = false;
      };

      /**
       * Searches the criteria matching the input query
       *
       * @param query
       * @returns {*}
       */
      var searchCriteria = function (query) {
        // search for taxonomy terms
        // search for matching variables/studies/... count
        return TaxonomiesSearchResource.get({
          query: query
        }).$promise.then(function (response) {
          if (response) {
            var results = [];
            var total = 0;
            var size = 10;
            response.forEach(function (bundle) {
              var target = bundle.target;
              var taxonomy = bundle.taxonomy;
              if (taxonomy.vocabularies) {
                taxonomy.vocabularies.forEach(function (vocabulary) {
                  if (vocabulary.terms) {
                    vocabulary.terms.forEach(function (term) {
                      if (results.length < size) {
                        results.push(RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, term, $scope.lang));
                      }
                      total++;
                    });
                  } else {
                    if (results.length < size) {
                      results.push(RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, null, $scope.lang));
                    }
                    total++;
                  }
                });
              }
            });
            if (total > results.length) {
              var note = {
                query: query,
                total: total,
                size: size,
                message: 'Showing ' + size + ' / ' + total,
                status: 'has-warning'
              };
              results.push(note);
            }
            return results;
          } else {
            return [];
          }
        });
      };

      /**
       * Propagates a Scope change that results in criteria panel update
       * @param item
       */
      var selectCriteria = function (item) {
        if (item.id) {
          RqlQueryService.addCriteriaItem($scope.search.rqlQuery, item);
          refreshQuery();
          $scope.selectedCriteria = null;
        } else {
          $scope.selectedCriteria = item.query;
        }
      };

      var searchKeyUp = function (event) {
        switch (event.keyCode) {
          case 27: // ESC
            if ($scope.documents.search.active) {
              clearSearch();
            }
            break;

          default:
            if ($scope.documents.search.text) {
              searchCriteria($scope.documents.search.text);
            }
            break;
        }
      };

      var navigateTaxonomy = function (taxonomy, vocabulary) {
        var toFilter = ($scope.taxonomies.taxonomy && !taxonomy) || ($scope.taxonomies.vocabulary && !vocabulary);
        $scope.taxonomies.taxonomy = taxonomy;
        $scope.taxonomies.vocabulary = vocabulary;
        if (toFilter) {
          filterTaxonomies($scope.taxonomies.search.text);
        }
      };

      /**
       * Callback used in the views
       *
       * @param target
       * @param taxonomy
       * @param vocabulary
       * @param term
       */
      var selectTerm = function (target, taxonomy, vocabulary, term) {
        selectCriteria(RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, term, $scope.lang));
      };

      var onTypeChanged = function (type) {
        if (type) {
          validateType(type);
          var search = $location.search();
          search.type = type;
          $location.search(search).replace();
        }
      };

      var onDisplayChanged = function (display) {
        if (display) {
          validateDisplay(display);
          var search = $location.search();
          search.display = display;
          $location.search(search).replace();
        }
      };

      /**
       * Removes the item from the criteria tree
       * @param item
       */
      var removeCriteriaItem = function(item) {
        RqlQueryService.removeCriteriaItem(item);
        refreshQuery();
      };

      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.lang = LocalizedValues.getLocal();

      $scope.search = {
        query: null,
        rqlQuery: null,
        type: null,
        result: {
          list: null,
          coverage: null,
          graphics: null
        },
        criteria: [],
        loading: false
      };

      $scope.documents = {
        search: {
          text: null,
          active: false
        }
      };

      $scope.taxonomies = {
        all: TaxonomiesResource.get({target: 'variable'}),
        search: {
          text: null,
          active: false
        },
        target: 'variable',
        taxonomy: null,
        vocabulary: null
      };

      $scope.headerTemplateUrl = ngObibaMicaSearchTemplateUrl.getHeaderUrl('view');
      $scope.clearFilterTaxonomies = clearFilterTaxonomies;
      $scope.searchCriteria = searchCriteria;
      $scope.selectCriteria = selectCriteria;
      $scope.searchKeyUp = searchKeyUp;
      $scope.filterTaxonomiesKeyUp = filterTaxonomiesKeyUp;
      $scope.navigateTaxonomy = navigateTaxonomy;
      $scope.selectTaxonomyTarget = selectTaxonomyTarget;
      $scope.selectTerm = selectTerm;
      $scope.removeCriteriaItem = removeCriteriaItem;
      $scope.refreshQuery = refreshQuery;
      $scope.closeTaxonomies = closeTaxonomies;
      $scope.onTypeChanged = onTypeChanged;
      $scope.onDisplayChanged = onDisplayChanged;
      $scope.taxonomiesShown = false;

      angular.element('#taxonomies').on('show.bs.collapse', function () {
        $scope.taxonomiesShown = true;
      });
      angular.element('#taxonomies').on('hide.bs.collapse', function () {
        $scope.taxonomiesShown = false;
      });

      $scope.$watch('search', function () {
        executeSearchQuery();
      });

      $scope.$on('$locationChangeSuccess', function (newLocation, oldLocation) {
        if (newLocation !== oldLocation) {
          executeSearchQuery();
        }
      });

    }])

  .controller('SearchResultController', [
    '$scope',
    'ObibaSearchConfig',
    function ($scope,
              ObibaSearchConfig) {

      $scope.settingsDisplay = ObibaSearchConfig.getOptions();

      $scope.selectDisplay = function (display) {
        console.log('Display', display);
        $scope.display = display;
        $scope.$parent.onDisplayChanged(display);
      };
      $scope.selectTarget = function (type) {
        console.log('Target', type);
        $scope.type = type;
        $scope.$parent.onTypeChanged(type);
      };
      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.DISPLAY_TYPES = DISPLAY_TYPES;

      $scope.$watch('type', function () {
        $scope.activeTarget = {
          networks: ($scope.type === QUERY_TYPES.NETWORKS && $scope.settingsDisplay.networks.showSearchTab) || false,
          studies: ($scope.type === QUERY_TYPES.STUDIES && $scope.settingsDisplay.studies.showSearchTab) || false,
          datasets: ($scope.type === QUERY_TYPES.DATASETS && $scope.settingsDisplay.datasets.showSearchTab) || false,
          variables: ($scope.type === QUERY_TYPES.VARIABLES && $scope.settingsDisplay.variables.showSearchTab) || false
        };
      });

      $scope.$watch('display', function () {
        $scope.activeDisplay = {
          list: $scope.display === DISPLAY_TYPES.LIST || false,
          coverage: $scope.display === DISPLAY_TYPES.COVERAGE || false,
          graphics: $scope.display === DISPLAY_TYPES.GRAPHICS || false
        };
      });

    }])

  .controller('CriterionLogicalController', [
    '$scope',
    function ($scope) {
      $scope.updateLogical = function(operator) {
        $scope.item.rqlQuery.name = operator;
        $scope.$emit(CRITERIA_ITEM_EVENT.refresh);
      };
    }])

  .controller('CriterionDropdownController', ['$scope', 'StringUtils',
    function ($scope, StringUtils) {
      console.log('CriterionDropdownController -', $scope.criterion.vocabulary.name);

      var closeDropdown = function() {
        if (!$scope.state.open) {
          return;
        }

        $scope.state.onClose();

        var wasDirty = $scope.state.dirty;
        $scope.state.open = false;
        $scope.state.dirty = false;
        if (wasDirty) {
          // trigger a query update
          $scope.$emit(CRITERIA_ITEM_EVENT.refresh);
        }
      };

      var openDropdown = function () {
        if ($scope.state.open) {
          closeDropdown();
          return;
        }

        $scope.state.open = true;
        $scope.state.onOpen();
      };

      var remove = function() {
        $scope.$emit(CRITERIA_ITEM_EVENT.deleted, $scope.criterion);
      };

      $scope.state = new CriterionState();
      $scope.localize = function(values) {
        return StringUtils.localize(values, $scope.criterion.lang);
      };
      $scope.truncate = StringUtils.truncate;
      $scope.remove = remove;
      $scope.openDropdown = openDropdown;
      $scope.closeDropdown = closeDropdown;
    }])

  .controller('StringCriterionTermsController', [
    '$scope',
    'RqlQueryService',
    'StringUtils',
    'JoinQuerySearchResource',
    'RqlQueryUtils',
    function ($scope, RqlQueryService, StringUtils, JoinQuerySearchResource, RqlQueryUtils) {
      console.log('StringCriterionTermsController');

      var isSelected = function (name) {
        return $scope.checkboxTerms.indexOf(name) !== -1;
      };

      var updateSelection = function() {
        $scope.state.dirty = true;
        var selected = [];
        Object.keys($scope.checkboxTerms).forEach(function(key) {
          if ($scope.checkboxTerms[key]) {
            selected.push(key);
          }
        });
        RqlQueryUtils.updateQuery($scope.criterion.rqlQuery, selected);
      };

      var updateFilter = function() {
        RqlQueryUtils.updateQuery($scope.criterion.rqlQuery, [], RQL_NODE.MISSING === $scope.selectedFilter);
        $scope.state.dirty = true;
      };

      var isInFilter = function() {
        return $scope.selectedFilter === RQL_NODE.IN;
      };

      var onOpen = function() {
        var target = $scope.criterion.target;
        var joinQuery = RqlQueryService.prepareCriteriaTermsQuery($scope.query, $scope.criterion);
        JoinQuerySearchResource[targetToType(target)]({query: joinQuery}).$promise.then(function (joinQueryResponse) {
          $scope.state.open = true;
          $scope.terms = RqlQueryService.getTargetAggregegations(joinQueryResponse, $scope.criterion);
          $scope.terms.forEach(function(term) {
            $scope.checkboxTerms[term.key] =
              $scope.criterion.selectedTerms && $scope.criterion.selectedTerms.indexOf(term.key) !== -1;
          });
        });
      };

      $scope.state.addOnOpen(onOpen);
      $scope.checkboxTerms = {};
      $scope.RQL_NODE = RQL_NODE;
      $scope.selectedFilter = $scope.criterion.type;
      $scope.isSelected = isSelected;
      $scope.updateFilter = updateFilter;
      $scope.localize = function(values) {
        return StringUtils.localize(values, $scope.criterion.lang);
      };
      $scope.truncate = StringUtils.truncate;
      $scope.isInFilter = isInFilter;
      $scope.updateSelection = updateSelection;
    }])

  .controller('CoverageResultTableController', [
    '$scope',
    function ($scope) {

      function processCoverageResponse() {
        var response = $scope.result;
        var taxonomyHeaders = [];
        var vocabularyHeaders = [];
        var termHeaders = [];
        var rows = {};
        var footers = {
          total: []
        };
        if (response.taxonomies) {
          var termsCount = 0;
          response.taxonomies.forEach(function (taxo) {
            var taxonomyTermsCount = 0;
            if (taxo.vocabularies) {
              taxo.vocabularies.forEach(function (voc) {
                if (voc.terms) {
                  voc.terms.forEach(function (trm) {
                    termsCount++;
                    taxonomyTermsCount++;
                    termHeaders.push({
                      taxonomy: taxo.taxonomy,
                      vocabulary: voc.vocabulary,
                      term: trm.term
                    });
                    footers.total.push(trm.hits);
                    if (trm.buckets) {
                      trm.buckets.forEach(function (bucket) {
                        if (!(bucket.field in rows)) {
                          rows[bucket.field] = {};
                        }
                        if (!(bucket.value in rows[bucket.field])) {
                          rows[bucket.field][bucket.value] = {
                            field: bucket.field,
                            title: bucket.title,
                            description: bucket.description,
                            hits: {}
                          };
                        }
                        // store the hits per field, per value at the position of the term
                        rows[bucket.field][bucket.value].hits[termsCount] = bucket.hits;
                      });
                    }
                  });
                  vocabularyHeaders.push({
                    taxonomy: taxo.taxonomy,
                    vocabulary: voc.vocabulary,
                    termsCount: voc.terms.length
                  });
                }
              });
              taxonomyHeaders.push({
                taxonomy: taxo.taxonomy,
                termsCount: taxonomyTermsCount
              });
            }
          });
        }

        // compute totalHits for each row
        Object.keys(rows).forEach(function (field) {
          Object.keys(rows[field]).forEach(function (value) {
            var hits = rows[field][value].hits;
            rows[field][value].totalHits = Object.keys(hits).map(function (idx) {
              return hits[idx];
            }).reduce(function (a, b) {
              return a + b;
            });
          });
        });

        $scope.table = {
          taxonomyHeaders: taxonomyHeaders,
          vocabularyHeaders: vocabularyHeaders,
          termHeaders: termHeaders,
          rows: rows,
          footers: footers,
          totalHits: response.totalHits,
          totalCount: response.totalCount
        };
      }

      $scope.$watch('result', function () {
        if ($scope.result) {
          processCoverageResponse();
        } else {
          $scope.table = null;
        }
      });

      $scope.showMissing = true;
      $scope.toggleMissing = function (value) {
        $scope.showMissing = value;
      };
      $scope.keys = Object.keys;

    }])

  .controller('GraphicsResultController', [
    'GraphicChartsConfig',
    'GraphicChartsUtils',
    '$filter',
    '$scope',
    function (GraphicChartsConfig,
              GraphicChartsUtils,
              $filter,
              $scope) {

      var setChartObject = function (tem, dtoObject, header, title, options) {
        var ChartObject = GraphicChartsUtils.getArrayByAggregation(tem, dtoObject);
        if (ChartObject.length > 0) {
          ChartObject.unshift(header);
          angular.extend(options, {title: title});
          return {
            data: ChartObject,
            options: options
          };
        }
        return false;
      };

      $scope.$watch('result', function (result) {
        $scope.chartObjects={};
        if (result) {
          var geoStudies = setChartObject('populations-selectionCriteria-countriesIso',
            result.studyResultDto,
            [$filter('translate')('graphics.country'), $filter('translate')('graphics.nbr-studies')],
            $filter('translate')('graphics.geo-chart-title') + ' (N = ' + result.studyResultDto.totalHits + ')',
            GraphicChartsConfig.getOptions().ChartsOptions.geoChartOptions.options);


          var methodDesignStudies = setChartObject('methods-designs',
            result.studyResultDto,
            [$filter('translate')('graphics.study-design'), $filter('translate')('graphics.nbr-studies')],
            $filter('translate')('graphics.study-design-chart-title') + ' (N = ' + result.studyResultDto.totalHits + ')',
            GraphicChartsConfig.getOptions().ChartsOptions.studiesDesigns.options);


          var bioSamplesStudies = setChartObject('populations-dataCollectionEvents-bioSamples',
            result.studyResultDto,
            [$filter('translate')('graphics.bio-samples'), $filter('translate')('graphics.nbr-studies')],
            $filter('translate')('graphics.bio-samples-chart-title') + ' (N = ' + result.studyResultDto.totalHits + ')',
            GraphicChartsConfig.getOptions().ChartsOptions.biologicalSamples.options);

          console.log('=========}}}}}}}}geo', geoStudies);

          if (geoStudies) {
            angular.extend($scope.chartObjects,
              {
                geoChartOptions: {
                  chartObject: {
                    geoTitle: geoStudies.options.title,
                    options: geoStudies.options,
                    type: 'GeoChart',
                    data: geoStudies.data
                  }
                }
              });
          }
          if (methodDesignStudies) {
            angular.extend($scope.chartObjects,{
              studiesDesigns: {
                chartObject: {
                  options: methodDesignStudies.options,
                  type: 'BarChart',
                  data: methodDesignStudies.data
                }
              }
            });
          }
          if (bioSamplesStudies) {
            angular.extend($scope.chartObjects,{
              biologicalSamples: {
                chartObject: {
                  options: bioSamplesStudies.options,
                  type: 'PieChart',
                  data: bioSamplesStudies.data
                }
              }
            });
          }

          console.log($scope.chartObjects);
        }
      });

    }]);
