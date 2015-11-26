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

angular.module('obiba.mica.access')

  .factory('DataAccessFormConfigResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessFormConfigResource'), {}, {
        'get': {method: 'GET', errorHandler: true}
      });
    }])

  .factory('DataAccessRequestsResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestsResource'), {}, {
        'save': {method: 'POST', errorHandler: true},
        'get': {method: 'GET'}
      });
    }])

  .factory('DataAccessRequestResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestResource'), {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET'},
        'delete': {method: 'DELETE'}
      });
    }])

  .factory('DataAccessRequestCommentsResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestCommentsResource'), {}, {
        'save': {
          method: 'POST',
          params: {id: '@id'},
          headers : {'Content-Type' : 'text/plain' },
          errorHandler: true
        },
        'get': {method: 'GET', params: {id: '@id'}, errorHandler: true}
      });
    }])

  .factory('DataAccessRequestCommentResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestCommentResource'), {}, {
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

  .factory('DataAccessRequestStatusResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestStatusResource'), {}, {
        'update': {method: 'PUT', params: {id: '@id', status: '@status'}, errorHandler: true}
      });
    }])

  .service('DataAccessRequestService', ['$translate',
    function ($translate) {
      var statusList = {
        OPENED: 'OPENED',
        SUBMITTED: 'SUBMITTED',
        REVIEWED: 'REVIEWED',
        APPROVED: 'APPROVED',
        REJECTED: 'REJECTED'
      };

      this.status = statusList;

      this.getStatusFilterData = function(userCallback) {
        if (userCallback) {
          $translate(Object.keys(statusList)).then(function(translation) {
            userCallback(Object.keys(translation).map(function(key){
              return translation[key];
            }));
          });
        }
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

      this.getStatusHistoryInfo = function(userCallback) {
        if (!userCallback) {
          return;
        }

        var keyIdMap = {
          'data-access-request.histories.opened': 'opened',
          'data-access-request.histories.reopened': 'reopened',
          'data-access-request.histories.submitted': 'submitted',
          'data-access-request.histories.reviewed': 'reviewed',
          'data-access-request.histories.approved': 'approved',
          'data-access-request.histories.rejected': 'rejected'
        };

        var statusHistoryInfo = {
          opened: {
            id: 'opened',
            icon: 'glyphicon glyphicon-saved',
          },
          reopened: {
            id: 'reopened',
            icon: 'glyphicon glyphicon-repeat',
          },
          submitted: {
            id: 'submitted',
            icon: 'glyphicon glyphicon-export',
          },
          reviewed: {
            id: 'reviewed',
            icon: 'glyphicon glyphicon-check',
          },
          approved: {
            id: 'approved',
            icon: 'glyphicon glyphicon-ok',
          },
          rejected: {
            id: 'rejected',
            icon: 'glyphicon glyphicon-remove',
          }
        };

        $translate(Object.keys(keyIdMap))
          .then(
            function(translation) {
              Object.keys(translation).forEach(
                function(key){
                  statusHistoryInfo[keyIdMap[key]].msg = translation[key];
                });

              userCallback(statusHistoryInfo);
            });
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
