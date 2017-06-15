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
    self.permissions = DraftDocumentPermissionsResource.query({id: $routeParams.id});
    return self.permissions;
  };

  self.deletePermission = function (permission) {
    return DraftDocumentPermissionsResource.delete({id: $routeParams.id}, permission);
  };

  self.addPermission = function (permission) {
    return DraftDocumentPermissionsResource.save({id: $routeParams.id}, permission);
  };

  self.loadAccesses = function () {
    self.accesses = DraftDocumentAccessesResource.query({id: $routeParams.id});
    return self.accesses;
  };

  self.deleteAccess = function (access) {
    return DraftDocumentAccessesResource.delete({id: $routeParams.id}, access);
  };

  self.addAccess = function (access) {
    return DraftDocumentAccessesResource.save({id: $routeParams.id}, access);
  };

  angular.extend($scope, self); // update scope
};
