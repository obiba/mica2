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

/* global obiba */

mica.entitySfConfig

  .controller('EntityTaxonomyConfigContentController', [
    '$scope',
    '$q',
    '$filter',
    'MicaConfigResource',
    'EntityTaxonomyConfigResource',
    'ServerErrorUtils',
    'AlertService',
    'FormDirtyStateObserver',
    'VocabularyAttributeService',
    function ($scope,
              $q,
              $filter,
              MicaConfigResource,
              EntityTaxonomyConfigResource,
              ServerErrorUtils,
              AlertService,
              FormDirtyStateObserver,
              VocabularyAttributeService) {

      var onError = function(response) {
        AlertService.alert({
          id: 'EntityTaxonomyConfigController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response)
        });
      };

      var navigateToVocabulary = function(content) {
        if ($scope.target.name === 'study') {
          $scope.model.possibleClassNames = ['Study', 'HarmonizationStudy'];
        } else if ($scope.target.name === 'dataset') {
          $scope.model.possibleClassNames = ['CollectedDataset', 'HarmonizedDataset'];
        }

        $scope.model.content = content;
        $scope.model.children = content.terms ? content.terms : [];
        $scope.model.type = 'criterion';
        $scope.model.forClassName = $scope.forClassName;
        $scope.model.aliases = VocabularyAttributeService.getAliases($scope.taxonomy.vocabularies, content);
      };

      var navigateToTaxonomy = function() {
        $scope.model.content = $scope.taxonomy;
        $scope.model.children = $scope.taxonomy.vocabularies;
        $scope.model.type = 'taxonomy';
      };

      var getTaxonomy = function() {
        EntityTaxonomyConfigResource.get({target: $scope.target.name}).$promise.then(
          function (response) {
            $scope.taxonomy = response;
            navigateToTaxonomy(response);
          },
          onError
        );
      };

      function swapVocabulary(from, to) {
        $scope.state.setDirty(true);
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
        return EntityTaxonomyConfigResource.save({target: $scope.target.name}, $scope.taxonomy);
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

      FormDirtyStateObserver.observe($scope.state.getDirtyObservable());
    }])

  .controller('entityTaxonomyConfigPropertiesViewController', [
    '$rootScope',
    '$scope',
    '$uibModal',
    '$filter',
    'VocabularyAttributeService',
    'NOTIFICATION_EVENTS',

    function ($rootScope,
              $scope,
              $uibModal,
              $filter,
              VocabularyAttributeService,
              NOTIFICATION_EVENTS) {

      var edit = function() {
          $uibModal.open({
            templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-properties-modal.html',
            controller: 'entityTaxonomyConfigPropertiesModalController',
            scope: $scope,
            resolve: {
              model: function() {
                return $scope.model;
              }
            }
          });
      };

      var listenerRegistry = new obiba.utils.EventListenerRegistry();

      function onDelete(event, criterion) {
        listenerRegistry.unregisterAll();
        var i = $scope.taxonomy.vocabularies.indexOf(criterion);
        if (i > -1) {
          $scope.state.setDirty(true);
          $scope.taxonomy.vocabularies.splice(i, 1);
          $scope.model.content = $scope.taxonomy;
          $scope.model.children = $scope.taxonomy.vocabularies;
          $scope.model.type = 'taxonomy';
        }
      }

      var deleteCriterion = function() {
        listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogRejected, function () {
          listenerRegistry.unregisterAll();
        }));
        listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onDelete));
        var criterionLabel = $filter('translate')('global.criterion');
        var criterion = $scope.model.content;
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'taxonomy-config.delete-dialog.title',
            titleArgs: [criterionLabel],
            messageKey: 'taxonomy-config.delete-dialog.message',
            messageArgs: [criterionLabel.toLowerCase(), criterion.name]
          }, $scope.model.content
        );
      };

      $scope.getField = VocabularyAttributeService.getField;
      $scope.getTypeMap = VocabularyAttributeService.getTypeMap;
      $scope.getRange = VocabularyAttributeService.getRange;
      $scope.getTermsSortKeyMap = VocabularyAttributeService.getTermsSortKeyMap;
      $scope.getHidden = VocabularyAttributeService.getHidden;
      $scope.getLocalized = VocabularyAttributeService.getLocalized;
      $scope.getFacet = VocabularyAttributeService.getFacet;
      $scope.getFacetPosition = VocabularyAttributeService.getFacetPosition;
      $scope.getFacetExpanded = VocabularyAttributeService.getFacetExpanded;
      $scope.getForClassName = VocabularyAttributeService.getForClassName;
      $scope.isStatic = VocabularyAttributeService.isStatic;
      $scope.deleteCriterion = deleteCriterion;
      $scope.edit = edit;

    }])

  .controller('entityTaxonomyConfigCriteriaViewController', [
    '$scope',
    '$uibModal',
    'EntityTaxonomyService',
    'VocabularyAttributeService',
    function ($scope,
              $uibModal,
              EntityTaxonomyService,
              VocabularyAttributeService) {

      var addCriteria = function() {

        var vocabularies = $scope.taxonomy.vocabularies || [];
        var model = {
          content: null,
          children: null,
          siblings: vocabularies,
          forClassName: $scope.forClassName,
          type: 'criterion',
          aliases: VocabularyAttributeService.getAliases(vocabularies, null)
        };

        if ($scope.target.name === 'study') {
          model.possibleClassNames = ['Study', 'HarmonizationStudy'];
        } else if ($scope.target.name === 'dataset') {
          model.possibleClassNames = ['CollectedDataset', 'HarmonizedDataset'];
        }

        $uibModal.open({
          templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-properties-modal.html',
          controller: 'entityTaxonomyConfigPropertiesModalController',
          scope: $scope,
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

  .controller('entityTaxonomyConfigTermsViewController', [
    '$rootScope',
    '$scope',
    '$filter',
    '$uibModal',
    'EntityTaxonomyService',
    'VocabularyAttributeService',
    'NOTIFICATION_EVENTS',
    function ($rootScope,
              $scope,
              $filter,
              $uibModal,
              EntityTaxonomyService,
              VocabularyAttributeService,
              NOTIFICATION_EVENTS) {

      var addTerm = function(vocabulary, term) {
        var model = {
          content: term || null,
          children: null,
          vocabulary: vocabulary,
          forClassName: VocabularyAttributeService.getForClassName(vocabulary),
          siblings: (vocabulary.terms || []).filter(function(t){
            return term ? term.name !== t.name : t;
          }),
          type: 'term',
          valueType: VocabularyAttributeService.getType(vocabulary)
        };

        $uibModal.open({
          templateUrl: 'app/entity-taxonomy-config/views/entity-taxonomy-config-properties-modal.html',
          controller: 'entityTaxonomyConfigPropertiesModalController',
          scope: $scope,
          resolve: {
            model: function() {
              return model;
            }
          }
        }).result.then(function(){
          $scope.model.children = vocabulary.terms = vocabulary.terms || [];
          if (!term) {
            vocabulary.terms.push(model.content);
          }
        });
      };

      var listenerRegistry = new obiba.utils.EventListenerRegistry();

      function onDelete(event, args) {
        listenerRegistry.unregisterAll();
        var i = args.vocabulary.terms.indexOf(args.term);
        if (i > -1) {
          $scope.state.setDirty(true);
          args.vocabulary.terms.splice(i, 1);
        }
      }

      var deleteTerm = function(vocabulary, term) {
        listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogRejected, function () {
          listenerRegistry.unregisterAll();
        }));
        listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onDelete));
        var termLabel = $filter('translate')('global.term');
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'taxonomy-config.delete-dialog.title',
            titleArgs: [termLabel],
            messageKey: 'taxonomy-config.delete-dialog.message',
            messageArgs: [termLabel.toLowerCase(), term.name]
          }, {vocabulary: vocabulary, term: term}
        );
      };

      $scope.addTerm = addTerm;
      $scope.deleteTerm = deleteTerm;
      $scope.isStatic = VocabularyAttributeService.isStatic;
      $scope.getTermsCount = EntityTaxonomyService.getTermsCount;
    }])

  .controller('entityTaxonomyConfigPropertiesModalController', [
    '$scope',
    '$uibModalInstance',
    '$filter',
    'EntityTaxonomySchemaFormService',
    'VocabularyAttributeService',
    'MicaConfigResource',
    'EntitySchemaFormFieldsService',
    'model',
    function ($scope,
              $uibModalInstance,
              $filter,
              EntityTaxonomySchemaFormService,
              VocabularyAttributeService,
              MicaConfigResource,
              EntitySchemaFormFieldsService,
              model) {

      $scope.model = model; //
      var apply = function () {
        $scope.$broadcast('schemaFormValidate');

        if($scope.form.$valid) {
          EntityTaxonomySchemaFormService.updateModel($scope.sfForm, $scope.model);
          $scope.state.setDirty($scope.model.content !== null && $scope.form.$dirty);
          $uibModalInstance.close($scope.sfForm.model);
        }

        $scope.form.saveAttempted = true;
      };

      var cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

      var getTitle = function() {
        var key = 'taxonomy-config.' + $scope.model.type + '-dialog.' + ($scope.model.content ? 'edit-' : 'add-');
        switch ($scope.model.type) {
          case 'taxonomy':
            key = 'edit';
            break;
          case 'criterion':
            key += 'criterion';
            break;
          case 'term':
            key += 'term';
            break;
        }

        return $filter('translate')(key);
      };

      MicaConfigResource.get(function (micaConfig) {
        $scope.sfOptions = {
          validationMessage: {
            '302': $filter('translate')('required')
          },
          formDefaults: {
            languages: {}
          }
        };

        micaConfig.languages.forEach(function (lang) {
          $scope.sfOptions.formDefaults.languages[lang] = $filter('translate')('language.' + lang);
        });

        if ('criterion' === $scope.model.type) {
          $scope.sfOptions.validationMessage['duplicate-criterion-alias'] =
            $filter('translate')('taxonomy-config.criterion-dialog.error.duplicate-criterion-alias');

          $scope.sfOptions.validators = {
            'duplicate-criterion-alias': function (value) {
              if (value) {
                return EntityTaxonomySchemaFormService.validateModel($scope.sfForm, $scope.model);
              }
              return true;
            }
          };

          $scope.model.onRangeChange = function() {
            if (!EntityTaxonomySchemaFormService.validateModel($scope.sfForm, $scope.model)) {
              $scope.$broadcast('schemaForm.error.field', 'duplicate-criterion-alias', false);
            } else {
              $scope.$broadcast('schemaForm.error.field', 'duplicate-criterion-alias', true);
            }
          };

          if ($scope.schemas) {
            $scope.sfOptions.formDefaults.values = EntitySchemaFormFieldsService.getFieldNames(
              EntitySchemaFormFieldsService.concatenateSchemas($scope.schemas)
            );
          }
        }

        $scope.sfForm = EntityTaxonomySchemaFormService.getFormData($scope.model);
        $scope.isStatic = VocabularyAttributeService.isStatic;
        $scope.apply = apply;
        $scope.cancel = cancel;
        $scope.getTitle = getTitle;
      });

    }]);
