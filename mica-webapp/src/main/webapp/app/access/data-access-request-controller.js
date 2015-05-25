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

  .controller('DataAccessRequestListController', ['$rootScope', '$scope', 'DataAccessRequestsResource', 'DataAccessRequestResource', 'NOTIFICATION_EVENTS',

    function ($rootScope, $scope, DataAccessRequestsResource, DataAccessRequestResource, NOTIFICATION_EVENTS) {

      $scope.requests = DataAccessRequestsResource.query();

      $scope.deleteRequest = function (request) {
        $scope.requestToDelete = request.id;
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'data-access-request.delete-dialog.title',
            messageKey:'data-access-request.delete-dialog.message',
            messageArgs: [request.title, request.applicant]
          }, request.id
        );
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
        if ($scope.requestToDelete === id) {
          DataAccessRequestResource.delete({id: $scope.requestToDelete},
            function () {
              $scope.requests = DataAccessRequestsResource.query();
            });

          delete $scope.requestToDelete;
        }
      });
    }])

  .controller('DataAccessRequestViewController', ['$scope', '$routeParams', 'DataAccessRequestResource',

    function ($scope, $routeParams, DataAccessRequestResource) {

      $scope.dataAccessRequest = $routeParams.id ?
        DataAccessRequestResource.get({id: $routeParams.id}, function(dataAccessRequest) {
          return dataAccessRequest;
        }) : {};

    }]);
