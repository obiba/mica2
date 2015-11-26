/*!
 * ng-obiba-mica - v1.0.0
 * https://github.com/obiba/ng-obiba-mica

 * License: GNU Public License version 3
 * Date: 2015-12-03
 */
'use strict';

function NgObibaMicaUrlProvider() {
  var registry = {
    'DataAccessFormConfigResource': 'ws/config/data-access-form',
    'DataAccessRequestsResource': 'ws/data-access-requests',
    'DataAccessRequestResource': 'ws/data-access-request/:id',
    'DataAccessRequestDownloadPdfResource': '/ws/data-access-request/:id/_pdf',
    'DataAccessRequestCommentsResource': 'ws/data-access-request/:id/comments',
    'DataAccessRequestCommentResource': 'ws/data-access-request/:id/comment/:commentId',
    'DataAccessRequestStatusResource': 'ws/data-access-request/:id/_status?to=:status',
    'TempFileUploadResource': 'ws/files/temp',
    'TempFileResource': 'ws/files/temp/:id'
  };

  function UrlProvider(registry) {
    var urlRegistry = registry;

    this.getUrl =function(resource) {
      if (resource in urlRegistry) {
        return urlRegistry[resource];
      }

      return null;
    };
  }

  this.setUrl = function(key, url) {
    if (key in registry) {
      registry[key] = url;
    }
  };

  this.$get = function() {
    return new UrlProvider(registry);
  };
}

/* exported NgObibaMicaTemplateUrlFactory */
function NgObibaMicaTemplateUrlFactory() {
  var factory = {registry: null};

  function TemplateUrlProvider(registry) {
    var urlRegistry = registry;

    this.getHeaderUrl =function(key) {
      if (key in urlRegistry) {
        return urlRegistry[key].header;
      }

      return null;
    };

    this.getFooterUrl =function(key) {
      if (key in urlRegistry) {
        return urlRegistry[key].footer;
      }

      return null;
    };
  }

  factory.setHeaderUrl = function(key, url) {
    if (key in this.registry) {
      this.registry[key].header = url;
    }
  };

  factory.setFooterUrl = function(key, url) {
    if (key in this.registry) {
      this.registry[key].footer = url;
    }
  };

  factory.$get = function() {
    return new TemplateUrlProvider(this.registry);
  };

  this.create = function(inputRegistry) {
    factory.registry = inputRegistry;
    return factory;
  };
}

