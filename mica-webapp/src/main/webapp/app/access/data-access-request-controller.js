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

  .controller('DataAccessRequestListController', ['$scope', 'DataAccessRequestsResource', 'DataAccessRequestResource',

    function ($scope, DataAccessRequestsResource, DataAccessRequestResource) {

      $scope.requests = DataAccessRequestsResource.query();

      $scope.deleteRequest = function (id) {
        //TODO ask confirmation
        DataAccessRequestResource.delete({id: id},
          function () {
            $scope.requests = DataAccessRequestsResource.query();
          });
      };

    }]);
