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

mica.study

  .constant('STUDY_EVENTS', {
    studyUpdated: 'event:study-updated'
  })

  .controller('StudyMainController', ['$scope', '$location', 'StudyStatesResource',
    function($scope, $location, StudyStatesResource) {
      $scope.path = '#/individual-study';

      if($scope.micaConfig.isSingleStudyEnabled) {
        $scope.studies = StudyStatesResource.query({}, function(res) {
          if(res.length) {
            $location.path('/individual-study/' + res[0].id);
            $location.replace();
          }
        });
      }
  }])

  .controller('StudyListController', [
    '$scope', '$timeout', 'StudyStatesResource', 'DraftStudyDeleteService', 'AlertBuilder', mica.commons.ListController
  ])

  .controller('HarmonizationStudyListController', [
    '$scope', '$timeout', 'HarmonizationStudyStatesResource', 'DraftHarmonizationStudyDeleteService', 'AlertBuilder', mica.commons.ListController
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
    'StudyStatesResource',
    'DraftFileSystemSearchResource',
    'DraftStudyResource',
    'DraftStudyDeleteService',
    'DraftStudyRevisionsResource',
    'StudyUpdateWarningService',
    'EntityPathBuilder',
    mica.study.ViewController
  ])

  .controller('HarmonizationStudyViewController', [
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
    'HarmonizationStudyStatesResource',
    'DraftFileSystemSearchResource',
    'DraftHarmonizationStudyResource',
    'DraftHarmonizationStudyDeleteService',
    'DraftHarmonizationStudyRevisionsResource',
    'StudyUpdateWarningService',
    mica.study.HarmonizationStudyViewController
  ])

  .controller('StudyPermissionsController', [
    '$scope', '$routeParams', 'DraftStudyPermissionsResource', 'DraftStudyAccessesResource', mica.commons.PermissionsController
  ])

  .controller('HarmonizationStudyPermissionsController', [
    '$scope', '$routeParams', 'DraftHarmonizationStudyPermissionsResource', 'DraftHarmonizationStudyAccessesResource', mica.commons.PermissionsController
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

  .controller('HarmonizationStudyPopulationController', [
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
    'DraftHarmonizationStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    'MicaUtil',
    mica.study.HarmonizationPopulationEditController
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
  ])

  .controller('StudyImportController', [
    '$scope',
    '$http', 
    function ($scope, $http) {

      $scope.displayBodyIndexOne = true;
      $scope.displayBodyIndexTwo = false;
      $scope.displayBodyIndexTwo = false;
      
      var listIds = [];

      //NEXT
      $scope.next = function(importModel, path, isDisplayedIndexOne) {
        console.log('[NEXT] ' + String(isDisplayedIndexOne) );

        if (isDisplayedIndexOne) {
        	
            importModel.type = path.indexOf('harmonization') > -1 ? 'harmonization-study' : 'individual-study';
            $scope.currentPage = 1;
            $scope.pageSize = 7;
            $('body').css('cursor', 'progress');
            
            $http({
                url: 'ws/draft/studies/import/_preview',
                method: 'GET',
                params: {url: importModel.url, username: importModel.username, 
                         password: importModel.password, type: importModel.type}
            
              }).then(function(response) {
                console.log('[NEXT(response)]');
              	
                $('#myModalDialog').addClass('modal-lg');
                $('#myPreviousButton').prop('disabled', false);
                $('body').css('cursor', 'default');

                $scope.studiesToImport = JSON.parse(response.data);
                $scope.displayBodyIndexOne = false;
              	$scope.displayBodyIndexTwo = true;
              	$scope.displayBodyIndexThree = false;
              });
            
        } else {
        	//will display Index Three
        	$scope.studiesToInclude = [];
        	$scope.studiesToUpdate = [];
        	
        	for (var i = 0; i < $scope.studiesToImport.length; i++) {	
                if ($scope.studiesToImport[i].checked) {
                	$scope.studiesToInclude.push($scope.studiesToImport[i]);
                	$scope.studiesToUpdate.push($scope.studiesToImport[i]);
                }
            }
        	
        	$scope.displayBodyIndexOne = false;
            $scope.displayBodyIndexTwo = false;
            $scope.displayBodyIndexThree = true;
        }
        
      };

      //PREVIOUS
      $scope.previous = function(isDisplayedIndexTwo) {
        console.log('[PREVIOUS] ' + String(isDisplayedIndexTwo) );

        if (isDisplayedIndexTwo) {
        	$scope.displayBodyIndexOne = true;
            $scope.displayBodyIndexTwo = false;
            $scope.displayBodyIndexThree = false;
            $('#myModalDialog').removeClass('modal-lg');
            $('#myPreviousButton').prop('disabled', true);
            $('#myNextButton').prop('disabled', false);
            $('#myFinishButton').prop('disabled', true);	
        } else {
        	$scope.displayBodyIndexOne = false;
            $scope.displayBodyIndexTwo = true;
            $scope.displayBodyIndexThree = false;
            $('#myFinishButton').prop('disabled', true);
        }

      };

      //CLICK_CHECK_BOX
      $scope.clickCheckbox = function(studyModal) {
    	  
        console.log('[CLICK_CHECKBOX]');

        for (var i = 0; i < $scope.studiesToImport.length; i++) {	
            if ($scope.studiesToImport[i].checked) {
            	$scope.studiesToImport.checked = true;
            	return;
            }
        }
        
        $scope.studiesToImport.checked = false;
      };

      //FINISH
      $scope.finish = function(importModel, studiesToImport) {
        console.log('[FINISH]');
        
        
        for (var i = 0; i < $scope.studiesToImport.length; i++) {
        	
            if ($scope.studiesToImport[i].checked){
            	
            	listIds.push( $scope.studiesToImport[i].id );
            }
        }
        
        /*
        $('#myTable input:checkbox:checked').each(function(i) {
        	
          var array = $(this).parent().siblings().map(function() {
            return $(this).text().trim();
          }).get();

          listIds.push( JSON.stringify(array[0]) );
        });
        */

        console.log('listIds = ' + listIds);

        //var endpoint = importModel.type.indexOf('harmonization') > -1 ? '/harmonization-studies/_import' : '/individual-studies/_import';

        //listIds = [];
        
        //TODO:  
        //1) Obter o estudo remoto completo (GET); 
        //2) Incluir o estudo localmente (POST); (o codigo abaixo esta incompleto)
        
        /*$http({
          url: 'ws/draft' + endpoint,
          method: 'POST',
          params: {url: importModel.url, 
                   username: importModel.username, 
                   password: importModel.password, 
                   ids: ids}

        }).then(function(response) {
          console.log('[FINISH(response)]');
          console.log(JSON.parse(response.data));
        });*/
      };
      
      //PAGE_CHANGED
      /*$scope.pageChanged = function (page, oldPage) {
    	  
    	  $('#myTable input:checkbox:checked').each(function(i) {
    		  var array = $(this).parent().siblings().map(function() {
    			  return $(this).text().trim();
    		  }).get();

    		  listIds.push( JSON.stringify(array[0]) );
    	  });
    	
    	  console.log('listIdsPG = ' + listIds);
      };*/

  }])


  .controller('HarmonizationStudyEditController', [
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
    'DraftHarmonizationStudiesResource',
    'DraftHarmonizationStudyResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'StudyUpdateWarningService',
    mica.study.HarmonizationStudyEditController
  ]);
