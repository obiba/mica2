'use strict';

mica.study

  .constant('STUDY_EVENTS', {
    studyUpdated: 'event:study-updated'
  })

  .controller('StudyListController', ['$rootScope', '$scope', 'StudyStatesResource', 'DraftStudyResource', 'NOTIFICATION_EVENTS',
    function ($rootScope, $scope, StudyStatesResource, DraftStudyResource, NOTIFICATION_EVENTS) {
      $scope.studies = StudyStatesResource.query();
      $scope.deleteStudy = function (id) {
        $scope.studyToDelete = id;
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {title: 'Delete study', message: 'Are you sure to delete the study?'}, id);
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
        if ($scope.studyToDelete === id) {
          DraftStudyResource.delete({id: id},
            function () {
              $scope.studies = StudyStatesResource.query();
            });
        }

        delete $scope.studyToDelete;
      });
    }])

  .controller('StudyViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', '$translate', 'StudyStateResource', 'DraftStudyResource', 'DraftStudyPublicationResource', 'MicaConfigResource', 'STUDY_EVENTS', 'NOTIFICATION_EVENTS', 'CONTACT_EVENTS',
    function ($rootScope, $scope, $routeParams, $log, $locale, $location, $translate, StudyStateResource, DraftStudyResource, DraftStudyPublicationResource, MicaConfigResource, STUDY_EVENTS, NOTIFICATION_EVENTS, CONTACT_EVENTS) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.study = DraftStudyResource.get(
        {id: $routeParams.id},
        function (study) {
          new $.MicaTimeline(new $.StudyDtoParser()).create('#timeline', study).addLegend();
        });

      $scope.studySummary = StudyStateResource.get({id: $routeParams.id});

      $scope.months = $locale.DATETIME_FORMATS.MONTH;

      $scope.emitStudyUpdated = function () {
        $scope.$emit(STUDY_EVENTS.studyUpdated, $scope.study);
      };

      $scope.$on(STUDY_EVENTS.studyUpdated, function (event, studyUpdated) {
        if (studyUpdated === $scope.study) {
          $log.debug('save study', studyUpdated);

          $scope.study.$save(function () {
              $scope.study = DraftStudyResource.get({id: $scope.study.id});
            },
            function (response) {
              $log.error('Error on study save:', response);
              $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
                message: response.data ? response.data : angular.fromJson(response)
              });
            });
        }
      });

      $scope.publish = function () {
        DraftStudyPublicationResource.publish({id: $scope.study.id}, function () {
          $scope.studySummary = StudyStateResource.get({id: $routeParams.id});
        });
      };

      $scope.sortableOptions = {
        stop: function () {
          $scope.emitStudyUpdated();
        }
      };

      $scope.$on(CONTACT_EVENTS.addInvestigator, function (event, study, contact) {
        if (study === $scope.study) {
          if (!$scope.study.investigators) {
            $scope.study.investigators = [];
          }
          $scope.study.investigators.push(contact);
          $scope.emitStudyUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.addContact, function (event, study, contact) {
        if (study === $scope.study) {
          if (!$scope.study.contacts) {
            $scope.study.contacts = [];
          }
          $scope.study.contacts.push(contact);
          $scope.emitStudyUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactUpdated, function (event, study) {
        if (study === $scope.study) {
          $scope.emitStudyUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactEditionCanceled, function (event, study) {
        if (study === $scope.study) {
          $scope.study = DraftStudyResource.get({id: $scope.study.id});
        }
      });

      $scope.$on(CONTACT_EVENTS.contactDeleted, function (event, study, contact, isInvestigator) {
        if (study === $scope.study) {
          if (isInvestigator) {
            var investigatorsIndex = $scope.study.investigators.indexOf(contact);
            if (investigatorsIndex !== -1) {
              $scope.study.investigators.splice(investigatorsIndex, 1);
            }
          } else {
            var contactsIndex = $scope.study.contacts.indexOf(contact);
            if (contactsIndex !== -1) {
              $scope.study.contacts.splice(contactsIndex, 1);
            }
          }
          $scope.emitStudyUpdated();
        }
      });

      $scope.editPopulation = function (study, population) {
        $location.url($location.url() + '/population/' + population.id + '/edit');
      };

      $scope.deletePopulation = function (population, index) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {title: 'Delete population', message: 'Are you sure to delete the population?'}, population);

        $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, population) {
          if ($scope.study.populations[index] === population) {
            $scope.study.populations.splice(index, 1);
            $scope.emitStudyUpdated();
          }
        });
      };

      $scope.addDataCollectionEvent = function (study, population) {
        $location.url($location.url() + '/population/' + population.id + '/dce/add');
      };

      $scope.editDataCollectionEvent = function (study, population, dce) {
        $location.url($location.url() + '/population/' + population.id + '/dce/' + dce.id + '/edit');
      };

      $scope.deleteDataCollectionEvent = function (study, popIndex, dce, dceIndex) {
        var titleKey = 'data-collection-event.delete-dialog-title';
        var messageKey = 'data-collection-event.delete-dialog-message';
        $translate([titleKey, messageKey])
          .then(function (translation) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
              {title: translation[titleKey], message: translation[messageKey]});
          });

        $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, dce) {
          if ($scope.study.populations[popIndex].dataCollectionEvents[dceIndex] === dce) {
            $scope.study.populations[popIndex].dataCollectionEvents.splice(dceIndex, 1);
            $scope.emitStudyUpdated();
          }
        });
      };

      $scope.addPopulation = function () {
        $location.url($location.url() + '/population/add');
      };

    }])

  .controller('StudyPopulationController', ['$rootScope', '$scope', '$routeParams', '$location', '$log', 'DraftStudyResource', 'MicaConfigResource', 'FormServerValidation', 'MicaConstants',
     function($rootScope, $scope, $routeParams, $location, $log, DraftStudyResource, MicaConfigResource, FormServerValidation, MicaConstants) {
    $scope.population = {selectionCriteria: {healthStatus: [], ethnicOrigin: []}};
    $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function() {
      if ($routeParams.pid) {
        $scope.population = $scope.study.populations.filter(function(p){
              return p.id === $routeParams.pid;
            })[0];
      } else {
        $scope.study.populations.push($scope.population);
      }
    }) : {};

    //TODO: possible values should be retreived from a resource. Hardcoded here for the moment.
    $scope.specificPopulationTypes = ['clinic_patients', 'other', 'specific_association'];
    $scope.selectionCriteriaGenders = ['N/A', 'men', 'women'];
    $scope.generalPopulationTypes = ['volunteer', 'selected_samples', 'random'];
    $scope.availableSelectionCriteria = ['criteria1', 'criteria2'];
    $scope.dataSourceTypes = ['questionnaires', 'administratives_databases', 'others'];
    $scope.availableCountries = MicaConstants.COUNTRIES_ISO_CODES;

    MicaConfigResource.get(function (micaConfig) {
      $scope.tabs = [];
      micaConfig.languages.forEach(function (lang) {
        $scope.tabs.push({ lang: lang, labelKey: 'language.' + lang });
      });
    });

    $scope.save = function () {
      if (!$scope.form.$valid) {
        $scope.form.saveAttempted = true;
        return;
      }

      updateStudy();
    };

    $scope.addHealthStatus = function () {
      $scope.population.selectionCriteria.healthStatus.push({localizedStrings: []});
    };

    $scope.addEthnicOrigin = function () {
      $scope.population.selectionCriteria.ethnicOrigin.push({localizedStrings: []});
    };

    $scope.cancel = function () {
      redirectToStudy();
    };

    var updateStudy = function () {
      $log.debug('Update study', $scope.study);
      $scope.study.$save(redirectToStudy, saveErrorHandler);
    };

    var saveErrorHandler = function (response) {
      FormServerValidation.error(response, $scope.form, $scope.languages);
    };

    var redirectToStudy = function() {
      $location.path('/study/' + $scope.study.id).replace();
    };
  }])

  .controller('StudyPopulationDceController', ['$rootScope', '$scope', '$routeParams', '$location', '$log', 'DraftStudyResource', 'MicaConfigResource', 'FormServerValidation', 'MicaConstants',
     function($rootScope, $scope, $routeParams, $location, $log, DraftStudyResource, MicaConfigResource, FormServerValidation) {
       $scope.dce = {};
       $scope.fileTypes = ".doc, .docx, .odm, .odt, .gdoc, .pdf, .txt  .xml  .xls, .xlsx, .ppt";
       $scope.defaultMinYear = 1900;
       $scope.defaultMaxYear = new Date().getFullYear() + 200;
       $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function() {
         if ($routeParams.pid) {
           $scope.population = $scope.study.populations.filter(function(p){
             return p.id === $routeParams.pid;
           })[0];

           if ($routeParams.dceId) {
             $scope.dce = $scope.population.dataCollectionEvents.filter(function(d){
                 return d.id === $routeParams.dceId;
               })[0];
           } else {
             $scope.population.dataCollectionEvents.push($scope.dce);
           }
           $scope.attachments =
             $scope.dce.attachments && $scope.dce.attachments.length > 0 ? $scope.dce.attachments : [];

           $scope.dataSources =
             ['questionnaires', 'physical_measures', 'administratives_databases', 'biological_samples', 'others'];
           $scope.bioSamples =
             [ 'blood', 'cord_blood', 'buccal_cells', 'tissues', 'saliva', 'urine', 'hair', 'nail', 'others'];
           $scope.administrativeDatabases =
             ['health_databases', 'vital_statistics_databases', 'socioeconomic_databases', 'environmental_databases'];

         } else {
           // TODO add error popup
           $log.error('Failed to retrieve population.');
         }
       }) : {};

       MicaConfigResource.get(function (micaConfig) {
         $scope.tabs = [];
         micaConfig.languages.forEach(function (lang) {
           $scope.tabs.push({ lang: lang, labelKey: 'language.' + lang });
         });
       });

       $scope.cancel = function () {
         redirectToStudy();
       };

       $scope.save = function () {
         $scope.dce.attachments = $scope.attachments.length > 0 ? $scope.attachments : null;

         if(!$scope.dce.attachments) { //protobuf doesnt like null values
           delete $scope.dce.attachments;
         }

         if (!$scope.form.$valid) {
           $scope.form.saveAttempted = true;
           return;
         }

         updateStudy();
       };

       var updateStudy = function () {
         $log.debug('Update study', $scope.study);
         $scope.study.$save(redirectToStudy, saveErrorHandler);
       };

       var saveErrorHandler = function (response) {
         FormServerValidation.error(response, $scope.form, $scope.languages);
       };

       var redirectToStudy = function() {
         $location.path('/study/' + $scope.study.id).replace();
       };

  }])

  .controller('StudyEditController', ['$rootScope', '$scope', '$routeParams', '$log', '$location', '$upload', '$timeout', '$modal', 'DraftStudyResource', 'DraftStudiesResource', 'MicaConfigResource', 'StringUtils', 'FormServerValidation', 'TempFileResource',

    function ($rootScope, $scope, $routeParams, $log, $location, $upload, $timeout, $modal, DraftStudyResource, DraftStudiesResource, MicaConfigResource, StringUtils, FormServerValidation, TempFileResource) {
      $scope.accessTypes = ['data', 'bio_samples', 'other'];
      $scope.methodDesignTypes = ['case_control', 'case_only', 'clinical_trial', 'cohort_study', 'cross_sectional', 'other'];
      $scope.methodRecruitmentTypes = ['individuals', 'families', 'other'];

      $scope.study = $routeParams.id ? DraftStudyResource.get({
        id: $routeParams.id}) : {};

      $log.debug('Edit study', $scope.study);

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        $scope.languages = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang });
          $scope.languages.push(lang);
        });
      });

      $scope.files = [];
      $scope.onFileSelect = function ($files) {
        $scope.uploadedFiles = $files;
        $scope.uploadedFiles.forEach(function (file) {
          uploadFile(file);
        });
      };

      var uploadFile = function (file) {
        $log.debug('file', file);

        var attachment = {
          showProgressBar: true,
          lang: getActiveTab().lang,
          progress: 0,
          file: file,
          fileName: file.name,
          size: file.size
        };
        $scope.study.attachments.push(attachment);

        $scope.upload = $upload
          .upload({
            url: '/ws/files/temp',
            method: 'POST',
            file: file
          })
          .progress(function (evt) {
            attachment.progress = parseInt(100.0 * evt.loaded / evt.total);
          })
          .success(function (data, status, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            var fileId = parts[parts.length - 1];
            TempFileResource.get(
              {id: fileId},
              function (tempFile) {
                $log.debug('tempFile', tempFile);
                attachment.id = tempFile.id;
                attachment.md5 = tempFile.md5;
                attachment.justUploaded = true;
                // wait for 1 second before hiding progress bar
                $timeout(function () { attachment.showProgressBar = false; }, 1000);
              }
            );
          });
      };

      $scope.deleteTempFile = function (tempFileId) {
        TempFileResource.delete(
          {id: tempFileId},
          function () {
            for (var i = $scope.study.attachments.length - 1; i--;) {
              var attachment = $scope.study.attachments[i];
              if (attachment.justUploaded && attachment.id === tempFileId) {
                $scope.study.attachments.splice(i, 1);
              }
            }
          }
        );
      };

      $scope.deleteFile = function (fileId) {
        $log.debug('Delete ', fileId);
      };

      $scope.save = function () {
        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }
        if ($scope.study.id) {
          updateStudy();
        } else {
          createStudy();
        }
      };

      var getActiveTab = function () {
        return $scope.tabs.filter(function (tab) {
          return tab.active;
        })[0];
      };

      var createStudy = function () {
        $log.debug('Create new study', $scope.study);
        DraftStudiesResource.save($scope.study,
          function (resource, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            $location.path('/study/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var updateStudy = function () {
        $log.debug('Update study', $scope.study);
        $scope.study.$save(
          function (study) {
            $location.path('/study/' + study.id).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

      $scope.cancel = function () {
        $location.path('/study' + ($scope.study.id ? '/' + $scope.study.id : '')).replace();
      };

    }]);
