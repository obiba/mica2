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

mica.fileSystem = angular.module('mica.fileSystem', [
  'mica.config',
  'pascalprecht.translate',
  'ui.bootstrap',
  'xeditable',
  'ngFileUpload'
]);

// Workaround for bug #1388
// https://github.com/angular/angular.js/issues/1388
// replace %2F by /

mica.fileSystem.factory('FileSystemInterceptor', ['$q', function ($q) {
  var encodedSlash = new RegExp('%2F', 'g');
  var interceptULRS = [new RegExp('ws/draft/file.*'), new RegExp('ws/file.*')];

  return {
    'request': function (config) {
      var url = config.url;

      for (var i = 0; i < interceptULRS.length; i++) {
        var regex = interceptULRS[i];
        if (url.match(regex)) {
          url = url.replace('/%2F', '/').replace(encodedSlash, '/');
          // end there is only one matching url
          break;
        }
      }
      config.url = url;
      return config || $q.when(config);
    }
  };
}]);

mica.fileSystem.config(['$httpProvider', function ($httpProvider) {
  $httpProvider.interceptors.push('FileSystemInterceptor');
}]);
