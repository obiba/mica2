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

mica.network

  .constant('NETWORK_EVENTS', {
    networkUpdated: 'event:network-updated'
  })

  .controller('NetworkMainController', ['$scope', '$location', 'NetworksResource',
    function($scope, $location, NetworksResource) {
      if($scope.micaConfig.isSingleNetworkEnabled) {
        $scope.networks = NetworksResource.query({}, function(res) {
          if(res.length) {
            $location.path('/network/' + res[0].id);
            $location.replace();
          }
        });
      }
    }])

  .controller('NetworkListController', [
    '$scope', '$timeout', 'NetworksResource', 'NetworkService', 'AlertBuilder', 'EntityStateFilterService', mica.commons.ListController])

  .controller('NetworkEditController', [
    '$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$filter',
    '$q',
    '$locale',
    '$location',
    '$translate',
    'DraftNetworkResource',
    'DraftNetworksResource',
    'DraftNetworkPublicationResource',
    'MicaConfigResource',
    'EntityFormResource',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'SfOptionsService',
    '$timeout',
    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $filter,
              $q,
              $locale,
              $location,
              $translate,
              DraftNetworkResource,
              DraftNetworksResource,
              DraftNetworkPublicationResource,
              MicaConfigResource,
              EntityFormResource,
              FormServerValidation,
              FormDirtyStateObserver,
              SfOptionsService,
              $timeout) {

      $scope.files = [];
      $scope.newNetwork= !$routeParams.id;
      $scope.revision = {comment: null};
      $scope.network = $routeParams.id ? DraftNetworkResource.get({id: $routeParams.id}, function(network) {
        $scope.files = network.logo ? [network.logo] : [];

        $scope.$broadcast('sfLocalizedStringLocaleChanged', $translate.use());
        return network;

      }) : {published: false, model:{}};

      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          $scope.sfOptions = {};

          var formLanguages = {};
          micaConfig.languages.forEach(function (loc) {
            formLanguages[loc] = $filter('translate')('language.' + loc);
          });

          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.formDefaults = {languages: formLanguages};
          });

          EntityFormResource.get({target: 'network', locale: $translate.use()}, function(form) {
            form.schema = angular.fromJson(form.schema);
            form.definition = angular.fromJson(form.definition);
            $scope.sfForm = form;

            $timeout(function () { $scope.sfForm = angular.copy(form); }, 250);
          });
        });
      }

      $scope.save = function () {
        $scope.network.logo = $scope.files.length > 0 ? $scope.files[0] : null;

        if(!$scope.network.logo) {
          delete $scope.network.logo;
        }

        $scope.$broadcast('schemaFormValidate');
        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }
        if ($scope.network.id) {
          updateNetwork();
        } else {
          createNetwork();
        }
      };

      $scope.cancel = function () {
        $location.path('/network' + ($scope.network.id ? '/' + $scope.network.id : '')).replace();
      };

      var updateNetwork = function () {
        $scope.network.$save({comment: $scope.revision.comment},
          function (network) {
            FormDirtyStateObserver.unobserve();
            $location.path('/network/' + network.id).replace();
          },
          saveErrorHandler);
      };

      var createNetwork = function () {
        DraftNetworksResource.save($scope.network,
          function (resource, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            FormDirtyStateObserver.unobserve();
            $location.path('/network/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };


      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();

      FormDirtyStateObserver.observe($scope);
    }])

  .controller('NetworkLinksModalController', ['$scope', '$uibModalInstance', 'entityStatesResource', 'currentLinks', 'type', 'lang',
    function ($scope, $uibModalInstance, entityStatesResource, currentLinks, type, lang) {
      $scope.type = type;
      $scope.lang = lang;
      $scope.entities = [];

      entityStatesResource.query({type: ''}).$promise.then(function(entities) {
        $scope.entities = entities.filter(function(s) {
          return currentLinks === undefined || currentLinks.indexOf(s.id) < 0;
        });
      });

      $scope.hasSelectedEntities = function() {
        for(var i = $scope.entities.length; i--; ){
          if($scope.entities[i].selected) {
            return true;
          }
        }

        return false;
      };

      $scope.removeSelectedEntity = function(entity) {
        entity.selected = false;
      };

      $scope.save = function () {
        var selectedIds = $scope.entities //
          .filter(function(s){ return s.selected; }) //
          .map(function(s) { return s.id; });

        $uibModalInstance.close(selectedIds);
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }])

  .controller('NetworkContactsModalController', ['$scope', '$uibModalInstance', 'ContactsSearchResource', 'network', 'lang',
    function ($scope, $uibModalInstance, ContactsSearchResource, network, lang) {
      $scope.network = network;
      $scope.lang = lang;
      $scope.persons = [];

      var studyIds = network.studyIds ? network.studyIds.concat([]) : [];
      var networkIds = network.networkIds ? network.networkIds.concat([]) : [];
      if (network.networkSummaries) {
        network.networkSummaries.forEach(function(n) {
          if (n.studyIds) {
            studyIds = studyIds.concat(studyIds, n.studyIds);
          }
        });
      }

      $scope.query = studyIds.length === 0 ? '' : 'studyMemberships.parentId:(' + studyIds.join(' OR ') + ')';
      if (networkIds.length > 0) {
        if ($scope.query.length > 0) {
          $scope.query = $scope.query + ' OR ';
        }
        $scope.query = $scope.query + 'networkMemberships.parentId:(' + networkIds.join(' OR ') + ')';

      }

      if(network.studyIds.length > 0 || network.networkIds.length > 0) {
        ContactsSearchResource.get({
          query: $scope.query,
          limit: 999
        }).$promise.then(function (result) {
            $scope.persons = (result.persons || []).filter(function (p) {
              return p.studyMemberships && p.studyMemberships.length > 0 || p.networkMemberships && p.networkMemberships.length > 0;
            });
          });
      }

      $scope.getDownloadAllContactsParams = function () {
        return $.param({
          query: $scope.query,
          limit: 999
        });
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }])

  .controller('NetworkViewController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$q',
    '$locale',
    '$location',
    '$translate',
    '$timeout',
    'DraftNetworkResource',
    'DraftNetworksResource',
    'DraftNetworkPublicationResource',
    'DraftNetworkStatusResource',
    'DraftNetworkViewRevisionResource',
    'DraftNetworkRevisionsResource',
    'DraftNetworkRestoreRevisionResource',
    'MicaConfigResource',
    'EntityFormResource',
    'CONTACT_EVENTS',
    'NETWORK_EVENTS',
    'NOTIFICATION_EVENTS',
    'DraftStudiesSummariesResource',
    'DraftFileSystemSearchResource',
    'StudyStatesResource',
    '$uibModal',
    'LocalizedValues',
    '$filter',
    'NetworkService',
    'DocumentPermissionsService',
    'SfOptionsService',
    'ngObibaMicaUrl',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $q,
              $locale,
              $location,
              $translate,
              $timeout,
              DraftNetworkResource,
              DraftNetworksResource,
              DraftNetworkPublicationResource,
              DraftNetworkStatusResource,
              DraftNetworkViewRevisionResource,
              DraftNetworkRevisionsResource,
              DraftNetworkRestoreRevisionResource,
              MicaConfigResource,
              EntityFormResource,
              CONTACT_EVENTS,
              NETWORK_EVENTS,
              NOTIFICATION_EVENTS,
              DraftStudiesSummariesResource,
              DraftFileSystemSearchResource,
              StudyStatesResource,
              $uibModal,
              LocalizedValues,
              $filter,
              NetworkService,
              DocumentPermissionsService,
              SfOptionsService,
              ngObibaMicaUrl) {


      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {

          $scope.openAccess = micaConfig.openAccess;

          var formLanguages = {};
          micaConfig.languages.forEach(function (loc) {
            formLanguages[loc] = $filter('translate')('language.' + loc);
          });

          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.pristine = {errors: true, success: false};
            $scope.sfOptions.formDefaults = {languages: formLanguages};
          });

          $scope.roles = micaConfig.roles;

          EntityFormResource.get({target: 'network', locale: $translate.use()}, function (form) {
            form.schema = angular.fromJson(form.schema);
            form.definition = angular.fromJson(form.definition);
            form.schema.readonly = true;
            $scope.sfForm = form;

            $timeout(function () { $scope.sfForm = angular.copy(form); }, 250);
          });
        });
      }

      var initializeNetwork = function(network){

        $scope.$broadcast('sfLocalizedStringLocaleChanged', $translate.use());

        if (network.logo) {
          $scope.logoUrl = contextPath + '/ws/draft/network/'+network.id+'/file/'+network.logo.id+'/_download';
        }

        $scope.permissions = DocumentPermissionsService.state(network['obiba.mica.EntityStateDto.state']);
        $scope.studySummaries = network.studySummaries || [];
        network.studyIds = network.studyIds || [];
        $scope.network.networkIds = $scope.network.networkIds || [];
        network.memberships = network.memberships || [];

        $scope.memberships = network.memberships.map(function (m) {
          if (!m.members) {
            m.members = [];
          }

          return m;
        }).reduce(function (res, m) {
          res[m.role] = m.members;
          return res;
        }, {});
      };

      $scope.memberships = {};
      $scope.isOrderingContacts = false; //prevent opening contact modal on reordering (firefox)
      $scope.sortableOptions = {
        start: function() {
          $scope.isOrderingContacts = true;
        },
        stop: function () {
          $scope.emitNetworkUpdated();
          $timeout(function () {
            $scope.isOrderingContacts = false;
          }, 300);
        }
      };

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};
      $scope.networkId = $routeParams.id;
      $scope.studySummaries = [];
      $scope.network = DraftNetworkResource.get({id: $routeParams.id}, initializeNetwork);

      $scope.publish = function (publish) {
        if (publish) {
          DraftFileSystemSearchResource.searchUnderReview({path: '/network/' + $scope.network.id},
            function onSuccess(response) {
              DraftNetworkPublicationResource.publish(
                {id: $scope.network.id, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
                function () {
                  $scope.network = DraftNetworkResource.get({id: $routeParams.id}, initializeNetwork);
                });
            },
            function onError() {
              $log.error('Failed to search for Under Review files.');
            }
          );
        } else {
          DraftNetworkPublicationResource.unPublish({id: $scope.network.id}, function () {
            $scope.network = DraftNetworkResource.get({id: $routeParams.id}, initializeNetwork);
          });
        }
      };

      $scope.toStatus = function (value) {
        DraftNetworkStatusResource.toStatus({id: $scope.network.id, value: value}, function () {
          $scope.network = DraftNetworkResource.get({id: $routeParams.id}, initializeNetwork);
        });
      };

      $scope.delete = function () {
        NetworkService.delete($scope.network, function() {
          $location.path('/network');
        });
      };

      $scope.setOrder = function(newOrder) {
        $scope.network.membershipSortOrder = newOrder;
        $scope.emitNetworkUpdated();
      };

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

      $scope.inViewMode = function () {
        return $scope.viewMode === $scope.Mode.View;
      };

      var viewRevision = function (networkId, commitInfo) {
        $scope.commitInfo = commitInfo;
        $scope.network = DraftNetworkViewRevisionResource.view({
          id: networkId,
          commitId: commitInfo.commitId
        }, initializeNetwork);
      };

      function viewDiff (id, leftCommitInfo, rightCommitInfo) {
        if (leftCommitInfo && rightCommitInfo) {
          return DraftNetworkRevisionsResource.diff({id: id, left: leftCommitInfo.commitId, right: rightCommitInfo.commitId, locale: $translate.use()});
        }
      }

      var fetchNetwork = function (networkId) {
        $scope.network = DraftNetworkResource.get({id: networkId}, initializeNetwork);
      };

      var fetchRevisions = function (networkId, onSuccess) {
        DraftNetworkRevisionsResource.query({id: networkId}, function (response) {
          if (onSuccess) {
            onSuccess(response);
          }
        });
      };

      var restoreFromFields = function (transformFn) {
        DraftNetworkResource.rGet({id: $scope.networkId}, function (network) {
          var result = transformFn(network);
          DraftNetworkResource.rSave({id: $scope.networkId}, result).$promise.then(function () {
            $location.reload();
          });
        });
      };

      var restoreRevision = function (networkId, commitInfo, onSuccess) {
        if (commitInfo && $scope.networkId === networkId) {
          var args = {commitId: commitInfo.commitId, restoreSuccessCallback: onSuccess};

          $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {
              titleKey: 'network.restore-dialog.title',
              messageKey: 'network.restore-dialog.message',
              messageArgs: [$filter('amDateFormat')(commitInfo.date, 'lll')]
            }, args
          );
        }
      };

      var onRestore = function (event, args) {
        if (args.commitId) {
          DraftNetworkRestoreRevisionResource.restore({id: $scope.networkId, commitId: args.commitId},
            function () {
              fetchNetwork($routeParams.id);
              $scope.networkId = $routeParams.id;
              if (args.restoreSuccessCallback) {
                args.restoreSuccessCallback();
              }
            });
        }
      };

      var onError = function (response) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
          message: response.data ? response.data : angular.fromJson(response)
        });
      };

      $scope.fetchNetwork = fetchNetwork;
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.fetchRevisions = fetchRevisions;
      $scope.restoreFromFields = restoreFromFields;
      $scope.viewDiff = viewDiff;

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onRestore);

      $scope.print = function () {
        setTimeout(function(){ window.print();}, 250);
      };

      $scope.emitNetworkUpdated = function () {
        $scope.$emit(NETWORK_EVENTS.networkUpdated, $scope.network);
      };

      $scope.$on(NETWORK_EVENTS.networkUpdated, function (event, networkUpdated) {
        if (networkUpdated === $scope.network) {
          $log.debug('save network', networkUpdated);

          $scope.network.$save(function () {
              $scope.network = DraftNetworkResource.get({id: $routeParams.id}, initializeNetwork);
            }, onError);
        }
      });

      function updateExistingContact(contact, contacts) {
        var existingContact = contacts.filter(function (c) {
          return c.id === contact.id && !angular.equals(c, contact);
        })[0];

        if (existingContact) {
          angular.copy(contact, existingContact);
        }
      }

      $scope.$on(CONTACT_EVENTS.addContact, function (event, network, contact, type) {
        if (network === $scope.network) {
          var roleMemberships = $scope.network.memberships.filter(function(m) {
            return m.role === type;


          })[0];

          if (!roleMemberships) {
            roleMemberships = {role: type, members: []};
            $scope.network.memberships.push(roleMemberships);
          }

          var members = $scope.network.memberships.map(function(m) {
            return m.members;
          });

          updateExistingContact(contact, [].concat.apply([], members) || []);
          roleMemberships.members.push(contact);

          $scope.emitNetworkUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactUpdated, function (event, network, contact) {
        var members = $scope.network.memberships.map(function (m) {
          return m.members;
        });

        updateExistingContact(contact, [].concat.apply([], members) || []);

        if (network === $scope.network) {
          $scope.emitNetworkUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactEditionCanceled, function (event, network) {
        if (network === $scope.network) {
          $scope.network = DraftNetworkResource.get({id: $scope.network.id}, initializeNetwork);
        }
      });

      $scope.$on(CONTACT_EVENTS.contactDeleted, function (event, network, contact, type) {
        if (network === $scope.network) {
          var roleMemberships = $scope.network.memberships.filter(function (m) {
              return m.role === type;
            })[0] || { members: [] };

          var idx = roleMemberships.members.indexOf(contact);

          if (idx !== -1) {
            roleMemberships.members.splice(idx, 1);
          }

          $scope.emitNetworkUpdated();
        }
      });

      $scope.addStudyEvent = function () {
        $uibModal.open({
          templateUrl: 'app/network/views/network-modal-add-links.html',
          controller: 'NetworkLinksModalController',
          resolve: {
            entityStatesResource: function() {
              return StudyStatesResource;
            },
            type: function() {
              return 'study';
            },
            currentLinks: function() {
              return $scope.network.studyIds;
            },
            lang: function() {
              return $translate.use();
            }
          }
        }).result.then(function(selectedIds) {
            $scope.network.studyIds = ($scope.network.studyIds || []).concat(selectedIds);
            $scope.emitNetworkUpdated();
          });
      };

      $scope.getStudyReportByNetworkUrl = function () {
        return (ngObibaMicaUrl.getUrl('BaseUrl') + ngObibaMicaUrl.getUrl('JoinQuerySearchCsvReportByNetworkResource'))
          .replace(':type', 'studies')
          .replace(':networkId', $scope.network.id)
          .replace(':locale', $translate.use());
      };

      $scope.deleteStudyEvent = function (network, summary) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'network.study-delete-dialog.title',
            messageKey:'network.study-delete-dialog.message',
            messageArgs: [LocalizedValues.forLang(summary.name, $translate.use())]
          }, {type: 'study', summary: summary}
        );
      };

      $scope.deleteSelectedStudiesEvent = function () {
        var selectedSummaries = $scope.studySummaries.filter(function (s) {
          return s.selected;
        });

        if (!selectedSummaries.length) {
          return;
        }

        var currentLang = $translate.use();
        var names = selectedSummaries.map(function (s) {
          return LocalizedValues.forLang(s.name, currentLang);
        }).join(', ');

        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'network.study-delete-dialog.title',
            messageKey:'network.study-delete-dialog.message',
            messageArgs: [names]
          }, {type: 'study', summary: selectedSummaries}
        );
      };

      $scope.addNetwork = function () {
        $uibModal.open({
          templateUrl: 'app/network/views/network-modal-add-links.html',
          controller: 'NetworkLinksModalController',
          resolve: {
            entityStatesResource: function() {
              return DraftNetworksResource;
            },
            type: function() {
              return 'network';
            },
            currentLinks: function() {
              return ($scope.network.networkIds || []).concat($scope.network.id);
            },
            lang: function() {
              return $translate.use();
            }
          }
        }).result.then(function(selectedIds) {
            $scope.network.networkIds = ($scope.network.networkIds || []).concat(selectedIds);
            $scope.emitNetworkUpdated();
          });
      };

      $scope.deleteSelectedNetworksEvent = function () {
        var selectedSummaries = $scope.network.networkSummaries.filter(function (s) {
          return s.selected;
        });

        if (!selectedSummaries.length) {
          return;
        }

        var currentLang = $translate.use();
        var names = selectedSummaries.map(function (s) {
          return LocalizedValues.forLang(s.name, currentLang);
        }).join(', ');

        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'network.network-delete-dialog.title',
            messageKey:'network.network-delete-dialog.message',
            messageArgs: [names]
          }, {type: 'network', summary: selectedSummaries}
        );
      };

      $scope.deleteNetwork = function (network, summary) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'network.network-delete-dialog.title',
            messageKey: 'network.network-delete-dialog.message',
            messageArgs: [LocalizedValues.forLang(summary.name, $translate.use())]
          }, {type: 'network', summary: summary}
        );
      };

      $scope.showAllContacts = function () {
        $uibModal.open({
          templateUrl: 'app/network/views/network-modal-contacts.html',
          controller: 'NetworkContactsModalController',
          resolve: {
            network: function() {
              return $scope.network;
            },
            lang: function() {
              return $translate.use();
            }
          }
        });
      };

      var onConfirmSingleDeleteDialog = function (data) {
        var deleteIndex;
        if (data.type === 'study') {
          deleteIndex = $scope.network.studyIds.indexOf(data.summary.id);

          if (deleteIndex > -1) {
            $scope.network.studyIds.splice(deleteIndex, 1);
            $scope.emitNetworkUpdated();
          } else {
            $log.error('The id was not found: ', data.summary.id);
          }
        } else {
          deleteIndex = $scope.network.networkIds.indexOf(data.summary.id);

          if (deleteIndex > -1) {
            $scope.network.networkIds.splice(deleteIndex, 1);
            $scope.emitNetworkUpdated();
          } else {
            $log.error('The id was not found: ', data.summary.id);
          }
        }

        return deleteIndex > -1;
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, data) {
        if (!data.summary) { return; }

        if (!Array.isArray(data.summary)) {
          onConfirmSingleDeleteDialog(data);
        } else {
          var someChanges;
          if (data.type === 'study') {
            data.summary.forEach(function (d) {
              var delId = $scope.network.studyIds.indexOf(d.id);
              if (delId > -1) {
                $scope.network.studyIds.splice(delId, 1);
                someChanges = someChanges || true;
              } else {
                $log.error('The id was not found: ', d.id);
              }
            });
          } else {
            data.summary.forEach(function (d) {
              var delId = $scope.network.networkIds.indexOf(d.id);
              if (delId > -1) {
                $scope.network.networkIds.splice(delId, 1);
                someChanges = someChanges || true;
              } else {
                $log.error('The id was not found: ', d.id);
              }
            });
          }

          if (someChanges) {
            $scope.emitNetworkUpdated();
          }
        }
      });

      $scope.viewMode = getViewMode();

      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();
    }])

  .controller('NetworkPermissionsController', [
    '$scope', '$routeParams', 'DraftNetworkPermissionsResource', 'DraftNetworkAccessesResource', mica.commons.PermissionsController
  ]);
