/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.projectConfig

  .controller('ProjectConfigController', ['$rootScope', '$location', '$scope', '$log',
    'ProjectFormResource',
    'ProjectFormService',
    'AlertService',
    'ServerErrorUtils',
    'ProjectFormPermissionsResource',
    function ($rootScope, $location, $scope, $log,
              ProjectFormResource,
              ProjectFormService,
              AlertService,
              ServerErrorUtils,
              ProjectFormPermissionsResource) {

      ProjectFormService.configureAcePaths();

      var saveForm = function() {

        switch (ProjectFormService.isFormValid($scope.form)) {
          case ProjectFormService.ParseResult.VALID:
            $scope.projectForm.definition = $scope.form.definition;
            $scope.projectForm.schema = $scope.form.schema;

            ProjectFormResource.save($scope.projectForm,
              function () {
                $location.path('/admin').replace();
              },
              function (response) {
                AlertService.alert({
                  id: 'ProjectConfigController',
                  type: 'danger',
                  msg: ServerErrorUtils.buildMessage(response)
                });
              });
            break;
          case ProjectFormService.ParseResult.SCHEMA:
          AlertService.alert({
            id: 'ProjectConfigController',
            type: 'danger',
            msgKey: 'project-config.syntax-error.schema'
          });
          break;
        case ProjectFormService.ParseResult.DEFINITION:
          AlertService.alert({
            id: 'ProjectConfigController',
            type: 'danger',
            msgKey: 'project-config.syntax-error.definition'
          });
          break;
        }
      };

      var refreshPreview = function() {
        try {
          if ($scope.dirty) {
            $scope.form.schemaJson = JSON.parse($scope.form.schema);
            $scope.form.definitionJson = JSON.parse($scope.form.definition);
            $scope.dirty = false;
          }
        } catch (e){
        }
      };

      var selectTab = function(id) {
        $scope.selectedTab = id;
        switch (id) {
          case 'form-preview':
            refreshPreview();
            break;
          case 'form-model':
            $scope.modelPreview = ProjectFormService.prettifyJson($scope.form.model);
            break;
        }
      };

      var watchFormSchemaChanges = function(val,old){
        if (val && val !== old) {
          $scope.dirty = true;
        }
      };

      var watchFormDefinitionChanges = function(val,old){
        if (val && val !== old) {
          $scope.dirty = true;
        }
      };

      var aceEditorOnLoadCallback = function(editor) {
        if (editor.container.id === 'form-model') {
          editor.setReadOnly(true);
        }
      };

      var testPreview = function(form) {
        $scope.$broadcast('schemaFormValidate');
        // Then we check if the form is valid
        if (form.$valid) {
          AlertService.alert({
            id: 'ProjectConfigController',
            type: 'success',
            msgKey: 'project-config.preview.tested',
            delay: 5000
          });
        }
      };

      $scope.projectForm = {schema: '', definition: ''};

      ProjectFormResource.get(
        function(projectForm){
          $scope.dirty = true;
          $scope.form.definitionJson = ProjectFormService.parseJsonSafely(projectForm.definition, []);
          $scope.form.definition = ProjectFormService.prettifyJson($scope.form.definitionJson);
          $scope.form.schemaJson = ProjectFormService.parseJsonSafely(projectForm.schema, {});
          $scope.form.schema = ProjectFormService.prettifyJson($scope.form.schemaJson);
          $scope.projectForm = projectForm;
          selectTab('form-schema');

          if ($scope.form.definitionJson.length === 0) {
            AlertService.alert({
              id: 'ProjectConfigController',
              type: 'danger',
              msgKey: 'project-config.parse-error.definition'
            });
          }
          if (Object.getOwnPropertyNames($scope.form.schemaJson).length === 0) {
            AlertService.alert({
              id: 'ProjectConfigController',
              type: 'danger',
              msgKey: 'project-config.parse-error.schema'
            });
          }
        },
        function(response) {
          AlertService.alert({
            id: 'ProjectConfigController',
            type: 'danger',
            msg: ServerErrorUtils.buildMessage(response)
          });
        });

      $scope.form = {
        definitionJson: null,
        definition: null,
        schemaJson: null,
        schema: null,
        model: {}
      };

      var addDraftPermission = function (acl) {
        return ProjectFormPermissionsResource.save({draft: true}, acl);
      };

      var deleteDraftPermission = function (acl) {
        return ProjectFormPermissionsResource.delete({draft: true}, acl);
      };

      $scope.loadPermissions = function() {
        $scope.acls = ProjectFormPermissionsResource.get();
        return $scope.acls;
      };

      $scope.addPermission = function (acl) {
        addDraftPermission(acl);
        return ProjectFormPermissionsResource.save(acl);
      };

      $scope.deletePermission = function (acl) {
        deleteDraftPermission(acl);
        return ProjectFormPermissionsResource.delete(acl);
      };

      $scope.loadPermissions();

      $scope.tab = {name: 'form'};
      $scope.dirty = false;
      $scope.selectedTab = 'form-definition';
      $scope.ace = ProjectFormService.getEditorOptions(aceEditorOnLoadCallback);
      $scope.selectTab = selectTab;
      $scope.testPreview = testPreview;
      $scope.saveForm = saveForm;
      $scope.fullscreen = ProjectFormService.gotoFullScreen;
      $scope.$watch('form.definition', watchFormDefinitionChanges);
      $scope.$watch('form.schema', watchFormSchemaChanges);
    }]);
