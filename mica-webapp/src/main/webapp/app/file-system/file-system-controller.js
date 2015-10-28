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

  .controller('FileSystemController', [
    '$rootScope',
    '$scope',
    '$log',
    'FileSystemService',
    'BreadcrumbHelper',
    'AlertService',
    'ServerErrorUtils',
    'DraftFileSystemFileResource',
    'DraftFileSystemFilesResource',
    'DraftFileSystemSearchResource',
    'MicaConfigResource',
    '$q',

    function ($rootScope,
              $scope,
              $log,
              FileSystemService,
              BreadcrumbHelper,
              AlertService,
              ServerErrorUtils,
              DraftFileSystemFileResource,
              DraftFileSystemFilesResource,
              DraftFileSystemSearchResource,
              MicaConfigResource,
              $q) {

      $scope.selected = [];
      $scope.pagination = {
        currentPage: 1,
        itemsPerPage: 20
      };

      $scope.hasUnselected = true;

      function getCurrentPageDocuments() {
        return $scope.data.document.children.slice(
          $scope.pagination.itemsPerPage * ($scope.pagination.currentPage - 1),
          $scope.pagination.itemsPerPage * $scope.pagination.currentPage);
      }

      $scope.$watch('data.document.children', function (documents) {
        var items;

        if (!$scope.data.document || !$scope.data.document.children) {
          $scope.selected = [];
          $scope.hasUnselected = true;
          return;
        }

        items = getCurrentPageDocuments();

        $scope.hasUnselected = items.filter(function (d) {
          return d.selected;
        }).length < items.length;

        $scope.selected = documents.filter(function (d) {
          return d.selected;
        });

      }, true);

      $scope.$watch('pagination', function () {
        var items;

        if (!$scope.data.document || !$scope.data.document.children) {
          return;
        }

        items = getCurrentPageDocuments();

        $scope.hasUnselected = items.filter(function (d) {
          return d.selected;
        }).length < items.length;
      }, true);

      $scope.selectAll = function () {
        var selection = $scope.selected.length < $scope.data.document.children.length;

        $scope.data.document.children.map(function (d) {
          d.selected = selection;
        });
      };

      $scope.clearSelection = function () {
        $scope.data.document.children.map(function (d) {
          d.selected = false;
        });
      };

      $scope.selectPage = function () {
        var items = getCurrentPageDocuments(),
          selection = items.filter(function (d) { return d.selected; }).length < items.length;

        items.map(function (d) {
          d.selected = selection;
        });
      };

      var onError = function(response) {
        AlertService.alert({
          id: 'FileSystemController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response)
        });

        if ($scope.selected.length === 0 ) {
          navigateToParent($scope.data.document);
        } else {
          navigateTo($scope.data.document);
        }
      };

      var getDocument = function(path) {
        $scope.data.search.active = false;
        $log.debug('getDocument()\'', path, '\'');

        DraftFileSystemFileResource.get({path: path},
          function onSuccess(response) {
            $log.info(response);
            $scope.data.document = response;
            $scope.data.breadcrumbs = BreadcrumbHelper.toArray(path);
            $scope.data.isFile = FileSystemService.isFile(response);
            $scope.data.isRoot = FileSystemService.isRoot(response);

            $log.info('DATA', $scope.data);
          },
          function onError(response) {
            $log.info('!', response);
          }
        );
      };

      var navigateBack = function() {
        if (!$scope.data.isRoot) {
          var parentPath = $scope.data.document.path.replace(/\\/g,'/').replace(/\/[^\/]*$/, '');
          getDocument(parentPath ? parentPath : '/');
        }
      };

      var navigateToPath = function(path) {
        getDocument(path);
      };

      var navigateTo = function(document) {
        navigateToPath(document.path);
      };

      var navigateToParent = function(document) {
        var path = document.path;

        if (path.lastIndexOf('/') === 0) {
          path = '/';
        } else {
          path = path.substring(0, path.lastIndexOf('/'));
        }

        navigateToPath(path);
      };

      var renameDocument = function(form, document, newName) {
        var oldName = document.name;
        var newPath = document.path.replace(document.name, newName);
        DraftFileSystemFileResource.rename({path: document.path, name: newName},
          function onSuccess(){
            navigateToPath(newPath);
          },
          function (response) {
            document.name = oldName;
            onError(response);
          }
        );
      };

      var updateDocumentType = function(document, newType) {
        document.state.attachment.type = newType;
        updateDocument(document);
      };

      var updateDocumentDescription = function(document) {
        if (document.state.attachment.description) {
          $scope.data.editDescField = false;
          updateDocument(document);
        }
      };

      var updateDocument = function(document) {
        DraftFileSystemFilesResource.update(document.state.attachment,
          function onSuccess() {
            navigateTo(document);
          },
          onError
        );
      };

      var deleteDocuments = function () {
        applyToFiles(function (f) {
            return f.permissions.delete && f.revisionStatus === 'DELETED';
          },
          function (path) {
            return DraftFileSystemFileResource.delete({path: path});
          }
        );
      };

      var deleteDocument = function (document) {
        DraftFileSystemFileResource.delete({path: document.path}).$promise.then(function () {
          navigateTo($scope.data.document);
        }, onError);
      };

      var restoreRevision = function(document) {
        $log.info('restoreRevision', document.id);
        DraftFileSystemFileResource.restore(
          {path: $scope.data.document.path, version: document.id},
          function onSuccess() {
            navigateTo($scope.data.document);
          },
          onError
        );
      };

      var onFileSelect = function (files) {
        FileSystemService.onFileSelect(files,
          $scope.data.isFile ? $scope.data.document.state.attachment : $scope.data.document,
          function onSuccess(attachment) {
            return DraftFileSystemFilesResource.update(attachment,
              function onSuccess() {
                navigateTo($scope.data.document);
              },
              onError
            );
          });
      };

      var createFolder = function(nameOrPath) {
        if (!nameOrPath) {
          return;
        }
        var attachment = { id: '', path: $scope.data.document.path + '/' + nameOrPath, fileName: '.' };
        DraftFileSystemFilesResource.update(attachment,
          function onSuccess(response) {
            navigateTo(response);
          },
          onError
        );

        delete $scope.data.new.folder;
      };

      var toggleRecursively = function() {
        $scope.data.search.recursively = !$scope.data.search.recursively;
        if ($scope.data.search.text) {
          searchDocuments($scope.data.search.text);
        }
      };

      var clearSearch = function() {
        $scope.data.search.text = null;
        $scope.data.search.active = false;
        navigateTo($scope.data.document);
      };

      var searchDocuments = function(query) {
        $scope.data.search.active = true;
        var recursively = $scope.data.search.recursively;
        var orderBy = null;
        var sortBy = null;

        switch (query) {
          case 'DELETED':
          case 'UNDER_REVIEW':
            recursively = true;
            query = 'revisionStatus:' + query;
            break;

          case 'RECENT':
            query = '';
            recursively = true;
            orderBy = 'desc';
            sortBy = 'lastModifiedDate';
            break;
        }

        DraftFileSystemSearchResource.search(
          {path: $scope.data.document.path, query: query, recursively: recursively, sort: sortBy, order:orderBy},
          function onSuccess(response) {
            $log.info('Search result', response);
            var clone = angular.copy($scope.data.document);
            clone.children = response;
            $scope.data.document = clone;
          },
          function onError(response) {
            $log.debug(response);
          }
        );
      };

      var isPublished = function() {
        return $scope.data.document && $scope.data.document.state.publicationDate;
      };

      var publish = function(value) {
        applyToFiles(function (f) {
          if(value) {
            return f.permissions.publish && f.revisionStatus === 'UNDER_REVIEW';
          } else {
            return f.permissions.publish && f.state.publicationDate !== undefined;
          }
        }, function (path) {
          return DraftFileSystemFileResource.publish(
            {path: path, publish: value ? 'true' : 'false'});
        });
      };

      var toStatus = function (value) {
        $log.info('STATUS', value);
        applyToFiles(function (f) {
          switch (value) {
            case 'UNDER_REVIEW':
              return f.revisionStatus === 'DRAFT' && f.state.attachment.id !== f.state.publishedId;
            case 'DELETED':
              return f.revisionStatus !== 'DELETED';
            case 'DRAFT':
              return f.revisionStatus !== 'DRAFT';
          }
        }, function (path) {
          return DraftFileSystemFileResource.changeStatus(
            {path: path, status: value}
          );
        });
      };

      function ignoreConflicts(response) {
        if (response.status < 500) {
          return;
        }

        return $q.reject(response);
      }

      function applyToFiles(filter, consumer) {
        var files = $scope.selected.length === 0 ? [$scope.data.document] :
            $scope.selected,
          paths = files.filter(filter).map(function (d) {
            return d.path;
          });

        $q.all(paths.map(function (path) {
          return consumer(path).$promise.catch(function (response) {
            if ($scope.selected.length > 0) {
              return ignoreConflicts(response);
            }

            return $q.reject(response);
          });
        })).catch(onError)
          .finally(function () {
            if ($scope.selected.length === 0) {
              navigateToParent($scope.data.document);
            } else {
              navigateTo($scope.data.document);
            }
          });
      }

      $scope.hasRole = $rootScope.hasRole;
      $scope.getDocumentTypeTitle = FileSystemService.getDocumentTypeTitle;
      $scope.getDocumentIcon = FileSystemService.getDocumentIcon;
      $scope.navigateToPath = navigateToPath;
      $scope.navigateTo = navigateTo;
      $scope.navigateBack = navigateBack;
      $scope.renameDocument = renameDocument;
      $scope.navigateToParent = navigateToParent;
      $scope.updateDocument = updateDocument;
      $scope.updateDocumentType = updateDocumentType;
      $scope.updateDocumentDescription = updateDocumentDescription;
      $scope.deleteDocument = deleteDocument;
      $scope.deleteDocuments = deleteDocuments;
      $scope.restoreRevision = restoreRevision;
      $scope.clearSearch = clearSearch;
      $scope.searchDocuments = searchDocuments;
      $scope.toggleRecursively = toggleRecursively;
      $scope.onFileSelect = onFileSelect;
      $scope.createFolder = createFolder;
      $scope.isFile = FileSystemService.isFile;
      $scope.isRoot = FileSystemService.isRoot;
      $scope.getLocalizedValue = FileSystemService.getLocalizedValue;
      $scope.isPublished = isPublished;
      $scope.publish = publish;
      $scope.toStatus = toStatus;

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        $scope.languages = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
          $scope.languages.push(lang);
        });
      });

      $scope.data = {
        rootPath: null,
        document: null,
        search: {
          text: null,
          active: false,
          recursively: true
        },
        breadcrumbs: null,
        isFile: false,
        isRoot: false,
        editDescField: false
      };

      $scope.$watchGroup(['docPath', 'docId'], function(){
        if ($scope.docPath && $scope.docId) {
          var rootPath = $scope.docPath + ($scope.docId !== 'null' ? '/' + $scope.docId : '');
          getDocument(rootPath, null);
        }
      });

    }]);
