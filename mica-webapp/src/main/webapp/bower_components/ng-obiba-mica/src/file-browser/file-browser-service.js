/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.fileBrowser')

  .factory('FileBrowserFileResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      var url = ngObibaMicaUrl.getUrl('FileBrowserFileResource');
      console.log('PATH>', url);
      return $resource(url, {path: '@path'}, {
        'get': {method: 'GET', errorHandler: true}
      });
    }])

  .factory('FileBrowserSearchResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('FileBrowserSearchResource'), {path: '@path'}, {
        'search': { method: 'GET', isArray: true, errorHandler: true}
      });
    }])

  .service('FileBrowserDownloadService', ['ngObibaMicaUrl', 'ngObibaMicaFileBrowserOptions',
    function (ngObibaMicaUrl, ngObibaMicaFileBrowserOptions) {
      this.getUrl = function(path) {
        return ngObibaMicaUrl.getUrl('FileBrowserDownloadUrl')
          .replace(/:path/, path)
          .replace(/:inline/, ngObibaMicaFileBrowserOptions.downloadInline);
      };

      return this;
    }])


  .service('FileBrowserService', [function () {

      this.isFile = function (document) {
        return document && document.type === 'FILE';
      };

      this.isRoot = function (document) {
        return document && document.path === '/';
      };

      this.getLocalizedValue = function (values, lang) {
        if (!values) {
          return null;
        }

        var result = values.filter(function (value) {
          return value.lang === lang;
        });

        return result && result.length > 0 ? result[0].value : null;
      };

      this.getDocumentIcon = function (document) {
        if (!document) {
          return '';
        }

        if (document.type === 'FOLDER') {
          return 'fa-folder';
        }

        var ext = document.path.match(/\.(\w+)$/);
        if (ext && ext.length > 1) {
          switch (ext[1].toLowerCase()) {
            case 'doc':
            case 'docx':
            case 'odm':
            case 'gdoc':
              return 'fa-file-word-o';

            case 'xls':
            case 'xlsx':
              return 'fa-file-excel-o';

            case 'pdf':
              return 'fa-file-pdf-o';

            case 'ppt':
            case 'odt':
              return 'fa-file-powerpoint-o';

            case 'xt':
              return 'fa-file-text-o';
          }
        }

        return 'fa-file';
      };

    }])

  .service('BrowserBreadcrumbHelper', [function () {
    this.toArray = function (path, exclude) {
      if (path) {
        path = path.replace(exclude, '');
        var a = path.replace(/\/$/, '').split('/').slice(1);
        var parts = [];
        var prev = null;
        a.forEach(function (part) {
          prev = (prev === null ? exclude : prev) + '/' + part;
          parts.push({name: part, path: prev});
        });

        return parts;
      }

      // Should never happen
      return [{name: '', path: ''}];
    };

    this.rootIcon = function(docPath) {
      var matched = /^\/([^\/]*)/.exec(docPath);
      switch (matched ? matched[1] : '') {
        case 'study':
          return 'i-obiba-study';
        case 'network':
          return 'i-obiba-network';
        case 'study-dataset':
          return 'i-obiba-study-dataset';
        case 'harmonization-dataset':
          return 'i-obiba-harmo-dataset';
        default:
          return 'fa fa-hdd-o';
      }
    };
  }]);
