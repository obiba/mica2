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

mica.shareResource = angular.module('mica.shareResource', [
  'mica.config',
  'obiba.notification',
  'obiba.form',
  'obiba.mica.localized',
  'pascalprecht.translate',
  'ui.bootstrap'
]);

mica.shareResource
  .factory('ShareService', ['$resource',
    function ($resource) {

      return $resource('ws/draft/:resourceType/:resourceId/_share', {}, {
        'getShareLink': {
          method: 'PUT',
          params: {resourceType: '@resourceType', resourceId: '@resourceId', expire: '@expire'},
          responseType: 'text',
          transformResponse: function (response) {
            return {body: response};
          }
        }
      });
    }])

  .controller('ShareModalController', ['$scope', '$uibModalInstance', 'ShareService', 'resourceType', 'resourceId', 'moment',
    function ($scope, $uibModalInstance, ShareService, resourceType, resourceId, moment) {

      $scope.resourceType = resourceType;
      $scope.resourceId = resourceId;
      $scope.expire = moment().add(1, 'month').toDate();
      $scope.shareLink = '';

      $scope.updateShareLink = function () {
        ShareService.getShareLink({
          resourceType: $scope.resourceType,
          resourceId: $scope.resourceId,
          expire: ($scope.expire !== null) ? moment($scope.expire).format('YYYY-MM-DD') : null
        }).$promise.then(function (response) {
          $scope.shareLink = response.body;
        });
      };

      $scope.close = function () {
        $uibModalInstance.dismiss('close');
      };

      $scope.updateShareLink();

    }])

  .controller('ShareButtonController', ['$scope', '$uibModal',
    function ($scope, $uibModal) {

      $scope.openShareModal = function (resourceType, resourceId) {

        $uibModal.open({
          templateUrl: 'app/share-resource/modal-view.html',
          controller: 'ShareModalController',
          resolve: {
            resourceType: function () {
              return resourceType;
            },
            resourceId: function () {
              return resourceId;
            }
          }
        });
      };
    }])

  .directive('shareModal', [function () {
    return {
      restrict: 'EA',
      templateUrl: 'app/share-resource/modal-button.html',
      scope: {
        resourceType: '=',
        resourceId: '='
      },
      controller: 'ShareButtonController'
    };
  }]);
