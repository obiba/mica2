/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* global QUERY_TARGETS */

'use strict';

/* exported DISPLAY_TYPES */
var DISPLAY_TYPES = {
  LIST: 'list',
  COVERAGE: 'coverage',
  GRAPHICS: 'graphics'
};

/*global NgObibaMicaTemplateUrlFactory */
angular.module('obiba.mica.search', [
    'obiba.alert',
    'ui.bootstrap',
    'pascalprecht.translate',
    'templates-ngObibaMica'
  ])
  .config(['$provide', function ($provide) {
    $provide.provider('ngObibaMicaSearchTemplateUrl', new NgObibaMicaTemplateUrlFactory().create(
      {
        search: {header: null, footer: null},
        classifications: {header: null, footer: null}
      }
    ));
  }])
  .config(['$provide', '$injector', function ($provide) {
    $provide.provider('ngObibaMicaSearch', function () {
      var localeResolver = ['LocalizedValues', function (LocalizedValues) {
        return LocalizedValues.getLocal();
      }], options = {
        taxonomyTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
        searchTabsOrder: [DISPLAY_TYPES.LIST, DISPLAY_TYPES.COVERAGE, DISPLAY_TYPES.GRAPHICS],
        resultTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
        listLabel: 'search.list',
        coverageLabel: 'search.coverage',
        graphicsLabel: 'search.graphics',
        variables: {
          showSearchTab: true,
          searchLabel: 'search.variable.searchLabel',
          noResultsLabel: 'search.variable.noResults',
          variablesColumn: {
            showVariablesStudiesColumn: true,
            showVariablesDatasetsColumn: true,
            showDatasetsStudiesColumn: true,
            showDatasetsVariablesColumn: true
          }
        },
        datasets: {
          showSearchTab: true,
          showDatasetsSearchFilter: true,
          searchLabel: 'search.variable.searchLabel',
          noResultsLabel: 'search.dataset.noResults',
          datasetsColumn: {
            showDatasetsTypeColumn: true,
            showDatasetsNetworkColumn: true,
            showDatasetsStudiesColumn: true,
            showDatasetsVariablesColumn: true
          }
        },
        studies: {
          showSearchTab: true,
          searchLabel: 'search.variable.searchLabel',
          noResultsLabel: 'search.study.noResults',
          showStudiesSearchFilter: true, studiesColumn: {
            showStudiesDesignColumn: true,
            showStudiesQuestionnaireColumn: true,
            showStudiesPmColumn: true,
            showStudiesBioColumn: true,
            showStudiesOtherColumn: true,
            showStudiesParticipantsColumn: true,
            showStudiesNetworksColumn: true,
            showStudiesDatasetsColumn: true,
            showStudiesHarmonizedDatasetsColumn: true,
            showStudiesVariablesColumn: true
          }
        },
        networks: {
          showSearchTab: true,
          searchLabel: 'search.variable.searchLabel',
          noResultsLabel: 'search.network.noResults',
          networksColumn: {
            showNetworksStudiesColumn: true,
            showNetworksStudyDatasetColumn: true,
            showNetworksHarmonizedDatasetColumn: true,
            showNetworksVariablesColumn: true
          }
        }
      };

      this.setLocaleResolver = function(resolver) {
        localeResolver = resolver;
      };

      this.setOptions = function (value) {
        options = angular.merge(options, value);
        //NOTICE: angular.merge merges arrays by position. Overwriting manually.
        options.taxonomyTabsOrder = value.taxonomyTabsOrder || options.taxonomyTabsOrder;
        options.searchTabsOrder = value.searchTabsOrder || options.searchTabsOrder;
        options.resultTabsOrder = value.resultTabsOrder || options.resultTabsOrder;
      };

      this.$get = ['$q', '$injector', function ngObibaMicaSearchFactory($q, $injector) {
        return {
          getLocale: function(success, error) {
            return $q.when($injector.invoke(localeResolver), success, error);
          },
          getOptions: function() {
            return options;
          }
        };
      }];
    });
  }])
  .run(['GraphicChartsConfigurations',
  function (GraphicChartsConfigurations) {
    GraphicChartsConfigurations.setClientConfig();
  }]);
