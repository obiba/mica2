'use strict';

mica.publish
  .directive('publishStatus', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        state: '='
      },
      templateUrl: 'app/publish/publish-status-template.html'
    };
  }])

  .directive('publishSwitch', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        status: '&',
        publish: '&'
      },
      templateUrl: 'app/publish/publish-switch-template.html'
    };
  }]);
