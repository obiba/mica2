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

/* Services */

mica.factory('BrowserDetector', ['$window',
  function($window) {

    this.detect = function() {

      var userAgent = $window.navigator.userAgent;
      var browsers = {chrome: /chrome/i, safari: /safari/i, firefox: /firefox/i, ie: /internet explorer|mozilla.*windows nt/i};

      for(var key in browsers) {
        if (browsers[key].test(userAgent)) {
          return key;
        }
      }

      return 'unknown';
    };

    return this;
  }]);


mica.factory('CurrentSession', ['$resource',
  function ($resource) {
    return $resource('ws/auth/session/_current');
  }]);

mica.factory('UserProfile', ['$resource',
  function ($resource) {
    return $resource('ws/user/:id', {}, {
      'get': {method: 'GET', params: {id: '@id'}}
    });
  }]);

mica.factory('Account', ['$resource',
  function ($resource) {
    return $resource('ws/user/_current', {}, {
    });
  }]);

mica.factory('Password', ['$resource',
  function ($resource) {
    return $resource('ws/user/_current/password', {}, {
    });
  }]);

mica.factory('Session', ['SessionProxy','$cookieStore','$translate','UserProfileService', 'amMoment',
  function (SessionProxy, $cookieStore, $translate, UserProfileService, amMoment) {
    this.create = function (login, roles) {
      this.login = login;
      this.roles = roles;
      SessionProxy.update(this);
    };

    this.getPreferredLanguage = function() {
      if (this.profile) {
        if (this.profile.attributes) {
          return UserProfileService.getAttribute(this.profile.attributes,'locale');
        }
      }
      return null;
    };

    this.setProfile = function(profile) {
      this.profile = profile;
      var preferredLanguage = this.getPreferredLanguage();
      $translate.use(preferredLanguage);
      amMoment.changeLocale(preferredLanguage);
      SessionProxy.update(this);
    };

    this.destroy = function() {
      this.login = null;
      this.roles = null;
      this.profile = null;
      $cookieStore.remove('micasid');
      $cookieStore.remove('obibaid');
      SessionProxy.update(this);
    };

    return this;
  }]);

mica.service('AuthenticationSharedService', ['$rootScope', '$q', '$http', '$cookieStore', '$cookies', 'authService', 'Session', 'CurrentSession', 'UserProfile',
  function ($rootScope, $q, $http, $cookieStore, $cookies, authService, Session, CurrentSession, UserProfile) {
    var isInitializingSession = false, isInitializedDeferred = $q.defer(), self = this;

    this.isSessionInitialized = function() {
      return isInitializedDeferred.promise;
    };

    this.initSession = function() {
      var deferred = $q.defer();

      if(!isInitializingSession) {
        isInitializingSession = true;
        CurrentSession.get().$promise.then(function (data) {
          Session.create(data.username, data.roles);
          deferred.resolve(Session);
          authService.loginConfirmed(data);
          return data;
        }).catch(function() {
          deferred.reject();
          return $q.reject();
        }).then(function(data) {
          return UserProfile.get({id: data.username}).$promise;
        }).then(function(data) {
          Session.setProfile(data);
        }).finally(function() {
          isInitializingSession = false;
          isInitializedDeferred.resolve(true);
        });
      }

      return deferred.promise;
    };

    this.login = function (param) {
        $rootScope.authenticationError = false;
        $rootScope.userBannedError = false;
        var data = 'username=' + param.username + '&password=' + param.password;
        $http.post('ws/auth/sessions', data, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          ignoreAuthModule: 'ignoreAuthModule'
        }).then(function success() {
          self.initSession();
        }, function error(response) {
          var resp = response.data;
          if (resp.messageTemplate && resp.messageTemplate === 'error.userBanned') {
            $rootScope.userBannedError = true;
          }
          $rootScope.authenticationError = true;
          Session.destroy();
        });
      };

    this.isAuthenticated = function () {
      return Session.login !== null && Session.login !== undefined;
    };

    this.isAuthorized = function (authorizedRoles) {
        if (!angular.isArray(authorizedRoles)) {
          if (authorizedRoles === '*') {
            return true;
          }

          authorizedRoles = [authorizedRoles];
        }

        var isAuthorized = false;

        angular.forEach(authorizedRoles, function (authorizedRole) {
          var authorized = (!!Session.login &&
            !angular.isUndefined(Session.roles) && Session.roles.indexOf(authorizedRole) !== -1);

          if (authorized || authorizedRole === '*') {
            isAuthorized = true;
          }
        });

        return isAuthorized;
      };

    this.logout = function () {
        $rootScope.authenticationError = false;
        $rootScope.userBanned = false;
        $http({method: 'DELETE', url: 'ws/auth/session/_current', errorHandler: true})
          .then(
            function success() {
              Session.destroy();
              authService.loginCancelled(null, 'logout');
            },
            function error() {
              Session.destroy();
              authService.loginCancelled(null, 'logout failure');
            });
      };
  }]);

