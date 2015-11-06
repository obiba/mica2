/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.search

  .controller('SearchController', ['$scope', '$timeout', 'TaxonomiesResource', 'TaxonomyResource', 'VocabularyResource',
    function ($scope, $timeout, TaxonomiesResource, TaxonomyResource, VocabularyResource) {

      $scope.lang = 'en';

      $scope.documents = {
        search: {
          text: null,
          active: false
        }
      };
      $scope.taxonomies = {
        all: TaxonomiesResource.get(),
        search: {
          text: null,
          active: false
        },
        taxonomy: null,
        vocabulary: null
      };

      var filterTaxonomiesKeyUp = function(event) {
        switch(event.keyCode) {
          case 27: // ESC
            if ($scope.taxonomies.search.active) {
              clearFilterTaxonomies();
            }
            break;

          default:
            filterTaxonomies($scope.taxonomies.search.text);
            break;
        }
      };

      var clearFilterTaxonomies = function() {
        $scope.taxonomies.search.text = null;
        $scope.taxonomies.search.active = false;
      };

      var filterTaxonomies = function(query) {
        $scope.taxonomies.search.active = true;
        // taxonomy filter
        if ($scope.taxonomies.taxonomy) {
          if ($scope.taxonomies.vocabulary) {
            $scope.vocabulary = VocabularyResource.get({ query: query });
          } else {
            $scope.taxonomy = TaxonomyResource.get({ query: query });
          }
        } else {
          $scope.taxonomies.all = TaxonomiesResource.get({ query: query });
        }
      };

      var searchKeyUp = function(event) {
        switch(event.keyCode) {
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

      var clearSearch = function() {
        $scope.documents.search.text = null;
        $scope.documents.search.active = false;
      };

      var searchDocuments = function(query) {
        $scope.documents.search.active = true;
        // search for taxonomy terms
        // search for matching variables/studies/... count
      };

      var navigateTaxonomy = function(taxonomy, vocabulary) {
        $scope.taxonomies.taxonomy = taxonomy;
        $scope.taxonomies.vocabulary = vocabulary;
      };

      var selectTerm = function(taxonomy, vocabulary, term) {

      };

      $scope.searchKeyUp = searchKeyUp;
      $scope.navigateTaxonomy = navigateTaxonomy;
      $scope.selectTerm = selectTerm;
    }]);
