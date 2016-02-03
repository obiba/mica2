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

angular.module('obiba.mica.search')

  .constant('QUERY_TYPES', {
    NETWORKS: 'networks',
    STUDIES: 'studies',
    DATASETS: 'datasets',
    VARIABLES: 'variables'
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
    'QUERY_TYPES',
    'AlertService',
    'ServerErrorUtils',
    'LocalizedValues',
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
              QUERY_TYPES,
              AlertService,
              ServerErrorUtils,
              LocalizedValues) {

      function createCriteria(target, taxonomy, vocabulary, term) {
        var id = taxonomy.name + '::' + vocabulary.name;
        if (term) {
          id = id + ':' + term.name;
        }
        var criteria = {
          id: id,
          taxonomy: taxonomy,
          vocabulary: vocabulary,
          term: term,
          target: target,
          lang: $scope.lang,
          itemTitle: '',
          itemDescription: '',
          itemParentTitle: '',
          itemParentDescription: ''
        };

        // prepare some labels for display
        if(term) {
          criteria.itemTitle = LocalizedValues.forLocale(term.title, $scope.lang);
          criteria.itemDescription = LocalizedValues.forLocale(term.description,$scope.lang);
          criteria.itemParentTitle = LocalizedValues.forLocale(vocabulary.title, $scope.lang);
          criteria.itemParentDescription = LocalizedValues.forLocale(vocabulary.description, $scope.lang);
          if (!criteria.itemTitle) {
            criteria.itemTitle = term.name;
          }
          if (!criteria.itemParentTitle) {
            criteria.itemParentTitle = vocabulary.name;
          }
        } else {
          criteria.itemTitle = LocalizedValues.forLocale(vocabulary.title, $scope.lang);
          criteria.itemDescription = LocalizedValues.forLocale(vocabulary.description, $scope.lang);
          criteria.itemParentTitle = LocalizedValues.forLocale(taxonomy.title, $scope.lang);
          criteria.itemParentDescription = LocalizedValues.forLocale(taxonomy.description, $scope.lang);
          if (!criteria.itemTitle) {
            criteria.itemTitle = vocabulary.name;
          }
          if (!criteria.itemParentTitle) {
            criteria.itemParentTitle = taxonomy.name;
          }
        }

        return criteria;
      }

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

      function validateQueryData() {
        try {
          var search = $location.search();
          var type = search.type || QUERY_TYPES.VARIABLES;
          var query = search.query || getDefaultQuery(type);
          validateType(type);
          new RqlParser().parse(query);

          $scope.search.type = type;
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

      function executeQuery() {
        if (validateQueryData()) {
          JoinQuerySearchResource[$scope.search.type]({query: $scope.search.query},
            function onSuccess(response) {
              $scope.search.result = response;
              console.log('>>> Response', $scope.search.result);
            },
            onError);
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
                        results.push(createCriteria(target, taxonomy, vocabulary, term));
                      }
                      total++;
                    });
                  } else {
                    if (results.length < size) {
                      results.push(createCriteria(target, taxonomy, vocabulary));
                    }
                    total++;
                  }
                });
              }
            });
            if(total > results.length) {
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
        if(item.id){
          var found = $scope.search.criteria.filter(function(criterion) {
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
        selectCriteria(createCriteria(target, taxonomy, vocabulary, term));
      };

      var onTypeChanged = function (type) {
        if (type) {
          validateType(type);
          var search = $location.search();
          search.type = type;
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
      $scope.taxonomiesShown = false;

      //// TODO replace with angular code
      angular.element('#taxonomies').on('show.bs.collapse', function () {
        $scope.taxonomiesShown = true;
      });
      angular.element('#taxonomies').on('hide.bs.collapse', function () {
        $scope.taxonomiesShown = false;
      });

      $scope.$watch('search', function () {
        executeQuery();
      });

      $scope.$on('$locationChangeSuccess', function (newLocation, oldLocation) {
        if (newLocation !== oldLocation) {
          executeQuery();
        }
      });

    }])

  .controller('SearchResultController', [
    '$scope',
    'QUERY_TYPES',
    function ($scope, QUERY_TYPES) {
      var selectTab = function (type) {
        console.log('Type', type);
        $scope.type = type;
        $scope.$parent.onTypeChanged(type);
      };

      $scope.selectTab = selectTab;
      $scope.QUERY_TYPES = QUERY_TYPES;

      $scope.$watch('type', function () {
        $scope.activeTab = {
          networks: $scope.type === QUERY_TYPES.NETWORKS || false,
          studies: $scope.type === QUERY_TYPES.STUDIES || false,
          datasets: $scope.type === QUERY_TYPES.DATASETS || false,
          variables: $scope.type === QUERY_TYPES.VARIABLES || false
        };
      });

    }])

  .controller('CriterionDropdownController', [
    '$scope',
    'LocalizedValues',
    function ($scope, LocalizedValues) {
      console.log('QueryDropdownController', $scope);

      var isSelected = function(name) {
        return $scope.selectedTerms.indexOf(name) !== -1;
      };

      var selectAll = function () {
        $scope.selectedTerms = $scope.criterion.vocabulary.terms.map(function (term) {
          return term.name;
        });
      };

      var toggleSelection = function(term) {
        if (!isSelected(term.name)) {
          $scope.selectedTerms.push(term.name);
          return;
        }

        $scope.selectedTerms = $scope.selectedTerms.filter(function(name) {
          return name !== term.name;
        });
      };

      var localize = function(values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };

      var truncate = function(text) {
        return text.length > 40 ? text.substring(0, 40) + '...' : text;
      };

      $scope.selectedTerms = [];
      $scope.selectAll = selectAll;
      $scope.deselectAll = function() { $scope.selectedTerms = []; };
      $scope.toggleSelection = toggleSelection;
      $scope.isSelected = isSelected;
      $scope.localize = localize;
      $scope.truncate = truncate;

    }])

  .controller('CriteriaPanelController', [
    '$scope',
    function ($scope) {
      console.log('QueryPanelController', $scope);

      $scope.removeCriteria = function(id) {
        $scope.criteria = $scope.criteria.filter(function(criterion) {
          return id !== criterion.id;
        });
      };

    }]);
