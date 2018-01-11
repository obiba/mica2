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

mica.entitySfConfig

  .controller('EntitySfConfigController', ['$rootScope',
    '$scope',
    'EntitySchemaFormService',
    'LocalizedSchemaFormService',
    'AlertService',
    'SfOptionsService',
    'FormDirtyStateObserver',
    function ($rootScope,
              $scope,
              EntitySchemaFormService,
              LocalizedSchemaFormService,
              AlertService,
              SfOptionsService,
              FormDirtyStateObserver) {

      EntitySchemaFormService.configureAcePaths();

      var refreshPreview = function(force) {
        try {
          if ($scope.dirtyObservable.isDirty() || force) {
            $scope.form.$promise.then(function() {
              $scope.form.schemaJson = LocalizedSchemaFormService.translate(JSON.parse($scope.form.schema));
              $scope.form.definitionJson = LocalizedSchemaFormService.translate(JSON.parse($scope.form.definition));
            });
          }
        } catch (e){
        }
      };

      SfOptionsService.transform().then(function(options){
        $scope.sfOptions = options;
      });

      var selectTab = function(id) {
        $scope.selectedTab = id;
        switch (id) {
          case 'form-preview':
            refreshPreview(false);
            break;
          case 'form-model':
            $scope.modelPreview = EntitySchemaFormService.prettifyJson($scope.form.model);
            break;
        }
      };

      var watchFormChanges = function(val){
        if (val) {
          refreshPreview(false);
          $scope.ace = EntitySchemaFormService.getEditorOptions(aceEditorOnLoadCallback, aceEditorOnChangeCallback);
        }
      };

      var aceEditorOnLoadCallback = function(editor) {
        if (editor.container.id === 'form-model') {
          editor.setReadOnly(true);
        }
      };

      var aceEditorOnChangeCallback = function(data) {
        var editor = data[1];
        if (editor.container.id === $scope.selectedTab && !editor.session.$modified) {
          $scope.dirtyObservable.setDirty(true);
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
      $scope.selectedTab = 'form-schema';
      $scope.$watch('form', watchFormChanges);
      $scope.selectTab = selectTab;
      $scope.testPreview = testPreview;
      $scope.fullscreen = EntitySchemaFormService.gotoFullScreen;

      FormDirtyStateObserver.observe($scope.dirtyObservable);

      $rootScope.$on('$translateChangeSuccess', function () {
        refreshPreview(true);
      });

	  refreshPreview(true);
    }]);
