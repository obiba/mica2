'use strict';

mica.publish
  .directive('publishStatus', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        version: '=',
        ahead: '='
      },
      templateUrl: 'app/publish/publish-status-template.html'
    };
  }])

  .directive('publishButton', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        version: '=',
        ahead: '=',
        publish: '&'
      },
      templateUrl: 'app/publish/publish-button-template.html'
    };
  }]);
