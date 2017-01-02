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

mica.projectConfig

  .controller('ProjectConfigController', ['$rootScope',
    '$q',
    '$location',
    '$scope',
    '$log',
    'ProjectFormCustomResource',
    'EntitySchemaFormService',
    'LocalizedSchemaFormService',
    'AlertBuilder',
    'ProjectFormPermissionsResource',
    'ProjectFormAccessesResource',
    'MicaConfigResource',
    function ($rootScope,
              $q,
              $location,
              $scope,
              $log,
              ProjectFormCustomResource,
              EntitySchemaFormService,
              LocalizedSchemaFormService,
              AlertBuilder,
              ProjectFormPermissionsResource,
              ProjectFormAccessesResource,
              MicaConfigResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.openAccess = micaConfig.openAccess;
      });

      var saveForm = function() {
        switch (EntitySchemaFormService.isFormValid($scope.form)) {
          case EntitySchemaFormService.ParseResult.VALID:
            $scope.projectForm.definition = $scope.form.definition;
            $scope.projectForm.schema = $scope.form.schema;
            ProjectFormCustomResource.save($scope.projectForm,
              function () {
                $location.path('/admin').replace();
              },
              function (response) {
                AlertBuilder.newBuilder().response(response).build();
              });
            break;
          case EntitySchemaFormService.ParseResult.SCHEMA:
            AlertBuilder.newBuilder().trMsg('entity-config.syntax-error.schema').build();
            break;
          case EntitySchemaFormService.ParseResult.DEFINITION:
            AlertBuilder.newBuilder().trMsg('entity-config.syntax-error.definition').build();
            break;
        }
      };

      $scope.state = new mica.commons.EntityState($q, $scope);
      $scope.projectForm = {schema: '', definition: ''};

      ProjectFormCustomResource.get(
        function(projectForm){
          $scope.form.definitionJson = EntitySchemaFormService.parseJsonSafely(projectForm.definition, []);
          $scope.form.definition = EntitySchemaFormService.prettifyJson($scope.form.definitionJson);
          $scope.form.schemaJson = EntitySchemaFormService.parseJsonSafely(projectForm.schema, {});
          $scope.form.schema = EntitySchemaFormService.prettifyJson($scope.form.schemaJson);
          $scope.projectForm = projectForm;

          if ($scope.form.definitionJson.length === 0) {
            AlertBuilder.newBuilder().trMsg('entity-config.parse-error.definition').build();
          }
          if (Object.getOwnPropertyNames($scope.form.schemaJson).length === 0) {
            AlertBuilder.newBuilder().trMsg('entity-config.parse-error.schema').build();
          }
        },
        function(response) {
          AlertBuilder.newBuilder().response(response).build();
        });

      $scope.form = {
        definitionJson: null,
        definition: null,
        schemaJson: null,
        schema: null,
        model: {}
      };

      $scope.loadPermissions = function() {
        $scope.acls = ProjectFormPermissionsResource.get();
        return $scope.acls;
      };

      $scope.addPermission = function (acl) {
        return ProjectFormPermissionsResource.save(acl);
      };

      $scope.deletePermission = function (acl) {
        return ProjectFormPermissionsResource.delete(acl);
      };

      $scope.loadAccesses =function () {
        $scope.accesses = ProjectFormAccessesResource.get();
        return $scope.accesses;
      };

      $scope.addAccess = function (acl) {
        return ProjectFormAccessesResource.save(acl);
      };

      $scope.deleteAccess = function (acl) {
        return ProjectFormAccessesResource.delete(acl);
      };

      $scope.titleInfo = {
        'permissions': 'permission.help',
        'accesses': 'access.help'
      };

      $scope.tab = {name: 'form'};
      $scope.saveForm = saveForm;
    }]);
