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

mica.revisions
  .controller('RevisionsController', [
    '$rootScope',
    '$scope',
    '$uibModal',
    function ($rootScope, $scope, $uibModal) {
      var onSuccess = function(revisions) {
        $scope.commitInfos = revisions;
        viewRevision($scope.active.index, $scope.id, $scope.commitInfos[$scope.active.index]);
      };

      var viewRevision = function(index, id, commitInfo) {
        $scope.active.realIndex = $scope.commitInfos.indexOf(commitInfo);
        $scope.active.index = index;
        $scope.active.page = $scope.pages.index;
        $scope.onViewRevision()(id, commitInfo);
      };

      var restoreRevision = function(id, commitInfo) {
        $scope.onRestoreRevision()(id, commitInfo, function() {
          $scope.onFetchRevisions()($scope.id, onSuccess);
        });
      };

      var viewDiff = function(id, leftSideCommitInfo, rightSideCommitInfo) {
        var response = $scope.onViewDiff()(id, leftSideCommitInfo, rightSideCommitInfo);

        $uibModal.open({
          templateUrl: 'app/entity-revisions/entity-revisions-diff-modal-template.html',
          controller: 'RevisionDiffModalController',
          resolve: {
            diff: function () {
              console.log('view diff', response);
              return response;
            }
          }
        }).result.then(function () {
          restoreRevision(id, rightSideCommitInfo);
        });
      };

      var onWatchId = function() {
        if ($scope.id) {
          $scope.onFetchRevisions()($scope.id, onSuccess);
        }
      };

      var canPaginate = function() {
        return $scope.commitInfos && $scope.commitInfos.length > $scope.pages.perPage;
      };

      $scope.pages = {index: 1, perPage: 5};
      $scope.active = {index: 0, realIndex: 0, page: 1};
      $scope.$watch('id', onWatchId);
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.canPaginate = canPaginate;
      $scope.viewDiff = viewDiff;
    }])

    .controller('RevisionDiffModalController', ['$scope', '$uibModalinstance', 'diff',
    function($scope, $uibModalInstance, diff) {
      $scope.diff = diff;

      $scope.cancel = function () {
        $uibModalInstance.dismiss();
      };

      $scope.restoreRevision = function () {
        $uibModalInstance.close();
      };
    }]);