angular.module('ngObibaMica', [
  'schemaForm',
  'obiba.mica.utils',
  'obiba.mica.file',
  'obiba.mica.attachment',
  'obiba.mica.access'
])
  .constant('USER_ROLES', {
    all: '*',
    admin: 'mica-administrator',
    reviewer: 'mica-reviewer',
    editor: 'mica-editor',
    user: 'mica-user',
    dao: 'mica-data-access-officer'
  })
  .config(['$provide', function($provide) {
    $provide.provider('ngObibaMicaUrl', NgObibaMicaUrlProvider);
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
    });

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
      $scope.onFileSelect = function (file) {
        $scope.uploadedFiles = file;
        $scope.uploadedFiles.forEach(function (f) {
          uploadFile(f);
        });
      };

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
  .config(['$provide', function($provide) {
    $provide.provider('ngObibaMicaAccessTemplateUrl', new NgObibaMicaTemplateUrlFactory().create(
      {
        list: { header: null, footer: null},
        view: { header: null, footer: null},
        form: { header: null, footer: null}
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
    'DataAccessRequestsResource',
    'DataAccessRequestResource',
    'DataAccessRequestService',
    'NOTIFICATION_EVENTS',
    'SessionProxy',
    'USER_ROLES',
    'ngObibaMicaAccessTemplateUrl',

    function ($rootScope,
              $scope,
              DataAccessRequestsResource,
              DataAccessRequestResource,
              DataAccessRequestService,
              NOTIFICATION_EVENTS,
              SessionProxy,
              USER_ROLES,
              ngObibaMicaAccessTemplateUrl) {

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

    function ($rootScope,
              $scope,
              $location,
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
              NOTIFICATION_EVENTS) {

      $scope.form = {
        schema: null,
        definition: null,
        model: {},
        comments: null
      };

      var onError = function (response) {
        AlertService.alert({
          id: 'DataAccessRequestViewController',
          type: 'danger',
          msg: ServerErrorUtils.buildMessage(response)
        });
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

      var retrieveComments = function() {
        $scope.form.comments = DataAccessRequestCommentsResource.query({id: $routeParams.id});
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

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
        if ($scope.commentToDelete === id) {
           DataAccessRequestCommentResource.delete({id: $routeParams.id, commentId: id}, {}, retrieveComments, onError);
        }
      });

      $scope.getDownloadHref = function(attachments, id) {
        return '/ws/data-access-request/' + $scope.dataAccessRequest.id + '/attachments/' + id + '/_download';
      };

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
          }, onUpdatStatusSuccess, onError);
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
    '$modal',
    'DataAccessRequestsResource',
    'DataAccessRequestResource',
    'DataAccessFormConfigResource',
    'JsonUtils',
    'AlertService',
    'ServerErrorUtils',
    'SessionProxy',
    'DataAccessRequestService',
    'ngObibaMicaAccessTemplateUrl',

    function ($log, $scope, $routeParams, $location, $modal,
              DataAccessRequestsResource,
              DataAccessRequestResource,
              DataAccessFormConfigResource,
              JsonUtils,
              AlertService,
              ServerErrorUtils,
              SessionProxy,
              DataAccessRequestService,
              ngObibaMicaAccessTemplateUrl) {

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

        $modal.open({
          scope: $scope,
          templateUrl: 'access/views/data-access-request-validation-modal.html',
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
          headers : {'Content-Type' : 'text/plain' },
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
          headers : {'Content-Type' : 'text/plain' },
          errorHandler: true
        }
      });
    }])

  .factory('DataAccessRequestStatusResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('DataAccessRequestStatusResource'), {}, {
        'update': {method: 'PUT', params: {id: '@id', status: '@status'}, errorHandler: true}
      });
    }])

  .service('DataAccessRequestService', ['$translate',
    function ($translate) {
      var statusList = {
        OPENED: 'OPENED',
        SUBMITTED: 'SUBMITTED',
        REVIEWED: 'REVIEWED',
        APPROVED: 'APPROVED',
        REJECTED: 'REJECTED'
      };

      this.status = statusList;

      this.getStatusFilterData = function(userCallback) {
        if (userCallback) {
          $translate(Object.keys(statusList)).then(function(translation) {
            userCallback(Object.keys(translation).map(function(key){
              return translation[key];
            }));
          });
        }
      };

      var canDoAction = function (request, action) {
        return request.actions ? request.actions.indexOf(action) !== -1 : null;
      };

      this.actions = {
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
        return request.nextStatus ? request.nextStatus.indexOf(to) !== -1 : null;
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

      this.getStatusHistoryInfo = function(userCallback) {
        if (!userCallback) {
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
            function(translation) {
              Object.keys(translation).forEach(
                function(key){
                  statusHistoryInfo[keyIdMap[key]].msg = translation[key];
                });

              userCallback(statusHistoryInfo);
            });
      };

      this.getStatusHistoryInfoId = function(status) {
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
    }]);
;angular.module('templates-ngObibaMica', ['access/views/data-access-request-form.html', 'access/views/data-access-request-histroy-view.html', 'access/views/data-access-request-list.html', 'access/views/data-access-request-validation-modal.html', 'access/views/data-access-request-view.html', 'attachment/attachment-input-template.html', 'attachment/attachment-list-template.html']);

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
    "      <h2 translate>data-access-request.documents</h2>\n" +
    "      <p translate>data-access-request.documents-help</p>\n" +
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
    "    <i class=\"fa fa-plus\"></i> <span translate>data-access-request.add</span>\n" +
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
    "            class=\"glyphicon glyphicon-search\"></i></span>\n" +
    "          <input ng-model=\"searchText\" type=\"text\" class=\"form-control\"\n" +
    "            aria-describedby=\"data-access-requests-search\">\n" +
    "        </span>\n" +
    "      </div>\n" +
    "      <div class=\"col-xs-2\">\n" +
    "        <div class=\"input-group\">\n" +
    "          <ui-select id=\"status-select\" theme=\"bootstrap\" ng-model=\"searchStatus.filter\" reset-search-input=\"true\">\n" +
    "            <ui-select-match allow-clear=\"true\" placeholder=\"{{'data-access-request.status-placeholder' | translate}}\">\n" +
    "              <span ng-bind-html=\"$select.selected\"></span>\n" +
    "            </ui-select-match>\n" +
    "            <ui-select-choices repeat=\"data in REQUEST_STATUS\">\n" +
    "              {{data}}\n" +
    "            </ui-select-choices>\n" +
    "          </ui-select>\n" +
    "        </div>\n" +
    "        </div>\n" +
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
    "          dir-paginate=\"request in requests | filter:{status: searchStatus.filter} | filter:searchText | itemsPerPage: 20\">\n" +
    "          <td>\n" +
    "            <a ng-href=\"#/data-access-request/{{request.id}}\" ng-if=\"actions.canView(request)\" translate>{{request.id}}</a>\n" +
    "            <span ng-if=\"!actions.canView(request)\">{{request.id}}</span>\n" +
    "          </td>\n" +
    "          <td ng-if=\"showApplicant\">\n" +
    "            {{userProfileService.getFullName(request.profile) || request.applicant}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{request.title}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{(request.timestamps.lastUpdate || request.timestamps.created) | fromNow}}\n" +
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
    "                <a ng-href=\"#/data-access-request/{{request.id}}/edit\"  title=\"{{'edit' | translate}}\"><i class=\"fa fa-pencil\"></i></a>\n" +
    "              </li>\n" +
    "              <li>\n" +
    "                <a ng-if=\"actions.canDelete(request)\" ng-click=\"deleteRequest(request)\" title=\"{{'delete' | translate}}\"><i\n" +
    "                  class=\"fa fa-trash-o\"></i></a>\n" +
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
    "    <p class=\"help-block pull-left\"><span translate>created-by</span> {{userProfileService.getFullName(dataAccessRequest.profile) || dataAccessRequest.applicant}},\n" +
    "      <span>{{dataAccessRequest.timestamps.created | amCalendar}}</span>\n" +
    "      <span class=\"label label-success\">{{dataAccessRequest.status}}</span></p>\n" +
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
    "        <i class=\"glyphicon glyphicon-download-alt\"></i> <span translate>download</span>\n" +
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
    "    <tabset class=\"voffset5\">\n" +
    "      <tab ng-click=\"selectTab('form')\" heading=\"{{'data-access-request.form' | translate}}\">\n" +
    "        <form id=\"request-form\" name=\"forms.requestForm\">\n" +
    "          <div sf-model=\"form.model\" sf-form=\"form.definition\" sf-schema=\"form.schema\"></div>\n" +
    "        </form>\n" +
    "        <h2 translate>data-access-request.documents</h2>\n" +
    "\n" +
    "        <p ng-if=\"dataAccessRequest.attachments.length == 0\" translate>\n" +
    "          data-access-request.no-documents\n" +
    "        </p>\n" +
    "\n" +
    "        <attachment-list files=\"dataAccessRequest.attachments\"\n" +
    "            href-builder=\"getDownloadHref(attachments, id)\"></attachment-list>\n" +
    "      </tab>\n" +
    "      <tab ng-click=\"selectTab('comments')\" heading=\"{{'data-access-request.comments' | translate}}\">\n" +
    "        <obiba-comments comments=\"form.comments\" on-update=\"updateComment\" on-delete=\"deleteComment\" name-resolver=\"userProfileService.getFullName\" edit-action=\"EDIT\" delete-action=\"DELETE\"></obiba-comments>\n" +
    "        <obiba-comment-editor on-submit=\"submitComment\"></obiba-comment-editor>\n" +
    "      </tab>\n" +
    "      <tab ng-click=\"selectTab('history')\" heading=\"{{'data-access-request.history' | translate}}\">\n" +
    "        <div ng-include=\"'access/views/data-access-request-histroy-view.html'\"></div>\n" +
    "      </tab>\n" +
    "    </tabset>\n" +
    "  </div>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("attachment/attachment-input-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("attachment/attachment-input-template.html",
    "<button type=\"button\" class=\"btn btn-primary btn-xs\" aria-hidden=\"true\" ngf-select ngf-change=\"onFileSelect($files)\" translate>file.upload.button</button>\n" +
    "\n" +
    "<table ng-show=\"files.length\" class=\"table table-striped\">\n" +
    "  <tbody>\n" +
    "  <tr ng-repeat=\"file in files\">\n" +
    "    <td>\n" +
    "      {{file.fileName}}\n" +
    "      <progressbar ng-show=\"file.showProgressBar\" class=\"progress-striped\" value=\"file.progress\">\n" +
    "        {{file.progress}}%\n" +
    "      </progressbar>\n" +
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
