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

/* Controllers */

mica.controller('MainController', [
  '$rootScope',
  '$scope',
  '$window',
  'MicaConfigResource',
  'PublicMicaConfigResource',
  'screenSize',
  'AuthenticationSharedService',
  '$cacheFactory',
  function ($rootScope, $scope, $window, MicaConfigResource, PublicMicaConfigResource, screenSize, AuthenticationSharedService, $cacheFactory) {
    function getScreenSize() {
      var size = ['lg', 'md', 'sm', 'xs'].filter(function (size) {
        return screenSize.is(size);
      });

      $scope.screen.size = size ? size[0] : 'lg';
      $scope.screen.device = screenSize.is('md, lg') ? 'desktop' : 'mobile';
      $scope.screen.is = screenSize.is;
    }

    function applyTitle(config) {
      $window.document.title = config.name;
    }

    if (AuthenticationSharedService.isAuthenticated()) {
      $scope.micaConfig = MicaConfigResource.get(applyTitle);
    } else {
      $scope.micaConfig = PublicMicaConfigResource.get(applyTitle);
    }

    $rootScope.screen = $scope.screen = {size: null, device: null};

    $rootScope.$on('event:auth-loginConfirmed', function () {
      $scope.micaConfig = MicaConfigResource.get();

      var taxonomyResourceCache = $cacheFactory.get('taxonomyResource');
      if (taxonomyResourceCache) {
        taxonomyResourceCache.removeAll();
      }

      var taxonomiesResourceCache =  $cacheFactory.get('taxonomiesResource');
      if (taxonomiesResourceCache) {
        taxonomiesResourceCache.removeAll();
      }
    });

    getScreenSize();

    screenSize.on('lg, md, sm, xs', function () {
      getScreenSize();
    });
  }]);

mica.controller('AdminController', [function () {}]);

mica.controller('LanguageController', ['$scope', '$translate', 'amMoment', 'PublicMicaConfigResource',
  function ($scope, $translate, amMoment, PublicMicaConfigResource) {
    $scope.changeLanguage = function (languageKey) {
      $translate.use(languageKey);
      amMoment.changeLocale(languageKey);
    };
    $scope.getCurrentLanguage = $translate.use;

    $scope.publicMicaConfig = PublicMicaConfigResource.get(function (config) {
      $scope.languages = config.languages;
      // allow switching to languages translated by default
      ['fr','en'].forEach(function (lang) {
        if (config.languages.indexOf(lang) === -1) {
          $scope.languages.unshift(lang);
        }
      });
      $scope.languages.sort();
    });
  }]);

mica.controller('MenuController', [function () {}]);

mica.controller('LoginController',
  ['$scope',
    '$location',
    '$window',
    '$translate',
    'PublicMicaConfigResource',
    'OidcProvidersResource',
    'AuthenticationSharedService',
  function ($scope,
            $location,
            $window,
            $translate,
            PublicMicaConfigResource,
            OidcProvidersResource,
            AuthenticationSharedService) {

    function getRedirectUrl(providerName) {
      return $scope.config.agateUrl + '/auth/signin/' + providerName + '?redirect=' + new $window.URL($location.absUrl()).origin;
    }

    function login() {
      AuthenticationSharedService.login({
        username: $scope.username,
        password: $scope.password,
        success: function () {
          $location.path('');
        }
      });
    }

    PublicMicaConfigResource.get().$promise.then(function (config) {
      $scope.config = config;
      OidcProvidersResource.get({locale: $translate.use()}).$promise.then(function (providers) {
        $scope.providers = providers;
      });
    });

    $scope.getRedirectUrl = getRedirectUrl;
    $scope.login = login;
  }]);

mica.controller('LogoutController', ['$location', 'AuthenticationSharedService',
  function ($location, AuthenticationSharedService) {
    AuthenticationSharedService.logout({
      success: function () {
        $location.path('');
      }
    });
  }]);

mica.controller('SettingsController', ['$scope', 'Account',
  function ($scope, Account) {
    $scope.success = null;
    $scope.error = null;
    $scope.settingsAccount = Account.get();

    $scope.save = function () {
      Account.save($scope.settingsAccount,
        function () {
          $scope.error = null;
          $scope.success = 'OK';
          $scope.settingsAccount = Account.get();
        },
        function () {
          $scope.success = null;
          $scope.error = 'ERROR';
        });
    };
  }]);

mica.controller('PasswordController', ['$scope', 'Password',
  function ($scope, Password) {
    $scope.success = null;
    $scope.error = null;
    $scope.doNotMatch = null;
    $scope.changePassword = function () {
      if ($scope.password !== $scope.confirmPassword) {
        $scope.doNotMatch = 'ERROR';
      } else {
        $scope.doNotMatch = null;
        Password.save($scope.password,
          function () {
            $scope.error = null;
            $scope.success = 'OK';
          },
          function () {
            $scope.success = null;
            $scope.error = 'ERROR';
          });
      }
    };
  }]);

mica.controller('SessionsController', ['$scope', 'resolvedSessions', 'Sessions',
  function ($scope, resolvedSessions, Sessions) {
    $scope.success = null;
    $scope.error = null;
    $scope.sessions = resolvedSessions;
    $scope.invalidate = function (series) {
      Sessions.delete({series: encodeURIComponent(series)},
        function () {
          $scope.error = null;
          $scope.success = 'OK';
          $scope.sessions = Sessions.get();
        },
        function () {
          $scope.success = null;
          $scope.error = 'ERROR';
        });
    };
  }]);

mica.controller('AuditsController', ['$scope', '$translate', '$filter', 'AuditsService',
  function ($scope, $translate, $filter, AuditsService) {
    $scope.onChangeDate = function () {
      AuditsService.findByDates($scope.fromDate, $scope.toDate).then(function (data) {
        $scope.audits = data;
      });
    };

    // Date picker configuration
    $scope.today = function () {
      // Today + 1 day - needed if the current day must be included
      var today = new Date();
      var tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1); // create new increased date

      $scope.toDate = $filter('date')(tomorrow, 'yyyy-MM-dd');
    };

    $scope.previousMonth = function () {
      var fromDate = new Date();
      if (fromDate.getMonth() === 0) {
        fromDate = new Date(fromDate.getFullYear() - 1, 0, fromDate.getDate());
      } else {
        fromDate = new Date(fromDate.getFullYear(), fromDate.getMonth() - 1, fromDate.getDate());
      }

      $scope.fromDate = $filter('date')(fromDate, 'yyyy-MM-dd');
    };

    $scope.today();
    $scope.previousMonth();

    AuditsService.findByDates($scope.fromDate, $scope.toDate).then(function (data) {
      $scope.audits = data;
    });
  }]);

