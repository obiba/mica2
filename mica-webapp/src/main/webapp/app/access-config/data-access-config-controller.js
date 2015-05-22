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

mica.dataAccesConfig

  .controller('DataAccessConfigController', ['$rootScope', '$scope', '$log', 'DataAccessFormResource', 'DataAccessFormService', 'AlertService', 'ServerErrorUtils',
    function ($rootScope, $scope, $log, DataAccessFormResource, DataAccessFormService, AlertService, ServerErrorUtils) {

     var saveForm = function() {

        if (DataAccessFormService.isFormValid($scope.dataAccessForm)) {
          $scope.dataAccessForm.definition = $scope.form.definition;
          $scope.dataAccessForm.schema = $scope.form.schema;
          DataAccessFormResource.save($scope.dataAccessForm,
            function () {
              AlertService.alert({
                id: 'DataAccessConfigController',
                type: 'success',
                msgKey: 'data-access-config.saved',
                delay: 5000
              });
            },
            function (response) {
              AlertService.alert({
                id: 'DataAccessConfigController',
                type: 'danger',
                msg: ServerErrorUtils.buildMessage(response)
              });
            });
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
            $scope.modelPreview = DataAccessFormService.prettifyJson($scope.form.model);
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

      DataAccessFormResource.get(
        function(dataAccessForm){
          $scope.dirty = true;
          $scope.form.definitionJson = dataAccessForm.definition ? JSON.parse(dataAccessForm.definition) : null;
          $scope.form.definition = DataAccessFormService.prettifyJson($scope.form.definitionJson);
          $scope.form.schemaJson = dataAccessForm.schema ? JSON.parse(dataAccessForm.schema) : null;
          $scope.form.schema = DataAccessFormService.prettifyJson($scope.form.schemaJson);
          $scope.dataAccessForm = dataAccessForm;
        },
        function(response) {
          AlertService.alert({
            id: 'DataAccessConfigController',
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

      $scope.dirty = false;
      $scope.dataAccessForm = {schema: '', definition: ''};
      $scope.selectedTab = 'form-definition';
      $scope.ace = DataAccessFormService.getEditorOptions(aceEditorOnLoadCallback);
      $scope.selectTab = selectTab;
      $scope.saveForm = saveForm;
      $scope.fullscreen = DataAccessFormService.gotoFullScreen;
      $scope.$watch('form.definition', watchFormDefinitionChanges);
      $scope.$watch('form.schema', watchFormSchemaChanges);
    }]);
