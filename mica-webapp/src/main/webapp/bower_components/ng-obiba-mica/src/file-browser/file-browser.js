'use strict';

function NgObibaMicaFileBrowserOptionsProvider() {
  var options = {
    locale: 'en',
    folders: {
      excludes: ['population']
    }
  };

  this.setLocale = function(locale) {
    options.locale = locale;
  };

  this.$get = function () {
    return options;
  };
}

angular.module('obiba.mica.fileBrowser', [
  'pascalprecht.translate',
  'ui.bootstrap',
  'templates-ngObibaMica'
]).config(['$provide', function ($provide) {
  $provide.provider('ngObibaMicaFileBrowserOptions', new NgObibaMicaFileBrowserOptionsProvider());
}]);
