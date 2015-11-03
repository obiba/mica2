'use strict';

mica.attachment

.directive('permissionsTable', [function () {
  return {
    restrict: 'E',
    scope: {
      permissions: '=',
      onAdd: '=',
      onLoad: '=',
      onDelete: '='
    },
    templateUrl: 'app/permission/permission-table-template.html',
    controller: 'PermissionsController'
  };
}])

.controller('PermissionsController', ['$scope', '$modal', function ($scope, $modal) {
  $scope.pagination = {searchText: ''};

  $scope.addPermission = function () {
    $modal.open({
      templateUrl: 'app/permission/permission-modal-form.html',
      controller: 'PermissionsModalController',
      resolve: {
        onAdd: function() {
          return $scope.onAdd;
        },
        onLoad: function() {
          return $scope.onLoad;
        }
      }
    }).result.then(function(reload) {
        if (reload) {
          $scope.onLoad();
        }
      });
  };

  $scope.deletePermission = function (permission) {
    $scope.onDelete(permission).$promise.then($scope.onLoad);
  };

  $scope.onLoad();
}])

.controller('PermissionsModalController', ['$scope', '$modalInstance', 'AlertService', 'ServerErrorUtils', 'onAdd',
  function ($scope, $modalInstance, AlertService, ServerErrorUtils, onAdd) {
    $scope.ROLES = ['READER', 'EDITOR', 'PUBLISHER'];
    $scope.TYPES = ['USER', 'GROUP'];
    $scope.permission = {};

    $scope.save = function (form) {
      if(form.$valid) {
        onAdd($scope.permission).$promise.then(function () {
          $modalInstance.close(true);
        }, function (response) {
          AlertService.alert({
            id: 'formAlert',
            type: 'danger',
            msg: ServerErrorUtils.buildMessage(response)
          });
        });
      }

      form.saveAttempted = true;
    };

    $scope.cancel = function () {
      $modalInstance.close();
    };
  }]);
