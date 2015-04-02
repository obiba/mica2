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

  .controller('StudyViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location',
    '$translate', 'StudyStateResource', 'DraftStudyResource', 'DraftStudyPublicationResource', 'MicaConfigResource', 'STUDY_EVENTS', 'NOTIFICATION_EVENTS', 'CONTACT_EVENTS',
    'MicaStudiesConfigResource', function ($rootScope, $scope, $routeParams, $log, $locale, $location, $translate, StudyStateResource, DraftStudyResource, DraftStudyPublicationResource, MicaConfigResource, STUDY_EVENTS, NOTIFICATION_EVENTS, CONTACT_EVENTS, MicaStudiesConfigResource) {
      var getActiveTab = function () {
        return $scope.tabs.filter(function (tab) {
          return tab.active;
        })[0];
      };

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.studiesConfig = MicaStudiesConfigResource.get();

      $scope.getLabel = function(vocabulary, term) {
        if (!term) {
          return;
        }

        return ((($scope.studiesConfig.vocabularies.filter(function(v) {
          return v.name === vocabulary;
        }) || [{terms: []}])[0].terms.filter(function(t) {
            return t.name === term;
          }) || [{title : []}])[0].title.filter(function(v) {
            return v.locale === getActiveTab().lang;
          }) || [{text: term}])[0].text;
      };

      $scope.study = DraftStudyResource.get(
        {id: $routeParams.id},
        function (study) {
          if (study.logo) {
            $scope.logoUrl = 'ws/draft/study/'+study.id+'/file/'+study.logo.id+'/_download';
          }
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
              {title: translation[titleKey], message: translation[messageKey]}, dce);
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

  .controller('StudyPopulationController', ['$rootScope', '$scope', '$routeParams', '$location', '$log',
    'DraftStudyResource', 'MicaConfigResource', 'FormServerValidation', 'MicaConstants', 'MicaStudiesConfigResource',
    function ($rootScope, $scope, $routeParams, $location, $log, DraftStudyResource, MicaConfigResource, FormServerValidation, MicaConstants,
              MicaStudiesConfigResource) {
      $scope.availableCountries = MicaConstants.COUNTRIES_ISO_CODES;
      $scope.selectionCriteriaGenders = [];
      $scope.availableSelectionCriteria = [];
      $scope.recruitmentSourcesTypes = [];
      $scope.generalPopulationTypes = [];
      $scope.specificPopulationTypes = [];
      $scope.tabs = [];
      $scope.recruitmentTabs = {};
      $scope.population = {selectionCriteria: {healthStatus: [], ethnicOrigin: []}, recruitment: {dataSources: []}};

      $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function () {
        if ($routeParams.pid) {
          $scope.population = $scope.study.populations.filter(function (p) {
            return p.id === $routeParams.pid;
          })[0];
          $scope.numberOfParticipants = $scope.population.numberOfParticipants;
          $scope.numberOfParticipantsTemplate = 'app/study/views/common/number-of-participants.html';
        } else {
          if ($scope.study.populations === undefined) {
            $scope.study.populations = [];
          }
          $scope.study.populations.push($scope.population);
        }
      }) : {};

      $scope.$watch('population.recruitment.dataSources', function (newVal, oldVal) {
        if (oldVal === undefined || newVal === undefined) {
          $scope.population.recruitment.dataSources = [];
          return;
        }

        updateActiveDatasourceTab(newVal, oldVal);
      }, true);

      var getActiveTab = function () {
        return $scope.tabs.filter(function (tab) {
          return tab.active;
        })[0];
      };

      var getLabel = function (localizedString) {
        return (localizedString.filter(function (t) {
          return t.locale === getActiveTab().lang;
        }) || [{text: null}])[0].text;
      };

      var updateActiveDatasourceTab = function (newVal, oldVal) {
        function arrayDiff(source, target) {
          for (var i = 0; i < source.length; i++) {
            if (target.indexOf(source[i]) < 0) {
              return source[i];
            }
          }
        }

        if (newVal.length < oldVal.length) {
          var rem = arrayDiff(oldVal, newVal);

          if (rem) {
            if ($scope.recruitmentTabs[rem]) {
              $scope.recruitmentTabs[newVal[0]] = true;
            }

            $scope.recruitmentTabs[rem] = false;
          }
        } else {
          var added = arrayDiff(newVal, oldVal);

          if (added) {
            for (var k in $scope.recruitmentTabs) {
              $scope.recruitmentTabs[k] = false;
            }

            $scope.recruitmentTabs[added] = true;
          }
        }
      };

      MicaConfigResource.get(function (micaConfig) {
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang, labelKey: 'language.' + lang});
        });
      });

      MicaStudiesConfigResource.get(function (studiesConfig) {
        function extractVocabulary(options) {
          var opts = studiesConfig.vocabularies.map(function (v) {
            if (v.name === options) {
              return v.terms.map(function (t) {
                return {name: t.name, label: getLabel(t.title) || t.name};
              });
            }
          }).filter(function (x) { return x; });

          return opts ? opts[0] : [];
        }

        $scope.selectionCriteriaGenders = extractVocabulary('populations-selectionCriteria-gender').map(function (obj) {
          return {id: obj.name, label: obj.label};
        });
        $scope.availableSelectionCriteria = extractVocabulary('selectionCriteriaCriteria');
        $scope.recruitmentSourcesTypes = extractVocabulary('recruitmentDatasources');
        $scope.generalPopulationTypes = extractVocabulary('recruitmentGeneralPopulation');
        $scope.specificPopulationTypes = extractVocabulary('recruitmentSpecificPopulation');
      });

      $scope.save = function () {
        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }

        var selectedDatasources = [];
        var hasRecruitmentDatasources = null != $scope.population.recruitment.dataSources; // null or undefined

        if (hasRecruitmentDatasources && $scope.population.recruitment.dataSources.indexOf('general') < 0) {
          delete $scope.population.recruitment.generalPopulationSources;
        } else if ($scope.population.recruitment.generalPopulationSources &&
          $scope.population.recruitment.generalPopulationSources.length) {
          selectedDatasources.push('general');
        }

        if (hasRecruitmentDatasources && $scope.population.recruitment.dataSources.indexOf('specific_population') < 0) {
          delete $scope.population.recruitment.specificPopulationSources;
        } else if ($scope.population.recruitment.specificPopulationSources &&
          $scope.population.recruitment.specificPopulationSources.length) {
          selectedDatasources.push('specific_population');
        }

        if (!$scope.population.recruitment.specificPopulationSources ||
          $scope.population.recruitment.specificPopulationSources.indexOf('other') < 0) {
          $scope.population.recruitment.otherSpecificPopulationSource = [];
        }

        if (hasRecruitmentDatasources && $scope.population.recruitment.dataSources.indexOf('exist_studies') < 0) {
          delete $scope.population.recruitment.studies;
        } else if ($scope.population.recruitment.studies && $scope.population.recruitment.studies.length) {
          selectedDatasources.push('exist_studies');
        }

        if (hasRecruitmentDatasources && $scope.population.recruitment.dataSources.indexOf('other') < 0) {
          delete $scope.population.recruitment.otherSource;
        } else if ($scope.population.recruitment.otherSource) {
          selectedDatasources.push('other');
        }

        $scope.population.recruitment.dataSources = selectedDatasources;

        if (!$scope.population.selectionCriteria.gender) {
          delete $scope.population.selectionCriteria.gender;
        }

        updateStudy();
      };

      function removeLocalizedString(target, item) {
        var idx = -1;

        for (var i = target.length; i--;) {
          if (target[i].localizedStrings === item) {
            idx = i;
          }
        }

        if (idx > -1) {
          target.splice(idx, 1);
        }
      }

      $scope.removeHealthStatus = function (item) {
        removeLocalizedString($scope.population.selectionCriteria.healthStatus, item);
      };

      $scope.removeEthnicOrigin = function (item) {
        removeLocalizedString($scope.population.selectionCriteria.ethnicOrigin, item);
      };

      $scope.addHealthStatus = function () {
        $scope.population.selectionCriteria.healthStatus = $scope.population.selectionCriteria.healthStatus || [];
        $scope.population.selectionCriteria.healthStatus.push({localizedStrings: []});
      };

      $scope.addEthnicOrigin = function () {
        $scope.population.selectionCriteria.ethnicOrigin = $scope.population.selectionCriteria.ethnicOrigin || [];
        $scope.population.selectionCriteria.ethnicOrigin.push({localizedStrings: []});
      };

      $scope.removeRecruitmentStudy = function (item) {
        removeLocalizedString($scope.population.recruitment.studies, item);
      };

      $scope.addRecruitmentStudy = function () {
        $scope.population.recruitment.studies = $scope.population.recruitment.studies || [];
        $scope.population.recruitment.studies.push({localizedStrings: []});
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

      var redirectToStudy = function () {
        $location.path('/study/' + $scope.study.id).replace();
      };
    }])

  .controller('StudyPopulationDceController', ['$rootScope', '$scope', '$routeParams', '$location', '$log', 'DraftStudyResource', 'MicaConfigResource', 'FormServerValidation', 'MicaConstants',
     function($rootScope, $scope, $routeParams, $location, $log, DraftStudyResource, MicaConfigResource, FormServerValidation) {
       $scope.dce = {};
       $scope.fileTypes = '.doc, .docx, .odm, .odt, .gdoc, .pdf, .txt  .xml  .xls, .xlsx, .ppt';
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
             if ($scope.population.dataCollectionEvents === undefined) {
               $scope.population.dataCollectionEvents = [];
             }
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
         $scope.dce.attachments = $scope.attachments === undefined || $scope.attachments.length > 0 ? $scope.attachments : null;

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
         $log.info('Update study', $scope.study);
         $scope.study.$save(redirectToStudy, saveErrorHandler);
       };

       var saveErrorHandler = function (response) {
         FormServerValidation.error(response, $scope.form, $scope.languages);
       };

       var redirectToStudy = function() {
         $location.path('/study/' + $scope.study.id).replace();
       };

  }])

  .controller('StudyEditController', ['$rootScope', '$scope', '$routeParams', '$log', '$location', '$upload', '$timeout', '$modal', 'DraftStudyResource', 'DraftStudiesResource', 'MicaConfigResource', 'StringUtils', 'FormServerValidation',

    function ($rootScope, $scope, $routeParams, $log, $location, $upload, $timeout, $modal, DraftStudyResource, DraftStudiesResource, MicaConfigResource, StringUtils, FormServerValidation) {
      $scope.fileTypes = '.doc, .docx, .odm, .odt, .gdoc, .pdf, .txt  .xml  .xls, .xlsx, .ppt';
      $scope.accessTypes = ['data', 'bio_samples', 'other'];
      $scope.methodDesignTypes = ['case_control', 'case_only', 'clinical_trial', 'cohort_study', 'cross_sectional', 'other'];
      $scope.methodRecruitmentTypes = ['individuals', 'families', 'other'];
      $scope.files = [];
      $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function(response) {
        if ($routeParams.id) {
          $scope.numberOfParticipants = response.numberOfParticipants;
          $scope.numberOfParticipantsTemplate = 'app/study/views/common/number-of-participants.html';
          $scope.files = response.logo ? [response.logo] : [];
          $scope.study.attachments =
            response.attachments && response.attachments.length > 0 ? response.attachments : [];
        }
      }) : {};

      $log.debug('Edit study', $scope.study);

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        $scope.languages = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang });
          $scope.languages.push(lang);
        });
      });

      $scope.save = function () {
        $scope.study.logo = $scope.files.length > 0 ? $scope.files[0] : null;

        if(!$scope.study.logo) { //protobuf doesnt like null values
          delete $scope.study.logo;
        }

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
