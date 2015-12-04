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
  .factory('NetworksResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/networks');
    }])

  .factory('DraftNetworksResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/networks', {}, {
        'save': {method: 'POST', errorHandler: true}
      });
    }])

  .factory('DraftNetworkResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
        'delete': {method: 'DELETE', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET'}
      });
    }])

  .factory('DraftNetworkPermissionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', role: '@role'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('DraftNetworkAccessesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id/accesses', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', file: '@file'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('DraftNetworkPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id/_publish', {id: '@id'}, {
        'publish': {method: 'PUT', params: {cascading: '@cascading'}},
        'unPublish': {method: 'DELETE'}
      });
    }])

  .factory('DraftNetworkStatusResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id/_status', {}, {
        'toStatus': {method: 'PUT', params: {id: '@id', value: '@value'}}
      });
    }])

  .factory('DraftNetworkRevisionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id/commits', {}, {
        'get': {method: 'GET', params: {id: '@id'}}
      });
    }])

  .factory('DraftNetworkRestoreRevisionResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id/commit/:commitId/restore', {}, {
        'restore': {method: 'PUT', params: {id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('DraftNetworkViewRevisionResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id/commit/:commitId/view', {}, {
        'view': {method: 'GET', params: {id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('NetworkService', ['$rootScope', '$translate', 'DraftNetworkResource',
    'NOTIFICATION_EVENTS', 'LocalizedValues', function ($rootScope, $translate, DraftNetworkResource,
                                                        NOTIFICATION_EVENTS,
                                                        LocalizedValues) {
      return {
        deleteNetwork: function (network, onSuccess) {
          var networkToDelete;

          var removeSubscriber = $rootScope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
            if (networkToDelete === id) {
              DraftNetworkResource.delete({id: id}, onSuccess);
              removeSubscriber();
            }
          });

          if (network) {
            networkToDelete = network.id;
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
              {
                titleKey: 'network.delete-dialog.title',
                messageKey: 'network.delete-dialog.message',
                messageArgs: [LocalizedValues.forLang(network.name, $translate.use())]
              }, network.id
            );
          }
        }
      };
    }]);
