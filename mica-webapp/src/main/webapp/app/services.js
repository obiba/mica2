/* global document */

'use strict';

mica.constant('USER_ROLES', {
  all: '*',
  admin: 'mica-administrator',
  reviewer: 'mica-reviewer',
  editor: 'mica-editor',
  user: 'mica-user',
  dao: 'mica-data-access-officer'
});

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


mica.factory('UserProfileService',
  function () {

    var getAttributeValue = function(attributes, key) {
      var result = attributes.filter(function (attribute) {
        return attribute.key === key;
      });

      return result && result.length > 0 ? result[0].value : null;
    };

    return {

      'getAttribute': function (attributes, key) {
        return getAttributeValue(attributes, key);
      },

      'getFullName': function (profile) {
        if (profile) {
          if (profile.attributes) {
            return getAttributeValue(profile.attributes, 'firstName') + ' ' + getAttributeValue(profile.attributes, 'lastName');
          }
          return profile.username;
        }
        return null;
      }
    };
  });

mica.factory('Session', ['$cookieStore',
  function ($cookieStore) {
    this.create = function (login, roles) {
      this.login = login;
      this.roles = roles;
    };
    this.setProfile = function(profile) {
      this.profile = profile;
    };
    this.destroy = function () {
      this.login = null;
      this.roles = null;
      this.profile = null;
      $cookieStore.remove('mica_subject');
      $cookieStore.remove('micasid');
      $cookieStore.remove('obibaid');
    };
    return this;
  }]);

mica.factory('AuthenticationSharedService', ['$rootScope', '$http', '$cookieStore', '$cookies', 'authService', 'Session', 'CurrentSession', 'UserProfile',
  function ($rootScope, $http, $cookieStore, $cookies, authService, Session, CurrentSession, UserProfile) {
    return {
      login: function (param) {
        $rootScope.authenticationError = false;
        var data = 'username=' + param.username + '&password=' + param.password;
        $http.post('ws/auth/sessions', data, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          ignoreAuthModule: 'ignoreAuthModule'
        }).success(function () {
          CurrentSession.get(function (data) {
            Session.create(data.username, data.roles);
            $cookieStore.put('mica_subject', JSON.stringify(Session));
            authService.loginConfirmed(data);

            UserProfile.get({id: data.username}, function(data){
              Session.setProfile(data);
            });
          });
        }).error(function () {
          $rootScope.authenticationError = true;
          Session.destroy();
        });
      },
      isAuthenticated: function () {
        // WORKAROUND: until next angular update, cookieStore is currently buggy
        function getSidCookie(app) {
          var regexp = new RegExp(app + '=([^;]+)', 'g');
          var result = regexp.exec(document.cookie);
          return (result === null) ? null : result[1];
        }

        if (angular.isUndefined(getSidCookie('obibaid')) && angular.isUndefined(getSidCookie('micasid'))) {
          // session has terminated, cleanup
          Session.destroy();
          return false;
        }

        // check for Session object state
        if (!Session.login) {
          // check if there is a cookie for the subject
          var subjectCookie = $cookieStore.get('mica_subject');
          if (subjectCookie !== null && subjectCookie) {
            var account = JSON.parse($cookieStore.get('mica_subject'));
            Session.create(account.login, account.roles);
            UserProfile.get({id: account.login}, function(data){
              Session.setProfile(data);
            });
            $rootScope.account = Session;
            return true;
          }
          // check if there is a Obiba session
          var obibaCookie = $cookies.obibaid;
          if (obibaCookie !== null && obibaCookie) {
            CurrentSession.get(function (data) {
              Session.create(data.username, data.roles);
              $cookieStore.put('mica_subject', JSON.stringify(Session));
              authService.loginConfirmed(data);
            });
          }
        }
        return !!Session.login;
      },
      isAuthorized: function (authorizedRoles) {
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
      },
      logout: function () {
        $rootScope.authenticationError = false;
        $http({method: 'DELETE', url: 'ws/auth/session/_current', errorHandler: true})
          .success(function () {
            Session.destroy();
            authService.loginCancelled(null, 'logout');
          }).error(function () {
            Session.destroy();
            authService.loginCancelled(null, 'logout failure');
          }
        );
      }
    };
  }]);

mica.factory('MetricsService', ['$resource',
  function ($resource) {
    return $resource('metrics/metrics', {}, {
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

mica.factory('HealthCheckService', ['$rootScope', '$http',
  function ($rootScope, $http) {
    return {
      check: function () {
        return $http.get('health').then(function (response) {
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
  var generateNextId = function(prevId) {
    var r= /[0-9]+$/,
      matches = r.exec(prevId);

    if (matches && matches.length) {
      return prevId.replace(r, parseInt(matches[0], 10) + 1);
    }

    return prevId ? prevId + '_1' : '';
  };

  return {
    generateNextId: function(prevId) {
      if(angular.isArray(prevId)) {
        var res = [];

        for(var i = 0; i < prevId.length; i++) {
          res[i] = {lang: prevId[i].lang, value: generateNextId(prevId[i].value)};
        }

        return res;
      }

      return generateNextId(prevId);
    }
  };
}]);

