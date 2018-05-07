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

mica.fileSystem

  .controller('FileSystemController', [
    '$rootScope',
    '$scope',
    '$log',
    '$filter',
    'FileSystemService',
    'BreadcrumbHelper',
    'LocationService',
    'AlertService',
    'ServerErrorUtils',
    'DraftFileSystemFileResource',
    'DraftFileSystemFilesResource',
    'DraftFilePermissionResource',
    'DraftFileAccessResource',
    'DraftFileSystemSearchResource',
    'MicaConfigResource',
    '$q',

    function ($rootScope,
              $scope,
              $log,
              $filter,
              FileSystemService,
              BreadcrumbHelper,
              LocationService,
              AlertService,
              ServerErrorUtils,
              DraftFileSystemFileResource,
              DraftFileSystemFilesResource,
              DraftFilePermissionResource,
              DraftFileAccessResource,
              DraftFileSystemSearchResource,
              MicaConfigResource,
              $q) {

      function buildClipboardCommand(command, origin, items) {
        return {
          command: command,
          origin: origin,
          items: items
        };
      }

      $scope.clipboard = buildClipboardCommand(null, null, []);
      $scope.selected = [];
      $scope.pagination = {
        currentPage: 1,
        itemsPerPage: 20
      };

      $scope.hasUnselected = false;

      function getCurrentPageDocuments() {
        return $scope.data.document.children.slice(
          $scope.pagination.itemsPerPage * ($scope.pagination.currentPage - 1),
          $scope.pagination.itemsPerPage * $scope.pagination.currentPage);
      }

      $scope.$watch('data.document.children', function (documents) {
        var items;

        if (!$scope.data.document) {
          $scope.selected = [];
          $scope.hasUnselected = false;
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

        if (!$scope.data.document) {
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

        if (response.status !== 403 && $scope.data.document) {
          navigateTo($scope.data.document);
        } else {
          navigateToPath('/');
        }
      };

      var getDocument = function(path) {
        $scope.data.search.active = false;

        DraftFileSystemFileResource.get({path: path},
          function onSuccess(response) {
            $log.info(response);
            $scope.data.document = response;

            if (!$scope.data.document.children) {
              $scope.data.document.children = [];
            }

            if ($scope.data.document.permissions.edit) {
              DraftFilePermissionResource.query({path: $scope.data.document.path}, function onSuccess(response) {
                $scope.data.permissions = response;
              });
              DraftFileAccessResource.query({path: $scope.data.document.path}, function onSuccess(response) {
                $scope.data.accesses = response;
              });
            }

            $scope.data.breadcrumbs = BreadcrumbHelper.toArray(path);
            $scope.data.isFile = FileSystemService.isFile(response);
            $scope.data.isRoot = FileSystemService.isRoot(response);

            if (LocationService.hasSearchQuery()) {
              var searchParams =LocationService.getSearchQueryParams();
              $scope.data.search.active = true;
              $scope.data.search.text = searchParams.query;
              $scope.data.search.recursively =
                searchParams.hasOwnProperty('recursively') && searchParams.recursively !== 'false';
              searchDocumentsInternal($scope.data.document.path, searchParams);
            } else {
              LocationService.update(path);
            }
          },
          onError
        );
      };

      var navigateBack = function() {
        if (!$scope.data.isRoot && $scope.data.document) {
          var parentPath = $scope.data.document.path.replace(/\\/g,'/').replace(/\/[^\/]*$/, '');
          getDocument(parentPath ? parentPath : '/');
        }
      };

      var navigateToPath = function(path) {
        clearSearchInternal();
        getDocument(path);
      };

      var navigateTo = function(document) {
        if (document) {
          navigateToPath(document.path);
        }
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
        applyToFiles(getSelection(function (f) {
            return f.permissions.delete && f.revisionStatus === 'DELETED';
          }),
          function (f) {
            return DraftFileSystemFileResource.delete({path: f.path});
          })
          .finally(function () {
            if ($scope.selected.length === 0) {
              navigateToParent($scope.data.document);
            } else {
              navigateTo($scope.data.document);
            }
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
        FileSystemService.onFileSelect(files, $scope.data.document,
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
        } else if ($scope.data.search.query) {
          searchDocuments($scope.data.search.query);
        }
      };

      function clearSearchInternal() {
        if ($scope.data.document) {
          LocationService.update($scope.data.document.path);
        }
        $scope.data.search.text = null;
        $scope.data.search.active = false;
      }

      var clearSearch = function() {
        clearSearchInternal();
        getDocument($scope.data.document.path);
      };

      var searchKeyUp = function(event) {
        switch(event.keyCode) {
          case 13: // ENTER
            if ($scope.data.search.text) {
              searchDocuments($scope.data.search.text);
            } else {
              clearSearch();
            }
            break;

          case 27: // ESC
            if ($scope.data.search.active) {
              clearSearch();
            }
            break;
        }
      };

      function searchDocumentsInternal(path, searchParams) {
        var urlParams = angular.extend({}, {path: path}, searchParams);
        DraftFileSystemSearchResource.search(urlParams,
          function onSuccess(response) {
            $log.info('Search result', response);
            LocationService.update(path, searchParams);
            var clone = $scope.data.document ? angular.copy($scope.data.document) : {};
            clone.children = response;
            $scope.data.document = clone;
          },
          function onError(response) {
            $log.debug(response);
          }
        );
      }

      var searchDocuments = function(query) {
        $scope.data.search.active = true;
        var recursively = $scope.data.search.recursively;
        var orderBy = null;
        var sortBy = null;
        var limit = 999;
        $scope.data.search.query = query;

        switch (query) {
          case 'DELETED':
          case 'UNDER_REVIEW':
            query = 'revisionStatus:' + query;
            break;

          case 'NOT_PUBLISHED':
            query = 'NOT(publicationDate:*)';
            break;

          case 'RECENT':
            query = '';
            orderBy = 'desc';
            sortBy = 'lastModifiedDate';
            limit = 10;
            break;
        }

        var searchParams = {query: query, recursively: recursively, sort: sortBy, order:orderBy, limit: limit};
        searchDocumentsInternal($scope.data.document.path, searchParams);
      };

      var isPublished = function() {
        return $scope.data.document && $scope.data.document.state.publicationDate;
      };

      var publish = function(value) {
        applyToFiles(getSelection(function (f) {
          if (value) {
            return f.permissions.publish && f.revisionStatus === 'UNDER_REVIEW';
          } else {
            return f.permissions.publish && f.state.publicationDate !== undefined;
          }
        }), function (f) {
          return DraftFileSystemFileResource.publish(
            {path: f.path, publish: value ? 'true' : 'false'});
        }).finally(function () {
          navigateTo($scope.data.document);
        });
      };

      var toStatus = function (value) {
        $log.info('STATUS', value);
        applyToFiles(getSelection(function (f) {
          switch (value) {
            case 'UNDER_REVIEW':
              return f.revisionStatus === 'DRAFT' && f.state.attachment.id !== f.state.publishedId;
            case 'DELETED':
              return f.revisionStatus !== 'DELETED';
            case 'DRAFT':
              return f.revisionStatus !== 'DRAFT';
          }
        }), function (f) {
          return DraftFileSystemFileResource.changeStatus(
            {path: f.path, status: value}
          );
        }).finally(function () {
          navigateTo($scope.data.document);
        });
      };

      function ignoreConflicts(response) {
        if (response.status < 500) {
          return;
        }

        return $q.reject(response);
      }

      function getSelection(filter) {
        var files = $scope.selected.length === 0 ? [$scope.data.document] :
            $scope.selected;

        return files.filter(filter);
      }

      function applyToFiles(files, consumer) {
        return $q.all(files.map(function (f) {
          return consumer(f).$promise.catch(function (response) {
            if (files.length > 1) {
              return ignoreConflicts(response);
            }

            return $q.reject(response);
          });
        })).catch(onError);
      }

      $scope.copyFilesToClipboard = function () {
        var selected = $scope.selected.length ? angular.copy($scope.selected) : [$scope.data.document];

        $scope.clipboard = buildClipboardCommand('copy', $scope.data.document.path, selected.filter(function (d) {
          return d.permissions.view;
        }));
      };

      $scope.cutFilesToClipboard = function () {
        var selected = $scope.selected.length ? angular.copy($scope.selected) : [$scope.data.document];

        $scope.clipboard = buildClipboardCommand('move', $scope.data.document.path, selected.filter(function (d) {
          return d.permissions.edit && d.revisionStatus === 'DRAFT';
        }));
      };

      $scope.pasteFilesFromClipboard = function () {
        if ($scope.data.document.path === $scope.clipboard.origin) {
          AlertService.alert({
            id: 'FileSystemController',
            type: 'danger',
            msgKey: 'file.invalid-paste',
            delay: 5000
          });

          return;
        }

        applyToFiles($scope.clipboard.items, function (d) {
          return DraftFileSystemFileResource[$scope.clipboard.command]({
            path: d.path,
            destinationFolder: d.type === 'FOLDER' ? [$scope.data.document.path, d.name].join('/') : $scope.data.document.path
          });
        }).finally(function () {
          $scope.clipboard = buildClipboardCommand(null, null, []);
          navigateTo($scope.data.document);
        });
      };

      $scope.loadPermissions = function (document) {
        DraftFilePermissionResource.query({path: document.path}, function onSuccess(response) {
          $scope.data.permissions = response;
        });
      };

      $scope.deletePermission = function (document, perm) {
        DraftFilePermissionResource.delete({path: document.path}, perm, function onSuccess() {
          $scope.loadPermissions(document);
        });
      };

      $scope.addPermission = function (document, perm) {
        var submittedPerm = angular.copy(perm);
        submittedPerm.type = perm.type.name ? perm.type.name : 'USER';
        submittedPerm.role = perm.role.name ? perm.role.name : 'READER';
        DraftFilePermissionResource.save({path: document.path}, submittedPerm, function onSuccess() {
          $scope.loadPermissions(document);
          $scope.data.permission = { type: $scope.SUBJECT_TYPES[0], role: $scope.SUBJECT_ROLES[0] };
        });
        $scope.data.addPermission = false;
      };

      $scope.loadAccesses = function (document) {
        DraftFileAccessResource.query({path: document.path}, function onSuccess(response) {
          $scope.data.accesses = response;
        });
      };

      $scope.deleteAccess = function (document, access) {
        DraftFileAccessResource.delete({path: document.path}, access, function onSuccess() {
          $scope.loadAccesses(document);
        });
      };

      $scope.addAccess = function (document, access) {
        var submittedAccess = angular.copy(access);
        submittedAccess.type = access.type.name ? access.type.name : 'USER';
        DraftFileAccessResource.save({path: document.path}, submittedAccess, function onSuccess() {
          $scope.loadAccesses(document);
          $scope.data.access = { type: $scope.SUBJECT_TYPES[0] };
        });
        $scope.data.addAccess = false;
      };

      $scope.SUBJECT_TYPES = [
        {name: 'USER', label: $filter('translate')('permission.user')},
        {name: 'GROUP', label: $filter('translate')('permission.group')}
      ];

      $scope.SUBJECT_ROLES = [
        {name: 'READER', label: $filter('translate')('permission.reader')},
        {name: 'EDITOR', label: $filter('translate')('permission.editor')},
        {name: 'REVIEWER', label: $filter('translate')('permission.reviewer')}
      ];

      $scope.screen = $rootScope.screen;
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
      $scope.searchKeyUp = searchKeyUp;
      $scope.createFolder = createFolder;
      $scope.isFile = FileSystemService.isFile;
      $scope.isRoot = FileSystemService.isRoot;
      $scope.getLocalizedValue = FileSystemService.getLocalizedValue;
      $scope.isPublished = isPublished;
      $scope.publish = publish;
      $scope.toStatus = toStatus;

      MicaConfigResource.get(function (micaConfig) {
        $scope.openAccess = micaConfig.openAccess;
        $scope.tabs = [];
        $scope.languages = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
          $scope.languages.push(lang);
        });
        $scope.isNetworkEnabled = micaConfig.isNetworkEnabled;
        $scope.isCollectedDatasetEnabled = micaConfig.isCollectedDatasetEnabled;
        $scope.isHarmonizedDatasetEnabled = micaConfig.isHarmonizedDatasetEnabled;
      });

      $scope.data = {
        rootPath: null,
        document: null,
        permissions: [],
        permission: { type: $scope.SUBJECT_TYPES[0], role: $scope.SUBJECT_ROLES[0] },
        accesses: [],
        access: { type: $scope.SUBJECT_TYPES[0] },
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

          if (LocationService.hasPathQuery()) {
            rootPath = LocationService.getPathQuery();
          }

          getDocument(rootPath, null);
        }
      });

    }]);