mica.factory('MetricsService', ['$resource',
  function ($resource) {
    return $resource('jvm', {}, {
      'get': { method: 'GET'}
    });
  }]);

mica.factory('ThreadDumpService', ['$http',
  function ($http) {
    return {
      dump: function () {
        return $http.get('dump').then(function (response) {
          return response.data;
        });
      }
    };
  }]);

mica.factory('LogsService', ['$resource',
  function ($resource) {
    return $resource('ws/logs', {}, {
      'findAll': { method: 'GET', isArray: true},
      'changeLevel': { method: 'PUT'}
    });
  }]);

mica.factory('CacheService', ['$resource',
  function ($resource) {
    return {
      caches: $resource('ws/caches', {}, {
        'clear': {method: 'DELETE'}
      }),
      cache: $resource('ws/cache/:id', {id : '@id'}, {
        'clear': {method: 'DELETE'},
        'build': {method: 'PUT'}
      })
    };
  }]);

mica.factory('IndexService', ['$resource',
  function ($resource) {
    return {
      all: $resource('ws/config/_index', {}, {
        'build': {method: 'PUT'}
      }),
      networks: $resource('ws/draft/networks/_index', {}, {
        'build': {method: 'PUT'}
      }),
      studies: $resource('ws/draft/individual-studies/_index', {}, {
        'build': {method: 'PUT'}
      }),
      datasets: $resource('ws/draft/datasets/_index', {}, {
        'build': {method: 'PUT'}
      }),
      collectedDatasets: $resource('ws/draft/collected-datasets/_index', {}, {
        'build': {method: 'PUT'}
      }),
      harmonizedDatasets: $resource('ws/draft/harmonized-datasets/_index', {}, {
        'build': {method: 'PUT'}
      }),
      taxonomies: $resource('ws/taxonomies/_index', {}, {
        'build': {method: 'PUT'}
      })
    };
  }]);

mica.factory('MicaMetricsService', ['$resource',
  function($resource) {
    return $resource('ws/config/metrics', {}, {
      get: {method: 'GET'}
    });
  }]);

mica.factory('OidcProvidersResource', ['$resource',
  function ($resource) {
    return $resource('ws/auth/providers', {locale: '@locale'}, {
      'get': { method: 'GET', errorHandler: true, isArray: true }
    });
  }]);

mica.factory('AuditsService', ['$http',
  function ($http) {
    return {
      findAll: function () {
        return $http.get('ws/audits/all').then(function (response) {
          return response.data;
        });
      },
      findByDates: function (fromDate, toDate) {
        return $http.get('ws/audits/byDates', {params: {fromDate: fromDate, toDate: toDate}}).then(function (response) {
          return response.data;
        });
      }
    };
  }]);

mica.factory('MicaUtil', [function() {
  var generateNextId = function (ids) {
    var r = /[0-9]+$/, prevId, matches, newId, i = ids.length - 1;

    while (i > -1) {
      prevId = ids[i];
      matches = r.exec(prevId);

      if (matches && matches.length) {
        newId = prevId.replace(r, parseInt(matches[0], 10) + 1);
      } else {
        newId = prevId + '_1';
      }

      i = ids.indexOf(newId);
    }

    return newId;
  };

  return {
    generateNextId: function(ids) {
      return generateNextId(ids);
    }
  };
}]);

