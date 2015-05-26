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

mica.dataAccessRequest

  .factory('DataAccessRequestsResource', ['$resource',
    function ($resource) {
      return $resource('ws/data-access-requests', {}, {
        'save': {method: 'POST', errorHandler: true},
        'get': {method: 'GET'}
      });
    }])

  .factory('DataAccessRequestResource', ['$resource',
    function ($resource) {
      return $resource('ws/data-access-request/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET'},
        'delete': {method: 'DELETE'}
      });
    }])

  .factory('DataAccessRequestService',
    function () {
      this.status = {
        OPENED: "OPENED",
        SUBMITTED: "SUBMITTED",
        REVIEWED: "REVIEWED",
        APPROVED: "APPROVED",
        REJECTED: "REJECTED"
      };

      var canDoAction = function (request, action) {
        return request.actions ? request.actions.indexOf(action) !== -1 : null;
      };

      this.actions = {
        canView: function (request) {
          return canDoAction(request, "VIEW");
        },

        canEdit: function (request) {
          return canDoAction(request, "EDIT");
        },

        canDelete: function (request) {
          return canDoAction(request, "DELETE");
        }
      };

      return this;
    });
