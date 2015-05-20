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

      var selectTab = function(id) {
        $scope.selectedTab = id;
      };

      var watchFormSchemaChanges = function(val,old){
        if (val && val !== old) {
          try {
            $scope.form.schemaJson = JSON.parse(val);
            $scope.dataAccessForm.schema = val;
          } catch (e){
          }
        }
      };

      var watchFormDefinitionChanges = function(val,old){
        if (val && val !== old) {
          try {
            $scope.form.definitionJson = JSON.parse(val);
            $scope.dataAccessForm.definition = val;
          } catch (e){
          }
        }
      };

      $scope.dataAccessForm = DataAccessFormResource.get(
        function(dataAccessForm){
          $scope.form.definitionJson = dataAccessForm.definition ? JSON.parse(dataAccessForm.definition) : null;
          $scope.form.definition = JSON.stringify($scope.form.definitionJson,undefined,2);
          $scope.form.schemaJson = dataAccessForm.schema ? JSON.parse(dataAccessForm.schema) : null;
          $scope.form.schema = JSON.stringify($scope.form.schemaJson,undefined,2);
        },
        function(response) {
          AlertService.alert({
            id: 'DataAccessConfigController',
            type: 'danger',
            msg: ServerErrorUtils.buildMessage(response)
          });
        }) || {definition: null, schema: null};

      $scope.form = {
        definitionJson: null,
        definition: null,
        schemaJson: null,
        schema: null
      };

      $scope.dataAccessForm = {schema: '', definition: ''};
      $scope.modelData = {};
      $scope.selectedTab = 'form-definition';
      $scope.ace = DataAccessFormService.getEditorOptions();
      $scope.selectTab = selectTab;
      $scope.saveForm = saveForm;
      $scope.fullscreen = DataAccessFormService.gotoFullScreen;
      $scope.$watch('form.definition', watchFormDefinitionChanges);
      $scope.$watch('form.schema', watchFormSchemaChanges);
    }]);
