/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.search = angular.module('mica.search', [
    'obiba.mica.search'
  ])
  .config(['ngObibaMicaSearchProvider', 'ngObibaMicaSearchTemplateUrlProvider',
    function (ngObibaMicaSearchProvider, ngObibaMicaSearchTemplateUrlProvider) {
      ngObibaMicaSearchProvider.setLocaleResolver(['$q', '$translate', 'MicaConfigResource', function ($q, $translate, MicaConfigResource) {
        var res = $q.defer();

        MicaConfigResource.get(function (micaConfig) {
          res.resolve(micaConfig.languages || $translate.use());
        });

        return res.promise;
      }]);
      ngObibaMicaSearchProvider.setOptionsResolver(['$q', 'MicaConfigResource', function ($q, MicaConfigResource) {
        var res = $q.defer();

        MicaConfigResource.get(function (micaConfig) {
          var hasMultipleNetworks = micaConfig.isNetworkEnabled && !micaConfig.isSingleNetworkEnabled;
          var hasMultipleStudies = micaConfig.isHarmonizedDatasetEnabled;
          var hasMultipleDatasets = micaConfig.isCollectedDatasetEnabled || micaConfig.isHarmonizedDatasetEnabled;
          var options = {
            showSearchRefreshButton: true,
            networks: {
              showSearchTab: hasMultipleNetworks
            },
            studies: {
              showSearchTab: hasMultipleStudies,
              studiesColumn: {
                showStudiesNetworksColumn: hasMultipleNetworks,
                showStudiesVariablesColumn: hasMultipleDatasets,
                showStudiesStudyDatasetsColumn: hasMultipleDatasets && micaConfig.isCollectedDatasetEnabled,
                showStudiesStudyVariablesColumn: hasMultipleDatasets && micaConfig.isCollectedDatasetEnabled,
                showStudiesHarmonizationDatasetsColumn: hasMultipleDatasets && micaConfig.isHarmonizedDatasetEnabled,
                showStudiesDataschemaVariablesColumn: hasMultipleDatasets && micaConfig.isHarmonizedDatasetEnabled
              }
            },
            datasets: {
              showSearchTab: hasMultipleDatasets,
              datasetsColumn: {
                showDatasetsTypeColumn: micaConfig.isCollectedDatasetEnabled && micaConfig.isHarmonizedDatasetEnabled,
                showDatasetsNetworkColumn: hasMultipleNetworks,
                showDatasetsStudiesColumn: hasMultipleStudies
              }
            },
            variables: {
              showSearchTab: hasMultipleDatasets,
              variablesColumn: {
                showVariablesTypeColumn: micaConfig.isCollectedDatasetEnabled && micaConfig.isHarmonizedDatasetEnabled,
                showVariablesStudiesColumn: hasMultipleStudies
              }
            }
          };
          res.resolve(options);
        });

        return res.promise;
      }]);
      ngObibaMicaSearchTemplateUrlProvider.setHeaderUrl('search', 'app/search/views/search-view-header.html');
      ngObibaMicaSearchTemplateUrlProvider.setHeaderUrl('classifications', 'app/search/views/classifications-view-header.html');
    }]);
