/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
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

  .controller('NetworkListController', ['$scope', 'NetworksResource', 'NetworkResource',

    function ($scope, NetworksResource, NetworkResource) {

      $scope.networks = NetworksResource.query();

      $scope.deleteNetwork = function (id) {
        //TODO ask confirmation
        NetworkResource.delete({id: id},
          function () {
            $scope.networks = NetworksResource.query();
          });
      };

    }])

  .controller('NetworkEditController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location',
    'NetworkResource', 'DraftNetworksResource', 'NetworkPublicationResource', 'MicaConfigResource',
    'FormServerValidation',
    function ($rootScope, $scope, $routeParams, $log, $locale, $location, NetworkResource, DraftNetworksResource,
              NetworkPublicationResource, MicaConfigResource, FormServerValidation) {
      $scope.files = [];
      $scope.network = $routeParams.id ?
        NetworkResource.get({id: $routeParams.id}, function(response) {
          $scope.files = response.logo ? [response.logo] : [];
          return response;
        }) : {published: false};

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.save = function () {
        $scope.network.logo = $scope.files.length > 0 ? $scope.files[0] : null;

        if(!$scope.network.logo) { //protobuf doesnt like null values
          delete $scope.network.logo;
        }

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
        $scope.network.$save(
          function (network) {
            $location.path('/network/' + network.id).replace();
          },
          saveErrorHandler);
      };

      var createNetwork = function () {
        DraftNetworksResource.save($scope.network,
          function (resource, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            $location.path('/network/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };
    }])

  .controller('NetworkStudiesModalController', ['$scope', '$modalInstance', 'StudyStatesResource', 'currentStudies', 'lang',
    function ($scope, $modalInstance, StudyStatesResource, currentStudies, lang) {
      $scope.lang = lang;
      $scope.studies = [];

      StudyStatesResource.query().$promise.then(function(studies) {
        $scope.studies = studies.filter(function(s) { return currentStudies.indexOf(s.id) < 0; });
      });

      $scope.hasSelectedStudies = function() {
        for(var i= $scope.studies.length; i--; ){
          if($scope.studies[i].selected) {
            return true;
          }
        }

        return false;
      };

      $scope.removeSelectedStudy = function(study) {
        study.selected = false;
      };

      $scope.save = function () {
        var selectedIds = $scope.studies //
          .filter(function(s){ return s.selected; }) //
          .map(function(s) { return s.id; });

        $modalInstance.close(selectedIds);
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };
    }])

  .controller('NetworkViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale',
    '$location', '$translate', 'NetworkResource', 'NetworkPublicationResource', 'MicaConfigResource',
    'CONTACT_EVENTS', 'NETWORK_EVENTS', 'NOTIFICATION_EVENTS', 'DraftStudiesSummariesResource', '$modal',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, $translate, NetworkResource,
              NetworkPublicationResource, MicaConfigResource, CONTACT_EVENTS, NETWORK_EVENTS, NOTIFICATION_EVENTS,
              DraftStudiesSummariesResource, $modal) {
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

      $scope.studySummaries = [];
      $scope.network = NetworkResource.get({id: $routeParams.id}, function(network){
        if (network.studyIds && network.studyIds.length > 0) {
          if (network.logo) {
            $scope.logoUrl = 'ws/draft/network/'+network.id+'/file/'+network.logo.id+'/_download';
          }
          DraftStudiesSummariesResource.summaries({id: network.studyIds},function (summaries){
            $scope.studySummaries = summaries;
          });
        }
      });

      $scope.isPublished = function() {
        return $scope.network.published;
      };

      $scope.publish = function () {
        if ($scope.network.published) {
          NetworkPublicationResource.unPublish({id: $scope.network.id}, function () {
            $scope.network = NetworkResource.get({id: $routeParams.id});
          });
        } else {
          NetworkPublicationResource.publish({id: $scope.network.id}, function () {
            $scope.network = NetworkResource.get({id: $routeParams.id});
          });
        }
      };

      $scope.emitNetworkUpdated = function () {
        $scope.$emit(NETWORK_EVENTS.networkUpdated, $scope.network);
      };

      $scope.$on(NETWORK_EVENTS.networkUpdated, function (event, networkUpdated) {
        if (networkUpdated === $scope.network) {
          $log.debug('save network', networkUpdated);

          $scope.network.$save(function () {
              $scope.network = NetworkResource.get({id: $routeParams.id}, function(network){
                if (network.studyIds && network.studyIds.length > 0) {
                  DraftStudiesSummariesResource.summaries({id: network.studyIds},function (summaries){
                    $scope.studySummaries = summaries;
                  });
                }
              });
            },
            function (response) {
              $log.error('Error on network save:', response);
              $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
                message: response.data ? response.data : angular.fromJson(response)
              });
            });
        }
      });

      $scope.$on(CONTACT_EVENTS.addInvestigator, function (event, study, contact) {
        if (study === $scope.network) {
          if (!$scope.network.investigators) {
            $scope.network.investigators = [];
          }
          $scope.network.investigators.push(contact);
          $scope.emitNetworkUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.addContact, function (event, network, contact) {
        if (network === $scope.network) {
          if (!$scope.network.contacts) {
            $scope.network.contacts = [];
          }
          $scope.network.contacts.push(contact);
          $scope.emitNetworkUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactUpdated, function (event, study) {
        if (study === $scope.network) {
          $scope.emitNetworkUpdated();
        }
      });

      $scope.$on(CONTACT_EVENTS.contactEditionCanceled, function (event, study) {
        if (study === $scope.network) {
          $scope.network = NetworkResource.get({id: $scope.network.id});
        }
      });

      $scope.$on(CONTACT_EVENTS.contactDeleted, function (event, study, contact, isInvestigator) {
        if (study === $scope.network) {
          if (isInvestigator) {
            var investigatorsIndex = $scope.network.investigators.indexOf(contact);
            if (investigatorsIndex !== -1) {
              $scope.network.investigators.splice(investigatorsIndex, 1);
            }
          } else {
            var contactsIndex = $scope.network.contacts.indexOf(contact);
            if (contactsIndex !== -1) {
              $scope.network.contacts.splice(contactsIndex, 1);
            }
          }
          $scope.emitNetworkUpdated();
        }
      });

      $scope.addStudyEvent = function () {
        $modal.open({
          templateUrl: 'app/network/views/network-modal-add-studies.html',
          controller: 'NetworkStudiesModalController',
          resolve: {
            currentStudies: function() {
              return $scope.network.studyIds;
            },
            lang: function() {
              return getActiveTab().lang;
            }
          }
        }).result.then(function(selectedIds) {
            $scope.network.studyIds = ($scope.network.studyIds || []).concat(selectedIds);
            $scope.emitNetworkUpdated();
          });
      };

      $scope.deleteStudyEvent = function (network, summary, index) {
        var titleKey = 'network.delete-dialog-title';
        var messageKey = 'network.delete-dialog-message';
        $translate([titleKey, messageKey])
          .then(function (translation) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
              {title: translation[titleKey], message: translation[messageKey]}, summary);
          });

        $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, summary) {
          if ($scope.studySummaries[index] === summary) {
            var deleteIndex = $scope.network.studyIds.indexOf(summary.id);
            if (deleteIndex > -1) {
              $scope.studySummaries.splice(index, 1);
              $scope.network.studyIds.splice(deleteIndex, 1);
              $scope.emitNetworkUpdated();
            } else {
              $log.error('The study id was not found: ', summary.id);
            }
          }
        });
      };

    }]);
