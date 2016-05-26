/*!
 * ng-obiba-mica - v1.1.1
 * https://github.com/obiba/ng-obiba-mica

 * License: GNU Public License version 3
 * Date: 2016-05-26
 */
'use strict';

function NgObibaMicaUrlProvider() {
  var registry = {
    'DataAccessFormConfigResource': 'ws/config/data-access-form',
    'DataAccessRequestsResource': 'ws/data-access-requests',
    'DataAccessRequestResource': 'ws/data-access-request/:id',
    'DataAccessRequestAttachmentDownloadResource': '/ws/data-access-request/:id/attachments/:attachmentId/_download',
    'DataAccessRequestDownloadPdfResource': '/ws/data-access-request/:id/_pdf',
    'DataAccessRequestCommentsResource': 'ws/data-access-request/:id/comments',
    'DataAccessRequestCommentResource': 'ws/data-access-request/:id/comment/:commentId',
    'DataAccessRequestStatusResource': 'ws/data-access-request/:id/_status?to=:status',
    'TempFileUploadResource': 'ws/files/temp',
    'TempFileResource': 'ws/files/temp/:id',
    'TaxonomiesSearchResource': 'ws/taxonomies/_search',
    'TaxonomiesResource': 'ws/taxonomies/_filter',
    'TaxonomyResource': 'ws/taxonomy/:taxonomy/_filter',
    'VocabularyResource': 'ws/taxonomy/:taxonomy/vocabulary/:vocabulary/_filter',
    'JoinQuerySearchResource': 'ws/:type/_rql?query=:query',
    'JoinQueryCoverageResource': 'ws/variables/_coverage?query=:query',
    'JoinQueryCoverageDownloadResource': 'ws/variables/_coverage_download?query=:query',
    'VariablePage': '',
    'NetworkPage': '#/network/:network',
    'StudyPage': '#/study/:study',
    'StudyPopulationsPage': '#/study/:study',
    'DatasetPage': '#/:type/:dataset',
    'BaseUrl': '/',
    'FileBrowserFileResource': 'ws/file/:path/',
    'FileBrowserSearchResource': 'ws/files-search/:path',
    'FileBrowserDownloadUrl': 'ws/draft/file-dl/:path?inline=:inline',
    'GraphicsSearchRootUrl': '#/search'
  };

  function UrlProvider(registry) {
    var urlRegistry = registry;

    this.getUrl = function (resource) {
      if (resource in urlRegistry) {
        return urlRegistry[resource];
      }

      return null;
    };
  }

  this.setUrl = function (key, url) {
    if (key in registry) {
      registry[key] = url;
    }
  };

  this.$get = function () {
    return new UrlProvider(registry);
  };
}

/* exported NgObibaMicaTemplateUrlFactory */
function NgObibaMicaTemplateUrlFactory() {
  var factory = {registry: null};

  function TemplateUrlProvider(registry) {
    var urlRegistry = registry;

    this.getHeaderUrl = function (key) {
      if (key in urlRegistry) {
        return urlRegistry[key].header;
      }

      return null;
    };

    this.getFooterUrl = function (key) {
      if (key in urlRegistry) {
        return urlRegistry[key].footer;
      }

      return null;
    };
  }

  factory.setHeaderUrl = function (key, url) {
    if (key in this.registry) {
      this.registry[key].header = url;
    }
  };

  factory.setFooterUrl = function (key, url) {
    if (key in this.registry) {
      this.registry[key].footer = url;
    }
  };

  factory.$get = function () {
    return new TemplateUrlProvider(this.registry);
  };

  this.create = function (inputRegistry) {
    factory.registry = inputRegistry;
    return factory;
  };
}

angular.module('ngObibaMica', [
    'schemaForm',
    'ngCookies',
    'obiba.mica.utils',
    'obiba.mica.file',
    'obiba.mica.attachment',
    'obiba.mica.access',
    'obiba.mica.search',
    'obiba.mica.graphics',
    'obiba.mica.localized',
    'obiba.mica.fileBrowser',
    'angularUtils.directives.dirPagination',
  ])
  .constant('USER_ROLES', {
    all: '*',
    admin: 'mica-administrator',
    reviewer: 'mica-reviewer',
    editor: 'mica-editor',
    user: 'mica-user',
    dao: 'mica-data-access-officer'
  })
  .config(['$provide', 'paginationTemplateProvider', function ($provide, paginationTemplateProvider) {
    $provide.provider('ngObibaMicaUrl', NgObibaMicaUrlProvider);
    paginationTemplateProvider.setPath('views/pagination-template.html');
  }]);

;'use strict';

angular.module('obiba.mica.utils', [])

  .factory('UserProfileService',
    function () {

      var getAttributeValue = function(attributes, key) {
        var result = attributes.filter(function (attribute) {
          return attribute.key === key;
        });

        return result && result.length > 0 ? result[0].value : null;
      };

      return {

        'getAttribute': function (attributes, key) {
          return getAttributeValue(attributes, key);
        },

        'getFullName': function (profile) {
          if (profile) {
            if (profile.attributes) {
              return getAttributeValue(profile.attributes, 'firstName') + ' ' + getAttributeValue(profile.attributes, 'lastName');
            }
            return profile.username;
          }
          return null;
        }
      };
    })

  .service('GraphicChartsConfigurations', function(){

    this.getClientConfig = function(){
      return true;
    };

    this.setClientConfig = function(){
      return true;
    };
  })

  .directive('fixedHeader', ['$timeout','$window', function ($timeout, $window) {
    return {
      restrict: 'A',
      scope: {
        tableMaxHeight: '@',
        trigger: '=fixedHeader'
      },
      link: function ($scope, $elem) {
        var elem = $elem[0];

        function isVisible(el) {
          var style = $window.getComputedStyle(el);
          return (style.display !== 'none' && el.offsetWidth !==0 );
        }

        function isTableReady() {
          return isVisible(elem.querySelector('tbody')) && elem.querySelector('tbody tr:first-child') !== null;
        }

        $scope.redraw = false;

        // wait for content to load into table and to have at least one row, tdElems could be empty at the time of execution if td are created asynchronously (eg ng-repeat with promise)
        function redrawTable() {
          if ($scope.redraw) {
            return;
          }
          // reset display styles so column widths are correct when measured below
          angular.element(elem.querySelectorAll('thead, tbody, tfoot')).css('display', '');

          // wrap in $timeout to give table a chance to finish rendering
          $timeout(function () {
            $scope.redraw = true;
            console.log('do redrawTable');
            // set widths of columns
            var totalColumnWidth = 0;
            angular.forEach(elem.querySelectorAll('tr:first-child th'), function (thElem, i) {

              var tdElems = elem.querySelector('tbody tr:first-child td:nth-child(' + (i + 1) + ')');
              var tfElems = elem.querySelector('tfoot tr:first-child td:nth-child(' + (i + 1) + ')');
              var columnWidth = tdElems ? tdElems.offsetWidth : thElem.offsetWidth;

              if(tdElems) {
                tdElems.style.width = columnWidth + 'px';
              }
              if(thElem) {
                thElem.style.width = columnWidth + 'px';
              }
              if (tfElems) {
                tfElems.style.width = columnWidth + 'px';
              }
              totalColumnWidth = totalColumnWidth + columnWidth;
            });

            // set css styles on thead and tbody
            angular.element(elem.querySelectorAll('thead, tfoot')).css('display', 'block');

            angular.element(elem.querySelectorAll('tbody')).css({
              'display': 'block',
              'max-height': $scope.tableMaxHeight || 'inherit',
              'overflow': 'auto'
            });

            // add missing width to fill the table
            if (totalColumnWidth < elem.offsetWidth) {
              var last = elem.querySelector('tbody tr:first-child td:last-child');
              last.style.width = (last.offsetWidth + elem.offsetWidth - totalColumnWidth) + 'px';
              last = elem.querySelector('thead tr:first-child th:last-child');
              last.style.width = (last.offsetWidth + elem.offsetWidth - totalColumnWidth) + 'px';
            }

            // reduce width of last column by width of scrollbar
            var tbody = elem.querySelector('tbody');
            var scrollBarWidth = tbody.offsetWidth - tbody.clientWidth;
            if (scrollBarWidth > 0) {
              var lastColumn = elem.querySelector('tbody tr:first-child td:last-child');
              lastColumn.style.width = (parseInt(lastColumn.style.width.replace('px','')) - scrollBarWidth) + 'px';
            }
            $scope.redraw = false;
          });
        }

        // watch table content change
        $scope.$watchGroup(['trigger', isTableReady],
          function (newValue) {
            if (newValue[1] === true) {
               redrawTable();
            }
          }
        );

        // watch table resize
        $scope.$watch(function() {
          return elem.offsetWidth;
        }, function() {
          redrawTable();
        });
      }
    };
  }]);

;'use strict';

angular.module('obiba.mica.file', ['ngResource']);
;'use strict';

angular.module('obiba.mica.file')
  .filter('bytes', function () {
    return function (bytes) {
      return bytes === null || typeof bytes === 'undefined' ? '' : filesize(bytes);
    };
  });

;'use strict';

angular.module('obiba.mica.file')
  .factory('TempFileResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('TempFileResource'), {}, {
        'get': {method: 'GET'},
        'delete': {method: 'DELETE'}
      });
    }])
;
;'use strict';

angular.module('obiba.mica.attachment', [
  'obiba.mica.file',
  'ui',
  'ui.bootstrap',
  'ngFileUpload',
  'templates-ngObibaMica'
]);
;'use strict';

angular.module('obiba.mica.attachment')
  .directive('attachmentList', [function() {
    return {
      restrict: 'E',
      scope: {
        hrefBuilder: '&',
        files: '='
      },
      templateUrl: 'attachment/attachment-list-template.html',
      link: function(scope) {
        scope.attachments = [];
        scope.hrefBuilder = scope.hrefBuilder || function(a) { return a.id; };

        scope.$watch('files', function(val) {
          if (val) {
            scope.attachments = val.map(function (a) {
              var temp = angular.copy(a);
              temp.href = scope.hrefBuilder({id: a.id});
              return temp;
            });
          }
        }, true);
      }
    };
  }])
  .directive('attachmentInput', [function () {
    return {
      restrict: 'E',
      require: '^form',
      scope: {
        multiple: '=',
        accept: '@',
        files: '='
      },
      templateUrl: 'attachment/attachment-input-template.html',
      controller: 'AttachmentCtrl'
    };
  }])
  .controller('AttachmentCtrl', ['$scope', '$timeout', '$log', 'Upload', 'TempFileResource', 'ngObibaMicaUrl',
    function ($scope, $timeout, $log, Upload, TempFileResource, ngObibaMicaUrl) {
      var uploadFile = function (file) {
        $log.debug('file', file);

        var attachment = {
          showProgressBar: true,
          lang: 'en',
          progress: 0,
          fileName: file.name,
          size: file.size
        };

        if ($scope.multiple) {
          $scope.files.push(attachment);
        } else {
          $scope.files.splice(0, $scope.files.length);
          $scope.files.push(attachment);
        }

        $scope.upload = Upload
          .upload({
            url: ngObibaMicaUrl.getUrl('TempFileUploadResource'),
            method: 'POST',
            file: file
          })
          .progress(function (evt) {
            attachment.progress = parseInt(100.0 * evt.loaded / evt.total);
          })
          .success(function (data, status, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            var fileId = parts[parts.length - 1];
            TempFileResource.get(
              {id: fileId},
              function (tempFile) {
                $log.debug('tempFile', tempFile);
                attachment.id = tempFile.id;
                attachment.md5 = tempFile.md5;
                attachment.justUploaded = true;
                // wait for 1 second before hiding progress bar
                $timeout(function () { attachment.showProgressBar = false; }, 1000);
              }
            );
          });
      };

      $scope.onFileSelect = function (file) {
        $scope.uploadedFiles = file;
        $scope.uploadedFiles.forEach(function (f) {
          uploadFile(f);
        });
      };

      $scope.deleteTempFile = function (tempFileId) {
        TempFileResource.delete(
          {id: tempFileId},
          function () {
            for (var i = $scope.files.length; i--;) {
              var attachment = $scope.files[i];
              if (attachment.justUploaded && attachment.id === tempFileId) {
                $scope.files.splice(i, 1);
              }
            }
          }
        );
      };

      $scope.deleteFile = function (fileId) {
        for (var i = $scope.files.length; i--;) {
          if ($scope.files[i].id === fileId) {
            $scope.files.splice(i, 1);
          }
        }
      };
    }
  ]);
;/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/*global NgObibaMicaTemplateUrlFactory */
angular.module('obiba.mica.access', [
    'pascalprecht.translate',
    'obiba.alert',
    'obiba.comments',
    'obiba.mica.attachment',
    'obiba.utils',
    'angularMoment',
    'templates-ngObibaMica'
  ])
  .config(['$provide', function ($provide) {
    $provide.provider('ngObibaMicaAccessTemplateUrl', new NgObibaMicaTemplateUrlFactory().create(
      {
        list: {header: null, footer: null},
        view: {header: null, footer: null},
        form: {header: null, footer: null}
      }
    ));
  }]);
;/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.access')
  .controller('DataAccessRequestListController', ['$rootScope',
    '$scope',
    '$uibModal',
    'DataAccessRequestsResource',
    'DataAccessRequestResource',
    'DataAccessRequestService',
    'NOTIFICATION_EVENTS',
    'SessionProxy',
    'USER_ROLES',
    'ngObibaMicaAccessTemplateUrl',
    'DataAccessRequestConfig',

    function ($rootScope,
              $scope,
              $uibModal,
              DataAccessRequestsResource,
              DataAccessRequestResource,
              DataAccessRequestService,
              NOTIFICATION_EVENTS,
              SessionProxy,
              USER_ROLES,
              ngObibaMicaAccessTemplateUrl,
              DataAccessRequestConfig) {

      var onSuccess = function(reqs) {
        for (var i = 0; i < reqs.length; i++) {
          var req = reqs[i];
          if (req.status !== 'OPENED') {
            for (var j = 0; j < req.statusChangeHistory.length; j++) {
              var change = req.statusChangeHistory[j];
              if (change.from === 'OPENED' && change.to === 'SUBMITTED') {
                req.submissionDate = change.changedOn;
              }
            }
          }
        }
        $scope.requests = reqs;
        $scope.loading = false;
      };

      var onError = function() {
        $scope.loading = false;
      };

      DataAccessRequestService.getStatusFilterData(function(translated) {
        $scope.REQUEST_STATUS  = translated;
      });


      $scope.headerTemplateUrl = ngObibaMicaAccessTemplateUrl.getHeaderUrl('list');
      $scope.footerTemplateUrl = ngObibaMicaAccessTemplateUrl.getFooterUrl('list');
      $scope.config = DataAccessRequestConfig.getOptions();
      $scope.searchStatus = {};
      $scope.loading = true;
      DataAccessRequestsResource.query({}, onSuccess, onError);
      $scope.actions = DataAccessRequestService.actions;
      $scope.showApplicant = SessionProxy.roles().filter(function(role) {
        return [USER_ROLES.dao, USER_ROLES.admin].indexOf(role) > -1;
      }).length > 0;

      $scope.deleteRequest = function (request) {
        $scope.requestToDelete = request.id;
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'data-access-request.delete-dialog.title',
            messageKey:'data-access-request.delete-dialog.message',
            messageArgs: [request.title, request.applicant]
          }, request.id
        );
      };

      $scope.userProfile = function (profile) {
        $scope.applicant = profile;
        $uibModal.open({
          scope: $scope,
          templateUrl: 'access/views/data-access-request-profile-user-modal.html'
        });
      };

      var getAttributeValue = function(attributes, key) {
        var result = attributes.filter(function (attribute) {
          return attribute.key === key;
        });

        return result && result.length > 0 ? result[0].value : null;
      };

      $scope.getFullName = function (profile) {
        if (profile) {
          if (profile.attributes) {
            return getAttributeValue(profile.attributes, 'firstName') + ' ' + getAttributeValue(profile.attributes, 'lastName');
          }
          return profile.username;
        }
        return null;
      };

      $scope.getProfileEmail = function (profile) {
        if (profile) {
          if (profile.attributes) {
            return getAttributeValue(profile.attributes, 'email');
          }
        }
        return null;
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
        if ($scope.requestToDelete === id) {
          DataAccessRequestResource.delete({id: $scope.requestToDelete},
            function () {
              $scope.loading = true;
              DataAccessRequestsResource.query({}, onSuccess, onError);
            });

          delete $scope.requestToDelete;
        }
      });
    }])

  .controller('DataAccessRequestViewController',
    ['$rootScope',
      '$scope',
      '$location',
      '$uibModal',
      '$routeParams',
      '$filter',
      'DataAccessRequestResource',
      'DataAccessRequestService',
      'DataAccessRequestStatusResource',
      'DataAccessFormConfigResource',
      'JsonUtils',
      'DataAccessRequestCommentsResource',
      'DataAccessRequestCommentResource',
      'ngObibaMicaUrl',
      'ngObibaMicaAccessTemplateUrl',
      'AlertService',
      'ServerErrorUtils',
      'NOTIFICATION_EVENTS',
      'DataAccessRequestConfig',

    function ($rootScope,
              $scope,
              $location,
              $uibModal,
              $routeParams,
              $filter,
              DataAccessRequestResource,
              DataAccessRequestService,
              DataAccessRequestStatusResource,
              DataAccessFormConfigResource,
              JsonUtils,
              DataAccessRequestCommentsResource,
              DataAccessRequestCommentResource,
              ngObibaMicaUrl,
              ngObibaMicaAccessTemplateUrl,
              AlertService,
              ServerErrorUtils,
              NOTIFICATION_EVENTS,
              DataAccessRequestConfig) {

      var onError = function (response) {
        AlertService.alert({
          id: 'DataAccessRequestViewController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response)
        });
      };

      var retrieveComments = function() {
        $scope.form.comments = DataAccessRequestCommentsResource.query({id: $routeParams.id});
      };

      var selectTab = function(id) {
        $scope.selectedTab = id;
        switch (id) {
          case 'form':
            break;
          case 'comments':
            retrieveComments();
            break;
        }
      };

      var submitComment = function(comment) {
        DataAccessRequestCommentsResource.save({id: $routeParams.id}, comment.message, retrieveComments, onError);
      };

      var updateComment = function(comment) {
        DataAccessRequestCommentResource.update({id: $routeParams.id, commentId: comment.id}, comment.message, retrieveComments, onError);
      };

      var deleteComment = function(comment) {
        $scope.commentToDelete = comment.id;

        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'comment.delete-dialog.title',
            messageKey:'comment.delete-dialog.message',
            messageArgs: [comment.createdBy]
          }, comment.id
        );
      };

      $scope.form = {
        schema: null,
        definition: null,
        model: {},
        comments: null
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
        if ($scope.commentToDelete === id) {
           DataAccessRequestCommentResource.delete({id: $routeParams.id, commentId: id}, {}, retrieveComments, onError);
        }
      });

      $scope.getDownloadHref = function(attachments, id) {
        return ngObibaMicaUrl.getUrl('DataAccessRequestAttachmentDownloadResource')
          .replace(':id', $scope.dataAccessRequest.id).replace(':attachmentId', id);
      };

      $scope.config = DataAccessRequestConfig.getOptions();
      $scope.actions = DataAccessRequestService.actions;
      $scope.nextStatus = DataAccessRequestService.nextStatus;
      $scope.selectTab = selectTab;
      $scope.submitComment = submitComment;
      $scope.updateComment = updateComment;
      $scope.deleteComment = deleteComment;
      $scope.headerTemplateUrl = ngObibaMicaAccessTemplateUrl.getHeaderUrl('view');
      $scope.footerTemplateUrl = ngObibaMicaAccessTemplateUrl.getFooterUrl('view');
      $scope.getStatusHistoryInfoId = DataAccessRequestService.getStatusHistoryInfoId;
      DataAccessRequestService.getStatusHistoryInfo(function(statusHistoryInfo) {
        $scope.getStatusHistoryInfo = statusHistoryInfo;
      });

      $scope.validForm = true;

      var getRequest = function () {
        return DataAccessRequestResource.get({id: $routeParams.id}, function onSuccess(request) {
          try {
            $scope.form.model = request.content ? JSON.parse(request.content) : {};
            $scope.requestDownloadUrl =
              ngObibaMicaUrl.getUrl('DataAccessRequestDownloadPdfResource').replace(':id', $scope.dataAccessRequest.id);
          } catch (e) {
            $scope.validForm = false;
            $scope.form.model = {};
            AlertService.alert({
              id: 'DataAccessRequestViewController',
              type: 'danger',
              msgKey: 'data-access-request.parse-error'
            });
          }

          // Retrieve form data
          DataAccessFormConfigResource.get(
            function onSuccess(dataAccessForm) {
              $scope.form.definition = JsonUtils.parseJsonSafely(dataAccessForm.definition, []);
              $scope.form.schema = JsonUtils.parseJsonSafely(dataAccessForm.schema, {});

              if ($scope.form.definition.length === 0) {
                $scope.validForm = false;
                $scope.form.definition = [];
                AlertService.alert({
                  id: 'DataAccessRequestViewController',
                  type: 'danger',
                  msgKey: 'data-access-config.parse-error.definition'
                });
              }
              if (Object.getOwnPropertyNames($scope.form.schema).length === 0) {
                $scope.validForm = false;
                $scope.form.schema = {readonly: true};
                AlertService.alert({
                  id: 'DataAccessRequestViewController',
                  type: 'danger',
                  msgKey: 'data-access-config.parse-error.schema'
                });
              }
              $scope.form.schema.readonly = true;
              $scope.$broadcast('schemaFormRedraw');
            },
            onError
          );

          request.attachments = request.attachments || [];

          return request;
        });
      };

      $scope.dataAccessRequest = $routeParams.id ? getRequest() : {};

      $scope.delete = function () {
        $scope.requestToDelete = $scope.dataAccessRequest.id;
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'data-access-request.delete-dialog.title',
            messageKey:'data-access-request.delete-dialog.message',
            messageArgs: [$scope.dataAccessRequest.title, $scope.dataAccessRequest.applicant]
          }, $scope.requestToDelete
        );
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
        if ($scope.requestToDelete === id) {
          DataAccessRequestResource.delete({id: $scope.requestToDelete},
            function () {
              $location.path('/data-access-requests').replace();
            });

          delete $scope.requestToDelete;
        }
      });

      var onUpdatStatusSuccess = function () {
        $scope.dataAccessRequest = getRequest();
      };

      var confirmStatusChange = function(status, messageKey, statusName) {
        $rootScope.$broadcast(
          NOTIFICATION_EVENTS.showConfirmDialog,
          {
            titleKey: 'data-access-request.status-change-confirmation.title',
            messageKey: messageKey !== null ? messageKey : 'data-access-request.status-change-confirmation.message',
            messageArgs: statusName !== null ? [$filter('translate')(statusName).toLowerCase()] : []
          }, status);
      };

      var statusChangedConfirmed = function(status, expectedStatus) {
        if (status === expectedStatus) {
          DataAccessRequestStatusResource.update({
            id: $scope.dataAccessRequest.id,
            status: status
          }, onUpdatStatusSuccess, onError);
        }
      };

      $scope.submit = function () {
        $scope.$broadcast('schemaFormValidate');
        if ($scope.forms.requestForm.$valid) {
          DataAccessRequestStatusResource.update({
            id: $scope.dataAccessRequest.id,
            status: DataAccessRequestService.status.SUBMITTED
          }, function onSubmitted() {
            onUpdatStatusSuccess();
            $uibModal.open({
              scope: $scope,
              templateUrl:'access/views/data-access-request-submitted-modal.html'
            });
          }, onError);
        } else {
          AlertService.alert({
            id: 'DataAccessRequestViewController',
            type: 'danger',
            msgKey: 'data-access-request.submit.invalid'
          });
        }
      };

      $scope.reopen = function () {
        confirmStatusChange(DataAccessRequestService.status.OPENED, null, 'reopen');
      };
      $scope.review = function () {
        confirmStatusChange(DataAccessRequestService.status.REVIEWED, 'data-access-request.status-change-confirmation.message-review', null);
      };
      $scope.approve = function () {
        confirmStatusChange(DataAccessRequestService.status.APPROVED, null, 'approve');
      };
      $scope.reject = function () {
        confirmStatusChange(DataAccessRequestService.status.REJECTED, null, 'reject');
      };

      $scope.userProfile = function (profile) {
        $scope.applicant = profile;
        $uibModal.open({
          scope: $scope,
          templateUrl: 'access/views/data-access-request-profile-user-modal.html'
        });
      };

      var getAttributeValue = function(attributes, key) {
        var result = attributes.filter(function (attribute) {
          return attribute.key === key;
        });

        return result && result.length > 0 ? result[0].value : null;
      };

      $scope.getFullName = function (profile) {
        if (profile) {
          if (profile.attributes) {
            return getAttributeValue(profile.attributes, 'firstName') + ' ' + getAttributeValue(profile.attributes, 'lastName');
          }
          return profile.username;
        }
        return null;
      };

      $scope.getProfileEmail = function (profile) {
        if (profile) {
          if (profile.attributes) {
            return getAttributeValue(profile.attributes, 'email');
          }
        }
        return null;
      };

      $scope.$on(
        NOTIFICATION_EVENTS.confirmDialogAccepted,
        function(event, status) {
          statusChangedConfirmed(DataAccessRequestService.status.OPENED, status);
        }
      );
      $scope.$on(
        NOTIFICATION_EVENTS.confirmDialogAccepted,
        function(event, status) {
          statusChangedConfirmed(DataAccessRequestService.status.REVIEWED, status);
        }
      );
      $scope.$on(
        NOTIFICATION_EVENTS.confirmDialogAccepted,
        function(event, status) {
          statusChangedConfirmed(DataAccessRequestService.status.APPROVED, status);
        }
      );
      $scope.$on(
        NOTIFICATION_EVENTS.confirmDialogAccepted,
        function(event, status) {
          statusChangedConfirmed(DataAccessRequestService.status.REJECTED, status);
        }
      );

      $scope.forms = {};
    }])

  .controller('DataAccessRequestEditController', ['$log',
    '$scope',
    '$routeParams',
    '$location',
    '$uibModal',
    'DataAccessRequestsResource',
    'DataAccessRequestResource',
    'DataAccessFormConfigResource',
    'JsonUtils',
    'AlertService',
    'ServerErrorUtils',
    'SessionProxy',
    'DataAccessRequestService',
    'ngObibaMicaAccessTemplateUrl',
    'DataAccessRequestConfig',

    function ($log, $scope, $routeParams, $location, $uibModal,
              DataAccessRequestsResource,
              DataAccessRequestResource,
              DataAccessFormConfigResource,
              JsonUtils,
              AlertService,
              ServerErrorUtils,
              SessionProxy,
              DataAccessRequestService,
              ngObibaMicaAccessTemplateUrl,
              DataAccessRequestConfig) {

      var onSuccess = function(response, getResponseHeaders) {
        var parts = getResponseHeaders().location.split('/');
        $location.path('/data-access-request/' + parts[parts.length - 1]).replace();
      };

      var onError = function(response) {
        AlertService.alert({
          id: 'DataAccessRequestEditController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response)
        });
      };

      var validate = function() {
        $scope.$broadcast('schemaFormValidate');

        $uibModal.open({
          scope: $scope,
          templateUrl: 'access/views/data-access-request-validation-modal.html'
        });
      };

      var cancel = function() {
        $location.path('/data-access-request' + ($routeParams.id ? '/' + $routeParams.id : 's')).replace();
      };

      var save = function() {
        $scope.dataAccessRequest.content = JSON.stringify($scope.form.model);

        if ($scope.newRequest) {
          DataAccessRequestsResource.save($scope.dataAccessRequest, onSuccess, onError);
        } else {
          DataAccessRequestResource.save($scope.dataAccessRequest, function() {
            $location.path('/data-access-request' + ($scope.dataAccessRequest.id ? '/' + $scope.dataAccessRequest.id : 's')).replace();
          }, onError);
        }
      };

      // Retrieve form data
      DataAccessFormConfigResource.get(
        function onSuccess(dataAccessForm) {
          $scope.form.definition = JsonUtils.parseJsonSafely(dataAccessForm.definition, []);
          $scope.form.schema = JsonUtils.parseJsonSafely(dataAccessForm.schema, {});
          if ($scope.form.definition.length === 0) {
            $scope.form.definition = [];
            $scope.validForm = false;
            AlertService.alert({
              id: 'DataAccessRequestEditController',
              type: 'danger',
              msgKey: 'data-access-config.parse-error.definition'
            });
          }
          if (Object.getOwnPropertyNames($scope.form.schema).length === 0) {
            $scope.form.schema = {};
            $scope.validForm = false;
            AlertService.alert({
              id: 'DataAccessRequestEditController',
              type: 'danger',
              msgKey: 'data-access-config.parse-error.schema'
            });
          }

          if ($scope.validForm) {
            $scope.dataAccessRequest = $routeParams.id ?
              DataAccessRequestResource.get({id: $routeParams.id}, function onSuccess(request) {
                try {
                  $scope.form.model = request.content ? JSON.parse(request.content) : {};
                } catch (e) {
                  $scope.form.model = {};
                  AlertService.alert({
                    id: 'DataAccessRequestEditController',
                    type: 'danger',
                    msgKey: 'data-access-request.parse-error'
                  });
                }

                $scope.canEdit = DataAccessRequestService.actions.canEdit(request);
                $scope.form.schema.readonly = !$scope.canEdit;
                $scope.$broadcast('schemaFormRedraw');

                request.attachments = request.attachments || [];
                return request;
              }) : {
              applicant: SessionProxy.login(),
              status: DataAccessRequestService.status.OPENED,
              attachments: []
            };
          }
        },
        onError
        );

      $scope.config = DataAccessRequestConfig.getOptions();
      $scope.validForm = true;
      $scope.requestId = $routeParams.id;
      $scope.newRequest = $routeParams.id ? false : true;
      $scope.cancel = cancel;
      $scope.save = save;
      $scope.editable = true;
      $scope.validate = validate;
      $scope.headerTemplateUrl = ngObibaMicaAccessTemplateUrl.getHeaderUrl('form');
      $scope.footerTemplateUrl = ngObibaMicaAccessTemplateUrl.getFooterUrl('form');
      $scope.form = {
        schema: null,
        definition: null,
        model: {}
      };

    }]);
;/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.access')
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/data-access-requests', {
          templateUrl: 'access/views/data-access-request-list.html',
          controller: 'DataAccessRequestListController'
        })
        .when('/data-access-request/new', {
          templateUrl: 'access/views/data-access-request-form.html',
          controller: 'DataAccessRequestEditController'
        })
        .when('/data-access-request/:id/edit', {
          templateUrl: 'access/views/data-access-request-form.html',
          controller: 'DataAccessRequestEditController'
        })
        .when('/data-access-request/:id', {
          templateUrl: 'access/views/data-access-request-view.html',
          controller: 'DataAccessRequestViewController'
        });
    }]);
;/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.access')

  .factory('DataAccessFormConfigResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessFormConfigResource'), {}, {
        'get': {method: 'GET', errorHandler: true}
      });
    }])

  .factory('DataAccessRequestsResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestsResource'), {}, {
        'save': {method: 'POST', errorHandler: true},
        'get': {method: 'GET'}
      });
    }])

  .factory('DataAccessRequestResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestResource'), {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET'},
        'delete': {method: 'DELETE'}
      });
    }])

  .factory('DataAccessRequestCommentsResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestCommentsResource'), {}, {
        'save': {
          method: 'POST',
          params: {id: '@id'},
          headers: {'Content-Type': 'text/plain'},
          errorHandler: true
        },
        'get': {method: 'GET', params: {id: '@id'}, errorHandler: true}
      });
    }])

  .factory('DataAccessRequestCommentResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestCommentResource'), {}, {
        'delete': {
          method: 'DELETE',
          params: {id: '@id', commentId: '@commentId'},
          errorHandler: true
        },
        'update': {
          method: 'PUT',
          params: {id: '@id', commentId: '@commentId'},
          headers: {'Content-Type': 'text/plain'},
          errorHandler: true
        }
      });
    }])

  .factory('DataAccessRequestStatusResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestStatusResource'), {}, {
        'update': {
          method: 'PUT',
          params: {id: '@id', status: '@status'},
          errorHandler: true
        }
      });
    }])

  .service('DataAccessRequestConfig', function () {
    var options = {
      newRequestButtonCaption: null,
      documentsSectionTitle: null,
      documentsSectionHelpText: null,
      downloadButtonCaption: null,
      commentsEnabled: true,
      userListPageTitle: null
    };

    this.setOptions = function (newOptions) {
      if (typeof(newOptions) === 'object') {
        Object.keys(newOptions).forEach(function (option) {
          if (option in options) {
            options[option] = newOptions[option];
          }
        });
      }
    };

    this.getOptions = function () {
      return angular.copy(options);
    };

  })

  .service('DataAccessRequestService', ['$translate', 'SessionProxy', 'USER_ROLES',
    function ($translate, SessionProxy, USER_ROLES) {
      var statusList = {
        OPENED: 'OPENED',
        SUBMITTED: 'SUBMITTED',
        REVIEWED: 'REVIEWED',
        APPROVED: 'APPROVED',
        REJECTED: 'REJECTED'
      };

      this.status = statusList;

      this.getStatusFilterData = function (userCallback) {
        if (userCallback) {
          $translate(Object.keys(statusList)).then(function (translation) {
            userCallback(Object.keys(translation).map(function (key) {
              return translation[key];
            }));
          });
        }
      };

      var canDoAction = function (request, action) {
        return request.actions ? request.actions.indexOf(action) !== - 1 : null;
      };

      this.actions = {
        canViewProfile: function (role) {
          var found = false;
          var currentUserRoles = SessionProxy.roles();
          angular.forEach(currentUserRoles, function (value) {
            if (value === role || value === USER_ROLES.admin) {
              found = true;
            }
          });
          return found;
        },
        canView: function (request) {
          return canDoAction(request, 'VIEW');
        },

        canEdit: function (request) {
          return canDoAction(request, 'EDIT');
        },

        canEditStatus: function (request) {
          return canDoAction(request, 'EDIT_STATUS');
        },

        canDelete: function (request) {
          return canDoAction(request, 'DELETE');
        }
      };

      var canChangeStatus = function (request, to) {
        return request.nextStatus ? request.nextStatus.indexOf(to) !== - 1 : null;
      };

      this.nextStatus = {
        canSubmit: function (request) {
          return canChangeStatus(request, 'SUBMITTED');
        },

        canReopen: function (request) {
          return canChangeStatus(request, 'OPENED');
        },

        canReview: function (request) {
          return canChangeStatus(request, 'REVIEWED');
        },

        canApprove: function (request) {
          return canChangeStatus(request, 'APPROVED');
        },

        canReject: function (request) {
          return canChangeStatus(request, 'REJECTED');
        }

      };

      this.getStatusHistoryInfo = function (userCallback) {
        if (! userCallback) {
          return;
        }

        var keyIdMap = {
          'data-access-request.histories.opened': 'opened',
          'data-access-request.histories.reopened': 'reopened',
          'data-access-request.histories.submitted': 'submitted',
          'data-access-request.histories.reviewed': 'reviewed',
          'data-access-request.histories.approved': 'approved',
          'data-access-request.histories.rejected': 'rejected'
        };

        var statusHistoryInfo = {
          opened: {
            id: 'opened',
            icon: 'glyphicon glyphicon-saved',
          },
          reopened: {
            id: 'reopened',
            icon: 'glyphicon glyphicon-repeat',
          },
          submitted: {
            id: 'submitted',
            icon: 'glyphicon glyphicon-export',
          },
          reviewed: {
            id: 'reviewed',
            icon: 'glyphicon glyphicon-check',
          },
          approved: {
            id: 'approved',
            icon: 'glyphicon glyphicon-ok',
          },
          rejected: {
            id: 'rejected',
            icon: 'glyphicon glyphicon-remove',
          }
        };

        $translate(Object.keys(keyIdMap))
          .then(
            function (translation) {
              Object.keys(translation).forEach(
                function (key) {
                  statusHistoryInfo[keyIdMap[key]].msg = translation[key];
                });

              userCallback(statusHistoryInfo);
            });
      };

      this.getStatusHistoryInfoId = function (status) {
        var id = 'opened';

        if (status.from !== 'OPENED' || status.from !== status.to) {
          switch (status.to) {
            case 'OPENED':
              id = 'reopened';
              break;
            case 'SUBMITTED':
              id = 'submitted';
              break;
            case 'REVIEWED':
              id = 'reviewed';
              break;
            case 'APPROVED':
              id = 'approved';
              break;
            case 'REJECTED':
              id = 'rejected';
              break;
          }
        }

        return id;
      };

      return this;
    }])
  .filter('filterProfileAttributes', function () {
    return function (attributes) {
      var exclude = ['email', 'firstName', 'lastName', 'createdDate', 'lastLogin', 'username'];
      return attributes.filter(function (attribute) {
        return exclude.indexOf(attribute.key) === - 1;
      });
    };
  });
;/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* global QUERY_TARGETS */

'use strict';

/* exported DISPLAY_TYPES */
var DISPLAY_TYPES = {
  LIST: 'list',
  COVERAGE: 'coverage',
  GRAPHICS: 'graphics'
};

/*global NgObibaMicaTemplateUrlFactory */
angular.module('obiba.mica.search', [
    'obiba.alert',
    'ui.bootstrap',
    'pascalprecht.translate',
    'templates-ngObibaMica'
  ])
  .config(['$provide', function ($provide) {
    $provide.provider('ngObibaMicaSearchTemplateUrl', new NgObibaMicaTemplateUrlFactory().create(
      {
        search: {header: null, footer: null},
        classifications: {header: null, footer: null}
      }
    ));
  }])
  .config(['$provide', '$injector', function ($provide) {
    $provide.provider('ngObibaMicaSearch', function () {
      var localeResolver = ['LocalizedValues', function (LocalizedValues) {
        return LocalizedValues.getLocal();
      }], options = {
        targetTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
        searchTabsOrder: [DISPLAY_TYPES.LIST, DISPLAY_TYPES.COVERAGE, DISPLAY_TYPES.GRAPHICS],
        resultTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
        showAllFacetedTaxonomies: true,
        showSearchBox: true,
        showSearchBrowser: true,
        variableTaxonomiesOrder: [],
        studyTaxonomiesOrder: [],
        datasetTaxonomiesOrder: [],
        networkTaxonomiesOrder: [],
        hideNavigate: [],
        hideSearch: ['studyIds', 'dceIds', 'datasetId', 'networkId', 'studyId'],
        variables: {
          showSearchTab: true,
          variablesColumn: {
            showVariablesTypeColumn: true,
            showVariablesStudiesColumn: true,
            showVariablesDatasetsColumn: true,
            showDatasetsStudiesColumn: true,
            showDatasetsVariablesColumn: true
          }
        },
        datasets: {
          showSearchTab: true,
          showDatasetsSearchFilter: true,
          datasetsColumn: {
            showDatasetsAcronymColumn: true,
            showDatasetsTypeColumn: true,
            showDatasetsNetworkColumn: true,
            showDatasetsStudiesColumn: true,
            showDatasetsVariablesColumn: true
          }
        },
        studies: {
          showSearchTab: true,
          showStudiesSearchFilter: true,
          studiesColumn: {
            showStudiesDesignColumn: true,
            showStudiesQuestionnaireColumn: true,
            showStudiesPmColumn: true,
            showStudiesBioColumn: true,
            showStudiesOtherColumn: true,
            showStudiesParticipantsColumn: true,
            showStudiesNetworksColumn: true,
            showStudiesStudyDatasetsColumn: true,
            showStudiesHarmonizationDatasetsColumn: true,
            showStudiesVariablesColumn: false,
            showStudiesStudyVariablesColumn: true,
            showStudiesDataschemaVariablesColumn: true
          }
        },
        networks: {
          showSearchTab: true,
          networksColumn: {
            showNetworksStudiesColumn: true,
            showNetworksStudyDatasetColumn: true,
            showNetworksHarmonizationDatasetColumn: true,
            showNetworksVariablesColumn: false,
            showNetworksStudyVariablesColumn: true,
            showNetworksDataschemaVariablesColumn: true
          }
        },
        coverage: {
          groupBy: {
            study: true,
            dce: true,
            dataset: true,
            dataschema: true,
            network: true
          }
        }
      };

      this.setLocaleResolver = function(resolver) {
        localeResolver = resolver;
      };

      this.setOptions = function (value) {
        options = angular.merge(options, value);
        //NOTICE: angular.merge merges arrays by position. Overriding manually.
        options.targetTabsOrder = value.targetTabsOrder || options.targetTabsOrder;
        options.searchTabsOrder = value.searchTabsOrder || options.searchTabsOrder;
        options.resultTabsOrder = value.resultTabsOrder || options.resultTabsOrder;
        options.variableTaxonomiesOrder = value.variableTaxonomiesOrder || options.variableTaxonomiesOrder;
        options.studyTaxonomiesOrder = value.studyTaxonomiesOrder || options.studyTaxonomiesOrder;
        options.datasetTaxonomiesOrder = value.datasetTaxonomiesOrder || options.datasetTaxonomiesOrder;
        options.networkTaxonomiesOrder = value.networkTaxonomiesOrder || options.networkTaxonomiesOrder;
        options.hideNavigate = value.hideNavigate || options.hideNavigate;
        options.hideSearch = value.hideSearch || options.hideSearch;
      };

      this.$get = ['$q', '$injector', function ngObibaMicaSearchFactory($q, $injector) {
        function normalizeOptions() {
          var canShowCoverage = Object.keys(options.coverage.groupBy).filter(function(canShow) {
              return options.coverage.groupBy[canShow];
            }).length > 0;

          if (!canShowCoverage) {
            var index = options.searchTabsOrder.indexOf(DISPLAY_TYPES.COVERAGE);
            if (index > -1) {
              options.searchTabsOrder.splice(index, 1);
            }
          }
        }

        normalizeOptions();

        return {
          getLocale: function(success, error) {
            return $q.when($injector.invoke(localeResolver), success, error);
          },
          getOptions: function() {
            return options;
          },
          toggleHideSearchNavigate: function (vocabulary) {
            var index = options.hideNavigate.indexOf(vocabulary.name);
            if (index > -1) {
              options.hideNavigate.splice(index, 1);
            } else {
              options.hideNavigate.push(vocabulary.name);
            }
          }
        };
      }];
    });
  }])
  .run(['GraphicChartsConfigurations',
  function (GraphicChartsConfigurations) {
    GraphicChartsConfigurations.setClientConfig();
  }]);
;/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.search')

  .filter('regex', function() {
    return function(elements, regex, fields, lang) {
      var out = [];

      try {
        var pattern = new RegExp(regex, 'i');
        out = elements.filter(function(element) {
          return fields.some(function(field) {
            var value = element[field];
            
            if(angular.isArray(value) && lang) {
              return value.filter(function(item) {
                return item.locale === lang;
              }).some(function(item) {
                return pattern.test(item.text);
              });
            }

            return pattern.test(value);
          });
        });
      } catch(e) {
      }

      return out;
    };
  })

  .filter('orderBySelection', function() {
    return function (elements, selections) {
      if (!elements){
        return [];
      }

      var selected = [];
      var unselected = [];

      elements.forEach(function(element) {
        if (selections[element.key]) {
          selected.push(element);
        } else {
          unselected.push(element);
        }
      });

      return selected.concat(unselected);
    };
  })

  .filter('dceDescription', function() {
    return function(input) {
      return input.split(':<p>').map(function(d){
        return '<p>' + d;
      })[2];
    };
  });;/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/* exported QUERY_TYPES */
var QUERY_TYPES = {
  NETWORKS: 'networks',
  STUDIES: 'studies',
  DATASETS: 'datasets',
  VARIABLES: 'variables'
};

/* exported QUERY_TARGETS */
var QUERY_TARGETS = {
  NETWORK: 'network',
  STUDY: 'study',
  DATASET: 'dataset',
  VARIABLE: 'variable'
};

/* exported BUCKET_TYPES */
var BUCKET_TYPES = {
  NETWORK: 'network',
  STUDY: 'study',
  DCE: 'dce',
  DATASCHEMA: 'dataschema',
  DATASET: 'dataset'
};

/* exported RQL_NODE */
var RQL_NODE = {
  // target nodes
  VARIABLE: 'variable',
  DATASET: 'dataset',
  STUDY: 'study',
  NETWORK: 'network',

  /* target children */
  LIMIT: 'limit',
  SORT: 'sort',
  AND: 'and',
  NAND: 'nand',
  OR: 'or',
  NOR: 'nor',
  NOT: 'not',
  FACET: 'facet',
  LOCALE: 'locale',
  AGGREGATE: 'aggregate',
  BUCKET: 'bucket',

  /* leaf criteria nodes */
  CONTAINS: 'contains',
  IN: 'in',
  OUT: 'out',
  EQ: 'eq',
  GT: 'gt',
  GE: 'ge',
  LT: 'lt',
  LE: 'le',
  BETWEEN: 'between',
  MATCH: 'match',
  EXISTS: 'exists',
  MISSING: 'missing'
};

/* exported SORT_FIELDS */
var SORT_FIELDS = {
  ACRONYM: 'acronym',
  NAME: 'name'
};

/* exported targetToType */
function targetToType(target) {
  switch (target.toLocaleString()) {
    case QUERY_TARGETS.NETWORK:
      return QUERY_TYPES.NETWORKS;
    case QUERY_TARGETS.STUDY:
      return QUERY_TYPES.STUDIES;
    case QUERY_TARGETS.DATASET:
      return QUERY_TYPES.DATASETS;
    case QUERY_TARGETS.VARIABLE:
      return QUERY_TYPES.VARIABLES;
  }

  throw new Error('Invalid target: ' + target);
}

/* exported targetToType */
function typeToTarget(type) {
  switch (type.toLocaleString()) {
    case QUERY_TYPES.NETWORKS:
      return QUERY_TARGETS.NETWORK;
    case QUERY_TYPES.STUDIES:
      return QUERY_TARGETS.STUDY;
    case QUERY_TYPES.DATASETS:
      return QUERY_TARGETS.DATASET;
    case QUERY_TYPES.VARIABLES:
      return QUERY_TARGETS.VARIABLE;
  }

  throw new Error('Invalid type: ' + type);
}

/* exported VOCABULARY_TYPES */
var VOCABULARY_TYPES = {
  STRING: 'string',
  INTEGER: 'integer',
  DECIMAL: 'decimal'
};

/* exported CriteriaIdGenerator */
var CriteriaIdGenerator = {
  generate: function (taxonomy, vocabulary, term) {
    return taxonomy && vocabulary ?
    taxonomy.name + '.' + vocabulary.name + (term ? '.' + term.name : '') :
      undefined;
  }
};

/* exported CriteriaItem */
function CriteriaItem(model) {
  var self = this;
  Object.keys(model).forEach(function(k) {
    self[k] = model[k];
  });
}

CriteriaItem.prototype.isRepeatable = function() {
  return false;
};

CriteriaItem.prototype.getTarget = function() {
  return this.target || null;
};

/* exported RepeatableCriteriaItem */
function RepeatableCriteriaItem() {
  CriteriaItem.call(this, {});
  this.list = [];
}

RepeatableCriteriaItem.prototype = Object.create(CriteriaItem.prototype);

RepeatableCriteriaItem.prototype.isRepeatable = function() {
  return true;
};

RepeatableCriteriaItem.prototype.addItem = function(item) {
  this.list.push(item);
  return this;
};

RepeatableCriteriaItem.prototype.items = function() {
  return this.list;
};

RepeatableCriteriaItem.prototype.first = function() {
  return this.list[0];
};

RepeatableCriteriaItem.prototype.getTarget = function() {
  return this.list.length > 0 ? this.list[0].getTarget() : null;
};

/**
 * Criteria Item builder
 */
function CriteriaItemBuilder(LocalizedValues, useLang) {
  var criteria = {
    type: null,
    rqlQuery: null,
    lang: useLang || 'en',
    parent: null,
    children: []
  };

  this.type = function (value) {
    if (!RQL_NODE[value.toUpperCase()]) {
      throw new Error('Invalid node type:', value);
    }
    criteria.type = value;
    return this;
  };

  this.target = function (value) {
    criteria.target = value;
    return this;
  };

  this.parent = function (value) {
    criteria.parent = value;
    return this;
  };

  this.taxonomy = function (value) {
    criteria.taxonomy = value;
    return this;
  };

  this.vocabulary = function (value) {
    criteria.vocabulary = value;
    return this;
  };

  this.term = function (value) {
    criteria.term = value;
    return this;
  };

  this.rqlQuery = function (value) {
    criteria.rqlQuery = value;
    return this;
  };

  this.selectedTerm = function (term) {
    if (!criteria.selectedTerms) {
      criteria.selectedTerms = [];
    }

    criteria.selectedTerms.push(typeof term === 'string' ? term : term.name);
    return this;
  };

  this.selectedTerms = function (terms) {
    criteria.selectedTerms = terms.filter(function (term) {
      return term;
    }).map(function (term) {
      if (typeof term === 'string') {
        return term;
      } else {
        return term.name;
      }
    });
    return this;
  };

  /**
   * This is
   */
  function prepareForLeaf() {
    if (criteria.term) {
      criteria.itemTitle = LocalizedValues.forLocale(criteria.term.title, criteria.lang);
      criteria.itemDescription = LocalizedValues.forLocale(criteria.term.description, criteria.lang);
      criteria.itemParentTitle = LocalizedValues.forLocale(criteria.vocabulary.title, criteria.lang);
      criteria.itemParentDescription = LocalizedValues.forLocale(criteria.vocabulary.description, criteria.lang);
      if (!criteria.itemTitle) {
        criteria.itemTitle = criteria.term.name;
      }
      if (!criteria.itemParentTitle) {
        criteria.itemParentTitle = criteria.vocabulary.name;
      }
    } else {
      criteria.itemTitle = LocalizedValues.forLocale(criteria.vocabulary.title, criteria.lang);
      criteria.itemDescription = LocalizedValues.forLocale(criteria.vocabulary.description, criteria.lang);
      criteria.itemParentTitle = LocalizedValues.forLocale(criteria.taxonomy.title, criteria.lang);
      criteria.itemParentDescription = LocalizedValues.forLocale(criteria.taxonomy.description, criteria.lang);
      if (!criteria.itemTitle) {
        criteria.itemTitle = criteria.vocabulary.name;
      }
      if (!criteria.itemParentTitle) {
        criteria.itemParentTitle = criteria.taxonomy.name;
      }
    }

    criteria.id = CriteriaIdGenerator.generate(criteria.taxonomy, criteria.vocabulary, criteria.term);
  }

  this.build = function () {
    if (criteria.taxonomy && criteria.vocabulary) {
      prepareForLeaf();
    }
    return new CriteriaItem(criteria);
  };

}

/**
 * Class for all criteria builders
 * @param rootRql
 * @param rootItem
 * @param taxonomies
 * @param LocalizedValues
 * @param lang
 * @constructor
 */
function CriteriaBuilder(rootRql, rootItem, taxonomies, LocalizedValues, lang) {

  /**
   * Helper to get a builder
   * @returns {CriteriaItemBuilder}
   */
  this.newCriteriaItemBuilder = function () {
    return new CriteriaItemBuilder(LocalizedValues, lang);
  };

  this.initialize = function (target) {
    this.leafItemMap = {};
    this.target = target;
    this.rootRql = rootRql;
    this.taxonomies = taxonomies;
    this.LocalizedValues = LocalizedValues;
    this.lang = lang;
    this.rootItem = this.newCriteriaItemBuilder()
      .parent(rootItem)
      .type(this.target)
      .rqlQuery(this.rootRql)
      .target(this.target)
      .build();
  };

  /**
   * Called by the leaf visitor to create a criteria
   * @param targetTaxonomy
   * @param targetVocabulary
   * @param targetTerms
   * @param node
   */
  this.buildLeafItem = function (targetTaxonomy, targetVocabulary, targetTerms, node, parentItem) {
    var self = this;

    var builder = new CriteriaItemBuilder(self.LocalizedValues, self.lang)
      .type(node.name)
      .target(self.target)
      .taxonomy(targetTaxonomy)
      .vocabulary(targetVocabulary)
      .rqlQuery(node)
      .parent(parentItem);

    builder.selectedTerms(targetTerms).build();

    return builder.build();
  };

}

/**
 * Search for the taxonomy vocabulary corresponding to the provided field name. Can be defined either in the
 * vocabulary field attribute or be the vocabulary name.
 * @param field
 * @returns {{taxonomy: null, vocabulary: null}}
 */
CriteriaBuilder.prototype.fieldToVocabulary = function (field) {
  var found = {
    taxonomy: null,
    vocabulary: null
  };

  var normalizedField = field;
  if (field.indexOf('.') < 0) {
    normalizedField = 'Mica_' + this.target + '.' + field;
  }
  var parts = normalizedField.split('.', 2);
  var targetTaxonomy = parts[0];
  var targetVocabulary = parts[1];

  var foundTaxonomy = this.taxonomies.filter(function (taxonomy) {
    return targetTaxonomy === taxonomy.name;
  });

  if (foundTaxonomy.length === 0) {
    throw new Error('Could not find taxonomy:', targetTaxonomy);
  }

  found.taxonomy = foundTaxonomy[0];

  var foundVocabulary = found.taxonomy.vocabularies.filter(function (vocabulary) {
    return targetVocabulary === vocabulary.name;
  });

  if (foundVocabulary.length === 0) {
    throw new Error('Could not find vocabulary:', targetVocabulary);
  }

  found.vocabulary = foundVocabulary[0];

  return found;
};

/**
 * This method is where a criteria gets created
 */
CriteriaBuilder.prototype.visitLeaf = function (node, parentItem) {
  var match = RQL_NODE.MATCH === node.name;
  var field = node.args[match ? 1 : 0];
  var values = node.args[match ? 0 : 1];

  var searchInfo = this.fieldToVocabulary(field);
  var item =
    this.buildLeafItem(searchInfo.taxonomy,
      searchInfo.vocabulary,
      values instanceof Array ? values : [values],
      node,
      parentItem);

  var current = this.leafItemMap[item.id];

  if (current) {
    if (current.isRepeatable()) {
      current.addItem(item);
    } else {
      console.error('Non-repeatable criteria items must be unique,', current.id, 'will be overwritten.');
      current = item;
    }
  } else {
    current = item.vocabulary.repeatable ? new RepeatableCriteriaItem().addItem(item) : item;
  }

  this.leafItemMap[item.id] = current;

  parentItem.children.push(item);
};

/**
 * Returns all the criterias found
 * @returns {Array}
 */
CriteriaBuilder.prototype.getRootItem = function () {
  return this.rootItem;
};

/**
 * Returns the leaf criteria item map needed for finding duplicates
 * @returns {Array}
 */
CriteriaBuilder.prototype.getLeafItemMap = function () {
  return this.leafItemMap;
};

/**
 * Node condition visitor
 * @param node
 * @param parentItem
 */
CriteriaBuilder.prototype.visitCondition = function (node, parentItem) {
  var item = this.newCriteriaItemBuilder().parent(parentItem).rqlQuery(node).type(node.name).build();
  parentItem.children.push(item);

  this.visit(node.args[0], item);
  this.visit(node.args[1], item);
};

/**
 * Node not visitor
 * @param node
 * @param parentItem
 */
CriteriaBuilder.prototype.visitNot = function (node, parentItem) {
  var item = this.newCriteriaItemBuilder().parent(parentItem).rqlQuery(node).type(node.name).build();
  parentItem.children.push(item);

  this.visit(node.args[0], item);
};

/**
 * General purpose node visitor
 * @param node
 * @param parentItem
 */
CriteriaBuilder.prototype.visit = function (node, parentItem) {

  // TODO needs to add more types
  switch (node.name) {
    case RQL_NODE.NOT:
      this.visitNot(node, parentItem);
      break;
    case RQL_NODE.AND:
    case RQL_NODE.NAND:
    case RQL_NODE.OR:
    case RQL_NODE.NOR:
      this.visitCondition(node, parentItem);
      break;

    case RQL_NODE.CONTAINS:
    case RQL_NODE.IN:
    case RQL_NODE.OUT:
    case RQL_NODE.EQ:
    case RQL_NODE.LE:
    case RQL_NODE.LT:
    case RQL_NODE.GE:
    case RQL_NODE.GT:
    case RQL_NODE.BETWEEN:
    case RQL_NODE.EXISTS:
    case RQL_NODE.MISSING:
    case RQL_NODE.MATCH:
      this.visitLeaf(node, parentItem);
      break;
    default:
  }
};

/**
 * Builds a criteria list for this target
 */
CriteriaBuilder.prototype.build = function () {
  var self = this;
  this.rootRql.args.forEach(function (node) {
    self.visit(node, self.rootItem);
  });
};

angular.module('obiba.mica.search')

  // TODO merge with RqlQueryService or place all node manipularions here
  .service('RqlQueryUtils', [function () {
    var self = this;

    /**
     * Finds the parent node to which new queries can be added
     *
     * @param targetNode
     * @returns {*}
     */
    function findValidParentNode(targetNode) {
      var target = targetNode.args.filter(function (query) {
        switch (query.name) {
          case RQL_NODE.AND:
          case RQL_NODE.NAND:
          case RQL_NODE.OR:
          case RQL_NODE.NOR:
          case RQL_NODE.NOT:
          case RQL_NODE.CONTAINS:
          case RQL_NODE.IN:
          case RQL_NODE.OUT:
          case RQL_NODE.EQ:
          case RQL_NODE.GT:
          case RQL_NODE.GE:
          case RQL_NODE.LT:
          case RQL_NODE.LE:
          case RQL_NODE.BETWEEN:
          case RQL_NODE.MATCH:
          case RQL_NODE.EXISTS:
          case RQL_NODE.MISSING:
            return true;
        }

        return false;
      }).pop();

      if (target) {
        return targetNode.args.findIndex(function (arg) {
          return arg.name === target.name;
        });
      }

      return -1;
    }

    this.vocabularyTermNames = function (vocabulary) {
      return vocabulary && vocabulary.terms ? vocabulary.terms.map(function (term) {
        return term.name;
      }) : [];
    };

    this.hasTargetQuery = function (rootRql, target) {
      return rootRql.args.filter(function (query) {
          switch (query.name) {
            case RQL_NODE.VARIABLE:
            case RQL_NODE.DATASET:
            case RQL_NODE.STUDY:
            case RQL_NODE.NETWORK:
              return target ? target === query.name : true;
            default:
              return false;
          }
        }).length > 0;
    };

    this.variableQuery = function () {
      return new RqlQuery(QUERY_TARGETS.VARIABLE);
    };

    this.eqQuery = function (field, term) {
      var query = new RqlQuery(RQL_NODE.EQ);
      query.args.push(term);
      return query;
    };

    this.orQuery = function (left, right) {
      var query = new RqlQuery(RQL_NODE.OR);
      query.args = [left, right];
      return query;
    };

    this.aggregate = function (fields) {
      var query = new RqlQuery(RQL_NODE.AGGREGATE);
      fields.forEach(function (field) {
        query.args.push(field);
      });
      return query;
    };

    this.limit = function (from, size) {
      var query = new RqlQuery(RQL_NODE.LIMIT);
      query.args.push(from);
      query.args.push(size);
      return query;
    };

    this.fieldQuery = function (name, field, terms) {
      var query = new RqlQuery(name);
      query.args.push(field);

      if (terms && terms.length > 0) {
        query.args.push(terms);
      }

      return query;
    };

    this.inQuery = function (field, terms) {
      var hasValues = terms && terms.length > 0;
      var name = hasValues ? RQL_NODE.IN : RQL_NODE.EXISTS;
      return this.fieldQuery(name, field, terms);
    };

    this.matchQuery = function (field, queryString) {
      var query = new RqlQuery(RQL_NODE.MATCH);
      query.args.push(queryString || '*');
      query.args.push(field);
      return query;
    };

    this.updateMatchQuery = function (query, queryString) {
      query.args[0] = queryString || '*';
      return query;
    };

    this.rangeQuery = function (field, from, to) {
      var query = new RqlQuery(RQL_NODE.BETWEEN);
      query.args.push(field);
      self.updateRangeQuery(query, from, to);
      return query;
    };

    this.updateQueryInternal = function (query, terms) {
      var hasValues = terms && terms.length > 0;

      if (hasValues) {
        query.args[1] = terms;
      } else {
        query.args.splice(1, 1);
      }

      return query;
    };

    this.mergeInQueryArgValues = function (query, terms, replace) {
      var hasValues = terms && terms.length > 0;

      if (hasValues) {
        var current = query.args[1];

        if (!current || replace) {
          query.args[1] = terms;
        } else {
          if (!(current instanceof Array)) {
            current = [current];
          }

          var unique = terms.filter(function (term) {
            return current.indexOf(term) === -1;
          });

          query.args[1] = current.concat(unique);
        }
      } else {
        query.args.splice(1, 1);
      }

      return query;
    };

    this.updateRangeQuery = function (query, from, to, missing) {
      if (missing) {
        query.name = RQL_NODE.MISSING;
        query.args.splice(1, 1);
      } else if (angular.isDefined(from) && from !== null && angular.isDefined(to) && to !== null) {
        query.name = RQL_NODE.BETWEEN;
        query.args[1] = [from, to];
      } else if (angular.isDefined(from) && from !== null) {
        query.name = RQL_NODE.GE;
        query.args[1] = from;
      } else if (angular.isDefined(to) && to !== null) {
        query.name = RQL_NODE.LE;
        query.args[1] = to;
      } else {
        query.name = RQL_NODE.EXISTS;
        query.args.splice(1, 1);
      }
    };

    /**
     * Creates a RqlQuery from an item
     *
     * @param item
     * @returns {RqlQuery}
     */
    this.buildRqlQuery = function (item) {
      if (this.isNumericVocabulary(item.vocabulary)) {
        return this.rangeQuery(this.criteriaId(item.taxonomy, item.vocabulary), null, null);
      } else if (this.isMatchVocabulary(item.vocabulary)) {
        return this.matchQuery(this.criteriaId(item.taxonomy, item.vocabulary), null);
      } else {
        return this.inQuery(
          this.criteriaId(item.taxonomy, item.vocabulary),
          item.term ? item.term.name : undefined
        );
      }
    };

    /**
     * Adds a new query to the parent query node
     *
     * @param parentQuery
     * @param query
     * @returns {*}
     */
    this.addQuery = function (parentQuery, query, logicalOp) {
      if (parentQuery.args.length === 0) {
        parentQuery.args.push(query);
      } else {
        var parentIndex = findValidParentNode(parentQuery);

        if (parentIndex === -1) {
          parentQuery.args.push(query);
        } else {
          var oldArg = parentQuery.args.splice(parentIndex, 1).pop();
          // check if the field is from the target's taxonomy, in which case the criteria is
          // added with a AND operator otherwise it is a OR
          if (!logicalOp && query.args && query.args.length > 0) {
            var targetTaxo = 'Mica_' + parentQuery.name;
            var criteriaVocabulary = query.name === 'match' ? query.args[1] : query.args[0];
            logicalOp = criteriaVocabulary.startsWith(targetTaxo + '.') ? RQL_NODE.AND : RQL_NODE.OR;
          }
          var orQuery = new RqlQuery(logicalOp || RQL_NODE.AND);
          orQuery.args.push(oldArg, query);
          parentQuery.args.push(orQuery);
        }
      }

      return parentQuery;
    };

    /**
     * Update repeatable vocabularies as follows:
     *
     * IN(q, [a,b]) OR [c] => CONTAINS(q, [a,c]) OR CONTAINS(q, [b,c])
     * CONTAINS(q, [a,b]) OR [c] => CONTAINS(q, [a,b,c])
     * EXISTS(q) OR [c] => CONTAINS(q, [c])
     *
     * @param existingItemWrapper
     * @param terms
     */
    this.updateRepeatableQueryArgValues = function (existingItem, terms) {
      var self = this;
      existingItem.items().forEach(function(item) {
        var query = item.rqlQuery;
        switch (query.name) {
          case RQL_NODE.EXISTS:
            query.name = RQL_NODE.CONTAINS;
            self.mergeInQueryArgValues(query, terms, false);
            break;

          case RQL_NODE.CONTAINS:
            self.mergeInQueryArgValues(query, terms, false);
            break;

          case RQL_NODE.IN:
            var values = query.args[1] ?  [].concat(query.args[1]) : [];
            if (values.length === 1) {
              query.name = RQL_NODE.CONTAINS;
              self.mergeInQueryArgValues(query, terms, false);
              break;
            }

            var field = query.args[0];
            var contains = values.filter(function(value){
              // remove duplicates (e.g. CONTAINS(q, [a,a])
              return terms.indexOf(value) < 0;
            }).map(function(value){
              return self.fieldQuery(RQL_NODE.CONTAINS, field, [].concat(value, terms));
            });

            var orRql;
            if (contains.length > 1) {
              var firstTwo = contains.splice(0, 2);
              orRql = self.orQuery(firstTwo[0], firstTwo[1]);

              contains.forEach(function(value){
                orRql = self.orQuery(value, orRql);
              });
              
              query.name = orRql.name;
              query.args = orRql.args;
            } else {
              query.name = RQL_NODE.CONTAINS;
              query.args = contains[0].args;
            }
        }
      });

    };

    this.updateQueryArgValues = function (query, terms, replace) {
      switch (query.name) {
        case RQL_NODE.EXISTS:
        case RQL_NODE.MISSING:
          query.name = RQL_NODE.IN;
          this.mergeInQueryArgValues(query, terms, replace);
          break;
        case RQL_NODE.CONTAINS:
        case RQL_NODE.IN:
          this.mergeInQueryArgValues(query, terms, replace);
          break;
        case RQL_NODE.BETWEEN:
        case RQL_NODE.GE:
        case RQL_NODE.LE:
          query.args[1] = terms;
          break;
        case RQL_NODE.MATCH:
          query.args[0] = terms;
          break;
      }
    };

    this.updateQuery = function (query, values) {
      switch (query.name) {
        case RQL_NODE.CONTAINS:
        case RQL_NODE.IN:
        case RQL_NODE.EXISTS:
        case RQL_NODE.MISSING:
          this.updateQueryInternal(query, values);
          break;
      }
    };

    function vocabularyAttributeValue(vocabulary, key, defaultValue) {
      var value = defaultValue;
      if (vocabulary.attributes) {
        vocabulary.attributes.some(function (attribute) {
          if (attribute.key === key) {
            value = attribute.value;
            return true;
          }

          return false;
        });
      }

      return value;
    }

    this.addLocaleQuery = function (rqlQuery, locale) {
      var found = rqlQuery.args.filter(function (arg) {
        return arg.name === RQL_NODE.LOCALE;
      }).pop();

      if (!found) {
        var localeQuery = new RqlQuery('locale');
        localeQuery.args.push(locale);
        rqlQuery.args.push(localeQuery);
      }
    };

    this.addLimit = function (targetQuery, limitQuery) {
      var found = targetQuery.args.filter(function (arg) {
        return arg.name === RQL_NODE.LIMIT;
      }).pop();

      if (found) {
        found.args = limitQuery.args;
      } else {
        targetQuery.args.push(limitQuery);
      }
    };

    this.addSort = function (targetQuery, sort) {
      var found = targetQuery.args.filter(function (arg) {
        return arg.name === RQL_NODE.SORT;
      }).pop();

      if (!found) {
        var sortQuery = new RqlQuery('sort');
        sortQuery.args.push(sort);
        targetQuery.args.push(sortQuery);
      }
    };

    /**
     * Helper finding the vocabulary field, return name if none was found
     *
     * @param taxonomy
     * @param vocabulary
     * @returns {*}
     */
    this.criteriaId = function (taxonomy, vocabulary) {
      return taxonomy.name + '.' + vocabulary.name;
    };

    this.vocabularyType = function (vocabulary) {
      return vocabularyAttributeValue(vocabulary, 'type', VOCABULARY_TYPES.STRING);
    };

    this.vocabularyField = function (vocabulary) {
      return vocabularyAttributeValue(vocabulary, 'field', vocabulary.name);
    };

    this.vocabularyAlias = function (vocabulary) {
      return vocabularyAttributeValue(vocabulary, 'alias', vocabulary.name);
    };

    this.isTermsVocabulary = function (vocabulary) {
      return self.vocabularyType(vocabulary) === VOCABULARY_TYPES.STRING && vocabulary.terms;
    };

    this.isMatchVocabulary = function (vocabulary) {
      return self.vocabularyType(vocabulary) === VOCABULARY_TYPES.STRING && !vocabulary.terms;
    };

    this.isNumericVocabulary = function (vocabulary) {
      return !vocabulary.terms && (self.vocabularyType(vocabulary) === VOCABULARY_TYPES.INTEGER || self.vocabularyType(vocabulary) === VOCABULARY_TYPES.DECIMAL);
    };

    this.isRangeVocabulary = function (vocabulary) {
      return vocabulary.terms && (self.vocabularyType(vocabulary) === VOCABULARY_TYPES.INTEGER || self.vocabularyType(vocabulary) === VOCABULARY_TYPES.DECIMAL);
    };
  }])

  .service('RqlQueryService', [
    '$q',
    'TaxonomiesResource',
    'TaxonomyResource',
    'LocalizedValues',
    'RqlQueryUtils',
    function ($q, TaxonomiesResource, TaxonomyResource, LocalizedValues, RqlQueryUtils) {
      var taxonomiesCache = {
        variable: null,
        dataset: null,
        study: null,
        network: null
      };

      function findTargetQuery(target, query) {
        return query.args.filter(function (arg) {
          return arg.name === target;
        }).pop();
      }

      function isLeafCriteria(item) {
        switch (item.type) {
          case RQL_NODE.CONTAINS:
          case RQL_NODE.IN:
          case RQL_NODE.OUT:
          case RQL_NODE.EQ:
          case RQL_NODE.GT:
          case RQL_NODE.GE:
          case RQL_NODE.LT:
          case RQL_NODE.LE:
          case RQL_NODE.BETWEEN:
          case RQL_NODE.MATCH:
          case RQL_NODE.EXISTS:
          case RQL_NODE.MISSING:
            return true;
        }

        return false;
      }

      function deleteNode(item) {
        var parent = item.parent;
        var query = item.rqlQuery;
        var queryArgs = query.args;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);
        var indexChild = parent.children.indexOf(item);

        if (index === -1 || indexChild === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parent.children.splice(indexChild, 1);
        item.children.forEach(function (c) {
          c.parent = parent;
        });
        parent.children.splice.apply(parent.children, [indexChild, 0].concat(item.children));

        parentQuery.args.splice(index, 1);

        if (queryArgs) {
          if (queryArgs instanceof Array) {
            parentQuery.args.splice.apply(parentQuery.args, [index, 0].concat(queryArgs));
          } else {
            parentQuery.args.splice(index, 0, queryArgs);
          }
        }

        if (parent.parent !== null && parentQuery.args.length === 0) {
          deleteNode(parent);
        }
      }

      function deleteNodeCriteriaWithOrphans(item) {
        var parent = item.parent;
        var query = item.rqlQuery;
        var queryArgs = query.args;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);
        var indexChild = parent.children.indexOf(item);

        if (index === -1 || indexChild === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parent.children.splice(indexChild, 1);
        item.children.forEach(function (c) {
          c.parent = parent;
        });
        parent.children.splice.apply(parent.children, [indexChild, 0].concat(item.children));

        parentQuery.args.splice(index, 1);

        if (queryArgs) {
          if (queryArgs instanceof Array) {
            parentQuery.args.splice.apply(parentQuery.args, [index, 0].concat(queryArgs));
          } else {
            parentQuery.args.splice(index, 0, queryArgs);
          }
        }

        if (parentQuery.args.length === 0) {
          deleteNode(parent);
        }
      }

      function deleteLeafCriteria(item) {
        var parent = item.parent;
        if (!parent) {
          throw new Error('Cannot remove criteria when parent is NULL');
        }

        var query = item.rqlQuery;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);

        if (index === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parentQuery.args.splice(index, 1);

        if ([RQL_NODE.OR, RQL_NODE.AND, RQL_NODE.NAND, RQL_NODE.NOR].indexOf(parent.type) !== -1) {
          deleteNodeCriteriaWithOrphans(parent);
        } else if (parentQuery.args.length === 0) {
          deleteNode(parent);
        }

      }

      /**
       * Removes the item from criteria item tree. This should be from a leaf.
       * @param item
       */
      this.removeCriteriaItem = function (item) {
        if (isLeafCriteria(item)) {
          deleteLeafCriteria(item);
        } else {
          deleteNode(item);
        }
      };

      /**
       * Creates a criteria item
       * @param target
       * @param taxonomy
       * @param vocabulary
       * @param term
       * @param lang
       * @returns A criteria item
       */
      this.createCriteriaItem = function (target, taxonomy, vocabulary, term, lang) {
        function createBuilder(taxonomy, vocabulary, term) {
          return new CriteriaItemBuilder(LocalizedValues, lang)
            .target(target)
            .taxonomy(taxonomy)
            .vocabulary(vocabulary)
            .term(term);
        }

        if (angular.isString(taxonomy)) {
          return TaxonomyResource.get({
            target: target,
            taxonomy: taxonomy
          }).$promise.then(function (taxonomy) {
            vocabulary = taxonomy.vocabularies.filter(function (v) {return v.name === vocabulary; })[0];
            term = vocabulary.terms.filter(function (t) {return t.name === term; })[0];

            return createBuilder(taxonomy, vocabulary, term).build();
          });
        }

        return createBuilder(taxonomy, vocabulary, term).build();
      };

      /**
       * Adds new item to the item tree
       *
       * @param rootItem
       * @param item
       */
      this.addCriteriaItem = function (rootRql, newItem, logicalOp) {
        var target = rootRql.args.filter(function (query) {
          return newItem.target === query.name;
        }).pop();

        if (!target) {
          target = new RqlQuery(RQL_NODE[newItem.target.toUpperCase()]);
          rootRql.args.push(target);
        }

        var rqlQuery = newItem.rqlQuery ? newItem.rqlQuery : RqlQueryUtils.buildRqlQuery(newItem);
        return RqlQueryUtils.addQuery(target, rqlQuery, logicalOp);
      };

      /**
       * Update an existing item to the item tree
       *
       * @param rootItem
       * @param item
       */
      this.updateCriteriaItem = function (existingItem, newItem, replace) {
        var newTerms;
        var isRepeatable = existingItem.isRepeatable();
        var isMatchNode = !isRepeatable && existingItem.rqlQuery.name === RQL_NODE.MATCH;

        if(replace && newItem.rqlQuery) {
          existingItem.rqlQuery.name = newItem.rqlQuery.name;
        }
        
        if (newItem.rqlQuery) {
          newTerms = newItem.rqlQuery.args[isMatchNode ? 0 : 1];
        } else if (newItem.term) {
          newTerms = [newItem.term.name];
        } else {
          existingItem = isRepeatable ? existingItem.first() : existingItem;
          existingItem.rqlQuery.name = RQL_NODE.EXISTS;
          existingItem.rqlQuery.args.splice(1, 1);
        }

        if (newTerms) {
          if (isRepeatable) {
            RqlQueryUtils.updateRepeatableQueryArgValues(existingItem, newTerms);
          } else {
            RqlQueryUtils.updateQueryArgValues(existingItem.rqlQuery, newTerms, replace);
          }
        }
      };

      /**
       * Builders registry
       *
       * @type {{variable: builders.variable, study: builders.study}}
       */
      this.builders = function (target, rootRql, rootItem, lang) {
        var deferred = $q.defer();

        function build(rootRql, rootItem) {
          var builder = new CriteriaBuilder(rootRql, rootItem, taxonomiesCache[target], LocalizedValues, lang);
          builder.initialize(target);
          builder.build();
          deferred.resolve({root: builder.getRootItem(), map: builder.getLeafItemMap()});
        }

        if (taxonomiesCache[target]) {
          build(rootRql, rootItem);
        } else {
          TaxonomiesResource.get({
            target: target
          }).$promise.then(function (response) {
            taxonomiesCache[target] = response;
            build(rootRql, rootItem);
          });
        }

        return deferred.promise;
      };

      /**
       * Builds the criteria tree
       *
       * @param rootRql
       * @param lang
       * @returns {*}
       */
      this.createCriteria = function (rootRql, lang) {
        var deferred = $q.defer();
        var rootItem = new CriteriaItemBuilder().type(RQL_NODE.AND).rqlQuery(rootRql).build();
        var leafItemMap = {};

        if (!RqlQueryUtils.hasTargetQuery(rootRql)) {
          deferred.resolve({root: rootItem, map: leafItemMap});
          return deferred.promise;
        }

        var queries = [];
        var self = this;
        var resolvedCount = 0;

        rootRql.args.forEach(function (node) {
          if (QUERY_TARGETS[node.name.toUpperCase()]) {
            queries.push(node);
          }
        });

        queries.forEach(function (node) {
          self.builders(node.name, node, rootItem, lang).then(function (result) {
            rootItem.children.push(result.root);
            leafItemMap = angular.extend(leafItemMap, result.map);
            resolvedCount++;
            if (resolvedCount === queries.length) {
              deferred.resolve({root: rootItem, map: leafItemMap});
            }
          });
        });

        return deferred.promise;
      };

      /**
       * Append the aggregate and facet for criteria term listing.
       *
       * @param query
       * @para
       * @returns the new query
       */
      this.prepareCriteriaTermsQuery = function (query, item, lang) {
        function iterReplaceQuery(query, criteriaId, newQuery) {
          if (!query || !query.args) {
            return null;
          }

          if ((query.name === RQL_NODE.IN || query.name === RQL_NODE.MISSING || query.name === RQL_NODE.CONTAINS) && query.args[0] === criteriaId) {
            return query;
          }

          for (var i = query.args.length; i--;) {
            var res = iterReplaceQuery(query.args[i], criteriaId, newQuery);

            if (res) {
              query.args[i] = newQuery;
            }
          }
        }

        var parsedQuery = new RqlParser().parse(query);
        var targetQuery = parsedQuery.args.filter(function (node) {
          return node.name === item.target;
        }).pop();

        if (targetQuery) {
          var anyQuery = new RqlQuery(RQL_NODE.EXISTS),
            criteriaId = RqlQueryUtils.criteriaId(item.taxonomy, item.vocabulary);

          anyQuery.args.push(criteriaId);
          iterReplaceQuery(targetQuery, criteriaId, anyQuery);
          targetQuery.args.push(RqlQueryUtils.aggregate([criteriaId]));
          targetQuery.args.push(RqlQueryUtils.limit(0, 0));
        }

        parsedQuery.args.push(new RqlQuery(RQL_NODE.FACET));

        if (lang) {
          RqlQueryUtils.addLocaleQuery(parsedQuery, lang);
        }

        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      this.prepareSearchQuery = function (type, query, pagination, lang, sort) {
        var rqlQuery = angular.copy(query);
        var target = typeToTarget(type);
        RqlQueryUtils.addLocaleQuery(rqlQuery, lang);
        var targetQuery = findTargetQuery(target, rqlQuery);

        if (!targetQuery) {
          targetQuery = new RqlQuery(target);
          rqlQuery.args.push(targetQuery);
        }

        var limit = pagination[target] || {from: 0, size: 10};
        RqlQueryUtils.addLimit(targetQuery, RqlQueryUtils.limit(limit.from, limit.size));

        if (sort) {
          RqlQueryUtils.addSort(targetQuery, sort);
        }

        return new RqlQuery().serializeArgs(rqlQuery.args);
      };

      /**
       * Append the aggregate and bucket operations to the variable.
       *
       * @param query
       * @param bucketArg
       * @returns the new query
       */
      this.prepareCoverageQuery = function (query, bucketArg) {
        var parsedQuery = new RqlParser().parse(query);
        var aggregate = new RqlQuery('aggregate');
        var bucketField;

        switch (bucketArg) {
          case BUCKET_TYPES.NETWORK:
            bucketField = 'networkId';
            break;
          case BUCKET_TYPES.STUDY:
            bucketField = 'studyIds';
            break;
          case BUCKET_TYPES.DCE:
            bucketField = 'dceIds';
            break;
          case BUCKET_TYPES.DATASCHEMA:
          case BUCKET_TYPES.DATASET:
            bucketField = 'datasetId';
            break;
        }

        var bucket = new RqlQuery('bucket');
        bucket.args.push(bucketField);
        aggregate.args.push(bucket);

        var variable;
        parsedQuery.args.forEach(function (arg) {
          if (!variable && arg.name === 'variable') {
            variable = arg;
          }
        });
        if (!variable) {
          variable = new RqlQuery('variable');
          parsedQuery.args.push(variable);
        }

        if (variable.args.length > 0 && variable.args[0].name !== 'limit') {
          var variableType = new RqlQuery('in');
          variableType.args.push('Mica_variable.variableType');
          if (bucketArg === BUCKET_TYPES.NETWORK || bucketArg === BUCKET_TYPES.DATASCHEMA) {
            variableType.args.push('Dataschema');
          } else {
            variableType.args.push('Study');
          }
          var andVariableType = new RqlQuery('and');
          andVariableType.args.push(variableType);
          andVariableType.args.push(variable.args[0]);
          variable.args[0] = andVariableType;
        }

        variable.args.push(aggregate);
        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      this.prepareGraphicsQuery = function (query, aggregateArgs, bucketArgs) {
        var parsedQuery = new RqlParser().parse(query);
        // aggregate
        var aggregate = new RqlQuery(RQL_NODE.AGGREGATE);
        aggregateArgs.forEach(function (a) {
          aggregate.args.push(a);
        });
        //bucket
        if (bucketArgs && bucketArgs.length > 0) {
          var bucket = new RqlQuery(RQL_NODE.BUCKET);
          bucketArgs.forEach(function (b) {
            bucket.args.push(b);
          });
          aggregate.args.push(bucket);
        }

        // study
        var study;
        var hasQuery = false;
        var hasStudyTarget = false;
        parsedQuery.args.forEach(function (arg) {
          if (arg.name === 'study') {
            hasStudyTarget = true;
            var limitIndex = null;
            hasQuery = arg.args.filter(function (requestArg, index) {
              if (requestArg.name === 'limit') {
                limitIndex = index;
              }
              return ['limit', 'sort', 'aggregate'].indexOf(requestArg.name) < 0;
            }).length;
            if (limitIndex !== null) {
              arg.args.splice(limitIndex, 1);
            }
            study = arg;
          }
        });
        // Study match all if no study query.
        if (!hasStudyTarget) {
          study = new RqlQuery('study');
          parsedQuery.args.push(study);
        }
        if (!hasQuery) {
          study.args.push(new RqlQuery(RQL_NODE.MATCH));
        }
        study.args.push(aggregate);
        // facet
        parsedQuery.args.push(new RqlQuery('facet'));
        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      this.getTargetAggregations = function (joinQueryResponse, criterion, lang) {

        /**
         * Helper to merge the terms that are not in the aggregation list
         *
         * @param aggs
         * @param vocabulary
         * @returns Array of aggs
         */
        function addMissingTerms(aggs, vocabulary) {
          var terms = vocabulary.terms;
          if (terms && terms.length > 0) {
            var keys = aggs && aggs.map(function (agg) {
                return agg.key;
              }) || [];

            if (aggs) {
              // Add the missing terms not present in the aggs list
              var missingTerms = [];

              terms.forEach(function (term) {
                if (keys.length === 0 || keys.indexOf(term.name) === -1) {
                  missingTerms.push({
                    count: 0,
                    default: 0,
                    description: LocalizedValues.forLocale(term.description, lang),
                    key: term.name,
                    title: LocalizedValues.forLocale(term.title, lang)
                  });
                }
              });

              return aggs.concat(missingTerms);
            }

            // The query didn't have any match, return default empty aggs based on the vocabulary terms
            return terms.map(function (term) {
              return {
                count: 0,
                default: 0,
                description: LocalizedValues.forLocale(term.description, lang),
                key: term.name,
                title: LocalizedValues.forLocale(term.title, lang)
              };
            });

          }

          return aggs;
        }

        function getChildAggragations(parentAgg, aggKey) {
          if (parentAgg.children) {
            var child = parentAgg.children.filter(function (child) {
              return child.hasOwnProperty(aggKey);
            }).pop();

            if (child) {
              return child[aggKey];
            }
          }

          return null;
        }

        var alias = RqlQueryUtils.vocabularyAlias(criterion.vocabulary);
        var targetResponse = joinQueryResponse[criterion.target + 'ResultDto'];

        if (targetResponse && targetResponse.aggs) {
          var isProperty = criterion.taxonomy.name.startsWith('Mica_');
          var filter = isProperty ? alias : criterion.taxonomy.name;
          var filteredAgg = targetResponse.aggs.filter(function (agg) {
            return agg.aggregation === filter;
          }).pop();

          if (filteredAgg) {
            if (isProperty) {
              if (RqlQueryUtils.isNumericVocabulary(criterion.vocabulary)) {
                return filteredAgg['obiba.mica.StatsAggregationResultDto.stats'];
              } else {
                return RqlQueryUtils.isRangeVocabulary(criterion.vocabulary) ?
                  addMissingTerms(filteredAgg['obiba.mica.RangeAggregationResultDto.ranges'], criterion.vocabulary) :
                  addMissingTerms(filteredAgg['obiba.mica.TermsAggregationResultDto.terms'], criterion.vocabulary);
              }
            } else {
              var vocabularyAgg = filteredAgg.children.filter(function (agg) {
                return agg.aggregation === alias;
              }).pop();

              if (vocabularyAgg) {
                return RqlQueryUtils.isRangeVocabulary(criterion.vocabulary) ?
                  addMissingTerms(getChildAggragations(filteredAgg, 'obiba.mica.RangeAggregationResultDto.ranges'), criterion.vocabulary) :
                  addMissingTerms(getChildAggragations(filteredAgg, 'obiba.mica.TermsAggregationResultDto.terms'), criterion.vocabulary);
              }
            }
          }
        }

        return addMissingTerms([], criterion.vocabulary);
      };

      this.findCriterion = function(criteria, id) {
        function inner(criteria, id) {
          var result;
          if(criteria.id === id) { return criteria; }
          for(var i = criteria.children.length; i--;){
            result = inner(criteria.children[i], id);

            if (result) {return result;}
          }
        }
        
        return inner(criteria, id);
      };
    }]);
;/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/* global BUCKET_TYPES */
/* global RQL_NODE */

/**
 * Module services and factories
 */
angular.module('obiba.mica.search')
  .factory('TaxonomiesSearchResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('TaxonomiesSearchResource'), {}, {
        'get': {
          method: 'GET',
          isArray: true,
          errorHandler: true
        }
      });
    }])

  .factory('TaxonomiesResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('TaxonomiesResource'), {}, {
        'get': {
          method: 'GET',
          isArray: true,
          errorHandler: true
        }
      });
    }])

  .factory('TaxonomyResource', ['$resource', 'ngObibaMicaUrl', '$cacheFactory',
    function ($resource, ngObibaMicaUrl, $cacheFactory) {
      return $resource(ngObibaMicaUrl.getUrl('TaxonomyResource'), {}, {
        'get': {
          method: 'GET',
          errorHandler: true,
          cache: $cacheFactory('taxonomyResource')
        }
      });
    }])

  .factory('JoinQuerySearchResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('JoinQuerySearchResource'), {}, {
        'variables': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'variables'}
        },
        'studies': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'studies'}
        },
        'networks': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'networks'}
        },
        'datasets': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'datasets'}
        }
      });
    }])

  .factory('JoinQueryCoverageResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('JoinQueryCoverageResource'), {}, {
        'get': {
          method: 'GET',
          errorHandler: true
        }
      });
    }])

  .factory('VocabularyResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('VocabularyResource'), {}, {
        'get': {
          method: 'GET',
          errorHandler: true
        }
      });
    }])

  .service('SearchContext', function() {
    var selectedLocale = null;

    this.setLocale = function(locale) {
      selectedLocale = locale;
    };

    this.currentLocale = function() {
      return selectedLocale;
    };
  })

  .service('PageUrlService', ['ngObibaMicaUrl', 'StringUtils', function(ngObibaMicaUrl, StringUtils) {

    this.studyPage = function(id) {
      return id ? StringUtils.replaceAll(ngObibaMicaUrl.getUrl('StudyPage'), {':study': id}) : '';
    };

    this.studyPopulationPage = function(id, populationId) {
      return id ? StringUtils.replaceAll(ngObibaMicaUrl.getUrl('StudyPopulationsPage'), {':study': id, ':population': populationId}) : '';
    };

    this.networkPage = function(id) {
      return id ? StringUtils.replaceAll(ngObibaMicaUrl.getUrl('NetworkPage'), {':network': id}) : '';
    };

    this.datasetPage = function(id, type) {
      var dsType = (type.toLowerCase() === 'study' ? 'study' : 'harmonization') + '-dataset';
      var result = id ? StringUtils.replaceAll(ngObibaMicaUrl.getUrl('DatasetPage'), {':type': dsType, ':dataset': id}) : '';
      return result;
    };

    this.variablePage = function(id) {
      return id ? StringUtils.replaceAll(ngObibaMicaUrl.getUrl('VariablePage'), {':variable': id}) : '';
    };

    this.downloadCoverage = function(query) {
      return StringUtils.replaceAll(ngObibaMicaUrl.getUrl('JoinQueryCoverageDownloadResource'), {':query': query});
    };

    return this;
  }])

  .service('ObibaSearchConfig', function () {
    var options = {
      networks: {
        showSearchTab:1
      },
      studies: {
        showSearchTab:1
      },
      datasets: {
        showSearchTab:1
      },
      variables: {
        showSearchTab:1
      }
    };

    this.setOptions = function (newOptions) {
      if (typeof(newOptions) === 'object') {
        Object.keys(newOptions).forEach(function (option) {
          if (option in options) {
            options[option] = newOptions[option];
          }
        });
      }
    };

    this.getOptions = function () {
      return angular.copy(options);
    };
  })

  .service('CoverageGroupByService', ['ngObibaMicaSearch', function(ngObibaMicaSearch) {
    var groupByOptions = ngObibaMicaSearch.getOptions().coverage.groupBy;
    return {
      canShowStudy: function() {
        return groupByOptions.study || groupByOptions.dce;
      },

      canShowDce: function(bucket) {
        return (bucket === BUCKET_TYPES.STUDY || bucket === BUCKET_TYPES.DCE) &&
          groupByOptions.study && groupByOptions.dce;
      },

      canShowDataset: function() {
        return groupByOptions.dataset || groupByOptions.dataschema;
      },

      canShowDatasetStudyDataschema: function(bucket) {
        return (bucket=== BUCKET_TYPES.DATASET || bucket === BUCKET_TYPES.DATASCHEMA) &&
          groupByOptions.dataset && groupByOptions.dataschema;
      },

      canShowNetwork: function() {
        return groupByOptions.network;
      },

      studyTitle: function() {
        return groupByOptions.study ? 'search.coverage-buckets.study' : (groupByOptions.dce ? 'search.coverage-buckets.dce' : '');
      },

      studyBucket: function() {
        return groupByOptions.study ? BUCKET_TYPES.STUDY : BUCKET_TYPES.DCE;
      },

      datasetTitle: function() {
        return groupByOptions.dataset && groupByOptions.dataschema ?
          'search.coverage-buckets.datasetNav' :
          (groupByOptions.dataset ?
            'search.coverage-buckets.dataset' :
            (groupByOptions.dataschema ? 'search.coverage-buckets.dataschema' : ''));
      },

      datasetBucket: function() {
        return groupByOptions.dataset ? BUCKET_TYPES.DATASET : BUCKET_TYPES.DATASCHEMA;
      },

      canGroupBy: function(bucket) {
        return groupByOptions.hasOwnProperty(bucket) && groupByOptions[bucket];
      },

      defaultBucket: function() {
        return groupByOptions.study ? BUCKET_TYPES.STUDY :
          (groupByOptions.dce ? BUCKET_TYPES.DCE : groupByOptions.dataset ? BUCKET_TYPES.DATASET :
            (groupByOptions.dataschema ? BUCKET_TYPES.DATASCHEMA :
              (groupByOptions.network ? BUCKET_TYPES.NETWORK : '')));
      }

    };

  }])

  .factory('CriteriaNodeCompileService', ['$templateCache', '$compile', function($templateCache, $compile){

    return {
      compile: function(scope, element) {
        var template = '';
        if (scope.item.type === RQL_NODE.OR || scope.item.type === RQL_NODE.AND || scope.item.type === RQL_NODE.NAND || scope.item.type === RQL_NODE.NOR) {
          template = angular.element($templateCache.get('search/views/criteria/criteria-node-template.html'));
        } else {
          template = angular.element('<criterion-dropdown criterion="item" query="query"></criterion-dropdown>');
        }

        $compile(template)(scope, function(cloned){
          element.replaceWith(cloned);
        });
      }
    };

  }]);

;/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/* global CRITERIA_ITEM_EVENT */
/* global QUERY_TARGETS */
/* global QUERY_TYPES */
/* global BUCKET_TYPES */
/* global RQL_NODE */
/* global DISPLAY_TYPES */
/* global CriteriaIdGenerator */
/* global targetToType */
/* global SORT_FIELDS */

/**
 * State shared between Criterion DropDown and its content directives
 *
 * @constructor
 */
function CriterionState() {
  var onOpenCallbacks = [];
  var onCloseCallbacks = [];

  this.dirty = false;
  this.open = false;
  this.loading = true;

  this.addOnOpen = function (callback) {
    onOpenCallbacks.push(callback);
  };

  this.addOnClose = function (callback) {
    onCloseCallbacks.push(callback);
  };

  this.onOpen = function () {
    onOpenCallbacks.forEach(function (callback) {
      callback();
    });
  };

  this.onClose = function () {
    onCloseCallbacks.forEach(function (callback) {
      callback();
    });
  };
}

/**
 * Base controller for taxonomies and classification panels.
 *
 * @param $scope
 * @param $location
 * @param TaxonomyResource
 * @param TaxonomiesResource
 * @param ngObibaMicaSearch
 * @constructor
 */
function BaseTaxonomiesController($scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils) {
  $scope.options = ngObibaMicaSearch.getOptions();
  $scope.RqlQueryUtils = RqlQueryUtils;
  $scope.metaTaxonomy = TaxonomyResource.get({
    target: 'taxonomy',
    taxonomy: 'Mica_taxonomy'
  });

  $scope.taxonomies = {
    all: [],
    search: {
      text: null,
      active: false
    },
    target: $scope.target || 'variable',
    taxonomy: null,
    vocabulary: null
  };

  // vocabulary (or term) will appear in navigation iff it doesn't have the 'showNavigate' attribute
  $scope.canNavigate = function(vocabulary) {
    if ($scope.options.hideNavigate.indexOf(vocabulary.name) > -1) {
      return false;
    }

    return (vocabulary.attributes || []).filter(function (attr) { return attr.key === 'showNavigate'; }).length === 0;
  };

  this.navigateTaxonomy = function (taxonomy, vocabulary, term) {
    $scope.taxonomies.term = term;

    if ($scope.isHistoryEnabled) {
      var search = $location.search();
      search.taxonomy = taxonomy ? taxonomy.name : null;
      search.vocabulary = vocabulary ? vocabulary.name : null;
      $location.search(search);
    } else {
      $scope.taxonomies.taxonomy = taxonomy;
      $scope.taxonomies.vocabulary = vocabulary;
    }
  };

  this.updateStateFromLocation = function () {
    var search = $location.search();
    var taxonomyName = search.taxonomy,
      vocabularyName = search.vocabulary, taxonomy = null, vocabulary = null;

    if (!$scope.taxonomies.all) { //page loading
      return;
    }

    $scope.taxonomies.all.forEach(function (t) {
      if (t.name === taxonomyName) {
        taxonomy = t;
        t.vocabularies.forEach(function (v) {
          if (v.name === vocabularyName) {
            vocabulary = v;
          }
        });
      }
    });

    if (!angular.equals($scope.taxonomies.taxonomy, taxonomy) || !angular.equals($scope.taxonomies.vocabulary, vocabulary)) {
      $scope.taxonomies.taxonomy = taxonomy;
      $scope.taxonomies.vocabulary = vocabulary;
    }
  };

  this.selectTerm = function (target, taxonomy, vocabulary, args) {
    $scope.onSelectTerm(target, taxonomy, vocabulary, args);
  };

  var self = this;

  $scope.$on('$locationChangeSuccess', function () {
    if ($scope.isHistoryEnabled) {
      self.updateStateFromLocation();
    }
  });
  
  $scope.$watch('taxonomies.vocabulary', function(value) {
    if(RqlQueryUtils && value) {
      $scope.taxonomies.isNumericVocabulary = RqlQueryUtils.isNumericVocabulary($scope.taxonomies.vocabulary);
      $scope.taxonomies.isMatchVocabulary = RqlQueryUtils.isMatchVocabulary($scope.taxonomies.vocabulary);
    } else {
      $scope.taxonomies.isNumericVocabulary = null;
      $scope.taxonomies.isMatchVocabulary = null;
    }
  });

  $scope.navigateTaxonomy = this.navigateTaxonomy;
  $scope.selectTerm = this.selectTerm;
}
/**
 * TaxonomiesPanelController
 *
 * @param $scope
 * @param $location
 * @param TaxonomyResource
 * @param TaxonomiesResource
 * @param ngObibaMicaSearch
 * @constructor
 */
function TaxonomiesPanelController($scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils) {
  BaseTaxonomiesController.call(this, $scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils);
  $scope.$watchGroup(['taxonomyName', 'target'], function (newVal) {
    if (newVal[0] && newVal[1]) {
      if ($scope.showTaxonomies) {
        $scope.showTaxonomies();
      }
      $scope.taxonomies.target = newVal[1];
      $scope.taxonomies.search.active = true;
      $scope.taxonomies.all = null;
      $scope.taxonomies.taxonomy = null;
      $scope.taxonomies.vocabulary = null;
      $scope.taxonomies.term = null;
      TaxonomyResource.get({
        target: newVal[1],
        taxonomy: newVal[0]
      }, function onSuccess(response) {
        $scope.taxonomies.taxonomy = response;
        $scope.taxonomies.search.active = false;
      });
    }
  });

}
/**
 * ClassificationPanelController
 * 
 * @param $scope
 * @param $location
 * @param TaxonomyResource
 * @param TaxonomiesResource
 * @param ngObibaMicaSearch
 * @constructor
 */
function ClassificationPanelController($scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils) {
  BaseTaxonomiesController.call(this, $scope, $location, TaxonomyResource, TaxonomiesResource, ngObibaMicaSearch, RqlQueryUtils);
  var groupTaxonomies = function (taxonomies, target) {
    var res = taxonomies.reduce(function (res, t) {
      res[t.name] = t;
      return res;
    }, {});

    return $scope.metaTaxonomy.$promise.then(function (metaTaxonomy) {
      var targetVocabulary = metaTaxonomy.vocabularies.filter(function (v) {
        return v.name === target;
      })[0];

      $scope.taxonomyGroups = targetVocabulary.terms.map(function (v) {
        if (!v.terms) {
          var taxonomy = res[v.name];

          if (!taxonomy) {
            return null;
          }

          taxonomy.title = v.title;
          taxonomy.description = v.description;
          return {title: null, taxonomies: [taxonomy]};
        }

        var taxonomies = v.terms.map(function (t) {
          var taxonomy = res[t.name];

          if (!taxonomy) {
            return null;
          }

          taxonomy.title = t.title;
          taxonomy.description = t.description;
          return taxonomy;
        }).filter(function (t) {
          return t;
        });
        var title = v.title.filter(function (t) {
          return t.locale === $scope.lang;
        })[0];
        var description = v.description ? v.description.filter(function (t) {
          return t.locale === $scope.lang;
        })[0] : undefined;

        return {
          title: title ? title.text : null,
          description: description ? description.text : null,
          taxonomies: taxonomies
        };
      }).filter(function (t) {
        return t;
      });
    });
  };

  var self = this;
  $scope.$watch('target', function (newVal) {
    if (newVal) {
      $scope.taxonomies.target = newVal;
      $scope.taxonomies.search.active = true;
      $scope.taxonomies.all = null;
      $scope.taxonomies.taxonomy = null;
      $scope.taxonomies.vocabulary = null;
      $scope.taxonomies.term = null;

      TaxonomiesResource.get({
        target: $scope.taxonomies.target
      }, function onSuccess(taxonomies) {
        $scope.taxonomies.all = taxonomies;
        groupTaxonomies(taxonomies, $scope.taxonomies.target);
        $scope.taxonomies.search.active = false;
        self.updateStateFromLocation();
      });
    }
  });
}

angular.module('obiba.mica.search')

  .controller('SearchController', [
    '$scope',
    '$rootScope',
    '$timeout',
    '$routeParams',
    '$location',
    '$translate',
    '$cookies',
    'TaxonomiesSearchResource',
    'TaxonomiesResource',
    'TaxonomyResource',
    'VocabularyResource',
    'ngObibaMicaSearchTemplateUrl',
    'ngObibaMicaSearch',
    'JoinQuerySearchResource',
    'JoinQueryCoverageResource',
    'AlertService',
    'ServerErrorUtils',
    'LocalizedValues',
    'RqlQueryService',
    'RqlQueryUtils',
    'SearchContext',
    'CoverageGroupByService',
    function ($scope,
              $rootScope,
              $timeout,
              $routeParams,
              $location,
              $translate,
              $cookies,
              TaxonomiesSearchResource,
              TaxonomiesResource,
              TaxonomyResource,
              VocabularyResource,
              ngObibaMicaSearchTemplateUrl,
              ngObibaMicaSearch,
              JoinQuerySearchResource,
              JoinQueryCoverageResource,
              AlertService,
              ServerErrorUtils,
              LocalizedValues,
              RqlQueryService,
              RqlQueryUtils,
              SearchContext,
              CoverageGroupByService) {

      $scope.options = ngObibaMicaSearch.getOptions();
      var cookiesSearchHelp = 'micaHideSearchHelpText';
      var cookiesClassificationHelp = 'micaHideClassificationHelpBox';

      $translate(['search.help', 'search.coverage-help'])
        .then(function (translation) {
          if(!$scope.options.SearchHelpText && !$cookies.get(cookiesSearchHelp)){
            $scope.options.SearchHelpText = translation['search.help'];
          }
          if(!$scope.options.ClassificationHelpText && !$cookies.get(cookiesClassificationHelp)){
            $scope.options.ClassificationHelpText = translation['classifications.help'];
          }
        });
      // Close the Help search box and set the local cookies
      $scope.closeHelpBox = function () {
        $cookies.put(cookiesSearchHelp, true);
        $scope.options.SearchHelpText = null;
      };

      // Close the Help classification box and set the local cookies
      $scope.closeClassificationHelpBox = function () {
        $cookies.put(cookiesClassificationHelp, true);
        $scope.options.ClassificationHelpText = null;
      };

      // Retrieve from local cookies if user has disabled the Help Search Box and hide the box if true
      if ($cookies.get(cookiesSearchHelp)) {
        $scope.options.SearchHelpText = null;
      }
      // Retrieve from local cookies if user has disabled the Help Classification Box and hide the box if true
      if ($cookies.get(cookiesClassificationHelp)) {
        $scope.options.ClassificationHelpText = null;
      }

      $scope.taxonomyTypeMap = { //backwards compatibility for pluralized naming in configs.
        variable: 'variables',
        study: 'studies',
        network: 'networks',
        dataset: 'datasets'
      };

      $translate(['search.classifications-title', 'search.classifications-link', 'search.faceted-navigation-help'])
        .then(function (translation) {
          $scope.hasClassificationsTitle = translation['search.classifications-title'];
          $scope.hasClassificationsLinkLabel = translation['search.classifications-link'];
          $scope.hasFacetedNavigationHelp = translation['search.faceted-navigation-help'];
        });
      
      var taxonomyTypeInverseMap = Object.keys($scope.taxonomyTypeMap).reduce(function (prev, k) {
        prev[$scope.taxonomyTypeMap[k]] = k;
        return prev;
      }, {});
      $scope.targets = [];
      $scope.lang = LocalizedValues.getLocal();
      $scope.metaTaxonomy = TaxonomyResource.get({
        target: 'taxonomy',
        taxonomy: 'Mica_taxonomy'
      }, function (t) {
        $scope.targets = t.vocabularies.map(function (v) {
          return v.name;
        });
        
        function flattenTaxonomies(terms){
          function inner(acc, terms) {
            angular.forEach(terms, function(t) {
              if(!t.terms) {
                acc.push(t);
                return;
              }

              inner(acc, t.terms);
            });

            return acc;
          }

          return inner([], terms);
        }

        $scope.hasFacetedTaxonomies = false;

        $scope.facetedTaxonomies = t.vocabularies.reduce(function(res, target) {
          var taxonomies = flattenTaxonomies(target.terms);
          
          function getTaxonomy(taxonomyName) {
            return taxonomies.filter(function(t) {
              return t.name === taxonomyName;
            })[0];
          }

          function notNull(t) {
            return t !== null && t !== undefined;
          }

          if($scope.options.showAllFacetedTaxonomies) {
            res[target.name] = taxonomies.filter(function(t) {
              return t.attributes && t.attributes.some(function(att) {
                  return att.key === 'showFacetedNavigation' &&  att.value.toString() === 'true';
                });
            });
          } else {
            res[target.name] = ($scope.options[target.name + 'TaxonomiesOrder'] || []).map(getTaxonomy).filter(notNull);
          }
          
          $scope.hasFacetedTaxonomies = $scope.hasFacetedTaxonomies || res[target.name].length;
          
          return res;
        }, {});
      });

      var searchTaxonomyDisplay = {
        variable: $scope.options.variables.showSearchTab,
        dataset: $scope.options.datasets.showSearchTab,
        study: $scope.options.studies.showSearchTab,
        network: $scope.options.networks.showSearchTab
      };

      function initSearchTabs() {
        $scope.taxonomyNav = [];

        function getTabsOrderParam(arg) {
          var value = $location.search()[arg];

          return value && value.split(',')
              .filter(function (t) {
                return t;
              })
              .map(function (t) {
                return t.trim();
              });
        }

        var targetTabsOrderParam = getTabsOrderParam('targetTabsOrder');
        $scope.targetTabsOrder = (targetTabsOrderParam || $scope.options.targetTabsOrder).filter(function (t) {
          return searchTaxonomyDisplay[t];
        });

        var searchTabsOrderParam = getTabsOrderParam('searchTabsOrder');
        $scope.searchTabsOrder = searchTabsOrderParam || $scope.options.searchTabsOrder;

        var resultTabsOrderParam = getTabsOrderParam('resultTabsOrder');
        $scope.resultTabsOrder = (resultTabsOrderParam || $scope.options.resultTabsOrder).filter(function (t) {
          return searchTaxonomyDisplay[t];
        });

        if($location.search().target) {
          $scope.target = $location.search().target;
        } else if (!$scope.target) {
          $scope.target = $scope.targetTabsOrder[0];
        }

        $scope.metaTaxonomy.$promise.then(function (metaTaxonomy) {
          $scope.targetTabsOrder.forEach(function (target) {
            var targetVocabulary = metaTaxonomy.vocabularies.filter(function (vocabulary) {
              return vocabulary.name === target;
            }).pop();
            if (targetVocabulary && targetVocabulary.terms) {
              targetVocabulary.terms.forEach(function (term) {
                term.target = target;
                var title = term.title.filter(function (t) {
                  return t.locale === $scope.lang;
                })[0];
                var description = term.description ? term.description.filter(function (t) {
                  return t.locale === $scope.lang;
                })[0] : undefined;
                term.locale = {
                  title: title,
                  description: description
                };
                if (term.terms) {
                  term.terms.forEach(function (trm) {
                    var title = trm.title.filter(function (t) {
                      return t.locale === $scope.lang;
                    })[0];
                    var description = trm.description ? trm.description.filter(function (t) {
                      return t.locale === $scope.lang;
                    })[0] : undefined;
                    trm.locale = {
                      title: title,
                      description: description
                    };
                  });
                }
                $scope.taxonomyNav.push(term);
              });
            }
          });
        });

      }

      function onError(response) {
        $scope.search.result = {};
        AlertService.alert({
          id: 'SearchController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response),
          delay: 5000
        });
      }

      function validateType(type) {
        if (!type || !QUERY_TYPES[type.toUpperCase()]) {
          throw new Error('Invalid type: ' + type);
        }
      }

      function validateBucket(bucket) {
        if (bucket && !BUCKET_TYPES[bucket.toUpperCase()]) {
          throw new Error('Invalid bucket: ' + bucket);
        }
      }

      function validateDisplay(display) {
        if (!display || !DISPLAY_TYPES[display.toUpperCase()]) {
          throw new Error('Invalid display: ' + display);
        }
      }

      function getDefaultQueryType() {
        return $scope.taxonomyTypeMap[$scope.resultTabsOrder[0]];
      }

      function getDefaultDisplayType() {
        return $scope.searchTabsOrder[0] || DISPLAY_TYPES.LIST;
      }

      function validateQueryData() {
        try {
          var search = $location.search();
          var type = $scope.resultTabsOrder.indexOf(taxonomyTypeInverseMap[search.type]) > -1 ? search.type : getDefaultQueryType();
          var bucket = search.bucket && CoverageGroupByService.canGroupBy(search.bucket) ? search.bucket : CoverageGroupByService.defaultBucket();
          var display = $scope.searchTabsOrder.indexOf(search.display) > -1 ? search.display : getDefaultDisplayType();
          var query = search.query || '';
          validateType(type);
          validateBucket(bucket);
          validateDisplay(display);
          $scope.search.type = type;
          $scope.search.bucket = bucket;
          $scope.search.display = display;
          $scope.search.query = query;
          $scope.search.rqlQuery = new RqlParser().parse(query);

          return true;
        } catch (e) {
          AlertService.alert({
            id: 'SearchController',
            type: 'danger',
            msg: e.message,
            delay: 5000
          });
        }

        return false;
      }

      function quoteQuery(query) {
        query = query.trim();

        if (query.match(/\s+/)) {
          return '"'+query.replace(/^"|"$/g, '').replace(/"/, '\"')+'"';
        }

        return query;
      }

      var clearSearchQuery = function () {
        var search = $location.search();
        delete search.query;
        $location.search(search);
      };

      var toggleSearchQuery = function () {
        $scope.search.advanced = !$scope.search.advanced;
      };

      var showAdvanced = function() {
        var children = $scope.search.criteria.children || [];
        for(var i = children.length; i--;) {
          var vocabularyChildren = children[i].children || [];
          for (var j = vocabularyChildren.length; j--;) {
            if (vocabularyChildren[j].type === RQL_NODE.OR || vocabularyChildren[j].type === RQL_NODE.AND) {
              return true;
            }
          }
        }
      };
      
      function sortCriteriaItems(items) {
        items.sort(function (a, b) {
          if (a.target === 'network' || b.target === 'variable') {
            return -1;
          }
          if (a.target === 'variable' || b.target === 'network') {
            return 1;
          }
          if (a.target < b.target) {
            return 1;
          }
          if (a.target > b.target) {
            return -1;
          }
          // a must be equal to b
          return 0;
        });
      }

      function loadResults() {
        // execute search only when results are to be shown
        if ($location.path() !== '/search') {
          return;
        }
        var localizedQuery =
          RqlQueryService.prepareSearchQuery(
            $scope.search.type,
            $scope.search.rqlQuery,
            $scope.search.pagination,
            $scope.lang,
            $scope.search.type === QUERY_TYPES.VARIABLES ? SORT_FIELDS.NAME : SORT_FIELDS.ACRONYM
          );
        switch ($scope.search.display) {
          case DISPLAY_TYPES.LIST:
            $scope.search.loading = true;
            $scope.search.executedQuery = localizedQuery;
            JoinQuerySearchResource[$scope.search.type]({query: localizedQuery},
              function onSuccess(response) {
                $scope.search.result.list = response;
                $scope.search.loading = false;
              },
              onError);
            break;
          case DISPLAY_TYPES.COVERAGE:
            var hasVariableCriteria = Object.keys($scope.search.criteriaItemMap).map(function (k) {
                return $scope.search.criteriaItemMap[k];
              }).filter(function (item) {
                return QUERY_TARGETS.VARIABLE  === item.getTarget() && item.taxonomy.name !== 'Mica_variable';
              }).length > 0;

            if (hasVariableCriteria) {
              $scope.search.loading = true;
              $scope.search.executedQuery = RqlQueryService.prepareCoverageQuery(localizedQuery, $scope.search.bucket);
              JoinQueryCoverageResource.get({query: $scope.search.executedQuery},
                function onSuccess(response) {
                  $scope.search.result.coverage = response;
                  $scope.search.loading = false;
                },
                onError);
            } else {
              $scope.search.result = {};
            }

            break;
          case DISPLAY_TYPES.GRAPHICS:
            $scope.search.loading = true;
            $scope.search.executedQuery = RqlQueryService.prepareGraphicsQuery(localizedQuery,
              ['Mica_study.populations-selectionCriteria-countriesIso', 'Mica_study.populations-dataCollectionEvents-bioSamples', 'Mica_study.numberOfParticipants-participant-number'],
              ['Mica_study.methods-designs']);
            JoinQuerySearchResource.studies({query: $scope.search.executedQuery},
              function onSuccess(response) {
                $scope.search.result.graphics = response;
                $scope.search.loading = false;
              },
              onError);
            break;
        }

      }

      function executeSearchQuery() {
        if (validateQueryData()) {
          // build the criteria UI
          RqlQueryService.createCriteria($scope.search.rqlQuery, $scope.lang).then(function (result) {
            // criteria UI is updated here
            $scope.search.criteria = result.root;

            if ($scope.search.criteria && $scope.search.criteria.children) {
              sortCriteriaItems($scope.search.criteria.children);
            }

            $scope.search.criteriaItemMap = result.map;

            if ($scope.search.query) {
              loadResults();
            }

            $scope.$broadcast('ngObibaMicaQueryUpdated', $scope.search.criteria);
          });
        }
      }

      $scope.setLocale = function (locale) {
        $scope.lang = locale;
        SearchContext.setLocale($scope.lang);
        executeSearchQuery();
      };

      var showTaxonomy = function (target, name) {
        if ($scope.target === target && $scope.taxonomyName === name && $scope.taxonomiesShown) {
          $scope.taxonomiesShown = false;
          return;
        }

        $scope.taxonomiesShown = true;
        $scope.target = target;
        $scope.taxonomyName = name;
      };

      var clearTaxonomy = function () {
        $scope.target = null;
        $scope.taxonomyName = null;
      };

      /**
       * Updates the URL location triggering a query execution
       */
      var refreshQuery = function () {
        var query = new RqlQuery().serializeArgs($scope.search.rqlQuery.args);
        var search = $location.search();
        if ('' === query) {
          delete search.query;
        } else {
          search.query = query;
        }
        $location.search(search);
      };

      var clearSearch = function () {
        $scope.documents.search.text = null;
        $scope.documents.search.active = false;
      };

      /**
       * Searches the criteria matching the input query
       *
       * @param query
       * @returns {*}
       */
      var searchCriteria = function (query) {
        // search for taxonomy terms
        // search for matching variables/studies/... count

        function score(item) {
          var result = 0;
          var regExp = new RegExp(query, 'ig');

          if (item.itemTitle.match(regExp)) {
            result = 10;
          } else if (item.itemDescription && item.itemDescription.match(regExp)) {
            result = 8;
          } else if (item.itemParentTitle.match(regExp)) {
            result = 6;
          } else if (item.itemParentDescription && item.itemParentDescription.match(regExp)) {
            result = 4;
          }

          return result;
        }

        // vocabulary (or term) can be used in search if it doesn't have the 'showSearch' attribute
        function canSearch(taxonomyEntity, hideSearchList) {
          if ((hideSearchList || []).indexOf(taxonomyEntity.name) > -1) {
            return false;
          }

          return (taxonomyEntity.attributes || []).filter(function (attr) { return attr.key === 'showSearch'; }).length === 0;
        }

        function processBundle(bundle) {
          var results = [];
          var total = 0;
          var target = bundle.target;
          var taxonomy = bundle.taxonomy;
          if (taxonomy.vocabularies) {
            taxonomy.vocabularies.filter(function (vocabulary) {
              return canSearch(vocabulary, $scope.options.hideSearch);
            }).forEach(function (vocabulary) {
              if (vocabulary.terms) {
                vocabulary.terms.filter(function (term) {
                  return canSearch(term, $scope.options.hideSearch);
                }).forEach(function (term) {
                  var item = RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, term, $scope.lang);
                  results.push({
                    score: score(item),
                    item: item
                  });
                  total++;
                });
              } else {
                var item = RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, null, $scope.lang);
                results.push({
                  score: score(item),
                  item: item
                });
                total++;
              }
            });
          }
          return {results: results, total: total};
        }

        var criteria = TaxonomiesSearchResource.get({
          query: quoteQuery(query), locale: $scope.lang, target: $scope.documents.search.target
        }).$promise.then(function (response) {
          if (response) {
            var results = [];
            var total = 0;
            var size = 10;

            response.forEach(function (bundle) {
              var rval = processBundle(bundle);
              results.push.apply(results, rval.results);
              total = total + rval.total;
            });

            results.sort(function (a, b) {
              return b.score - a.score;
            });

            results = results.splice(0, size);

            if (total > results.length) {
              var note = {
                query: query,
                total: total,
                size: size,
                message: 'Showing ' + size + ' / ' + total,
                status: 'has-warning'
              };
              results.push({score: -1, item: note});
            }

            return results.map(function (result) {
              return result.item;
            });
          } else {
            return [];
          }
        });

        return criteria;
      };

      /**
       * Removes the item from the criteria tree
       * @param item
       */
      var removeCriteriaItem = function (item) {
        RqlQueryService.removeCriteriaItem(item);
        refreshQuery();
      };

      /**
       * Propagates a Scope change that results in criteria panel update
       * @param item
       */
      var selectCriteria = function (item, logicalOp, replace, showNotification) {
        if (angular.isUndefined(showNotification)) {
          showNotification = true;
        }
        
        if (item.id) {
          var id = CriteriaIdGenerator.generate(item.taxonomy, item.vocabulary);
          var existingItem = $scope.search.criteriaItemMap[id];
          var growlMsgKey;

          if (existingItem && id.indexOf('dceIds') !== -1) {
            removeCriteriaItem(existingItem);
            growlMsgKey = 'search.criterion.updated';
            RqlQueryService.addCriteriaItem($scope.search.rqlQuery, item, logicalOp);
          } else if (existingItem) {
            growlMsgKey = 'search.criterion.updated';
            RqlQueryService.updateCriteriaItem(existingItem, item, replace);
          } else {
            growlMsgKey = 'search.criterion.created';
            RqlQueryService.addCriteriaItem($scope.search.rqlQuery, item, logicalOp);
          }

          if (showNotification) {
            AlertService.growl({
              id: 'SearchControllerGrowl',
              type: 'info',
              msgKey: growlMsgKey,
              msgArgs: [LocalizedValues.forLocale(item.vocabulary.title, $scope.lang)],
              delay: 3000
            });
          }

          refreshQuery();
          $scope.selectedCriteria = null;
        } else {
          $scope.selectedCriteria = item.query;
        }
      };

      var searchKeyUp = function (event) {
        switch (event.keyCode) {
          case 27: // ESC
            if ($scope.documents.search.active) {
              clearSearch();
            }
            break;

          default:
            if ($scope.documents.search.text) {
              searchCriteria($scope.documents.search.text);
            }
            break;
        }
      };

      var onTypeChanged = function (type) {
        if (type) {
          validateType(type);
          var search = $location.search();
          search.type = type;
          $location.search(search);
        }
      };

      var onBucketChanged = function (bucket) {
        if (bucket) {
          validateBucket(bucket);
          var search = $location.search();
          search.bucket = bucket;
          $location.search(search);
        }
      };

      var onPaginate = function (target, from, size) {
        $scope.search.pagination[target] = {from: from, size: size};
        executeSearchQuery();
      };

      var onDisplayChanged = function (display) {
        if (display) {
          validateDisplay(display);
          var search = $location.search();
          search.display = display;
          $location.search(search);
        }
      };

      function reduce(criteriaItem) {
        var parentItem = criteriaItem.parent;
        if (parentItem.type === RQL_NODE.OR) {
          var grandParentItem = parentItem.parent;
          var parentItemIndex = grandParentItem.children.indexOf(parentItem);
          grandParentItem.children[parentItemIndex] = criteriaItem;

          var parentRql = parentItem.rqlQuery;
          var grandParentRql = grandParentItem.rqlQuery;
          var parentRqlIndex = grandParentRql.args.indexOf(parentRql);
          grandParentRql.args[parentRqlIndex] = criteriaItem.rqlQuery;

          if (grandParentItem.type !== QUERY_TARGETS.VARIABLE) {
            reduce(grandParentItem);
          }
        }
      }

      var onUpdateCriteria = function (item, type, useCurrentDisplay, replaceTarget, showNotification) {
        if (type) {
          onTypeChanged(type);
        }

        if (replaceTarget) {
          var criteriaItem = criteriaItemFromMap(item);
          if (criteriaItem) {
            reduce(criteriaItem);
          }
        }

        onDisplayChanged(useCurrentDisplay && $scope.search.display ? $scope.search.display : DISPLAY_TYPES.LIST);
        selectCriteria(item, RQL_NODE.AND, true, showNotification);
      };

      function criteriaItemFromMap(item) {
        var key = Object.keys($scope.search.criteriaItemMap).filter(function (k) {
          return item.id.indexOf(k) !== -1;
        })[0];
        return $scope.search.criteriaItemMap[key];
      }

      var onRemoveCriteria = function(item) {
        var found = RqlQueryService.findCriterion($scope.search.criteria, item.id); 
        removeCriteriaItem(found);
      };

      var onSelectTerm = function (target, taxonomy, vocabulary, args) {
        args = args || {};
        
        if(angular.isString(args)) {
          args = {term: args};
        }
        
        if (vocabulary) {
          var item;
          if (RqlQueryUtils.isNumericVocabulary(vocabulary)) {
            item = RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, null, $scope.lang);
            item.rqlQuery = RqlQueryUtils.buildRqlQuery(item);
            RqlQueryUtils.updateRangeQuery(item.rqlQuery, args.from, args.to);
            selectCriteria(item, null, true);

            return;
          } else if(RqlQueryUtils.isMatchVocabulary(vocabulary)) {
            item = RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, null, $scope.lang);
            item.rqlQuery = RqlQueryUtils.buildRqlQuery(item);
            RqlQueryUtils.updateMatchQuery(item.rqlQuery, args.text);
            selectCriteria(item, null, true);

            return;
          }
        }

        selectCriteria(RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, args && args.term, $scope.lang));
      };

      var selectSearchTarget = function (target) {
        $scope.documents.search.target = target;
      };

      var VIEW_MODES = {
        SEARCH: 'search',
        CLASSIFICATION: 'classification'
      };

      $scope.goToSearch = function () {
        $scope.viewMode = VIEW_MODES.SEARCH;
        $location.search('taxonomy', null);
        $location.search('vocabulary', null);
        $location.search('target', null);
        $location.path('/search');
      };

      $scope.goToClassifications = function () {
        $scope.viewMode = VIEW_MODES.CLASSIFICATION;
        $location.path('/classifications');
        $location.search('target', $scope.targetTabsOrder[0]);
      };

      $scope.navigateToTarget = function(target) {
        $location.search('target', target);
        $location.search('taxonomy', null);
        $location.search('vocabulary', null);
        $scope.target = target;
      };

      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.BUCKET_TYPES = BUCKET_TYPES;

      $scope.search = {
        pagination: {},
        query: null,
        advanced: false,
        rqlQuery: new RqlQuery(),
        executedQuery: null,
        type: null,
        bucket: null,
        result: {
          list: null,
          coverage: null,
          graphics: null
        },
        criteria: [],
        criteriaItemMap: {},
        loading: false
      };

      $scope.viewMode = VIEW_MODES.SEARCH;
      $scope.documents = {
        search: {
          text: null,
          active: false,
          target: null
        }
      };

      $scope.searchHeaderTemplateUrl = ngObibaMicaSearchTemplateUrl.getHeaderUrl('search');
      $scope.classificationsHeaderTemplateUrl = ngObibaMicaSearchTemplateUrl.getHeaderUrl('classifications');
      $scope.selectSearchTarget = selectSearchTarget;
      $scope.selectDisplay = onDisplayChanged;
      $scope.searchCriteria = searchCriteria;
      $scope.selectCriteria = selectCriteria;
      $scope.searchKeyUp = searchKeyUp;

      $scope.showTaxonomy = showTaxonomy;
      $scope.clearTaxonomy = clearTaxonomy;

      $scope.removeCriteriaItem = removeCriteriaItem;
      $scope.refreshQuery = refreshQuery;
      $scope.clearSearchQuery = clearSearchQuery;
      $scope.toggleSearchQuery = toggleSearchQuery;
      $scope.showAdvanced = showAdvanced;

      $scope.onTypeChanged = onTypeChanged;
      $scope.onBucketChanged = onBucketChanged;
      $scope.onDisplayChanged = onDisplayChanged;
      $scope.onUpdateCriteria = onUpdateCriteria;
      $scope.onRemoveCriteria = onRemoveCriteria;
      $scope.onSelectTerm = onSelectTerm;
      $scope.QUERY_TARGETS = QUERY_TARGETS;
      $scope.onPaginate = onPaginate;
      $scope.inSearchMode = function() {
        return $scope.viewMode === VIEW_MODES.SEARCH;
      };
      $scope.toggleFullscreen = function() {
        $scope.isFullscreen = !$scope.isFullscreen;
      };

      $scope.$on('$locationChangeSuccess', function (newLocation, oldLocation) {
        initSearchTabs();

        if (newLocation !== oldLocation) {
          executeSearchQuery();
        }
      });

      $rootScope.$on('ngObibaMicaSearch.fullscreenChange', function(obj, isEnabled) {
        $scope.isFullscreen = isEnabled;
      });

      function init() {
        ngObibaMicaSearch.getLocale(function (locales) {
          if (angular.isArray(locales)) {
            $scope.tabs = locales;
            $scope.lang = locales[0];
          } else {
            $scope.lang = locales || $scope.lang;
          }

          SearchContext.setLocale($scope.lang);
          initSearchTabs();
          executeSearchQuery();
        });
      }

      init();
    }])

  .controller('NumericVocabularyPanelController', ['$scope', function($scope) {
    $scope.$watch('taxonomies', function() {
      $scope.from = null;
      $scope.to = null;
    }, true);
  }])
  
  .controller('MatchVocabularyPanelController', ['$scope', function($scope) {
    $scope.$watch('taxonomies', function() {
      $scope.text = null;
    }, true);
  }])
  
  .controller('NumericVocabularyFacetController', ['$scope','JoinQuerySearchResource', 'RqlQueryService',
    'RqlQueryUtils', function($scope, JoinQuerySearchResource, RqlQueryService, RqlQueryUtils) {
    function updateLimits (criteria, vocabulary) {
      function createExistsQuery(criteria, criterion) {
        var rootQuery = angular.copy(criteria.rqlQuery);
        criterion.rqlQuery = RqlQueryUtils.buildRqlQuery(criterion);
        RqlQueryService.addCriteriaItem(rootQuery, criterion);
        return rootQuery;
      }

      var criterion = RqlQueryService.findCriterion(criteria, CriteriaIdGenerator.generate($scope.$parent.taxonomy, vocabulary));

      if(!criterion) {
        criterion = RqlQueryService.createCriteriaItem($scope.target, $scope.$parent.taxonomy, $scope.vocabulary);
      }

      if(criterion.rqlQuery && criterion.rqlQuery.args[1]) {
        if(angular.isArray(criterion.rqlQuery.args[1])) {
          $scope.from = criterion.rqlQuery.args[1][0];
          $scope.to = criterion.rqlQuery.args[1][1];
        } else {
          if(criterion.rqlQuery.name === RQL_NODE.GE) {
            $scope.from = criterion.rqlQuery.args[1];
          } else {
            $scope.to = criterion.rqlQuery.args[1];
          }
        }
      } else {
        $scope.from = null;
        $scope.to = null;
        $scope.min = null;
        $scope.max = null;
      }

      var query = RqlQueryUtils.hasTargetQuery(criteria.rqlQuery, criterion.target) ? angular.copy(criteria.rqlQuery) : createExistsQuery(criteria, criterion);
      var joinQuery = RqlQueryService.prepareCriteriaTermsQuery(query, criterion);
      JoinQuerySearchResource[targetToType($scope.target)]({query: joinQuery}).$promise.then(function (joinQueryResponse) {
        var stats = RqlQueryService.getTargetAggregations(joinQueryResponse, criterion, $scope.lang);

        if (stats && stats.default) {
          $scope.min = stats.default.min;
          $scope.max = stats.default.max;
        }
      });
    }

    function updateCriteria() {
      $scope.$parent.selectTerm($scope.$parent.target, $scope.$parent.taxonomy, $scope.vocabulary, {from: $scope.from, to: $scope.to});
    }

    $scope.onKeypress = function(ev) {
      if(ev.keyCode === 13) { updateCriteria(); }
    };

    $scope.$on('ngObibaMicaQueryUpdated', function(ev, criteria) {
      if ($scope.vocabulary.isNumeric && $scope.vocabulary.isOpen) {
        updateLimits(criteria, $scope.vocabulary);
      }
    });

    $scope.$on('ngObibaMicaLoadVocabulary', function(ev, taxonomy, vocabulary) {
      if ($scope.vocabulary.isNumeric &&
        vocabulary.name === $scope.vocabulary.name && !vocabulary.isOpen) {
        updateLimits($scope.criteria, vocabulary);
      }
    });
  }])

  .controller('MatchVocabularyFacetController', ['$scope', 'RqlQueryService', function($scope, RqlQueryService) {
    function updateMatch (criteria, vocabulary) {
      var criterion = RqlQueryService.findCriterion(criteria, CriteriaIdGenerator.generate($scope.$parent.taxonomy, vocabulary));
      if(criterion && criterion.rqlQuery && criterion.rqlQuery.args[1]) {
        $scope.text = criterion.rqlQuery.args[0];
      } else {
        $scope.text = null;
      }
    }
    
    function updateCriteria() {
      $scope.$parent.selectTerm($scope.$parent.target, $scope.$parent.taxonomy, $scope.vocabulary, {text: $scope.text || '*'});
    }
    
    $scope.onKeypress = function(ev) {
      if(ev.keyCode === 13) {
        updateCriteria();
      }
    };

    $scope.$on('ngObibaMicaQueryUpdated', function(ev, criteria) {
      if ($scope.vocabulary.isMatch && $scope.vocabulary.isOpen) {
        updateMatch(criteria, $scope.vocabulary);
      }
    });

    $scope.$on('ngObibaMicaLoadVocabulary', function(ev, taxonomy, vocabulary) {
      if (vocabulary.name === $scope.vocabulary.name && !vocabulary.isOpen) {
        updateMatch($scope.criteria, vocabulary);
      }
    });
  }])

  .controller('TermsVocabularyFacetController', ['$scope', '$filter', 'JoinQuerySearchResource', 'RqlQueryService',
    'RqlQueryUtils',
    function($scope, $filter, JoinQuerySearchResource, RqlQueryService, RqlQueryUtils) {
      function isSelectedTerm (criterion, term) {
        return criterion.selectedTerms && criterion.selectedTerms.indexOf(term.key) !== -1;
      }

      $scope.selectTerm = function (target, taxonomy, vocabulary, args) {
        var selected = vocabulary.terms.filter(function(t) {return t.selected;}).map(function(t) { return t.name; }),
          criterion = RqlQueryService.findCriterion($scope.criteria, CriteriaIdGenerator.generate(taxonomy, vocabulary));
        if(criterion) {
          if (selected.length === 0 && $scope.selectedFilter !== RQL_NODE.MISSING) {
            criterion.rqlQuery.name = RQL_NODE.EXISTS;
          }

          RqlQueryUtils.updateQuery(criterion.rqlQuery, selected);
          $scope.onRefresh();
        } else {
          $scope.onSelectTerm(target, taxonomy, vocabulary, args);
        }
      };

      function updateCounts(criteria, vocabulary) {
        var query = null, isCriterionPresent = false;

        function createExistsQuery(criteria, criterion) {
          var rootQuery = angular.copy(criteria.rqlQuery);
          criterion.rqlQuery = RqlQueryUtils.buildRqlQuery(criterion);
          RqlQueryService.addCriteriaItem(rootQuery, criterion);
          return rootQuery;
        }

        var criterion = RqlQueryService.findCriterion(criteria,
          CriteriaIdGenerator.generate($scope.$parent.taxonomy, vocabulary));

        if(criterion) {
          isCriterionPresent = true;
        } else {
          criterion = RqlQueryService.createCriteriaItem($scope.target, $scope.$parent.taxonomy, $scope.vocabulary);
        }
        
        if(RqlQueryUtils.hasTargetQuery(criteria.rqlQuery, criterion.target)) {
          query = angular.copy(criteria.rqlQuery);
          
          if(!isCriterionPresent) {
            RqlQueryService.addCriteriaItem(query, criterion, RQL_NODE.OR);
          }
        } else {
          query = createExistsQuery(criteria, criterion); 
        }
        
        var joinQuery = RqlQueryService.prepareCriteriaTermsQuery(query, criterion, criterion.lang);
        JoinQuerySearchResource[targetToType($scope.target)]({query: joinQuery}).$promise.then(function (joinQueryResponse) {
          RqlQueryService.getTargetAggregations(joinQueryResponse, criterion, criterion.lang).forEach(function (term) {
            $scope.vocabulary.terms.some(function(t) {
              if (t.name === term.key) {
                t.selected = isSelectedTerm(criterion, term);
                t.count = term.count;
                return true;
              }
            });
          });
        });
      }
      
      $scope.$on('ngObibaMicaQueryUpdated', function(ev, criteria) {
        if(!$scope.vocabulary.isNumeric && !$scope.vocabulary.isMatch && $scope.vocabulary.isOpen) {
          updateCounts(criteria, $scope.vocabulary);
        }
      });
      
      $scope.$on('ngObibaMicaLoadVocabulary', function(ev, taxonomy, vocabulary) {
        if(vocabulary.name === $scope.vocabulary.name && !$scope.vocabulary.isNumeric && !$scope.vocabulary.isMatch &&
          !vocabulary.isOpen) {
          updateCounts($scope.criteria, vocabulary);
        }
      });
  }])

  .controller('TaxonomiesPanelController', ['$scope', '$location', 'TaxonomyResource',
    'TaxonomiesResource', 'ngObibaMicaSearch', 'RqlQueryUtils', TaxonomiesPanelController])

  .controller('ClassificationPanelController', ['$scope', '$location', 'TaxonomyResource',
    'TaxonomiesResource', 'ngObibaMicaSearch', 'RqlQueryUtils', ClassificationPanelController])

  .controller('TaxonomiesFacetsController', ['$scope', 'TaxonomyResource', 'TaxonomiesResource', 'LocalizedValues', 'ngObibaMicaSearch',
    'RqlQueryUtils', function ($scope, TaxonomyResource, TaxonomiesResource, LocalizedValues, ngObibaMicaSearch, RqlQueryUtils) {
      $scope.options = ngObibaMicaSearch.getOptions();
      $scope.taxonomies = {};
      $scope.targets = [];
      $scope.RqlQueryUtils = RqlQueryUtils;
      
      $scope.$watch('facetedTaxonomies', function(facetedTaxonomies) {
        if(facetedTaxonomies) {
          $scope.targets = $scope.options.targetTabsOrder.filter(function (t) {
            return facetedTaxonomies[t].length;
          });
          
          $scope.target = $scope.targets[0];
          init($scope.target);
        }
      });

      $scope.selectTerm = function(target, taxonomy, vocabulary, args) {
        $scope.onSelectTerm(target, taxonomy, vocabulary, args);
      };
      
      $scope.setTarget = function(target) {
        $scope.target=target;
        init(target);
      };
      
      $scope.loadVocabulary = function(taxonomy, vocabulary) {
        $scope.$broadcast('ngObibaMicaLoadVocabulary', taxonomy, vocabulary);
      };

      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.lang);
      };
      
      function init(target) {
        if($scope.taxonomies[target]) { return; }
        
        TaxonomiesResource.get({
          target: target 
        }, function onSuccess(taxonomies) {
          $scope.taxonomies[target] = $scope.facetedTaxonomies[target].map(function(f) {
            return taxonomies.filter(function(t) {
              return f.name === t.name;
            })[0];
          }).filter(function(t) { return t; }).map(function(t) {
            t.vocabularies.map(function (v) {
              v.limit = 10;
              v.isMatch = RqlQueryUtils.isMatchVocabulary(v);
              v.isNumeric = RqlQueryUtils.isNumericVocabulary(v);
            });
            
            return t;
          });
          
          if($scope.taxonomies[target].length === 1) {
            $scope.taxonomies[target][0].isOpen = 1;
          }
        });
      }
    }
  ])
  
  .controller('SearchResultController', [
    '$scope',
    'ngObibaMicaSearch',
    function ($scope,
              ngObibaMicaSearch) {

      function updateTarget(type) {
        Object.keys($scope.activeTarget).forEach(function (key) {
          $scope.activeTarget[key].active = type === key;
        });
      }

      $scope.targetTypeMap = $scope.$parent.taxonomyTypeMap;
      $scope.QUERY_TARGETS = QUERY_TARGETS;
      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.options = ngObibaMicaSearch.getOptions();
      $scope.activeTarget = {};
      $scope.activeTarget[QUERY_TYPES.VARIABLES] = {active: false, name: QUERY_TARGETS.VARIABLE, totalHits: 0};
      $scope.activeTarget[QUERY_TYPES.DATASETS] = {active: false, name: QUERY_TARGETS.DATASET, totalHits: 0};
      $scope.activeTarget[QUERY_TYPES.STUDIES] = {active: false, name: QUERY_TARGETS.STUDY, totalHits: 0};
      $scope.activeTarget[QUERY_TYPES.NETWORKS] = {active: false, name: QUERY_TARGETS.NETWORK, totalHits: 0};

      $scope.selectTarget = function (type) {
        updateTarget(type);
        $scope.type = type;
        $scope.$parent.onTypeChanged(type);
      };

      $scope.$watchCollection('result', function () {
        if ($scope.result.list) {
          $scope.activeTarget[QUERY_TYPES.VARIABLES].totalHits = $scope.result.list.variableResultDto.totalHits;
          $scope.activeTarget[QUERY_TYPES.DATASETS].totalHits = $scope.result.list.datasetResultDto.totalHits;
          $scope.activeTarget[QUERY_TYPES.STUDIES].totalHits = $scope.result.list.studyResultDto.totalHits;
          $scope.activeTarget[QUERY_TYPES.NETWORKS].totalHits = $scope.result.list.networkResultDto.totalHits;
        }
      });


      $scope.$watch('type', function (type) {
        updateTarget(type);
      });

      $scope.DISPLAY_TYPES = DISPLAY_TYPES;
    }])

  .controller('CriterionLogicalController', [
    '$scope',
    function ($scope) {
      $scope.updateLogical = function (operator) {
        $scope.item.rqlQuery.name = operator;
        $scope.$emit(CRITERIA_ITEM_EVENT.refresh);
      };
    }])

  .controller('CriterionDropdownController', [
    '$scope',
    '$filter',
    'LocalizedValues',
    'RqlQueryUtils',
    'StringUtils',
    function ($scope, $filter, LocalizedValues, RqlQueryUtils, StringUtils) {
      var closeDropdown = function () {
        if (!$scope.state.open) {
          return;
        }

        $scope.state.onClose();

        var wasDirty = $scope.state.dirty;
        $scope.state.open = false;
        $scope.state.dirty = false;
        if (wasDirty) {
          // trigger a query update
          $scope.$emit(CRITERIA_ITEM_EVENT.refresh);
        }
      };

      var openDropdown = function () {
        if ($scope.state.open) {
          closeDropdown();
          return;
        }

        $scope.state.open = true;
        $scope.state.onOpen();
      };

      var remove = function () {
        $scope.$emit(CRITERIA_ITEM_EVENT.deleted, $scope.criterion);
      };

      var onKeyup = function (event) {
        if (event.keyCode === 13) {
          closeDropdown();
        }
      };

      $scope.state = new CriterionState();
      $scope.timestamp = new Date().getTime();
      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };
      $scope.localizeCriterion = function () {
        var rqlQuery = $scope.criterion.rqlQuery;
        if ((rqlQuery.name === RQL_NODE.IN || rqlQuery.name === RQL_NODE.CONTAINS) && $scope.criterion.selectedTerms && $scope.criterion.selectedTerms.length > 0) {
          var sep = rqlQuery.name === RQL_NODE.IN ? ' | ' : ' + ';
          return $scope.criterion.selectedTerms.map(function (t) {
            if (!$scope.criterion.vocabulary.terms) {
              return t;
            }
            var found = $scope.criterion.vocabulary.terms.filter(function (arg) {
              return arg.name === t;
            }).pop();
            return found ? LocalizedValues.forLocale(found.title, $scope.criterion.lang) : t;
          }).join(sep);
        }
        var operation = rqlQuery.name;
        switch (rqlQuery.name) {
          case RQL_NODE.EXISTS:
            operation = ':' + $filter('translate')('any');
            break;
          case RQL_NODE.MISSING:
            operation = ':' + $filter('translate')('none');
            break;
          case RQL_NODE.EQ:
            operation = '=' + rqlQuery.args[1];
            break;
          case RQL_NODE.GE:
            operation = '>' + rqlQuery.args[1];
            break;
          case RQL_NODE.LE:
            operation = '<' + rqlQuery.args[1];
            break;
          case RQL_NODE.BETWEEN:
            operation = ':[' + rqlQuery.args[1] + ')';
            break;
          case RQL_NODE.IN:
          case RQL_NODE.CONTAINS:
            operation = '';
            break;
          case RQL_NODE.MATCH:
            operation = ':match(' + rqlQuery.args[0] + ')';
            break;
        }
        return LocalizedValues.forLocale($scope.criterion.vocabulary.title, $scope.criterion.lang) + operation;
      };
      $scope.vocabularyType = function (vocabulary) {
        return RqlQueryUtils.vocabularyType(vocabulary);
      };
      $scope.onKeyup = onKeyup;
      $scope.truncate = StringUtils.truncate;
      $scope.remove = remove;
      $scope.openDropdown = openDropdown;
      $scope.closeDropdown = closeDropdown;
      $scope.RqlQueryUtils = RqlQueryUtils;
    }])

  .controller('MatchCriterionTermsController', [
    '$scope',
    'RqlQueryService',
    'LocalizedValues',
    'JoinQuerySearchResource',
    'RqlQueryUtils',
    'SearchContext',
    function ($scope, RqlQueryService, LocalizedValues, JoinQuerySearchResource, RqlQueryUtils, SearchContext) {
      $scope.lang = SearchContext.currentLocale();

      var update = function () {
        $scope.state.dirty = true;
        RqlQueryUtils.updateMatchQuery($scope.criterion.rqlQuery, $scope.match);
      };

      var queryString = $scope.criterion.rqlQuery.args[0];
      $scope.match = queryString === '*' ? '' : queryString;
      $scope.update = update;
      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };

    }])

  .controller('NumericCriterionController', [
    '$scope',
    'RqlQueryService',
    'LocalizedValues',
    'JoinQuerySearchResource',
    'RqlQueryUtils',
    'SearchContext',
    function ($scope, RqlQueryService, LocalizedValues, JoinQuerySearchResource, RqlQueryUtils, SearchContext) {
      $scope.lang = SearchContext.currentLocale();
      var range = $scope.criterion.rqlQuery.args[1];

      if (angular.isArray(range)) {
        $scope.from = $scope.criterion.rqlQuery.args[1][0];
        $scope.to = $scope.criterion.rqlQuery.args[1][1];
      } else {
        $scope.from = $scope.criterion.rqlQuery.name === RQL_NODE.GE ? range : null;
        $scope.to = $scope.criterion.rqlQuery.name === RQL_NODE.LE ? range : null;
      }

      var updateLimits = function () {
        var target = $scope.criterion.target,
          joinQuery = RqlQueryService.prepareCriteriaTermsQuery($scope.query, $scope.criterion);
        JoinQuerySearchResource[targetToType(target)]({query: joinQuery}).$promise.then(function (joinQueryResponse) {
          var stats = RqlQueryService.getTargetAggregations(joinQueryResponse, $scope.criterion, $scope.lang);

          if (stats && stats.default) {
            $scope.min = stats.default.min;
            $scope.max = stats.default.max;
          }
        });
      };

      var onOpen = function () {
        updateLimits();
      };

      var onClose = function () {
        $scope.updateSelection();
      };

      $scope.updateSelection = function () {
        RqlQueryUtils.updateRangeQuery($scope.criterion.rqlQuery, $scope.from, $scope.to, $scope.selectMissing);
        $scope.state.dirty = true;
      };

      $scope.selectMissing = $scope.criterion.rqlQuery.name === RQL_NODE.MISSING;
      $scope.state.addOnClose(onClose);
      $scope.state.addOnOpen(onOpen);
      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };
    }])

  .controller('StringCriterionTermsController', [
    '$scope',
    'RqlQueryService',
    'LocalizedValues',
    'StringUtils',
    'JoinQuerySearchResource',
    'RqlQueryUtils',
    'SearchContext',
    '$filter',
    function ($scope,
              RqlQueryService,
              LocalizedValues,
              StringUtils,
              JoinQuerySearchResource,
              RqlQueryUtils,
              SearchContext,
              $filter) {
      $scope.lang = SearchContext.currentLocale();

      var isSelected = function (name) {
        return $scope.checkboxTerms.indexOf(name) !== -1;
      };

      var updateSelection = function () {
        $scope.state.dirty = true;
        $scope.criterion.rqlQuery.name = $scope.selectedFilter;
        var selected = [];
        if($scope.selectedFilter !== RQL_NODE.MISSING && $scope.selectedFilter !== RQL_NODE.EXISTS) {
          Object.keys($scope.checkboxTerms).forEach(function (key) {
            if ($scope.checkboxTerms[key]) {
              selected.push(key);
            }
          });
        }
        if (selected.length === 0 && $scope.selectedFilter !== RQL_NODE.MISSING) {
          $scope.criterion.rqlQuery.name = RQL_NODE.EXISTS;
        }
        RqlQueryUtils.updateQuery($scope.criterion.rqlQuery, selected);
      };

      var updateFilter = function () {
        updateSelection();
      };

      var isInFilter = function () {
        return $scope.selectedFilter === RQL_NODE.IN;
      };

      var isContainsFilter = function () {
        return $scope.selectedFilter === RQL_NODE.CONTAINS;
      };

      var onOpen = function () {
        $scope.state.loading = true;
        var target = $scope.criterion.target;
        var joinQuery = RqlQueryService.prepareCriteriaTermsQuery($scope.query, $scope.criterion, $scope.lang);

        JoinQuerySearchResource[targetToType(target)]({query: joinQuery}).$promise.then(function (joinQueryResponse) {
          $scope.state.loading = false;
          $scope.terms = RqlQueryService.getTargetAggregations(joinQueryResponse, $scope.criterion, $scope.lang);
          if ($scope.terms) {
            $scope.terms.forEach(function (term) {
              $scope.checkboxTerms[term.key] = $scope.isSelectedTerm(term);
            });

            $scope.terms = $filter('orderBySelection')($scope.terms, $scope.checkboxTerms);
          }
        });
      };

      $scope.isSelectedTerm = function (term) {
        return $scope.criterion.selectedTerms && $scope.criterion.selectedTerms.indexOf(term.key) !== -1;
      };

      $scope.state.addOnOpen(onOpen);
      $scope.checkboxTerms = {};
      $scope.RQL_NODE = RQL_NODE;
      $scope.selectedFilter = $scope.criterion.type;
      $scope.isSelected = isSelected;
      $scope.updateFilter = updateFilter;
      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };
      $scope.truncate = StringUtils.truncate;
      $scope.isInFilter = isInFilter;
      $scope.isContainsFilter = isContainsFilter;
      $scope.updateSelection = updateSelection;
    }])

  .controller('CoverageResultTableController', [
    '$scope',
    '$location',
    '$q',
    'PageUrlService',
    'RqlQueryUtils',
    'RqlQueryService',
    'CoverageGroupByService',
    function ($scope, $location, $q, PageUrlService, RqlQueryUtils, RqlQueryService, CoverageGroupByService) {
      var targetMap = {}, vocabulariesTermsMap = {};
      
      targetMap[BUCKET_TYPES.NETWORK] = QUERY_TARGETS.NETWORK;
      targetMap[BUCKET_TYPES.STUDY] = QUERY_TARGETS.STUDY;
      targetMap[BUCKET_TYPES.DCE] = QUERY_TARGETS.VARIABLE;
      targetMap[BUCKET_TYPES.DATASCHEMA] = QUERY_TARGETS.DATASET;
      targetMap[BUCKET_TYPES.DATASET] = QUERY_TARGETS.DATASET;

      $scope.showMissing = true;
      $scope.toggleMissing = function (value) {
        $scope.showMissing = value;
      };
      $scope.groupByOptions = CoverageGroupByService;
      $scope.bucketSelection = {
        dceBucketSelected: $scope.bucket === BUCKET_TYPES.DCE,
        datasetBucketSelected: $scope.bucket !== BUCKET_TYPES.DATASCHEMA
      };

      function decorateTermHeaders(vocabularyHeaders, termHeaders) {
        var idx = 0;
        return vocabularyHeaders.reduce(function(vocabularies, v) {
          vocabularies[v.entity.name] = termHeaders.slice(idx, idx + v.termsCount).map(function(t) {
            if(v.termsCount > 1) {
              t.canRemove = true;
            }
            
            t.vocabularyName = v.entity.name;

            return t;
          });

          idx += v.termsCount;
          return vocabularies;
        }, {});
      }

      $scope.$watch('bucketSelection.dceBucketSelected', function (val, old) {
        if (val === old) {
          return;
        }

        if (val) {
          $scope.selectBucket(BUCKET_TYPES.DCE);
        } else if ($scope.bucket === BUCKET_TYPES.DCE) {
          $scope.selectBucket(BUCKET_TYPES.STUDY);
        }
      });

      $scope.$watch('bucketSelection.datasetBucketSelected', function (val, old) {
        if (val === old) {
          return;
        }

        if (val) {
          $scope.selectBucket(BUCKET_TYPES.DATASET);
        } else if ($scope.bucket === BUCKET_TYPES.DATASET) {
          $scope.selectBucket(BUCKET_TYPES.DATASCHEMA);
        }
      });

      $scope.selectBucket = function (bucket) {
        if (bucket === BUCKET_TYPES.STUDY && $scope.bucketSelection.dceBucketSelected) {
          bucket = BUCKET_TYPES.DCE;
        }

        if (bucket === BUCKET_TYPES.DATASET && !$scope.bucketSelection.datasetBucketSelected) {
          bucket = BUCKET_TYPES.DATASCHEMA;
        }

        $scope.bucket = bucket;
        $scope.$parent.onBucketChanged(bucket);
      };

      $scope.rowspans = {};

      $scope.getSpan = function (study, population) {
        var length = 0;
        if (population) {
          var prefix = study + ':' + population;
          length = $scope.result.rows.filter(function (row) {
            return row.title.startsWith(prefix + ':');
          }).length;
          $scope.rowspans[prefix] = length;
          return length;
        } else {
          length = $scope.result.rows.filter(function (row) {
            return row.title.startsWith(study + ':');
          }).length;
          $scope.rowspans[study] = length;
          return length;
        }
      };

      $scope.hasSpan = function (study, population) {
        if (population) {
          return $scope.rowspans[study + ':' + population] > 0;
        } else {
          return $scope.rowspans[study] > 0;
        }
      };

      $scope.hasVariableTarget = function () {
        var query = $location.search().query;
        return query && RqlQueryUtils.hasTargetQuery(new RqlParser().parse(query), RQL_NODE.VARIABLE);
      };

      $scope.hasSelected = function () {
        return $scope.table && $scope.table.rows && $scope.table.rows.filter(function (r) {
            return r.selected;
          }).length;
      };

      $scope.selectAll = function() {
        if ($scope.table && $scope.table.rows) {
          $scope.table.rows.forEach(function(r){
            r.selected = true;
          });
        }
      };

      $scope.selectNone = function() {
        if ($scope.table && $scope.table.rows) {
          $scope.table.rows.forEach(function(r){
            r.selected = false;
          });
        }
      };

      $scope.selectFull = function() {
        if ($scope.table && $scope.table.rows) {
          $scope.table.rows.forEach(function(r){
            if (r.hits) {
              r.selected = r.hits.filter(function(h){
                return h === 0;
              }).length === 0;
            } else {
              r.selected = false;
            }
          });
        }
      };      

      function getBucketUrl(bucket, id) {
        switch (bucket) {
          case BUCKET_TYPES.STUDY:
          case BUCKET_TYPES.DCE:
            return PageUrlService.studyPage(id);
          case BUCKET_TYPES.NETWORK:
            return PageUrlService.networkPage(id);
          case BUCKET_TYPES.DATASCHEMA:
            return PageUrlService.datasetPage(id, 'harmonization');
          case BUCKET_TYPES.DATASET:
            return PageUrlService.datasetPage(id, 'study');
        }

        return '';
      }

      function updateFilterCriteriaInternal(selected) {
        var vocabulary = $scope.bucket === BUCKET_TYPES.DCE ? 'dceIds' : 'id';
        $q.all(selected.map(function (r) {
          return RqlQueryService.createCriteriaItem(targetMap[$scope.bucket], 'Mica_' + targetMap[$scope.bucket], vocabulary, r.value);
        })).then(function (items) {
          if (!items.length) {
            return;
          }

          var selectionItem = items.reduce(function (prev, item) {
            if (prev) {
              RqlQueryService.updateCriteriaItem(prev, item);
              return prev;
            }

            item.rqlQuery = RqlQueryUtils.buildRqlQuery(item);
            return item;
          }, null);

          $scope.onUpdateCriteria(selectionItem, 'variables', true);
        });
      }

      function splitIds() {
        var cols = {
          colSpan: $scope.bucket === BUCKET_TYPES.DCE ? 3 : 1,
          ids: {}
        };

        var rowSpans = {};

        function appendRowSpan(id) {
          var rowSpan;
          if (!rowSpans[id]) {
            rowSpan = 1;
            rowSpans[id] = 1;
          } else {
            rowSpan = 0;
            rowSpans[id] = rowSpans[id] + 1;
          }
          return rowSpan;
        }

        var minMax = {};

        function appendMinMax(id, start, end) {
          if (minMax[id]) {
            if (start < minMax[id][0]) {
              minMax[id][0] = start;
            }
            if (end > minMax[id][1]) {
              minMax[id][1] = end;
            }
          } else {
            minMax[id] = [start, end];
          }
        }

        function toTime(yearMonth, start) {
          var res;
          if (yearMonth) {
            if (yearMonth.indexOf('-')>0) {
              var ym = yearMonth.split('-');
              if (!start) {
                var m = parseInt(ym[1]);
                if(m<12) {
                  ym[1] = m + 1;
                } else {
                  ym[0] = parseInt(ym[0]) + 1;
                  ym[1] = 1;
                }
              }
              var ymStr = ym[0] + '/'  + ym[1] + '/01';
              res = Date.parse(ymStr);
            } else {
              res = start ? Date.parse(yearMonth + '/01/01') : Date.parse(yearMonth + '/12/31');
            }
          }
          return res;
        }

        var currentYear = new Date().getFullYear();
        var currentMonth = new Date().getMonth() + 1;
        var currentDate = toTime(currentYear + '-' + currentMonth, true);

        function getProgress(startYearMonth, endYearMonth) {
          var start = toTime(startYearMonth, true);
          var end = endYearMonth ? toTime(endYearMonth, false) : currentDate;
          var current = end < currentDate ? end : currentDate;
          if(end === start) {
            return 100;
          } else {
            return Math.round(startYearMonth ? 100 * (current - start) / (end - start) : 0);
          }
        }

        var odd = true;
        var groupId;
        $scope.result.rows.forEach(function (row) {
          cols.ids[row.value] = [];
          if ($scope.bucket === BUCKET_TYPES.DCE) {
            var ids = row.value.split(':');
            var titles = row.title.split(':');
            var descriptions = row.description.split(':');
            var rowSpan;
            var id;

            // study
            id = ids[0];
            if (!groupId) {
              groupId = id;
            } else if(id !== groupId) {
              odd = !odd;
              groupId = id;
            }
            rowSpan = appendRowSpan(id);
            appendMinMax(id,row.start, row.end);
            cols.ids[row.value].push({
              id: id,
              url: PageUrlService.studyPage(id),
              title: titles[0],
              description: descriptions[0],
              rowSpan: rowSpan
            });

            // population
            id = ids[0] + ':' + ids[1];
            rowSpan = appendRowSpan(id);
            cols.ids[row.value].push({
              id: id,
              url: PageUrlService.studyPopulationPage(ids[0], ids[1]),
              title: titles[1],
              description: descriptions[1],
              rowSpan: rowSpan
            });

            // dce
            cols.ids[row.value].push({
              id: row.value,
              title: titles[2],
              description: descriptions[2],
              start: row.start,
              current: currentYear + '-' + currentMonth,
              end: row.end,
              progressClass: odd ? 'info' : 'warning',
              url: PageUrlService.studyPopulationPage(ids[0], ids[1]),
              rowSpan: 1
            });
          } else {
            cols.ids[row.value].push({
              id: row.value,
              url: getBucketUrl($scope.bucket, row.value),
              title: row.title,
              description: row.description,
              min: row.start,
              start: row.start,
              current: currentYear,
              end: row.end,
              max: row.end,
              progressStart: 0,
              progress: getProgress(row.start ? row.start + '-01' : undefined, row.end ? row.end + '-12' : undefined),
              progressClass: odd ? 'info' : 'warning',
              rowSpan: 1
            });
            odd = !odd;
          }
        });

        // adjust the rowspans and the progress
        if ($scope.bucket === BUCKET_TYPES.DCE) {
          $scope.result.rows.forEach(function (row) {
            if (cols.ids[row.value][0].rowSpan > 0) {
              cols.ids[row.value][0].rowSpan = rowSpans[cols.ids[row.value][0].id];
            }
            if (cols.ids[row.value][1].rowSpan > 0) {
              cols.ids[row.value][1].rowSpan = rowSpans[cols.ids[row.value][1].id];
            }
            var ids = row.value.split(':');
            if (minMax[ids[0]]) {
              var min = minMax[ids[0]][0];
              var max = minMax[ids[0]][1];
              var start = cols.ids[row.value][2].start;
              var end = cols.ids[row.value][2].end;
              var diff = toTime(max, false) - toTime(min, true);
              // set the DCE min and max dates of the study
              cols.ids[row.value][2].min = min;
              cols.ids[row.value][2].max = max;
              // compute the progress
              cols.ids[row.value][2].progressStart = 100 * (toTime(start, true) - toTime(min, true))/diff;
              cols.ids[row.value][2].progress = 100 * (toTime(end, false) - toTime(start, true))/diff;
            }
          });
        }

        return cols;
      }
      
      function mergeCriteriaItems(criteria) {
        return criteria.reduce(function(prev, item) {
          if (prev) {
            RqlQueryService.updateCriteriaItem(prev, item);
            return prev;
          }

          item.rqlQuery = RqlQueryUtils.buildRqlQuery(item);
          return item;
        }, null);
      }

      $scope.BUCKET_TYPES = BUCKET_TYPES;

      $scope.downloadUrl = function () {
        return PageUrlService.downloadCoverage($scope.query);
      };

      $scope.$watch('result', function () {
        $scope.table = {cols: []};
        vocabulariesTermsMap = {};
        
        if ($scope.result && $scope.result.rows) {
          var tableTmp = $scope.result;
          tableTmp.cols = splitIds();
          $scope.table = tableTmp;

          vocabulariesTermsMap = decorateTermHeaders($scope.table.vocabularyHeaders, $scope.table.termHeaders);
        }
      });

      $scope.updateCriteria = function (id, term, idx, type) { //
        var vocabulary = $scope.bucket === BUCKET_TYPES.DCE ? 'dceIds' : 'id',
          taxonomyHeader = $scope.table.taxonomyHeaders[0].entity,
          vocabularyHeader, countTerms = 0;

        for (var i = 0; i < $scope.table.vocabularyHeaders.length; i++) {
          countTerms += $scope.table.vocabularyHeaders[i].termsCount;
          if (idx < countTerms) {
            vocabularyHeader = $scope.table.vocabularyHeaders[i].entity;
            break;
          }
        }

        var criteria = {varItem: RqlQueryService.createCriteriaItem(QUERY_TARGETS.VARIABLE, taxonomyHeader.name, vocabularyHeader.name, term.entity.name)};

        if (id) {
          criteria.item = RqlQueryService.createCriteriaItem(targetMap[$scope.bucket], 'Mica_' + targetMap[$scope.bucket], vocabulary, id);
        }

        $q.all(criteria).then(function (criteria) {
          $scope.onUpdateCriteria(criteria.varItem, type, false, true);

          if (criteria.item) {
            $scope.onUpdateCriteria(criteria.item, type);
          }
        });
      };
      
      $scope.isFullCoverageImpossibleOrCoverageAlreadyFull = function () {
        var rows = $scope.table ? ($scope.table.rows || []) : [];
        var rowsWithZeroHitColumn = 0;

        if (rows.length === 0) {
          return true;
        }

        rows.forEach(function (row) {
          if (row.hits) {
            if (row.hits.filter(function (hit) { return hit === 0; }).length > 0) {
              rowsWithZeroHitColumn++;
            }
          }
        });
        
        if (rowsWithZeroHitColumn === 0) {
          return true;
        }

        return rows.length === rowsWithZeroHitColumn;
      };

      $scope.selectFullAndFilter = function() {
        var selected = [];
        if ($scope.table && $scope.table.rows) {
          $scope.table.rows.forEach(function(r){
            if (r.hits) {
              if (r.hits.filter(function(h){
                  return h === 0;
                }).length === 0) {
                selected.push(r);
              }
            }
          });
        }
        updateFilterCriteriaInternal(selected);
      };

      $scope.updateFilterCriteria = function () {
        updateFilterCriteriaInternal($scope.table.rows.filter(function (r) {
          return r.selected;
        }));
      };

      $scope.removeTerm = function(term) {
        var taxonomyHeader = $scope.table.taxonomyHeaders[0],
          remainingCriteriaItems = vocabulariesTermsMap[term.vocabularyName].filter(function(t) {
            return t.entity.name !== term.entity.name;
          }).map(function(t) {
            return RqlQueryService.createCriteriaItem(QUERY_TARGETS.VARIABLE, taxonomyHeader.entity.name, t.vocabularyName, t.entity.name);
          });
        
        $q.all(remainingCriteriaItems).then(function(criteriaItems) {
          $scope.onUpdateCriteria(mergeCriteriaItems(criteriaItems), null, true, false, false);
        });
      };

      $scope.removeVocabulary = function(vocabulary) {
        var taxonomyHeader = $scope.table.taxonomyHeaders[0];
        RqlQueryService.createCriteriaItem(QUERY_TARGETS.VARIABLE, taxonomyHeader.entity.name, vocabulary.entity.name).then(function(item){
          $scope.onRemoveCriteria(item);
        });
      };
    }])

  .controller('GraphicsResultController', [
    'GraphicChartsConfig',
    'GraphicChartsUtils',
    'RqlQueryService',
    '$filter',
    '$scope',
    function (GraphicChartsConfig,
              GraphicChartsUtils,
              RqlQueryService,
              $filter,
              $scope) {

      var setChartObject = function (vocabulary, dtoObject, header, title, options) {

        return GraphicChartsUtils.getArrayByAggregation(vocabulary, dtoObject)
          .then(function (entries){
            var data = entries.map(function (e) {
              if (e.participantsNbr) {
                return [e.title, e.value, e.participantsNbr];
              }
              else {
                return [e.title, e.value];
              }
            });

            if (data.length > 0) {
              data.unshift(header);
              angular.extend(options, {title: title});

              return {
                data: data,
                entries: entries,
                options: options,
                vocabulary: vocabulary
              };
            }
          });

      };

      var charOptions = GraphicChartsConfig.getOptions().ChartsOptions;

      $scope.updateCriteria = function (key, vocabulary) {
        RqlQueryService.createCriteriaItem('study', 'Mica_study', vocabulary, key).then(function (item) {
          $scope.onUpdateCriteria(item, 'studies');
        });
      };

      $scope.$watch('result', function (result) {
        $scope.chartObjects = {};
        $scope.noResults = true;

        if (result && result.studyResultDto.totalHits) {
          $scope.noResults = false;
          setChartObject('populations-selectionCriteria-countriesIso',
            result.studyResultDto,
            [$filter('translate')(charOptions.geoChartOptions.header[0]), $filter('translate')(charOptions.geoChartOptions.header[1])],
            $filter('translate')(charOptions.geoChartOptions.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.geoChartOptions.options).then(function(geoStudies) {
              if (geoStudies) {
                angular.extend($scope.chartObjects,
                  {
                    geoChartOptions: {
                      directiveTitle: geoStudies.options.title,
                      headerTitle: $filter('translate')('graphics.geo-charts'),
                      chartObject: {
                        geoTitle: geoStudies.options.title,
                        options: geoStudies.options,
                        type: 'GeoChart',
                        vocabulary: geoStudies.vocabulary,
                        data: geoStudies.data,
                        entries: geoStudies.entries
                      }
                    }
                  });
              }
            });

          setChartObject('methods-designs',
            result.studyResultDto,
            [$filter('translate')(charOptions.studiesDesigns.header[0]),
              $filter('translate')(charOptions.studiesDesigns.header[1]),
              //$filter('translate')(charOptions.studiesDesigns.header[2])
              ],
            $filter('translate')(charOptions.studiesDesigns.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.studiesDesigns.options).then(function(methodDesignStudies) {
              if (methodDesignStudies) {
                angular.extend($scope.chartObjects, {
                  studiesDesigns: {
                    //directiveTitle: methodDesignStudies.options.title ,
                    headerTitle: $filter('translate')('graphics.study-design'),
                    chartObject: {
                      options: methodDesignStudies.options,
                      type: 'BarChart',
                      data: methodDesignStudies.data,
                      vocabulary: methodDesignStudies.vocabulary,
                      entries: methodDesignStudies.entries
                    }
                  }
                });
              }
            });

          setChartObject('numberOfParticipants-participant-range',
            result.studyResultDto,
            [$filter('translate')(charOptions.numberParticipants.header[0]), $filter('translate')(charOptions.numberParticipants.header[1])],
            $filter('translate')(charOptions.numberParticipants.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.numberParticipants.options).then(function(numberParticipant) {
              if (numberParticipant) {
                angular.extend($scope.chartObjects, {
                  numberParticipants: {
                    headerTitle: $filter('translate')('graphics.number-participants'),
                    chartObject: {
                      options: numberParticipant.options,
                      type: 'PieChart',
                      data: numberParticipant.data,
                      vocabulary: numberParticipant.vocabulary,
                      entries: numberParticipant.entries
                    }
                  }
                });
              }
            });

          setChartObject('populations-dataCollectionEvents-bioSamples',
            result.studyResultDto,
            [$filter('translate')(charOptions.biologicalSamples.header[0]), $filter('translate')(charOptions.biologicalSamples.header[1])],
            $filter('translate')(charOptions.biologicalSamples.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.biologicalSamples.options).then(function(bioSamplesStudies) {
              if (bioSamplesStudies) {
                angular.extend($scope.chartObjects, {
                  biologicalSamples: {
                    headerTitle: $filter('translate')('graphics.bio-samples'),
                    chartObject: {
                      options: bioSamplesStudies.options,
                      type: 'BarChart',
                      data: bioSamplesStudies.data,
                      vocabulary: bioSamplesStudies.vocabulary,
                      entries: bioSamplesStudies.entries
                    }
                  }
                });
              }
            });
        }
      });
    }])

  .controller('SearchResultPaginationController', ['$scope', function ($scope) {

    function updateMaxSize() {
      $scope.maxSize = Math.min(3, Math.ceil($scope.totalHits / $scope.pagination.selected.value));
    }

    function calculateRange() {
      var pageSize = $scope.pagination.selected.value;
      var current = $scope.pagination.currentPage;
      $scope.pagination.from = pageSize * (current - 1) + 1;
      $scope.pagination.to = Math.min($scope.totalHits, pageSize * current);
    }

    var pageChanged = function () {
      calculateRange();
      if ($scope.onChange) {
        $scope.onChange(
          $scope.target,
          ($scope.pagination.currentPage - 1) * $scope.pagination.selected.value,
          $scope.pagination.selected.value
        );
      }
    };

    var pageSizeChanged = function () {
      updateMaxSize();
      $scope.pagination.currentPage = 1;
      pageChanged();
    };

    $scope.pageChanged = pageChanged;
    $scope.pageSizeChanged = pageSizeChanged;
    $scope.pageSizes = [
      {label: '10', value: 10},
      {label: '20', value: 20},
      {label: '50', value: 50},
      {label: '100', value: 100}
    ];

    $scope.pagination = {
      selected: $scope.pageSizes[0],
      currentPage: 1
    };

    $scope.$watch('totalHits', function () {
      updateMaxSize();
      calculateRange();
    });
  }]);

;/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

/* exported CRITERIA_ITEM_EVENT */
var CRITERIA_ITEM_EVENT = {
  deleted: 'event:delete-criteria-item',
  refresh: 'event:refresh-criteria-item'
};

angular.module('obiba.mica.search')

  .directive('taxonomyPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        taxonomy: '=',
        lang: '=',
        onNavigate: '='
      },
      templateUrl: 'search/views/classifications/taxonomy-panel-template.html'
    };
  }])

  .directive('vocabularyPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        target: '=',
        taxonomy: '=',
        vocabulary: '=',
        lang: '=',
        onNavigate: '=',
        onSelect: '=',
        onHideSearchNavigate: '=',
        isInHideNavigate: '='
      },
      templateUrl: 'search/views/classifications/vocabulary-panel-template.html'
    };
  }])

  .directive('termPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        target: '=',
        taxonomy: '=',
        vocabulary: '=',
        term: '=',
        lang: '=',
        onSelect: '='
      },
      templateUrl: 'search/views/classifications/term-panel-template.html'
    };
  }])

  .directive('networksResultTable', ['PageUrlService', 'ngObibaMicaSearch', 'RqlQueryService',
    function (PageUrlService, ngObibaMicaSearch, RqlQueryService) {
      return {
        restrict: 'EA',
        replace: true,
        scope: {
          summaries: '=',
          loading: '=',
          onUpdateCriteria: '='
        },
        templateUrl: 'search/views/list/networks-search-result-table-template.html',
        link: function(scope) {
          scope.options = ngObibaMicaSearch.getOptions().networks;
          scope.optionsCols = scope.options.networksColumn;
          scope.PageUrlService = PageUrlService;

          scope.updateCriteria = function (id, type) {
            var datasetClassName;
            if (type === 'HarmonizationDataset' || type === 'StudyDataset') {
              datasetClassName = type;
              type = 'datasets';
            }

            var variableType;
            if (type === 'DataschemaVariable' || type === 'StudyVariable') {
              variableType = type.replace('Variable', '');
              type = 'variables';
            }

            RqlQueryService.createCriteriaItem('network', 'Mica_network', 'id', id).then(function (item) {
              if(datasetClassName) {
                RqlQueryService.createCriteriaItem('dataset', 'Mica_dataset', 'className', datasetClassName).then(function(datasetItem) {
                  scope.onUpdateCriteria(item, type);
                  scope.onUpdateCriteria(datasetItem, type);
                });
              } else if (variableType) {
                RqlQueryService.createCriteriaItem('variable', 'Mica_variable', 'variableType', variableType).then(function (variableItem) {
                  scope.onUpdateCriteria(item, type);
                  scope.onUpdateCriteria(variableItem, type);
                });
              } else {
                scope.onUpdateCriteria(item, type);
              }
            });
          };
        }
      };
  }])

  .directive('datasetsResultTable', ['PageUrlService', 'ngObibaMicaSearch', 'TaxonomyResource', 'RqlQueryService', function (PageUrlService, ngObibaMicaSearch, TaxonomyResource, RqlQueryService) {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '=',
        onUpdateCriteria: '='
      },
      templateUrl: 'search/views/list/datasets-search-result-table-template.html',
      link: function(scope) {
        scope.classNames = {};
        TaxonomyResource.get({
          target: 'dataset',
          taxonomy: 'Mica_dataset'
        }).$promise.then(function (taxonomy) {
            scope.classNames = taxonomy.vocabularies.filter(function (v) {
              return v.name === 'className';
            })[0].terms.reduce(function (prev, t) {
                prev[t.name] = t.title.map(function (t) {
                  return {lang: t.locale, value: t.text};
                });
                return prev;
              }, {});
          });

        scope.updateCriteria = function (id, type) {
          RqlQueryService.createCriteriaItem('dataset', 'Mica_dataset', 'id', id).then(function(item) {
            scope.onUpdateCriteria(item, type);
          });
        };

        scope.options = ngObibaMicaSearch.getOptions().datasets;
        scope.optionsCols = scope.options.datasetsColumn;
        scope.PageUrlService = PageUrlService;
      }
    };
  }])

  .directive('studiesResultTable', ['PageUrlService', 'ngObibaMicaSearch', 'TaxonomyResource', 'RqlQueryService', 'LocalizedValues',
    function (PageUrlService, ngObibaMicaSearch, TaxonomyResource, RqlQueryService, LocalizedValues) {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        lang: '=',
        summaries: '=',
        loading: '=',
        onUpdateCriteria: '='
      },
      templateUrl: 'search/views/list/studies-search-result-table-template.html',
      link: function(scope) {
        scope.taxonomy = {};
        scope.designs = {};
        scope.datasourceTitles = {};

        function getDatasourceTitles() {
          if (Object.keys(scope.taxonomy) < 1 || Object.keys(scope.datasourceTitles) > 0) {
            return;
          }

          scope.taxonomy.vocabularies.some(function(vocabulary) {
            if (vocabulary.name === 'populations-dataCollectionEvents-dataSources') {
              vocabulary.terms.forEach(function(term) {
                scope.datasourceTitles[term.name] = {title: LocalizedValues.forLocale(term.title, scope.lang)};
              });
              return true;
            }
            return false;
          });
        }

        scope.$watch('lang', getDatasourceTitles);

        TaxonomyResource.get({
          target: 'study',
          taxonomy: 'Mica_study'
        }).$promise.then(function (taxonomy) {
          scope.taxonomy = taxonomy;
          getDatasourceTitles();
          scope.designs = taxonomy.vocabularies.filter(function (v) {
            return v.name === 'methods-designs';
          })[0].terms.reduce(function (prev, t) {
              prev[t.name] = t.title.map(function (t) {
                return {lang: t.locale, value: t.text};
              });
              return prev;
            }, {});
        });

        scope.hasDatasource = function (datasources, id) {
          return datasources && datasources.indexOf(id) > -1;
        };

        scope.options = ngObibaMicaSearch.getOptions().studies;
        scope.optionsCols = scope.options.studiesColumn;
        scope.PageUrlService = PageUrlService;

        scope.updateCriteria = function (id, type) {
          var datasetClassName;
          if (type === 'HarmonizationDataset' || type === 'StudyDataset') {
            datasetClassName = type;
            type = 'datasets';
          }

          var variableType;
          if (type === 'DataschemaVariable' || type === 'StudyVariable') {
            variableType = type.replace('Variable', '');
            type = 'variables';
          }

          RqlQueryService.createCriteriaItem('study', 'Mica_study', 'id', id).then(function(item) {
            if(datasetClassName) {
              RqlQueryService.createCriteriaItem('dataset', 'Mica_dataset', 'className', datasetClassName).then(function (datasetItem) {
                scope.onUpdateCriteria(item, type);
                scope.onUpdateCriteria(datasetItem, type);
              });
            } else if (variableType) {
              RqlQueryService.createCriteriaItem('variable', 'Mica_variable', 'variableType', variableType).then(function (variableItem) {
                scope.onUpdateCriteria(item, type);
                scope.onUpdateCriteria(variableItem, type);
              });
            } else {
              scope.onUpdateCriteria(item, type);
            }
          });
        };
      }
    };
  }])

  .directive('variablesResultTable', ['PageUrlService', 'ngObibaMicaSearch', function (PageUrlService, ngObibaMicaSearch) {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/variables-search-result-table-template.html',
      link: function(scope) {
        scope.options = ngObibaMicaSearch.getOptions().variables;
        scope.optionsCols = scope.options.variablesColumn;
        scope.PageUrlService = PageUrlService;
      }
    };
  }])

  .directive('coverageResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        result: '=',
        loading: '=',
        bucket: '=',
        query: '=',
        onUpdateCriteria: '=',
        onRemoveCriteria: '='
      },
      controller: 'CoverageResultTableController',
      templateUrl: 'search/views/coverage/coverage-search-result-table-template.html'
    };
  }])

  .directive('graphicsResult', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        result: '=',
        loading: '=',
        onUpdateCriteria: '='
      },
      controller: 'GraphicsResultController',
      templateUrl: 'search/views/graphics/graphics-search-result-template.html'
    };
  }])

  .directive('includeReplace', function () {
    return {
      require: 'ngInclude',
      link: function (scope, el) {
        el.replaceWith(el.children());
      }
    };
  })

  .directive('scrollToTop', function(){
    return {
      restrict: 'A',
      scope: {
        trigger: '=scrollToTop'
      },
      link: function postLink(scope, elem) {
        scope.$watch('trigger', function() {
          elem[0].scrollTop = 0;
        });
      }
    };
  })

  .directive('resultPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        type: '=',
        bucket: '=',
        query: '=',
        display: '=',
        result: '=',
        lang: '=',
        loading: '=',
        searchTabsOrder: '=',
        resultTabsOrder: '=',
        onTypeChanged: '=',
        onBucketChanged: '=',
        onPaginate: '=',
        onUpdateCriteria: '=',
        onRemoveCriteria: '='
      },
      controller: 'SearchResultController',
      templateUrl: 'search/views/search-result-panel-template.html'
    };
  }])

  .directive('criteriaRoot', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        item: '=',
        query: '=',
        advanced: '=',
        onRemove: '=',
        onRefresh: '='
      },
      templateUrl: 'search/views/criteria/criteria-root-template.html',
      link: function(scope) {
        scope.$on(CRITERIA_ITEM_EVENT.deleted, function(event, item){
          scope.onRemove(item);
        });

        scope.$on(CRITERIA_ITEM_EVENT.refresh, function(){
          scope.onRefresh();
        });
      }
    };
  }])

  .directive('criteriaTarget', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        item: '=',
        query: '=',
        advanced: '='
      },
      templateUrl: 'search/views/criteria/criteria-target-template.html'
    };
  }])

  .directive('criteriaNode', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        item: '=',
        query: '=',
        advanced: '='
      },
      controller: 'CriterionLogicalController',
      templateUrl: 'search/views/criteria/criteria-node-template.html'
    };
  }])

  /**
   * This directive creates a hierarchical structure matching that of a RqlQuery tree.
   */
  .directive('criteriaLeaf', ['CriteriaNodeCompileService', function(CriteriaNodeCompileService){
      return {
        restrict: 'EA',
        replace: true,
        scope: {
          item: '=',
          query: '=',
          advanced: '='
        },
        controller: 'CriterionLogicalController',
        link: function(scope, element) {
          CriteriaNodeCompileService.compile(scope, element);
        }
      };
    }])

  .directive('numericCriterion', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criterion: '=',
        query: '=',
        state: '='
      },
      controller: 'NumericCriterionController',
      templateUrl: 'search/views/criteria/criterion-numeric-template.html'
    };
  }])

  /**
   * This directive serves as the container for each time of criterion based on a vocabulary type.
   * Specialize contents types as directives and share the state with this container.
   */
  .directive('criterionDropdown', ['$document', '$timeout', function ($document, $timeout) {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criterion: '=',
        query: '='
      },
      controller: 'CriterionDropdownController',
      templateUrl: 'search/views/criteria/criterion-dropdown-template.html',//
      link: function( $scope, $element){
        var onDocumentClick = function (event) {
          var isChild = document.querySelector('#'+$scope.criterion.id.replace('.','-')+'-dropdown-'+$scope.timestamp)
            .contains(event.target);
          
          if (!isChild) {
            $timeout(function() {
              $scope.$apply('closeDropdown()');
            });
          }
        };

        $document.on('click', onDocumentClick);
        $element.on('$destroy', function () {
          $document.off('click', onDocumentClick);
        });
      }
    };
  }])

  /**
   * Directive specialized for vocabulary of type String
   */
  .directive('stringCriterionTerms', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criterion: '=',
        query: '=',
        state: '='
      },
      controller: 'StringCriterionTermsController',
      templateUrl: 'search/views/criteria/criterion-string-terms-template.html'
    };
  }])

  /**
   * Directive specialized for vocabulary of type String
   */
  .directive('matchCriterion', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criterion: '=',
        query: '=',
        state: '='
      },
      controller: 'MatchCriterionTermsController',
      templateUrl: 'search/views/criteria/criterion-match-template.html'
    };
  }])

  .directive('searchResultPagination', [function() {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        target: '=',
        totalHits: '=',
        onChange: '='
      },
      controller: 'SearchResultPaginationController',
      templateUrl: 'search/views/list/search-result-pagination-template.html'
    };
  }])

  .directive('taxonomiesFacetsPanel',[function() {
    return {
      restrict: 'EA',
      scope: {
        facetedTaxonomies: '=',
        onRefresh: '=',
        onSelectTerm: '=',
        lang: '=',
        criteria: '='
      },
      controller: 'TaxonomiesFacetsController',
      templateUrl: 'search/views/classifications/taxonomies-facets-view.html'
    };
  }])
  
  .directive('taxonomiesPanel',[function() {
    return {
    restrict: 'EA',
    replace: true,
    scope: {
      taxonomyName: '=',
      target: '=',
      onClose: '=',
      onSelectTerm: '=',
      taxonomiesShown: '=',
      lang: '='
    },
    controller: 'TaxonomiesPanelController',
    templateUrl: 'search/views/classifications/taxonomies-view.html',
    link: function(scope, element) {
      scope.closeTaxonomies = function () {
        element.collapse('hide');
        scope.onClose();
      };

      scope.showTaxonomies = function() {
        element.collapse('show');
      };

      element.on('show.bs.collapse', function () {
        scope.taxonomiesShown = true;
      });

      element.on('hide.bs.collapse', function () {
        scope.taxonomiesShown = false;
      });

      scope.$watch('taxonomiesShown', function(value) {
        if(value) {
          element.collapse('show');
        } else {
          element.collapse('hide');
        }
      });

      }
    };
  }])

  .directive('classificationsPanel',[function() {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        target: '=',
        onSelectTerm: '=',
        isHistoryEnabled: '=',
        lang: '='
      },
      controller: 'ClassificationPanelController',
      templateUrl: 'search/views/classifications/classifications-view.html'
    };
  }])

  .factory('Fullscreen', ['$document', '$window', '$rootScope', function ($document, $window, $rootScope) {
    // based on: https://github.com/fabiobiondi/angular-fullscreen
    var document = $document[0];
    var isKeyboardAvailbleOnFullScreen = (typeof $window.Element !== 'undefined' && 'ALLOW_KEYBOARD_INPUT' in $window.Element) && $window.Element.ALLOW_KEYBOARD_INPUT;
    var emitter = $rootScope.$new();


    var serviceInstance = {
      $on: angular.bind(emitter, emitter.$on),
      enable: function(element) {
        if(element.requestFullScreen) {
          element.requestFullScreen();
        } else if(element.mozRequestFullScreen) {
          element.mozRequestFullScreen();
        } else if(element.webkitRequestFullscreen) {
          // Safari temporary fix
          if (/Version\/[\d]{1,2}(\.[\d]{1,2}){1}(\.(\d){1,2}){0,1} Safari/.test($window.navigator.userAgent)) {
            element.webkitRequestFullscreen();
          } else {
            element.webkitRequestFullscreen(isKeyboardAvailbleOnFullScreen);
          }
        } else if (element.msRequestFullscreen) {
          element.msRequestFullscreen();
        }
      },
      cancel: function() {
        if(document.cancelFullScreen) {
          document.cancelFullScreen();
        } else if(document.mozCancelFullScreen) {
          document.mozCancelFullScreen();
        } else if(document.webkitExitFullscreen) {
          document.webkitExitFullscreen();
        } else if (document.msExitFullscreen) {
          document.msExitFullscreen();
        }
      },
      isEnabled: function(){
        var fullscreenElement = document.fullscreenElement || document.mozFullScreenElement || document.webkitFullscreenElement || document.msFullscreenElement;
        return fullscreenElement ? true : false;
      }
    };

    $document.on('fullscreenchange webkitfullscreenchange mozfullscreenchange MSFullscreenChange', function(){
      emitter.$emit('ngObibaMicaSearch.fullscreenChange', serviceInstance.isEnabled());
    });

    return serviceInstance;
  }])

  .directive('fullscreen', ['Fullscreen', function(Fullscreen) {
    return {
      link : function ($scope, $element, $attrs) {
        if ($attrs.fullscreen) {
          $scope.$watch($attrs.fullscreen, function(value) {
            var isEnabled = Fullscreen.isEnabled();
            if (value && !isEnabled) {
              Fullscreen.enable($element[0]);
              $element.addClass('isInFullScreen');
            } else if (!value && isEnabled) {
              Fullscreen.cancel();
              $element.removeClass('isInFullScreen');
            }
          });
        }
      }
    };
  }]);
;/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.search')
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/search', {
          templateUrl: 'search/views/search.html',
          controller: 'SearchController',
          reloadOnSearch: false
        })
        .when('/classifications', {
          templateUrl: 'search/views/classifications.html',
          controller: 'SearchController',
          reloadOnSearch: false
        });
    }]);
;/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

function GraphicChartsDataProvider() {

  function DataProvider(dataResponse) {
    var data = dataResponse;
    this.getData = function (callback) {
      if (callback) {
        data.$promise.then(callback);
      }
    };
  }

  this.$get = function ($log, JoinQuerySearchResource, ServerErrorUtils, AlertService, GraphicChartsConfig, GraphicChartsQuery) {
    var queryDto = GraphicChartsQuery.queryDtoBuilder(GraphicChartsConfig.getOptions().entityIds, GraphicChartsConfig.getOptions().entityType);

    return new DataProvider(JoinQuerySearchResource.studies({
        query: queryDto
      },
      function onSuccess (response) {
          return response;
      },
      function (response) {
        $log.error('Server error', response);
      }));
  };
}

angular.module('obiba.mica.graphics', [
    'googlechart',
    'obiba.utils',
    'templates-ngObibaMica'
  ])
  .config(['$provide', function ($provide) {
    $provide.provider('GraphicChartsData', GraphicChartsDataProvider);
  }])
  .run(['GraphicChartsConfigurations',
  function (GraphicChartsConfigurations) {
    GraphicChartsConfigurations.setClientConfig();
  }]);
;/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.graphics')

  .directive('obibaChart', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        fieldTransformer: '=',
        chartType: '=',
        chartAggregationName: '=',
        chartEntityDto: '=',
        chartOptionsName: '=',
        chartOptions: '=',
        chartHeader: '=',
        chartTitle: '=',
        chartTitleGraph: '=',
        chartSelectGraphic: '='
      },
      templateUrl: 'graphics/views/charts-directive.html',
      controller: 'GraphicChartsController'
    };
  }])
  .directive('obibaTable', [function () {
  return {
    restrict: 'EA',
    replace: true,
    scope: {
      fieldTransformer: '=',
      chartType: '@',
      chartAggregationName: '=',
      chartEntityDto: '=',
      chartOptionsName: '=',
      chartOptions: '=',
      chartHeader: '=',
      chartTitle: '=',
      chartTitleGraph: '=',
      chartSelectGraphic: '=',
      chartOrdered: '=',
      chartNotOrdered: '='
    },
    templateUrl: 'graphics/views/tables-directive.html',
    controller: 'GraphicChartsController'
  };
}]);;/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.graphics')

  .controller('GraphicChartsController', [
    '$rootScope',
    '$scope',
    '$filter',
    '$window',
    'GraphicChartsConfig',
    'GraphicChartsUtils',
    'GraphicChartsData',
    'RqlQueryService',
    'ngObibaMicaUrl',
    'googleChartApiPromise',
    function ($rootScope,
              $scope,
              $filter,
              $window,
              GraphicChartsConfig,
              GraphicChartsUtils,
              GraphicChartsData,
              RqlQueryService,
              ngObibaMicaUrl,
              googleChartApiPromise) {

      function initializeChartData() {
        $scope.chartObject = {};
        GraphicChartsData.getData(function (StudiesData) {
          if (StudiesData) {
            GraphicChartsUtils.getArrayByAggregation($scope.chartAggregationName, StudiesData[$scope.chartEntityDto])
              .then(function(entries){

                var data = entries.map(function(e) {
                  if(e.participantsNbr) {
                    return [e.title, e.value, e.participantsNbr];
                  }
                  else{
                    return [e.title, e.value];
                  }
                });

                $scope.updateCriteria = function(key, vocabulary) {
                  RqlQueryService.createCriteriaItem('study', 'Mica_study', vocabulary, key).then(function (item) {
                    var entity = GraphicChartsConfig.getOptions().entityType;
                    var id = GraphicChartsConfig.getOptions().entityIds;
                    var parts = item.id.split('.');

                    var urlRedirect = ngObibaMicaUrl.getUrl('GraphicsSearchRootUrl') + '?type=studies&query=' +
                      entity + '(in(Mica_' + entity + '.id,' + id + ')),study(in(' + parts[0] + '.' + parts[1] + ',' +
                      parts[2].replace(':', '%253A') + '))';

                    $window.location.href = ngObibaMicaUrl.getUrl('BaseUrl') + urlRedirect;
                  });
                };

                if (data) {
                  if (/^Table-/.exec($scope.chartType) !== null) {
                    $scope.chartObject.ordered = $scope.chartOrdered;
                    $scope.chartObject.notOrdered = $scope.chartNotOrdered;
                    if($scope.chartHeader.length<3){
                      $scope.chartObject.header = [
                        $filter('translate')($scope.chartHeader[0]),
                        $filter('translate')($scope.chartHeader[1])
                      ];
                    }
                    else{
                      $scope.chartObject.header = [
                        $filter('translate')($scope.chartHeader[0]),
                        $filter('translate')($scope.chartHeader[1]),
                   //     $filter('translate')($scope.chartHeader[2])
                      ];
                    }
                    $scope.chartObject.type = $scope.chartType;
                    $scope.chartObject.data = data;
                    $scope.chartObject.vocabulary = $scope.chartAggregationName;
                    $scope.chartObject.entries = entries;
                  }
                  else {
                    if($scope.chartHeader.length<3){
                      data.unshift([$filter('translate')($scope.chartHeader[0]), $filter('translate')($scope.chartHeader[1])]);
                    }
                    else{
                      data.unshift([
                        $filter('translate')($scope.chartHeader[0]),
                        $filter('translate')($scope.chartHeader[1]),
                   //     $filter('translate')($scope.chartHeader[2])
                      ]);
                    }
                    $scope.chartObject.term = true;
                    $scope.chartObject.type = $scope.chartType;
                    $scope.chartObject.data = data;
                    $scope.chartObject.options = {backgroundColor: {fill: 'transparent'}};
                    angular.extend($scope.chartObject.options, $scope.chartOptions);
                    $scope.chartObject.options.title = $filter('translate')($scope.chartTitleGraph) + ' (N=' + StudiesData.studyResultDto.totalHits + ')';
                    $scope.$parent.directive = {title: $scope.chartObject.options.title};
                  }
                }
              });
          }
        });

      }

      googleChartApiPromise.then(function() {
        $scope.ready = true;
      });

      $scope.$watchGroup(['chartType', 'ready'], function() {
        if ($scope.chartType && $scope.ready) {
          initializeChartData();
        }
      });

    }]);
;/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.graphics')
  .factory('GraphicChartsDataResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('JoinQuerySearchResource'), {}, {
        'studies': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'studies'}
        }
      });
    }])
  .service('GraphicChartsConfig', function () {
    var factory = {
      options: {
        entityIds: 'NaN',
        entityType: null,
        ChartsOptions: {
          geoChartOptions: {
            header : ['graphics.country', 'graphics.nbr-studies'],
            title : 'graphics.geo-chart-title',
            options: {
              backgroundColor: {fill: 'transparent'},
              colors: [
                '#e5edfb',
                '#cfddf5',
                '#b8cbed',
                '#a0b8e2',
                '#88a4d4'
              ]
            }
          },
          studiesDesigns: {
            header: ['graphics.study-design', 'graphics.nbr-studies', 'graphics.number-participants'],
            title : 'graphics.study-design-chart-title',
            options: {
              bars: 'horizontal',
              series: {
                0: { axis: 'nbrStudies' }, // Bind series 1 to an axis
                1: { axis: 'nbrParticipants' } // Bind series 0 to an axis
              },
              axes: {
                x: {
                  nbrStudies: {side: 'top', label: 'Number of Studies'}, // Top x-axis.
                  nbrParticipants: {label: 'Number of Participants'} // Bottom x-axis.
                }
              },
              backgroundColor: {fill: 'transparent'},
              colors: ['#b8cbed',
                '#e5edfb',
                '#cfddf5',
                '#a0b8e2',
                '#88a4d4']
            }
          },
          numberParticipants: {
            header: ['graphics.number-participants', 'graphics.nbr-studies'],
            title: 'graphics.number-participants-chart-title',
            options: {
              backgroundColor: {fill: 'transparent'},
              colors: ['#b8cbed',
                '#e5edfb',
                '#cfddf5',
                '#a0b8e2',
                '#88a4d4'],
              pieSliceTextStyle: {color: '#000000'}
            }
          },
          biologicalSamples: {
            header : ['graphics.bio-samples', 'graphics.nbr-studies'],
            title : 'graphics.bio-samples-chart-title',
            options: {
              bars: 'horizontal',
              series: {
                0: { axis: 'nbrStudies' } // Bind series 1 to an axis
              },
              axes: {
                x: {
                  nbrStudies: {side: 'top', label: 'Number of Studies'} // Top x-axis.
                }
              },
              backgroundColor: {fill: 'transparent'},
              colors: ['#b8cbed',
                '#e5edfb',
                '#cfddf5',
                '#a0b8e2',
                '#88a4d4']
            }
          }

        }

      }
    };
    factory.setOptions = function (newOptions) {
      if (typeof(newOptions) === 'object') {
        Object.keys(newOptions).forEach(function (option) {
          if (option in factory.options) {
            factory.options[option] = newOptions[option];
          }
        });
      }
    };

    factory.getOptions = function () {
      return angular.copy(factory.options);
    };
    return factory;

  })
  .service('GraphicChartsUtils', ['LocalizedValues','TaxonomyResource', '$q',
    function (LocalizedValues, TaxonomyResource, $q) {
      var studyTaxonomy = {};

      studyTaxonomy.getTerms = function (aggregationName) {
        var deferred = $q.defer();

        function getTerms() {
          var terms = null;
          if (studyTaxonomy.vocabularies){
            angular.forEach(studyTaxonomy.vocabularies, function (vocabulary) {
              if (vocabulary.name === aggregationName) {
                terms = vocabulary.terms;
              }
            });
          }

          deferred.resolve(terms);
        }

        if (!studyTaxonomy.vocabularies) {
          TaxonomyResource.get({
            target: 'study',
            taxonomy: 'Mica_study'
          }).$promise.then(function(taxonomy){
            studyTaxonomy.vocabularies = angular.copy(taxonomy.vocabularies);
            getTerms();
          });

        } else {
          getTerms();
        }

        return deferred.promise;
      };

      this.getArrayByAggregation = function (aggregationName, entityDto) {
        var deferred = $q.defer();

        if (!aggregationName || !entityDto) {
          deferred.resolve([]);
        }

        var arrayData = [];
        studyTaxonomy.getTerms(aggregationName).then(function(terms) {
          var sortedTerms = terms;
          var i = 0;
          angular.forEach(entityDto.aggs, function (aggregation) {
            if (aggregation.aggregation === aggregationName) {
              if (aggregation['obiba.mica.RangeAggregationResultDto.ranges']) {
                i = 0;
                angular.forEach(sortedTerms, function (sortTerm) {
                  angular.forEach(aggregation['obiba.mica.RangeAggregationResultDto.ranges'], function (term) {
                    if (sortTerm.name === term.key) {
                      if (term.count) {
                        arrayData[i] = {title: term.title, value: term.count, key: term.key};
                        i++;
                      }
                    }
                  });
                });
              }
              else {
                // MK-924 sort countries by title in the display language
                if (aggregation.aggregation === 'populations-selectionCriteria-countriesIso') {
                  var locale = LocalizedValues.getLocal();
                  sortedTerms.sort(function(a, b) {
                    var textA = LocalizedValues.forLocale(a.title, locale);
                    var textB = LocalizedValues.forLocale(b.title, locale);
                    return (textA < textB) ? -1 : (textA > textB) ? 1 : 0;
                  });
                }
                var numberOfParticipant = 0;
                i = 0;
                angular.forEach(sortedTerms, function (sortTerm) {
                  angular.forEach(aggregation['obiba.mica.TermsAggregationResultDto.terms'], function (term) {
                    if (sortTerm.name === term.key) {
                      if (term.count) {
                        if (aggregation.aggregation === 'methods-designs') {
                          arrayData[i] = {
                            title: term.title,
                            value: term.count,
                            participantsNbr: numberOfParticipant,
                            key: term.key
                          };
                        } else {
                          arrayData[i] = {title: term.title, value: term.count, key: term.key};
                        }
                        i++;
                      }
                    }
                  });
                });
              }
            }
          });
          
          deferred.resolve(arrayData);
        });
        return deferred.promise;
      };
    }])

  .service('GraphicChartsQuery', ['RqlQueryService', 'RqlQueryUtils','LocalizedValues', function (RqlQueryService, RqlQueryUtils,LocalizedValues) {
    this.queryDtoBuilder = function (entityIds, entityType) {
      var query;
      if (!(entityIds) || entityIds === 'NaN') {
        query =  'study(exists(Mica_study.id))';
      }
      if(entityType && entityIds !== 'NaN') {
        query =  entityType + '(in(Mica_'+ entityType +'.id,(' + entityIds + ')))';
      }
      var localizedRqlQuery = angular.copy(new RqlParser().parse(query));
      RqlQueryUtils.addLocaleQuery(localizedRqlQuery, LocalizedValues.getLocal());
      var localizedQuery = new RqlQuery().serializeArgs(localizedRqlQuery.args);
      return RqlQueryService.prepareGraphicsQuery(localizedQuery,
        ['Mica_study.populations-selectionCriteria-countriesIso', 'Mica_study.populations-dataCollectionEvents-bioSamples', 'Mica_study.numberOfParticipants-participant-number'],
        ['Mica_study.methods-designs']
      );
    };
  }]);
;'use strict';

angular.module('obiba.mica.localized', [
  'obiba.notification',
  'pascalprecht.translate',
  'templates-ngObibaMica'
]);
;'use strict';

angular.module('obiba.mica.localized')

  .directive('localized', ['LocalizedValues', function (LocalizedValues) {
    return {
      restrict: 'AE',
      scope: {
        value: '=',
        lang: '='
      },
      templateUrl: 'localized/localized-template.html',
      link: function(scope) {
        scope.LocalizedValues = LocalizedValues;
      }
    };
  }])

  .directive('localizedNumber', ['LocalizedValues', function (LocalizedValues) {
    return {
      restrict: 'E',
      scope: {number: '=value'},
      template: '{{LocalizedValues.formatNumber(number)}}',
      link: function($scope) {
        $scope.LocalizedValues = LocalizedValues;
      }
    };
  }])

  .directive('localizedInput', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        label: '@',
        required: '@',
        disabled: '=',
        lang: '=',
        help: '@'
      },
      templateUrl: 'localized/localized-input-template.html',
      link: function ($scope, elem, attr, ctrl) {
        if (angular.isUndefined($scope.model) || $scope.model === null) {
          $scope.model = [
            {lang: $scope.lang, value: ''}
          ];
        }

        $scope.$watch('model', function(newModel) {
          if (angular.isUndefined(newModel) || newModel === null) {
            $scope.model = [{lang: $scope.lang, value: ''}];
          }

          var currentLang = $scope.model.filter(function(e) {
            if (e.lang === $scope.lang) {
              return e;
            }
          });

          if (currentLang.length === 0) {
            $scope.model.push({lang:$scope.lang, value: ''});
          }
        }, true);

        $scope.fieldName = $scope.name + '-' + $scope.lang;
        $scope.form = ctrl;
      }
    };
  }])

  .directive('localizedInputGroup', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        label: '@',
        required: '@',
        disabled: '=',
        lang: '=',
        help: '@',
        remove: '='
      },
      templateUrl: 'localized/localized-input-group-template.html',
      link: function ($scope, elem, attr, ctrl) {
        if (angular.isUndefined($scope.model) || $scope.model === null) {
          $scope.model = [
            {lang: $scope.lang, value: ''}
          ];
        }

        $scope.$watch('model', function(newModel) {
          if (angular.isUndefined(newModel) || newModel === null) {
            $scope.model = [{lang: $scope.lang, value: ''}];
          }

          var currentLang = $scope.model.filter(function(e) {
            if (e.lang === $scope.lang) {
              return e;
            }
          });

          if (currentLang.length === 0) {
            $scope.model.push({lang:$scope.lang, value: ''});
          }
        }, true);

        $scope.fieldName = $scope.name + '-' + $scope.lang;
        $scope.form = ctrl;
      }
    };
  }])

  .directive('localizedTextarea', [function () {
    return {
      restrict: 'AE',
      require: '^form',
      scope: {
        name: '@',
        model: '=',
        label: '@',
        required: '@',
        disabled: '=',
        lang: '=',
        help: '@',
        rows: '@'
      },
      templateUrl: 'localized/localized-textarea-template.html',
      link: function ($scope, elem, attr, ctrl) {
        if (angular.isUndefined($scope.model) || $scope.model === null) {
          $scope.model = [
            {lang: $scope.lang, value: ''}
          ];
        }

        $scope.$watch('model', function(newModel) {
          if (angular.isUndefined(newModel) || newModel === null) {
            $scope.model = [{lang: $scope.lang, value: ''}];
          }

          var currentLang = $scope.model.filter(function(e) {
            if (e.lang === $scope.lang) {
              return e;
            }
          });

          if (currentLang.length === 0) {
            $scope.model.push({lang:$scope.lang, value: ''});
          }
        }, true);

        $scope.fieldName = $scope.name + '-' + $scope.lang;
        $scope.form = ctrl;
      }
    };
  }]);
;/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.localized')

  .service('LocalizedValues',
    function () {
      this.for = function (values, lang, keyLang, keyValue) {
        if (angular.isArray(values)) {
          var result = values.filter(function (item) {
            return item[keyLang] === lang;
          });

          if (result && result.length > 0) {
            return result[0][keyValue];
          }
        }
        return '';
      };

      this.forLocale = function (values, lang) {
        var rval = this.for(values, lang, 'locale', 'text');
        if (rval === '') {
          rval = this.for(values, 'und', 'locale', 'text');
        }
        return rval;
      };

      this.forLang = function (values, lang) {
        var rval = this.for(values, lang, 'lang', 'value');
        if (rval === '') {
          rval = this.for(values, 'und', 'lang', 'value');
        }
        return rval;
      };

      this.getLocal = function () {
        return 'en';
      };

      this.formatNumber = function (number){
        return number.toLocaleString(this.getLocal());
      };

    });
;/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.localized')

  .filter('localizedNumber', ['LocalizedValues', function(LocalizedValues) {
    return function(value){
      return value ? LocalizedValues.formatNumber(value) : '';
    };
  }]);
;'use strict';

function NgObibaMicaFileBrowserOptionsProvider() {
  var options = {
    locale: 'en',
    downloadInline: true,
    folders: {
      excludes: ['population']
    }
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
;/*
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
  
  .directive('fileBrowser', [function () {
    return {
      restrict: 'EA',
      replace: true,
      controller: 'FileBrowserController',
      scope: {
        docPath: '@',
        docId: '@',
        subject: '='
      },
      templateUrl: 'file-browser/views/file-browser-template.html'
    };
  }]);
;/*
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

      var navigateToPath = function (path) {
        clearSearchInternal();
        getDocument(path);
      };

      var navigateTo = function (event, document) {
        event.stopPropagation();
        if (document) {
          navigateToPath(document.path);
        }
      };

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

      function clearSearchInternal() {
        $scope.data.search.text = null;
        $scope.data.search.active = false;
      }

      function getDocument(path) {
        $scope.data.search.active = false;

        FileBrowserFileResource.get({path: path},
          function onSuccess(response) {
            $log.info(response);
            $scope.pagination.selected = -1;
            $scope.data.document = $scope.data.details.document = response;

            if (!$scope.data.document.children) {
              $scope.data.document.children = [];
            }

            if ($scope.data.document.path === $scope.data.rootPath) {
              $scope.data.document.children = $scope.data.document.children.filter(function(child){
                return ngObibaMicaFileBrowserOptions.folders.excludes.indexOf(child.name) < 0;
              });
              $scope.data.document.size = $scope.data.document.children.length;
            }

            $scope.data.breadcrumbs = BrowserBreadcrumbHelper.toArray(path, $scope.data.rootPath);
            $scope.data.isFile = FileBrowserService.isFile(response);
            $scope.data.isRoot = FileBrowserService.isRoot(response);
          },
          onError
        );
      }

      function navigateToParent(event, document) {
        event.stopPropagation();
        var path = document.path;

        if (path.lastIndexOf('/') === 0) {
          path = '/';
        } else {
          path = path.substring(0, path.lastIndexOf('/'));
        }

        navigateToPath(path);
      }

      function navigateBack() {
        if (!$scope.data.isRoot && $scope.data.document) {
          var parentPath = $scope.data.document.path.replace(/\\/g, '/').replace(/\/[^\/]*$/, '');
          getDocument(parentPath ? parentPath : '/');
        }
      }

      function hideDetails() {
        $scope.pagination.selected = -1;
        $scope.data.details.show = false;
      }

      function searchDocumentsInternal(path, searchParams) {
        function excludeFolders(query) {
          var excludeQuery = '';
          try {
            var excludes = [];
            ngObibaMicaFileBrowserOptions.folders.excludes.forEach(function (exclude) {
              var q = path.replace(/\//g, '\\/') + '\\/' + exclude.replace(/\s/, '\\ ');
              excludes.push(q);
              excludes.push(q + '\\/*');
            });

            excludeQuery = excludes.length > 0 ? 'NOT path:(' + excludes.join(' OR ') + ')' : '';
          } catch (error) {
            // just return the input query
          }

          return query ? query + ' AND ' + excludeQuery : excludeQuery;
        }

        searchParams.query = excludeFolders(searchParams.query);

        var urlParams = angular.extend({}, {path: path}, searchParams);

        FileBrowserSearchResource.search(urlParams,
            function onSuccess(response) {
              $log.info('Search result', response);
              var clone = $scope.data.document ? angular.copy($scope.data.document) : {};
              clone.children = response;
              $scope.data.document = clone;
            },
            function onError(response) {
              $log.debug('ERROR:',response);
            }
        );
      }

      var searchDocuments = function (query) {
        $scope.data.search.active = true;
        hideDetails();
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

      var toggleRecursively = function () {
        $scope.data.search.recursively = !$scope.data.search.recursively;
        if ($scope.data.search.text) {
          searchDocuments($scope.data.search.text);
        } else if ($scope.data.search.query) {
          searchDocuments($scope.data.search.query);
        }
      };

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

      var showDetails = function(document, index) {
        $scope.pagination.selected = index;
        $scope.data.details.document = document;
        $scope.data.details.show = true;
      };

      var getTypeParts = function(document) {
        return FileBrowserService.isFile(document) && document.attachment.type ?
          document.attachment.type.split(/,|\s+/) :
          [];
      };

      var getLocalizedValue = function(values) {
        return FileBrowserService.getLocalizedValue(values, ngObibaMicaFileBrowserOptions.locale);
      };

      $scope.downloadTarget = ngObibaMicaFileBrowserOptions.downloadInline ? '_blank' : '_self';
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
      $scope.hideDetails = hideDetails;
      $scope.showDetails = showDetails;
      $scope.getTypeParts = getTypeParts;

      $scope.pagination = {
        selected: -1,
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

;/*
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
;angular.module('templates-ngObibaMica', ['access/views/data-access-request-form.html', 'access/views/data-access-request-histroy-view.html', 'access/views/data-access-request-list.html', 'access/views/data-access-request-profile-user-modal.html', 'access/views/data-access-request-submitted-modal.html', 'access/views/data-access-request-validation-modal.html', 'access/views/data-access-request-view.html', 'attachment/attachment-input-template.html', 'attachment/attachment-list-template.html', 'file-browser/views/document-detail-template.html', 'file-browser/views/documents-table-template.html', 'file-browser/views/file-browser-template.html', 'file-browser/views/toolbar-template.html', 'graphics/views/charts-directive.html', 'graphics/views/tables-directive.html', 'localized/localized-input-group-template.html', 'localized/localized-input-template.html', 'localized/localized-template.html', 'localized/localized-textarea-template.html', 'search/views/classifications.html', 'search/views/classifications/classifications-view.html', 'search/views/classifications/taxonomies-facets-view.html', 'search/views/classifications/taxonomies-view.html', 'search/views/classifications/taxonomy-accordion-group.html', 'search/views/classifications/taxonomy-panel-template.html', 'search/views/classifications/taxonomy-template.html', 'search/views/classifications/term-panel-template.html', 'search/views/classifications/vocabulary-accordion-group.html', 'search/views/classifications/vocabulary-panel-template.html', 'search/views/coverage/coverage-search-result-table-template.html', 'search/views/criteria/criteria-node-template.html', 'search/views/criteria/criteria-root-template.html', 'search/views/criteria/criteria-target-template.html', 'search/views/criteria/criterion-dropdown-template.html', 'search/views/criteria/criterion-header-template.html', 'search/views/criteria/criterion-match-template.html', 'search/views/criteria/criterion-numeric-template.html', 'search/views/criteria/criterion-string-terms-template.html', 'search/views/criteria/target-template.html', 'search/views/graphics/graphics-search-result-template.html', 'search/views/list/datasets-search-result-table-template.html', 'search/views/list/networks-search-result-table-template.html', 'search/views/list/pagination-template.html', 'search/views/list/search-result-pagination-template.html', 'search/views/list/studies-search-result-table-template.html', 'search/views/list/variables-search-result-table-template.html', 'search/views/search-result-coverage-template.html', 'search/views/search-result-graphics-template.html', 'search/views/search-result-list-dataset-template.html', 'search/views/search-result-list-network-template.html', 'search/views/search-result-list-study-template.html', 'search/views/search-result-list-template.html', 'search/views/search-result-list-variable-template.html', 'search/views/search-result-panel-template.html', 'search/views/search.html', 'views/pagination-template.html']);

angular.module("access/views/data-access-request-form.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("access/views/data-access-request-form.html",
    "<!--\n" +
    "  ~ Copyright (c) 2015 OBiBa. All rights reserved.\n" +
    "  ~\n" +
    "  ~ This program and the accompanying materials\n" +
    "  ~ are made available under the terms of the GNU Public License v3.0.\n" +
    "  ~\n" +
    "  ~ You should have received a copy of the GNU General Public License\n" +
    "  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.\n" +
    "  -->\n" +
    "\n" +
    "<div>\n" +
    "\n" +
    "  <div ng-if=\"headerTemplateUrl\" ng-include=\"headerTemplateUrl\"></div>\n" +
    "\n" +
    "  <obiba-alert id=\"DataAccessRequestEditController\"></obiba-alert>\n" +
    "\n" +
    "  <div ng-if=\"validForm\">\n" +
    "\n" +
    "    <div class=\"pull-right\">\n" +
    "      <a ng-click=\"cancel()\" type=\"button\" class=\"btn btn-default\">\n" +
    "        <span translate>cancel</span>\n" +
    "      </a>\n" +
    "\n" +
    "      <a ng-click=\"save()\" type=\"button\" class=\"btn btn-primary\">\n" +
    "        <span translate>save</span>\n" +
    "      </a>\n" +
    "\n" +
    "      <a ng-click=\"validate()\" type=\"button\" class=\"btn btn-info\">\n" +
    "        <span translate>validate</span>\n" +
    "      </a>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"clearfix\"></div>\n" +
    "\n" +
    "    <form name=\"form.requestForm\" ng-submit=\"submit(form.requestForm)\">\n" +
    "      <div sf-model=\"form.model\" sf-form=\"form.definition\" sf-schema=\"form.schema\" required=\"true\"></div>\n" +
    "      <h2>{{config.documentsSectionTitle || 'data-access-request.documents' | translate}}</h2>\n" +
    "      <p>{{config.documentsSectionHelpText || 'data-access-request.documents-help' | translate}}</p>\n" +
    "      <div class=\"form-group\">\n" +
    "        <attachment-input files=\"dataAccessRequest.attachments\" multiple=\"true\"></attachment-input>\n" +
    "      </div>\n" +
    "    </form>\n" +
    "  </div>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("access/views/data-access-request-histroy-view.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("access/views/data-access-request-histroy-view.html",
    "<!--\n" +
    "  ~ Copyright (c) 2015 OBiBa. All rights reserved.\n" +
    "  ~\n" +
    "  ~ This program and the accompanying materials\n" +
    "  ~ are made available under the terms of the GNU Public License v3.0.\n" +
    "  ~\n" +
    "  ~ You should have received a copy of the GNU General Public License\n" +
    "  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.\n" +
    "  -->\n" +
    "\n" +
    "<table id=\"data-access-request-history\" class=\"table table-striped\">\n" +
    "  <thead>\n" +
    "  <tr>\n" +
    "    <th class=\"status-icon\"></th>\n" +
    "    <th translate>status</th>\n" +
    "    <th translate>changed-by</th>\n" +
    "    <th translate>Changed On</th>\n" +
    "  </tr>\n" +
    "  </thead>\n" +
    "  <tbody>\n" +
    "  <tr ng-repeat=\"status in dataAccessRequest.statusChangeHistory\"\n" +
    "    ng-init=\"info = getStatusHistoryInfo[getStatusHistoryInfoId(status)]\">\n" +
    "    <td><span><i class=\"{{info.icon}} hoffset\"></i></span></td>\n" +
    "    <td>{{info.msg}}</span></span></td>\n" +
    "    <td>{{userProfileService.getFullName(status.profile) || status.author}}</td>\n" +
    "    <td><span>{{status.changedOn | fromNow}}</span></td>\n" +
    "  </tr>\n" +
    "  </tbody>\n" +
    "</table>\n" +
    "");
}]);

angular.module("access/views/data-access-request-list.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("access/views/data-access-request-list.html",
    "<!--\n" +
    "  ~ Copyright (c) 2015 OBiBa. All rights reserved.\n" +
    "  ~\n" +
    "  ~ This program and the accompanying materials\n" +
    "  ~ are made available under the terms of the GNU Public License v3.0.\n" +
    "  ~\n" +
    "  ~ You should have received a copy of the GNU General Public License\n" +
    "  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.\n" +
    "  -->\n" +
    "\n" +
    "<div id=\"data-access-request-list\">\n" +
    "  <div ng-if=\"headerTemplateUrl\" ng-include=\"headerTemplateUrl\"></div>\n" +
    "\n" +
    "  <a ng-href=\"#/data-access-request/new\" class=\"btn btn-info\">\n" +
    "    <i class=\"fa fa-plus\"></i> <span>{{config.newRequestButtonCaption || 'data-access-request.add' | translate}}</span>\n" +
    "  </a>\n" +
    "\n" +
    "  <p class=\"help-block\" ng-if=\"requests.length == 0 && !loading\">\n" +
    "    <span translate>data-access-request.none</span>\n" +
    "  </p>\n" +
    "\n" +
    "  <p ng-if=\"loading\" class=\"voffset2 loading\">\n" +
    "  </p>\n" +
    "\n" +
    "  <div ng-if=\"requests.length > 0\">\n" +
    "    <div class=\"row voffset2\">\n" +
    "      <div class=\"col-xs-4\">\n" +
    "        <span class=\"input-group input-group-sm no-padding-top\">\n" +
    "          <span class=\"input-group-addon\" id=\"data-access-requests-search\"><i\n" +
    "              class=\"glyphicon glyphicon-search\"></i></span>\n" +
    "          <input ng-model=\"searchText\" type=\"text\" class=\"form-control\"\n" +
    "              aria-describedby=\"data-access-requests-search\">\n" +
    "        </span>\n" +
    "      </div>\n" +
    "      <div class=\"col-xs-2\">\n" +
    "        <div class=\"input-group\">\n" +
    "          <ui-select id=\"status-select\" theme=\"bootstrap\"\n" +
    "              ng-model=\"searchStatus.filter\" reset-search-input=\"true\">\n" +
    "            <ui-select-match allow-clear=\"true\"\n" +
    "                placeholder=\"{{'data-access-request.status-placeholder' | translate}}\">\n" +
    "              <span ng-bind-html=\"$select.selected\"></span>\n" +
    "            </ui-select-match>\n" +
    "            <ui-select-choices repeat=\"data in REQUEST_STATUS\">\n" +
    "              {{data}}\n" +
    "            </ui-select-choices>\n" +
    "          </ui-select>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "      <div class=\"col-xs-6\">\n" +
    "        <dir-pagination-controls class=\"pull-right\"></dir-pagination-controls>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"table-responsive\">\n" +
    "      <table class=\"table table-bordered table-striped\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th>ID</th>\n" +
    "          <th translate ng-if=\"showApplicant\">data-access-request.applicant</th>\n" +
    "          <th translate>data-access-request.title</th>\n" +
    "          <th translate>data-access-request.lastUpdate</th>\n" +
    "          <th translate>data-access-request.submissionDate</th>\n" +
    "          <th translate>data-access-request.status</th>\n" +
    "          <th translate>actions</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "        <tr\n" +
    "            dir-paginate=\"request in requests | filter:{status: searchStatus.filter} | filter:searchText | itemsPerPage: 20\">\n" +
    "          <td>\n" +
    "            <a ng-href=\"#/data-access-request/{{request.id}}\"\n" +
    "                ng-if=\"actions.canView(request)\" translate>{{request.id}}</a>\n" +
    "            <span ng-if=\"!actions.canView(request)\">{{request.id}}</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"showApplicant\">\n" +
    "            <span ng-if=\"!request.profile.attributes\">\n" +
    "              {{request.applicant}}\n" +
    "            </span>\n" +
    "            <span ng-if=\"request.profile.attributes && actions.canViewProfile('mica-user') && !actions.canViewProfile('mica-data-access-officer')\">\n" +
    "              {{getFullName(request.profile) || request.applicant}}\n" +
    "            </span>\n" +
    "            <a href ng-click=\"userProfile(request.profile)\" ng-if=\"request.profile.attributes && actions.canViewProfile('mica-data-access-officer')\">\n" +
    "              {{getFullName(request.profile) || request.applicant}}\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{request.title}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{(request.timestamps.lastUpdate || request.timestamps.created) |\n" +
    "            fromNow}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <span ng-if=\"request.submissionDate\">{{request.submissionDate | fromNow}}</span>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{request.status | translate}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <ul class=\"list-inline\">\n" +
    "              <li ng-if=\"actions.canEdit(request)\">\n" +
    "                <a ng-href=\"#/data-access-request/{{request.id}}/edit\"\n" +
    "                    title=\"{{'edit' | translate}}\"><i class=\"fa fa-pencil\"></i></a>\n" +
    "              </li>\n" +
    "              <li>\n" +
    "                <a ng-if=\"actions.canDelete(request)\"\n" +
    "                    ng-click=\"deleteRequest(request)\"\n" +
    "                    title=\"{{'delete' | translate}}\"><i\n" +
    "                    class=\"fa fa-trash-o\"></i></a>\n" +
    "              </li>\n" +
    "            </ul>\n" +
    "          </td>\n" +
    "        </tr>\n" +
    "        </tbody>\n" +
    "      </table>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("access/views/data-access-request-profile-user-modal.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("access/views/data-access-request-profile-user-modal.html",
    "<div class=\"modal-content\">\n" +
    "  <div class=\"modal-header\">\n" +
    "    <button type=\"button\" class=\"close\" aria-hidden=\"true\"\n" +
    "      ng-click=\"$dismiss()\">&times;</button>\n" +
    "    <h4 class=\"modal-title\">\n" +
    "      {{'data-access-request.profile.title' | translate}}\n" +
    "    </h4>\n" +
    "  </div>\n" +
    "  <div class=\"modal-body\">\n" +
    "\n" +
    "    <table class=\"table table-bordered table-striped\">\n" +
    "      <tbody>\n" +
    "      <tr>\n" +
    "        <th>{{'data-access-request.profile.name' | translate}}</th>\n" +
    "        <td>{{getFullName(applicant)}}</td>\n" +
    "      </tr>\n" +
    "      <tr>\n" +
    "        <th>{{'data-access-request.profile.email' | translate}}</th>\n" +
    "        <td>{{getProfileEmail(applicant)}}</td>\n" +
    "      </tr>\n" +
    "      <tr ng-repeat=\"attribute in applicant.attributes | filterProfileAttributes\">\n" +
    "        <th>{{attribute.key}}</th>\n" +
    "        <td>{{attribute.value}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "\n" +
    "    <a class=\"btn btn-default\" ng-if=\"getProfileEmail(applicant)\" href=\"mailto:{{getProfileEmail(applicant)}}\" target=\"_blank\">\n" +
    "      {{'data-access-request.profile.send-email' | translate}}</a>\n" +
    "  </div>\n" +
    "  <div class=\"modal-footer\">\n" +
    "    <button type=\"button\" class=\"btn btn-primary voffest4\"\n" +
    "      ng-click=\"$dismiss()\">\n" +
    "      <span ng-hide=\"confirm.close\" translate>close</span>\n" +
    "      {{confirm.close}}\n" +
    "    </button>\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("access/views/data-access-request-submitted-modal.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("access/views/data-access-request-submitted-modal.html",
    "<?php\n" +
    "/**\n" +
    " * @file\n" +
    " * Code for the obiba_mica_data_access_request modules.\n" +
    " */\n" +
    "\n" +
    "?>\n" +
    "<!--\n" +
    "  ~ Copyright (c) 2015 OBiBa. All rights reserved.\n" +
    "  ~\n" +
    "  ~ This program and the accompanying materials\n" +
    "  ~ are made available under the terms of the GNU Public License v3.0.\n" +
    "  ~\n" +
    "  ~ You should have received a copy of the GNU General Public License\n" +
    "  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.\n" +
    "  -->\n" +
    "\n" +
    "<div class=\"modal-content\">\n" +
    "  <div class=\"modal-header\">\n" +
    "    <button type=\"button\" class=\"close\" aria-hidden=\"true\" ng-click=\"$dismiss()\">&times;</button>\n" +
    "    <h4 class=\"modal-title\">\n" +
    "      <i class=\"fa fa-check fa-lg\"></i>\n" +
    "      {{'data-access-request.submit-confirmation.title' | translate}}\n" +
    "    </h4>\n" +
    "  </div>\n" +
    "  <div class=\"modal-body\">\n" +
    "    <p>{{'data-access-request.submit-confirmation.message' | translate}}</p>\n" +
    "  </div>\n" +
    "  <div class=\"modal-footer\">\n" +
    "    <button type=\"button\" class=\"btn btn-primary voffest4\" ng-click=\"$dismiss()\">\n" +
    "      <span ng-hide=\"confirm.ok\" translate>ok</span>\n" +
    "      {{confirm.ok}}\n" +
    "    </button>\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("access/views/data-access-request-validation-modal.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("access/views/data-access-request-validation-modal.html",
    "<div class=\"modal-content\">\n" +
    "  <div class=\"modal-header\">\n" +
    "    <button type=\"button\" class=\"close\" aria-hidden=\"true\" ng-click=\"$dismiss()\">&times;</button>\n" +
    "    <h4 ng-if=\"form.requestForm.$valid\" class=\"modal-title\">\n" +
    "      <i class=\"fa fa-check fa-lg\"></i>\n" +
    "      {{'data-access-request.validation.title-success' | translate}}\n" +
    "    </h4>\n" +
    "    <h4 ng-if=\"!form.requestForm.$valid\" class=\"modal-title\">\n" +
    "      <i class=\"fa fa-times fa-lg\"></i>\n" +
    "      {{'data-access-request.validation.title-error' | translate}}\n" +
    "    </h4>\n" +
    "  </div>\n" +
    "  <div class=\"modal-body\">\n" +
    "    <p ng-if=\"form.requestForm.$valid\">{{'data-access-request.validation.success' | translate}}</p>\n" +
    "    <p ng-if=\"!form.requestForm.$valid\" translate>{{'data-access-request.validation.error' | translate}}</p>\n" +
    "  </div>\n" +
    "  <div class=\"modal-footer\">\n" +
    "    <button type=\"button\" class=\"btn btn-primary voffest4\" ng-click=\"$dismiss()\">\n" +
    "      <span ng-hide=\"confirm.ok\" translate>ok</span>\n" +
    "      {{confirm.ok}}\n" +
    "    </button>\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("access/views/data-access-request-view.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("access/views/data-access-request-view.html",
    "<!--\n" +
    "  ~ Copyright (c) 2015 OBiBa. All rights reserved.\n" +
    "  ~\n" +
    "  ~ This program and the accompanying materials\n" +
    "  ~ are made available under the terms of the GNU Public License v3.0.\n" +
    "  ~\n" +
    "  ~ You should have received a copy of the GNU General Public License\n" +
    "  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.\n" +
    "  -->\n" +
    "\n" +
    "<div>\n" +
    "  <div ng-if=\"headerTemplateUrl\" ng-include=\"headerTemplateUrl\"></div>\n" +
    "\n" +
    "  <obiba-alert id=\"DataAccessRequestViewController\"></obiba-alert>\n" +
    "\n" +
    "  <div ng-if=\"validForm\">\n" +
    "\n" +
    "    <p class=\"help-block pull-left\"><span translate>created-by</span>\n" +
    "       <span ng-if=\"actions.canViewProfile('mica-user')\">\n" +
    "         {{getFullName(dataAccessRequest.profile) || dataAccessRequest.applicant}}\n" +
    "      </span>\n" +
    "      <a href ng-click=\"userProfile(dataAccessRequest.profile)\"\n" +
    "          ng-if=\"actions.canViewProfile('mica-data-access-officer')\">\n" +
    "        {{getFullName(dataAccessRequest.profile) || dataAccessRequest.applicant}}\n" +
    "      </a>,\n" +
    "      <span>{{dataAccessRequest.timestamps.created | amCalendar}}</span>\n" +
    "      <span class=\"label label-success\">{{dataAccessRequest.status | translate}}</span></p>\n" +
    "\n" +
    "    <div class=\"pull-right\">\n" +
    "      <a ng-click=\"submit()\"\n" +
    "        ng-if=\"actions.canEditStatus(dataAccessRequest) && nextStatus.canSubmit(dataAccessRequest)\"\n" +
    "        class=\"btn btn-info\" translate>submit\n" +
    "      </a>\n" +
    "      <a ng-click=\"reopen()\"\n" +
    "        ng-if=\"actions.canEditStatus(dataAccessRequest) && nextStatus.canReopen(dataAccessRequest)\"\n" +
    "        class=\"btn btn-info\" translate>reopen\n" +
    "      </a>\n" +
    "      <a ng-click=\"review()\"\n" +
    "        ng-if=\"actions.canEditStatus(dataAccessRequest) && nextStatus.canReview(dataAccessRequest)\"\n" +
    "        class=\"btn btn-info\" translate>review\n" +
    "      </a>\n" +
    "      <a ng-click=\"approve()\"\n" +
    "        ng-if=\"actions.canEditStatus(dataAccessRequest) && nextStatus.canApprove(dataAccessRequest)\"\n" +
    "        class=\"btn btn-info\" translate>approve\n" +
    "      </a>\n" +
    "      <a ng-click=\"reject()\"\n" +
    "        ng-if=\"actions.canEditStatus(dataAccessRequest) && nextStatus.canReject(dataAccessRequest)\"\n" +
    "        class=\"btn btn-info\" translate>reject\n" +
    "      </a>\n" +
    "      <a ng-href=\"#/data-access-request/{{dataAccessRequest.id}}/edit\"\n" +
    "        ng-if=\"actions.canEdit(dataAccessRequest)\"\n" +
    "        class=\"btn btn-primary\" title=\"{{'edit' | translate}}\">\n" +
    "        <i class=\"fa fa-pencil-square-o\"></i>\n" +
    "      </a>\n" +
    "      <a target=\"_self\" href=\"{{requestDownloadUrl}}\" class=\"btn btn-default\">\n" +
    "        <i class=\"glyphicon glyphicon-download-alt\"></i> <span>{{config.downloadButtonCaption || 'download' | translate}}</span>\n" +
    "      </a>\n" +
    "      <a ng-click=\"delete()\"\n" +
    "        ng-if=\"actions.canDelete(dataAccessRequest)\"\n" +
    "        class=\"btn btn-danger\" title=\"{{'delete' | translate}}\">\n" +
    "        <i class=\"fa fa-trash-o\"></i>\n" +
    "      </a>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"clearfix\"></div>\n" +
    "\n" +
    "    <uib-tabset class=\"voffset5\">\n" +
    "      <uib-tab ng-click=\"selectTab('form')\" heading=\"{{'data-access-request.form' | translate}}\">\n" +
    "        <form id=\"request-form\" name=\"forms.requestForm\">\n" +
    "          <div sf-model=\"form.model\" sf-form=\"form.definition\" sf-schema=\"form.schema\"></div>\n" +
    "        </form>\n" +
    "        <h2>{{config.documentsSectionTitle || 'data-access-request.documents' | translate}}</h2>\n" +
    "\n" +
    "        <p ng-if=\"dataAccessRequest.attachments.length == 0\" translate>\n" +
    "          data-access-request.no-documents\n" +
    "        </p>\n" +
    "\n" +
    "        <attachment-list files=\"dataAccessRequest.attachments\"\n" +
    "            href-builder=\"getDownloadHref(attachments, id)\"></attachment-list>\n" +
    "      </uib-tab>\n" +
    "      <uib-tab ng-if=\"config.commentsEnabled\" ng-click=\"selectTab('comments')\" heading=\"{{'data-access-request.comments' | translate}}\">\n" +
    "        <obiba-comments comments=\"form.comments\" on-update=\"updateComment\" on-delete=\"deleteComment\" name-resolver=\"userProfileService.getFullName\" edit-action=\"EDIT\" delete-action=\"DELETE\"></obiba-comments>\n" +
    "        <obiba-comment-editor on-submit=\"submitComment\"></obiba-comment-editor>\n" +
    "      </uib-tab>\n" +
    "      <uib-tab ng-click=\"selectTab('history')\" heading=\"{{'data-access-request.history' | translate}}\">\n" +
    "        <div ng-include=\"'access/views/data-access-request-histroy-view.html'\"></div>\n" +
    "      </uib-tab>\n" +
    "    </uib-tabset>\n" +
    "  </div>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("attachment/attachment-input-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("attachment/attachment-input-template.html",
    "<button type=\"button\" class=\"btn btn-primary btn-xs\" aria-hidden=\"true\" ngf-multiple=\"{{multiple}}\" ngf-select\n" +
    "        ngf-change=\"onFileSelect($files)\" translate>file.upload.button\n" +
    "</button>\n" +
    "\n" +
    "<table ng-show=\"files.length\" class=\"table table-striped\">\n" +
    "  <tbody>\n" +
    "  <tr ng-repeat=\"file in files\">\n" +
    "    <td>\n" +
    "      {{file.fileName}}\n" +
    "      <uib-progressbar ng-show=\"file.showProgressBar\" class=\"progress-striped\" value=\"file.progress\">\n" +
    "        {{file.progress}}%\n" +
    "      </uib-progressbar>\n" +
    "    </td>\n" +
    "    <td>\n" +
    "      {{file.size | bytes}}\n" +
    "    </td>\n" +
    "    <td>\n" +
    "      <a ng-show=\"file.id\" ng-click=\"deleteFile(file.id)\" class=\"action\">\n" +
    "        <i class=\"fa fa-trash-o\"></i>\n" +
    "      </a>\n" +
    "      <a ng-show=\"file.tempId\" ng-click=\"deleteTempFile(file.tempId)\" class=\"action\">\n" +
    "        <i class=\"fa fa-trash-o\"></i>\n" +
    "      </a>\n" +
    "    </td>\n" +
    "  </tr>\n" +
    "  </tbody>\n" +
    "</table>\n" +
    "");
}]);

angular.module("attachment/attachment-list-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("attachment/attachment-list-template.html",
    "<table class=\"table table-bordered table-striped\" ng-show=\"attachments.length\">\n" +
    "  <tbody>\n" +
    "  <tr ng-repeat=\"attachment in attachments\">\n" +
    "    <th>\n" +
    "      <a target=\"_self\" ng-href=\"{{attachment.href}}\"\n" +
    "        download=\"{{attachment.fileName}}\">{{attachment.fileName}}\n" +
    "      </a>\n" +
    "    </th>\n" +
    "    <td>\n" +
    "      {{attachment.size | bytes}}\n" +
    "    </td>\n" +
    "  </tr>\n" +
    "  </tbody>\n" +
    "</table>\n" +
    "");
}]);

angular.module("file-browser/views/document-detail-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("file-browser/views/document-detail-template.html",
    "<!--\n" +
    "  ~ Copyright (c) 2016 OBiBa. All rights reserved.\n" +
    "  ~\n" +
    "  ~ This program and the accompanying materials\n" +
    "  ~ are made available under the terms of the GNU Public License v3.0.\n" +
    "  ~\n" +
    "  ~ You should have received a copy of the GNU General Public License\n" +
    "  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.\n" +
    "  -->\n" +
    "\n" +
    "<div class=\"panel panel-default\">\n" +
    "  <div class=\"panel-heading\">\n" +
    "    <span>\n" +
    "      <span title=\"{{data.details.document.name}}\">\n" +
    "        <i class=\"fa {{getDocumentIcon(data.details.document)}}\"></i> {{truncate(data.details.document.name, 30)}}\n" +
    "      </span>\n" +
    "      <a href class=\"pull-right\" ng-click=\"hideDetails()\"><i class=\"fa fa-times\"></i></a>\n" +
    "    </span>\n" +
    "  </div>\n" +
    "  <div class=\"panel-body\">\n" +
    "    <div>\n" +
    "      <label class=\"text-muted no-margin\">\n" +
    "        <small>{{'size' | translate}}</small>\n" +
    "      </label>\n" +
    "      <div>\n" +
    "        <span ng-if=\"!isFile(data.details.document)\">{{data.details.document.size}} {{data.details.document.size === 1 ? 'item' : 'items' | translate}}</span>\n" +
    "        <span ng-if=\"isFile(data.details.document)\">{{data.details.document.size | bytes}}</span>\n" +
    "        <a target=\"{{downloadTarget}}\" ng-href=\"{{getDownloadUrl(data.details.document.path)}}\" class=\"hoffset2\" title=\"{{'download' | translate}}\">\n" +
    "          <span><i class=\"fa fa-download\"></i><span class=\"hoffset2\"></span></span>\n" +
    "        </a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"voffset2\">\n" +
    "      <label class=\"text-muted no-margin\">\n" +
    "        <small>{{'created-on' | translate}}</small>\n" +
    "      </label>\n" +
    "      <div>\n" +
    "        <span>{{data.details.document.timestamps.created | amDateFormat : 'lll'}}</span>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div class=\"voffset2\">\n" +
    "      <label class=\"text-muted no-margin\">\n" +
    "        <small>{{'last-modified' | translate}}</small>\n" +
    "      </label>\n" +
    "      <div>\n" +
    "        <span>{{data.details.document.timestamps.lastUpdate | amDateFormat : 'lll'}}</span>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div ng-if=\"isFile(data.details.document)\" class=\"voffset2\">\n" +
    "      <div ng-if=\"data.details.document.attachment.type\">\n" +
    "        <label class=\"text-muted no-margin\">\n" +
    "          <small>{{'type' | translate}}</small>\n" +
    "        </label>\n" +
    "        <div>\n" +
    "          <span>{{data.details.document.attachment.type}}</span>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "\n" +
    "      <div ng-init=\"desc = getLocalizedValue(data.details.document.attachment.description)\"\n" +
    "           ng-show=\"desc\"\n" +
    "           class=\"voffset2\">\n" +
    "        <label class=\"text-muted no-margin\">\n" +
    "          <small>{{'description' | translate}}</small>\n" +
    "        </label>\n" +
    "        <div>\n" +
    "          <span>{{desc}}</span>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("file-browser/views/documents-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("file-browser/views/documents-table-template.html",
    "<div class=\"panel panel-default table-responsive table-responsive-dropdown\">\n" +
    "  <div class=\"panel-heading\" ng-if=\"data.search.active\">\n" +
    "      <a class=\"no-text-decoration\" ng-click=\"clearSearch()\">\n" +
    "        <i class=\"fa fa-chevron-left\"> </i>\n" +
    "      </a>\n" +
    "      <span ng-if=\"data.search.recursively\">{{'file.search-results.current-sub' | translate}}</span>\n" +
    "      <span ng-if=\"!data.search.recursively\">{{'file.search-results.current' | translate}}</span>\n" +
    "      ({{data.document.children.length}})\n" +
    "  </div>\n" +
    "  <div ng-if=\"data.document.children.length > 0\">\n" +
    "    <table class=\"table table-bordered table-striped no-padding no-margin\">\n" +
    "      <thead>\n" +
    "      <tr>\n" +
    "        <th colspan=\"2\" translate>name</th>\n" +
    "        <th style=\"width: 100px\" translate>type</th>\n" +
    "        <th style=\"width: 100px\" translate>size</th>\n" +
    "        <th style=\"width: 150px\" translate>modified</th>\n" +
    "        <th ng-if=\"data.search.active\" translate>folder</th>\n" +
    "      </tr>\n" +
    "      </thead>\n" +
    "      <tbody>\n" +
    "      <tr ng-show=\"!data.isRoot && data.document.path !== data.rootPath && !data.search.active\">\n" +
    "        <td colspan=\"5\">\n" +
    "          <i class=\"fa fa-folder\"></i>\n" +
    "          <span><a href style=\"text-decoration: none\" class=\"no-text-decoration\" ng-click=\"navigateBack()\"> ..</a></span>\n" +
    "        </td>\n" +
    "      </tr>\n" +
    "      <tr ng-class=\"{'selected-row': $index === pagination.selected}\"\n" +
    "          dir-paginate=\"document in data.document.children | itemsPerPage: pagination.itemsPerPage\"\n" +
    "          ng-init=\"fileDocument = isFile(document)\"\n" +
    "          current-page=\"pagination.currentPage\">\n" +
    "\n" +
    "        <td ng-click=\"showDetails(document, $index);\">\n" +
    "          <span>\n" +
    "            <span ng-if=\"fileDocument\">\n" +
    "              <i class=\"fa {{getDocumentIcon(document)}}\"></i>\n" +
    "              <a ng-if=\"fileDocument\" target=\"{{downloadTarget}}\"\n" +
    "                 style=\"text-decoration: none\" ng-click=\"$event.stopPropagation();\" ng-href=\"{{getDownloadUrl(document.path)}}\"\n" +
    "                  title=\"{{document.name}}\">\n" +
    "                {{document.name}}\n" +
    "              </a>\n" +
    "            </span>\n" +
    "            <span ng-if=\"!fileDocument\">\n" +
    "              <i class=\"fa {{getDocumentIcon(document)}}\"></i>\n" +
    "              <a href style=\"text-decoration: none\" ng-click=\"navigateTo($event, document)\">\n" +
    "                {{document.name}}\n" +
    "              </a>\n" +
    "            </span>\n" +
    "          </span>\n" +
    "        </td>\n" +
    "\n" +
    "        <td class=\"fit-content\">\n" +
    "          <span class=\"btn-group pull-right\" uib-dropdown is-open=\"status.isopen\">\n" +
    "            <a title=\"{{'show-details' | translate}}\" id=\"single-button\" class=\"dropdown-anchor\" uib-dropdown-toggle\n" +
    "               ng-disabled=\"disabled\">\n" +
    "              <i class=\"glyphicon glyphicon-option-horizontal btn-large\"></i>\n" +
    "            </a>\n" +
    "            <ul class=\"dropdown-menu\" uib-dropdown-menu role=\"menu\" aria-labelledby=\"single-button\">\n" +
    "              <li role=\"menuitem\">\n" +
    "                <a href ng-click=\"showDetails(document, $index)\">\n" +
    "                  <span><i class=\"fa fa-info\"></i><span class=\"hoffset2\">{{'details' | translate}}</span></span>\n" +
    "                </a>\n" +
    "              </li>\n" +
    "              <li role=\"menuitem\">\n" +
    "                <a target=\"{{downloadTarget}}\" ng-href=\"{{getDownloadUrl(document.path)}}\">\n" +
    "                  <span><i class=\"fa fa-download\"></i><span class=\"hoffset2\">{{'download' | translate}}</span></span>\n" +
    "                </a>\n" +
    "              </li>\n" +
    "            </ul>\n" +
    "          </span>\n" +
    "        </td>\n" +
    "\n" +
    "        <td>\n" +
    "          <span ng-repeat=\"t in getTypeParts(document) track by $index\"\n" +
    "            class=\"label label-info\"\n" +
    "            ng-class=\"{'hoffset1' : !$first}\">{{t}}</span>\n" +
    "        </td>\n" +
    "        <td class=\"no-wrap\" ng-if=\"fileDocument\">\n" +
    "          {{document.size | bytes}}\n" +
    "        </td>\n" +
    "        <td class=\"no-wrap\" ng-if=\"!fileDocument\">\n" +
    "          {{document.size}} {{document.size === 1 ? 'item' : 'items' | translate}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{document.timestamps.lastUpdate | amTimeAgo}}\n" +
    "        </td>\n" +
    "        <td ng-if=\"data.search.active\">\n" +
    "          <a href class=\"no-text-decoration\" ng-click=\"navigateToParent($event, document)\">\n" +
    "            {{document.attachment.path === data.rootPath ? '/' : document.attachment.path.replace(data.rootPath, '')}}\n" +
    "          </a>\n" +
    "        </td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("file-browser/views/file-browser-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("file-browser/views/file-browser-template.html",
    "<div ng-cloak>\n" +
    "  <div ng-if=\"!data.document\" class=\"loading\"></div>\n" +
    "\n" +
    "  <div ng-if=\"data.document\">\n" +
    "    <obiba-alert id=\"FileSystemController\"></obiba-alert>\n" +
    "\n" +
    "    <div>\n" +
    "      <!-- Document details -->\n" +
    "      <div class=\"row\">\n" +
    "        <div class=\"col-md-12\">\n" +
    "          <div ng-include=\"'file-browser/views/toolbar-template.html'\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "\n" +
    "      <div class=\"row voffset2\">\n" +
    "        <div ng-class=\"{'col-md-8': data.details.show, 'col-md-12': !data.details.show}\">\n" +
    "          <div ng-include=\"'file-browser/views/documents-table-template.html'\"></div>\n" +
    "          <div ng-if=\"!data.isFile && data.document.children.length < 1 && !data.search.active\" class=\"text-muted\">\n" +
    "            <em>{{'empty-folder' | translate}}</em>\n" +
    "          </div>\n" +
    "          <div class=\"pull-right no-margin\">\n" +
    "            <dir-pagination-controls></dir-pagination-controls>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "        <div ng-if=\"data.details.show\" class=\"col-md-4\">\n" +
    "          <div ng-include=\"'file-browser/views/document-detail-template.html'\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "</div>");
}]);

angular.module("file-browser/views/toolbar-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("file-browser/views/toolbar-template.html",
    "<!--\n" +
    " ~ Copyright (c) 2016 OBiBa. All rights reserved.\n" +
    " ~\n" +
    " ~ This program and the accompanying materials\n" +
    " ~ are made available under the terms of the GNU Public License v3.0.\n" +
    " ~\n" +
    " ~ You should have received a copy of the GNU General Public License\n" +
    " ~ along with this program. If not, see <http://www.gnu.org/licenses/>.\n" +
    " -->\n" +
    "<div>\n" +
    "    <div class=\"pull-left voffset3\">\n" +
    "        <ol ng-show=\"data.document.path !== data.rootPath\" class=\"breadcrumb mica-breadcrumb no-margin no-padding\">\n" +
    "            <li>\n" +
    "                <a href ng-click=\"navigateToPath(data.rootPath)\">\n" +
    "                    <span><i class=\"fa {{getDocumentIcon(data.document)}}\"></i></span>\n" +
    "                </a>\n" +
    "            </li>\n" +
    "            <li ng-repeat=\"part in data.breadcrumbs\" ng-class=\"{'active': $first === $last && $last}\">\n" +
    "                <a ng-show=\"!$last && part.name !== '/'\" href ng-click=\"navigateToPath(part.path)\">\n" +
    "                    <span ng-show=\"part.name !== '/'\">{{part.name}}</span>\n" +
    "                </a>\n" +
    "                <span class=\"no-padding\" ng-if=\"part.name !== '/' && $last\">{{data.document.name || 'empty'}}</span>\n" +
    "            </li>\n" +
    "        </ol>\n" +
    "    </div>\n" +
    "    <div class=\"pull-right\">\n" +
    "      <table style=\"border:none\">\n" +
    "        <tbody>\n" +
    "        <tr>\n" +
    "          <td>\n" +
    "            <a href>\n" +
    "              <span class=\"input-group input-group-sm no-padding-top no-padding-right\">\n" +
    "               <span class=\"input-group-addon\"><i class=\"glyphicon glyphicon-search\"></i></span>\n" +
    "               <input ng-keyup=\"searchKeyUp($event)\"\n" +
    "                   ng-model=\"data.search.text\"\n" +
    "                   type=\"text\"\n" +
    "                   class=\"form-control ng-pristine ng-untouched ng-valid\"\n" +
    "                   aria-describedby=\"study-search\"\n" +
    "                   style=\"max-width: 200px;\">\n" +
    "               <span ng-show=\"data.search.text\" title=\"{{'search-tooltip.clear' | translate}}\" ng-click=\"clearSearch()\"\n" +
    "                  class=\"input-group-addon\">\n" +
    "                <i class=\"fa fa-times\"></i>\n" +
    "               </span>\n" +
    "              </span>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <a href ng-model=\"data.search.recursively\"\n" +
    "              class=\"btn btn-sm hoffset1\"\n" +
    "              ng-class=\"{'btn-info': data.search.recursively, 'btn-default': !data.search.recursively}\"\n" +
    "              data-toggle=\"button\" ng-click=\"toggleRecursively()\"\n" +
    "              title=\"{{'search-tooltip.recursively' | translate}}\">\n" +
    "              <i class=\"fa i-obiba-hierarchy\"></i>\n" +
    "            </a>\n" +
    "            <a href ng-click=\"searchDocuments('RECENT')\"\n" +
    "              class=\"btn btn-info btn-sm\"\n" +
    "              title=\"{{'search-tooltip.most-recent' | translate}}\">\n" +
    "              <span><i class=\"fa fa-clock-o fa-lg\"></i></span>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "        </tr>\n" +
    "        </tbody>\n" +
    "      </table>\n" +
    "    </div>\n" +
    "</div>");
}]);

angular.module("graphics/views/charts-directive.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("graphics/views/charts-directive.html",
    "<div>\n" +
    "  <div google-chart chart=\"chartObject\" style=\"min-height:350px; width:100%;\">\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("graphics/views/tables-directive.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("graphics/views/tables-directive.html",
    "<div>\n" +
    "    <table style=\"max-height: 400px;\" class=\"table table-bordered table-striped\" >\n" +
    "        <thead>\n" +
    "        <th ng-repeat=\"header in chartObject.header\">{{header}}</th>\n" +
    "        </thead>\n" +
    "        <tr ng-show=\"chartObject.ordered\" ng-repeat=\"row in chartObject.entries\">\n" +
    "            <td>{{row.title}}</td>\n" +
    "            <td><a href ng-click=\"updateCriteria(row.key, chartObject.vocabulary)\">{{row.value}}</a></td>\n" +
    "            <td ng-if=\"row.participantsNbr\">{{row.participantsNbr}}</td>\n" +
    "        </tr>\n" +
    "        <tr ng-show=\"chartObject.notOrdered\" ng-repeat=\"row in chartObject.entries\">\n" +
    "            <td>{{row.title}}</td>\n" +
    "            <td><a href ng-click=\"updateCriteria(row.key, chartObject.vocabulary)\">{{row.value}}</a></td>\n" +
    "            <td ng-if=\"row.participantsNbr\">{{row.participantsNbr}}</td>\n" +
    "        </tr>\n" +
    "        </tbody>\n" +
    "    </table>\n" +
    "</div>\n" +
    "");
}]);

angular.module("localized/localized-input-group-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("localized/localized-input-group-template.html",
    "<div class=\"form-group\" ng-class=\"{'has-error': (form[fieldName].$dirty || form.saveAttempted) && form[fieldName].$invalid}\">\n" +
    "  <label for=\"{{fieldName}}\" class=\"control-label\">\n" +
    "    {{label | translate}}\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "  </label>\n" +
    "  <div class=\"input-group\">\n" +
    "    <input ng-repeat=\"localized in model | filter:{lang:lang}\" ng-model=\"localized.value\" type=\"text\" class=\"form-control\" id=\"{{fieldName}}\" name=\"{{fieldName}}\" ng-disabled=\"disabled\" form-server-error ng-required=\"required\">\n" +
    "  <span class=\"input-group-btn\" ng-show=\"remove\">\n" +
    "    <button class=\"btn btn-default\" type=\"button\" ng-click=\"remove(model)\"><span class=\"glyphicon glyphicon-remove\" aria-hidden=\"true\"></span></button>\n" +
    "  </span>\n" +
    "  </div>\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[fieldName].$dirty && form[fieldName].$invalid\">\n" +
    "    <li ng-show=\"form[fieldName].$error.required\" translate>required</li>\n" +
    "    <li ng-repeat=\"error in form[fieldName].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "</div>");
}]);

angular.module("localized/localized-input-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("localized/localized-input-template.html",
    "<div class=\"form-group\" ng-class=\"{'has-error': (form[fieldName].$dirty || form.saveAttempted) && form[fieldName].$invalid}\">\n" +
    "  <label for=\"{{fieldName}}\" class=\"control-label\">\n" +
    "    {{label | translate}}\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "  </label>\n" +
    "  <input ng-repeat=\"localized in model | filter:{lang:lang}\" ng-model=\"localized.value\" type=\"text\" class=\"form-control\" id=\"{{fieldName}}\" name=\"{{fieldName}}\" ng-disabled=\"disabled\" form-server-error ng-required=\"required\">\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[fieldName].$dirty && form[fieldName].$invalid\">\n" +
    "    <li ng-show=\"form[fieldName].$error.required\" translate>required</li>\n" +
    "    <li ng-repeat=\"error in form[fieldName].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "</div>");
}]);

angular.module("localized/localized-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("localized/localized-template.html",
    "<span>{{LocalizedValues.forLang(value,lang)}}</span>");
}]);

angular.module("localized/localized-textarea-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("localized/localized-textarea-template.html",
    "<div class=\"form-group\" ng-class=\"{'has-error': (form[fieldName].$dirty || form.saveAttempted) && form[fieldName].$invalid}\">\n" +
    "\n" +
    "  <label for=\"{{fieldName}}\" class=\"control-label\">\n" +
    "    {{label | translate}}\n" +
    "    <span ng-show=\"required\">*</span>\n" +
    "  </label>\n" +
    "\n" +
    "  <textarea ng-repeat=\"localized in model | filter:{lang:lang}\" ng-model=\"localized.value\" rows=\"{{rows ? rows : 5}}\" class=\"form-control\" id=\"{{fieldName}}\" name=\"{{fieldName}}\" form-server-error ng-disabled=\"disabled\" ng-required=\"required\"></textarea>\n" +
    "\n" +
    "  <ul class=\"input-error list-unstyled\" ng-show=\"form[fieldName].$dirty && form[fieldName].$invalid\">\n" +
    "    <li ng-show=\"form[fieldName].$error.required\" translate>required</li>\n" +
    "    <li ng-repeat=\"error in form[fieldName].errors\">{{error}}</li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <p ng-show=\"help\" class=\"help-block\">{{help | translate}}</p>\n" +
    "\n" +
    "</div>");
}]);

angular.module("search/views/classifications.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications.html",
    "<div>\n" +
    "  <div ng-if=\"classificationsHeaderTemplateUrl\" ng-include=\"classificationsHeaderTemplateUrl\"></div>\n" +
    "\n" +
    "  <div class=\"container alert-fixed-position\">\n" +
    "    <obiba-alert id=\"SearchController\"></obiba-alert>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"alert-growl-container\">\n" +
    "    <obiba-alert id=\"SearchControllerGrowl\"></obiba-alert>\n" +
    "  </div>\n" +
    "\n" +
    "  <a href class=\"btn btn-sm btn-success\" ng-click=\"goToSearch()\">\n" +
    "    <i class=\"fa fa-chevron-left\"></i>\n" +
    "    <span translate>search.back</span>\n" +
    "  </a>\n" +
    "\n" +
    "  <!-- Lang tabs -->\n" +
    "  <ul class=\"nav nav-tabs voffset2\" role=\"tablist\" ng-if=\"tabs && tabs.length>1\">\n" +
    "    <li ng-repeat=\"tab in tabs\" role=\"presentation\" ng-class=\"{ active: tab === lang }\"><a href role=\"tab\"\n" +
    "      ng-click=\"setLocale(tab)\">{{'language.' + tab | translate}}</a></li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <!-- Search criteria region -->\n" +
    "  <div class=\"panel panel-default voffset2\" ng-if=\"search.criteria.children && search.criteria.children.length>0\">\n" +
    "    <div class=\"panel-body\">\n" +
    "      <table style=\"border:none\">\n" +
    "        <tbody>\n" +
    "        <tr>\n" +
    "          <td>\n" +
    "            <a href class=\"btn btn-sm btn-default\" ng-click=\"clearSearchQuery()\" translate>clear</a>\n" +
    "          </td>\n" +
    "          <td style=\"padding-left: 10px\">\n" +
    "            <div criteria-root item=\"search.criteria\" query=\"search.query\" advanced=\"search.advanced\" on-remove=\"removeCriteriaItem\"\n" +
    "              on-refresh=\"refreshQuery\" class=\"inline\"></div>\n" +
    "\n" +
    "            <small ng-if=\"showAdvanced()\" class=\"hoffset2\">\n" +
    "              <a href ng-click=\"toggleSearchQuery()\"\n" +
    "                title=\"{{search.advanced ? 'search.basic-help' : 'search.advanced-help' | translate}}\" translate>\n" +
    "                {{search.advanced ? 'search.basic' : 'search.advanced' | translate}}\n" +
    "              </a>\n" +
    "            </small>\n" +
    "          </td>\n" +
    "        </tr>\n" +
    "        </tbody>\n" +
    "      </table>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <!-- Classifications region -->\n" +
    "  <div class=\"{{tabs && tabs.length>1 ? 'tab-content voffset4' : ''}}\">\n" +
    "    <ul class=\"nav nav-pills voffset2\" role=\"tablist\" ng-if=\"targetTabsOrder.length > 1\">\n" +
    "      <li ng-repeat=\"target in targetTabsOrder\" role=\"presentation\" ng-class=\"{ active: target === $parent.target }\"><a href role=\"tab\"\n" +
    "          ng-click=\"navigateToTarget(target)\">{{'taxonomy.target.' + target | translate}}</a></li>\n" +
    "    </ul>\n" +
    "\n" +
    "    <classifications-panel target=\"target\" is-history-enabled=\"true\" on-select-term=\"onSelectTerm\" lang=\"lang\"></classifications-panel>\n" +
    "  </div>\n" +
    "\n" +
    "</div>");
}]);

angular.module("search/views/classifications/classifications-view.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/classifications-view.html",
    "<div ng-show=\"!inSearchMode()\" class=\"voffset2\">\n" +
    "  <div>\n" +
    "    <ol class=\"breadcrumb\">\n" +
    "      <li ng-if=\"!taxonomies.taxonomy\">\n" +
    "        {{'all-' + taxonomies.target + '-classifications' | translate}}\n" +
    "      </li>\n" +
    "      <li ng-if=\"taxonomies.taxonomy\">\n" +
    "        <a href ng-click=\"navigateTaxonomy()\">{{'all-' + taxonomies.target + '-classifications' |\n" +
    "          translate}}</a>\n" +
    "      </li>\n" +
    "      <li ng-if=\"taxonomies.taxonomy\">\n" +
    "        <span ng-repeat=\"label in taxonomies.taxonomy.title\" ng-if=\"!taxonomies.vocabulary && label.locale === lang\">\n" +
    "          {{label.text}}\n" +
    "        </span>\n" +
    "        <a href ng-click=\"navigateTaxonomy(taxonomies.taxonomy)\" ng-if=\"taxonomies.vocabulary\">\n" +
    "          <span ng-repeat=\"label in taxonomies.taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "            {{label.text}}\n" +
    "          </span>\n" +
    "        </a>\n" +
    "      </li>\n" +
    "      <li ng-if=\"taxonomies.vocabulary\">\n" +
    "        <span ng-repeat=\"label in taxonomies.vocabulary.title\" ng-if=\"label.locale === lang\">\n" +
    "          {{label.text}}\n" +
    "        </span>\n" +
    "      </li>\n" +
    "    </ol>\n" +
    "  </div>\n" +
    "\n" +
    "  <div ng-if=\"taxonomies.search.active\" class=\"loading\"></div>\n" +
    "\n" +
    "  <div ng-if=\"!taxonomies.search.active\">\n" +
    "    <div ng-if=\"!taxonomies.taxonomy\">\n" +
    "      <div ng-repeat=\"group in taxonomyGroups\">\n" +
    "        <h3 ng-if=\"group.title\">{{group.title}}</h3>\n" +
    "        <p class=\"help-block\" ng-if=\"group.description\">{{group.description}}</p>\n" +
    "        <div ng-if=\"!taxonomies.taxonomy\">\n" +
    "          <div ng-repeat=\"taxonomy in group.taxonomies\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "            <div class=\"col-md-4\">\n" +
    "              <div taxonomy-panel taxonomy=\"group.taxonomies[$index]\" lang=\"lang\"\n" +
    "                   on-navigate=\"navigateTaxonomy\"></div>\n" +
    "            </div>\n" +
    "            <div class=\"col-md-4\">\n" +
    "              <div taxonomy-panel taxonomy=\"group.taxonomies[$index + 1]\" lang=\"lang\"\n" +
    "                   on-navigate=\"navigateTaxonomy\"></div>\n" +
    "            </div>\n" +
    "            <div class=\"col-md-4\">\n" +
    "              <div taxonomy-panel taxonomy=\"group.taxonomies[$index + 2]\" lang=\"lang\"\n" +
    "                   on-navigate=\"navigateTaxonomy\"></div>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div ng-if=\"taxonomies.taxonomy && !taxonomies.vocabulary\">\n" +
    "      <h3 ng-repeat=\"label in taxonomies.taxonomy.title\"\n" +
    "          ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </h3>\n" +
    "\n" +
    "      <p class=\"help-block\" ng-repeat=\"label in taxonomies.taxonomy.description\" ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </p>\n" +
    "\n" +
    "      <div ng-repeat=\"vocabulary in taxonomies.taxonomy.vocabularies\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div vocabulary-panel target=\"taxonomies.target\" taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabularies[$index]\"\n" +
    "               lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div vocabulary-panel target=\"taxonomies.target\" taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabularies[$index + 1]\"\n" +
    "               lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div vocabulary-panel target=\"taxonomies.target\" taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabularies[$index + 2]\"\n" +
    "               lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div ng-if=\"taxonomies.taxonomy && taxonomies.vocabulary && !taxonomies.term\">\n" +
    "      <h3 ng-repeat=\"label in taxonomies.vocabulary.title\"\n" +
    "          ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </h3>\n" +
    "\n" +
    "      <p class=\"help-block\" ng-repeat=\"label in taxonomies.vocabulary.description\"\n" +
    "         ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </p>\n" +
    "\n" +
    "      <div ng-repeat=\"term in taxonomies.vocabulary.terms\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div term-panel target=\"taxonomies.target\" taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.vocabulary\" term=\"taxonomies.vocabulary.terms[$index]\"\n" +
    "               lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div term-panel target=\"taxonomies.target\" taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.vocabulary\" term=\"taxonomies.vocabulary.terms[$index + 1]\"\n" +
    "               lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div term-panel target=\"taxonomies.target\" taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.vocabulary\" term=\"taxonomies.vocabulary.terms[$index + 2]\"\n" +
    "               lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div ng-if=\"taxonomies.taxonomy && taxonomies.vocabulary && taxonomies.term\">\n" +
    "      <h5 ng-repeat=\"label in taxonomies.term.title\" ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </h5>\n" +
    "      <p ng-repeat=\"label in taxonomies.term.description\" ng-if=\"label.locale === lang\">\n" +
    "        <span class=\"help-block\" ng-bind-html=\"label.text | dceDescription\" ng-if=\"taxonomies.vocabulary.name === 'dceIds'\"></span>\n" +
    "        <span class=\"help-block\" ng-bind-html=\"label.text\" ng-if=\"taxonomies.vocabulary.name !== 'dceIds'\"></span>\n" +
    "      </p>\n" +
    "      <div>\n" +
    "        <a href class=\"btn btn-default btn-xs\"\n" +
    "           ng-click=\"selectTerm(taxonomies.target, taxonomies.taxonomy, taxonomies.vocabulary, {term: taxonomies.term})\">\n" +
    "          <i class=\"fa fa-plus-circle\"></i>\n" +
    "          <span translate>add-query</span>\n" +
    "        </a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/classifications/taxonomies-facets-view.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/taxonomies-facets-view.html",
    "<ul class=\"nav nav-tabs\" role=\"tablist\" ng-if=\"targets.length > 1\">\n" +
    "  <li ng-repeat=\"tab in targets\" role=\"presentation\" ng-class=\"{ active: tab === target }\">\n" +
    "    <a href role=\"tab\" ng-click=\"setTarget(tab)\">{{'search.' + tab + '.facet-label' | translate}}</a></li>\n" +
    "</ul>\n" +
    "\n" +
    "<uib-accordion close-others=\"false\">\n" +
    "    <uib-accordion-group ng-repeat=\"taxonomy in taxonomies[target]\" is-open=\"taxonomy.isOpen\" is-disabled=\"false\" template-url=\"search/views/classifications/taxonomy-accordion-group.html\">\n" +
    "      <uib-accordion-heading>\n" +
    "          <i class=\"fa\" ng-class=\"{'fa-chevron-down': taxonomy.isOpen, 'fa-chevron-right': !taxonomy.isOpen}\"></i>\n" +
    "          <span uib-popover=\"{{localize(taxonomy.description ? taxonomy.description : taxonomy.title)}}\"\n" +
    "                popover-title=\"{{taxonomy.description ? localize(taxonomy.title) : null}}\"\n" +
    "                popover-placement=\"bottom\"\n" +
    "                popover-trigger=\"mouseenter\"\n" +
    "                popover-popup-delay=\"1000\">\n" +
    "            {{localize(taxonomy.title)}}\n" +
    "          </span>\n" +
    "      </uib-accordion-heading>\n" +
    "      <uib-accordion close-others=\"false\">\n" +
    "        <uib-accordion-group ng-repeat=\"vocabulary in taxonomy.vocabularies\" is-open=\"vocabulary.isOpen\" is-disabled=\"false\" template-url=\"search/views/classifications/vocabulary-accordion-group.html\">\n" +
    "          <uib-accordion-heading>\n" +
    "            <span uib-popover=\"{{localize(vocabulary.description ? vocabulary.description : vocabulary.title)}}\"\n" +
    "                  popover-title=\"{{vocabulary.description ? localize(vocabulary.title) : null}}\"\n" +
    "                  popover-placement=\"bottom\"\n" +
    "                  popover-trigger=\"mouseenter\"\n" +
    "                  popover-popup-delay=\"1000\"\n" +
    "                  ng-click=\"loadVocabulary(taxonomy, vocabulary)\">\n" +
    "              <i class=\"fa\" ng-class=\"{'fa-caret-down': vocabulary.isOpen, 'fa-caret-right': !vocabulary.isOpen}\"></i>\n" +
    "              <span>\n" +
    "                {{localize(vocabulary.title)}}\n" +
    "              </span>\n" +
    "              <span ng-if=\"!vocabulary.title\">\n" +
    "                {{vocabulary.name}}\n" +
    "              </span>\n" +
    "            </span>\n" +
    "          </uib-accordion-heading>\n" +
    "          <div>\n" +
    "            <div ng-if=\"vocabulary.isMatch\" ng-controller=\"MatchVocabularyFacetController\" class=\"voffset2 form-group\">\n" +
    "              <form novalidate class=\"form-inline\" ng-keypress=\"onKeypress($event)\">\n" +
    "                <div class=\"form-group form-group-sm\">\n" +
    "                  <input type=\"text\" class=\"form-control\" ng-model=\"text\" placeholder=\"{{'search.match.placeholder' | translate}}\">\n" +
    "                </div>\n" +
    "              </form>\n" +
    "            </div>\n" +
    "            <div ng-if=\"vocabulary.isNumeric\" ng-controller=\"NumericVocabularyFacetController\" class=\"voffset2 form-group\">\n" +
    "              <form novalidate class=\"form-inline\"  ng-keypress=\"onKeypress($event)\">\n" +
    "                <div class=\"form-group form-group-sm\">\n" +
    "                  <label for=\"nav-{{vocabulary.name}}-from\" translate>from</label>\n" +
    "                  <input type=\"number\" class=\"form-control\" id=\"nav-{{vocabulary.name}}-from\" ng-model=\"from\" placeholder=\"{{min}}\" style=\"width:75px;\">\n" +
    "                  <label for=\"nav-{{vocabulary.name}}-to\" translate>to</label>\n" +
    "                  <input type=\"number\" class=\"form-control\" id=\"nav-{{vocabulary.name}}-to\" ng-model=\"to\" placeholder=\"{{max}}\" style=\"width:75px;\">\n" +
    "                </div>\n" +
    "              </form>\n" +
    "            </div>\n" +
    "            <div ng-controller=\"TermsVocabularyFacetController\">\n" +
    "              <ul class=\"nav nav-pills nav-stacked\" ng-if=\"vocabulary.terms\">\n" +
    "                <li ng-repeat=\"term in vocabulary.terms | orderBy:['-selected', '-count', '+name']  | limitTo:vocabulary.limit:begin\"\n" +
    "                    class=\"checkbox\" ng-class=\"{active: term.name === term.name}\">\n" +
    "                  <label style=\"max-width: 80%;\">\n" +
    "                    <input type=\"checkbox\" ng-model=\"term.selected\" ng-change=\"selectTerm(target, taxonomy, vocabulary, {term: term})\">\n" +
    "                    <span uib-popover=\"{{localize(term.description ? term.description : term.title)}}\"\n" +
    "                          popover-title=\"{{term.description ? localize(term.title) : null}}\"\n" +
    "                          popover-placement=\"bottom\"\n" +
    "                          popover-trigger=\"mouseenter\"\n" +
    "                          popover-popup-delay=\"1000\">\n" +
    "                      <span>\n" +
    "                        {{localize(term.title)}}\n" +
    "                      </span>\n" +
    "                      <span ng-if=\"!term.title\">\n" +
    "                        {{term.name}}\n" +
    "                      </span>\n" +
    "                    </span>\n" +
    "                  </label>\n" +
    "                    <span class=\"pull-right\" ng-class=\"{'text-muted': !term.selected}\">\n" +
    "                      {{term.count}}\n" +
    "                    </span>\n" +
    "                </li>\n" +
    "              </ul>\n" +
    "              <div ng-if=\"!vocabulary.isNumeric && !vocabulary.isMatch && vocabulary.terms.length > 10\" class=\"voffset1 pull-right form-group\">\n" +
    "                <button class=\"btn btn-xs btn-primary\" ng-if=\"vocabulary.limit\" ng-click=\"vocabulary.limit = undefined\" translate>search.facet.more</button>\n" +
    "                <button class=\"btn btn-xs btn-default\" ng-if=\"!vocabulary.limit\" ng-click=\"vocabulary.limit = 10\" translate>search.facet.less</button>\n" +
    "              </div>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "        </uib-accordion-group>\n" +
    "      </uib-accordion>\n" +
    "    </uib-accordion-group>\n" +
    "</uib-accordion>\n" +
    "");
}]);

angular.module("search/views/classifications/taxonomies-view.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/taxonomies-view.html",
    "<div class=\"collapse\">\n" +
    "  <div class=\"voffset2\">\n" +
    "    <div class=\"panel panel-default\">\n" +
    "      <div class=\"panel-heading no-padding-top no-padding-bottom\">\n" +
    "        <div class=\"row no-padding\">\n" +
    "          <div class=\"col-md-8\">\n" +
    "            <ol class=\"breadcrumb no-margin no-padding pull-left\">\n" +
    "              <li ng-if=\"taxonomies.taxonomy\">\n" +
    "                <h4 ng-repeat=\"label in taxonomies.taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "                  <strong>{{label.text}}</strong>\n" +
    "                </h4>\n" +
    "              </li>\n" +
    "            </ol>\n" +
    "          </div>\n" +
    "          <div class=\"col-md-4\">\n" +
    "            <h4 ng-click=\"closeTaxonomies()\" title=\"{{'close' | translate}}\" class=\"pull-right\" style=\"cursor: pointer\">\n" +
    "              <i class=\"fa fa-close\"></i>\n" +
    "            </h4>\n" +
    "        </div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "      <div class=\"panel-body\">\n" +
    "        <div ng-if=\"taxonomies.search.active\" class=\"loading\"></div>\n" +
    "\n" +
    "        <div ng-if=\"!taxonomies.search.active\">\n" +
    "          <div ng-if=\"!taxonomies.taxonomy\">\n" +
    "            <div ng-repeat=\"group in taxonomyGroups\">\n" +
    "              <h4 ng-if=\"group.title\">{{group.title}}</h4>\n" +
    "              <p class=\"help-block\" ng-if=\"group.description\">{{group.description}}</p>\n" +
    "              <div ng-if=\"!taxonomies.taxonomy\">\n" +
    "                <div ng-repeat=\"taxonomy in group.taxonomies\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "                  <div class=\"col-md-4\">\n" +
    "                    <div taxonomy-panel taxonomy=\"group.taxonomies[$index]\" lang=\"lang\"\n" +
    "                         on-navigate=\"navigateTaxonomy\"></div>\n" +
    "                  </div>\n" +
    "                  <div class=\"col-md-4\">\n" +
    "                    <div taxonomy-panel taxonomy=\"group.taxonomies[$index + 1]\" lang=\"lang\"\n" +
    "                         on-navigate=\"navigateTaxonomy\"></div>\n" +
    "                  </div>\n" +
    "                  <div class=\"col-md-4\">\n" +
    "                    <div taxonomy-panel taxonomy=\"group.taxonomies[$index + 2]\" lang=\"lang\"\n" +
    "                         on-navigate=\"navigateTaxonomy\"></div>\n" +
    "                  </div>\n" +
    "                </div>\n" +
    "              </div>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "\n" +
    "          <div ng-if=\"taxonomies.taxonomy\">\n" +
    "            <div class=\"row\">\n" +
    "              <div class=\"col-md-4 height3\" scroll-to-top=\"taxonomies.taxonomy\">\n" +
    "                <h5 ng-repeat=\"label in taxonomies.taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "                  {{label.text}}\n" +
    "                </h5>\n" +
    "                <p class=\"help-block\" ng-repeat=\"label in taxonomies.taxonomy.description\"\n" +
    "                   ng-if=\"label.locale === lang\">\n" +
    "                  {{label.text}}\n" +
    "                </p>\n" +
    "                <ul class=\"nav nav-pills nav-stacked\" ng-if=\"taxonomies.taxonomy.vocabularies\">\n" +
    "                  <li ng-repeat=\"vocabulary in taxonomies.taxonomy.vocabularies | filter:canNavigate\"\n" +
    "                      class=\"{{taxonomies.vocabulary.name === vocabulary.name ? 'active' : ''}}\">\n" +
    "                    <a class=\"clearfix\" id=\"search-navigate-taxonomy\" href\n" +
    "                       ng-click=\"navigateTaxonomy(taxonomies.taxonomy, vocabulary)\">\n" +
    "                      <i class=\"pull-right {{taxonomies.vocabulary.name !== vocabulary.name ? 'hidden' : ''}} hidden-sm hidden-xs fa fa-chevron-circle-right\"></i>\n" +
    "                      <span ng-repeat=\"label in vocabulary.title\" ng-if=\"label.locale === lang\">\n" +
    "                        {{label.text}}\n" +
    "                      </span>\n" +
    "                      <span ng-if=\"!vocabulary.title\">\n" +
    "                        {{vocabulary.name}}\n" +
    "                      </span>\n" +
    "\n" +
    "\n" +
    "                    </a>\n" +
    "                  </li>\n" +
    "                </ul>\n" +
    "              </div>\n" +
    "              <div class=\"col-md-4 height3\" scroll-to-top=\"taxonomies.vocabulary\">\n" +
    "                <div ng-if=\"taxonomies.vocabulary\">\n" +
    "                  <h5 ng-repeat=\"label in taxonomies.vocabulary.title\" ng-if=\"label.locale === lang\">\n" +
    "                    {{label.text}}\n" +
    "                  </h5>\n" +
    "                  <div class=\"form-group\" ng-if=\"!taxonomies.isNumericVocabulary && !taxonomies.isMatchVocabulary\">\n" +
    "                    <a href class=\"btn btn-default btn-xs\"\n" +
    "                       ng-click=\"selectTerm(taxonomies.target, taxonomies.taxonomy, taxonomies.vocabulary)\">\n" +
    "                      <i class=\"fa fa-plus-circle\"></i>\n" +
    "                      <span translate>add-query</span>\n" +
    "                    </a>\n" +
    "                  </div>\n" +
    "                  <p class=\"help-block\" ng-repeat=\"label in taxonomies.vocabulary.description\"\n" +
    "                     ng-if=\"label.locale === lang\">\n" +
    "                    {{label.text}}\n" +
    "                  </p>\n" +
    "                  <div ng-if=\"taxonomies.isMatchVocabulary\" ng-controller=\"MatchVocabularyPanelController\">\n" +
    "                    <div class=\"form-group\">\n" +
    "                      <a href class=\"btn btn-default btn-xs\"\n" +
    "                         ng-click=\"selectTerm(taxonomies.target, taxonomies.taxonomy, taxonomies.vocabulary, {text: text})\">\n" +
    "                        <i class=\"fa fa-plus-circle\"></i>\n" +
    "                        <span translate>add-query</span>\n" +
    "                      </a>\n" +
    "                    </div>\n" +
    "                    <form novalidate class=\"form-inline\"\n" +
    "                          ui-keypress=\"{13: 'selectTerm(taxonomies.target, taxonomies.taxonomy, taxonomies.vocabulary, {text: text})'}\">\n" +
    "                      <div class=\"form-group\">\n" +
    "                        <input type=\"text\" class=\"form-control\" ng-model=\"text\"\n" +
    "                               placeholder=\"{{'search.match.placeholder' | translate}}\">\n" +
    "                      </div>\n" +
    "                    </form>\n" +
    "                  </div>\n" +
    "                  <div ng-if=\"taxonomies.isNumericVocabulary\" ng-controller=\"NumericVocabularyPanelController\">\n" +
    "                    <div class=\"form-group\">\n" +
    "                      <a href class=\"btn btn-default btn-xs\"\n" +
    "                         ng-click=\"selectTerm(taxonomies.target, taxonomies.taxonomy, taxonomies.vocabulary, {from: from, to: to})\">\n" +
    "                        <i class=\"fa fa-plus-circle\"></i>\n" +
    "                        <span translate>add-query</span>\n" +
    "                      </a>\n" +
    "                    </div>\n" +
    "                    <form novalidate class=\"form-inline\"\n" +
    "                          ui-keypress=\"{13:'selectTerm(taxonomies.target, taxonomies.taxonomy, taxonomies.vocabulary, {from: from, to: to})'}\">\n" +
    "                      <div class=\"form-group\">\n" +
    "                        <label for=\"nav-{{taxonomies.vocabulary.name}}-from\" translate>from</label>\n" +
    "                        <input type=\"number\" class=\"form-control\" id=\"nav-{{taxonomies.vocabulary.name}}-from\"\n" +
    "                               ng-model=\"from\" style=\"width:150px\">\n" +
    "                      </div>\n" +
    "                      <div class=\"form-group\">\n" +
    "                        <label for=\"nav-{{taxonomies.vocabulary.name}}-to\" translate>to</label>\n" +
    "                        <input type=\"number\" class=\"form-control\" id=\"nav-{{taxonomies.vocabulary.name}}-to\"\n" +
    "                               ng-model=\"to\" style=\"width:150px\">\n" +
    "                      </div>\n" +
    "                    </form>\n" +
    "                  </div>\n" +
    "                  <ul class=\"nav nav-pills nav-stacked\" ng-if=\"taxonomies.vocabulary.terms\">\n" +
    "                    <li ng-repeat=\"term in taxonomies.vocabulary.terms\"\n" +
    "                        class=\"{{taxonomies.term.name === term.name ? 'active' : ''}}\">\n" +
    "                      <a class=\"clearfix\" id=\"search-navigate-vocabulary\" href\n" +
    "                         ng-click=\"navigateTaxonomy(taxonomies.taxonomy, taxonomies.vocabulary, term)\">\n" +
    "                        <i class=\"pull-right {{taxonomies.term.name !== term.name ? 'hidden' : ''}} hidden-sm hidden-xs fa fa-chevron-circle-right\"></i>\n" +
    "                        <span ng-repeat=\"label in term.title\" ng-if=\"label.locale === lang\">\n" +
    "                          {{label.text}}\n" +
    "                        </span>\n" +
    "                        <span ng-if=\"!term.title\">\n" +
    "                          {{term.name}}\n" +
    "                        </span>\n" +
    "                      </a>\n" +
    "                    </li>\n" +
    "                  </ul>\n" +
    "                </div>\n" +
    "                <div ng-if=\"!taxonomies.vocabulary\" translate>search.taxonomy-nav-help</div>\n" +
    "              </div>\n" +
    "              <div class=\"col-md-4 height3\" scroll-to-top=\"taxonomies.term\">\n" +
    "                <div ng-if=\"taxonomies.term\">\n" +
    "                  <h5 ng-repeat=\"label in taxonomies.term.title\" ng-if=\"label.locale === lang\">\n" +
    "                    {{label.text}}\n" +
    "                  </h5>\n" +
    "                  <div>\n" +
    "                    <a href class=\"btn btn-default btn-xs\"\n" +
    "                       ng-click=\"selectTerm(taxonomies.target, taxonomies.taxonomy, taxonomies.vocabulary, {term: taxonomies.term})\">\n" +
    "                      <i class=\"fa fa-plus-circle\"></i>\n" +
    "                      <span translate>add-query</span>\n" +
    "                    </a>\n" +
    "                  </div>\n" +
    "                  <p ng-repeat=\"label in taxonomies.term.description\" ng-if=\"label.locale === lang\">\n" +
    "                    <span class=\"help-block\" ng-bind-html=\"label.text | dceDescription\"\n" +
    "                          ng-if=\"taxonomies.vocabulary.name === 'dceIds'\"></span>\n" +
    "                    <span class=\"help-block\" ng-bind-html=\"label.text\"\n" +
    "                          ng-if=\"taxonomies.vocabulary.name !== 'dceIds'\"></span>\n" +
    "                  </p>\n" +
    "                </div>\n" +
    "                <div ng-if=\"!taxonomies.term && taxonomies.vocabulary\" translate>search.vocabulary-nav-help</div>\n" +
    "              </div>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/classifications/taxonomy-accordion-group.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/taxonomy-accordion-group.html",
    "<div class=\"panel no-margin no-border-radius\" ng-class=\"panelClass || 'panel-default'\">\n" +
    "  <div role=\"tab\" id=\"{{::headingId}}\" aria-selected=\"{{isOpen}}\" class=\"panel-heading\" ng-keypress=\"toggleOpen($event)\">\n" +
    "    <h4 class=\"panel-title\">\n" +
    "      <a role=\"button\" data-toggle=\"collapse\" href aria-expanded=\"{{isOpen}}\" aria-controls=\"{{::panelId}}\" tabindex=\"0\" class=\"accordion-toggle\" ng-click=\"toggleOpen()\" uib-accordion-transclude=\"heading\"><small><span uib-accordion-header ng-class=\"{'text-muted': isDisabled}\">{{heading}}</span></small></a>\n" +
    "    </h4>\n" +
    "  </div>\n" +
    "  <div id=\"{{::panelId}}\" aria-labelledby=\"{{::headingId}}\" aria-hidden=\"{{!isOpen}}\" role=\"tabpanel\" class=\"panel-collapse collapse\" uib-collapse=\"!isOpen\">\n" +
    "    <div class=\"panel-body no-padding small\" ng-transclude></div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/classifications/taxonomy-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/taxonomy-panel-template.html",
    "<div>\n" +
    "  <div class=\"panel panel-default\" ng-if=\"taxonomy\">\n" +
    "    <div class=\"panel-heading\">\n" +
    "      <div ng-repeat=\"label in taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "        <a href ng-click=\"onNavigate(taxonomy)\">{{label.text}}</a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div class=\"panel-body\">\n" +
    "      <div ng-repeat=\"label in taxonomy.description\" ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/classifications/taxonomy-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/taxonomy-template.html",
    "<div ng-repeat=\"vocabulary in taxonomies.taxonomy.vocabularies\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "  <div class=\"col-md-4\">\n" +
    "    <div vocabulary-panel taxonomy=\"taxonomies.taxonomy.vocabularies[$index]\"></div>\n" +
    "  </div>\n" +
    "  <div class=\"col-md-4\">\n" +
    "    <div taxonomy-panel taxonomy=\"taxonomies.taxonomy.vocabularies[$index + 1]\"></div>\n" +
    "  </div>\n" +
    "  <div class=\"col-md-4\">\n" +
    "    <div taxonomy-panel taxonomy=\"taxonomies.taxonomy.vocabularies[$index + 2]\"></div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/classifications/term-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/term-panel-template.html",
    "<div>\n" +
    "  <div class=\"panel panel-default\" ng-if=\"term\">\n" +
    "    <div class=\"panel-heading\">\n" +
    "      <div ng-repeat=\"label in term.title\" ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "        <small>\n" +
    "          <a href ng-click=\"onSelect(target, taxonomy, vocabulary, {term: term})\">\n" +
    "            <i class=\"fa fa-plus-circle\" title=\"{{'add-query' | translate}}\"></i>\n" +
    "          </a>\n" +
    "        </small>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div class=\"panel-body\">\n" +
    "      <div ng-repeat=\"label in term.description\" ng-if=\"label.locale === lang\">\n" +
    "        <span ng-bind-html=\"label.text | dceDescription\" ng-if=\"vocabulary.name === 'dceIds'\"></span>\n" +
    "        <span ng-bind-html=\"label.text\" ng-if=\"vocabulary.name !== 'dceIds'\"></span>\n" +
    "      </div>\n" +
    "      <div ng-if=\"!term.description\" class=\"help-block\" translate>search.no-description</div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/classifications/vocabulary-accordion-group.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/vocabulary-accordion-group.html",
    "<div class=\"panel no-margin no-padding no-border-radius\" ng-class=\"panelClass || 'panel-default'\">\n" +
    "  <div role=\"tab\" id=\"{{::headingId}}\" aria-selected=\"{{isOpen}}\" class=\"panel-heading\" ng-keypress=\"toggleOpen($event)\">\n" +
    "    <h4 class=\"panel-title\">\n" +
    "      <a role=\"button\" data-toggle=\"collapse\" href aria-expanded=\"{{isOpen}}\" aria-controls=\"{{::panelId}}\" tabindex=\"0\" class=\"accordion-toggle\" ng-click=\"toggleOpen()\" uib-accordion-transclude=\"heading\"><small><span uib-accordion-header ng-class=\"{'text-muted': isDisabled}\">{{heading}}</span></small></a>\n" +
    "    </h4>\n" +
    "  </div>\n" +
    "  <div id=\"{{::panelId}}\" aria-labelledby=\"{{::headingId}}\" aria-hidden=\"{{!isOpen}}\" role=\"tabpanel\" class=\"panel-collapse collapse\" uib-collapse=\"!isOpen\">\n" +
    "    <div class=\"panel-body no-padding-top no-padding-bottom\" ng-transclude></div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/classifications/vocabulary-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/vocabulary-panel-template.html",
    "<div>\n" +
    "  <div class=\"panel panel-default\" ng-if=\"vocabulary\">\n" +
    "    <div class=\"panel-heading\">\n" +
    "      <div ng-repeat=\"label in vocabulary.title\" ng-if=\"label.locale === lang\" class=\"clearfix\">\n" +
    "        <a href ng-click=\"onNavigate(taxonomy, vocabulary)\" ng-if=\"vocabulary.terms\">{{label.text}}</a>\n" +
    "        <span ng-if=\"!vocabulary.terms\">{{label.text}}</span>\n" +
    "        <a href ng-click=\"onSelect(target, taxonomy, vocabulary)\">\n" +
    "          <small ng-if=\"vocabulary.terms\"><i class=\"fa fa-plus-circle\" title=\"{{'add-query' | translate}}\"></i></small>\n" +
    "          <small ng-if=\"!vocabulary.terms\"><i class=\"fa fa-plus-circle\" title=\"{{'add-query' | translate}}\"></i></small>\n" +
    "        </a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div class=\"panel-body\">\n" +
    "      <div ng-repeat=\"label in vocabulary.description\" ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </div>\n" +
    "      <div ng-if=\"!vocabulary.description\" class=\"help-block\" translate>search.no-description</div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/coverage/coverage-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/coverage/coverage-search-result-table-template.html",
    "<div>\n" +
    "\n" +
    "  <div ng-if=\"hasVariableTarget()\">\n" +
    "    <ul class=\"nav nav-pills pull-left\">\n" +
    "      <li ng-if=\"groupByOptions.canShowStudy()\"\n" +
    "        ng-class=\"{'active': bucket === BUCKET_TYPES.STUDY || bucket === BUCKET_TYPES.DCE}\">\n" +
    "        <a href ng-click=\"selectBucket(groupByOptions.studyBucket())\" translate>{{groupByOptions.studyTitle()}}</a>\n" +
    "      </li>\n" +
    "      <li ng-if=\"groupByOptions.canShowDataset()\"\n" +
    "        ng-class=\"{'active': bucket === BUCKET_TYPES.DATASET || bucket === BUCKET_TYPES.DATASCHEMA}\">\n" +
    "        <a href ng-click=\"selectBucket(groupByOptions.datasetBucket())\" translate>{{groupByOptions.datasetTitle()}}</a>\n" +
    "      </li>\n" +
    "      <li ng-if=\"groupByOptions.canShowNetwork()\" ng-class=\"{'active': bucket === BUCKET_TYPES.NETWORK}\">\n" +
    "        <a href ng-click=\"selectBucket(BUCKET_TYPES.NETWORK)\" translate>search.coverage-buckets.network</a>\n" +
    "      </li>\n" +
    "    </ul>\n" +
    "\n" +
    "    <div class=\"pull-right\">\n" +
    "      <a ng-if=\"hasSelected()\" href class=\"btn btn-default\" ng-click=\"updateFilterCriteria()\">\n" +
    "        <i class=\"fa fa-filter\"></i> {{'search.filter' | translate}}\n" +
    "      </a>\n" +
    "\n" +
    "      <span ng-if=\"table.taxonomyHeaders.length > 0\" >\n" +
    "        <a href class=\"btn btn-info btn-responsive\" ng-click=\"selectFullAndFilter()\" ng-hide=\"isFullCoverageImpossibleOrCoverageAlreadyFull()\">\n" +
    "          {{'search.coverage-select.full' | translate}}\n" +
    "        </a>\n" +
    "        <a target=\"_self\" class=\"btn btn-info btn-responsive\"\n" +
    "           ng-href=\"{{downloadUrl()}}\">\n" +
    "          <i class=\"fa fa-download\"></i> {{'download' | translate}}\n" +
    "        </a>\n" +
    "      </span>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"clearfix\"></div>\n" +
    "\n" +
    "    <div class=\"voffset2\" ng-if=\"groupByOptions.canShowDce(bucket)\">\n" +
    "      <label class=\"checkbox-inline\">\n" +
    "        <input type=\"checkbox\" ng-model=\"bucketSelection.dceBucketSelected\">\n" +
    "        <span translate>search.coverage-buckets.dce</span>\n" +
    "      </label>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"voffset2\" ng-if=\"groupByOptions.canShowDatasetStudyDataschema(bucket)\">\n" +
    "      <label class=\"radio-inline\">\n" +
    "        <input type=\"radio\" ng-model=\"bucketSelection.datasetBucketSelected\" ng-value=\"true\">\n" +
    "        <span translate>search.coverage-buckets.dataset</span>\n" +
    "      </label>\n" +
    "      <label class=\"radio-inline\">\n" +
    "        <input type=\"radio\" ng-model=\"bucketSelection.datasetBucketSelected\" ng-value=\"false\">\n" +
    "        <span translate>search.coverage-buckets.dataschema</span>\n" +
    "      </label>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <p class=\"help-block\" ng-if=\"!loading && !table.taxonomyHeaders\">\n" +
    "    <span ng-if=\"!hasVariableTarget()\" translate>search.no-coverage</span>\n" +
    "    <span ng-if=\"hasVariableTarget()\" translate>search.no-results</span>\n" +
    "  </p>\n" +
    "\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "\n" +
    "  <div class=\"table-responsive\" ng-if=\"!loading && table.taxonomyHeaders.length > 0\">\n" +
    "    <table class=\"table table-bordered table-striped\">\n" +
    "      <thead>\n" +
    "      <tr>\n" +
    "        <th rowspan=\"2\" width=\"50\" style=\"text-align: center\">\n" +
    "          <div class=\"btn-group voffset1\" uib-dropdown>\n" +
    "            <div uib-dropdown-toggle>\n" +
    "              <small><i class=\"glyphicon glyphicon-unchecked\"></i></small>\n" +
    "              <span class='fa fa-caret-down'></span>\n" +
    "            </div>\n" +
    "            <ul uib-dropdown-menu role=\"menu\">\n" +
    "              <li role=\"menuitem\"><a href ng-click=\"selectAll()\" translate>search.coverage-select.all</a></li>\n" +
    "              <li role=\"menuitem\"><a href ng-click=\"selectNone()\" translate>search.coverage-select.none</a></li>\n" +
    "              <li role=\"menuitem\"><a href ng-click=\"selectFull()\" translate>search.coverage-select.full</a></li>\n" +
    "            </ul>\n" +
    "          </div>\n" +
    "        </th>\n" +
    "        <th rowspan=\"{{bucket === BUCKET_TYPES.DCE ? 1 : 2}}\" colspan=\"{{table.cols.colSpan}}\" translate>\n" +
    "          {{'search.coverage-buckets.' + bucket}}\n" +
    "        </th>\n" +
    "        <th ng-repeat=\"header in table.vocabularyHeaders\" colspan=\"{{header.termsCount}}\">\n" +
    "          <span\n" +
    "            uib-popover=\"{{header.entity.descriptions[0].value}}\"\n" +
    "            popover-title=\"{{header.entity.titles[0].value}}\"\n" +
    "            popover-placement=\"bottom\"\n" +
    "            popover-trigger=\"mouseenter\">\n" +
    "          {{header.entity.titles[0].value}}\n" +
    "          </span>\n" +
    "          <small>\n" +
    "            <a href ng-click=\"removeVocabulary(header)\"><i class=\"fa fa-times\"></i></a>\n" +
    "          </small>\n" +
    "        </th>\n" +
    "      </tr>\n" +
    "      <tr>\n" +
    "        <th ng-if=\"bucket === BUCKET_TYPES.DCE\" translate>search.coverage-dce-cols.study</th>\n" +
    "        <th ng-if=\"bucket === BUCKET_TYPES.DCE\" translate>search.coverage-dce-cols.population</th>\n" +
    "        <th ng-if=\"bucket === BUCKET_TYPES.DCE\" translate>search.coverage-dce-cols.dce</th>\n" +
    "        <th ng-repeat=\"header in table.termHeaders\">\n" +
    "          <span\n" +
    "            uib-popover=\"{{header.entity.descriptions[0].value}}\"\n" +
    "            popover-title=\"{{header.entity.titles[0].value}}\"\n" +
    "            popover-placement=\"bottom\"\n" +
    "            popover-trigger=\"mouseenter\">\n" +
    "          {{header.entity.titles[0].value}}\n" +
    "          </span>\n" +
    "          <small>\n" +
    "            <a ng-if=\"header.canRemove\" href ng-click=\"removeTerm(header)\"><i class=\"fa fa-times\"></i></a>\n" +
    "          </small>\n" +
    "        </th>\n" +
    "      </tr>\n" +
    "      </thead>\n" +
    "      <tbody>\n" +
    "      <tr ng-repeat=\"row in table.rows\" ng-if=\"showMissing || table.termHeaders.length == keys(row.hits).length\">\n" +
    "        <td style=\"text-align: center\">\n" +
    "          <input type=\"checkbox\" ng-model=\"row.selected\">\n" +
    "        </td>\n" +
    "        <td ng-repeat=\"col in table.cols.ids[row.value]\">\n" +
    "          <a ng-if=\"col.rowSpan > 0\" href=\"{{col.url ? col.url : ''}}\"\n" +
    "            uib-popover-html=\"col.description === col.title ? null : col.description\"\n" +
    "            popover-title=\"{{col.title}}\"\n" +
    "            popover-placement=\"bottom\"\n" +
    "            popover-trigger=\"mouseenter\">{{col.title}}</a>\n" +
    "          <div style=\"text-align: center\" ng-if=\"col.start && bucket === BUCKET_TYPES.DCE\">\n" +
    "            <div>\n" +
    "              <small class=\"help-block no-margin\">\n" +
    "                {{col.start}} {{'to' | translate}} {{col.end ? col.end : '...'}}\n" +
    "              </small>\n" +
    "            </div>\n" +
    "            <div class=\"progress no-margin\">\n" +
    "              <div class=\"progress-bar progress-bar-transparent\" role=\"progressbar\"\n" +
    "                aria-valuenow=\"{{col.start}}\" aria-valuemin=\"{{col.min}}\"\n" +
    "                aria-valuemax=\"{{col.start}}\" style=\"{{'width: ' + col.progressStart + '%'}}\">\n" +
    "              </div>\n" +
    "              <div class=\"{{'progress-bar progress-bar-' + col.progressClass}}\" role=\"progressbar\"\n" +
    "                aria-valuenow=\"{{col.current}}\" aria-valuemin=\"{{col.start}}\"\n" +
    "                aria-valuemax=\"{{col.end ? col.end : col.current}}\" style=\"{{'width: ' + col.progress + '%'}}\">\n" +
    "              </div>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "        </td>\n" +
    "        <td ng-repeat=\"h in table.termHeaders\" title=\"{{h.entity.titles[0].value}}\">\n" +
    "          <a href ng-click=\"updateCriteria(row.value, h, $index, 'variables')\"><span class=\"label label-info\"\n" +
    "            ng-if=\"row.hits[$index]\"><localized-number value=\"row.hits[$index]\"></localized-number></span></a>\n" +
    "          <span ng-if=\"!row.hits[$index]\">0</span>\n" +
    "        </td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "      <tfoot>\n" +
    "      <tr>\n" +
    "        <th></th>\n" +
    "        <th colspan=\"{{table.cols.colSpan}}\" translate>all</th>\n" +
    "        <th ng-repeat=\"header in table.termHeaders\" title=\"{{header.entity.descriptions[0].value}}\">\n" +
    "          <a href ng-click=\"updateCriteria(null, header, $index, 'variables')\"><localized-number value=\"header.hits\"></localized-number></a>\n" +
    "        </th>\n" +
    "      </tr>\n" +
    "      </tfoot>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "\n" +
    "  <!--<pre>-->\n" +
    "  <!--{{table.cols | json}}-->\n" +
    "  <!--</pre>-->\n" +
    "</div>");
}]);

angular.module("search/views/criteria/criteria-node-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criteria-node-template.html",
    "<span>\n" +
    "  <span ng-if=\"item.children.length > 0\">\n" +
    "    <criteria-leaf item=\"item.children[0]\" parent-type=\"$parent.item.type\" query=\"query\" advanced=\"advanced\"></criteria-leaf>\n" +
    "\n" +
    "    <div class=\"btn-group voffset1\" ng-show=\"$parent.advanced\" uib-dropdown>\n" +
    "      <button type=\"button\" class=\"btn btn-default btn-xs\" uib-dropdown-toggle>\n" +
    "        {{item.type | translate}} <span class=\"caret\"></span>\n" +
    "      </button>\n" +
    "      <ul uib-dropdown-menu role=\"menu\">\n" +
    "        <li role=\"menuitem\"><a href ng-click=\"updateLogical('or')\" translate>or</a></li>\n" +
    "        <li role=\"menuitem\"><a href ng-click=\"updateLogical('and')\" translate>and</a></li>\n" +
    "      </ul>\n" +
    "    </div>\n" +
    "    <criteria-leaf item=\"item.children[1]\" parent-type=\"$parent.item.type\" query=\"query\" advanced=\"advanced\"></criteria-leaf>\n" +
    "\n" +
    "  </span>\n" +
    "  <span ng-if=\"item.children.length === 0\">\n" +
    "    <criteria-leaf item=\"item\" parent-type=\"item.parent.type\" query=\"query\" advanced=\"advanced\"></criteria-leaf>\n" +
    "  </span>\n" +
    "</span>");
}]);

angular.module("search/views/criteria/criteria-root-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criteria-root-template.html",
    "<div class=\"form-inline\">\n" +
    "  <div ng-repeat=\"child in item.children\" class=\"inline\">\n" +
    "    <div class=\"inline hoffset2\" ng-if=\"$index>0\">+</div>\n" +
    "    <criteria-target item=\"child\" query=\"$parent.query\" advanced=\"$parent.advanced\" class=\"inline\"></criteria-target>\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/criteria/criteria-target-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criteria-target-template.html",
    "<div>\n" +
    "  <div class=\"form-group\" title=\"{{'search.' + item.target + '-where' | translate}}\">\n" +
    "    <i class=\"{{'i-obiba-x-large i-obiba-' + item.target + ' color-' + item.target}}\">&nbsp;</i>\n" +
    "  </div>\n" +
    "  <criteria-node ng-repeat=\"child in item.children\" item=\"child\" query=\"$parent.query\" advanced=\"$parent.advanced\"></criteria-node>\n" +
    "</div>");
}]);

angular.module("search/views/criteria/criterion-dropdown-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criterion-dropdown-template.html",
    "<div id=\"{{criterion.id.replace('.','-')}}-dropdown-{{timestamp}}\" class=\"{{'btn-group voffset1 btn-' + criterion.target}}\" ng-class='{open: state.open}'\n" +
    "     ng-keyup=\"onKeyup($event)\">\n" +
    "\n" +
    "  <button class=\"{{'btn btn-xs dropdown btn-' + criterion.target}}\"\n" +
    "    ng-click=\"openDropdown()\">\n" +
    "    <span uib-popover=\"{{localize(criterion.vocabulary.description ? criterion.vocabulary.description : criterion.vocabulary.title)}}\"\n" +
    "          popover-title=\"{{criterion.vocabulary.description ? localize(criterion.vocabulary.title) : null}}\"\n" +
    "          popover-placement=\"bottom\"\n" +
    "          popover-trigger=\"mouseenter\">\n" +
    "    <i class=\"fa fa-info-circle\"> </i>\n" +
    "  </span>\n" +
    "    <span title=\"{{localizeCriterion()}}\">\n" +
    "    {{truncate(localizeCriterion())}}\n" +
    "    </span>\n" +
    "    <span class='fa fa-caret-down'></span>\n" +
    "  </button>\n" +
    "  <button class='btn btn-xs btn-default' ng-click='remove(criterion.id)'>\n" +
    "    <span class='fa fa-times'></span>\n" +
    "  </button>\n" +
    "\n" +
    "  <match-criterion ng-if=\"RqlQueryUtils.isMatchVocabulary(criterion.vocabulary)\" criterion=\"criterion\" query=\"query\"\n" +
    "                  state=\"state\"></match-criterion>\n" +
    "\n" +
    "  <string-criterion-terms\n" +
    "    ng-if=\"RqlQueryUtils.isTermsVocabulary(criterion.vocabulary) || RqlQueryUtils.isRangeVocabulary(criterion.vocabulary)\"\n" +
    "    criterion=\"criterion\" query=\"query\" state=\"state\"></string-criterion-terms>\n" +
    "\n" +
    "  <numeric-criterion ng-if=\"RqlQueryUtils.isNumericVocabulary(criterion.vocabulary)\" criterion=\"criterion\" query=\"query\"\n" +
    "    state=\"state\"></numeric-criterion>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/criteria/criterion-header-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criterion-header-template.html",
    "<li class=\"criteria-list-item\">\n" +
    "  <label uib-popover=\"{{localize(criterion.vocabulary.description)}}\"\n" +
    "         popover-title=\"{{localize(criterion.vocabulary.title)}}\"\n" +
    "         popover-placement=\"bottom\"\n" +
    "         popover-trigger=\"mouseenter\">\n" +
    "    {{localize(criterion.vocabulary.title)}}\n" +
    "  </label>\n" +
    "  <span class=\"pull-right\" title=\"{{'search.close-and-search' | translate}}\" ng-click=\"$parent.$parent.closeDropdown()\"><i class=\"fa fa-close\"></i></span>\n" +
    "</li>\n" +
    "<li class='divider'></li>\n" +
    "");
}]);

angular.module("search/views/criteria/criterion-match-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criterion-match-template.html",
    "<ul class=\"dropdown-menu query-dropdown-menu\" aria-labelledby=\"{{criterion.vocabulary.name}}-button\">\n" +
    "  <ng-include src=\"'search/views/criteria/criterion-header-template.html'\"></ng-include>\n" +
    "  <li class=\"criteria-list-item\">\n" +
    "    <form novalidate>\n" +
    "      <div>\n" +
    "        <input class=\"form-control\" id=\"{{criterion.vocabulary.name}}-match\"\n" +
    "               ng-model=\"match\"\n" +
    "               ng-change=\"update()\"\n" +
    "               placeholder=\"{{'search.match.placeholder' | translate}}\">\n" +
    "      </div>\n" +
    "    </form>\n" +
    "  </li>\n" +
    "</ul>");
}]);

angular.module("search/views/criteria/criterion-numeric-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criterion-numeric-template.html",
    "<ul class=\"dropdown-menu query-dropdown-menu\" aria-labelledby=\"{{criterion.vocabulary.name}}-button\">\n" +
    "  <ng-include src=\"'search/views/criteria/criterion-header-template.html'\"></ng-include>\n" +
    "  <li class=\"btn-group\">\n" +
    "    <ul class=\"criteria-list-item\">\n" +
    "      <li>\n" +
    "        <label title=\"{{'search.any-help' | translate}}\">\n" +
    "          <input ng-click=\"updateSelection()\" type=\"radio\" ng-model=\"selectMissing\" ng-value=\"false\">\n" +
    "          {{'search.any' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "      <li>\n" +
    "        <label title=\"{{'search.none-help' | translate}}\">\n" +
    "          <input ng-click=\"updateSelection()\" type=\"radio\" ng-model=\"selectMissing\" ng-value=\"true\">\n" +
    "          {{'search.none' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "    </ul>\n" +
    "  </li>\n" +
    "  <li ng-show=\"!selectMissing\" class='divider'></li>\n" +
    "  <li ng-show=\"!selectMissing\" class=\"btn-group criteria-list-item\">\n" +
    "    <form novalidate>\n" +
    "      <div class=\"form-group\">\n" +
    "        <label for=\"{{criterion.vocabulary.name}}-from\" translate>from</label>\n" +
    "        <input type=\"number\" class=\"form-control\" id=\"{{criterion.vocabulary.name}}-from\" placeholder=\"{{min}}\" ng-model=\"from\" style=\"width:150px\">\n" +
    "      </div>\n" +
    "      <div class=\"form-group\">\n" +
    "        <label for=\"{{criterion.vocabulary.name}}-to\" translate>to</label>\n" +
    "        <input type=\"number\" class=\"form-control\" id=\"{{criterion.vocabulary.name}}-to\" placeholder=\"{{max}}\" ng-model=\"to\" style=\"width:150px\">\n" +
    "      </div>\n" +
    "    </form>\n" +
    "  </li>\n" +
    "</ul>");
}]);

angular.module("search/views/criteria/criterion-string-terms-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criterion-string-terms-template.html",
    "<ul class=\"dropdown-menu query-dropdown-menu\" aria-labelledby=\"{{criterion.vocabulary.name}}-button\">\n" +
    "  <ng-include src=\"'search/views/criteria/criterion-header-template.html'\"></ng-include>\n" +
    "  <li class=\"btn-group\">\n" +
    "    <ul class=\"criteria-list-item\">\n" +
    "      <li>\n" +
    "        <label title=\"{{'search.any-help' | translate}}\">\n" +
    "          <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.EXISTS}}\">\n" +
    "          {{'search.any' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "      <li>\n" +
    "        <label title=\"{{'search.none-help' | translate}}\">\n" +
    "          <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.MISSING}}\">\n" +
    "          {{'search.none' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "      <li>\n" +
    "        <label title=\"{{'search.in-help' | translate}}\">\n" +
    "          <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.IN}}\">\n" +
    "          {{'search.in' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "      <li ng-show=\"criterion.vocabulary.repeatable\">\n" +
    "        <label title=\"{{'search.contains-help' | translate}}\">\n" +
    "          <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.CONTAINS}}\">\n" +
    "          {{'search.contains' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "    </ul>\n" +
    "  </li>\n" +
    "  <li ng-show=\"isInFilter() || isContainsFilter()\" class='divider'></li>\n" +
    "  <li class=\"criteria-list-item\" ng-show=\"state.loading\">\n" +
    "    <p class=\"voffset2 loading\">\n" +
    "    </p>\n" +
    "  </li>\n" +
    "  <li ng-show=\"isInFilter() || isContainsFilter()\">\n" +
    "    <ul ng-show=\"!state.loading\" class=\"no-padding criteria-list-terms\">\n" +
    "      <li class=\"criteria-list-item\" ng-show=\"terms && terms.length>10\">\n" +
    "        <span class=\"input-group input-group-sm no-padding-top\">\n" +
    "          <input ng-model=\"searchText\" type=\"text\" class=\"form-control\" aria-describedby=\"term-search\">\n" +
    "          <span class=\"input-group-addon\" id=\"term-search\"><i class=\"glyphicon glyphicon-search\"></i></span>\n" +
    "        </span>\n" +
    "      </li>\n" +
    "      <li ng-show=\"terms && terms.length>10\"></li>\n" +
    "      <li class=\"criteria-list-item\"\n" +
    "        ng-show=\"isInFilter() || isContainsFilter()\"\n" +
    "        ng-repeat=\"term in terms | regex:searchText:['key','title','description']\"\n" +
    "        uib-popover=\"{{term.description ? term.description : (truncate(term.title) === term.title ? null : term.title)}}\"\n" +
    "        popover-title=\"{{term.description ? term.title : null}}\"\n" +
    "        popover-placement=\"bottom\"\n" +
    "        popover-trigger=\"mouseenter\">\n" +
    "          <span>\n" +
    "            <label class=\"control-label\">\n" +
    "              <input ng-model=\"checkboxTerms[term.key]\"\n" +
    "                type=\"checkbox\"\n" +
    "                ng-click=\"updateSelection()\">\n" +
    "              <span>{{truncate(term.title)}}</span>\n" +
    "            </label>\n" +
    "          </span>\n" +
    "          <span class=\"pull-right\">\n" +
    "            <span class=\"agg-term-count\" ng-show=\"isSelectedTerm(term)\">{{term.count}}</span>\n" +
    "            <span class=\"agg-term-count-default\" ng-show=\"!isSelectedTerm(term)\">{{term.count}}</span>\n" +
    "          </span>\n" +
    "      </li>\n" +
    "    </ul>\n" +
    "  </li>\n" +
    "</ul>");
}]);

angular.module("search/views/criteria/target-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/target-template.html",
    "<span></span>");
}]);

angular.module("search/views/graphics/graphics-search-result-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/graphics/graphics-search-result-template.html",
    "<div>\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "\n" +
    "  <p class=\"help-block\" ng-if=\"!loading && noResults\" translate>search.no-results</p>\n" +
    "\n" +
    "  <div ng-repeat=\"chart in chartObjects\" class=\"panel panel-default\">\n" +
    "    <div class=\"panel-heading\">\n" +
    "      {{chart.headerTitle}}\n" +
    "    </div>\n" +
    "    <div class=\"panel-body\">\n" +
    "      <div class=\"row\">\n" +
    "        <div class=\"col-md-6\">\n" +
    "          <div ng-if=\"chart.directiveTitle\" class=\"chart-title\">\n" +
    "            {{chart.directiveTitle}}\n" +
    "          </div>\n" +
    "          <div google-chart chart=\"chart.chartObject\" style=\"min-height:350px; width:100%;\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-6\">\n" +
    "          <div class=\"table-responsive\" ng-if=\"chart.chartObject.data && chart.chartObject.data.length>1\">\n" +
    "            <table style=\"max-height: 400px;\" class=\"table table-bordered table-striped\" fixed-header=\"chart.chartObject.data\">\n" +
    "              <thead>\n" +
    "              <tr>\n" +
    "                <th>{{chart.chartObject.data[0][0]}}</th>\n" +
    "                <th>{{chart.chartObject.data[0][1]}}</th>\n" +
    "                <th ng-if=\"chart.chartObject.data[0][2]\">{{chart.chartObject.data[0][2]}}</th>\n" +
    "              </tr>\n" +
    "              </thead>\n" +
    "              <tbody>\n" +
    "              <tr ng-repeat=\"row in chart.chartObject.entries\">\n" +
    "                <td>{{row.title}}</td>\n" +
    "                <td><a href ng-click=\"updateCriteria(row.key, chart.chartObject.vocabulary)\"><localized-number value=\"row.value\"></localized-number></a></td>\n" +
    "                <td ng-if=\"row.participantsNbr\">{{row.participantsNbr}}</td>\n" +
    "              </tr>\n" +
    "              </tbody>\n" +
    "            </table>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/list/datasets-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/list/datasets-search-result-table-template.html",
    "<div>\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "  <div ng-show=\"!loading\">\n" +
    "    <p class=\"help-block\" ng-if=\"!summaries || !summaries.length\" translate>search.dataset.noResults</p>\n" +
    "    <div class=\"table-responsive\" ng-if=\"summaries && summaries.length\">\n" +
    "      <table class=\"table table-bordered table-striped\" ng-init=\"lang = $parent.$parent.lang\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th translate ng-if=\"optionsCols.showDatasetsAcronymColumn\">acronym</th>\n" +
    "          <th translate>name</th>\n" +
    "          <th translate ng-if=\"optionsCols.showDatasetsTypeColumn\">type</th>\n" +
    "          <th translate ng-if=\"optionsCols.showDatasetsNetworkColumn\">networks</th>\n" +
    "          <th translate ng-if=\"optionsCols.showDatasetsStudiesColumn\">studies</th>\n" +
    "          <th translate ng-if=\"optionsCols.showDatasetsVariablesColumn\">variables</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "        <tr ng-if=\"!summaries || !summaries.length\">\n" +
    "          <td colspan=\"6\" translate>search.dataset.noResults</td>\n" +
    "        </tr>\n" +
    "        <tr ng-repeat=\"summary in summaries\">\n" +
    "          <td ng-if=\"optionsCols.showDatasetsAcronymColumn\">\n" +
    "            <a ng-href=\"{{PageUrlService.datasetPage(summary.id, summary.variableType)}}\">\n" +
    "              <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <a ng-if=\"!optionsCols.showDatasetsAcronymColumn\" ng-href=\"{{PageUrlService.datasetPage(summary.id, summary.variableType)}}\">\n" +
    "              <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "            <localized ng-if=\"optionsCols.showDatasetsAcronymColumn\" value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showDatasetsTypeColumn\">\n" +
    "            <localized value=\"classNames[(summary.variableType === 'Study' ? 'Study' : 'Harmonization') + 'Dataset']\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showDatasetsNetworkColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'networks')\" ng-if=\"summary['obiba.mica.CountStatsDto.datasetCountStats'].networks\"><localized-number value=\"summary['obiba.mica.CountStatsDto.datasetCountStats'].networks\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.datasetCountStats'].networks\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showDatasetsStudiesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'studies')\" ng-if=\"summary['obiba.mica.CountStatsDto.datasetCountStats'].studies\"><localized-number value=\"summary['obiba.mica.CountStatsDto.datasetCountStats'].studies\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.datasetCountStats'].studies\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showDatasetsVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'variables')\"><localized-number value=\"summary['obiba.mica.CountStatsDto.datasetCountStats'].variables\"></localized-number></a>\n" +
    "          </td>\n" +
    "        </tr>\n" +
    "        </tbody>\n" +
    "      </table>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/list/networks-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/list/networks-search-result-table-template.html",
    "<div>\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "  <div ng-show=\"!loading\">\n" +
    "    <p class=\"help-block\" ng-if=\"!summaries || !summaries.length\" translate>search.network.noResults</p>\n" +
    "    <div class=\"table-responsive\" ng-if=\"summaries && summaries.length\">\n" +
    "      <table class=\"table table-bordered table-striped\" ng-init=\"lang = $parent.$parent.lang\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th rowspan=\"2\" translate>acronym</th>\n" +
    "          <th rowspan=\"2\" translate>name</th>\n" +
    "          <th rowspan=\"2\" translate ng-if=\"optionsCols.showNetworksStudiesColumn\">studies</th>\n" +
    "          <th translate\n" +
    "              ng-attr-colspan=\"{{optionsCols.showNetworksStudyDatasetColumn + optionsCols.showNetworksHarmonizationDatasetColumn}}\"\n" +
    "              ng-if=\"optionsCols.showNetworksStudyDatasetColumn || optionsCols.showNetworksHarmonizationDatasetColumn\">\n" +
    "            datasets\n" +
    "          </th>\n" +
    "          <th rowspan=\"2\" translate ng-if=\"optionsCols.showNetworksVariablesColumn\">variables</th>\n" +
    "          <th translate\n" +
    "              ng-attr-colspan=\"{{optionsCols.showNetworksStudyVariablesColumn + optionsCols.showNetworksDataschemaVariablesColumn}}\"\n" +
    "              ng-if=\"optionsCols.showNetworksStudyVariablesColumn || optionsCols.showNetworksDataschemaVariablesColumn\">variables</th>\n" +
    "        </tr>\n" +
    "        </tr>\n" +
    "        <tr>\n" +
    "          <th translate ng-if=\"optionsCols.showNetworksStudyDatasetColumn\">search.study.label</th>\n" +
    "          <th translate ng-if=\"optionsCols.showNetworksHarmonizationDatasetColumn\">search.harmonization</th>\n" +
    "          <th translate ng-if=\"optionsCols.showNetworksStudyVariablesColumn\">search.variable.study</th>\n" +
    "          <th translate ng-if=\"optionsCols.showNetworksDataschemaVariablesColumn\">search.variable.dataschema</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "        <tr ng-if=\"!summaries || !summaries.length\">\n" +
    "          <td colspan=\"6\" translate>search.network.noResults</td>\n" +
    "        </tr>\n" +
    "        <tr ng-repeat=\"summary in summaries\">\n" +
    "          <td>\n" +
    "            <a ng-href=\"{{PageUrlService.networkPage(summary.id)}}\">\n" +
    "              <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showNetworksStudiesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'studies')\" ng-if=\"summary['obiba.mica.CountStatsDto.networkCountStats'].studies\"><localized-number value=\"summary['obiba.mica.CountStatsDto.networkCountStats'].studies\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.networkCountStats'].studies\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showNetworksStudyDatasetColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'StudyDataset')\" ng-if=\"summary['obiba.mica.CountStatsDto.networkCountStats'].studyDatasets\"><localized-number value=\"summary['obiba.mica.CountStatsDto.networkCountStats'].studyDatasets\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.networkCountStats'].studyDatasets\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showNetworksHarmonizationDatasetColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'HarmonizationDataset')\" ng-if=\"summary['obiba.mica.CountStatsDto.networkCountStats'].harmonizationDatasets\"><localized-number value=\"summary['obiba.mica.CountStatsDto.networkCountStats'].harmonizationDatasets\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.networkCountStats'].harmonizationDatasets\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showNetworksVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'variables')\"><localized-number value=\"summary['obiba.mica.CountStatsDto.networkCountStats'].variables\"></localized-number></a>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showNetworksStudyVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'StudyVariable')\" ng-if=\"summary['obiba.mica.CountStatsDto.networkCountStats'].studyDatasets\"><localized-number value=\"summary['obiba.mica.CountStatsDto.networkCountStats'].studyVariables\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.networkCountStats'].studyDatasets\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showNetworksDataschemaVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'DataschemaVariable')\" ng-if=\"summary['obiba.mica.CountStatsDto.networkCountStats'].harmonizationDatasets\"><localized-number value=\"summary['obiba.mica.CountStatsDto.networkCountStats'].dataschemaVariables\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.networkCountStats'].harmonizationDatasets\">-</span>\n" +
    "          </td>\n" +
    "        </tr>\n" +
    "        </tbody>\n" +
    "      </table>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/list/pagination-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/list/pagination-template.html",
    "<span class=\"input-group voffset1\">\n" +
    "  <ul class=\"pagination pagination-sm no-padding no-margin\">\n" +
    "    <li ng-if=\"::boundaryLinks\" ng-class=\"{disabled: noPrevious()||ngDisabled}\" class=\"pagination-first\">\n" +
    "      <a href ng-click=\"selectPage(1, $event)\">{{::getText('first')}}</a>\n" +
    "    </li>\n" +
    "    <li ng-if=\"::directionLinks\" ng-class=\"{disabled: noPrevious()||ngDisabled}\" class=\"pagination-prev\">\n" +
    "      <a href ng-click=\"selectPage(page - 1, $event)\">{{::getText('previous')}}</a>\n" +
    "    </li>\n" +
    "    <li ng-repeat=\"page in pages track by $index\" ng-class=\"{active: page.active,disabled: ngDisabled&&!page.active}\"\n" +
    "        class=\"pagination-page\">\n" +
    "      <a href ng-click=\"selectPage(page.number, $event)\">{{page.text}}</a>\n" +
    "    </li>\n" +
    "    <li ng-if=\"::directionLinks\" ng-class=\"{disabled: noNext()||ngDisabled}\" class=\"pagination-next\">\n" +
    "      <a href ng-click=\"selectPage(page + 1, $event)\">{{::getText('next')}}</a>\n" +
    "    </li>\n" +
    "    <li ng-if=\"::boundaryLinks\" ng-class=\"{disabled: noNext()||ngDisabled}\" class=\"pagination-last\">\n" +
    "      <a href ng-click=\"selectPage(totalPages, $event)\">{{::getText('last')}}</a>\n" +
    "    </li>\n" +
    "  </ul>\n" +
    "  <ul class=\"pagination no-margin pagination-sm\" ng-show=\"1 < totalPages\">\n" +
    "    <li>\n" +
    "      <a href ng-show=\"1 < totalPages\" class=\"pagination-total\">{{$parent.pagination.from}} - {{$parent.pagination.to}} {{'of' | translate}} {{totalItems}}</a>\n" +
    "    </li>\n" +
    "  </ul>\n" +
    "</span>");
}]);

angular.module("search/views/list/search-result-pagination-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/list/search-result-pagination-template.html",
    "<span ng-show=\"totalHits > 10\" class=\"form-inline\">\n" +
    "  <span class=\"input-group\">\n" +
    "    <select class=\"form-control form-select\"\n" +
    "            ng-model=\"pagination.selected\"\n" +
    "            ng-options=\"size.label for size in pageSizes\"\n" +
    "            ng-change=\"pageSizeChanged()\"></select>\n" +
    "  </span>\n" +
    "\n" +
    "  <span ng-show=\"maxSize > 1\"\n" +
    "        uib-pagination\n" +
    "        total-items=\"totalHits\"\n" +
    "        max-size=\"maxSize\"\n" +
    "        ng-model=\"pagination.currentPage\"\n" +
    "        boundary-links=\"true\"\n" +
    "        force-ellipses=\"true\"\n" +
    "        items-per-page=\"pagination.selected.value\"\n" +
    "        previous-text=\"&lsaquo;\"\n" +
    "        next-text=\"&rsaquo;\"\n" +
    "        first-text=\"&laquo;\"\n" +
    "        last-text=\"&raquo;\"\n" +
    "        template-url=\"search/views/list/pagination-template.html\"\n" +
    "        ng-change=\"pageChanged()\">\n" +
    "  </span>\n" +
    "</span>");
}]);

angular.module("search/views/list/studies-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/list/studies-search-result-table-template.html",
    "<div>\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "  <div ng-show=\"!loading\">\n" +
    "    <p class=\"help-block\" ng-if=\"!summaries || !summaries.length\" translate>search.study.noResults</p>\n" +
    "    <div class=\"table-responsive\" ng-if=\"summaries && summaries.length\">\n" +
    "      <table class=\"table table-bordered table-striped\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th rowspan=\"2\" translate>acronym</th>\n" +
    "          <th rowspan=\"2\" translate>name</th>\n" +
    "          <th rowspan=\"2\" translate ng-if=\"optionsCols.showStudiesDesignColumn\">search.study.design</th>\n" +
    "          <th translate\n" +
    "              ng-attr-colspan=\"{{optionsCols.showStudiesQuestionnaireColumn + optionsCols.showStudiesPmColumn + optionsCols.showStudiesBioColumn + optionsCols.showStudiesOtherColumn}}\"\n" +
    "              ng-if=\"optionsCols.showStudiesQuestionnaireColumn || optionsCols.showStudiesPmColumn || optionsCols.showStudiesBioColumn || optionsCols.showStudiesOtherColumn\">\n" +
    "            search.study.dataSources\n" +
    "          </th>\n" +
    "          <th rowspan=\"2\" translate>search.study.participants</th>\n" +
    "          <th rowspan=\"2\" translate ng-if=\"optionsCols.showStudiesNetworksColumn\">networks</th>\n" +
    "          <th translate\n" +
    "              ng-attr-colspan=\"{{optionsCols.showStudiesStudyDatasetsColumn + optionsCols.showStudiesHarmonizationDatasetsColumn}}\"\n" +
    "              ng-if=\"optionsCols.showStudiesStudyDatasetsColumn || optionsCols.showStudiesHarmonizationDatasetsColumn\">datasets\n" +
    "          </th>\n" +
    "          <th rowspan=\"2\" translate ng-if=\"optionsCols.showStudiesVariablesColumn\">variables</th>\n" +
    "          <th translate\n" +
    "              ng-attr-colspan=\"{{optionsCols.showStudiesStudyVariablesColumn + optionsCols.showStudiesDataschemaVariablesColumn}}\"\n" +
    "              ng-if=\"optionsCols.showStudiesStudyVariablesColumn || optionsCols.showStudiesDataschemaVariablesColumn\">variables</th>\n" +
    "        </tr>\n" +
    "        <tr>\n" +
    "          <th class=\"text-nowrap\" title=\"{{datasourceTitles.questionnaires.title}}\" ng-if=\"optionsCols.showStudiesQuestionnaireColumn\">\n" +
    "            <i class=\"fa fa-file-text-o\"></i>\n" +
    "          </th>\n" +
    "          <th class=\"text-nowrap\" title=\"{{datasourceTitles.physical_measures.title}}\" ng-if=\"optionsCols.showStudiesPmColumn\">\n" +
    "            <i class=\"fa fa-stethoscope\"></i>\n" +
    "          </th>\n" +
    "          <th class=\"text-nowrap\"  title=\"{{datasourceTitles.biological_samples.title}}\" ng-if=\"optionsCols.showStudiesBioColumn\">\n" +
    "            <i class=\"fa fa-flask\"></i>\n" +
    "          </th>\n" +
    "          <th class=\"text-nowrap\"  title=\"{{datasourceTitles.others.title}}\" ng-if=\"optionsCols.showStudiesOtherColumn\">\n" +
    "            <i class=\"fa fa-plus-square-o\"></i>\n" +
    "          </th>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesStudyDatasetsColumn\">search.study.label</th>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesHarmonizationDatasetsColumn\">search.harmonization</th>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesStudyVariablesColumn\">search.variable.study</th>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesDataschemaVariablesColumn\">search.variable.dataschema</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "        <tr ng-repeat=\"summary in summaries\" ng-init=\"lang = $parent.$parent.lang\">\n" +
    "          <td>\n" +
    "            <a ng-href=\"{{PageUrlService.studyPage(summary.id)}}\">\n" +
    "              <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized ng-repeat=\"d in summary.designs\" value=\"designs[d]\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <i class=\"fa fa-check\" ng-if=\"hasDatasource(summary.dataSources, 'questionnaires')\"></i><span\n" +
    "              ng-if=\"!hasDatasource(summary.dataSources, 'questionnaires')\">-</span>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <i class=\"fa fa-check\" ng-if=\"hasDatasource(summary.dataSources, 'physical_measures')\"></i><span\n" +
    "              ng-if=\"!hasDatasource(summary.dataSources, 'physical_measures')\">-</span>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <i class=\"fa fa-check\" ng-if=\"hasDatasource(summary.dataSources, 'biological_samples')\"></i><span\n" +
    "              ng-if=\"!hasDatasource(summary.dataSources, 'biological_samples')\">-</span>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <i class=\"fa fa-check\" ng-if=\"hasDatasource(summary.dataSources, 'others')\"></i><span\n" +
    "              ng-if=\"!hasDatasource(summary.dataSources, 'others')\">-</span>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized-number value=\"summary.targetNumber.number\"></localized-number>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesNetworksColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'networks')\"\n" +
    "                ng-if=\"summary['obiba.mica.CountStatsDto.studyCountStats'].networks\"><localized-number value=\"summary['obiba.mica.CountStatsDto.studyCountStats'].networks\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.studyCountStats'].networks\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesStudyDatasetsColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'StudyDataset')\"\n" +
    "                ng-if=\"summary['obiba.mica.CountStatsDto.studyCountStats'].studyDatasets\"><localized-number value=\"summary['obiba.mica.CountStatsDto.studyCountStats'].studyDatasets\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.studyCountStats'].studyDatasets\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesHarmonizationDatasetsColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'HarmonizationDataset')\"\n" +
    "                ng-if=\"summary['obiba.mica.CountStatsDto.studyCountStats'].harmonizationDatasets\"><localized-number value=\"summary['obiba.mica.CountStatsDto.studyCountStats'].harmonizationDatasets\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.studyCountStats'].harmonizationDatasets\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'variables')\"><localized-number value=\"summary['obiba.mica.CountStatsDto.studyCountStats'].variables\"></localized-number></a>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesStudyVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'StudyVariable')\" ng-if=\"summary['obiba.mica.CountStatsDto.studyCountStats'].studyDatasets\"><localized-number value=\"summary['obiba.mica.CountStatsDto.studyCountStats'].studyVariables\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.studyCountStats'].studyDatasets\">-</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesDataschemaVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'DataschemaVariable')\" ng-if=\"summary['obiba.mica.CountStatsDto.studyCountStats'].harmonizationDatasets\"><localized-number value=\"summary['obiba.mica.CountStatsDto.studyCountStats'].dataschemaVariables\"></localized-number></a>\n" +
    "            <span ng-if=\"!summary['obiba.mica.CountStatsDto.studyCountStats'].harmonizationDatasets\">-</span>\n" +
    "          </td>\n" +
    "        </tr>\n" +
    "        </tbody>\n" +
    "      </table>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/list/variables-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/list/variables-search-result-table-template.html",
    "<div>\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "  <div ng-show=\"!loading\">\n" +
    "    <p class=\"help-block\" ng-if=\"!summaries || !summaries.length\" translate>search.variable.noResults</p>\n" +
    "    <div class=\"table-responsive\" ng-if=\"summaries && summaries.length\">\n" +
    "      <table class=\"table table-bordered table-striped\" ng-init=\"lang = $parent.$parent.lang\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th translate>name</th>\n" +
    "          <th translate>search.variable.label</th>\n" +
    "          <th translate ng-if=\"optionsCols.showVariablesTypeColumn\">type</th>\n" +
    "          <th translate ng-if=\"optionsCols.showVariablesStudiesColumn\">search.variable.studyNetwork</th>\n" +
    "          <th translate ng-if=\"optionsCols.showVariablesDatasetsColumn\">search.dataset.label</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tr ng-repeat=\"summary in summaries\">\n" +
    "          <td>\n" +
    "            <a\n" +
    "              href=\"{{PageUrlService.variablePage(summary.id) ? PageUrlService.variablePage(summary.id) : PageUrlService.datasetPage(summary.datasetId, summary.variableType)}}\">\n" +
    "              {{summary.name}}\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.variableLabel\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showVariablesTypeColumn\">\n" +
    "            {{'search.variable.' + summary.variableType.toLowerCase() | translate}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showVariablesStudiesColumn\">\n" +
    "            <a ng-if=\"summary.studyId\" ng-href=\"{{PageUrlService.studyPage(summary.studyId)}}\">\n" +
    "              <localized value=\"summary.studyAcronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "            <a ng-if=\"summary.networkId\" ng-href=\"{{PageUrlService.networkPage(summary.networkId)}}\">\n" +
    "              <localized value=\"summary.networkAcronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showVariablesDatasetsColumn\">\n" +
    "            <a ng-href=\"{{PageUrlService.datasetPage(summary.datasetId, summary.variableType)}}\">\n" +
    "              <localized value=\"summary.datasetAcronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "        </tr>\n" +
    "        </tbody>\n" +
    "      </table>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/search-result-coverage-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-coverage-template.html",
    "<div ng-show=\"display === 'coverage'\">\n" +
    "  <coverage-result-table result=\"result.coverage\" loading=\"loading\" bucket=\"bucket\" query=\"query\"\n" +
    "      class=\"voffset2\" on-update-criteria=\"onUpdateCriteria\" on-remove-criteria=\"onRemoveCriteria\"></coverage-result-table>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search-result-graphics-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-graphics-template.html",
    "<div ng-show=\"display === 'graphics'\">\n" +
    "  <graphics-result on-update-criteria=\"onUpdateCriteria\" result=\"result.graphics\" loading=\"loading\" class=\"voffset2\"></graphics-result>\n" +
    "</div>");
}]);

angular.module("search/views/search-result-list-dataset-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-list-dataset-template.html",
    "<div class=\"tab-pane\" ng-show=\"options.datasets.showSearchTab\" ng-class=\"{'active': activeTarget.datasets.active}\">\n" +
    "  <div class=\"voffset2\" ng-if=\"resultTabsOrder.length === 1\">{{'datasets' | translate}} ({{result.list.datasetResultDto.totalHits}})</div>\n" +
    "  <datasets-result-table loading=\"loading\" on-update-criteria=\"onUpdateCriteria\"\n" +
    "      summaries=\"result.list.datasetResultDto['obiba.mica.DatasetResultDto.result'].datasets\"></datasets-result-table>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search-result-list-network-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-list-network-template.html",
    "<div class=\"tab-pane\" ng-show=\"options.networks.showSearchTab\" ng-class=\"{'active': activeTarget.networks.active}\">\n" +
    "  <div class=\"voffset2\" ng-if=\"resultTabsOrder.length === 1\">{{'networks' | translate}} ({{result.list.networkResultDto.totalHits}})</div>\n" +
    "  <networks-result-table loading=\"loading\" on-update-criteria=\"onUpdateCriteria\"\n" +
    "      summaries=\"result.list.networkResultDto['obiba.mica.NetworkResultDto.result'].networks\"></networks-result-table>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search-result-list-study-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-list-study-template.html",
    "<div class=\"tab-pane\" ng-show=\"options.studies.showSearchTab\" ng-class=\"{'active': activeTarget.studies.active}\">\n" +
    "  <div class=\"voffset2\" ng-if=\"resultTabsOrder.length === 1\">{{'studies' | translate}} ({{result.list.studyResultDto.totalHits}})</div>\n" +
    "  <studies-result-table lang=\"lang\" loading=\"loading\" on-update-criteria=\"onUpdateCriteria\"\n" +
    "      summaries=\"result.list.studyResultDto['obiba.mica.StudyResultDto.result'].summaries\"></studies-result-table>\n" +
    "</div>");
}]);

angular.module("search/views/search-result-list-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-list-template.html",
    "<div ng-show=\"display === 'list'\">\n" +
    "  <ul class=\"nav nav-pills voffset2\" ng-if=\"resultTabsOrder.length > 1\">\n" +
    "    <li role=\"presentation\" ng-repeat=\"res in resultTabsOrder\"\n" +
    "        ng-class=\"{active: activeTarget[targetTypeMap[res]].active}\"\n" +
    "        ng-if=\"options[targetTypeMap[res]].showSearchTab\">\n" +
    "      <a href\n" +
    "        ng-click=\"selectTarget(targetTypeMap[res])\">\n" +
    "      {{targetTypeMap[res] | translate}}\n" +
    "      ({{result.list[res + 'ResultDto'].totalHits === 0 ? 0 : (result.list[res + 'ResultDto'].totalHits | localizedNumber)}})\n" +
    "    </a>\n" +
    "    </li>\n" +
    "    <li ng-repeat=\"res in resultTabsOrder\" ng-show=\"activeTarget[targetTypeMap[res]].active\" class=\"pull-right\">\n" +
    "      <span search-result-pagination\n" +
    "            target=\"activeTarget[targetTypeMap[res]].name\"\n" +
    "            total-hits=\"activeTarget[targetTypeMap[res]].totalHits\"\n" +
    "            on-change=\"onPaginate\"></span>\n" +
    "    </li>\n" +
    "  </ul>\n" +
    "  <div class=\"tab-content\">\n" +
    "    <ng-include include-replace ng-repeat=\"res in resultTabsOrder\"\n" +
    "        src=\"'search/views/search-result-list-' + res + '-template.html'\"></ng-include>\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search-result-list-variable-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-list-variable-template.html",
    "<div class=\"tab-pane\" ng-show=\"options.variables.showSearchTab\" ng-class=\"{'active': activeTarget.variables.active}\">\n" +
    "  <div class=\"voffset2\" ng-if=\"resultTabsOrder.length === 1\">{{'variables' | translate}} ({{result.list.variableResultDto.totalHits}})</div>\n" +
    "  <variables-result-table loading=\"loading\"\n" +
    "      summaries=\"result.list.variableResultDto['obiba.mica.DatasetVariableResultDto.result'].summaries\"></variables-result-table>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search-result-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-panel-template.html",
    "<div>\n" +
    "  <ng-include include-replace ng-repeat=\"tab in searchTabsOrder\"\n" +
    "    src=\"'search/views/search-result-' + tab + '-template.html'\"></ng-include>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search.html",
    "<div ng-show=\"inSearchMode()\">\n" +
    "  <div class=\"container alert-fixed-position\">\n" +
    "    <obiba-alert id=\"SearchController\"></obiba-alert>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"alert-growl-container\">\n" +
    "    <obiba-alert id=\"SearchControllerGrowl\"></obiba-alert>\n" +
    "  </div>\n" +
    "\n" +
    "  <div ng-if=\"searchHeaderTemplateUrl\" ng-include=\"searchHeaderTemplateUrl\"></div>\n" +
    "\n" +
    "  <!-- Lang tabs -->\n" +
    "  <ul class=\"nav nav-tabs\" role=\"tablist\" ng-if=\"tabs && tabs.length>1\">\n" +
    "    <li ng-repeat=\"tab in tabs\" role=\"presentation\" ng-class=\"{ active: tab === lang }\"><a href role=\"tab\"\n" +
    "      ng-click=\"setLocale(tab)\">{{'language.' + tab | translate}}</a></li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <div class=\"row voffset2\">\n" +
    "    <div class=\"col-md-3\" ng-if=\"hasFacetedTaxonomies\" >\n" +
    "      <!-- Search Facets region -->\n" +
    "      <taxonomies-facets-panel id=\"search-facets-region\" faceted-taxonomies=\"facetedTaxonomies\" criteria=\"search.criteria\" on-select-term=\"onSelectTerm\"\n" +
    "                    on-refresh=\"refreshQuery\" lang=\"lang\"></taxonomies-facets-panel>\n" +
    "    </div>\n" +
    "    <div class=\"{{hasFacetedTaxonomies ? 'col-md-9' : 'col-md-12'}}\">\n" +
    "\n" +
    "      <!-- Search box region -->\n" +
    "      <div id=\"search-region\" class=\"{{tabs && tabs.length>1 ? 'tab-content voffset4' : ''}}\">\n" +
    "        <div ng-if=\"options.showSearchBox\" id=\"search-box-region\" class=\"{{hasFacetedTaxonomies ? '' : 'row'}}\">\n" +
    "          <div class=\"{{hasFacetedTaxonomies ? '' : 'col-md-3'}}\"></div>\n" +
    "          <div class=\"{{hasFacetedTaxonomies ? '' : 'col-md-6'}}\">\n" +
    "            <script type=\"text/ng-template\" id=\"customTemplate.html\">\n" +
    "              <a ng-if=\"match.model.id\">\n" +
    "                <table style=\"border:none;\">\n" +
    "                  <tbody>\n" +
    "                  <tr>\n" +
    "                    <td style=\"min-width: 30px;\">\n" +
    "                  <span title=\"{{match.model.target + '-classifications' | translate}}\">\n" +
    "                    <i class=\"{{'i-obiba-large i-obiba-' + match.model.target}}\"></i>\n" +
    "                  </span>\n" +
    "                    </td>\n" +
    "                    <td>\n" +
    "                  <span\n" +
    "                          uib-popover-html=\"match.model.itemDescription | uibTypeaheadHighlight:query\"\n" +
    "                          popover-title=\"{{match.model.itemTitle}}\"\n" +
    "                          popover-placement=\"bottom\"\n" +
    "                          popover-trigger=\"mouseenter\"\n" +
    "                          ng-bind-html=\"match.model.itemTitle | uibTypeaheadHighlight:query\">\n" +
    "                  </span>\n" +
    "                      <small class=\"help-block no-margin\" title=\"{{match.model.itemParentDescription}}\">\n" +
    "                        {{match.model.itemParentTitle}}\n" +
    "                      </small>\n" +
    "                    </td>\n" +
    "                  </tr>\n" +
    "                  </tbody>\n" +
    "                </table>\n" +
    "              </a>\n" +
    "              <a ng-if=\"!match.model.id\" class=\"{{match.model.status}}\">\n" +
    "                <small class=\"help-block no-margin\">\n" +
    "                  {{match.model.message}}\n" +
    "                </small>\n" +
    "              </a>\n" +
    "            </script>\n" +
    "          <span class=\"input-group input-group-sm\">\n" +
    "            <span class=\"input-group-btn\" uib-dropdown>\n" +
    "              <button type=\"button\" class=\"btn btn-primary\" uib-dropdown-toggle>\n" +
    "                {{'taxonomy.target.' + (documents.search.target ? documents.search.target : 'all')| translate}} <span\n" +
    "                      class=\"caret\"></span>\n" +
    "              </button>\n" +
    "              <ul uib-dropdown-menu role=\"menu\">\n" +
    "                <li>\n" +
    "                  <a href ng-click=\"selectSearchTarget()\" translate>taxonomy.target.all</a>\n" +
    "                </li>\n" +
    "                <li ng-repeat=\"target in targets\" role=\"menuitem\"><a href ng-click=\"selectSearchTarget(target)\">{{'taxonomy.target.'\n" +
    "                  + target | translate}}</a></li>\n" +
    "              </ul>\n" +
    "            </span>\n" +
    "            <input type=\"text\" ng-model=\"$parent.selectedCriteria\"\n" +
    "                   placeholder=\"{{'search.placeholder.' + (documents.search.target ? documents.search.target : 'all') | translate}}\"\n" +
    "                   uib-typeahead=\"criteria for criteria in searchCriteria($viewValue)\"\n" +
    "                   typeahead-min-length=\"2\"\n" +
    "                   typeahead-loading=\"documents.search.active\"\n" +
    "                   typeahead-template-url=\"customTemplate.html\"\n" +
    "                   typeahead-on-select=\"selectCriteria($item)\"\n" +
    "                   class=\"form-control\">\n" +
    "            <span class=\"input-group-addon\"><i class=\"glyphicon glyphicon-search\"></i></span>\n" +
    "            <span ng-if=\"options.SearchHelpLinkUrl\" class=\"input-group-btn\">\n" +
    "              <a type=\"button\" target=\"_blank\" class=\"btn btn-default\" href=\"{{options.SearchHelpLinkUrl}}\">\n" +
    "                <span class=\"glyphicon glyphicon-question-sign\"></span> {{options.SearchHelpLinkLabel}}</a>\n" +
    "            </span>\n" +
    "          </span>\n" +
    "\n" +
    "          </div>\n" +
    "        </div>\n" +
    "        <div ng-if=\"options.showSearchBrowser\" id=\"search-selector-region\" class=\"{{hasFacetedTaxonomies ? '' : 'row'}}\">\n" +
    "          <div class=\"{{hasFacetedTaxonomies ? '' : 'col-md-3'}}\"></div>\n" +
    "          <div class=\"{{hasFacetedTaxonomies ? '' : 'col-md-6'}}\">\n" +
    "            <small>\n" +
    "              <ul class=\"nav nav-pills\">\n" +
    "                <li ng-if=\"hasClassificationsTitle\">\n" +
    "                  <label class=\"nav-label\" translate>search.classifications-title</label>\n" +
    "                </li>\n" +
    "                <li ng-repeat=\"t in taxonomyNav track by $index\" title=\"{{t.locale.description.text}}\">\n" +
    "                  <a href ng-click=\"showTaxonomy(t.target, t.name)\" ng-if=\"!t.terms\">{{t.locale.title.text}}</a>\n" +
    "            <span uib-dropdown ng-if=\"t.terms\">\n" +
    "              <ul class=\"nav nav-pills\">\n" +
    "                <li>\n" +
    "                  <a href uib-dropdown-toggle>{{t.locale.title.text}} <span class=\"caret\"></span></a>\n" +
    "                </li>\n" +
    "              </ul>\n" +
    "              <ul uib-dropdown-menu>\n" +
    "                <li ng-repeat=\"st in t.terms\">\n" +
    "                  <a href ng-click=\"showTaxonomy(t.target, st.name)\" title=\"{{st.locale.description.text}}\">{{st.locale.title.text}}</a>\n" +
    "                </li>\n" +
    "              </ul>\n" +
    "            </span>\n" +
    "                </li>\n" +
    "                <li>\n" +
    "                  <a href ng-click=\"goToClassifications()\" title=\"{{'search.classifications-show' | translate}}\">\n" +
    "                    <span ng-if=\"hasClassificationsLinkLabel\" translate>search.classifications-link</span>\n" +
    "                    <i class=\"glyphicon glyphicon-option-horizontal\" ng-if=\"!hasClassificationsLinkLabel\"></i>\n" +
    "                  </a>\n" +
    "                </li>\n" +
    "              </ul>\n" +
    "            </small>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "        <taxonomies-panel ng-if=\"options.showSearchBrowser\" taxonomy-name=\"taxonomyName\" target=\"target\" on-select-term=\"onSelectTerm\"\n" +
    "                          on-close=\"clearTaxonomy\" lang=\"lang\" taxonomies-shown=\"taxonomiesShown\"></taxonomies-panel>\n" +
    "      </div>\n" +
    "\n" +
    "      <div ng-if=\"hasFacetedTaxonomies && hasFacetedNavigationHelp && !(search.criteria.children && search.criteria.children.length > 0)\">\n" +
    "        <p class=\"help-block\" translate>search.faceted-navigation-help</p>\n" +
    "      </div>\n" +
    "\n" +
    "      <!-- Search criteria region -->\n" +
    "      <div id=\"search-criteria-region\" ng-class=\"options.showSearchBox || options.showSearchBrowser ? 'voffset2' : ''\" class=\"panel panel-default\" ng-if=\"search.criteria.children && search.criteria.children.length>0\">\n" +
    "        <div class=\"panel-body\">\n" +
    "          <table style=\"border:none\">\n" +
    "            <tbody>\n" +
    "            <tr>\n" +
    "              <td>\n" +
    "                <a href class=\"btn btn-sm btn-default\" ng-click=\"clearSearchQuery()\" translate>clear</a>\n" +
    "              </td>\n" +
    "              <td style=\"padding-left: 10px\">\n" +
    "                <div criteria-root item=\"search.criteria\" query=\"search.query\" advanced=\"search.advanced\" on-remove=\"removeCriteriaItem\"\n" +
    "                     on-refresh=\"refreshQuery\" class=\"inline\"></div>\n" +
    "\n" +
    "                <small class=\"hoffset2\" ng-if=\"showAdvanced()\">\n" +
    "                  <a href ng-click=\"toggleSearchQuery()\"\n" +
    "                     title=\"{{search.advanced ? 'search.basic-help' : 'search.advanced-help' | translate}}\" translate>\n" +
    "                    {{search.advanced ? 'search.basic' : 'search.advanced' | translate}}\n" +
    "                  </a>\n" +
    "                </small>\n" +
    "              </td>\n" +
    "            </tr>\n" +
    "            </tbody>\n" +
    "          </table>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "\n" +
    "      <!-- Search Results region -->\n" +
    "      <div id=\"search-result-region\" class=\"voffset3 can-full-screen\" ng-if=\"search.query\" fullscreen=\"isFullscreen\">\n" +
    "        <div ng-if=\"searchTabsOrder.length > 1\">\n" +
    "          <a href class=\"btn btn-sm btn-default pull-right\" ng-click=\"toggleFullscreen()\">\n" +
    "            <i class=\"glyphicon\" ng-class=\"{'glyphicon-resize-full': !isFullscreen, 'glyphicon-resize-small': isFullscreen}\"></i>\n" +
    "          </a>\n" +
    "          <ul class=\"nav nav-tabs voffset2\">\n" +
    "            <li role=\"presentation\" ng-repeat=\"tab in searchTabsOrder\" ng-class=\"{active: search.display === tab}\">\n" +
    "              <a href ng-click=\"selectDisplay(tab)\">{{ 'search.' + tab | translate}}</a>\n" +
    "            </li>\n" +
    "          </ul>\n" +
    "        </div>\n" +
    "\n" +
    "        <div translate>{{'search.' + search.display + '-help'}}</div>\n" +
    "        <result-panel display=\"search.display\"\n" +
    "                      type=\"search.type\"\n" +
    "                      bucket=\"search.bucket\"\n" +
    "                      query=\"search.executedQuery\"\n" +
    "                      result=\"search.result\"\n" +
    "                      loading=\"search.loading\"\n" +
    "                      on-update-criteria=\"onUpdateCriteria\"\n" +
    "                      on-remove-criteria=\"onRemoveCriteria\"\n" +
    "                      on-type-changed=\"onTypeChanged\"\n" +
    "                      on-bucket-changed=\"onBucketChanged\"\n" +
    "                      on-paginate=\"onPaginate\"\n" +
    "                      search-tabs-order=\"searchTabsOrder\"\n" +
    "                      result-tabs-order=\"resultTabsOrder\"\n" +
    "                      lang=\"lang\"></result-panel>\n" +
    "      </div>\n" +
    "\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "</div>");
}]);

angular.module("views/pagination-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("views/pagination-template.html",
    "<!--\n" +
    "  ~ Copyright (c) 2016 OBiBa. All rights reserved.\n" +
    "  ~\n" +
    "  ~ This program and the accompanying materials\n" +
    "  ~ are made available under the terms of the GNU Public License v3.0.\n" +
    "  ~\n" +
    "  ~ You should have received a copy of the GNU General Public License\n" +
    "  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.\n" +
    "  -->\n" +
    "\n" +
    "<ul class=\"pagination no-margin pagination-sm\" ng-if=\"1 < pages.length\">\n" +
    "  <li ng-if=\"boundaryLinks\" ng-class=\"{ disabled : pagination.current == 1 }\">\n" +
    "    <a href=\"\" ng-click=\"setCurrent(1)\">&laquo;</a>\n" +
    "  </li>\n" +
    "  <li ng-if=\"directionLinks\" ng-class=\"{ disabled : pagination.current == 1 }\">\n" +
    "    <a href=\"\" ng-click=\"setCurrent(pagination.current - 1)\">&lsaquo;</a>\n" +
    "  </li>\n" +
    "  <li ng-repeat=\"pageNumber in pages track by $index\" ng-class=\"{ active : pagination.current == pageNumber, disabled : pageNumber == '...' }\">\n" +
    "    <a href=\"\" ng-click=\"setCurrent(pageNumber)\">{{ pageNumber }}</a>\n" +
    "  </li>\n" +
    "  <li ng-if=\"directionLinks\" ng-class=\"{ disabled : pagination.current == pagination.last }\">\n" +
    "    <a href=\"\" ng-click=\"setCurrent(pagination.current + 1)\">&rsaquo;</a>\n" +
    "  </li>\n" +
    "  <li ng-if=\"boundaryLinks\" ng-class=\"{ disabled : pagination.current == pagination.last }\">\n" +
    "    <a ng-class=\"round-border\" href=\"\" ng-click=\"setCurrent(pagination.last)\">&raquo;</a>\n" +
    "  </li>\n" +
    "</ul>\n" +
    "\n" +
    "\n" +
    "<ul class=\"pagination no-margin pagination-sm\" ng-if=\"1 < pages.length\">\n" +
    "  <li>\n" +
    "    <a href=\"\" class=\"pagination-total\" ng-if=\"1 < pages.length\" class=\"pagination-total\">{{ range.lower }} - {{ range.upper }} of {{ range.total }}</a>\n" +
    "  </li>\n" +
    "</ul>");
}]);
