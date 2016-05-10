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
        targetTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
        searchTabsOrder: [DISPLAY_TYPES.LIST, DISPLAY_TYPES.COVERAGE, DISPLAY_TYPES.GRAPHICS],
        resultTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
        showAllFacetedTaxonomies: true,
        showSearchBox: true,
        showSearchBrowser: true,
        variableTaxonomiesOrder: [],
        studyTaxonomiesOrder: [],
        datasetTaxonomiesOrder: [],
        networkTaxonomiesOrder: [],
        hideNavigate: [],
        hideSearch: ['studyIds', 'dceIds', 'datasetId', 'networkId', 'studyId'],
        variables: {
          showSearchTab: true,
          variablesColumn: {
            showVariablesTypeColumn: true,
            showVariablesStudiesColumn: true,
            showVariablesDatasetsColumn: true,
            showDatasetsStudiesColumn: true,
            showDatasetsVariablesColumn: true
          }
        },
        datasets: {
          showSearchTab: true,
          showDatasetsSearchFilter: true,
          datasetsColumn: {
            showDatasetsAcronymColumn: true,
            showDatasetsTypeColumn: true,
            showDatasetsNetworkColumn: true,
            showDatasetsStudiesColumn: true,
            showDatasetsVariablesColumn: true
          }
        },
        studies: {
          showSearchTab: true,
          showStudiesSearchFilter: true,
          studiesColumn: {
            showStudiesDesignColumn: true,
            showStudiesQuestionnaireColumn: true,
            showStudiesPmColumn: true,
            showStudiesBioColumn: true,
            showStudiesOtherColumn: true,
            showStudiesParticipantsColumn: true,
            showStudiesNetworksColumn: true,
            showStudiesStudyDatasetsColumn: true,
            showStudiesHarmonizationDatasetsColumn: true,
            showStudiesVariablesColumn: false,
            showStudiesStudyVariablesColumn: true,
            showStudiesDataschemaVariablesColumn: true
          }
        },
        networks: {
          showSearchTab: true,
          networksColumn: {
            showNetworksStudiesColumn: true,
            showNetworksStudyDatasetColumn: true,
            showNetworksHarmonizationDatasetColumn: true,
            showNetworksVariablesColumn: false,
            showNetworksStudyVariablesColumn: true,
            showNetworksDataschemaVariablesColumn: true
          }
        },
        coverage: {
          groupBy: {
            study: true,
            dce: true,
            dataset: true,
            dataschema: true,
            network: true
          }
        }
      };

      this.setLocaleResolver = function(resolver) {
        localeResolver = resolver;
      };

      this.setOptions = function (value) {
        options = angular.merge(options, value);
        //NOTICE: angular.merge merges arrays by position. Overriding manually.
        options.targetTabsOrder = value.targetTabsOrder || options.targetTabsOrder;
        options.searchTabsOrder = value.searchTabsOrder || options.searchTabsOrder;
        options.resultTabsOrder = value.resultTabsOrder || options.resultTabsOrder;
        options.variableTaxonomiesOrder = value.variableTaxonomiesOrder || options.variableTaxonomiesOrder;
        options.studyTaxonomiesOrder = value.studyTaxonomiesOrder || options.studyTaxonomiesOrder;
        options.datasetTaxonomiesOrder = value.datasetTaxonomiesOrder || options.datasetTaxonomiesOrder;
        options.networkTaxonomiesOrder = value.networkTaxonomiesOrder || options.networkTaxonomiesOrder;
        options.hideNavigate = value.hideNavigate || options.hideNavigate;
        options.hideSearch = value.hideSearch || options.hideSearch;
      };

      this.$get = ['$q', '$injector', function ngObibaMicaSearchFactory($q, $injector) {
        function normalizeOptions() {
          var canShowCoverage = Object.keys(options.coverage.groupBy).filter(function(canShow) {
              return options.coverage.groupBy[canShow];
            }).length > 0;

          if (!canShowCoverage) {
            var index = options.searchTabsOrder.indexOf(DISPLAY_TYPES.COVERAGE);
            if (index > -1) {
              options.searchTabsOrder.splice(index, 1);
            }
          }
        }

        normalizeOptions();

        return {
          getLocale: function(success, error) {
            return $q.when($injector.invoke(localeResolver), success, error);
          },
          getOptions: function() {
            return options;
          },
          toggleHideSearchNavigate: function (vocabulary) {
            var index = options.hideNavigate.indexOf(vocabulary.name);
            if (index > -1) {
              options.hideNavigate.splice(index, 1);
            } else {
              options.hideNavigate.push(vocabulary.name);
            }
          }
        };
      }];
    });
  }])
  .run(['GraphicChartsConfigurations',
  function (GraphicChartsConfigurations) {
    GraphicChartsConfigurations.setClientConfig();
  }]);
