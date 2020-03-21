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
      $scope.displayBodyIndexThree = false;
      $scope.studyType = ($scope.path.indexOf('harmonization') > -1) ? 'harmonization-study' : 'individual-study';

      //NEXT
      $scope.next = function() {
        

        if ($scope.displayBodyIndexOne) {
        	
        	console.log('[NEXT-2]');
        	
            $scope.currentPage = 1;
            $scope.pageSize = 7;
            $('body').css('cursor', 'progress');
            
            $http({
                url: 'ws/draft/studies/import/_preview',
                method: 'GET',
                params: {url: $scope.importVO.url, username: $scope.importVO.username, 
                         password: $scope.importVO.password, type: $scope.studyType}
            
              }).then(function(response) {
                console.log('[NEXT-2(response)]');
              	
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
        	console.log('[NEXT-3]');
        	
        	$scope.studiesToInclude = [];
        	$scope.studiesToUpdate = [];

        	var idStudiesToCheck = [];
        	
        	//check if studies exist locally
        	for (var i = 0; i < $scope.studiesToImport.length; i++) {
                if ( $scope.studiesToImport[i].checked ) {
                	
                	idStudiesToCheck.push($scope.studiesToImport[i].id);
                	$scope.studiesToInclude.push($scope.studiesToImport[i]);
                }
        	}
            
        	$http({
                url: 'ws/draft/studies/import/_summary',
                method: 'GET',
                params: {ids: idStudiesToCheck, 
                		 type: $scope.studyType }

              }).then(function(response) {
        		
                console.log('[NEXT-3(response)]');
                console.log(response.data);
                
                var resp = response.data;
                
            	for (var j in $scope.studiesToImport) {
            		
            		if (resp.includes($scope.studiesToImport[j].id)) {
            			
            			$scope.studiesToUpdate.push($scope.studiesToImport[j]);
            			
            			delete $scope.studiesToInclude[j];
            		} 
            	}
            	
            	$scope.studiesToInclude = $scope.studiesToInclude.filter(function (el) { return el != null; });
            });
        	
        	$scope.displayBodyIndexOne = false;
            $scope.displayBodyIndexTwo = false;
            $scope.displayBodyIndexThree = true;
        }
      };

      //PREVIOUS
      $scope.previous = function() {

        if ($scope.displayBodyIndexTwo) {
        	
        	console.log('[PREVIOUS-1]');
        	
        	$scope.displayBodyIndexOne = true;
            $scope.displayBodyIndexTwo = false;
            $scope.displayBodyIndexThree = false;
            $('#myModalDialog').removeClass('modal-lg');
            $('#myPreviousButton').prop('disabled', true);
            $('#myNextButton').prop('disabled', false);
            $('#myFinishButton').prop('disabled', true);	
            
        } else {
        	console.log('[PREVIOUS-2]');
        	
        	$scope.displayBodyIndexOne = false;
            $scope.displayBodyIndexTwo = true;
            $scope.displayBodyIndexThree = false;
            $('#myFinishButton').prop('disabled', true);
        }

      };

      //CLICK_CHECKBOX
      $scope.clickCheckBox = function() {
		console.log('[CLICK_CHECKBOX]');
		
		for (var i = 0; i < $scope.studiesToImport.length; i++) {	
		    if ($scope.studiesToImport[i].checked) {
		    	$scope.studiesToImport.checked = true;
		    	return;
		    }
		}
		
		$scope.studiesToImport.checked = false;
      };
      
      //REMOVE_FROM_LIST
      $scope.removeFromList = function(studySummary, isInIncludeList) {	  
		console.log('[REMOVE_FROM_LIST]');
		console.log(studySummary);
		
		var newDataList = [];

        angular.forEach(isInIncludeList ? $scope.studiesToInclude : $scope.studiesToUpdate, function(v) {
	        if (v.id != studySummary.id) {
	            newDataList.push(v);
	        }
        });    
        
        (isInIncludeList) ? $scope.studiesToInclude = newDataList : $scope.studiesToUpdate = newDataList;

      };

      
      //CLOSE
      $scope.close = function() {	  
  		console.log('[CLOSE]');
  		
  		//$scope.$apply();
      };
      
      
      //FINISH
      $scope.finish = function() {
        console.log('[FINISH]');

        var idsToInclude = [];
        var idsToUpdate = [];
        
        $('body').css('cursor', 'progress');
        
        for (var i in $scope.studiesToInclude) idsToInclude.push( $scope.studiesToInclude[i].id );
        for (var j in $scope.studiesToUpdate) idsToUpdate.push( $scope.studiesToUpdate[j].id );
        
        if (idsToInclude.length > 0) {

	        $http({
	          url: 'ws/draft/studies/import/_include',
	          method: 'POST',
	          params: {url: $scope.importVO.url, 
	                   username: $scope.importVO.username, 
	                   password: $scope.importVO.password, 
	                   type: $scope.studyType,
	                   idsToInclude: idsToInclude}
	
	        }).then(function(response) {
	          console.log('[FINISH(response)]');
	          console.log( response.data );
	          
	          $scope.displayMsgImportedSuccessfully = true;
	          $scope.displayMsgImportedProblem = false;
	          $scope.studiesToInclude = [];
	          $scope.studiesToUpdate = [];
	          
	        });
        }
        
        if (idsToUpdate.length > 0) {

	        $http({
	          url: 'ws/draft/studies/import/_update',
	          method: 'PUT',
	          params: {url: $scope.importVO.url, 
	                   username: $scope.importVO.username, 
	                   password: $scope.importVO.password, 
	                   type: $scope.studyType,
	                   idsToUpdate: idsToUpdate}
	
	        }).then(function(response) {
	          console.log('[FINISH(response)]');
	          console.log( response.data );
	          
	          $scope.displayMsgImportedSuccessfully = true;
	          $scope.displayMsgImportedProblem = false;
	          $scope.studiesToInclude = [];
	          $scope.studiesToUpdate = [];
	          
	        });
        }
        
        $('body').css('cursor', 'default');

      };
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
