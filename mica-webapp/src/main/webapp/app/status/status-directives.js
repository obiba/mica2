'use strict';

mica.status
  .directive('statusButtons', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        state: '=',
        onEdit: '&',
        canEdit: '&',
        onPublish: '&',
        onUnPublish: '&',
        canPublish: '&',
        onDelete: '&',
        canDelete: '&',
        toDraft: '&',
        toUnderReview: '&',
        toDeleted: '&'
      },
      templateUrl: 'app/status/status-buttons-template.html'
    };
  }]);
