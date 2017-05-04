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
    '$rootScope',
    '$scope',
    '$translate',
    '$interpolate',
    '$timeout',
    'StudyStatesResource',
    'DraftStudyResource',
    'NOTIFICATION_EVENTS',
    'DraftStudyDeleteService',
    'AlertBuilder',
    function ($rootScope,
              $scope,
              $translate,
              $interpolate,
              $timeout,
              StudyStatesResource,
              DraftStudyResource,
              NOTIFICATION_EVENTS,
              DraftStudyDeleteService,
              AlertBuilder) {

      var onSuccess = function(response, responseHeaders) {
        $scope.totalCount = parseInt(responseHeaders('X-Total-Count'), 10);
        $scope.studies = response;
        $scope.loading = false;

        if (!$scope.hasStudies) {
          $scope.hasStudies = $scope.totalCount && !$scope.pagination.searchText;
        }
      };

      var onError = function() {
        $scope.loading = false;
      };

      function refreshPage() {
        if($scope.pagination.current !== 1) {
          $scope.pagination.current = 1; //pageChanged event triggers reload
        } else {
          loadPage(1);
        }
      }

      $scope.pageChanged = function(page) {
        loadPage(page);
      };

      function loadPage(page) {
        var data = {from:(page - 1) * $scope.limit, limit: $scope.limit};

        if($scope.pagination.searchText) {
          data.query = $scope.pagination.searchText + '*';
        }

        StudyStatesResource.query(data, onSuccess, AlertBuilder.newBuilder().onError(onError));
      }

      $scope.loading = true;
      $scope.hasStudies = false;
      $scope.pagination = {current: 1, searchText: ''};
      $scope.totalCount = 0;
      $scope.limit = 20;

      $scope.deleteStudy = function (study) {
        DraftStudyDeleteService.delete(study, function() {
          refreshPage();
        });
      };

      var currentSearch = null;

      $scope.$watch('pagination.searchText', function(newVal, oldVal) {
        if (!newVal && !oldVal) {
          return;
        }

        if(currentSearch) {
          $timeout.cancel(currentSearch);
        }

        currentSearch = $timeout(function() {
          refreshPage();
        }, 500);
      });

      loadPage($scope.pagination.current);
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
    'EntityFormResource',
    'LocalizedSchemaFormService',
    'MicaConfigResource',
    'STUDY_EVENTS',
    'NOTIFICATION_EVENTS',
    'CONTACT_EVENTS',
    'StudyTaxonomyService',
    '$uibModal',
    'DraftStudyDeleteService',
    'EntityPathBuilder',
    'DocumentPermissionsService',
    'moment',

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
              EntityFormResource,
              LocalizedSchemaFormService,
              MicaConfigResource,
              STUDY_EVENTS,
              NOTIFICATION_EVENTS,
              CONTACT_EVENTS,
              StudyTaxonomyService,
              $uibModal,
              DraftStudyDeleteService,
              EntityPathBuilder,
              DocumentPermissionsService,
              moment) {


      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          $scope.months = moment.months();

          var formLanguages = {};
          micaConfig.languages.forEach(function (loc) {
            formLanguages[loc] = $filter('translate')('language.' + loc);
          });
          $scope.sfOptions = {formDefaults: {
            readonly: true,
            languages: formLanguages
          }};
          $scope.sfOptions.pristine = {errors: true, success: false};

          $scope.languages = micaConfig.languages;
          $scope.roles = micaConfig.roles;
          $scope.openAccess = micaConfig.openAccess;

          EntityFormResource.get({target: 'study', locale: $translate.use()}, function(form) {
            form.schema = angular.fromJson(form.schema);
            form.definition = angular.fromJson(form.definition);
            $scope.sfForm = form;
          });

          EntityFormResource.get({target: 'population', locale: $translate.use()}, function(form) {
            form.schema = angular.fromJson(form.schema);
            form.definition = angular.fromJson(form.definition);
            $scope.populationSfForm = form;
          });

          EntityFormResource.get({target: 'data-collection-event', locale: $translate.use()}, function(form) {
            form.schema = angular.fromJson(form.schema);
            form.definition = angular.fromJson(form.definition);
            $scope.dceSfForm = form;
          });
        });
      }

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};

      $scope.selectedLocale = $translate.use();
      $scope.$on('sfLocalizedStringLocaleChanged', function (event, locale) {
        $scope.selectedLocale = locale;
      });

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

      var updateTimeline = function (study) {
        if (!$scope.timeline) {
          $scope.timeline = new $.MicaTimeline(new $.StudyDtoParser($scope.selectedLocale));
        }

        $timeout(function () {
          $scope.timeline.reset().create('#timeline', study).addLegend();
          $scope.sfForm = angular.copy($scope.sfForm);
        }, 250);
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
        if (study.populations.length>0) {
          $scope.selectedPopulation = study.populations[0];
        } else {
          $scope.selectedPopulation = undefined;
        }
        study.memberships = study.memberships || [];

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

      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();

      StudyTaxonomyService.get(function() {
        $scope.getLabel = StudyTaxonomyService.getLabel;
      });

      if ($scope.viewMode === $scope.Mode.Revision) {
        $scope.studyId = $routeParams.id;
      } else {
        fetchStudy($routeParams.id);
      }

      var initializeState = function(studyState) {
        $scope.permissions = DocumentPermissionsService.state(studyState['obiba.mica.EntityStateDto.studySummaryState']);
      };

      $scope.fetchStudy = fetchStudy;
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.fetchRevisions = fetchRevisions;
      $scope.studySummary = StudyStateResource.get({id: $routeParams.id}, initializeState);

      $scope.print = function () {
        setTimeout(function(){ window.print();}, 250);
      };

      $scope.emitStudyUpdated = function () {
        $scope.$emit(STUDY_EVENTS.studyUpdated, $scope.study);
      };

      $scope.$on(STUDY_EVENTS.studyUpdated, function (event, studyUpdated) {
        if (studyUpdated === $scope.study) {
          $log.debug('save study', studyUpdated);
          $scope.study.$save(function () {
              $scope.study.content = $scope.study.model ? angular.toJson($scope.study.model) : null;
              $scope.studySummary = StudyStateResource.get({id: $scope.study.id}, initializeState);
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
        }, $translate.use());
      };

      $scope.publish = function (doPublish) {
        if (doPublish) {
          DraftFileSystemSearchResource.searchUnderReview({path: '/study/' + $scope.study.id},
            function onSuccess(response) {
              DraftStudyPublicationResource.publish(
                {id: $scope.study.id, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
                function () {
                  $scope.studySummary = StudyStateResource.get({id: $routeParams.id}, initializeState);
                });
            },
            function onError() {
              $log.error('Failed to search for Under Review files.');
            }
          );
        } else {
          DraftStudyPublicationResource.unPublish({id: $scope.study.id}, function () {
            $scope.studySummary = StudyStateResource.get({id: $routeParams.id}, initializeState);
          });
        }
      };

      $scope.toStatus = function (value) {
        DraftStudyStatusResource.toStatus({id: $scope.study.id, value: value}, function () {
          $scope.studySummary = StudyStateResource.get({id: $routeParams.id}, initializeState);
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
            return m.role === type;
          })[0];

          if (!roleMemberships) {
            roleMemberships = {role: type, members: []};
            $scope.study.memberships.push(roleMemberships);
          }

          var members = $scope.study.memberships.map(function(m) {
            return m.members;
          });

          updateExistingContact(contact, [].concat.apply([], members) || []);
          roleMemberships.members.push(contact);

          $scope.emitStudyUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactUpdated, function (event, study, contact) {
        var members = $scope.study.memberships.map(function(m) {
          return m.members;
        });

        updateExistingContact(contact, [].concat.apply([], members) || []);

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

      $scope.selectPopulation = function (population) {
        $scope.selectedPopulation = population;
      };

      $scope.editPopulation = function (study, population) {
        $location.url($location.url() + '/population/' + population.id + '/edit');
      };

      $scope.deletePopulation = function (study, population) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {title: 'Delete population', message: 'Are you sure to delete the population?'}, population);

        $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, population) {
          $scope.study.populations = $scope.study.populations.filter(function(pop) {
            return pop.id !== population.id;
          });
          $scope.emitStudyUpdated();
        });
      };

      $scope.addDataCollectionEvent = function (study, population, dce) {
        $location.url($location.path() + '/population/' + population.id + '/dce/add');

        if (dce) {
          $location.search('sourceDceId', dce.id);
        }
      };

      $scope.showDataCollectionEvent = function (study, population, dce) {
        $uibModal.open({
          templateUrl: 'app/study/views/population/dce/data-collection-event-view.html',
          controller: 'StudyPopulationDceModalController',
          resolve: {
            lang: function() {
              return $scope.selectedLocale;
            },
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
            },
            sfOptions: function() {
              return $scope.sfOptions;
            },
            sfForm: function() {
              return $scope.dceSfForm;
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

  .controller('StudyPopulationController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$location',
    '$log',
    '$filter',
    '$translate',
    'DraftStudyResource',
    'EntityFormResource',
    'LocalizedSchemaFormService',
    'MicaConfigResource',
    'FormServerValidation',
    'StudyTaxonomyService',
    'MicaUtil',
    'SfOptionsService',
    function ($rootScope,
              $scope,
              $routeParams,
              $location,
              $log,
              $filter,
              $translate,
              DraftStudyResource,
              EntityFormResource,
              LocalizedSchemaFormService,
              MicaConfigResource,
              FormServerValidation,
              StudyTaxonomyService,
              MicaUtil,
              SfOptionsService) {


      $scope.selectionCriteriaGenders = {};
      $scope.population = {model: {}};
      $scope.revision = {comment: null};

      $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function (study) {

        var populationsIds;

        if ($routeParams.pid) {
          $scope.population = study.populations.filter(function (p) {
            return p.model._id === $routeParams.pid;
          })[0];

        } else {
          if (!study.populations) {
            $scope.study.populations = [];
          }

          if (study.populations.length) {
            populationsIds = study.populations.map(function (p) {
              return p.model._id;
            });

            $scope.population.model._id = MicaUtil.generateNextId(populationsIds);
          }

          study.populations.push($scope.population);
        }
      }) : {};

      $scope.newPopulation = !$routeParams.pid;

      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          var formLanguages = {};
          micaConfig.languages.forEach(function (loc) {
            formLanguages[loc] = $filter('translate')('language.' + loc);
          });
          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.formDefaults = {languages: formLanguages};
          });

          EntityFormResource.get({target: 'population', locale: $translate.use()}, function (form) {
            form.schema = angular.fromJson(form.schema);
            form.definition = angular.fromJson(form.definition);
            $scope.populationSfForm = form;
          });
        });
      }


      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();

      $scope.save = function (form) {
        $scope.$broadcast('schemaFormValidate');

        if (!validate(form)) {
          form.saveAttempted = true;
          return;
        }

        updateStudy();
      };

      $scope.cancel = function () {
        redirectToStudy();
      };

      var updateStudy = function () {
        $log.debug('Update study', $scope.study);
        $scope.study.$save({comment: $scope.revision.comment}, redirectToStudy, saveErrorHandler);
      };

      var validate = function (form) {
        if ($scope.study.populations.filter(function (p) {
            return p.model._id === $scope.population.model._id;
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

  .controller('StudyPopulationDceController', [
    '$rootScope',
    '$scope',
    '$routeParams',
    '$location',
    '$log',
    '$filter',
    '$translate',
    'DraftStudyResource',
    'EntityFormResource',
    'LocalizedSchemaFormService',
    'MicaConfigResource',
    'FormServerValidation',
    'MicaUtil',
    'StudyTaxonomyService',
    'SfOptionsService',
    function ($rootScope,
              $scope,
              $routeParams,
              $location,
              $log,
              $filter,
              $translate,
              DraftStudyResource,
              EntityFormResource,
              LocalizedSchemaFormService,
              MicaConfigResource,
              FormServerValidation,
              MicaUtil,
              StudyTaxonomyService,
              SfOptionsService
    ) {
      $scope.dce = {model: {}};
      $scope.revision = {comment: null};
      $scope.fileTypes = '.doc, .docx, .odm, .odt, .gdoc, .pdf, .txt  .xml  .xls, .xlsx, .ppt';
      $scope.defaultMinYear = 1900;
      $scope.defaultMaxYear = new Date().getFullYear() + 200;
      $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function (study) {

        if ($routeParams.pid) {
          $scope.population = study.populations.filter(function (p) {
            return p.model._id === $routeParams.pid;
          })[0];

          $scope.newDCE = !$routeParams.dceId;

          if ($routeParams.dceId) {
            $scope.dce = $scope.population.dataCollectionEvents.filter(function (d) {
              return d.model._id === $routeParams.dceId;
            })[0];
          } else {
            var sourceDceId = $location.search().sourceDceId;

            if ($scope.population.dataCollectionEvents === undefined) {
              $scope.population.dataCollectionEvents = [];
            }

            var dceIds = $scope.population.dataCollectionEvents.map(function (dce) {
              return dce.model._id;
            });

            if (sourceDceId) {
              var sourceDce = $scope.population.dataCollectionEvents.filter(function (dce) {
                return dce.model._id === sourceDceId;
              })[0];

              if (sourceDce) {
                angular.copy(sourceDce, $scope.dce);
                $scope.dce.model._id = MicaUtil.generateNextId(dceIds);
                delete $scope.dce.attachments;
                delete $scope.dce.startYear;
                delete $scope.dce.startMonth;
                delete $scope.dce.endYear;
                delete $scope.dce.endMonth;
              }
            } else if (dceIds.length) {
              $scope.dce.model._id = MicaUtil.generateNextId(dceIds);
            }

            $scope.population.dataCollectionEvents.push($scope.dce);
          }
          $scope.attachments =
            $scope.dce.attachments && $scope.dce.attachments.length > 0 ? $scope.dce.attachments : [];

          StudyTaxonomyService.get(function() {
            var lang = $translate.use();
            $scope.dataSources = StudyTaxonomyService.getTerms('populations-dataCollectionEvents-dataSources', lang);
            $scope.bioSamples = StudyTaxonomyService.getTerms('populations-dataCollectionEvents-bioSamples', lang);
            $scope.administrativeDatabases = StudyTaxonomyService.getTerms('populations-dataCollectionEvents-administrativeDatabases', lang);
          });

        } else {
          // TODO add error popup
          $log.error('Failed to retrieve population.');
        }
      }) : {};

      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          var sfLanguages = {};
          micaConfig.languages.forEach(function (lang) {
            sfLanguages[lang] = $filter('translate')('language.' + lang);
          });

          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.formDefaults = { languages: sfLanguages };
          });

          EntityFormResource.get({target: 'study'}, function (form) {
            form.schema = LocalizedSchemaFormService.translate(angular.fromJson(form.schema));
            form.definition = LocalizedSchemaFormService.translate(angular.fromJson(form.definition));
            $scope.sfForm = form;
          });

          EntityFormResource.get({target: 'data-collection-event', locale: $translate.use()}, function (form) {
            form.schema = angular.fromJson(form.schema);
            form.definition = angular.fromJson(form.definition);
            $scope.dceSfForm = form;
          });

        });
      }

      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();

      $scope.cancel = function () {
        redirectToStudy();
      };

      $scope.save = function (form) {
        $scope.dce.attachments = $scope.attachments === undefined || $scope.attachments.length > 0 ? $scope.attachments : null;

        if (!$scope.dce.attachments) { //protobuf doesnt like null values
          delete $scope.dce.attachments;
        }

        $scope.$broadcast('schemaFormValidate');

        if (!validate(form)) {
          form.saveAttempted = true;
          return;
        }

        updateStudy();
      };

      var validate = function (form) {
        if ($scope.population.dataCollectionEvents.filter(function (d) {
            return d.model._id === $scope.dce.model._id;
          }).length > 1) {
          form.$setValidity('dce_id', false);
        } else {
          form.$setValidity('dce_id', true);
        }

        return form.$valid;
      };

      var updateStudy = function () {
        $log.info('Update study', $scope.study);
        $scope.study.$save({comment: $scope.revision.comment}, redirectToStudy, saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

      var redirectToStudy = function () {
        $location.search('sourceDceId', null);
        $location.path('/study/' + $scope.study.id).replace();
      };

    }])

  .controller('StudyFileSystemController', ['$scope', '$log', '$routeParams',
    function ($scope, $log, $routeParams) {
      $scope.documentInfo = {type: 'study', id: $routeParams.id};
      $log.debug($scope.documentInfo);
    }
  ])

  .controller('StudyEditController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$filter',
    '$location',
    '$uibModal',
    'DraftStudyResource',
    'DraftStudiesResource',
    'EntityFormResource',
    'LocalizedSchemaFormService',
    'MicaConfigResource',
    'StudyTaxonomyService',
    'StringUtils',
    'FormServerValidation',
    'RadioGroupOptionBuilder',
    'FormDirtyStateObserver',
    'SfOptionsService',
    '$timeout',
    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $filter,
              $location,
              $uibModal,
              DraftStudyResource,
              DraftStudiesResource,
              EntityFormResource,
              LocalizedSchemaFormService,
              MicaConfigResource,
              StudyTaxonomyService,
              StringUtils,
              FormServerValidation,
              RadioGroupOptionBuilder,
              FormDirtyStateObserver,
              SfOptionsService,
              $timeout) {

      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          var sfLanguages = {};
          micaConfig.languages.forEach(function (lang) {
            sfLanguages[lang] = $filter('translate')('language.' + lang);
          });
          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.formDefaults = {languages: sfLanguages};
          });

          EntityFormResource.get({target: 'study'}, function (form) {
            form.schema = LocalizedSchemaFormService.translate(angular.fromJson(form.schema));
            form.definition = LocalizedSchemaFormService.translate(angular.fromJson(form.definition));
            $scope.sfForm = form;

            $timeout(function () { $scope.sfForm = angular.copy(form); }, 250);
          });
        });
      }

      function createNewStudy() {
        return {attachments: [], maelstromAuthorization: {date: null}, specificAuthorization: {date: null}, model: {}};
      }

      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();

      $scope.revision = {comment: null};
      $scope.today = new Date();
      $scope.authorization = {maelstrom: {date: $scope.today}, specific: {date: $scope.today}};
      $scope.datePicker = {maelstrom: {opened: false}, specific: {opened: false}};
      $scope.openDatePicker = function ($event, id) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.datePicker[id].opened = true;
      };

      // TODO ng-obiba's formCheckboxGroup directive must check for empty options array, once that's implemented we no
      // longer need to initialize these variables to empty arrays
      $scope.accessTypes = {};
      $scope.methodDesignTypes = {};
      $scope.methods= {};
      $scope.methodRecruitmentTypes = {};
      $scope.defaultMinYear = 1900;
      $scope.defaultMaxYear = 9999;
      $scope.fileTypes = '.doc, .docx, .odm, .odt, .gdoc, .pdf, .txt  .xml  .xls, .xlsx, .ppt';
      $scope.files = [];
      $scope.newStudy = !$routeParams.id;
      $scope.study = $routeParams.id ? DraftStudyResource.get({id: $routeParams.id}, function (study) {

        $scope.files = study.logo ? [study.logo] : [];
        $scope.study.attachments =
          study.attachments && study.attachments.length > 0 ? study.attachments : [];
      }) : createNewStudy();


      $scope.save = function () {
        $scope.study.logo = $scope.files.length > 0 ? $scope.files[0] : null;

        if (!$scope.study.logo) { //protobuf doesnt like null values
          delete $scope.study.logo;
        }

        $scope.$broadcast('schemaFormValidate');

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
            FormDirtyStateObserver.unobserve();
            var parts = getResponseHeaders().location.split('/');
            $location.path('/study/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var updateStudy = function () {
        $log.debug('Update study', $scope.study);
        $scope.study.$save({comment: $scope.revision.comment},
          function (study) {
            FormDirtyStateObserver.unobserve();
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

      FormDirtyStateObserver.observe($scope);
    }]);
