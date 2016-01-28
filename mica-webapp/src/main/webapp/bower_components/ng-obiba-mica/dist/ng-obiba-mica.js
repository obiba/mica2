/*!
 * ng-obiba-mica - v1.0.0
 * https://github.com/obiba/ng-obiba-mica

 * License: GNU Public License version 3
 * Date: 2016-01-28
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
    'PublishedStudiesSearchResource': 'ws/:type/_search',
    'TaxonomiesResource': 'ws/taxonomies/_filter',
    'TaxonomyResource': 'ws/taxonomy/:taxonomy/_filter',
    'VocabularyResource': 'ws/taxonomy/:taxonomy/vocabulary/:vocabulary/_filter',
    'JoinQuerySearchResource': 'ws/:type/_rql?:query'
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
    'obiba.mica.utils',
    'obiba.mica.file',
    'obiba.mica.attachment',
    'obiba.mica.access',
    'obiba.mica.search',
    'obiba.mica.graphics'
  ])
  .constant('USER_ROLES', {
    all: '*',
    admin: 'mica-administrator',
    reviewer: 'mica-reviewer',
    editor: 'mica-editor',
    user: 'mica-user',
    dao: 'mica-data-access-officer'
  })
  .config(['$provide', function ($provide) {
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

  .service('DataAccessRequestService', ['$translate', 'SessionProxy',
    function ($translate, SessionProxy) {
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
            if (value === role) {
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

'use strict';

/*global NgObibaMicaTemplateUrlFactory */
angular.module('obiba.mica.search', [
    'ui.bootstrap',
    'pascalprecht.translate',
    'templates-ngObibaMica'
  ])
  .config(['$provide', function ($provide) {
    $provide.provider('ngObibaMicaSearchTemplateUrl', new NgObibaMicaTemplateUrlFactory().create(
      {
        list: {header: null, footer: null},
        view: {header: null, footer: null},
        form: {header: null, footer: null}
      }
    ));
  }]);;/*
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

  .constant('QUERY_TYPES', {
    NETWORKS: 'networks',
    STUDIES: 'studies',
    DATASETS: 'datasets',
    VARIABLES: 'variables'
  })

  .controller('SearchController', [
    '$scope',
    '$timeout',
    '$routeParams',
    '$location',
    'TaxonomiesResource',
    'TaxonomyResource',
    'VocabularyResource',
    'ngObibaMicaSearchTemplateUrl',
    'JoinQuerySearchResource',
    'QUERY_TYPES',
    function ($scope,
              $timeout,
              $routeParams,
              $location,
              TaxonomiesResource,
              TaxonomyResource,
              VocabularyResource,
              ngObibaMicaSearchTemplateUrl,
              JoinQuerySearchResource,
              QUERY_TYPES) {


console.log('THIS IS SEARCH CONTROLLER');


      var closeTaxonomies = function () {
        $('#taxonomies').collapse('hide');
      };

      var filterTaxonomies = function (query) {
        $scope.taxonomies.search.active = true;
        if (query && query.length === 1) {
          $scope.taxonomies.search.active = false;
          return;
        }
        // taxonomy filter
        if ($scope.taxonomies.taxonomy) {
          if ($scope.taxonomies.vocabulary) {
            VocabularyResource.get({
              target: $scope.taxonomies.target,
              taxonomy: $scope.taxonomies.taxonomy.name,
              vocabulary: $scope.taxonomies.vocabulary.name,
              query: query
            }, function onSuccess(response) {
              $scope.taxonomies.vocabulary.terms = response.terms;
              $scope.taxonomies.search.active = false;
            });
          } else {
            TaxonomyResource.get({
              target: $scope.taxonomies.target,
              taxonomy: $scope.taxonomies.taxonomy.name,
              query: query
            }, function onSuccess(response) {
              $scope.taxonomies.taxonomy.vocabularies = response.vocabularies;
              $scope.taxonomies.search.active = false;
            });
          }
        } else {
          TaxonomiesResource.get({
            target: $scope.taxonomies.target,
            query: query
          }, function onSuccess(response) {
            $scope.taxonomies.all = response;
            $scope.taxonomies.search.active = false;
          });
        }
      };

      var selectTaxonomyTarget = function (target) {
        if (!$scope.taxonomiesShown) {
          $('#taxonomies').collapse('show');
        }
        if ($scope.taxonomies.target !== target) {
          $scope.taxonomies.target = target;
          $scope.taxonomies.taxonomy = null;
          $scope.taxonomies.vocabulary = null;
          filterTaxonomies($scope.taxonomies.search.text);
        }
      };

      var clearFilterTaxonomies = function () {
        $scope.taxonomies.search.text = null;
        $scope.taxonomies.search.active = false;
        filterTaxonomies(null);
      };

      var filterTaxonomiesKeyUp = function (event) {
        switch (event.keyCode) {
          case 27: // ESC
            if (!$scope.taxonomies.search.active) {
              clearFilterTaxonomies();
            }
            break;

          case 13: // Enter
            filterTaxonomies($scope.taxonomies.search.text);
            break;
        }
      };

      var clearSearch = function () {
        $scope.documents.search.text = null;
        $scope.documents.search.active = false;
      };

      var searchDocuments = function (/*query*/) {
        $scope.documents.search.active = true;
        // search for taxonomy terms
        // search for matching variables/studies/... count
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
              searchDocuments($scope.documents.search.text);
            }
            break;
        }
      };

      var navigateTaxonomy = function (taxonomy, vocabulary) {
        var toFilter = ($scope.taxonomies.taxonomy && !taxonomy) || ($scope.taxonomies.vocabulary && !vocabulary);
        $scope.taxonomies.taxonomy = taxonomy;
        $scope.taxonomies.vocabulary = vocabulary;
        if (toFilter) {
          filterTaxonomies($scope.taxonomies.search.text);
        }
      };

      var selectTerm = function (/*taxonomy, vocabulary, term*/) {

      };

      function executeQuery() {
        JoinQuerySearchResource[$scope.search.type]({query: $scope.search.query}, function onSuccess(response) {
          $scope.search.result = response;
          console.log('>>> Response', $scope.search.result);
        });
      }

      function updateSearchData() {
        var search = $location.search();
        $scope.search.type = ensureValidType(search.type);
        $scope.search.query = ensureValidQuery(search.query, $scope.search.type);
      }

      function ensureValidType(type) {
        var validType = null;

        if (type) {
          validType = QUERY_TYPES[type.toUpperCase()];
        }

        return validType || $scope.search.type || QUERY_TYPES.VARIABLES;
      }

      function getDefaultQuery(type) {
        var query = ':q(match())';

        switch (type) {
          case QUERY_TYPES.NETWORKS:
            return query.replace(/:q/, 'network');
          case QUERY_TYPES.STUDIES:
            return query.replace(/:q/, 'study');
          case QUERY_TYPES.DATASETS:
            return query.replace(/:q/, 'dataset');
          case QUERY_TYPES.VARIABLES:
            return query.replace(/:q/, 'variable');
        }

        throw new Error('Invalid query type: ' + type);
      }

      function ensureValidQuery(query, type) {
        // TODO validate query with RQL parser
        if (!query) {
          return getDefaultQuery(type);
        }

        return query;
      }

      var onTypeChanged = function(type) {
        if (type) {
          var search = $location.search();
          search.type = ensureValidType(type);
          $location.search(search).replace();
        }
      };

      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.lang = 'en';
      var type = ensureValidType($routeParams.type);

      $scope.search = {
        query: ensureValidQuery($routeParams.query, type),
        type: type,
        result: null
      };

      $scope.documents = {
        search: {
          text: null,
          active: false
        }
      };

      $scope.taxonomies = {
        all: TaxonomiesResource.get({target: 'variable'}),
        search: {
          text: null,
          active: false
        },
        target: 'variable',
        taxonomy: null,
        vocabulary: null
      };

      $scope.headerTemplateUrl = ngObibaMicaSearchTemplateUrl.getHeaderUrl('view');
      $scope.clearFilterTaxonomies = clearFilterTaxonomies;
      $scope.searchKeyUp = searchKeyUp;
      $scope.filterTaxonomiesKeyUp = filterTaxonomiesKeyUp;
      $scope.navigateTaxonomy = navigateTaxonomy;
      $scope.selectTaxonomyTarget = selectTaxonomyTarget;
      $scope.selectTerm = selectTerm;
      $scope.closeTaxonomies = closeTaxonomies;
      $scope.onTypeChanged = onTypeChanged;
      $scope.taxonomiesShown = false;

      //// TODO replace with angular code
      $('#taxonomies').on('show.bs.collapse', function () {
        $scope.taxonomiesShown = true;
      });
      $('#taxonomies').on('hide.bs.collapse', function () {
        $scope.taxonomiesShown = false;
      });

      $scope.$watch('search', function () {
        //if ($scope.search.query) {
          executeQuery();
        //}
      });


      $scope.$on('$locationChangeSuccess', function(newLocation, oldLocation) {
        if (newLocation !== oldLocation) {
          updateSearchData(newLocation);
          executeQuery();
        }
      });

    }])

  .controller('SearchResultController', [
    '$scope',
    'QUERY_TYPES',
    function ($scope, QUERY_TYPES) {
      var selectTab = function(type) {
        console.log('Type', type);
        $scope.type = type;
        $scope.$parent.onTypeChanged(type);
      };

      $scope.activeTab = {
        networks: $scope.type === QUERY_TYPES.NETWORKS || false,
        studies: $scope.type === QUERY_TYPES.STUDIES || false,
        datasets: $scope.type === QUERY_TYPES.DATASETS || false,
        variables: $scope.type === QUERY_TYPES.VARIABLES || false
      };

      $scope.selectTab = selectTab;
      $scope.QUERY_TYPES = QUERY_TYPES;

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
  .directive('taxonomyPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        taxonomy: '=',
        lang: '=',
        onNavigate: '='
      },
      templateUrl: 'search/views/taxonomy-panel-template.html'
    };
  }])

  .directive('vocabularyPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        taxonomy: '=',
        vocabulary: '=',
        lang: '=',
        onNavigate: '='
      },
      templateUrl: 'search/views/vocabulary-panel-template.html'
    };
  }])

  .directive('termPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        taxonomy: '=',
        vocabulary: '=',
        term: '=',
        lang: '=',
        onSelect: '='
      },
      templateUrl: 'search/views/term-panel-template.html'
    };
  }])

  .directive('networksResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '='
      },
      templateUrl: 'search/views/networks-search-result-table-template.html'
    };
  }])

  .directive('datasetsResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '='
      },
      templateUrl: 'search/views/datasets-search-result-table-template.html'
    };
  }])

  .directive('studiesResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '='
      },
      templateUrl: 'search/views/studies-search-result-table-template.html'
    };
  }])

  .directive('variablesResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '='
      },
      templateUrl: 'search/views/variables-search-result-table-template.html'
    };
  }])

  .directive('resultPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        type: '=',
        dto: '=',
        lang: '=',
        onTypeChanged: '='
      },
      controller: 'SearchResultController',
      templateUrl: 'search/views/search-result-panel-template.html'
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

