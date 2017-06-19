'use strict';
/* global processMemberships, STUDY_EVENTS, moment */

mica.study.ViewController = function (
  $scope,
  $rootScope,
  $location,
  $routeParams,
  $translate,
  $uibModal,
  $timeout,
  $filter,
  $q,
  $log,
  NOTIFICATION_EVENTS,
  CONTACT_EVENTS,
  EntityFormResource,
  LocalizedSchemaFormService,
  MicaConfigResource,
  DocumentPermissionsService,
  StudyStatesResource,
  DraftFileSystemSearchResource,
  DraftStudyResource,
  DraftStudyDeleteService,
  DraftStudyRevisionsResource,
  StudyUpdateWarningService,
  EntityPathBuilder) {

  mica.commons.ViewController.call(this, $location);

  var self = this;
  self.months = moment.months();
  self.languages = [];
  self.roles = [];
  self.populationSfForm = {};
  self.dceSfForm = {};

  self.toStatus = function (value) {
    DraftStudyResource.toStatus({id: $routeParams.id, value: value}, function () {
      $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, initializeState);
    });
  };

  self.delete = function (study) {
    DraftStudyDeleteService.delete(study, function() {
      $location.path('/study').replace();
    }, $translate.use());
  };

  self.publish = function (doPublish) {
    if (doPublish) {
      DraftFileSystemSearchResource.searchUnderReview({path: '/study/' + $routeParams.id},
      function onSuccess(response) {
        DraftStudyResource.publish({id: $routeParams.id, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
        function () {
          $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, initializeState);
        });
      },
      function onError() {
        $log.error('Failed to search for Under Review files.');
      });
    } else {
      DraftStudyResource.unPublish({id: $routeParams.id},
      function (response) {
        $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, initializeState);
        $log.debug(response);
      });
    }
  };

  self.emitStudyUpdated = function () {
    $scope.$emit(STUDY_EVENTS.studyUpdated, $scope.study);
  };

  function initializeForm() {
    $q.all([
      MicaConfigResource.get().$promise,
      EntityFormResource.get({target: 'study', locale: $translate.use()}).$promise,
      EntityFormResource.get({target: 'population', locale: $translate.use()}).$promise,
      EntityFormResource.get({target: 'data-collection-event', locale: $translate.use()}).$promise
    ]).then(function (data) {
      var micaConfig = data[0];
      var formLanguages = {};

      micaConfig.languages.forEach(function (loc) {
        formLanguages[loc] = $filter('translate')('language.' + loc);
      });

      self.languages = micaConfig.languages;
      self.roles = micaConfig.roles;
      self.openAccess = micaConfig.openAccess;
      self.sfOptions.formDefaults = {readonly: true, languages: formLanguages};
      self.sfForm = data[1];
      self.populationSfForm = data[2];
      self.dceSfForm = data[3];

      angular.extend($scope, self); // update scope
    });
  }

  function updateTimeline(study) {
    if (!$scope.timeline) {
      $scope.timeline = new $.MicaTimeline(new $.StudyDtoParser($translate.use()));
    }

    $timeout(function () {
      $scope.timeline.reset().create('#timeline', study).addLegend();
      $scope.sfForm = angular.copy(self.sfForm);
    }, 250);
  }

  function initializeStudy(study) {
    if (study.logo) {
      self.logoUrl = 'ws/draft/study/' + study.id + '/file/' + study.logo.id + '/_download';
    }

    if (self.getViewMode() === self.Mode.View || self.getViewMode() === self.Mode.Revision) {
      try {
        updateTimeline(study);
      } catch (e) {
        $log.warn(e);
      }
    }

    study.populations = study.populations || [];
    if (study.populations.length > 0) {
      $scope.selectedPopulation = study.populations[0];
    } else {
      $scope.selectedPopulation = undefined;
    }

    $scope.memberships = processMemberships(study);
  }

  function fetchStudy(id) {
    return DraftStudyResource.get({id: id}, initializeStudy);
  }

  function onRestore(event, args) {
    if (args.commitId) {
      DraftStudyRevisionsResource.restore({id: $routeParams.id, commitId: args.commitId}, function () {
        $scope.study = fetchStudy($routeParams.id);
        $scope.studyId = $routeParams.id;
        if (args.restoreSuccessCallback) {
          args.restoreSuccessCallback();
        }
      });
    }
  }

  function initializeState(state) {
    $scope.permissions = DocumentPermissionsService.state(state['obiba.mica.EntityStateDto.studySummaryState']);
  }

  if (self.getViewMode() !== self.Mode.Revision) {
    $scope.study = fetchStudy($routeParams.id);
  } else {
    $scope.studyId = $routeParams.id;
  }

  $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, initializeState);
  $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onRestore);
  $scope.$on(STUDY_EVENTS.studyUpdated, function (event, studyUpdated) {
    if (studyUpdated) {
      $log.debug('save study', studyUpdated);

      $scope.study.$save(function onSuccess(response) {
        $scope.study.content = $scope.study.model ? angular.toJson(response.study.model) : null;
        $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, initializeState);
        $scope.study = fetchStudy($routeParams.id);
      }, function onError(response) {
        $log.error('Error on study save:', response);
        if (response.status === 409) {
          StudyUpdateWarningService.popup(response, 'study.population-or-dce-delete-conflict', 'study.population-or-dce-delete-conflict-message', function () {
            $scope.study = fetchStudy($routeParams.id);
          });
        } else {
          $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
            message: response.data ? response.data : angular.fromJson(response)
          });
        }
      });
    }
  });

  $rootScope.$on('$translateChangeSuccess', function () { initializeForm(); });
  initializeForm();

  populationManagement($rootScope, $scope, $location, NOTIFICATION_EVENTS);
  populationDceManagement($rootScope, $scope, $location, $translate, $uibModal, EntityPathBuilder, NOTIFICATION_EVENTS);
  contactManagement($scope, $routeParams, CONTACT_EVENTS, fetchStudy);
  revisionManagement($rootScope, $scope, $filter, DraftStudyRevisionsResource, NOTIFICATION_EVENTS, initializeStudy);
};

