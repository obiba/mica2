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

/* App Module */


var mica = angular.module('mica', [
  'obibaShims',
  'angular-loading-bar',
  'http-auth-interceptor',
  'localytics.directives',
  'mica.config',
  'ngObiba',
  'mica.admin',
  'mica.sets',
  'mica.network',
  'mica.study',
  'mica.dataset',
  'mica.dataAccessConfig',
  'mica.projectConfig',
  'mica.entityConfig',
  'mica.data-access-request',
  'mica.search',
  'mica.analysis',
  'mica.project',
  'ngCookies',
  'ngResource',
  'ngRoute',
  'pascalprecht.translate',
  'tmh.dynamicLocale',
  'xeditable',
  'matchMedia',
  'ngObibaMica',
  'ui.bootstrap',
  'schemaForm',
  'schemaForm-datepicker',
  'sfLocalizedString',
  'sfObibaFileUpload',
  'sfCheckboxgroup',
  'sfTypeahead',
  'sfObibaCountriesUiSelect',
  'sfRadioGroupCollection',
  'hc.marked',
  'ngclipboard'
]);

mica
  .provider('SessionProxy',
    function () {
      function Proxy() {
        var real;
        this.update = function (value) {
          real = value;
        };

        this.login = function() {
          return real.login;
        };

        this.roles = function() {
          return real.roles || [];
        };

        this.profile = function() {
          return real.profile;
        };
      }

      this.$get = function() {
        return new Proxy();
      };
    });

mica
  .config(['$routeProvider',
    '$httpProvider',
    '$translateProvider',
    '$locationProvider',
    'tmhDynamicLocaleProvider',
    'AlertBuilderProvider',
    'USER_ROLES',
    'ObibaServerConfigResourceProvider',
    '$qProvider',
    function ($routeProvider,
              $httpProvider,
              $translateProvider,
              $locationProvider,
              tmhDynamicLocaleProvider,
              AlertBuilderProvider,
              USER_ROLES,
              ObibaServerConfigResourceProvider,
              $qProvider) {

      $qProvider.errorOnUnhandledRejections(false);
      $locationProvider.hashPrefix('');

      AlertBuilderProvider.setMsgKey('global.server-error');
      AlertBuilderProvider.setAlertId('MainController');
      AlertBuilderProvider.setGrowlId('MainControllerGrowl');
      AlertBuilderProvider.setModeAlert();

      $routeProvider
        .when('/login', {
          templateUrl: 'app/views/login.html',
          controller: 'LoginController',
          reloadOnSearch: false,
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        })
        .when('/error', {
          templateUrl: 'app/views/error.html',
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        })
        .when('/logout', {
          templateUrl: 'app/views/main.html',
          controller: 'LogoutController',
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        })
        .otherwise({
          templateUrl: 'app/views/main.html',
          controller: 'MainController',
          access: {
            authorizedRoles: [USER_ROLES.all]
          }
        });

      // Initialize angular-translate
      $translateProvider
        .useStaticFilesLoader({
          prefix: 'ws/config/i18n/',
          suffix: '.json'
        })
        .registerAvailableLanguageKeys(['en', 'fr'], {
          'en_*': 'en',
          'fr_*': 'fr',
          '*': 'en'
        })
        .determinePreferredLanguage()
        .fallbackLanguage('en')
        .useCookieStorage()
        .useSanitizeValueStrategy('escaped');

      tmhDynamicLocaleProvider.localeLocationPattern('bower_components/angular-i18n/angular-locale_{{locale}}.js');
      tmhDynamicLocaleProvider.useCookieStorage('NG_TRANSLATE_LANG_KEY');

      ObibaServerConfigResourceProvider.setFactory(
        ['MicaConfigResource', function(MicaConfigResource){
          return {get: MicaConfigResource.get};
        }]
      );

    }])

  .run(['$rootScope',
    '$location',
    '$route',
    '$http',
    'AuthenticationSharedService',
    'Session',
    'USER_ROLES',
    'ServerErrorUtils',
    'UserProfileService',
    'editableOptions',
    'amMoment',
    '$cookies',

    function ($rootScope,
              $location,
              $route,
              $http,
              AuthenticationSharedService,
              Session,
              USER_ROLES,
              ServerErrorUtils,
              UserProfileService,
              editableOptions,
              amMoment,
              $cookies) {

      var langKey = $cookies.get('NG_TRANSLATE_LANG_KEY');
      amMoment.changeLocale(langKey ? langKey.replace(/"/g, '') : 'en');

      var isSessionInitialized = false;

      function updateRedirect() {
        var path = $location.path();
        var invalidRedirectPaths = ['', '/error', '/logout', '/login'];
        if (invalidRedirectPaths.indexOf(path) === -1) {
          // save path to navigate to after login
          var search = $location.search();
          search.redirect = path;
          $location.search(search);
        }
      }

      function login() {
        if (!$rootScope.routeToLogin) {
          $rootScope.routeToLogin = true;
          updateRedirect();
        }
        $location.path('/login').replace();
      }

      $rootScope.$on('$routeChangeStart', function (event, next) {
        if(!isSessionInitialized) {
          event.preventDefault();
          AuthenticationSharedService.isSessionInitialized().then(function() {
            $route.reload();
          });
        } else {
          editableOptions.theme = 'bs3';
          $rootScope.authenticated = AuthenticationSharedService.isAuthenticated();
          $rootScope.hasRole = AuthenticationSharedService.isAuthorized;
          $rootScope.userRoles = USER_ROLES;
          $rootScope.subject = Session;
          $rootScope.UserProfileService = UserProfileService;

          if (!$rootScope.authenticated) {
            if ('/login' !== $location.path()) {
              delete $rootScope.routeToLogin;
            }

            $rootScope.$broadcast('event:auth-loginRequired');
          } else if (!AuthenticationSharedService.isAuthorized(next.access ? next.access.authorizedRoles : '*')) {
            $rootScope.$broadcast('event:auth-notAuthorized');
          }
        }
      });

      // Call when the the client is confirmed
      $rootScope.$on('event:auth-loginConfirmed', function () {
        delete $rootScope.routeToLogin;

        if ($location.path() === '/login') {
          var path = '/';
          var search = $location.search();
          if (search.hasOwnProperty('redirect')) {
            path = search.redirect;
            delete search.redirect;
          }
          $location.path(path).search(search).replace();
        }
      });

      $rootScope.$on('event:auth-loginRequired', function () {
        Session.destroy();
        login();
      });

      // Call when the 403 response is returned by the server
      $rootScope.$on('event:auth-notAuthorized', function () {
        if (!$rootScope.authenticated) {
          login();
        } else {
          $rootScope.errorMessage = 'errors.403';
          $location.path('/error').replace();
        }
      });

      $rootScope.$on('event:unhandled-server-error', function (event, response) {
        $rootScope.errorMessage = ServerErrorUtils.buildMessage(response);
        $location.path('/error').replace();
      });

      // Call when the user logs out
      $rootScope.$on('event:auth-loginCancelled', function () {
        $rootScope.authenticated = undefined;
        login();
      });

      AuthenticationSharedService.initSession().finally(function() {
        isSessionInitialized = true;
      });
    }]);