angular.module('obiba.mica.search')
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

  .factory('TaxonomyResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('TaxonomyResource'), {}, {
        'get': {
          method: 'GET',
          errorHandler: true
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

  .factory('VocabularyResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('VocabularyResource'), {}, {
        'get': {
          method: 'GET',
          errorHandler: true
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

function GraphicChartsDataProvider() {

  function DataProvider(dataResponse) {
    var data = dataResponse;
    this.getData = function (callback) {
      if (callback) {
        data.$promise.then(callback);
      }
    };
  }

  this.$get = function (GraphicChartsDataResource, GraphicChartsConfig, GraphicChartsQuery) {
    var queryDto = GraphicChartsQuery.queryDtoBuilder(GraphicChartsConfig.getOptions().entityIds);
    return new DataProvider(GraphicChartsDataResource.get({
      type: GraphicChartsConfig.getOptions().entityType,
      id: GraphicChartsConfig.getOptions().entityIds
    }, queryDto));
  };
}

angular.module('obiba.mica.graphics', [
    'googlechart',
    'obiba.utils',
    'templates-ngObibaMica'
  ])
  .config(['$provide', function ($provide) {
    $provide.provider('GraphicChartsData', GraphicChartsDataProvider);
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
        fieldTransformer: '@',
        chartType: '@',
        chartAggregationName: '@',
        chartEntityDto: '@',
        chartOptionsName: '@',
        chartOptions: '=',
        chartHeader: '='
      },
      templateUrl: 'graphics/views/charts-directive.html',
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
    'GraphicChartsConfig',
    'GraphicChartsUtils',
    'GraphicChartsData',
    function ($rootScope,
              $scope,
              $filter,
              GraphicChartsConfig,
              GraphicChartsUtils,
              GraphicChartsData) {

      GraphicChartsData.getData(function (StudiesData) {
        if (StudiesData) {
          $scope.ItemDataJSon = GraphicChartsUtils.getArrayByAggregation($scope.chartAggregationName, StudiesData[$scope.chartEntityDto], $scope.fieldTransformer);
          $scope.ItemDataJSon.unshift($scope.chartHeader);
          if ($scope.ItemDataJSon) {
            $scope.chartObject = {};
            $scope.chartObject.type = $scope.chartType;
            $scope.chartObject.data = $scope.ItemDataJSon;
            $scope.chartObject.options = {backgroundColor: {fill: 'transparent'}};
            angular.extend($scope.chartObject.options, $scope.chartOptions);
            $scope.chartObject.options.title = $scope.chartOptions.title + ' (N=' + StudiesData.studyResultDto.totalHits + ')';
          }
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
      return $resource(ngObibaMicaUrl.getUrl('PublishedStudiesSearchResource'), {}, {
        'get': {method: 'POST', errorHandler: true}
      });
    }])
  .service('GraphicChartsConfig', function () {
    var factory = {
      options: {
        entityIds: 'NaN',
        entityType: null
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
  .service('GraphicChartsUtils', [
    'CountriesIsoUtils',
    'LocalizedStringService',
    function (CountriesIsoUtils,
              LocalizedStringService) {

      this.getArrayByAggregation = function (AggregationName, EntityDto, fieldTransformer) {
        var ArrayData = [];
        angular.forEach(EntityDto.aggs, function (aggragation) {
          var itemName = [];
          if (aggragation.aggregation === AggregationName) {
            var i = 0;
            angular.forEach(aggragation['obiba.mica.TermsAggregationResultDto.terms'], function (term) {
              switch (fieldTransformer) {
                case 'country' :
                  itemName.name = CountriesIsoUtils.findByCode(term.title.toUpperCase(), LocalizedStringService.getLocal());
                  break;
                default :
                  itemName.name = term.title;
                  break;
              }
              if (term.count) {
                ArrayData[i] = [itemName.name, term.count];
                i ++;
              }
            });
          }
        });
        return ArrayData;
      };
    }])
  .service('GraphicChartsQuery', [function () {
    this.queryDtoBuilder = function (entityIds) {
      if (!(entityIds) || entityIds ==='NaN') {
        return '{"studyQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0}},"locale":"en","withFacets":true}';
      }
      else{
        return '{"studyQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0}},"networkQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0},"filteredQuery":{"obiba.mica.LogicalFilterQueryDto.filter":{"fields":[{"field":{"field":"id","obiba.mica.TermsFilterQueryDto.terms":{"values":["'+entityIds+'"]}},"op":1}]}}},"locale":"en","withFacets":true}';
      }
    };
  }]);
;angular.module('templates-ngObibaMica', ['access/views/data-access-request-form.html', 'access/views/data-access-request-histroy-view.html', 'access/views/data-access-request-list.html', 'access/views/data-access-request-profile-user-modal.html', 'access/views/data-access-request-submitted-modal.html', 'access/views/data-access-request-validation-modal.html', 'access/views/data-access-request-view.html', 'attachment/attachment-input-template.html', 'attachment/attachment-list-template.html', 'graphics/views/charts-directive.html', 'search/views/datasets-search-result-table-template.html', 'search/views/networks-search-result-table-template.html', 'search/views/search-result-panel-template.html', 'search/views/search.html', 'search/views/studies-search-result-table-template.html', 'search/views/taxonomies-view.html', 'search/views/taxonomy-panel-template.html', 'search/views/taxonomy-template.html', 'search/views/term-panel-template.html', 'search/views/variables-search-result-table-template.html', 'search/views/vocabulary-panel-template.html']);

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
    "            <span ng-if=\"actions.canViewProfile('mica-user')\">\n" +
    "         {{getFullName(request.profile) || request.applicant}}\n" +
    "            </span>\n" +
    "            <a href ng-click=\"userProfile(request.profile)\"\n" +
    "                ng-if=\"actions.canViewProfile('mica-data-access-officer')\">\n" +
    "              {{getFullName(request.profile) ||\n" +
    "              request.applicant}}\n" +
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
    "    <div>\n" +
    "      <label class=\"control-label\">\n" +
    "        {{'data-access-request.profile.name' | translate}}\n" +
    "      </label> :\n" +
    "      <span>\n" +
    "        {{getFullName(applicant)}}\n" +
    "      </span>\n" +
    "    </div>\n" +
    "\n" +
    "    <div>\n" +
    "      <label class=\"control-label\">\n" +
    "        {{'data-access-request.profile.email' | translate}}\n" +
    "      </label> :\n" +
    "      <span>\n" +
    "        {{getProfileEmail(applicant)}}\n" +
    "      </span>\n" +
    "    </div>\n" +
    "\n" +
    "    <div\n" +
    "      ng-repeat=\"attribute in applicant.attributes | filterProfileAttributes\">\n" +
    "      <label  class=\"control-label\">\n" +
    "        {{attribute.key}}\n" +
    "      </label> :\n" +
    "      <span >{{attribute.value}}</span>\n" +
    "    </div>\n" +
    "    <a  class=\"btn btn-default\" href=\"mailto:{{getProfileEmail(applicant)}}\" target=\"_blank\">\n" +
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
    "<button type=\"button\" class=\"btn btn-primary btn-xs\" aria-hidden=\"true\" ngf-select ngf-change=\"onFileSelect($files)\" translate>file.upload.button</button>\n" +
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

angular.module("graphics/views/charts-directive.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("graphics/views/charts-directive.html",
    "<div>\n" +
    "  <div google-chart chart=\"chartObject\">\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/datasets-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/datasets-search-result-table-template.html",
    "<div ng-show=\"summaries.length > 0\">\n" +
    "  <div class=\"row voffset2\">\n" +
    "    <div class=\"col-xs-4\">\n" +
    "    </div>\n" +
    "    <div class=\"col-xs-8\">\n" +
    "      <dir-pagination-controls class=\"pull-right\"></dir-pagination-controls>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"table-responsive\">\n" +
    "    <table class=\"table table-bordered table-striped\">\n" +
    "      <thead>\n" +
    "      <tr>\n" +
    "        <th translate>acronym</th>\n" +
    "        <th translate>name</th>\n" +
    "        <th translate>type</th>\n" +
    "        <th translate>networks</th>\n" +
    "        <th translate>studies</th>\n" +
    "        <th translate>variables</th>\n" +
    "      </tr>\n" +
    "      </thead>\n" +
    "      <tbody>\n" +
    "\n" +
    "      <tr ng-repeat=\"summary in summaries\">\n" +
    "        <td>\n" +
    "          <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          <localized value=\"summary.type\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.datasetCountStats'].networks}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.datasetCountStats'].studies}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.datasetCountStats'].variables}}\n" +
    "        </td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/networks-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/networks-search-result-table-template.html",
    "<div ng-show=\"summaries.length > 0\">\n" +
    "\n" +
    "  <div class=\"row voffset2\">\n" +
    "    <div class=\"col-xs-4\">\n" +
    "    </div>\n" +
    "    <div class=\"col-xs-8\">\n" +
    "      <dir-pagination-controls class=\"pull-right\"></dir-pagination-controls>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"table-responsive\">\n" +
    "    <table class=\"table table-bordered table-striped\">\n" +
    "      <thead>\n" +
    "      <tr>\n" +
    "        <th translate>acronym</th>\n" +
    "        <th translate>name</th>\n" +
    "        <th translate>studies</th>\n" +
    "        <th translate colspan=\"2\">datasets</th>\n" +
    "        <th translate>variables</th>\n" +
    "      </tr>\n" +
    "      <tr>\n" +
    "        <th></th>\n" +
    "        <th></th>\n" +
    "        <th></th>\n" +
    "        <th translate>study</th>\n" +
    "        <th translate>harmonization</th>\n" +
    "        <th></th>\n" +
    "      </tr>\n" +
    "      </thead>\n" +
    "      <tbody>\n" +
    "\n" +
    "      <tr ng-repeat=\"summary in summaries\">\n" +
    "        <td>\n" +
    "          <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.networkCountStats'].studies}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.networkCountStats'].studyDatasets}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.networkCountStats'].harmonizationDatasets}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.networkCountStats'].variables}}\n" +
    "        </td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/search-result-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-panel-template.html",
    "<div ng-show=\"dto\">\n" +
    "  <uib-tabset class=\"voffset5\">\n" +
    "    <!-- Networks -->\n" +
    "    <uib-tab active=\"activeTab.networks\" ng-click=\"selectTab(QUERY_TYPES.NETWORKS)\"\n" +
    "             heading=\"{{'networks' | translate}} ({{dto.networkResultDto.totalHits}})\">\n" +
    "      <networks-result-table\n" +
    "        summaries=\"dto.networkResultDto['obiba.mica.NetworkResultDto.result'].networks\"></networks-result-table>\n" +
    "    </uib-tab>\n" +
    "\n" +
    "    <!-- Studies -->\n" +
    "    <uib-tab active=\"activeTab.studies\" ng-click=\"selectTab(QUERY_TYPES.STUDIES)\"\n" +
    "             heading=\"{{'studies' | translate}} ({{dto.studyResultDto.totalHits}})\">\n" +
    "      <studies-result-table\n" +
    "        summaries=\"dto.studyResultDto['obiba.mica.StudyResultDto.result'].summaries\"></studies-result-table>\n" +
    "    </uib-tab>\n" +
    "\n" +
    "    <!-- Datasets -->\n" +
    "    <uib-tab active=\"activeTab.datasets\" ng-click=\"selectTab(QUERY_TYPES.DATASETS)\"\n" +
    "             heading=\"{{'datasets' | translate}} ({{dto.datasetResultDto.totalHits}})\">\n" +
    "      <datasets-result-table\n" +
    "        summaries=\"dto.datasetResultDto['obiba.mica.DatasetResultDto.result'].datasets\"></datasets-result-table>\n" +
    "\n" +
    "    </uib-tab>\n" +
    "\n" +
    "    <!-- Variables -->\n" +
    "    <uib-tab active=\"activeTab.variables\" ng-click=\"selectTab(QUERY_TYPES.VARIABLES)\"\n" +
    "             heading=\"{{'variables' | translate}} ({{dto.variableResultDto.totalHits}})\">\n" +
    "      <variables-result-table\n" +
    "        summaries=\"dto.variableResultDto['obiba.mica.DatasetVariableResultDto.result'].summaries\"></variables-result-table>\n" +
    "    </uib-tab>\n" +
    "  </uib-tabset>\n" +
    "\n" +
    "</div>");
}]);

angular.module("search/views/search.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search.html",
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
    "<div>\n" +
    "  <!--<h2 translate>search</h2>-->\n" +
    "  <div ng-if=\"headerTemplateUrl\" ng-include=\"headerTemplateUrl\"></div>\n" +
    "\n" +
    "  <!-- Classifications region -->\n" +
    "  <div ng-mouseleave=\"closeTaxonomies()\">\n" +
    "  <!--<div>-->\n" +
    "    <div class=\"row\">\n" +
    "      <div class=\"col-xs-3\"></div>\n" +
    "      <div class=\"col-xs-6\">\n" +
    "        <a href>\n" +
    "        <span class=\"input-group input-group-sm no-padding-top\">\n" +
    "          <input ng-keyup=\"searchKeyUp($event)\" ng-model=\"documents.search.text\" type=\"text\" class=\"form-control ng-pristine ng-untouched ng-valid\" aria-describedby=\"study-search\">\n" +
    "          <span class=\"input-group-addon\"><i class=\"glyphicon glyphicon-search\"></i></span>\n" +
    "        </span>\n" +
    "        </a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div class=\"row\">\n" +
    "      <div class=\"col-xs-3\"></div>\n" +
    "      <div class=\"col-xs-6\">\n" +
    "        <ul class=\"nav nav-pills\">\n" +
    "          <li ng-class=\"{'active': taxonomies.target === 'variable' && taxonomiesShown}\" title=\"{{'variable-classifications' | translate}}\">\n" +
    "            <a ng-mouseover=\"selectTaxonomyTarget('variable')\" translate>variables</a>\n" +
    "          </li>\n" +
    "          <li ng-class=\"{'active': taxonomies.target === 'study' && taxonomiesShown}\" title=\"{{'study-classifications' | translate}}\">\n" +
    "            <a ng-mouseover=\"selectTaxonomyTarget('study')\" translate>studies</a>\n" +
    "          </li>\n" +
    "          <li ng-if=\"taxonomiesShown\">\n" +
    "            <a href ng-click=\"closeTaxonomies()\" title=\"{{'close' | translate}}\"><i class=\"fa fa-close\"></i> </a>\n" +
    "          </li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div id=\"taxonomies\" class=\"collapse card card-static container\">\n" +
    "      <div ng-include=\"'search/views/taxonomies-view.html'\"></div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <!-- Search criteria region -->\n" +
    "  <div class=\"voffset4\">\n" +
    "    <div class=\"row voffset4\">\n" +
    "      <div class=\"col-xs-12\">\n" +
    "        <div class=\"btn-group dropdown\" is-open=\"status.isopen\">\n" +
    "          <button type=\"button\" class=\"btn btn-info btn-sm dropdown-toggle\" ng-disabled=\"disabled\" data-toggle=\"dropdown\">\n" +
    "            Study: All <span class=\"caret\"></span>\n" +
    "          </button>\n" +
    "          <ul class=\"dropdown-menu\" role=\"menu\">\n" +
    "            <li><a role=\"menuitem\" href ng-click=\"selectCriteria()\">Atlantic Path</a></li>\n" +
    "            <li><a role=\"menuitem\" href ng-click=\"selectCriteria()\">CartaGene</a></li>\n" +
    "          </ul>\n" +
    "        </div>\n" +
    "\n" +
    "        <div class=\"btn-group dropdown\" is-open=\"status.isopen\">\n" +
    "          <button type=\"button\" class=\"btn btn-default btn-sm dropdown-toggle\" ng-disabled=\"disabled\" data-toggle=\"dropdown\">\n" +
    "            OR <span class=\"caret\"></span>\n" +
    "          </button>\n" +
    "          <ul class=\"dropdown-menu\" role=\"menu\">\n" +
    "            <li><a role=\"menuitem\" href ng-click=\"selectCriteria()\">AND</a></li>\n" +
    "          </ul>\n" +
    "        </div>\n" +
    "\n" +
    "        <div class=\"btn-group dropdown\" is-open=\"status.isopen\">\n" +
    "          <button type=\"button\" class=\"btn btn-info btn-sm dropdown-toggle\" ng-disabled=\"disabled\" data-toggle=\"dropdown\">\n" +
    "            Study designs: All <span class=\"caret\"></span>\n" +
    "          </button>\n" +
    "          <ul class=\"dropdown-menu\" role=\"menu\">\n" +
    "            <li><a role=\"menuitem\" href ng-click=\"selectCriteria()\">Cohort study (117)</a></li>\n" +
    "            <li><a role=\"menuitem\" href ng-click=\"selectCriteria()\">Cross sectional (8)</a></li>\n" +
    "            <li><a role=\"menuitem\" href ng-click=\"selectCriteria()\">Clinical trial (5)</a></li>\n" +
    "            <li><a role=\"menuitem\" href ng-click=\"selectCriteria()\">Case control (2)</a></li>\n" +
    "            <li><a role=\"menuitem\" href ng-click=\"selectCriteria()\">Other (2)</a></li>\n" +
    "          </ul>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "\n" +
    "  <!-- Results region -->\n" +
    "  <div>\n" +
    "    <result-panel type=\"search.type\" dto=\"search.result\" on-type-changed=\"onTypeChanged\"></result-panel>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/studies-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/studies-search-result-table-template.html",
    "<div ng-show=\"summaries.length > 0\">\n" +
    "\n" +
    "  <div class=\"row voffset2\">\n" +
    "    <div class=\"col-xs-4\">\n" +
    "    </div>\n" +
    "    <div class=\"col-xs-8\">\n" +
    "      <dir-pagination-controls class=\"pull-right\"></dir-pagination-controls>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"table-responsive\">\n" +
    "    <table class=\"table table-bordered table-striped\">\n" +
    "      <thead>\n" +
    "      <tr>\n" +
    "        <th translate>acronym</th>\n" +
    "        <th translate>name</th>\n" +
    "        <th translate>design</th>\n" +
    "        <th translate>participants</th>\n" +
    "        <th translate>networks</th>\n" +
    "        <th translate colspan=\"2\">datasets</th>\n" +
    "        <th translate>variables</th>\n" +
    "      </tr>\n" +
    "      <tr>\n" +
    "        <th></th>\n" +
    "        <th></th>\n" +
    "        <th></th>\n" +
    "        <th></th>\n" +
    "        <th></th>\n" +
    "        <th translate>study</th>\n" +
    "        <th translate>harmonization</th>\n" +
    "        <th></th>\n" +
    "      </tr>\n" +
    "      </thead>\n" +
    "      <tbody>\n" +
    "\n" +
    "      <tr ng-repeat=\"summary in summaries\">\n" +
    "        <td>\n" +
    "          <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary.designs.join(', ')}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary.targetNumber.number}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.studyCountStats'].networks}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.studyCountStats'].studyDatasets}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.studyCountStats'].harmonizationDatasets}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          {{summary['obiba.mica.CountStatsDto.studyCountStats'].variables}}\n" +
    "        </td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/taxonomies-view.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/taxonomies-view.html",
    "<div class=\"panel panel-default\">\n" +
    "  <div class=\"panel-heading\">\n" +
    "    <div class=\"row\">\n" +
    "      <div class=\"col-xs-8 voffset1\">\n" +
    "        <ol class=\"mica-breadcrumb no-margin no-padding\">\n" +
    "          <li ng-if=\"!taxonomies.taxonomy\">\n" +
    "            {{'all-' + taxonomies.target + '-classifications' | translate}}\n" +
    "          </li>\n" +
    "          <li ng-if=\"taxonomies.taxonomy\">\n" +
    "            <a href ng-click=\"navigateTaxonomy()\">{{'all-' + taxonomies.target + '-classifications' | translate}}</a>\n" +
    "          </li>\n" +
    "          <li ng-if=\"taxonomies.taxonomy && !taxonomies.vocabulary\">\n" +
    "            <span ng-repeat=\"label in taxonomies.taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "              {{label.text}}\n" +
    "            </span>\n" +
    "          </li>\n" +
    "          <li ng-if=\"taxonomies.taxonomy && taxonomies.vocabulary\">\n" +
    "            <a href ng-click=\"navigateTaxonomy(taxonomies.taxonomy)\">\n" +
    "              <span ng-repeat=\"label in taxonomies.taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "              {{label.text}}\n" +
    "              </span>\n" +
    "            </a>\n" +
    "          </li>\n" +
    "          <li ng-if=\"taxonomies.vocabulary\">\n" +
    "            <span ng-repeat=\"label in taxonomies.vocabulary.title\" ng-if=\"label.locale === lang\">\n" +
    "              {{label.text}}\n" +
    "            </span>\n" +
    "          </li>\n" +
    "        </ol>\n" +
    "      </div>\n" +
    "      <div class=\"col-xs-4\">\n" +
    "        <div class=\"form-inline pull-right\">\n" +
    "          <div class=\"form-group\">\n" +
    "            <span class=\"input-group input-group-sm no-padding-top\">\n" +
    "              <span class=\"input-group-addon\"><i class=\"glyphicon glyphicon-filter\"></i></span>\n" +
    "              <input ng-keyup=\"filterTaxonomiesKeyUp($event)\" ng-model=\"taxonomies.search.text\" type=\"text\" class=\"form-control ng-pristine ng-untouched ng-valid\" aria-describedby=\"study-search\">\n" +
    "              <span class=\"input-group-addon\"><i class=\"glyphicon glyphicon-remove\" ng-click=\"clearFilterTaxonomies()\"></i></span>\n" +
    "            </span>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "  <div class=\"panel-body\">\n" +
    "    <div ng-if=\"!taxonomies.search.active && taxonomies.all.length === 0\" translate>\n" +
    "      no-classifications\n" +
    "    </div>\n" +
    "    <div ng-if=\"taxonomies.search.active\" class=\"loading\"></div>\n" +
    "\n" +
    "    <div ng-if=\"!taxonomies.taxonomy\">\n" +
    "      <div ng-repeat=\"taxonomy in taxonomies.all\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "        <div class=\"col-xs-4\">\n" +
    "          <div taxonomy-panel taxonomy=\"taxonomies.all[$index]\" lang=\"lang\" on-navigate=\"navigateTaxonomy\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-4\">\n" +
    "          <div taxonomy-panel taxonomy=\"taxonomies.all[$index + 1]\" lang=\"lang\" on-navigate=\"navigateTaxonomy\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-4\">\n" +
    "          <div taxonomy-panel taxonomy=\"taxonomies.all[$index + 2]\" lang=\"lang\" on-navigate=\"navigateTaxonomy\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div ng-if=\"taxonomies.taxonomy && !taxonomies.vocabulary\">\n" +
    "      <p class=\"help-block\" ng-repeat=\"label in taxonomies.taxonomy.description\" ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </p>\n" +
    "      <div ng-repeat=\"vocabulary in taxonomies.taxonomy.vocabularies\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "        <div class=\"col-xs-4\">\n" +
    "          <div vocabulary-panel taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabularies[$index]\" lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-4\">\n" +
    "          <div vocabulary-panel taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabularies[$index + 1]\" lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-4\">\n" +
    "          <div vocabulary-panel taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabularies[$index + 2]\" lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div ng-if=\"taxonomies.taxonomy && taxonomies.vocabulary\">\n" +
    "      <p class=\"help-block\" ng-repeat=\"label in taxonomies.vocabulary.description\" ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </p>\n" +
    "      <div ng-repeat=\"term in taxonomies.vocabulary.terms\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "        <div class=\"col-xs-4\">\n" +
    "          <div term-panel taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabulary\" term=\"taxonomies.vocabulary.terms[$index]\" lang=\"lang\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-4\">\n" +
    "          <div term-panel taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabulary\" term=\"taxonomies.vocabulary.terms[$index + 1]\" lang=\"lang\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-xs-4\">\n" +
    "          <div term-panel taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabulary\" term=\"taxonomies.vocabulary.terms[$index + 2]\" lang=\"lang\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/taxonomy-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/taxonomy-panel-template.html",
    "<div>\n" +
    "  <h4 ng-repeat=\"label in taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "    <a href ng-click=\"onNavigate(taxonomy)\">{{label.text}}</a>\n" +
    "  </h4>\n" +
    "  <p class=\"help-block\" ng-repeat=\"label in taxonomy.description\" ng-if=\"label.locale === lang\">\n" +
    "    {{label.text}}\n" +
    "  </p>\n" +
    "  <ul>\n" +
    "    <li ng-repeat=\"vocabulary in taxonomy.vocabularies\" ng-if=\"$index<4\">\n" +
    "      <a href ng-click=\"onNavigate(taxonomy, vocabulary)\">\n" +
    "        <span ng-repeat=\"label in vocabulary.title\" ng-if=\"label.locale === lang\">\n" +
    "          {{label.text}}\n" +
    "        </span>\n" +
    "        <span ng-if=\"!vocabulary.title\">\n" +
    "          {{vocabulary.name}}\n" +
    "        </span>\n" +
    "      </a>\n" +
    "    </li>\n" +
    "    <div class=\"collapse\" id=\"{{taxonomy.name + '_vocabularies'}}\">\n" +
    "      <li ng-repeat=\" vocabulary in taxonomy.vocabularies\" ng-if=\"$index>=4\">\n" +
    "        <a href ng-click=\"onNavigate(taxonomy, vocabulary)\">\n" +
    "          <span ng-repeat=\"label in vocabulary.title\" ng-if=\"label.locale === lang\">\n" +
    "            {{label.text}}\n" +
    "          </span>\n" +
    "          <span ng-if=\"!vocabulary.title\">\n" +
    "            {{vocabulary.name}}\n" +
    "          </span>\n" +
    "        </a>\n" +
    "      </li>\n" +
    "    </div>\n" +
    "    <li ng-if=\"taxonomy.vocabularies && taxonomy.vocabularies.length>4\" class=\"list-unstyled\">\n" +
    "      <a data-toggle=\"collapse\" data-target=\"#{{taxonomy.name + '_vocabularies'}}\">\n" +
    "        <i class=\"fa fa-arrow-down\"></i>\n" +
    "      </a>\n" +
    "    </li>\n" +
    "  </ul>\n" +
    "</div>");
}]);

angular.module("search/views/taxonomy-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/taxonomy-template.html",
    "<div ng-repeat=\"vocabulary in taxonomies.taxonomy.vocabularies\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "  <div class=\"col-xs-4\">\n" +
    "    <div vocabulary-panel taxonomy=\"taxonomies.taxonomy.vocabularies[$index]\"></div>\n" +
    "  </div>\n" +
    "  <div class=\"col-xs-4\">\n" +
    "    <div taxonomy-panel taxonomy=\"taxonomies.taxonomy.vocabularies[$index + 1]\"></div>\n" +
    "  </div>\n" +
    "  <div class=\"col-xs-4\">\n" +
    "    <div taxonomy-panel taxonomy=\"taxonomies.taxonomy.vocabularies[$index + 2]\"></div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/term-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/term-panel-template.html",
    "<div>\n" +
    "  <h4 ng-repeat=\"label in term.title\" ng-if=\"label.locale === lang\">\n" +
    "    {{label.text}}\n" +
    "    <small>\n" +
    "    <a href ng-click=\"onSelect(taxonomy, vocabulary, term)\">\n" +
    "      <i class=\"fa fa-plus-circle\" title=\"{{'add-query' | translate}}\"></i>\n" +
    "    </a>\n" +
    "    </small>\n" +
    "  </h4>\n" +
    "  <p ng-repeat=\"label in term.description\" ng-if=\"label.locale === lang\">\n" +
    "    <span class=\"help-block\">{{label.text}}</span>\n" +
    "  </p>\n" +
    "</div>");
}]);

angular.module("search/views/variables-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/variables-search-result-table-template.html",
    "<div ng-show=\"summaries.length > 0\">\n" +
    "  <div class=\"row voffset2\">\n" +
    "    <div class=\"col-xs-4\">\n" +
    "    </div>\n" +
    "    <div class=\"col-xs-8\">\n" +
    "      <dir-pagination-controls class=\"pull-right\"></dir-pagination-controls>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"table-responsive\">\n" +
    "    <table class=\"table table-bordered table-striped\">\n" +
    "      <thead>\n" +
    "      <tr>\n" +
    "        <th translate>name</th>\n" +
    "        <th translate>label</th>\n" +
    "        <th translate>study</th>\n" +
    "        <th translate>dataset</th>\n" +
    "      </tr>\n" +
    "      </thead>\n" +
    "      <tbody>\n" +
    "\n" +
    "      <tr ng-repeat=\"summary in summaries\">\n" +
    "        <td>\n" +
    "          {{summary.name}}\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          <localized value=\"summary.variableLabel\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          <localized value=\"summary.studyName\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "        <td>\n" +
    "          <localized value=\"summary.datasetName\" lang=\"lang\"></localized>\n" +
    "        </td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/vocabulary-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/vocabulary-panel-template.html",
    "<div>\n" +
    "  <h4 ng-repeat=\"label in vocabulary.title\" ng-if=\"label.locale === lang\">\n" +
    "    <a href ng-click=\"onNavigate(taxonomy, vocabulary)\">{{label.text}}</a>\n" +
    "  </h4>\n" +
    "  <h4 ng-if=\"!vocabulary.title\">\n" +
    "    <a href ng-click=\"onNavigate(taxonomy, vocabulary)\">{{vocabulary.name}}</a>\n" +
    "  </h4>\n" +
    "  <p class=\"help-block\" ng-repeat=\"label in vocabulary.description\" ng-if=\"label.locale === lang\">\n" +
    "    {{label.text}}\n" +
    "  </p>\n" +
    "  <ul>\n" +
    "    <li ng-repeat=\"term in vocabulary.terms\" ng-if=\"$index<4\">\n" +
    "      <span ng-repeat=\"label in term.title\" ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </span>\n" +
    "      <span ng-if=\"!term.title\">\n" +
    "        {{term.name}}\n" +
    "      </span>\n" +
    "      <a href ng-click=\"onSelect(taxonomy, vocabulary, term)\">\n" +
    "        <i class=\"fa fa-plus-circle\" title=\"{{'add-query' | translate}}\"></i>\n" +
    "      </a>\n" +
    "    </li>\n" +
    "    <div class=\"collapse\" id=\"{{taxonomy.name + vocabulary.name + '_terms'}}\">\n" +
    "      <li ng-repeat=\"term in vocabulary.terms\" ng-if=\"$index>=4\">\n" +
    "        <span ng-repeat=\"label in term.title\" ng-if=\"label.locale === lang\">\n" +
    "          {{label.text}}\n" +
    "        </span>\n" +
    "        <span ng-if=\"!term.title\">\n" +
    "          {{term.name}}\n" +
    "        </span>\n" +
    "        <a href ng-click=\"onSelect(taxonomy, vocabulary, term)\">\n" +
    "          <i class=\"fa fa-plus-circle\" title=\"{{'add-query' | translate}}\"></i>\n" +
    "        </a>\n" +
    "      </li>\n" +
    "    </div>\n" +
    "    <li ng-if=\"vocabulary.terms && vocabulary.terms.length>4\" class=\"list-unstyled\">\n" +
    "      <a data-toggle=\"collapse\" data-target=\"#{{taxonomy.name + vocabulary.name + '_terms'}}\">\n" +
    "        <i class=\"fa fa-arrow-down\"></i>\n" +
    "      </a>\n" +
    "    </li>\n" +
    "  </ul>\n" +
    "</div>");
}]);
