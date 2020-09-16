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
/* global processMemberships, STUDY_EVENTS, moment */
/* global obiba, location */

/**
 * Basic study view controller class
 * Must be overridden before use
 *
 * @param $scope
 * @param $location
 * @param $routeParams
 * @param DocumentPermissionsService
 * @param StudyStatesResource
 * @param DraftStudyResource
 * @param DraftStudyRevisionsResource
 * @constructor
 */
mica.study.BaseViewController = function (
  $scope,
  $location,
  $routeParams,
  DocumentPermissionsService,
  StudyStatesResource,
  DraftStudyResource,
  DraftStudyRevisionsResource) {

  mica.commons.ViewController.call(this, $location);

  $scope.contextPath = contextPath;

  var self = this;
  self.months = moment.months();
  self.languages = [];
  self.roles = [];

  self.toStatus = function (value) {
    DraftStudyResource.toStatus({id: $routeParams.id, value: value}, function () {
      $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
    });
  };

  /* jshint unused:vars */
  self.delete = function (study) { };

  /* jshint unused:vars */
  self.publish = function (doPublish) { };

  self.emitStudyUpdated = function () {
    $scope.$emit(STUDY_EVENTS.studyUpdated, $scope.study);
  };

  self.initializeForm = function () { };

  self.isOrderingContacts = false; //prevent opening contact modal on reordering (firefox)
  self.sortableOptions = {
    start: function() {
      self.isOrderingContacts = true;
    },
    stop: function () {
      self.emitStudyUpdated();
      self.isOrderingContacts = false;
    }
  };

  /* jshint unused:vars */
  self.initializeStudy = function (study) { };

  self.fetchStudy = function (id) {
    return DraftStudyResource.get({id: id}, self.initializeStudy);
  };

  self.initializeState = function (state) {
    $scope.permissions = DocumentPermissionsService.state(state['obiba.mica.EntityStateDto.studySummaryState']);
  };

  self.onRestore = function (event, args) {
    if (args.commitId) {
      DraftStudyRevisionsResource.restore({id: $routeParams.id, commitId: args.commitId}, function () {
        $scope.study = self.fetchStudy($routeParams.id);
        $scope.studyId = $routeParams.id;
        if (args.restoreSuccessCallback) {
          args.restoreSuccessCallback();
        }
      });
    }
  };

  self.updateCollectionItemWeight = function (item, newWeight, collection) {
    function weightSortComparator(a, b) {
      var aWeight = a.weight || 0, bWeight = b.weight || 0;
      return aWeight - bWeight;
    }

    collection.sort(weightSortComparator);
    var itemIndex = collection.indexOf(item);

    if (itemIndex > -1) {
      item = collection[collection.indexOf(item)];

      if (collection[newWeight]) {
        collection[newWeight].weight = item.weight;
      }

      collection.forEach(function (collectionItem) { collectionItem._active = false; });
      item._active = true;

      item.weight = newWeight;

      collection.sort(weightSortComparator).forEach(function (collectionItem, index) {
        collectionItem.weight = index;
      });

      DraftStudyResource.save({weightChanged: true}, $scope.study,
        function () {
          $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
        },
        function () {
          $scope.fetchStudy($scope.study.id);
        }
      );
    }
  };
};

mica.study.BaseViewController.prototype = Object.create(mica.commons.ViewController.prototype);
mica.study.BaseViewController.prototype.constructor = mica.study.BaseViewController;
mica.study.BaseEditController.prototype.getConflictMessageKey = function () {
  return 'study.delete-conflict-message';
};


