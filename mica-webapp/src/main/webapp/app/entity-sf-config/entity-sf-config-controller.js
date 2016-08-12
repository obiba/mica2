/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *  
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.entitySfConfig

  .controller('EntitySfConfigController', [
    '$scope',
    'EntitySchemaFormService',
    'LocalizedSchemaFormService',
    'AlertService',
    function ($scope,
              EntitySchemaFormService,
              LocalizedSchemaFormService,
              AlertService) {

      EntitySchemaFormService.configureAcePaths();

      var refreshPreview = function(force) {
        try {
          if ($scope.dirty || force) {
            $scope.form.schemaJson = LocalizedSchemaFormService.translate(JSON.parse($scope.form.schema));
            $scope.form.definitionJson = LocalizedSchemaFormService.translate(JSON.parse($scope.form.definition));
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
            $scope.modelPreview = EntitySchemaFormService.prettifyJson($scope.form.model);
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
            id: $scope.alertId,
            type: 'success',
            msgKey: 'entity-sf-config.preview.tested',
            delay: 5000
          });
        }
      };

      $scope.entityForm = {schema: '', definition: ''};
      $scope.dirty = false;
      $scope.selectedTab = 'form-schema';
      $scope.ace = EntitySchemaFormService.getEditorOptions(aceEditorOnLoadCallback);
      $scope.selectTab = selectTab;
      $scope.testPreview = testPreview;
      $scope.fullscreen = EntitySchemaFormService.gotoFullScreen;
      $scope.$watch('form.definition', watchFormDefinitionChanges);
      $scope.$watch('form.schema', watchFormSchemaChanges);
      
      refreshPreview(true);
    }]);
