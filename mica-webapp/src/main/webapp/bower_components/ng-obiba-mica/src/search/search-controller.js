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


var QUERY_TYPES = {
  NETWORKS: 'networks',
  STUDIES: 'studies',
  DATASETS: 'datasets',
  VARIABLES: 'variables'
};

var QUERY_TARGETS = {
  NETWORKS: 'networks',
  STUDIES: 'studies',
  DATASETS: 'datasets',
  VARIABLES: 'variables'
};

function targetToType(target) {
  switch (target.toLocaleString()) {
    case 'network':
      return 'networks';
    case 'study':
      return 'studies';
    case 'dataset':
        return 'datasets';
    case'variable':
      return 'variables';
  }

  throw new Error('Invalid target: ' + target);
}

angular.module('obiba.mica.search')

  .constant('QUERY_TYPES', QUERY_TYPES)

  .constant('QUERY_TARGETS', QUERY_TARGETS)

  .constant('DISPLAY_TYPES', {
    LIST: 'list',
    COVERAGE: 'coverage',
    GRAPHICS: 'graphics'
  })

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
    'QUERY_TYPES',
    'DISPLAY_TYPES',
    'AlertService',
    'ServerErrorUtils',
    'LocalizedValues',
    'ObibaSearchConfig',
    'RqlQueryService',
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
              QUERY_TYPES,
              DISPLAY_TYPES,
              AlertService,
              ServerErrorUtils,
              LocalizedValues,
              ObibaSearchConfig,
              RqlQueryService) {

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

      function getDefaultQuery(type) {
        var query = ':q(match())';

        switch (type) {
          case QUERY_TYPES.NETWORKS:
            return query.replace(/:q/, 'network');
          case QUERY_TYPES.STUDIES:
            return query.replace(/:q/, 'study');
          case QUERY_TYPES.DATASETS:
            return query.replace(/:q/, 'dataset');
          case QUERY_TYPES.VARIABLES:
            return query.replace(/:q/, 'variable');
        }

        throw new Error('Invalid query type: ' + type);
      }

      function getDefaultQueryType() {
        if ($scope.settingsDisplay.variables.showSearchTab) {
          return QUERY_TYPES.VARIABLES;
        }
        else {
          var result =  Object.keys($scope.settingsDisplay).filter(function (key) {
            return $scope.settingsDisplay[key].showSearchTab===1;
          });
          console.log(result);
          return result[result.length-1];
        }
      }


      function validateQueryData() {
        try {
          var search = $location.search();
          var type = search.type || getDefaultQueryType();
          var display = search.display || DISPLAY_TYPES.LIST;
          var query = search.query || getDefaultQuery(type);
          validateType(type);
          validateDisplay(display);
          new RqlParser().parse(query);
          var rqlQuery = new RqlParser().parse(query);
          // TODO implement RqlQueryService.buildCriteria to take care of all types
          RqlQueryService.buildVariableCriteria(rqlQuery, $scope.lang).then(function (criteriaList) {
            criteriaList.forEach(function (criterion) {
              selectCriteria(criterion);
            });
          });

          $scope.search.type = type;
          $scope.search.display = display;
          $scope.search.query = query;
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
          $scope.search.result = null;
          switch ($scope.search.display) {
            case DISPLAY_TYPES.LIST:
              JoinQuerySearchResource[$scope.search.type]({query: $scope.search.query},
                function onSuccess(response) {
                  $scope.search.result = response;
                },
                onError);
              break;
            case DISPLAY_TYPES.COVERAGE:
              JoinQueryCoverageResource.get({query: RqlQueryService.prepareCoverageQuery($scope.search.query, ['studyIds'])},
                function onSuccess(response) {
                  $scope.search.result = response;
                },
                onError);
              break;
            case DISPLAY_TYPES.GRAPHICS:
              // TODO
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

      var clearSearch = function () {
        $scope.documents.search.text = null;
        $scope.documents.search.active = false;
      };

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
                        results.push(RqlQueryService.createCriteria(target, taxonomy, vocabulary, term, $scope.lang));
                      }
                      total++;
                    });
                  } else {
                    if (results.length < size) {
                      results.push(RqlQueryService.createCriteria(target, taxonomy, vocabulary, null, $scope.lang));
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

      var selectCriteria = function (item) {
        console.log('selectCriteria', item);
        if (item.id) {
          var found = $scope.search.criteria.filter(function (criterion) {
            return item.vocabulary.name === criterion.vocabulary.name;
          });
          console.log('Found', found);
          if (found && found.length === 0) {
            $scope.search.criteria.push(item);
          }
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

      var selectTerm = function (target, taxonomy, vocabulary, term) {
        selectCriteria(RqlQueryService.createCriteria(target, taxonomy, vocabulary, term, $scope.lang));
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

      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.lang = 'en';

      $scope.search = {
        query: null,
        type: null,
        result: null,
        criteria: []
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
      $scope.closeTaxonomies = closeTaxonomies;
      $scope.onTypeChanged = onTypeChanged;
      $scope.onDisplayChanged = onDisplayChanged;
      $scope.taxonomiesShown = false;

      //// TODO replace with angular code
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
    'QUERY_TYPES',
    'DISPLAY_TYPES',
    'ObibaSearchConfig',
    function ($scope,
              QUERY_TYPES,
              DISPLAY_TYPES,
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

  .controller('CriterionDropdownController', [
    '$scope',
    'RqlQueryService',
    'LocalizedValues',
    'JoinQuerySearchResource',
    function ($scope, RqlQueryService, LocalizedValues, JoinQuerySearchResource) {
      console.log('QueryDropdownController', $scope);

      var isSelected = function (name) {
        return $scope.selectedTerms.indexOf(name) !== -1;
      };

      var selectAll = function () {
        $scope.selectedTerms = $scope.criterion.vocabulary.terms.map(function (term) {
          return term.name;
        });
      };

      var toggleSelection = function (term) {
        if (!isSelected(term.name)) {
          $scope.selectedTerms.push(term.name);
          return;
        }

        $scope.selectedTerms = $scope.selectedTerms.filter(function (name) {
          return name !== term.name;
        });
      };

      var localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };

      var truncate = function (text) {
        return text.length > 40 ? text.substring(0, 40) + '...' : text;
      };

      var openDropdown = function() {
        if ($scope.open) {
          $scope.open = false;
          return;
        }

        var target = $scope.criterion.target;
        var joinQuery =
          RqlQueryService.prepareCriteriaTermsQuery(
            target,
            $scope.query,
            $scope.criterion.taxonomy.name,
            $scope.criterion.vocabulary.name);


        JoinQuerySearchResource[targetToType(target)]({query: joinQuery}).$promise.then(function (response) {
          console.log('GOT THE RESULT', response);
          $scope.open = true;
        });
      };

      $scope.selectedTerms = $scope.criterion.selectedTerms.map(function(term){
        return term.name;
      });
      $scope.isOpen = false;
      $scope.openDropdown = openDropdown;
      $scope.selectAll = selectAll;
      $scope.deselectAll = function () { $scope.selectedTerms = []; };
      $scope.toggleSelection = toggleSelection;
      $scope.isSelected = isSelected;
      $scope.localize = localize;
      $scope.truncate = truncate;

    }])

  .controller('CriteriaPanelController', [
    '$scope',
    function ($scope) {

      $scope.removeCriteria = function (id) {
        $scope.criteria = $scope.criteria.filter(function (criterion) {
          return id !== criterion.id;
        });
      };

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
            if (taxo.vocabularies) {
              taxo.vocabularies.forEach(function (voc) {
                if (voc.terms) {
                  voc.terms.forEach(function (trm) {
                    termsCount++;
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
                termsCount: termsCount
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

        return {
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
          $scope.table = processCoverageResponse();
        }
      });

    }]);
