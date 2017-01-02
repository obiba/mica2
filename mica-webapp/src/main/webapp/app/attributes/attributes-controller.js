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

mica.attributes

  .controller('AttributesListController', ['$scope', '$log', 'attributesService',

    function ($scope, $log, attributesService) {
      $scope.$watch('attributes', function () {
        attributesService.groupByNamespace($scope);
      });

    }])

  .controller('AttributesEditableListController', ['$scope', '$log', '$uibModal', 'attributesService',

    function ($scope, $log, $uibModal, attributesService) {

      $scope.deleteAttribute = function(index) {
        attributesService.deleteAttribute($scope, index);
      };

      $scope.addAttribute = function() {
        $uibModal
          .open({
            templateUrl: 'app/attributes/views/attribute-modal-form.html',
            controller: 'AttributeModalController',
            resolve: {
              attribute: function () {
                return {};
              }
            }
          })
          .result.then(function (attribute) {
            attributesService.addAttribute($scope, attribute);
          }, function () {
          });
      };

      $scope.editAttribute = function(index) {

        $uibModal
          .open({
            templateUrl: 'app/attributes/views/attribute-modal-form.html',
            controller: 'AttributeModalController',
            resolve: {
              attribute: function () {
                return $scope.attributes[index];
              }
            }
          })
          .result.then(function (attribute) {
            attributesService.editAttribute($scope, index, attribute);
          }, function () {
          });
      };

      $scope.$watch('attributes', function () {
        attributesService.groupByNamespace($scope);
      });


    }])

  .controller('AttributeModalController', ['$scope', '$uibModalInstance', '$log', 'attributeModalService', 'attribute',
    function ($scope, $uibModalInstance, $log, attributeModalService, attribute) {
      $scope.attribute =  $.extend(true, {}, attribute);
      attributeModalService.normalizeLocales($scope);

      $scope.save = function (form) {
        if (form.$valid) {
          attributeModalService.validateLocales($scope);
          $uibModalInstance.close($scope.attribute);
        }
        else {
          $scope.form = form;
          $scope.form.saveAttempted = true;
        }
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

    }]);
