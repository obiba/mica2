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

  .factory('NetworkResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET'}
      });
    }])

  .factory('NetworkPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/network/:id/_publish', {}, {
        'publish': {method: 'PUT', params: {id: '@id'}},
        'unPublish': {method: 'DELETE', params: {id: '@id'}}
      });
    }]);
