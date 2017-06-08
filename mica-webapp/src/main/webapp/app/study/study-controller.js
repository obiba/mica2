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

mica.study

  .constant('STUDY_EVENTS', {
    studyUpdated: 'event:study-updated'
  })

  .controller('StudyMainController', ['$scope', '$location', 'StudyStatesResource',
    function($scope, $location, StudyStatesResource) {
      if($scope.micaConfig.isSingleStudyEnabled) {
        $scope.studies = StudyStatesResource.query({}, function(res) {
          if(res.length) {
            $location.path('/study/' + res[0].id);
            $location.replace();
          }
        });
      }
  }])

  .controller('StudyListController', [
    '$scope', '$timeout', 'StudyStatesResource', 'DraftStudyDeleteService', 'AlertBuilder', mica.commons.ListController
  ])

  .controller('StudyViewController', [
    '$scope',
    '$rootScope',
    '$location',
    '$routeParams',
    '$translate',
    '$uibModal',
    '$timeout',
    '$filter',
    '$q',
    '$log',
    'NOTIFICATION_EVENTS',
    'CONTACT_EVENTS',
    'EntityFormResource',
    'LocalizedSchemaFormService',
    'MicaConfigResource',
    'DocumentPermissionsService',
    'StudyStateResource',
    'DraftFileSystemSearchResource',
    'DraftStudyResource',
    'DraftStudyDeleteService',
    'DraftStudyStatusResource',
    'DraftStudyPublicationResource',
    'DraftStudyRevisionsResource',
    'DraftStudyViewRevisionResource',
    'DraftStudyRestoreRevisionResource',
    'StudyUpdateWarningService',
    'EntityPathBuilder',
    mica.study.ViewController
  ])

  .controller('StudyPermissionsController', [
    '$scope', '$routeParams', 'DraftStudyPermissionsResource', 'DraftStudyAccessesResource', mica.commons.PermissionsController
  ])

  .controller('StudyPopulationDceModalController', [
    '$scope',
    '$uibModalInstance',
    '$locale',
    '$location',
    '$translate',
    'lang',
    'dce',
    'sfOptions',
    'sfForm',
    'study',
    'path',
    'StudyTaxonomyService',
    'moment',
    function ($scope,
              $uibModalInstance,
              $locale,
              $location,
              $translate,
              lang,
              dce,
              sfOptions,
              sfForm,
              study,
              path,
              StudyTaxonomyService,
              moment) {
      $scope.months = moment.months();
      $scope.selectedLocale = lang;
      $scope.dce = dce;
      $scope.study = study;
      $scope.path = path;
      $scope.sfOptions = sfOptions;
      $scope.dceSfForm = sfForm;

      $scope.close = function () {
        $uibModalInstance.close();
      };

      $scope.viewFiles = function () {
        $uibModalInstance.close();
        $location.path(path.root).search({p: path.entity});
      };

      $scope.getTermLabels = function(vocabularyName, terms) {
        if (!terms) {
          return [];
        }

        var result = terms.map(function(term){
          return StudyTaxonomyService.getLabel(vocabularyName, term, $translate.use());
        });
        return result.join(', ');
      };
    }])

  .controller('StudyPopulationController', [
    '$scope',
    '$rootScope',
    '$routeParams',
    '$location',
    '$filter',
    '$translate',
    '$q',
    '$log',
    'MicaConfigResource',
    'SfOptionsService',
    'EntityFormResource',
    'DraftStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    'MicaUtil',
    mica.study.PopulationEditController
  ])

  .controller('StudyPopulationDceController', [
    '$scope',
    '$rootScope',
    '$routeParams',
    '$location',
    '$filter',
    '$translate',
    '$q',
    '$log',
    'MicaConfigResource',
    'SfOptionsService',
    'EntityFormResource',
    'DraftStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    'MicaUtil',
    mica.study.DataCollectionEventEditController
  ])

  // .controller('StudyPopulationDceController', [
  //   '$rootScope',
  //   '$scope',
  //   '$routeParams',
  //   '$location',
  //   '$log',
  //   '$filter',
  //   '$translate',
  //   'DraftStudyResource',
  //   'EntityFormResource',
  //   'LocalizedSchemaFormService',
  //   'MicaConfigResource',
  //   'FormServerValidation',
  //   'MicaUtil',
  //   'StudyTaxonomyService',
  //   'SfOptionsService',
  //   'StudyUpdateWarningService',
  //   'FormDirtyStateObserver',
  //   function ($rootScope,
  //             $scope,
  //             $routeParams,
  //             $location,
  //             $log,
  //             $filter,
  //             $translate,
  //             DraftStudyResource,
  //             EntityFormResource,
  //             LocalizedSchemaFormService,
  //             MicaConfigResource,
  //             FormServerValidation,
  //             MicaUtil,
  //             StudyTaxonomyService,
  //             SfOptionsService,
  //             StudyUpdateWarningService,
  //             FormDirtyStateObserver
  //   ) {
  //     $scope.dce = {model: {}};
  //     $scope.revision = {comment: null};
  //     $scope.fileTypes = '.doc, .docx, .odm, .odt, .gdoc, .pdf, .txt  .xml  .xls, .xlsx, .ppt';
  //     $scope.defaultMinYear = 1900;
  //     $scope.defaultMaxYear = new Date().getFullYear() + 200;
  //     $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function (study) {
  //
  //       if ($routeParams.pid) {
  //         $scope.population = study.populations.filter(function (p) {
  //           return p.model._id === $routeParams.pid;
  //         })[0];
  //
  //         $scope.newDCE = !$routeParams.dceId;
  //
  //         if ($routeParams.dceId) {
  //           $scope.dce = $scope.population.dataCollectionEvents.filter(function (d) {
  //             return d.model._id === $routeParams.dceId;
  //           })[0];
  //         } else {
  //           var sourceDceId = $location.search().sourceDceId;
  //
  //           if ($scope.population.dataCollectionEvents === undefined) {
  //             $scope.population.dataCollectionEvents = [];
  //           }
  //
  //           var dceIds = $scope.population.dataCollectionEvents.map(function (dce) {
  //             return dce.model._id;
  //           });
  //
  //           if (sourceDceId) {
  //             var sourceDce = $scope.population.dataCollectionEvents.filter(function (dce) {
  //               return dce.model._id === sourceDceId;
  //             })[0];
  //
  //             if (sourceDce) {
  //               angular.copy(sourceDce, $scope.dce);
  //               $scope.dce.model._id = MicaUtil.generateNextId(dceIds);
  //               delete $scope.dce.attachments;
  //               delete $scope.dce.startYear;
  //               delete $scope.dce.startMonth;
  //               delete $scope.dce.endYear;
  //               delete $scope.dce.endMonth;
  //             }
  //           } else if (dceIds.length) {
  //             $scope.dce.model._id = MicaUtil.generateNextId(dceIds);
  //           }
  //
  //           $scope.population.dataCollectionEvents.push($scope.dce);
  //         }
  //         $scope.attachments =
  //           $scope.dce.attachments && $scope.dce.attachments.length > 0 ? $scope.dce.attachments : [];
  //
  //         StudyTaxonomyService.get(function() {
  //           var lang = $translate.use();
  //           $scope.dataSources = StudyTaxonomyService.getTerms('populations-dataCollectionEvents-dataSources', lang);
  //           $scope.bioSamples = StudyTaxonomyService.getTerms('populations-dataCollectionEvents-bioSamples', lang);
  //           $scope.administrativeDatabases = StudyTaxonomyService.getTerms('populations-dataCollectionEvents-administrativeDatabases', lang);
  //         });
  //
  //       } else {
  //         // TODO add error popup
  //         $log.error('Failed to retrieve population.');
  //       }
  //     }) : {};
  //
  //     function initializeForm() {
  //       MicaConfigResource.get(function (micaConfig) {
  //         var sfLanguages = {};
  //         micaConfig.languages.forEach(function (lang) {
  //           sfLanguages[lang] = $filter('translate')('language.' + lang);
  //         });
  //
  //         SfOptionsService.transform().then(function(options) {
  //           $scope.sfOptions = options;
  //           $scope.sfOptions.formDefaults = { languages: sfLanguages };
  //         });
  //
  //         EntityFormResource.get({target: 'study'}, function (form) {
  //           $scope.sfForm = form;
  //         });
  //
  //         EntityFormResource.get({target: 'data-collection-event', locale: $translate.use()}, function (form) {
  //           $scope.dceSfForm = form;
  //         });
  //
  //       });
  //     }
  //
  //     $rootScope.$on('$translateChangeSuccess', function () {
  //       initializeForm();
  //     });
  //
  //     initializeForm();
  //
  //     $scope.cancel = function () {
  //       redirectToStudy();
  //     };
  //
  //     $scope.save = function (form) {
  //       $scope.dce.attachments = $scope.attachments === undefined || $scope.attachments.length > 0 ? $scope.attachments : null;
  //
  //       if (!$scope.dce.attachments) { //protobuf doesnt like null values
  //         delete $scope.dce.attachments;
  //       }
  //
  //       $scope.$broadcast('schemaFormValidate');
  //
  //       if (!validate(form)) {
  //         form.saveAttempted = true;
  //         return;
  //       }
  //
  //       updateStudy();
  //     };
  //
  //     var validate = function (form) {
  //       if ($scope.population.dataCollectionEvents.filter(function (d) {
  //           return d.model._id === $scope.dce.model._id;
  //         }).length > 1) {
  //         form.$setValidity('dce_id', false);
  //       } else {
  //         form.$setValidity('dce_id', true);
  //       }
  //
  //       return form.$valid;
  //     };
  //
  //     var updateStudy = function () {
  //       $log.info('Update study', $scope.study);
  //       $scope.study.$save({comment: $scope.revision.comment},
  //         function onSuccess(response) {
  //           FormDirtyStateObserver.unobserve();
  //           redirectToStudy(response);
  //         },
  //         saveErrorHandler);
  //     };
  //
  //     var saveErrorHandler = function (response) {
  //       if (response.status === 409) {
  //         StudyUpdateWarningService.popup(response.data, 'study.population-or-dce-delete-conflict', 'study.population-or-dce-delete-conflict-message');
  //       } else {
  //         FormServerValidation.error(response, $scope.form, $scope.languages);
  //       }
  //     };
  //
  //     var redirectToStudy = function (response) {
  //       $location.search('sourceDceId', null);
  //       $location.path('/study/' + (response ? response.study.id : $scope.study.id)).replace();
  //     };
  //
  //     FormDirtyStateObserver.observe($scope);
  //   }])
  //
  // .controller('StudyFileSystemController', ['$scope', '$log', '$routeParams',
  //   function ($scope, $log, $routeParams) {
  //     $scope.documentInfo = {type: 'study', id: $routeParams.id};
  //     $log.debug($scope.documentInfo);
  //   }
  // ])

  .controller('StudyEditController', [
    '$scope',
    '$rootScope',
    '$routeParams',
    '$location',
    '$filter',
    '$translate',
    '$q',
    '$log',
    'MicaConfigResource',
    'SfOptionsService',
    'EntityFormResource',
    'DraftStudiesResource',
    'DraftStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    mica.study.EditController
  ]);
