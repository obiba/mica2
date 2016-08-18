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

  .controller('EntityTaxonomyConfigController', [
    '$scope',
    '$q',
    '$filter',
    'MicaConfigResource',
    'EntityTaxonomyConfigResource',
    'ServerErrorUtils',
    'AlertService',
    function ($scope,
              $q,
              $filter,
              MicaConfigResource,
              EntityTaxonomyConfigResource,
              ServerErrorUtils,
              AlertService) {

      var onError = function(response) {
        AlertService.alert({
          id: 'EntityTaxonomyConfigController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response)
        });
      };

      var navigateToVocabulary = function(content) {
        $scope.model.content = content;
        $scope.model.children = content.terms ? content.terms : [];
        $scope.model.type = 'vocabulary';
      };

      var navigateToTaxonomy = function() {
        $scope.model.content = $scope.taxonomy;
        $scope.model.children = $scope.taxonomy.vocabularies;
        $scope.model.type = 'taxonomy';
        console.log($scope.model);
      };

      var getTaxonomy = function() {
        EntityTaxonomyConfigResource.get({target: $scope.target}).$promise.then(
          function (response) {
            $scope.taxonomy = response;
            navigateToTaxonomy(response);
          },
          onError
        );
      };

      function swapVocabulary(from, to) {
        var tmp = $scope.taxonomy.vocabularies[from];
        $scope.taxonomy.vocabularies[from] = $scope.taxonomy.vocabularies[to];
        $scope.taxonomy.vocabularies[to] = tmp;
      }

      var moveVocabularyUp = function(vocabulary) {
        var from = $scope.taxonomy.vocabularies.indexOf(vocabulary);
        var to = from - 1;
        swapVocabulary(from, to < 0 ? $scope.taxonomy.vocabularies.length - 1 : to);
      };

      var moveVocabularyDown = function(vocabulary) {
        var from = $scope.taxonomy.vocabularies.indexOf(vocabulary);
        var to = from + 1;
        swapVocabulary(from, to > $scope.taxonomy.vocabularies.length ? 1 : to);
      };

      var onSave = function() {
        return EntityTaxonomyConfigResource.save({target: $scope.target}, $scope.taxonomy);
      };

      $scope.model = {
        content: null,
        children: null,
        type: 'taxonomy'
      };

      $scope.onSave = onSave;
      $scope.navigateToVocabulary = navigateToVocabulary;
      $scope.navigateToTaxonomy = navigateToTaxonomy;
      $scope.moveVocabularyUp = moveVocabularyUp;
      $scope.moveVocabularyDown = moveVocabularyDown;

      $scope.state.registerListener($scope);

      getTaxonomy();

    }])

  .controller('entityTaxonomyConfigPropertiesViewController', [
    '$scope',
    'VocabularyAttributeService',
    '$uibModal',

    function ($scope,
              VocabularyAttributeService,
              $uibModal) {

      var edit = function() {

          $uibModal.open({
            templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-properties-modal.html',
            controller: 'entityTaxonomyConfigPropertiesModalController',
            resolve: {
              model: function() {
                return $scope.model;
              }
            }
          });
      };

      var deleteCriterion = function() {

        // TODO add confirmation dialog
        var i = $scope.taxonomy.vocabularies.indexOf($scope.model.content);
        if (i > -1) {
          $scope.taxonomy.vocabularies.splice(i, 1);
          // TODO add moveTo
          $scope.model.content = $scope.taxonomy;
          $scope.model.children = $scope.taxonomy.vocabularies;
          $scope.model.type = 'taxonomy';
        }
      };

      $scope.getField = VocabularyAttributeService.getField;
      $scope.getTypeMap = VocabularyAttributeService.getTypeMap;
      $scope.getLocalized = VocabularyAttributeService.getLocalized;
      $scope.deleteCriterion = deleteCriterion;
      $scope.edit = edit;

    }])

  .controller('entityTaxonomyConfigCriteriaViewController', [
    '$scope',
    '$uibModal',
    'EntityTaxonomyService',
    'VocabularyAttributeService',
    function ($scope, $uibModal, EntityTaxonomyService, VocabularyAttributeService) {

      var addCriteria = function() {
        var model = {
          content: null,
          children: null,
          type: 'vocabulary'
        };

        $uibModal.open({
          templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-properties-modal.html',
          controller: 'entityTaxonomyConfigPropertiesModalController',
          resolve: {
            model: function() {
              return model;
            }
          }
        }).result.then(function(){
          $scope.taxonomy.vocabularies = $scope.taxonomy.vocabularies || [];
          $scope.taxonomy.vocabularies.push(model.content);
        });
      };

      $scope.add = addCriteria;
      $scope.getField = VocabularyAttributeService.getField;
      $scope.getTermsCount = EntityTaxonomyService.getTermsCount;

    }])

  .controller('entityTaxonomyConfigPropertiesModalController', [
    '$scope',
    '$uibModalInstance',
    '$filter',
    'EntityTaxonomySchemaFormService',
    'MicaConfigResource',
    'model',
    function ($scope,
              $uibModalInstance,
              $filter,
              EntityTaxonomySchemaFormService,
              MicaConfigResource,
              model) {

      $scope.model = model;
      var apply = function () {
        $scope.$broadcast('schemaFormValidate');

        if($scope.form.$valid) {
          EntityTaxonomySchemaFormService.updateModel($scope.sfForm, $scope.model);
          $uibModalInstance.close($scope.sfForm.model);
        }

        $scope.form.saveAttempted = true;
      };

      var cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

      MicaConfigResource.get(function (micaConfig) {
        $scope.sfOptions = {
          formDefaults: {
            languages: {}
          }
        };

        micaConfig.languages.forEach(function (lang) {
          $scope.sfOptions.formDefaults.languages[lang] = $filter('translate')('language.' + lang);
        });

        $scope.sfForm = EntityTaxonomySchemaFormService.getFormData($scope.model);
        $scope.apply = apply;
        $scope.cancel = cancel;
      });

    }])


  .controller('entityTaxonomyConfigTermsViewController', [
    '$scope',
    function ($scope) {
    }]);

