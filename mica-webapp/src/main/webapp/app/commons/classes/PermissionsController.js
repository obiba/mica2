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

mica.commons.PermissionsController = function (
  $scope,
  $routeParams,
  DraftDocumentPermissionsResource,
  DraftDocumentAccessesResource
) {
  var self = this;

  self.permissions= [];
  self.accesses = [];

  self.loadPermissions = function () {
    $scope.permissions = DraftDocumentPermissionsResource.query({id: $routeParams.id});
    return $scope.permissions;
  };

  self.deletePermission = function (permission) {
    return DraftDocumentPermissionsResource.delete({id: $routeParams.id}, permission);
  };

  self.addPermission = function (permission) {
    return DraftDocumentPermissionsResource.save({id: $routeParams.id}, permission);
  };

  self.loadAccesses = function () {
    $scope.accesses = DraftDocumentAccessesResource.query({id: $routeParams.id});
    return $scope.accesses;
  };

  self.deleteAccess = function (access) {
    return DraftDocumentAccessesResource.delete({id: $routeParams.id}, access);
  };

  self.addAccess = function (access) {
    return DraftDocumentAccessesResource.save({id: $routeParams.id}, access);
  };

  angular.extend($scope, self); // update scope
};
