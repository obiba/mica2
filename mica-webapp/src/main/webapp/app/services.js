'use strict';

/* Services */

mica.factory('Account', ['$resource',
  function ($resource) {
    return $resource('ws/account', {}, {
    });
  }]);

mica.factory('Password', ['$resource',
  function ($resource) {
    return $resource('ws/account/change_password', {}, {
    });
  }]);

mica.factory('Sessions', ['$resource',
  function ($resource) {
    return $resource('ws/account/sessions/:series', {}, {
      'get': { method: 'GET', isArray: true}
    });
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

mica.factory('Session', ['$cookieStore',
  function ($cookieStore) {
    this.create = function (login, firstName, lastName, email, userRoles) {
      this.login = login;
      this.firstName = firstName;
      this.lastName = lastName;
      this.email = email;
      this.userRoles = userRoles;
    };
    this.destroy = function () {
      this.login = null;
      this.firstName = null;
      this.lastName = null;
      this.email = null;
      this.roles = null;
      $cookieStore.remove('account');
    };
    return this;
  }]);

mica.constant('USER_ROLES', {
  all: '*',
  admin: 'ROLE_ADMIN',
  user: 'ROLE_USER'
});

mica.factory('AuthenticationSharedService', ['$rootScope', '$http', '$cookieStore', 'authService', 'Session', 'Account',
  function ($rootScope, $http, $cookieStore, authService, Session, Account) {
    return {
      login: function (param) {
        var data = 'j_username=' + param.username + '&j_password=' + param.password + '&_spring_security_remember_me=' + param.rememberMe + '&submit=Login';
        $http.post('ws/authentication', data, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          ignoreAuthModule: 'ignoreAuthModule'
        }).success(function () {
          Account.get(function (data) {
            Session.create(data.login, data.firstName, data.lastName, data.email, data.roles);
            $cookieStore.put('account', JSON.stringify(Session));
            authService.loginConfirmed(data);
          });
        }).error(function () {
          Session.destroy();
        });
      },
      isAuthenticated: function () {
        if (!Session.login) {
          // check if the user has a cookie
          if ($cookieStore.get('account') !== null) {
            var account = JSON.parse($cookieStore.get('account'));
            Session.create(account.login, account.firstName, account.lastName,
              account.email, account.userRoles);
            $rootScope.account = Session;
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
            Session.userRoles.indexOf(authorizedRole) !== -1);

          if (authorized || authorizedRole === '*') {
            isAuthorized = true;
          }
        });

        return isAuthorized;
      },
      logout: function () {
        $rootScope.authenticationError = false;
        $http.get('ws/logout')
          .success(function () {
            Session.destroy();
            authService.loginCancelled();
          });
      }
    };
  }]);
