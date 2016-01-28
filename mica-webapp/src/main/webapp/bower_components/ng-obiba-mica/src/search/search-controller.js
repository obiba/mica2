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
    'TaxonomiesResource',
    'TaxonomyResource',
    'VocabularyResource',
    'ngObibaMicaSearchTemplateUrl',
    'JoinQuerySearchResource',
    'QUERY_TYPES',
    function ($scope,
              $timeout,
              $routeParams,
              $location,
              TaxonomiesResource,
              TaxonomyResource,
              VocabularyResource,
              ngObibaMicaSearchTemplateUrl,
              JoinQuerySearchResource,
              QUERY_TYPES) {



      var closeTaxonomies = function () {
        $('#taxonomies').collapse('hide');
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
          $('#taxonomies').collapse('show');
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

      var searchDocuments = function (/*query*/) {
        $scope.documents.search.active = true;
        // search for taxonomy terms
        // search for matching variables/studies/... count
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
              searchDocuments($scope.documents.search.text);
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

      var selectTerm = function (/*taxonomy, vocabulary, term*/) {

      };

      function executeQuery() {
        JoinQuerySearchResource[$scope.search.type]({query: $scope.search.query}, function onSuccess(response) {
          $scope.search.result = response;
          console.log('>>> Response', $scope.search.result);
        });
      }

      var onTypeChanged = function(type) {
        if (type && QUERY_TYPES[type.toUpperCase()]) {
          var search = $location.search();
          search.type = type;
          $location.search(search).replace();
          executeQuery();
        }
      };

      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.lang = 'en';
      $scope.search = {
        query: $routeParams.query || '',
        type: $routeParams.type || QUERY_TYPES.VARIABLES,
        result: null
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
      $scope.searchKeyUp = searchKeyUp;
      $scope.filterTaxonomiesKeyUp = filterTaxonomiesKeyUp;
      $scope.navigateTaxonomy = navigateTaxonomy;
      $scope.selectTaxonomyTarget = selectTaxonomyTarget;
      $scope.selectTerm = selectTerm;
      $scope.closeTaxonomies = closeTaxonomies;
      $scope.onTypeChanged = onTypeChanged;
      $scope.taxonomiesShown = true;

      //
      //// TODO replace with angular code
      //$('#taxonomies').on('show.bs.collapse', function () {
      //  $scope.taxonomiesShown = true;
      //});
      //$('#taxonomies').on('hide.bs.collapse', function () {
      //  $scope.taxonomiesShown = false;
      //});

      $scope.$watch('search', function () {
        //if ($scope.search.query) {
          executeQuery();
        //}
      });

    }])

  .controller('SearchResultController', [
    '$scope',
    'QUERY_TYPES',
    function ($scope, QUERY_TYPES) {
      var selectTab = function(type) {
        console.log('Type', type);
        $scope.type = type;
        $scope.$parent.onTypeChanged(type);
      };

      $scope.activeTab = {
        networks: $scope.type === QUERY_TYPES.NETWORKS || false,
        studies: $scope.type === QUERY_TYPES.STUDIES || false,
        datasets: $scope.type === QUERY_TYPES.DATASETS || false,
        variables: $scope.type === QUERY_TYPES.VARIABLES || false
      };

      $scope.selectTab = selectTab;
      $scope.QUERY_TYPES = QUERY_TYPES;

    }]);