mica.study.ViewController.prototype = Object.create(mica.commons.ViewController.prototype);
mica.study.ViewController.prototype.constructor = mica.study.ViewController;

function populationManagement($rootScope, $scope, $location, NOTIFICATION_EVENTS) {
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

  $scope.addPopulation = function () {
    $location.url($location.url() + '/population/add');
  };
}

function populationDceManagement($rootScope, $scope, $location, $translate, $uibModal, EntityPathBuilder, NOTIFICATION_EVENTS) {
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
}

function contactManagement($scope, $routeParams, CONTACT_EVENTS, fetchStudy) {
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
    if (study) {
      $scope.study = fetchStudy($routeParams.id);
    }
  });

  $scope.$on(CONTACT_EVENTS.contactDeleted, function (event, study, contact, type) {
    if (study === $scope.study) {
      var roleMemberships = $scope.study.memberships.filter(function (m) {
          return m.role === type;
        })[0] || {members: []}; // ?

      var idx = roleMemberships.members.indexOf(contact);

      if (idx !== -1) {
        roleMemberships.members.splice(idx, 1);
      }

      $scope.emitStudyUpdated();
    }
  });
}

function revisionManagement($rootScope, $scope, $filter, DraftStudyRevisionsResource, NOTIFICATION_EVENTS, initializeStudy) {
  $scope.fetchRevisions = function (id, onSuccess) {
    DraftStudyRevisionsResource.query({id: id}, function (response) {
      if (onSuccess) {
        onSuccess(response);
      }
    });
  };

  $scope.viewRevision = function (id, commitInfo) {
    $scope.commitInfo = commitInfo;
    $scope.study = DraftStudyRevisionsResource.view({id: id, commitId: commitInfo.commitId}, initializeStudy);
  };

  $scope.restoreRevision = function (id, commitInfo, onSuccess) {
    if (commitInfo && $scope.study.id === id) {
      var args = {commitId: commitInfo.commitId, restoreSuccessCallback: onSuccess};

      $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog, {
        titleKey: 'study.restore-dialog.title',
        messageKey: 'study.restore-dialog.message',
        messageArgs: [$filter('amDateFormat')(commitInfo.date, 'lll')]
      }, args);
    }
  };
}