/**
 * Individual Study view controller
 *
 * @param $scope
 * @param $rootScope
 * @param $location
 * @param $routeParams
 * @param $translate
 * @param $uibModal
 * @param $timeout
 * @param $filter
 * @param $q
 * @param $log
 * @param NOTIFICATION_EVENTS
 * @param CONTACT_EVENTS
 * @param EntityFormResource
 * @param LocalizedSchemaFormService
 * @param MicaConfigResource
 * @param DocumentPermissionsService
 * @param StudyStatesResource
 * @param DraftFileSystemSearchResource
 * @param DraftStudyResource
 * @param DraftStudyDeleteService
 * @param DraftStudyRevisionsResource
 * @param StudyUpdateWarningService
 * @param EntityPathBuilder
 * @constructor
 */
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

  mica.study.BaseViewController.call(this, $scope, $location, $routeParams, DocumentPermissionsService, StudyStatesResource, DraftStudyResource, DraftStudyRevisionsResource);

  var self = this;
  self.populationSfForm = {};
  self.dceSfForm = {};

  self.delete = function (study) {
    DraftStudyDeleteService.delete(study, function() {
      $location.path('/individual-study').replace();
    }, $translate.use());
  };

  self.publish = function (doPublish) {
    if (doPublish) {
      DraftFileSystemSearchResource.searchUnderReview({path: '/individual-study/' + $routeParams.id},
      function onSuccess(response) {
        DraftStudyResource.publish({id: $routeParams.id, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
        function () {
          $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
        });
      },
      function onError() {
        $log.error('Failed to search for Under Review files.');
      });
    } else {
      DraftStudyResource.unPublish({id: $routeParams.id},
      function () {
        $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
      },
      function onError(response){
        if (response.status === 409) {
          StudyUpdateWarningService.popup(response.data, 'study.unpublish-conflicts', 'study.unpublish-conflicts-message');
        } else {
          $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
            titleKey: 'form-server-error',
            message: angular.toJson(response)
          });
        }
      });
    }
  };

  self.initializeForm = function () {
    $q.all([
      MicaConfigResource.get().$promise,
      EntityFormResource.get({target: 'individual-study', locale: $translate.use()}).$promise,
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
  };

  self.initializeStudy = function (study) {
    if (study.logo) {
      $scope.logoUrl = contextPath + '/ws/draft/individual-study/' + study.id + '/file/' + study.logo.id + '/_download';
    }

    safeUpdateStudyTimeline(study);

    study.populations = study.populations || [];
    if (study.populations.length > 0) {
      $scope.selectedPopulation = study.populations.sort(function (a, b) { return a.weight - b.weight; })[0];
    } else {
      $scope.selectedPopulation = undefined;
    }

    $scope.memberships = processMemberships(study);
  };

  self.setOrder = function(newOrder) {
    $scope.study.membershipSortOrder = newOrder;
    $scope.emitStudyUpdated();
  };

  $scope.$watch('studySummary', function () {
    safeUpdateStudyTimeline($scope.study);
  });

  function safeUpdateStudyTimeline(study) {
    if (self.getViewMode() === self.Mode.View || self.getViewMode() === self.Mode.Revision) {
      try {
        updateTimeline(study);
      } catch (e) {
        $log.warn(e);
      }
    }
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

  if (self.getViewMode() !== self.Mode.Revision) {
    $scope.study = self.fetchStudy($routeParams.id);
  } else {
    $scope.studyId = $routeParams.id;
  }

  $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
  $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, self.onRestore);
  $scope.$on(STUDY_EVENTS.studyUpdated, function (event, studyUpdated) {
    if (studyUpdated) {
      $scope.study.$save(function onSuccess(response) {
        $scope.study.content = $scope.study.model ? angular.toJson(response.study.model) : null;
        $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
        $scope.study = self.fetchStudy($routeParams.id);
      }, function onError(response) {
        $log.error('Error on study save:', response);
        if (response.status === 409) {
          StudyUpdateWarningService.popup(response, 'study.delete-conflict', 'study.population-or-dce-delete-conflict-message', function () {
            $scope.study = self.fetchStudy($routeParams.id);
          });
        } else {
          $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
            message: response.data ? response.data : angular.fromJson(response)
          });
        }
      });
    }
  });

  $rootScope.$on('$translateChangeSuccess', function () { self.initializeForm(); });
  self.initializeForm();

  populationManagement($rootScope, $scope, $location, NOTIFICATION_EVENTS);
  populationDceManagement($rootScope, $scope, $location, $translate, $uibModal, EntityPathBuilder, NOTIFICATION_EVENTS);
  contactManagement($scope, $routeParams, CONTACT_EVENTS, self.fetchStudy);
  revisionManagement($rootScope, $scope, $location, $filter, $translate, DraftStudyRevisionsResource, NOTIFICATION_EVENTS, self.initializeStudy, DraftStudyResource);
};

