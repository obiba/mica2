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
              MicaConfigResource) {

      var onError = function(response) {
        AlertService.alert({
          id: 'FileSystemController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response)
        });
      };


      var getDocument = function(path) {

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

      var renameDocument = function(document, newName) {
        var newPath = document.path.replace(document.name, newName);
        DraftFileSystemFileResource.rename({path: document.path, name: newName},
          function onSuccess(){
            navigateToPath(newPath);
          },
          onError
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
        console.log('UPDATE', JSON.stringify(document.state.attachment));
        DraftFileSystemFilesResource.update(document.state.attachment,
          function onSuccess() {
            navigateTo(document);
          },
          onError
        );
      };

      var deleteDocument = function(document) {
        DraftFileSystemFileResource.delete({path: document.path},
          function onSuccess() {
            navigateTo($scope.data.document);
          },
          onError
        );
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
            DraftFileSystemFilesResource.update(attachment,
              function onSuccess() {
                navigateTo($scope.data.document);
              },
              onError
            );
          });
      };

      var searchDocuments = function() {
        DraftFileSystemSearchResource.search(
          {path: $scope.data.document.path, query: $scope.data.searchText, recursively: false},
          function onSuccess(response) {
            $log.info('Search result', response);
          },
          onError
        );
      };

      var isPublished = function() {
        return $scope.data.document && $scope.data.document.state.publicationDate;
      };

      var publish = function(value) {
        DraftFileSystemFileResource.publish(
          {path: $scope.data.document.path, publish: value ? 'true' : 'false'},
          function onSuccess(){
            navigateTo($scope.data.document);
          },
          onError
        );
      };

      var toStatus = function(value) {
        $log.info('STATUS', value);
        DraftFileSystemFileResource.changeStatus(
          {path: $scope.data.document.path, status: value},
          function onSuccess(){
            navigateTo($scope.data.document);
          },
          onError
        );
      };

      $scope.hasRole = $rootScope.hasRole;
      $scope.getDocumentTypeTitle = FileSystemService.getDocumentTypeTitle;
      $scope.getDocumentIcon = FileSystemService.getDocumentIcon;
      $scope.navigateToPath = navigateToPath;
      $scope.navigateTo = navigateTo;
      $scope.navigateBack = navigateBack;
      $scope.renameDocument = renameDocument;
      $scope.updateDocument = updateDocument;
      $scope.updateDocumentType = updateDocumentType;
      $scope.updateDocumentDescription = updateDocumentDescription;
      $scope.deleteDocument = deleteDocument;
      $scope.restoreRevision = restoreRevision;
      $scope.searchDocuments = searchDocuments;
      $scope.onFileSelect = onFileSelect;
      $scope.isFile = FileSystemService.isFile;
      $scope.isRoot = FileSystemService.isRoot;
      $scope.isPathEditable = FileSystemService.isPathEditable;
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
        searchText: null,
        parentPath: null,
        breadcrumbs: null,
        isFile: false,
        isRoot: false,
        editDescField: false
      };

      $scope.$watchGroup(['docPath', 'docId'], function(){
        if ($scope.docPath && $scope.docId) {
          var rootPath = $scope.docPath + '/' + $scope.docId;
          getDocument(rootPath, null);
        }
      });

    }]);
