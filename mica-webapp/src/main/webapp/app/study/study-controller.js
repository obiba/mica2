'use strict';

mica.study

  .constant('STUDY_EVENTS', {
    studyUpdated: 'event:study-updated'
  })

  .controller('StudyListController', ['$rootScope', '$scope', '$translate', '$interpolate', 'StudyStatesResource', 'DraftStudyResource', 'NOTIFICATION_EVENTS',
    function ($rootScope, $scope, $translate, $interpolate, StudyStatesResource, DraftStudyResource, NOTIFICATION_EVENTS) {
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
            }, function (response) {
              if (response.status === 409) {
                var conflicts = '{{network ? networks + ": " + network + ". " : "" }}' +
                  '{{harmonizationDataset ? harmonizationDatasets + ": " + harmonizationDataset + ". " : "" }}' +
                  '{{studyDataset ? studyDatasets + ": " + studyDataset : "" }}';

                $translate(['study.delete-conflict-message', 'networks', 'study-datasets', 'harmonization-datasets'])
                  .then(function (translation) {
                    $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
                      titleKey: 'study.delete-conflict',
                      message: translation['study.delete-conflict-message'] + ' ' + $interpolate(conflicts)(
                        {
                          networks: translation.networks,
                          harmonizationDatasets: translation['harmonization-datasets'],
                          studyDatasets: translation['study-datasets'],
                          network: response.data.network.join(', '),
                          harmonizationDataset: response.data.harmonizationDataset.join(', '),
                          studyDataset: response.data.studyDataset.join(', ')
                        })
                    });
                  });
              } else {
                $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
                  titleKey: 'form-server-error',
                  message: angular.toJson(response)
                });
              }
            });
        }

        delete $scope.studyToDelete;
      });
    }])

  .controller('StudyViewController', [
    '$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    '$translate',
    '$filter',
    'StudyStateResource',
    'DraftStudyResource',
    'DraftStudyPublicationResource',
    'DraftStudyViewRevisionResource',
    'DraftStudyRevisionsResource',
    'DraftStudyRestoreRevisionResource',
    'MicaConfigResource',
    'STUDY_EVENTS',
    'NOTIFICATION_EVENTS',
    'CONTACT_EVENTS',
    'MicaStudiesConfigResource',
    '$modal',
    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              $translate,
              $filter,
              StudyStateResource,
              DraftStudyResource,
              DraftStudyPublicationResource,
              DraftStudyViewRevisionResource,
              DraftStudyRevisionsResource,
              DraftStudyRestoreRevisionResource,
              MicaConfigResource,
              STUDY_EVENTS,
              NOTIFICATION_EVENTS,
              CONTACT_EVENTS,
              MicaStudiesConfigResource,
              $modal) {

      $scope.Mode = { View: 0, Revision: 1};
      $scope.viewMode = /\/revision[s\/]*/.exec($location.path()) !== null ? $scope.Mode.Revision : $scope.Mode.View;

      $scope.inViewMode = function() {
        return $scope.viewMode === $scope.Mode.View;
      };

      var getActiveTab = function () {
        return $scope.tabs.filter(function (tab) {
          return tab.active;
        })[0];
      };

      var updateTimeline = function(study) {
        if (!$scope.timeline) {
          $scope.timeline = new $.MicaTimeline(new $.StudyDtoParser());
        }

        $scope.timeline.reset().create('#timeline', study).addLegend();
      };

      var initializeStudy = function (study) {
        if (study.logo) {
          $scope.logoUrl = 'ws/draft/study/' + study.id + '/file/' + study.logo.id + '/_download';
        }
        updateTimeline(study);
      };

      var viewRevision = function(studyId, commitId) {
        $scope.study = DraftStudyViewRevisionResource.view({id: studyId, commitId: commitId}, initializeStudy);
      };

      var fetchStudy = function(studyId) {
        $scope.study = DraftStudyResource.get({id: studyId}, initializeStudy);
      };

      var fetchRevisions = function(studyId, onSuccess) {
        DraftStudyRevisionsResource.query({id: studyId}, function (response) {
          if (onSuccess) {
            onSuccess(response);
          }
        });
      };

      var restoreRevision = function(studyId, commitInfo) {
        if (commitInfo) {
          $scope.studyId = studyId;
          $scope.studyRevisionToRestore = commitInfo.commitId;
          $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {
              titleKey: 'study.restore-dialog.title',
              messageKey:'study.restore-dialog.message',
              messageArgs: [$filter('amDateFormat')(commitInfo.date, 'lll')]
            }, commitInfo.commitId
          );
        }
      };

      var onRestore = function (event, id) {
        if ($scope.studyRevisionToRestore === id) {
          DraftStudyRestoreRevisionResource.restore({id: $scope.studyId, commitId: $scope.studyRevisionToRestore},
            function () {
              fetchStudy($routeParams.id);
            });

          delete $scope.studyId;
          delete $scope.studyRevisionToRestore;
        }
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onRestore);

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.studiesConfig = MicaStudiesConfigResource.get();

      $scope.getLabel = function (vocabulary, term) {
        if (!term || !$scope.studiesConfig.$resolved) {
          return;
        }

        return ((($scope.studiesConfig.vocabularies.filter(function (v) {
          return v.name === vocabulary;
        }) || [{terms: []}])[0].terms.filter(function (t) {
            return t.name === term;
          }) || [{title: []}])[0].title.filter(function (v) {
            return v.locale === getActiveTab().lang;
          }) || [{text: term}])[0].text;
      };

      if ($scope.viewMode === $scope.Mode.Revision && $routeParams.commitId) {
        viewRevision($scope.study.id, $routeParams.commitId);
      } else {
        fetchStudy($routeParams.id);
      }

      $scope.fetchStudy = fetchStudy;
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.fetchRevisions = fetchRevisions;
      $scope.studySummary = StudyStateResource.get({id: $routeParams.id});
      $scope.months = $locale.DATETIME_FORMATS.MONTH;

      $scope.emitStudyUpdated = function () {
        $scope.$emit(STUDY_EVENTS.studyUpdated, $scope.study);
      };

      $scope.$on(STUDY_EVENTS.studyUpdated, function (event, studyUpdated) {
        if (studyUpdated === $scope.study) {
          $log.debug('save study', studyUpdated);

          $scope.study.$save(function () {
              $scope.study = DraftStudyResource.get({id: $scope.study.id}, function onSuccess(study){
                initializeStudy(study);
              });
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

      $scope.addDataCollectionEvent = function (study, population, dce) {
        $location.url($location.path() + '/population/' + population.id + '/dce/add');

        if(dce) {
          $location.search('sourceDceId', dce.id);
        }
      };

      $scope.showDataCollectionEvent = function (study, population, dce) {
        $modal.open({
          templateUrl: 'app/study/views/population/dce/data-collection-event-view.html',
          controller: 'StudyPopulationDceModalController',
          resolve: {
            dce: function () {
              return dce;
            },
            study: function () {
              return study;
            }
          }
        });
      };

      $scope.editDataCollectionEvent = function (study, population, dce) {
        $location.url($location.url() + '/population/' + population.id + '/dce/' + dce.id + '/edit');
      };

      $scope.deleteDataCollectionEvent = function (population, dce) {
        var titleKey = 'data-collection-event.delete-dialog-title';
        var messageKey = 'data-collection-event.delete-dialog-message';
        $translate([titleKey, messageKey])
          .then(function (translation) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
              {title: translation[titleKey], message: translation[messageKey]}, {dce: dce, population: population});
          });
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, data) {
        var popIndex = $scope.study.populations.indexOf(data.population);
        if (popIndex > -1) {
          var dceIndex = data.population.dataCollectionEvents.indexOf(data.dce);
          if (dceIndex > -1) {
            data.population.dataCollectionEvents.splice(dceIndex, 1);
            $scope.emitStudyUpdated();
          }
        }
      });

      $scope.addPopulation = function () {
        $location.url($location.url() + '/population/add');
      };

    }])

  .controller('StudyPopulationDceModalController', ['$scope', '$modalInstance', '$locale', 'dce', 'study',
    function ($scope, $modalInstance, $locale, dce, study) {
      $scope.months = $locale.DATETIME_FORMATS.MONTH;
      $scope.dce = dce;
      $scope.study = study;

      $scope.cancel = function () {
        $modalInstance.close();
      };
    }])

  .controller('StudyPopulationController', ['$rootScope', '$scope', '$routeParams', '$location', '$log',
    'DraftStudyResource', 'MicaConfigResource', 'FormServerValidation', 'MicaStudiesConfigResource',
    'MicaUtil', 'ObibaCountriesIsoCodes',
    function ($rootScope, $scope, $routeParams, $location, $log, DraftStudyResource, MicaConfigResource, FormServerValidation,
              MicaStudiesConfigResource, MicaUtil, ObibaCountriesIsoCodes) {

      $scope.getAvailableCountries = function (locale) { return ObibaCountriesIsoCodes[locale]; };
      $scope.selectionCriteriaGenders = [];
      $scope.availableSelectionCriteria = [];
      $scope.recruitmentSourcesTypes = [];
      $scope.generalPopulationTypes = [];
      $scope.specificPopulationTypes = [];
      $scope.tabs = [];
      $scope.recruitmentTabs = {};
      $scope.population = {selectionCriteria: {healthStatus: [], ethnicOrigin: []}, recruitment: {dataSources: []}};

      $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function () {
        var populationsIds;

        if ($routeParams.pid) {
          $scope.population = $scope.study.populations.filter(function (p) {
            return p.id === $routeParams.pid;
          })[0];

        } else {
          if (!$scope.study.populations) {
            $scope.study.populations = [];
          }

          if ($scope.study.populations.length) {
            populationsIds = $scope.study.populations.map(function(p) {
              return p.id;
            });

            $scope.population.id = MicaUtil.generateNextId(populationsIds);
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

      $scope.save = function (form) {
        if (!validate(form)) {
          form.saveAttempted = true;
          return;
        }

        var selectedDatasources = [];
        var hasRecruitmentDatasources = null !== $scope.population.recruitment.dataSources; // null or undefined

        if (hasRecruitmentDatasources && $scope.population.recruitment.dataSources.indexOf('general_population') < 0) {
          delete $scope.population.recruitment.generalPopulationSources;
        } else if ($scope.population.recruitment.generalPopulationSources &&
          $scope.population.recruitment.generalPopulationSources.length) {
          selectedDatasources.push('general_population');
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

      var validate = function (form) {
        if ($scope.study.populations.filter(function (p) {
            return p.id === $scope.population.id;
          }).length > 1) {
          form.$setValidity('population_id', false);
        } else {
          form.$setValidity('population_id', true);
        }

        return form.$valid;
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

      var redirectToStudy = function () {
        $location.path('/study/' + $scope.study.id).replace();
      };
    }])

  .controller('StudyPopulationDceController', ['$rootScope', '$scope', '$routeParams', '$location', '$log',
    'DraftStudyResource', 'MicaConfigResource', 'FormServerValidation', 'MicaUtil',
    function ($rootScope, $scope, $routeParams, $location, $log, DraftStudyResource, MicaConfigResource,
              FormServerValidation, MicaUtil) {
      $scope.dce = {};
      $scope.fileTypes = '.doc, .docx, .odm, .odt, .gdoc, .pdf, .txt  .xml  .xls, .xlsx, .ppt';
      $scope.defaultMinYear = 1900;
      $scope.defaultMaxYear = new Date().getFullYear() + 200;
      $scope.dataSourcesTabs = {};
      $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function () {
        if ($routeParams.pid) {
          $scope.population = $scope.study.populations.filter(function (p) {
            return p.id === $routeParams.pid;
          })[0];

          if ($routeParams.dceId) {
            $scope.dce = $scope.population.dataCollectionEvents.filter(function (d) {
              return d.id === $routeParams.dceId;
            })[0];
          } else {
            var sourceDceId = $location.search().sourceDceId;

            if ($scope.population.dataCollectionEvents === undefined) {
              $scope.population.dataCollectionEvents = [];
            }

            var dceIds = $scope.population.dataCollectionEvents.map(function(dce){
              return dce.id;
            });

            if (sourceDceId) {
              var sourceDce = $scope.population.dataCollectionEvents.filter(function (dce) {
                return dce.id === sourceDceId;
              })[0];

              if (sourceDce) {
                angular.copy(sourceDce, $scope.dce);
                $scope.dce.id = MicaUtil.generateNextId(dceIds);
                delete $scope.dce.attachments;
                delete $scope.dce.startYear;
                delete $scope.dce.startMonth;
                delete $scope.dce.endYear;
                delete $scope.dce.endMonth;
              }
            } else if (dceIds.length) {
              $scope.dce.id = MicaUtil.generateNextId(dceIds);
            }

            $scope.population.dataCollectionEvents.push($scope.dce);
          }
          $scope.attachments =
            $scope.dce.attachments && $scope.dce.attachments.length > 0 ? $scope.dce.attachments : [];

          $scope.dataSources =
            ['questionnaires', 'physical_measures', 'administratives_databases', 'biological_samples', 'others'];
          $scope.bioSamples =
            ['blood', 'cord_blood', 'buccal_cells', 'tissues', 'saliva', 'urine', 'hair', 'nail', 'others'];
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
          $scope.tabs.push({lang: lang, labelKey: 'language.' + lang});
        });
      });

      $scope.cancel = function () {
        redirectToStudy();
      };

      $scope.save = function (form) {
        $scope.dce.attachments = $scope.attachments === undefined || $scope.attachments.length > 0 ? $scope.attachments : null;

        if (!$scope.dce.attachments) { //protobuf doesnt like null values
          delete $scope.dce.attachments;
        }

        if (!validate(form)) {
          form.saveAttempted = true;
          return;
        }

        updateStudy();
      };

      var validate = function (form) {
        if ($scope.population.dataCollectionEvents.filter(function (d) {
            return d.id === $scope.dce.id;
          }).length > 1) {
          form.$setValidity('dce_id', false);
        } else {
          form.$setValidity('dce_id', true);
        }

        return form.$valid;
      };

      var updateStudy = function () {
        $log.info('Update study', $scope.study);
        $scope.study.$save(redirectToStudy, saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

      var redirectToStudy = function () {
        $location.search('sourceDceId', null);
        $location.path('/study/' + $scope.study.id).replace();
      };

      $scope.$watch('dce.dataSources', function (newVal, oldVal) {
        if (oldVal === undefined || newVal === undefined) {
          $scope.dce.dataSources = [];
          return;
        }

        var dsFilter = function(d) {
          return d !== 'questionnaires' && d !=='physical_measures';
        };

        updateActiveDatasourceTab(angular.copy(newVal).filter(dsFilter), angular.copy(oldVal).filter(dsFilter));
      }, true);

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
            if ($scope.dataSourcesTabs[rem]) {
              $scope.dataSourcesTabs[newVal[0]] = true;
            }

            $scope.dataSourcesTabs[rem] = false;
          }
        } else {
          var added = arrayDiff(newVal, oldVal);

          if (added) {
            for (var k in $scope.dataSourcesTabs) {
              $scope.dataSourcesTabs[k] = false;
            }

            $scope.dataSourcesTabs[added] = true;
          }
        }
      };

    }])

  .controller('StudyEditController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$location',
    '$modal',
    'DraftStudyResource',
    'DraftStudiesResource',
    'MicaConfigResource',
    'StringUtils',
    'FormServerValidation',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $location,
              $modal,
              DraftStudyResource,
              DraftStudiesResource,
              MicaConfigResource,
              StringUtils,
              FormServerValidation) {

      // TODO this code needs simplification and possibly placed inside a directive
      $scope.revision = {comment: null};
      $scope.today = new Date();
      $scope.$watch('authorization.maelstrom.date', function (newVal) {
        if (newVal !== $scope.today) {
          $scope.study.maelstromAuthorization.date = newVal;
        }
      }, true);
      $scope.$watch('authorization.specific.date', function (newVal) {
        if (newVal !== $scope.today) {
          $scope.study.specificAuthorization.date = newVal;
        }
      }, true);
      $scope.authorization = {maelstrom: {date: $scope.today}, specific: {date: $scope.today}};
      $scope.datePicker = {maelstrom: {opened: false}, specific: {opened: false}};
      $scope.openDatePicker = function($event, id) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.datePicker[id].opened = true;
      };

      $scope.defaultMinYear = 1900;
      $scope.defaultMaxYear = 9999;
      $scope.fileTypes = '.doc, .docx, .odm, .odt, .gdoc, .pdf, .txt  .xml  .xls, .xlsx, .ppt';
      $scope.accessTypes = ['data', 'bio_samples', 'other'];
      $scope.methodDesignTypes = ['case_control', 'case_only', 'clinical_trial', 'cohort_study', 'cross_sectional', 'other'];
      $scope.methodRecruitmentTypes = ['individuals', 'families', 'other'];
      $scope.files = [];
      $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function (response) {
        if ($routeParams.id) {
          $scope.files = response.logo ? [response.logo] : [];
          $scope.study.attachments =
            response.attachments && response.attachments.length > 0 ? response.attachments : [];

          if (response.maelstromAuthorization.date) {
            $scope.authorization.maelstrom.date =
              new Date(response.maelstromAuthorization.date.split('-').map(function (x) { return parseInt(x, 10);}));
          }
          if (response.specificAuthorization.date) {
            $scope.authorization.specific.date =
              new Date(response.specificAuthorization.date.split('-').map(function(x) { return parseInt(x, 10);}));
          }
        }
      }) : {attachments: [], maelstromAuthorization: {date: null}, specificAuthorization: {date: null}};

      $log.debug('Edit study', $scope.study);

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        $scope.languages = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
          $scope.languages.push(lang);
        });
      });

      $scope.save = function () {
        $scope.study.logo = $scope.files.length > 0 ? $scope.files[0] : null;

        if (!$scope.study.logo) { //protobuf doesnt like null values
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
        $scope.study.$save({comment:$scope.revision.comment},
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
