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
      function Proxy() {
        var real;
        this.update = function (value) {
          real = value;
        };

        this.login = function() {
          return real.login;
        };

        this.roles = function() {
          return real.roles || [];
        };

        this.profile = function() {
          return real.profile;
        };
      }

      this.$get = function() {
        return new Proxy();
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
  .config(['$routeProvider', '$locationProvider', 'ObibaServerConfigResourceProvider', 'ngObibaMicaSearchProvider', 'ngObibaMicaUrlProvider',
    function ($routeProvider, $locationProvider, ObibaServerConfigResourceProvider, ngObibaMicaSearchProvider, ngObibaMicaUrlProvider) {
      // This will be used to delay the loading of the search config until the options are all resolved; the result is
      // injected to the SearchController.
      var optionsResolve = ['ngObibaMicaSearch', function (ngObibaMicaSearch) {
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
        });

      // TODO
      ngObibaMicaSearchProvider.initialize({});

      $locationProvider.hashPrefix('');

      ObibaServerConfigResourceProvider.setFactory(
        ['MicaConfigResource', function(MicaConfigResource){
          return {get: MicaConfigResource.get};
        }]
      );

      ngObibaMicaUrlProvider.setUrl('TaxonomiesSearchResource', '../ws/taxonomies/_search');
      ngObibaMicaUrlProvider.setUrl('TaxonomiesResource', '../ws/taxonomies/_filter');
      ngObibaMicaUrlProvider.setUrl('TaxonomyResource', '../ws/taxonomy/:taxonomy/_filter');
      ngObibaMicaUrlProvider.setUrl('VocabularyResource', '../ws/taxonomy/:taxonomy/vocabulary/:vocabulary/_filter');
      // TODO more URL override

    }]);
