'use strict';

mica.study

  .constant('STUDY_EVENTS', {
    studyUpdated: 'event:study-updated'
  })

  .controller('StudyListController', [
    '$rootScope',
    '$scope',
    '$translate',
    '$interpolate',
    'StudyStatesResource',
    'DraftStudyResource',
    'NOTIFICATION_EVENTS',
    'DraftStudyDeleteService',
    function ($rootScope,
              $scope,
              $translate,
              $interpolate,
              StudyStatesResource,
              DraftStudyResource,
              NOTIFICATION_EVENTS,
              DraftStudyDeleteService) {

      var onSuccess = function(response) {
        $scope.studies = response;
        $scope.loading = false;
      };

      var onError = function() {
        $scope.loading = false;
      };

      $scope.loading = true;
      StudyStatesResource.query({}, onSuccess, onError);

      $scope.deleteStudy = function (study) {
        DraftStudyDeleteService.delete(study, function() {
          $scope.loading = true;
          StudyStatesResource.query({}, onSuccess, onError);
        });
      };
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
    '$timeout',
    'StudyStateResource',
    'DraftStudyResource',
    'DraftStudyPublicationResource',
    'DraftStudyStatusResource',
    'DraftStudyViewRevisionResource',
    'DraftStudyRevisionsResource',
    'DraftStudyRestoreRevisionResource',
    'DraftFileSystemSearchResource',
    'MicaConfigResource',
    'STUDY_EVENTS',
    'NOTIFICATION_EVENTS',
    'CONTACT_EVENTS',
    'StudyTaxonomyService',
    'ActiveTabService',
    '$modal',
    'DraftStudyDeleteService',
    'EntityPathBuilder',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              $translate,
              $filter,
              $timeout,
              StudyStateResource,
              DraftStudyResource,
              DraftStudyPublicationResource,
              DraftStudyStatusResource,
              DraftStudyViewRevisionResource,
              DraftStudyRevisionsResource,
              DraftStudyRestoreRevisionResource,
              DraftFileSystemSearchResource,
              MicaConfigResource,
              STUDY_EVENTS,
              NOTIFICATION_EVENTS,
              CONTACT_EVENTS,
              StudyTaxonomyService,
              ActiveTabService,
              $modal,
              DraftStudyDeleteService,
              EntityPathBuilder) {

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};

      var getViewMode = function() {
        var result = /\/(revision[s\/]*|files|permissions|comments)/.exec($location.path());
        if (result && result.length > 1) {
          switch (result[1]) {
            case 'revision':
            case 'revisions':
              return $scope.Mode.Revision;
            case 'files':
              return $scope.Mode.File;
            case 'permissions':
              return $scope.Mode.Permission;
            case 'comments':
              return $scope.Mode.Comment;
          }
        }

        return $scope.Mode.View;
      };

      $scope.viewMode = getViewMode();

      $scope.inViewMode = function () {
        return $scope.viewMode === $scope.Mode.View;
      };

      $scope.getActiveTab = ActiveTabService.getActiveTab;

      var updateTimeline = function (study) {
        if (!$scope.timeline) {
          $scope.timeline = new $.MicaTimeline(new $.StudyDtoParser());
        }

        $scope.timeline.reset().create('#timeline', study).addLegend();
      };

      var initializeStudy = function (study) {
        if (study.logo) {
          $scope.logoUrl = 'ws/draft/study/' + study.id + '/file/' + study.logo.id + '/_download';
        }

        if ($scope.viewMode === $scope.Mode.View || $scope.viewMode === $scope.Mode.Revision) {
          try {
            updateTimeline(study);
          } catch (e) {
            $log.warn(e);
          }
        }

        study.populations = study.populations || [];

        $scope.memberships = study.memberships.map(function (m) {
          if (!m.members) {
            m.members = [];
          }

          return m;
        }).reduce(function (res, m) {
          res[m.role] = m.members;
          return res;
        }, {});
      };

      var viewRevision = function (studyId, commitInfo) {
        $scope.commitInfo = commitInfo;
        $scope.study = DraftStudyViewRevisionResource.view({
          id: studyId,
          commitId: commitInfo.commitId
        }, initializeStudy);
      };

      $scope.memberships = {};

      var fetchStudy = function (studyId) {
        $scope.study = DraftStudyResource.get({id: studyId}, initializeStudy);
      };

      var fetchRevisions = function (studyId, onSuccess) {
        DraftStudyRevisionsResource.query({id: studyId}, function (response) {
          if (onSuccess) {
            onSuccess(response);
          }
        });
      };

      var restoreRevision = function (studyId, commitInfo, onSuccess) {
        if (commitInfo && $scope.studyId === studyId) {
          var args = {commitId: commitInfo.commitId, restoreSuccessCallback: onSuccess};

          $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {
              titleKey: 'study.restore-dialog.title',
              messageKey: 'study.restore-dialog.message',
              messageArgs: [$filter('amDateFormat')(commitInfo.date, 'lll')]
            }, args
          );
        }
      };

      var onRestore = function (event, args) {
        if (args.commitId) {
          DraftStudyRestoreRevisionResource.restore({id: $scope.studyId, commitId: args.commitId},
            function () {
              fetchStudy($routeParams.id);
              $scope.studyId = $routeParams.id;
              if (args.restoreSuccessCallback) {
                args.restoreSuccessCallback();
              }
            });
        }
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onRestore);

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
        $scope.roles = micaConfig.roles;
        $scope.openAccess = micaConfig.openAccess;
      });

      StudyTaxonomyService.get(function() {
        $scope.getLabel = StudyTaxonomyService.getLabel;
      });

      if ($scope.viewMode === $scope.Mode.Revision) {
        $scope.studyId = $routeParams.id;
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
              $scope.studySummary = StudyStateResource.get({id: $scope.study.id});
              fetchStudy($scope.study.id);
            },
            function (response) {
              $log.error('Error on study save:', response);
              $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
                message: response.data ? response.data : angular.fromJson(response)
              });
            });
        }
      });

      $scope.delete = function (study) {
        DraftStudyDeleteService.delete(study, function() {
          $location.path('/study').replace();
        }, ActiveTabService.getActiveTab($scope.tabs).lang);
      };

      $scope.publish = function (doPublish) {
        if (doPublish) {
          DraftFileSystemSearchResource.searchUnderReview({path: '/study/' + $scope.study.id},
            function onSuccess(response) {
              DraftStudyPublicationResource.publish(
                {id: $scope.study.id, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
                function () {
                  $scope.studySummary = StudyStateResource.get({id: $routeParams.id});
                });
            },
            function onError() {
              $log.error('Failed to search for Under Review files.');
            }
          );
        } else {
          DraftStudyPublicationResource.unPublish({id: $scope.study.id}, function () {
            $scope.studySummary = StudyStateResource.get({id: $routeParams.id});
          });
        }
      };

      $scope.toStatus = function (value) {
        DraftStudyStatusResource.toStatus({id: $scope.study.id, value: value}, function () {
          $scope.studySummary = StudyStateResource.get({id: $routeParams.id});
        });
      };

      $scope.isOrderingContacts = false; //prevent opening contact modal on reordering (firefox)

      $scope.sortableOptions = {
        start: function() {
          $scope.isOrderingContacts = true;
        },
        stop: function () {
          $scope.emitStudyUpdated();
          $timeout(function () {
            $scope.isOrderingContacts = false;
          }, 300);
        }
      };

      function updateExistingContact(contact, contacts) {
        var existingContact = contacts.filter(function (c) {
          return c.id === contact.id && !angular.equals(c, contact);
        })[0];

        if (existingContact) {
          angular.copy(contact, existingContact);
        }
      }

      $scope.$on(CONTACT_EVENTS.addContact, function (event, study, contact, type) {
        if (study === $scope.study) {
          var roleMemberships = $scope.study.memberships.filter(function(m) {
            if (m.role === type) {
              return true;
            }

            return false;
          })[0];

          if (!roleMemberships) {
            roleMemberships = {role: type, members: []};
            $scope.study.memberships.push(roleMemberships);
          }

          updateExistingContact(contact, roleMemberships.members || []);
          roleMemberships.members.push(contact);

          $scope.emitStudyUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactUpdated, function (event, study, contact, type) {
        var roleMemberships = $scope.study.memberships.filter(function(m) {
          if (m.role === type) {
            return true;
          }

          return false;
        })[0];

        updateExistingContact(contact, roleMemberships.members || []);

        if (study === $scope.study) {
          $scope.emitStudyUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactEditionCanceled, function (event, study) {
        if (study === $scope.study) {
          fetchStudy(study.id);
        }
      });

      $scope.$on(CONTACT_EVENTS.contactDeleted, function (event, study, contact, type) {
        if (study === $scope.study) {
          var roleMemberships = $scope.study.memberships.filter(function (m) {
              return m.role === type;
            })[0] || {members: []};

          var idx = roleMemberships.members.indexOf(contact);

          if (idx !== -1) {
            roleMemberships.members.splice(idx, 1);
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

        if (dce) {
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
            },
            path: function() {
              return {
                root: EntityPathBuilder.studyFiles(study),
                entity: EntityPathBuilder.dce(study, population, dce)
              };
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

  .controller('StudyPermissionsController', ['$scope','$routeParams', 'DraftStudyPermissionsResource', 'DraftStudyAccessesResource',
  function ($scope, $routeParams, DraftStudyPermissionsResource, DraftStudyAccessesResource) {
    $scope.permissions = [];
    $scope.accesses = [];

    $scope.loadPermissions = function () {
      $scope.permissions = DraftStudyPermissionsResource.query({id: $routeParams.id});
      return $scope.permissions;
    };

    $scope.deletePermission = function (permission) {
      return DraftStudyPermissionsResource.delete({id: $routeParams.id}, permission);
    };

    $scope.addPermission = function (permission) {
      return DraftStudyPermissionsResource.save({id: $routeParams.id}, permission);
    };

    $scope.loadAccesses = function () {
      $scope.accesses = DraftStudyAccessesResource.query({id: $routeParams.id});
      return $scope.accesses;
    };

    $scope.deleteAccess = function (access) {
      return DraftStudyAccessesResource.delete({id: $routeParams.id}, access);
    };

    $scope.addAccess = function (access) {
      return DraftStudyAccessesResource.save({id: $routeParams.id}, access);
    };
  }])

  .controller('StudyPopulationDceModalController', ['$scope', '$modalInstance', '$locale', '$location', 'dce', 'study', 'path',
    function ($scope, $modalInstance, $locale, $location, dce, study, path) {
      $scope.months = $locale.DATETIME_FORMATS.MONTH;
      $scope.dce = dce;
      $scope.study = study;
      $scope.path = path;

      $scope.cancel = function () {
        $modalInstance.close();
      };

      $scope.viewFiles = function () {
        $modalInstance.close();
        $location.path(path.root).search({p: path.entity});
      };
    }])

  .controller('StudyPopulationController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$location',
    '$log',
    'DraftStudyResource',
    'MicaConfigResource',
    'FormServerValidation',
    'MicaStudiesConfigResource',
    'StudyTaxonomyService',
    'MicaUtil',
    'ActiveTabService',
    'ObibaCountriesIsoCodes',
    function ($rootScope,
              $scope,
              $routeParams,
              $location,
              $log,
              DraftStudyResource,
              MicaConfigResource,
              FormServerValidation,
              MicaStudiesConfigResource,
              StudyTaxonomyService,
              MicaUtil,
              ActiveTabService,
              ObibaCountriesIsoCodes) {


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
            populationsIds = $scope.study.populations.map(function (p) {
              return p.id;
            });

            $scope.population.id = MicaUtil.generateNextId(populationsIds);
          }

          $scope.study.populations.push($scope.population);
        }
      }) : {};

      $scope.getActiveTab = ActiveTabService.getActiveTab;
      $scope.newPopulation = !$routeParams.pid;
      $scope.$watch('population.recruitment.dataSources', function (newVal, oldVal) {
        if (oldVal === undefined || newVal === undefined) {
          $scope.population.recruitment.dataSources = [];
          return;
        }

        updateActiveDatasourceTab(newVal, oldVal);
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

      StudyTaxonomyService.get(function() {
        var lang = ActiveTabService.getActiveTab($scope.tabs).lang;
        $scope.selectionCriteriaGenders = StudyTaxonomyService.getTerms('populations-selectionCriteria-gender', lang).map(function (obj) {
          return {id: obj.name, label: obj.label};
        });
        $scope.availableSelectionCriteria = StudyTaxonomyService.getTerms('populations-selectionCriteria-criteria', lang);
        $scope.recruitmentSourcesTypes = StudyTaxonomyService.getTerms('populations-recruitment-dataSources', lang);
        $scope.generalPopulationTypes = StudyTaxonomyService.getTerms('populations-recruitment-generalPopulationSources', lang);
        $scope.specificPopulationTypes = StudyTaxonomyService.getTerms('populations-recruitment-specificPopulationSources', lang);
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
          $scope.newDCE = !$routeParams.dceId;
          if ($routeParams.dceId) {
            $scope.dce = $scope.population.dataCollectionEvents.filter(function (d) {
              return d.id === $routeParams.dceId;
            })[0];
          } else {
            var sourceDceId = $location.search().sourceDceId;

            if ($scope.population.dataCollectionEvents === undefined) {
              $scope.population.dataCollectionEvents = [];
            }

            var dceIds = $scope.population.dataCollectionEvents.map(function (dce) {
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

        var dsFilter = function (d) {
          return d !== 'questionnaires' && d !== 'physical_measures';
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

  .controller('StudyFileSystemController', ['$scope', '$log', '$routeParams',
    function ($scope, $log, $routeParams) {
      $scope.documentInfo = {type: 'study', id: $routeParams.id};
      $log.info('>>>', $scope.documentInfo);
    }
  ])

  .controller('StudyEditController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$location',
    '$modal',
    'DraftStudyResource',
    'DraftStudiesResource',
    'MicaConfigResource',
    'StudyTaxonomyService',
    'StringUtils',
    'ActiveTabService',
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
              StudyTaxonomyService,
              StringUtils,
              ActiveTabService,
              FormServerValidation) {

      $scope.getActiveTab = ActiveTabService.getActiveTab;
      $scope.getActiveTab = ActiveTabService.getActiveTab;
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
      $scope.openDatePicker = function ($event, id) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.datePicker[id].opened = true;
      };

      // TODO ng-obiba's formCheckboxGroup directive must check for empty options array, once that's implemented we no
      // longer need to initialize these variables to empty arrays
      $scope.accessTypes = [];
      $scope.methodDesignTypes = [];
      $scope.methodRecruitmentTypes = [];
      StudyTaxonomyService.get(function() {
        var lang = ActiveTabService.getActiveTab($scope.tabs).lang;
        $scope.methodDesignTypes = StudyTaxonomyService.getTerms('methods-designs', lang);
        $scope.accessTypes = StudyTaxonomyService.getTerms('access', lang);
        $scope.methodRecruitmentTypes = StudyTaxonomyService.getTerms('methods-recruitments', lang);
      });

      $scope.defaultMinYear = 1900;
      $scope.defaultMaxYear = 9999;
      $scope.fileTypes = '.doc, .docx, .odm, .odt, .gdoc, .pdf, .txt  .xml  .xls, .xlsx, .ppt';
      $scope.files = [];
      $scope.newStudy = !$routeParams.id;
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
              new Date(response.specificAuthorization.date.split('-').map(function (x) { return parseInt(x, 10);}));
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
        $scope.study.$save({comment: $scope.revision.comment},
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
