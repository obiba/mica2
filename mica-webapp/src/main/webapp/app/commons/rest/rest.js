'use strict';

mica.rest = angular.module('mica.rest', [])

  .config(function ($httpProvider) {
    $httpProvider.responseInterceptors.push('loadingHttpInterceptor');
    $httpProvider.defaults.transformRequest.push(function (data, headersGetter) {
      $('#loading').show();
      return data;
    });
  })

  // register the interceptor as a service, intercepts ALL angular ajax http calls
  .factory('loadingHttpInterceptor', function ($q, $window) {
    return function (promise) {
      return promise.then(
        function (response) {
          $('#loading').hide();
          return response;
        },
        function (response) {
          $('#loading').hide();
          return $q.reject(response);
        });
    };
  });
