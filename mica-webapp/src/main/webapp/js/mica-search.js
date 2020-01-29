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
  .config(['$locationProvider', 'ObibaServerConfigResourceProvider', 'ngObibaMicaSearchProvider',
    function ($locationProvider, ObibaServerConfigResourceProvider, ngObibaMicaSearchProvider) {
      // TODO
      ngObibaMicaSearchProvider.initialize({});

      $locationProvider.hashPrefix('');

      ObibaServerConfigResourceProvider.setFactory(
        ['MicaConfigResource', function(MicaConfigResource){
          return {get: MicaConfigResource.get};
        }]
      );

    }]);