mica.study.ViewController.prototype = Object.create(mica.study.BaseViewController.prototype);
mica.study.ViewController.prototype.constructor = mica.study.ViewController;

function populationManagement($rootScope, $scope, $location, NOTIFICATION_EVENTS) {
  $scope.selectPopulation = function (population) {
    $scope.selectedPopulation = population;
  };

  $scope.editPopulation = function (study, population) {
    $location.url($location.url() + '/population/' + population.id + '/edit');
  };

  var listenerRegistry = new obiba.utils.EventListenerRegistry();
  $scope.deletePopulation = function (study, population) {
    $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
      {titleKey: 'population.delete-dialog.title', messageKey: 'population.delete-dialog.message'}, population);
    listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogRejected, function () {
      listenerRegistry.unregisterAll();
    }));
    listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, population) {
      listenerRegistry.unregisterAll();
      $scope.study.populations = $scope.study.populations.filter(function(pop) {
        return pop.id !== population.id;
      });
      $scope.emitStudyUpdated();
    }));
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
        },
        canViewFiles: function() {
          return !$scope.inRevisionMode() && $scope.permissions.canEdit();
        }
      }
    });
  };

  $scope.editDataCollectionEvent = function (study, population, dce) {
    $location.url($location.url() + '/population/' + population.id + '/dce/' + dce.id + '/edit');
  };

  var listenerRegistry = new obiba.utils.EventListenerRegistry();
  $scope.deleteDataCollectionEvent = function (population, dce) {
    var titleKey = 'data-collection-event.delete-dialog.title';
    var messageKey = 'data-collection-event.delete-dialog.message';
    $translate([titleKey, messageKey])
      .then(function (translation) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {title: translation[titleKey], message: translation[messageKey]}, {dce: dce, population: population});
      });

    listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogRejected, function () {
      listenerRegistry.unregisterAll();
    }));
    listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, data) {
      listenerRegistry.unregisterAll();
      var popIndex = $scope.study.populations.indexOf(data.population);
      if (popIndex > -1) {
        var dceIndex = data.population.dataCollectionEvents.indexOf(data.dce);
        if (dceIndex > -1) {
          data.population.dataCollectionEvents.splice(dceIndex, 1);
          $scope.emitStudyUpdated();
        }
      }
    }));
  };


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

function revisionManagement($rootScope, $scope, $location, $filter, $translate, DraftStudyRevisionsResource, NOTIFICATION_EVENTS, initializeStudy, DraftStudyResource) {
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

  $scope.viewDiff = function (id, leftCommitInfo, rightCommitInfo) {
    if (leftCommitInfo && rightCommitInfo) {
      return DraftStudyRevisionsResource.diff({id: id, left: leftCommitInfo.commitId, right: rightCommitInfo.commitId, locale: $translate.use()});
    }
  };

  $scope.restoreFromFields = function (transformFn) {
    DraftStudyResource.rGet({id: $scope.studyId}).$promise.then(function (study) {
      return transformFn(study.toJSON());
    }).then(function (result) {
      DraftStudyResource.rSave({id: $scope.studyId, comment: 'Restored Fields'}, result).$promise.then(function () {
        location.reload();
      }).catch(function (response) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
          message: response.data ? response.data : angular.fromJson(response)
        });
      });

      return result;
    });
  };
}

/**
 * Harmonization Study view controller
 *
 * @param $scope
 * @param $rootScope
 * @param $location
 * @param $routeParams
 * @param $translate
 * @param $uibModal
 * @param $timeout
 * @param $filter
 * @param $q
 * @param $log
 * @param NOTIFICATION_EVENTS
 * @param CONTACT_EVENTS
 * @param EntityFormResource
 * @param LocalizedSchemaFormService
 * @param MicaConfigResource
 * @param DocumentPermissionsService
 * @param StudyStatesResource
 * @param DraftFileSystemSearchResource
 * @param DraftStudyResource
 * @param DraftStudyDeleteService
 * @param DraftStudyRevisionsResource
 * @param StudyUpdateWarningService
 * @param EntityPathBuilder
 * @constructor
 */
