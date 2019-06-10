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

mica.permission

.directive('permissionsTable', [function () {
  return {
    restrict: 'E',
    scope: {
      permissions: '=',
      accesses: '=',
      openAccess: '=',
      onAdd: '=',
      onLoad: '=',
      onDelete: '=',
      onAccessAdd: '=',
      onAccessLoad: '=',
      onAccessDelete: '=',
      permissionRoles: '=',
      customTitleInfo: '='

    },
    templateUrl: 'app/permission/permission-table-template.html',
    controller: 'PermissionsController'
  };
}])

.directive('permissionConfigTable', [function () {
  return {
    restrict: 'E',
    scope: {
      permissions: '=',
      onAdd: '=',
      onDelete: '=',
      onLoad: '=',
      name: '=',
      otherResources: '<',
      overriddenRoleHelpTexts: '<',
      allowBlockedPrincipals: '<'
    },
    templateUrl: 'app/permission/permission-config-table-template.html',
    controller: 'PermissionsConfigController'
  };
}])

.controller('PermissionsConfigController', ['$rootScope', '$scope', '$uibModal', 'NOTIFICATION_EVENTS',
  function ($rootScope, $scope, $uibModal, NOTIFICATION_EVENTS) {
    $scope.pagination = {searchText: ''};

    function editPermission(acl) {
      $uibModal.open({
        templateUrl: 'app/permission/permission-config-modal-form.html',
        controller: 'PermissionsConfigModalController',
        resolve: {
          acl: function () {
            return angular.copy(acl);
          },
          onAdd: function () {
            return $scope.onAdd;
          },
          name: function () {
            return $scope.name;
          },
          otherResources: function () {
            return $scope.otherResources;
          },
          allowBlockedPrincipals: function () {
            return $scope.allowBlockedPrincipals;
          },
          overriddenRoleHelpTexts: function () {
            return $scope.overriddenRoleHelpTexts;
          }
        }
      }).result.then(function(result) {
        if (result) {
          $scope.onLoad();
        }
      });
    }

    $scope.addPermission = function() {
      editPermission({role: 'READER', type: 'USER', file: true});
    };

    $scope.editPermission = function (acl) {
      acl.file =true;
      editPermission(acl);
    };

    $scope.deletePermission = function (acl) {
      $scope.principalPermissionToDelete = acl.principal;
      $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
        {
          titleKey: 'permission.delete-dialog.title',
          messageKey: 'permission.delete-dialog.message',
          messageArgs: [acl.type === 'USER' ? 'user' : 'group', acl.principal]
        }, acl
      );
    };

    $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, acl) {
      if ($scope.principalPermissionToDelete && $scope.principalPermissionToDelete === acl.principal) {
        delete $scope.principalPermissionToDelete;
        $scope.onDelete(acl).$promise.then($scope.onLoad);
      }
    });

    $scope.onLoad();
  }])

.controller('PermissionsConfigModalController', ['$scope', '$uibModalInstance', 'AlertService', 'ServerErrorUtils', '$filter', 'acl', 'onAdd', 'name', 'otherResources', 'allowBlockedPrincipals', 'overriddenRoleHelpTexts',
  function ($scope, $uibModalInstance, AlertService, ServerErrorUtils, $filter, acl, onAdd, name, otherResources, allowBlockedPrincipals, overriddenRoleHelpTexts) {
    $scope.ROLES = ['READER'];
    $scope.TYPES = [
      {name: 'USER', label: $filter('translate')('permission.user')},
      {name: 'GROUP', label: $filter('translate')('permission.group')}
    ];

    var BLOCKED_NAMES = [
      'mica-user',
      'mica-reviewer',
      'mica-editor',
      'mica-data-access-officer',
      'mica-administrator'
    ];

    $scope.name = {arg0: $filter('translate')(name).toLowerCase()};
    $scope.others = otherResources;

    var selectedIndex = acl ?
      $scope.TYPES.findIndex(function(type) {
        return type.name === acl.type;
      }) : -1;

    $scope.selectedType = selectedIndex > -1 ? $scope.TYPES[selectedIndex] : $scope.TYPES[0];
    $scope.editMode = acl && acl.principal;
    $scope.acl = acl;

    $scope.chosen = { others: {} };
    ($scope.acl.otherResources || []).forEach(function (other) {
      $scope.chosen.others[other] = true;
    });

    $scope.save = function (form) {
      form.principal.$setValidity('reserved-groups', true);
      if (!allowBlockedPrincipals && 'GROUP' === $scope.selectedType.name && BLOCKED_NAMES.indexOf(acl.principal) > -1) {
        form.principal.$setValidity('reserved-groups', false);
        AlertService.alert({
          id: 'PermissionsConfigModalController',
          type: 'danger',
          msgKey: 'permission.error.reserved-groups',
          msgArgs: [BLOCKED_NAMES.join(', ')]
        });
      }

      if(form.$valid) {
        $scope.acl.type = $scope.selectedType.name;

        $scope.acl.otherResources = Object.keys($scope.chosen.others|| {}).filter(function (value) { return $scope.chosen.others[value]; });

        onAdd($scope.acl).$promise.then(function () {
          $uibModalInstance.close(true);
        }, function (response) {
          AlertService.alert({
            id: 'PermissionsConfigModalController',
            type: 'danger',
            msg: ServerErrorUtils.buildMessage(response)
          });
        });
      }

      form.saveAttempted = true;
    };

    $scope.getRoleHelpText = function(role) {
      var text = 'permission.' + role.toLowerCase() + '-config-help';
      if (overriddenRoleHelpTexts) {
        text = overriddenRoleHelpTexts[role] || text;
      }
      return text;
    };

    $scope.cancel = function () {
      $uibModalInstance.close();
    };
  }])

