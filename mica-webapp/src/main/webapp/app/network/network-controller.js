/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.network

  .controller('NetworkListController', ['$scope', 'NetworksResource', 'NetworkResource',

    function ($scope, NetworksResource, NetworkResource) {

      $scope.networks = NetworksResource.query();

      $scope.deleteNetwork = function (id) {
        //TODO ask confirmation
        NetworkResource.delete({id: id},
          function () {
            $scope.networks = NetworksResource.query();
          });
      };

    }])

  .controller('NetworkEditController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'NetworkResource', 'DraftNetworksResource', 'NetworkPublicationResource', 'MicaConfigResource', 'FormServerValidation',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, NetworkResource, DraftNetworksResource, NetworkPublicationResource, MicaConfigResource, FormServerValidation) {

      $scope.network = $routeParams.id ?
        NetworkResource.get({id: $routeParams.id}) : {published: false, 'obiba.mica.NetworkDto.type': {} };

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.save = function () {
        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }
        if ($scope.network.id) {
          updateNetwork();
        } else {
          createNetwork();
        }
      };

      $scope.cancel = function () {
        $location.path('/network' + ($scope.network.id ? '/' + $scope.network.id : '')).replace();
      };

      var updateNetwork = function () {
        $scope.network.$save(
          function (network) {
            $location.path('/network/' + network.id).replace();
          },
          saveErrorHandler);
      };

      var createNetwork = function () {
        DraftNetworksResource.save($scope.network,
          function (resource, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            $location.path('/network/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

    }])

  .controller('NetworkViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'NetworkResource', 'NetworkPublicationResource', 'MicaConfigResource',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, NetworkResource, NetworkPublicationResource, MicaConfigResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.network = NetworkResource.get({id: $routeParams.id});

      $scope.isPublished = function() {
        return $scope.network.published;
      };

      $scope.publish = function () {
        if ($scope.network.published) {
          NetworkPublicationResource.unPublish({id: $scope.network.id}, function () {
            $scope.network = NetworkResource.get({id: $routeParams.id});
          });
        } else {
          NetworkPublicationResource.publish({id: $scope.network.id}, function () {
            $scope.network = NetworkResource.get({id: $routeParams.id});
          });
        }
      };

    }]);
