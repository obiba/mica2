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

  .factory('DataAccessRequestCommentsResource', ['$resource',
    function ($resource) {
      return $resource('ws/data-access-request/:id/comments', {}, {
        'save': {
          method: 'POST',
          params: {id: '@id'},
          headers : {'Content-Type' : 'text/plain' },
          errorHandler: true
        },
        'get': {method: 'GET', params: {id: '@id'}, errorHandler: true}
      });
    }])

  .factory('DataAccessRequestCommentResource', ['$resource',
    function ($resource) {
      return $resource('ws/data-access-request/:id/comment/:commentId', {}, {
        'delete': {
          method: 'DELETE',
          params: {id: '@id', commentId: '@commentId'},
          errorHandler: true
        },
        'update': {
          method: 'PUT',
          params: {id: '@id', commentId: '@commentId'},
          headers : {'Content-Type' : 'text/plain' },
          errorHandler: true
        }
      });
    }])

  .factory('DataAccessRequestStatusResource', ['$resource',
    function ($resource) {
      return $resource('ws/data-access-request/:id/_status?to=:status', {}, {
        'update': {method: 'PUT', params: {id: '@id', status: '@status'}, errorHandler: true}
      });
    }])

  .factory('ConfirmableCallback', ['NOTIFICATION_EVENTS',
      function(NOTIFICATION_EVENTS) {

        this.call =function(dispatcher, listener, callback, callbackArgs, dialogOptions) {
          dispatcher.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            dialogOptions, callbackArgs
          );

          listener.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, callback);
        };

      return this;
    }])

  .service('DataAccessRequestService', ['LocaleStringUtils', 'moment', '$filter',
    function (LocaleStringUtils, moment, $filter) {
      var statusList = {
        OPENED: 'OPENED',
        SUBMITTED: 'SUBMITTED',
        REVIEWED: 'REVIEWED',
        APPROVED: 'APPROVED',
        REJECTED: 'REJECTED'
      };

      this.status = statusList;

      this.getStatusFilterData = function() {
        return Object.keys(statusList).map(function(key) {
           return $filter('translate')(statusList[key]);
        });
      };

      var canDoAction = function (request, action) {
        return request.actions ? request.actions.indexOf(action) !== -1 : null;
      };

      this.actions = {
        canView: function (request) {
          return canDoAction(request, 'VIEW');
        },

        canEdit: function (request) {
          return canDoAction(request, 'EDIT');
        },

        canEditStatus: function (request) {
          return canDoAction(request, 'EDIT_STATUS');
        },

        canDelete: function (request) {
          return canDoAction(request, 'DELETE');
        }
      };

      var canChangeStatus = function (request, to) {
        return request.nextStatus ? request.nextStatus.indexOf(to) !== -1 : null;
      };

      this.nextStatus = {
        canSubmit: function (request) {
          return canChangeStatus(request, 'SUBMITTED');
        },

        canReopen: function (request) {
          return canChangeStatus(request, 'OPENED');
        },

        canReview: function (request) {
          return canChangeStatus(request, 'REVIEWED');
        },

        canApprove: function (request) {
          return canChangeStatus(request, 'APPROVED');
        },

        canReject: function (request) {
          return canChangeStatus(request, 'REJECTED');
        }

      };

      this.getStatusHistoryInfo = function() {
        return {
          opened: {
            id: 'opened',
            icon: 'glyphicon glyphicon-saved',
            msg: $filter('translate')('data-access-request.histories.opened')
          },
          reopened: {
            id: 'reopened',
            icon: 'glyphicon glyphicon-repeat',
            msg: $filter('translate')('data-access-request.histories.reopened')
          },
          submitted: {
            id: 'submitted',
            icon: 'glyphicon glyphicon-export',
            msg: $filter('translate')('data-access-request.histories.submitted')
          },
          reviewed: {
            id: 'reviewed',
            icon: 'glyphicon glyphicon-check',
            msg: $filter('translate')('data-access-request.histories.reviewed')
          },
          approved: {
            id: 'approved',
            icon: 'glyphicon glyphicon-ok',
            msg: $filter('translate')('data-access-request.histories.approved')
          },
          rejected: {
            id: 'rejected',
            icon: 'glyphicon glyphicon-remove',
            msg: $filter('translate')('data-access-request.histories.rejected')
          }
        };
      };

      this.getStatusHistoryInfoId = function(status) {
        var id = 'opened';

        if (status.from !== 'OPENED' || status.from !== status.to) {
          switch (status.to) {
            case 'OPENED':
              id = 'reopened';
              break;
            case 'SUBMITTED':
              id = 'submitted';
              break;
            case 'REVIEWED':
              id = 'reviewed';
              break;
            case 'APPROVED':
              id = 'approved';
              break;
            case 'REJECTED':
              id = 'rejected';
              break;
          }
        }

        return id;
      };

      return this;
    }]);
