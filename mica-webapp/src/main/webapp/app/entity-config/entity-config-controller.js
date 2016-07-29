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

mica.entityConfig

  .controller('EntityConfigController', ['$scope', '$location', '$routeParams', 'AlertService', 'ServerErrorUtils', 'EntitySchemaFormService', 'EntityFormResource',
    function ($scope, $location, $routeParams, AlertService, ServerErrorUtils, EntitySchemaFormService, EntityFormResource) {
      $scope.target = $routeParams.type.match(/(\w+)\-config/)[1];
      $scope.tab = {name: 'form'};
      $scope.entityForm = {schema: '', definition: ''};

      EntityFormResource.get({target: $scope.target},
        function(entityForm){
          $scope.form.definitionJson = EntitySchemaFormService.parseJsonSafely(entityForm.definition, []);
          $scope.form.definition = EntitySchemaFormService.prettifyJson($scope.form.definitionJson);
          $scope.form.schemaJson = EntitySchemaFormService.parseJsonSafely(entityForm.schema, {});
          $scope.form.schema = EntitySchemaFormService.prettifyJson($scope.form.schemaJson);
          $scope.entityForm = entityForm;

          if ($scope.form.definitionJson.length === 0) {
            AlertService.alert({
              id: 'EntityConfigController',
              type: 'danger',
              msgKey: $scope.target + '-config.parse-error.definition'
            });
          }

          if (Object.getOwnPropertyNames($scope.form.schemaJson).length === 0) {
            AlertService.alert({
              id: 'EntityConfigController',
              type: 'danger',
              msgKey: $scope.target + '-config.parse-error.schema'
            });
          }
        },
        function(response) {
          AlertService.alert({
            id: 'EntityConfigController',
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

      $scope.saveForm = function() {
        switch (EntitySchemaFormService.isFormValid($scope.form)) {
          case EntitySchemaFormService.ParseResult.VALID:
            $scope.entityForm.definition = $scope.form.definition;
            $scope.entityForm.schema = $scope.form.schema;
            EntityFormResource.save({target: $scope.target}, $scope.entityForm,
              function () {
                $location.path('/admin').replace();
              },
              function (response) {
                AlertService.alert({
                  id: 'EntityConfigController',
                  type: 'danger',
                  msg: ServerErrorUtils.buildMessage(response)
                });
              });
            break;
          case EntitySchemaFormService.ParseResult.SCHEMA:
            AlertService.alert({
              id: 'EntityConfigController',
              type: 'danger',
              msgKey: 'entity-config.syntax-error.schema'
            });
            break;
          case EntitySchemaFormService.ParseResult.DEFINITION:
            AlertService.alert({
              id: 'EntityConfigController',
              type: 'danger',
              msgKey: 'entity-config.syntax-error.definition'
            });
            break;
        }
      };
    }]);
