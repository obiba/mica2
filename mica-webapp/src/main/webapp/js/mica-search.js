/* exported mica */
'use strict';

var mica = angular.module('mica', [
  'ngObiba',
  'ngRoute',
  'ngSanitize',
  'ngResource',
  'ui.bootstrap',
  'obiba.form',
  'obiba.graphics',
  'obiba.comments',
  'angularUtils.directives.dirPagination',
  'pascalprecht.translate',
  'tmh.dynamicLocale',
  'ngObibaMica'
]);

mica.search = angular.module('mica.search', [
  'ngObiba',
  'ngRoute',
  'ngSanitize',
  'ngResource',
  'ui.bootstrap',
  'obiba.form',
  'obiba.graphics',
  'obiba.comments',
  'angularUtils.directives.dirPagination',
  'pascalprecht.translate',
  'tmh.dynamicLocale',
  'ngObibaMica',
  'obiba.mica.search'
]);

mica.search
  .factory('MicaConfigResource', ['$resource',
    function ($resource) {
      return $resource('ws/config', {}, {
        'get': {method: 'GET'}
      });
    }])
  .provider('SessionProxy',
    function () {
      function Proxy(user) {
        var real = {login: user.name, roles: user.roles, profile: user.data || null};

        this.update = function (value) {
          real = value;
        };

        this.login = function () {
          return real.login;
        };

        this.roles = function () {
          return real.roles || [];
        };

        this.profile = function () {
          return real.profile;
        };
      }

      this.$get = function () {
        return new Proxy({
          name: 'anonymous',
          roles: ['mica-user'],
          data: {}
        });
      };
    })
  .run([
    'GraphicChartsConfig',
    function (GraphicChartsConfig) {
      // TODO
      GraphicChartsConfig.setOptions({});
    }])
  .factory('MicaConfigResource', ['$resource',
    function ($resource) {
      return $resource('../ws/config', {}, {
        'get': {method: 'GET'}
      });
    }])
  .factory('options', [function () {
    const FIELDS_TO_FILTER = ['name', 'title', 'description', 'keywords'];
    const QUERY_TARGETS = {
      NETWORK: 'network',
      STUDY: 'study',
      DATASET: 'dataset',
      VARIABLE: 'variable',
      TAXONOMY: 'taxonomy'
    };
    const DISPLAY_TYPES = {
      LIST: 'list',
      COVERAGE: 'coverage',
      GRAPHICS: 'graphics'
    };

    return {
      searchLayout: 'layout1',
      taxonomyPanelOptions: {
        network: {
          taxonomies: {'Mica_network': {trKey: 'properties'}}
        },
        study: {
          taxonomies: {'Mica_study': {trKey: 'properties'}}
        },
        dataset: {
          taxonomies: {'Mica_dataset': {trKey: 'properties'}}
        },
        variable: {
          taxonomies: {
            'Mica_variable': {trKey: 'properties'}
          }
        },
        fieldsToFilter: FIELDS_TO_FILTER
      },
      obibaListOptions: {
        countCaption: true,
        searchForm: true,
        supplInfoDetails: true,
        trimmedDescription: true
      },
      targetTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
      searchTabsOrder: [DISPLAY_TYPES.LIST, DISPLAY_TYPES.COVERAGE, DISPLAY_TYPES.GRAPHICS],
      resultTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
      showAllFacetedTaxonomies: true,
      showFacetTermsWithZeroCount: false,
      showSearchBox: true,
      showSearchBrowser: true,
      showCopyQuery: true,
      showSearchRefreshButton: false,
      variableTaxonomiesOrder: [],
      studyTaxonomiesOrder: [],
      datasetTaxonomiesOrder: [],
      networkTaxonomiesOrder: [],
      hideNavigate: [],
      hideSearch: ['studyId', 'dceId', 'datasetId', 'networkId'],
      variables: {
        showSearchTab: true,
        listPageSize: 20,
        variablesColumn: {
          showVariablesTypeColumn: true,
          showVariablesStudiesColumn: true,
          showVariablesDatasetsColumn: true,
          showDatasetsStudiesColumn: true,
          showDatasetsVariablesColumn: true
        },
        fields: [
          'attributes.label.*',
          'variableType',
          'datasetId',
          'datasetAcronym'
        ],
        annotationTaxonomies: [
          'Mlstr_area'
        ],
        showCart: true
      },
      datasets: {
        showSearchTab: true,
        listPageSize: 20,
        showDatasetsSearchFilter: true,
        datasetsColumn: {
          showDatasetsAcronymColumn: true,
          showDatasetsTypeColumn: true,
          showDatasetsNetworkColumn: true,
          showDatasetsStudiesColumn: true,
          showDatasetsVariablesColumn: true
        },
        fields: [
          'acronym.*',
          'name.*',
          'variableType',
          'studyTable.studyId',
          'studyTable.project',
          'studyTable.table',
          'studyTable.populationId',
          'studyTable.dataCollectionEventId',
          'harmonizationTable.studyId',
          'harmonizationTable.project',
          'harmonizationTable.table',
          'harmonizationTable.populationId'
        ]
      },
      studies: {
        showSearchTab: true,
        listPageSize: 20,
        showStudiesSearchFilter: true,
        studiesColumn: {
          showStudiesTypeColumn: true,
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
        },
        fields: [
          'acronym.*',
          'name.*',
          'model.methods.design',
          'populations.dataCollectionEvents.model.dataSources',
          'model.numberOfParticipants.participant'
        ]
      },
      networks: {
        showSearchTab: true,
        listPageSize: 20,
        networksColumn: {
          showNetworksStudiesColumn: true,
          showNetworksStudyDatasetColumn: true,
          showNetworksHarmonizationDatasetColumn: true,
          showNetworksVariablesColumn: false,
          showNetworksStudyVariablesColumn: true,
          showNetworksDataschemaVariablesColumn: true
        },
        fields: [
          'acronym.*',
          'name.*',
          'studyIds'
        ]
      },
      coverage: {
        total: {
          showInHeader: true,
          showInFooter: false
        },
        groupBy: {
          study: true,
          dce: true,
          dataset: true
        }
      }
    };
    //return ngObibaMicaSearch.getOptionsAsyn();
  }])
  .config(['$routeProvider', '$locationProvider', 'ObibaServerConfigResourceProvider', 'ngObibaMicaSearchProvider', 'ngObibaMicaUrlProvider', 'tmhDynamicLocaleProvider', '$translateProvider',
    function ($routeProvider, $locationProvider, ObibaServerConfigResourceProvider, ngObibaMicaSearchProvider, ngObibaMicaUrlProvider, tmhDynamicLocaleProvider, $translateProvider) {

      // Initialize angular-translate
      $translateProvider
        .useStaticFilesLoader({
          prefix: 'ws/config/i18n/',
          suffix: '.json'
        })
        .registerAvailableLanguageKeys(['en', 'fr'], {
          'en_*': 'en',
          'fr_*': 'fr',
          '*': 'en'
        })
        .determinePreferredLanguage()
        .fallbackLanguage('en')
        .useCookieStorage()
        .useSanitizeValueStrategy('escaped');

      tmhDynamicLocaleProvider.localeLocationPattern('../bower_components/angular-i18n/angular-locale_{{locale}}.js');
      tmhDynamicLocaleProvider.useCookieStorage('NG_TRANSLATE_LANG_KEY');

      // This will be used to delay the loading of the search config until the options are all resolved; the result is
      // injected to the SearchController.
      /*var optionsResolve = ['ngObibaMicaSearch', function (ngObibaMicaSearch) {
         return ngObibaMicaSearch.getOptionsAsyn();
       }];

       $routeProvider
         .when('/', {
           templateUrl: '../bower_components/ng-obiba-mica/src/search/views/search-layout.html',
           controller: 'SearchController',
           reloadOnSearch: false,
           resolve: {
             options: optionsResolve
           }
         });*/

      // TODO

      //ngObibaMicaSearchProvider.initialize();

      $locationProvider.hashPrefix('');

      ObibaServerConfigResourceProvider.setFactory(
        ['MicaConfigResource', function (MicaConfigResource) {
          return {get: MicaConfigResource.get};
        }]
      );

      ngObibaMicaUrlProvider.setUrl('TaxonomiesSearchResource', '../ws/taxonomies/_search');
      ngObibaMicaUrlProvider.setUrl('TaxonomiesResource', '../ws/taxonomies/_filter');
      ngObibaMicaUrlProvider.setUrl('TaxonomyResource', '../ws/taxonomy/:taxonomy/_filter');
      ngObibaMicaUrlProvider.setUrl('VocabularyResource', '../ws/taxonomy/:taxonomy/vocabulary/:vocabulary/_filter');
      ngObibaMicaUrlProvider.setUrl('JoinQuerySearchResource', 'ws/:type/_rql');
      ngObibaMicaUrlProvider.setUrl('JoinQuerySearchCsvResource', 'ws/:type/_rql_csv?query=:query');
      ngObibaMicaUrlProvider.setUrl('JoinQuerySearchCsvReportResource', 'ws/:type/_report?query=:query');
      ngObibaMicaUrlProvider.setUrl('JoinQuerySearchCsvReportByNetworkResource', 'ws/:type/_report_by_network?networkId=:networkId&locale=:locale');
      ngObibaMicaUrlProvider.setUrl('JoinQueryCoverageResource', 'ws/variables/_coverage');
      ngObibaMicaUrlProvider.setUrl('JoinQueryCoverageDownloadResource', 'ws/variables/_coverage_download?query=:query');
      // TODO more URL override

    }]);