mica.study.HarmonizationStudyViewController = function (
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
  StudyUpdateWarningService) {

  mica.study.BaseViewController.call(this, $scope, $location, $routeParams, DocumentPermissionsService, StudyStatesResource, DraftStudyResource, DraftStudyRevisionsResource);

  var self = this;
  self.populationSfForm = {};

  self.delete = function (study) {
    DraftStudyDeleteService.delete(study, function() {
      $location.path('/harmonization-study').replace();
    }, $translate.use());
  };

  self.publish = function (doPublish) {
    if (doPublish) {
      DraftFileSystemSearchResource.searchUnderReview({path: '/harmonization-study/' + $routeParams.id},
        function onSuccess(response) {
          DraftStudyResource.publish({id: $routeParams.id, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
            function () {
              $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
            });
        },
        function onError() {
          $log.error('Failed to search for Under Review files.');
        });
    } else {
      DraftStudyResource.unPublish({id: $routeParams.id},
      function () {
        $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
      },
      function onError(response){
        if (response.status === 409) {
          StudyUpdateWarningService.popup(response.data, 'study.unpublish-conflicts', 'study.unpublish-conflicts-message');
        } else {
          $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
            titleKey: 'form-server-error',
            message: angular.toJson(response)
          });
        }
      });
    }
  };

  self.initializeForm = function () {
    $q.all([
      MicaConfigResource.get().$promise,
      EntityFormResource.get({target: 'harmonization-study', locale: $translate.use()}).$promise,
      EntityFormResource.get({target: 'harmonization-population', locale: $translate.use()}).$promise
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

      angular.extend($scope, self); // update scope
    });
  };

  self.initializeStudy = function (study) {
    if (study.logo) {
      self.logoUrl = contextPath + '/ws/draft/harmonization-study/' + study.id + '/file/' + study.logo.id + '/_download';
    }

    study.populations = study.populations || [];
    if (study.populations.length > 0) {
      $scope.selectedPopulation = study.populations[0];
    } else {
      $scope.selectedPopulation = undefined;
    }

    $scope.memberships = processMemberships(study);
  };

  if (self.getViewMode() !== self.Mode.Revision) {
    $scope.study = self.fetchStudy($routeParams.id);
  } else {
    $scope.studyId = $routeParams.id;
  }

  $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
  $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, self.onRestore);
  $scope.$on(STUDY_EVENTS.studyUpdated, function (event, studyUpdated) {
    if (studyUpdated) {
      $log.debug('save study', studyUpdated);

      $scope.study.$save(function onSuccess(response) {
        $scope.study.content = $scope.study.model ? angular.toJson(response.study.model) : null;
        $scope.studySummary = StudyStatesResource.get({id: $routeParams.id}, self.initializeState);
        $scope.study = self.fetchStudy($routeParams.id);
      }, function onError(response) {
        $log.error('Error on study save:', response);
        if (response.status === 409) {
          StudyUpdateWarningService.popup(response, 'study.delete-conflict', 'study.population-delete-conflict-message', function () {
            $scope.study = self.fetchStudy($routeParams.id);
          });
        } else {
          $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
            message: response.data ? response.data : angular.fromJson(response)
          });
        }
      });
    }
  });

  $rootScope.$on('$translateChangeSuccess', function () { self.initializeForm(); });
  self.initializeForm();

  populationManagement($rootScope, $scope, $location, NOTIFICATION_EVENTS);
  contactManagement($scope, $routeParams, CONTACT_EVENTS, self.fetchStudy);
  revisionManagement($rootScope, $scope, $location, $filter, $translate, DraftStudyRevisionsResource, NOTIFICATION_EVENTS, self.initializeStudy, DraftStudyResource);
};

mica.study.HarmonizationStudyViewController.prototype = Object.create(mica.study.BaseViewController.prototype);
mica.study.HarmonizationStudyViewController.prototype.constructor = mica.study.HarmonizationStudyViewController;
