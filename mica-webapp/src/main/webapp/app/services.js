'use strict';

mica.constant('USER_ROLES', {
  all: '*',
  admin: 'mica-administrator',
  user: 'mica-user'
});

/* Services */

mica.factory('CurrentSession', ['$resource',
  function ($resource) {
    return $resource('ws/auth/session/_current');
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

mica.factory('Session', ['$cookieStore',
  function ($cookieStore) {
    this.create = function (login, role) {
      this.login = login;
      this.role = role;
    };
    this.destroy = function () {
      this.login = null;
      this.role = null;
      $cookieStore.remove('mica_subject');
      $cookieStore.remove('micasid');
      $cookieStore.remove('obibaid');
    };
    return this;
  }]);

mica.factory('AuthenticationSharedService', ['$rootScope', '$http', '$cookieStore', '$cookies', 'authService', 'Session', 'CurrentSession',
  function ($rootScope, $http, $cookieStore, $cookies, authService, Session, CurrentSession) {
    return {
      login: function (param) {
        var data = 'username=' + param.username + '&password=' + param.password;
        $http.post('ws/auth/sessions', data, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          ignoreAuthModule: 'ignoreAuthModule'
        }).success(function () {
          CurrentSession.get(function (data) {
            Session.create(data.username, data.role);
            $cookieStore.put('mica_subject', JSON.stringify(Session));
            authService.loginConfirmed(data);
          });
        }).error(function () {
          Session.destroy();
        });
      },
      isAuthenticated: function () {
        // check for Session object state
        if (!Session.login) {
          // check if there is a cookie for the subject
          var subject_cookie = $cookieStore.get('mica_subject');
          if (subject_cookie !== null && subject_cookie) {
            var account = JSON.parse($cookieStore.get('mica_subject'));
            Session.create(account.login, account.role);
            $rootScope.account = Session;
            return true;
          }
          // check if there is a Obiba session
          var obiba_cookie = $cookies.obibaid;
          if (obiba_cookie !== null && obiba_cookie) {
            CurrentSession.get(function (data) {
              Session.create(data.username, data.role);
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
            Session.role === authorizedRole);

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