.controller('PermissionsController', ['$rootScope', '$scope', '$uibModal','NOTIFICATION_EVENTS',
  function ($rootScope, $scope, $uibModal, NOTIFICATION_EVENTS) {
    $scope.pagination = {searchText: ''};

    // draft permissions

    function editPermission(acl) {
      $uibModal.open({
        templateUrl: 'app/permission/permission-modal-form.html',
        controller: 'PermissionsModalController',
        resolve: {
          acl: function() {
            return angular.copy(acl);
          },
          onAdd: function() {
            return $scope.onAdd;
          },
          onLoad: function() {
            return $scope.onLoad;
          },
          permissionRoles: function () {
            return $scope.permissionRoles;
          }
        }
      }).result.then(function(reload) {
        if (reload) {
          $scope.onLoad();
        }
      });

    }

    $scope.addPermission = function () {
      editPermission({type:'USER', role: 'READER', file: true});
    };

    $scope.editPermission = function (acl) {
      editPermission(acl);
    };

    $scope.deletePermission = function (acl) {
      $scope.principalPermissionToDelete = acl.principal;
      $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
        {
          titleKey: 'permission.delete-dialog.title',
          messageKey: 'permission.delete-dialog.message',
          messageArgs: [acl.type === 'USER' ? 'user' : 'group', acl.principal]
        }, acl
      );
    };

    $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, acl) {
      if ($scope.principalPermissionToDelete && $scope.principalPermissionToDelete === acl.principal) {
        delete $scope.principalPermissionToDelete;
        $scope.onDelete(acl).$promise.then($scope.onLoad);
      }
    });

    $scope.onLoad();

    // published permissions (=access)

    function editAccess(acl) {
      $uibModal.open({
        templateUrl: 'app/permission/access-modal-form.html',
        controller: 'AccessesModalController',
        resolve: {
          acl: function() {
            return angular.copy(acl);
          },
          onAdd: function() {
            return $scope.onAccessAdd;
          },
          onLoad: function() {
            return $scope.onAccessLoad;
          }
        }
      }).result.then(function(reload) {
        if (reload) {
          $scope.onAccessLoad();
        }
      });
    }

    $scope.addAccess = function () {
      editAccess({type:'USER', role: 'READER', file: true});
    };

    $scope.deleteAccess = function (acl) {
      $scope.principalAccessToDelete = acl.principal;
      $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
        {
          titleKey: 'access.delete-dialog.title',
          messageKey: 'access.delete-dialog.message',
          messageArgs: [acl.type === 'USER' ? 'user' : 'group', acl.principal]
        }, acl
      );
    };

    $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, acl) {
      if ($scope.principalAccessToDelete && $scope.principalAccessToDelete === acl.principal) {
        delete $scope.principalAccessToDelete;
        $scope.onAccessDelete(acl).$promise.then($scope.onAccessLoad);
      }
    });

    $scope.onAccessLoad();
  }])

.controller('PermissionsModalController', ['$scope',
  '$uibModalInstance',
  '$filter',
  'AlertService',
  'ServerErrorUtils',
  'acl',
  'onAdd',
  function ($scope, $uibModalInstance, $filter, AlertService, ServerErrorUtils, acl, onAdd) {
    $scope.ROLES = ['READER', 'EDITOR', 'REVIEWER'];
    $scope.TYPES = [
      {name: 'USER', label: $filter('translate')('permission.user')},
      {name: 'GROUP', label: $filter('translate')('permission.group')}
    ];

    var selectedIndex = acl ?
      $scope.TYPES.findIndex(function(type) {
        return type.name === acl.type;
      }) : -1;

    $scope.selectedType = selectedIndex > -1 ? $scope.TYPES[selectedIndex] : $scope.TYPES[0];
    $scope.acl = acl ? acl : {type: $scope.selectedType.name, role: 'READER'};
    $scope.editMode = acl && acl.principal;

    $scope.save = function (form) {
      if(form.$valid) {
        $scope.acl.type = $scope.selectedType.name;

        onAdd($scope.acl).$promise.then(function () {
          $uibModalInstance.close(true);
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
      $uibModalInstance.close();
    };
  }])

.controller('AccessesModalController', ['$scope',
  '$uibModalInstance',
  '$filter',
  'AlertService',
  'ServerErrorUtils',
  'acl',
  'onAdd',
  function ($scope, $uibModalInstance, $filter, AlertService, ServerErrorUtils, acl, onAdd) {
    $scope.TYPES = [
      {name: 'USER', label: $filter('translate')('permission.user')},
      {name: 'GROUP', label: $filter('translate')('permission.group')}
    ];

    var selectedIndex = acl ?
      $scope.TYPES.findIndex(function(type) {
        return type.name === acl.type;
      }) : -1;

    $scope.selectedType = selectedIndex > -1 ? $scope.TYPES[selectedIndex] : $scope.TYPES[0];
    $scope.acl = acl ? acl : {type: $scope.selectedType.name, role: 'READER'};

    $scope.save = function (form) {
      if(form.$valid) {
        $scope.acl.type = $scope.selectedType.name;
        onAdd($scope.acl).$promise.then(function () {
          $uibModalInstance.close(true);
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
      $uibModalInstance.close();
    };
  }]);
