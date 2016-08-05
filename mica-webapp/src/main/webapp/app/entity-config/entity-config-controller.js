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

  .controller('EntityConfigController', ['$scope', '$q', '$location', '$routeParams', 'AlertService', 'ServerErrorUtils', 'EntitySchemaFormService', 'EntityFormResource',
    function ($scope, $q, $location, $routeParams, AlertService, ServerErrorUtils, EntitySchemaFormService, EntityFormResource) {
      var FORMS = {'network': ['network'],
        'study': ['study', 'population', 'data-collection-event'],
        'dataset': ['dataset']};

      $scope.target = $routeParams.type.match(/(\w+)\-config/)[1];
      $scope.tab = {name: 'form'};
      $scope.forms = [];


      $scope.forms = FORMS[$scope.target].map(function(name) {
        return {name: name, form: EntityFormResource.get({target: name},
          function(entityForm){
            return entityForm;
          },
          function(response) {
            AlertService.alert({
              id: 'EntityConfigController',
              type: 'danger',
              msg: ServerErrorUtils.buildMessage(response)
            });
          })};
      }).map(function(form) {
        var definitionJson = EntitySchemaFormService.parseJsonSafely(form.form.definition, []);
        form.form.definition = EntitySchemaFormService.prettifyJson(definitionJson);
        var schemaJson = EntitySchemaFormService.parseJsonSafely(form.form.schema, {});
        form.form.schema = EntitySchemaFormService.prettifyJson(schemaJson);
        return form;
      });

      $scope.forms[0].active = true;
      
      $scope.setActive = function(form) {
        $scope.forms.forEach(function(f) {
          f.active = f.name === form.name;
        });
      };

      $scope.saveForm = function() {
        var res = $scope.forms.map(function(form) {
          var defer = $q.defer();
          switch (EntitySchemaFormService.isFormValid(form.form)) {
            case EntitySchemaFormService.ParseResult.VALID:
              EntityFormResource.save({target: form.name}, form.form, function() {
                defer.resolve();
              }, function(response) {
                defer.reject(ServerErrorUtils.buildMessage(response));
              });
              break;
            case EntitySchemaFormService.ParseResult.SCHEMA:
              defer.reject('entity-config.syntax-error.schema'); 
              break;
            case EntitySchemaFormService.ParseResult.DEFINITION:
              defer.reject('entity-config.syntax-error.definition'); 
          }
          
          return defer.promise;
        });

        $q.all(res).then(function (res) {
          $location.path('/admin').replace();
          return res;
        }).catch(function (res) {
          AlertService.alert({
            id: 'EntityConfigController',
            type: 'danger',
            msg: res
          });
        });
      };
    }]);
