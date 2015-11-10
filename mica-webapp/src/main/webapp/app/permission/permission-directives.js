'use strict';

mica.attachment

.directive('permissionsTable', [function () {
  return {
    restrict: 'E',
    scope: {
      permission: '=',
      permissions: '=',
      onAdd: '=',
      onLoad: '=',
      onDelete: '='
    },
    templateUrl: 'app/permission/permission-table-template.html',
    controller: 'PermissionsController'
  };
}])

.controller('PermissionsController', ['$rootScope', '$scope', '$modal','NOTIFICATION_EVENTS',
  function ($rootScope, $scope, $modal, NOTIFICATION_EVENTS) {
    $scope.pagination = {searchText: ''};

    function editPermission(permission) {
      $modal.open({
        templateUrl: 'app/permission/permission-modal-form.html',
        controller: 'PermissionsModalController',
        resolve: {
          permission: function() {
            return angular.copy(permission);
          },
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

    }

    $scope.addPermission = function () {
      editPermission({type:'USER', role: 'READER'});
    };

    $scope.editPermission = function (permission) {
      editPermission(permission);
    };

    $scope.deletePermission = function (permission) {
      $scope.principalToDelete = permission.principal;
      $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
        {
          titleKey: 'permission.delete-dialog.title',
          messageKey: 'permission.delete-dialog.message',
          messageArgs: [permission.type === 'USER' ? 'user' : 'group', permission.principal]
        }, permission
      );
    };

    $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, permission) {
      if ($scope.principalToDelete === permission.principal) {
        delete $scope.principalToDelete;
        $scope.onDelete(permission).$promise.then($scope.onLoad);
      }
    });

    $scope.onLoad();
  }])

.controller('PermissionsModalController', ['$scope',
  '$modalInstance',
  '$filter',
  'AlertService',
  'ServerErrorUtils',
  'permission',
  'onAdd',
  function ($scope, $modalInstance, $filter, AlertService, ServerErrorUtils, permission, onAdd) {
    $scope.ROLES = ['READER', 'EDITOR', 'REVIEWER'];
    $scope.TYPES = [
      {name: 'USER', label: $filter('translate')('permission.user')},
      {name: 'GROUP', label: $filter('translate')('permission.group')}
    ];

    var selectedIndex = permission ?
      $scope.TYPES.findIndex(function(type) {
        return type.name === permission.type;
      }) : -1;

    $scope.selectedPermission = selectedIndex > -1 ? $scope.TYPES[selectedIndex] : $scope.TYPES[0];
    $scope.permission = permission ? permission : {type: $scope.selectedPermission.name, role: 'READER'};
    $scope.editMode = permission && permission.principal;

    $scope.save = function (form) {
      if(form.$valid) {
        $scope.permission.type = $scope.selectedPermission.name;
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
