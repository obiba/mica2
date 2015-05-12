'use strict';

angular.module('obiba.rest', ['obiba.notification'])

  .config(['$httpProvider', function ($httpProvider) {
    $httpProvider.responseInterceptors.push('httpErrorsInterceptor');
  }])

  .factory('httpErrorsInterceptor', ['$q', '$rootScope', 'NOTIFICATION_EVENTS', 'ServerErrorUtils',
    function ($q, $rootScope, NOTIFICATION_EVENTS, ServerErrorUtils) {
      return function (promise) {
        return promise.then(
          function (response) {
            // $log.debug('httpErrorsInterceptor success', response);
            return response;
          },
          function (response) {
            // $log.debug('httpErrorsInterceptor error', response);
            var config = response.config;
            if (config.errorHandler) {
              return $q.reject(response);
            }
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
              message: ServerErrorUtils.buildMessage(response)
            });
            return $q.reject(response);
          });
      };

    }]);
