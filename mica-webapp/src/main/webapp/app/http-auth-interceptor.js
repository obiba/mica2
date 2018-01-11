/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @license HTTP Auth Interceptor Module for AngularJS
 * (c) 2012 Witold Szczerba
 * License: MIT
 */
(function () {
  'use strict';

  angular.module('http-auth-interceptor', ['http-auth-interceptor-buffer', 'cfp.loadingBar'])

    .factory('authService', ['$rootScope', 'httpBuffer', function ($rootScope, httpBuffer) {
      return {
        /**
         * Call this function to indicate that authentication was successfull and trigger a
         * retry of all deferred requests.
         * @param data an optional argument to pass on to $broadcast which may be useful for
         * example if you need to pass through details of the user that was logged in
         */
        loginConfirmed: function (data, configUpdater) {
          var updater = configUpdater || function (config) {
            return config;
          };
          $rootScope.$broadcast('event:auth-loginConfirmed', data);
          httpBuffer.retryAll(updater);
        },

        /**
         * Call this function to indicate that authentication should not proceed.
         * All deferred requests will be abandoned or rejected (if reason is provided).
         * @param data an optional argument to pass on to $broadcast.
         * @param reason if provided, the requests are rejected; abandoned otherwise.
         */
        loginCancelled: function (data, reason) {
          httpBuffer.rejectAll(reason);
          $rootScope.$broadcast('event:auth-loginCancelled', data);
        }
      };
    }])

    .factory('MicaHttpInterceptor', ['$rootScope', '$q', '$injector', 'httpBuffer', '$timeout', 'cfpLoadingBar', 
      function($rootScope, $q, $injector, httpBuffer, $timeout, cfpLoadingBar) {
        return {
          // optional method
          'request': function(config) {
            // do something on success
            return config;
          },

          // optional method
          'requestError': function(rejection) {
            // do something on error
            return $q.reject(rejection);
          },

          // optional method
          'response': function(response) {
            // do something on success
            return response;
          },

          // optional method
          'responseError': function(response) {
            var FormDirtyStateObserver = $injector.get('FormDirtyStateObserver'); //NOTICE: using $injector to avoid circular dependency error.
            FormDirtyStateObserver.unobserve();
            
            if (response.status === 401 && !response.config.ignoreAuthModule) {
              // WORKAROUND the progressbar has its own interceptor to control the animation based on HTTP calls. Since
              // the request is not rejected and the promise is returned, the progressbar never completes and animation
              // never stops.
              $timeout(function() {
                cfpLoadingBar.complete();
              }, 500);

              $rootScope.$broadcast('event:auth-loginRequired', response);
            } else {
              if (angular.isObject(response.data) && !response.data.messageTemplate) {
                response.data.messageTemplate = 'server.error.' + response.status;
              }

              if (response.status === 403 && !response.config.ignoreAuthModule) {
                $rootScope.$broadcast('event:auth-notAuthorized', response);
              } else if (!response.config.errorHandler) {
                $rootScope.$broadcast('event:unhandled-server-error', response);
              }
            }

            // otherwise, default behaviour
            return $q.reject(response);
          }
        };
      }])

  /**
   * $http interceptor.
   * On 401 response (without 'ignoreAuthModule' option) stores the request
   * and broadcasts 'event:angular-auth-loginRequired'.
   */
    .config(['$httpProvider', function ($httpProvider) {
      $httpProvider.interceptors.push('MicaHttpInterceptor');
    }]);

  /**
   * Private module, a utility, required internally by 'http-auth-interceptor'.
   */
  angular.module('http-auth-interceptor-buffer', [])

    .factory('httpBuffer', ['$injector', function ($injector) {
      /** Holds all the requests, so they can be re-requested in future. */
      var buffer = [];

      /** Service initialized later because of circular dependency problem. */
      var $http;

      function retryHttpRequest(config, deferred) {
        function successCallback(response) {
          deferred.resolve(response);
        }

        function errorCallback(response) {
          deferred.reject(response);
        }

        $http = $http || $injector.get('$http');
        $http(config).then(successCallback, errorCallback);
      }

      return {
        /**
         * Appends HTTP request configuration object with deferred response attached to buffer.
         */
        append: function (config, deferred) {
          buffer.push({
            config: config,
            deferred: deferred
          });
        },

        /**
         * Abandon or reject (if reason provided) all the buffered requests.
         */
        rejectAll: function (reason) {
          if (reason) {
            for (var i = 0; i < buffer.length; ++i) {
              buffer[i].deferred.reject(reason);
            }
          }
          buffer = [];
        },

        /**
         * Retries all the buffered requests clears the buffer.
         */
        retryAll: function (updater) {
          for (var i = 0; i < buffer.length; ++i) {
            retryHttpRequest(updater(buffer[i].config), buffer[i].deferred);
          }
          buffer = [];
        }
      };
    }]);
})();
