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

/**
 * Studies Import controller class
 *
 * @param $scope
 * @param $route,
 * @param $rootScope,
 * @param $http
 * @constructor
 */
mica.study.StudiesImportController = function (
	$scope,
	$route,
	$rootScope,
	$http) {

  const CONNECTIONS_PARAMS_0 = 0;
  const DIFF_CUSTOM_FORM_1 = 1;
  const REMOTE_STUDIES_2 = 2;
  const OPERATIONS_SUMMARY_3 = 3;
  const FINISH_RESPONSE_MESSAGES_4 = 4;

  const INDIVIDUAL_STUDY = 'individual-study';
  const HARMONIZATION_STUDY = 'harmonization-study';
  const NONE = 'none';

  const REPLACE = 'replace';
  const CREATE = 'create';

  const CONFIG_FORM_TITLE = {
	'individual-study' : 'individual-study-config.individual-study-form-title',
    'study-population' : 'individual-study-config.population-form-title',
    'data-collection-event' : 'individual-study-config.data-collection-event-form-title',
    'harmonization-study' : 'harmonization-study-config.harmonization-study-form-title'
   };

  $scope.modalIndex = CONNECTIONS_PARAMS_0;
  $scope.studyType = ($scope.path.indexOf('harmonization') > -1) ? HARMONIZATION_STUDY : INDIVIDUAL_STUDY;
  $scope.diffsCustomFormJSON = [];
  $scope.listDiffsForm = [];
  $scope.studiesSaved = [];
  $scope.diffsConfigIsPossibleImport = false;

  //NEXT
  $scope.next = function() {
	if ($scope.modalIndex === CONNECTIONS_PARAMS_0) {
		nextConnectionParams();
	} else if ($scope.modalIndex === DIFF_CUSTOM_FORM_1) {
		nextDiffFormCustom();
    } else if ($scope.modalIndex === REMOTE_STUDIES_2) {
    	nextRemoteStudies();
    }
  };

  //PREVIOUS
  $scope.previous = function() {
    if ($scope.modalIndex === DIFF_CUSTOM_FORM_1) {
    	$('#myModalDialog').removeClass('modal-lg');
    	$scope.modalIndex = CONNECTIONS_PARAMS_0;
    } else if ($scope.modalIndex === REMOTE_STUDIES_2) {
    	$scope.modalIndex = DIFF_CUSTOM_FORM_1;
    } else if ($scope.modalIndex === OPERATIONS_SUMMARY_3) {
    	$scope.modalIndex = REMOTE_STUDIES_2;
    } else if ($scope.modalIndex === FINISH_RESPONSE_MESSAGES_4) {
    	$scope.modalIndex = DIFF_CUSTOM_FORM_1;
    }
  };

  //CLICK_CHECKBOX
  $scope.clickCheckBox = function() {
	for (var i = 0; i < $scope.studiesToImport.length; i++) {
	    if ($scope.studiesToImport[i].checked) {
	    	$scope.studiesToImport.checked = true;
	    	return;
	    }
	}

	$scope.studiesToImport.checked = false;
  };

  //REMOVE_FROM_LIST
  $scope.removeFromList = function(studySummary, isInCreateList) {
	var newDataList = [];

    angular.forEach(isInCreateList ? $scope.studiesToCreate : $scope.studiesToReplace, function(v) {
        if (v.id !== studySummary.id) {
            newDataList.push(v);
        }
    });

    if (isInCreateList) {
    	$scope.studiesToCreate = newDataList;
    } else {
    	$scope.studiesToReplace = newDataList;
    }

    angular.forEach($scope.studiesConflict, function(v) {
        if (v.id === studySummary.id) {
        	delete $scope.studiesConflict[v];
        }
    });

    $scope.studiesConflict = $scope.studiesConflict.filter(function (el) { return el !== null; });
    if ($scope.studiesToCreate.length === 0 && $scope.studiesToReplace.length === 0) {
    	$scope.modalIndex = REMOTE_STUDIES_2;
    }
  };

  //CLOSE
  $scope.close = function() {
	if ($scope.studiesSaved.length > 0) {
		window.top.location.reload();
	}
  };

  //FINISH
  $scope.finish = function() {
    var idsToSave = [];

    $scope.studiesSaved = [];
    $('body').css('cursor', 'progress');

    for (var i in $scope.studiesToCreate) {
    	idsToSave.push( $scope.studiesToCreate[i].id );
    }

    for (var j in $scope.studiesToReplace) {
    	idsToSave.push( $scope.studiesToReplace[j].id );
    }

    if (idsToSave.length > 0) {
    	$http({
          url: contextPath + '/ws/draft/studies/import/_save',
          method: 'PUT',
          params: {url: $scope.importVO.url,
                   username: $scope.importVO.username,
                   password: $scope.importVO.password,
                   type: $scope.studyType,
                   ids: idsToSave,
                   listDiffsForm: $scope.listDiffsForm
                   }
        }).then(function(response) {
          var responseData = response.data;

          $scope.idsSaved = Object.keys(responseData);
          angular.forEach($scope.studiesToCreate, function(v) {
        	if ($scope.idsSaved.includes(v.id)) {
        		v.statusImport =  handleHTTPStatus( responseData[v.id] );
        		v.operation = CREATE;
        		$scope.studiesSaved.push(v);
        	}
	      });

          angular.forEach($scope.studiesToReplace, function(v) {
  	        if ($scope.idsSaved.includes(v.id)) {
  	        	v.statusImport = handleHTTPStatus( responseData[v.id] );
  	        	v.operation = REPLACE;
  	        	$scope.studiesSaved.push(v);
  	        }
          });

          $scope.studiesToReplace = [];
          $scope.studiesToCreate = [];
          $scope.studiesConflict = [];
          $('body').css('cursor', 'default');
          $scope.modalIndex = FINISH_RESPONSE_MESSAGES_4;
        });
    }
  };

  function nextConnectionParams() {
    $('body').css('cursor', 'progress');
    $http({
        url: contextPath + '/ws/draft/studies/import/_differences',
        method: 'GET',
        params: {url: $scope.importVO.url, username: $scope.importVO.username,
                 password: $scope.importVO.password, type: $scope.studyType}
      }).then(function(response) {
        if (typeof(response.data) === 'number') {
       	 	$scope.statusErrorImport = handleHTTPStatus(response.data);
        } else {
        	$scope.statusErrorImport = '';

        	var diffsEnum = response.data;
            var mapDiffsFormParent = new Map();

            mapDiffsFormParent.set(NONE, true);
            $scope.diffsCustomFormJSON = [];
            $scope.listDiffsForm = [];

            for (var prop in diffsEnum) {
            	var propJSON = JSON.parse(String(prop));

            	mapDiffsFormParent.set(propJSON.formSection, diffsEnum[prop] && mapDiffsFormParent.get(propJSON.parentFormSection) );

            	var diffVO = {
	          			formSection : propJSON.formSection,
            			formTitle : CONFIG_FORM_TITLE[propJSON.formSection],
	      				endpoint : propJSON.endpoint,
	      				isEqual : diffsEnum[prop],
	      				parentIsImportable : mapDiffsFormParent.get(propJSON.parentFormSection)
	      			  };

        		$scope.diffsCustomFormJSON.push(diffVO);
        		if (!diffsEnum[prop]) {
        			$scope.listDiffsForm.push(propJSON.formSection);
        		}

            	if ((propJSON.formSection === INDIVIDUAL_STUDY || propJSON.formSection === HARMONIZATION_STUDY) &&  diffsEnum[prop]) {
            		$scope.diffsConfigIsPossibleImport = true;
            	}
            }

            $('#myModalDialog').addClass('modal-lg');
            $scope.modalIndex = DIFF_CUSTOM_FORM_1;
        }

        $('body').css('cursor', 'default');
      });
  }

  function nextDiffFormCustom() {
    $scope.currentPage = 1;
    $scope.pageSize = 7;
    $('body').css('cursor', 'progress');

    $http({
        url: contextPath + '/ws/draft/studies/import/_preview',
        method: 'GET',
        params: {url: $scope.importVO.url, username: $scope.importVO.username,
                 password: $scope.importVO.password, type: $scope.studyType}
      }).then(function(response) {
         if (typeof(response.data) === 'number') {
        	 $scope.statusErrorImport = handleHTTPStatus(response.data);
         } else {
        	 $scope.statusErrorImport = '';
        	 $scope.studiesToImport = response.data;
             $scope.modalIndex = REMOTE_STUDIES_2;
         }

         $('body').css('cursor', 'default');
      });
  }


  function handleHTTPStatus(code) {
	  switch(code) {
	    case 200:
	  	  return 'study.import.status-ok';
	    case 204:
		  return 'study.import.problems.problem-204';
	    case 400:
		  return 'study.import.problems.problem-400';
	  	case 401:
		  return 'study.import.problems.problem-401';
	  	case 404:
		  return 'study.import.problems.problem-404';
	  	case 408:
	  	  return 'study.import.problems.problem-408';
	  	case 500:
		  return 'study.import.problems.problem-500';
	  	case 503:
		  return 'study.import.problems.problem-503';
	  	default:
	  	  return 'study.import.status-ok';
	  }
  }


  function nextRemoteStudies() {
	$scope.studiesToCreate = [];
	$scope.studiesToReplace = [];
	$scope.studiesConflict = [];
	$scope.replacements = [];

	angular.forEach($scope.diffsCustomFormJSON, function(v) {
        if (v.isEqual && v.parentIsImportable) {
        	$scope.replacements.push(v.formSection);
        }
    });

	var idStudiesToCheck = [];

	//check if studies exist locally
	for (var i = 0; i < $scope.studiesToImport.length; i++) {
        if ( $scope.studiesToImport[i].checked ) {
        	idStudiesToCheck.push($scope.studiesToImport[i].id);
        	$scope.studiesToCreate.push($scope.studiesToImport[i]);
        }
	}

	$http({
        url: contextPath + '/ws/draft/studies/import/_summary',
        method: 'GET',
        params: {ids: idStudiesToCheck,
        		 type: $scope.studyType }
      }).then(function(response) {
        var responseData = response.data;

        if (Object.keys(responseData).length > 0) {
        	for (var j in $scope.studiesToCreate) {
        		if ($scope.studiesToCreate[j].id in responseData) {
        			var valueJSON = JSON.parse( responseData[ $scope.studiesToCreate[j].id ]);

        			console.log( typeof(valueJSON.conflict) );

            		if ( valueJSON.conflict === true ) {

            		    $scope.studiesConflict.push( $scope.studiesToCreate[j] );
            			delete $scope.studiesToCreate[j];
            		} else {

            			var studyToReplace = $scope.studiesToCreate[j];

            			studyToReplace.localPopulationSize = parseInt(valueJSON.localPopulationSize);
            			studyToReplace.localDCEsSize = parseInt(valueJSON.localDCEsSize);

            			$scope.studiesToReplace.push( studyToReplace );
            			delete $scope.studiesToCreate[j];
            		}
        		}
        	}
        }

    	$scope.studiesToCreate = $scope.studiesToCreate.filter(function (el) { return el !== null; });
    	$scope.modalIndex = OPERATIONS_SUMMARY_3;
    });
  }
};
