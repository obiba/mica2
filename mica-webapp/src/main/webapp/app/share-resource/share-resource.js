/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
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
          errorHandler: true,
          transformResponse: function (response) {
            return {body: response};
          }
        }
      });
    }])

  .controller('ShareModalController', ['$scope', '$uibModalInstance', '$timeout', 'ShareService', 'AlertService', 'LocaleStringUtils', 'resourceType', 'resourceId', 'moment',
    function ($scope, $uibModalInstance, $timeout, ShareService, AlertService, LocaleStringUtils, resourceType, resourceId, moment) {

      $scope.resourceType = resourceType;
      $scope.resourceId = resourceId;
      $scope.shareLink = '';
      $scope.format = 'YYYY-MM-DD';
      $scope.showDatePicker = false;

      $scope.expireDate = moment().add(1, 'month').toDate();
      $scope.expireTimePicker = $scope.expireDate;
      $scope.expireAsString = moment($scope.expireTimePicker).format('YYYY-MM-DD');

      $scope.showCopiedTooltipStatus = false;

      $scope.showCopiedTooltip = function () {
        $scope.showCopiedTooltipStatus = true;
        $timeout(function () {
          $scope.showCopiedTooltipStatus = false;
        }, 1000);
      };

      $scope.$watch('expireDate', function (newValue, oldValue) {
        if (newValue !== oldValue) {
          $scope.updateShareLink();
        }
      });

      $scope.updatedExpireTimePicker = function () {
        $scope.expireAsString = ($scope.expireTimePicker !== null) ? moment($scope.expireTimePicker).format('YYYY-MM-DD') : null;
        $scope.expireDate = $scope.expireTimePicker;
      };

      $scope.updatedExpireAsString = function () {

        if ($scope.expireAsString === '') {
          $scope.expireTimePicker = null;
          $scope.expireDate = null;
          return;
        }

        var dateAsMoment = moment($scope.expireAsString, 'YYYY-MM-DD');
        if (dateAsMoment.isValid()) {
          $scope.expireTimePicker = dateAsMoment.toDate();
          $scope.expireDate = dateAsMoment;
        }
      };

      $scope.updateShareLink = function () {

        $scope.showDatePicker = false;

        ShareService.getShareLink({
          resourceType: $scope.resourceType,
          resourceId: $scope.resourceId,
          expire: ($scope.expireDate !== null) ? moment($scope.expireDate).format('YYYY-MM-DD') : null

        }).$promise.then(function (response) {
          $scope.shareLink = response.body;

        }, function (error) {

          var errorKey = error.status === 409 ? 'share-resource.error-no-portal-url' : 'global.server-error';
          AlertService.alert({
            id: 'formAlert',
            type: 'danger',
            msg: LocaleStringUtils.translate(errorKey)
          });
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
        resourceId: '=',
        permissions: '='
      },
      controller: 'ShareButtonController'
    };
  }]);
