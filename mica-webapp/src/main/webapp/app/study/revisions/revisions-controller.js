'use strict';

mica.revisions
  .controller('RevisionsController', [
    '$rootScope',
    '$scope',
    '$filter',
    'LocaleStringUtils',
    function ($rootScope, $scope, $filter, LocaleStringUtils) {
      var onSuccess = function(revisions) {
        $scope.commitInfos = revisions;
        updateRevisionInfoMessage();
      };

      var viewRevision = function(index, id, commitInfo) {
        $scope.active.realIndex = $scope.commitInfos.indexOf(commitInfo);
        $scope.active.index = index;
        $scope.active.page = $scope.pages.index;
        updateRevisionInfoMessage();
        $scope.onViewRevision()(id, commitInfo.commitId);
      };

      var onWatchId = function() {
        if ($scope.id) {
          $scope.onFetchRevisions()($scope.id, onSuccess);
        }
      };

      var updateRevisionInfoMessage = function() {
        var commitInfo = $scope.commitInfos[$scope.active.realIndex];
        $scope.revisionInfoMessage = LocaleStringUtils.translate('current-reversion-info',
          [$filter('amDateFormat')(commitInfo.date, 'lll'), commitInfo.author]);
      };

      $scope.pages = {index: 1};
      $scope.active = {index: 0, realIndex: 0, page: 1};
      $scope.$watch('id', onWatchId);
      $scope.viewRevision = viewRevision;
    }]);
