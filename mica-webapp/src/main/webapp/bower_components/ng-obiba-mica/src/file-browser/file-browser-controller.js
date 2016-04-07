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

  .controller('FileBrowserController', [
    '$rootScope',
    '$scope',
    '$log',
    '$filter',
    'StringUtils',
    'FileBrowserService',
    'BrowserBreadcrumbHelper',
    'AlertService',
    'ServerErrorUtils',
    'FileBrowserFileResource',
    'FileBrowserSearchResource',
    'ngObibaMicaFileBrowserOptions',
    'FileBrowserDownloadService',

    function ($rootScope,
              $scope,
              $log,
              $filter,
              StringUtils,
              FileBrowserService,
              BrowserBreadcrumbHelper,
              AlertService,
              ServerErrorUtils,
              FileBrowserFileResource,
              FileBrowserSearchResource,
              ngObibaMicaFileBrowserOptions,
              FileBrowserDownloadService) {

      var onError = function (response) {
        AlertService.alert({
          id: 'FileSystemController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response)
        });

        if (response.status !== 403 && $scope.data.document) {
          navigateTo($scope.data.document);
        }
      };

      var getDocument = function (path) {
        $scope.data.search.active = false;

        FileBrowserFileResource.get({path: path},
          function onSuccess(response) {
            $log.info(response);
            $scope.data.document = response;

            if (!$scope.data.document.children) {
              $scope.data.document.children = [];
            } else {
              $scope.data.document.children = $scope.data.document.children.filter(function(child){
                return ngObibaMicaFileBrowserOptions.folders.excludes.indexOf(child.name) < 0;
              });
            }

            $scope.data.breadcrumbs = BrowserBreadcrumbHelper.toArray(path, $scope.data.rootPath);
            $scope.data.isFile = FileBrowserService.isFile(response);
            $scope.data.isRoot = FileBrowserService.isRoot(response);
          },
          onError
        );
      };

      var navigateBack = function () {
        if (!$scope.data.isRoot && $scope.data.document) {
          var parentPath = $scope.data.document.path.replace(/\\/g, '/').replace(/\/[^\/]*$/, '');
          getDocument(parentPath ? parentPath : '/');
        }
      };

      var navigateToPath = function (path) {
        clearSearchInternal();
        getDocument(path);
      };

      var navigateTo = function (document) {
        if (document) {
          navigateToPath(document.path);
        }
      };

      var navigateToParent = function (document) {
        var path = document.path;

        if (path.lastIndexOf('/') === 0) {
          path = '/';
        } else {
          path = path.substring(0, path.lastIndexOf('/'));
        }

        navigateToPath(path);
      };

      var toggleRecursively = function () {
        $scope.data.search.recursively = !$scope.data.search.recursively;
        if ($scope.data.search.text) {
          searchDocuments($scope.data.search.text);
        } else if ($scope.data.search.query) {
          searchDocuments($scope.data.search.query);
        }
      };

      function clearSearchInternal() {
        $scope.data.search.text = null;
        $scope.data.search.active = false;
      }

      var clearSearch = function () {
        clearSearchInternal();
        getDocument($scope.data.document.path);
      };

      var searchKeyUp = function (event) {
        switch (event.keyCode) {
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
        FileBrowserSearchResource.search(urlParams,
          function onSuccess(response) {
            $log.info('Search result', response);
            var clone = $scope.data.document ? angular.copy($scope.data.document) : {};
            clone.children = response;
            $scope.data.document = clone;
          },
          function onError(response) {
            $log.debug(response);
          }
        );
      }

      var searchDocuments = function (query) {
        $scope.data.search.active = true;
        var recursively = $scope.data.search.recursively;
        var orderBy = null;
        var sortBy = null;
        var limit = 999;
        $scope.data.search.query = query;

        switch (query) {
          case 'RECENT':
            query = '';
            orderBy = 'desc';
            sortBy = 'lastModifiedDate';
            limit = 10;
            break;
        }

        var searchParams = {query: query, recursively: recursively, sort: sortBy, order: orderBy, limit: limit};
        searchDocumentsInternal($scope.data.document.path, searchParams);
      };

      var getTypeParts = function(document) {
        return FileBrowserService.isFile(document) && document.attachment.type ?
          document.attachment.type.split(/,|\s+/) :
          [];
      };

      var getLocalizedValue = function(values) {
        return FileBrowserService.getLocalizedValue(values, ngObibaMicaFileBrowserOptions.locale);
      };

      $scope.getDownloadUrl = FileBrowserDownloadService.getUrl;
      $scope.screen = $rootScope.screen;
      $scope.truncate = StringUtils.truncate;
      $scope.getDocumentIcon = FileBrowserService.getDocumentIcon;
      $scope.navigateToPath = navigateToPath;
      $scope.navigateTo = navigateTo;
      $scope.navigateBack = navigateBack;
      $scope.navigateToParent = navigateToParent;
      $scope.clearSearch = clearSearch;
      $scope.searchDocuments = searchDocuments;
      $scope.toggleRecursively = toggleRecursively;
      $scope.searchKeyUp = searchKeyUp;
      $scope.isFile = FileBrowserService.isFile;
      $scope.isRoot = FileBrowserService.isRoot;
      $scope.getLocalizedValue = getLocalizedValue;
      $scope.hideDetails = function() { $scope.data.details.show = false; };
      $scope.getTypeParts = getTypeParts;
      $scope.showDetails = function(document) {
        $scope.data.details.document = document;
        $scope.data.details.show = true;
      };

      $scope.pagination = {
        currentPage: 1,
        itemsPerPage: 20
      };

      $scope.data = {
        details: {
          document: null,
          show: false
        },
        docRootIcon: null,
        rootPath: null,
        document: null,
        accesses: [],
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

      $scope.$watchGroup(['docPath', 'docId'], function () {
        if ($scope.docPath && $scope.docId) {
          $scope.data.docRootIcon = BrowserBreadcrumbHelper.rootIcon($scope.docPath);
          $scope.data.rootPath = $scope.docPath + ($scope.docId !== 'null' ? '/' + $scope.docId : '');
          getDocument($scope.data.rootPath, null);
        }
      });

    }]);

