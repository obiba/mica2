/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


'use strict';

mica.fileSystem

  .factory('DraftFileSystemFileResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/file/:path/', {path: '@path'}, {
        'get': {method: 'GET', errorHandler: true},
        'delete': {method: 'DELETE', errorHandler: true},
        'rename': {method: 'PUT', params: {name: '@name'}, errorHandler: true},
        'restore': {method: 'PUT', params: {version: '@version'}, errorHandler: true},
        'publish': {method: 'PUT', params: {publish: '@publish'}, errorHandler: true},
        'changeStatus': {method: 'PUT', params: {status: '@status'}, errorHandler: true}
      });
    }])

  .factory('DraftFileSystemFilesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/files', {}, {
        'update': {method: 'POST', errorHandler: true}
      });
    }])

  .factory('DraftFileSystemSearchResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/files-search/:path', {path: '@path'}, {
        'search': {
          method: 'GET',
          isArray: true,
          params: {query: '@query', recursively: '@recursively'},
          errorHandler: true
        }
      });
    }])

  .service('FileSystemService', ['TempFileResource', 'Upload',
    function (TempFileResource, Upload) {

      this.isFile = function (document) {
        return document && document.type === 'FILE';
      };

      this.isRoot = function (document) {
        return document && document.path === '/';
      };

      this.getDocumentTypeTitle = function (type) {
        switch (type) {
          case 'study':
            return 'studies';
        }

        return '';
      };

      this.getLocalizedValue = function(values, lang) {
        if (!values) {
          return null;
        }

        var result = values.filter(function(value) {
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

      this.onFileSelect = function (files, document, successCallback) {
        files.forEach(function (file) {
          Upload
            .upload({
              url: '/ws/files/temp',
              method: 'POST',
              file: file
            })
            .success(function (data, status, getResponseHeaders) {
              var parts = getResponseHeaders().location.split('/');
              var fileId = parts[parts.length - 1];
              TempFileResource.get({id: fileId}, function (tempFile) {
                  var attachment = {
                    fileName: tempFile.name,
                    path: document.path
                  };

                  if (document.children) {
                    document.children.forEach(function (file) {
                      if (file.type === 'FILE' && file.name && file.name === tempFile.name) {
                        attachment = angular.copy(file.state.attachment);
                        delete attachment.timestamps;
                      }
                    });
                  }

                  attachment.id = tempFile.id;
                  attachment.size = tempFile.size;
                  attachment.md5 = tempFile.md5;
                  attachment.justUploaded = true;

                  if (successCallback) {
                    successCallback(attachment);
                  }
                }
              );
            });

          // not handling multiple files
          return false;
        });
      };

    }])

  .service('BreadcrumbHelper', [function () {
    this.toArray = function (path) {
      if (path) {
        var a = path.replace(/\/$/, '').split('/').slice(1);
        var parts = [{name: '/', path: '/'}];
        var prev = null;
        a.forEach(function (part) {
          prev = (prev === null ? '' : prev) + '/' + part;
          parts.push({name: part, path: prev});
        });

        return parts;
      }

      // Should never happen
      return [{name: '', path: ''}];
    };
  }
  ]);
