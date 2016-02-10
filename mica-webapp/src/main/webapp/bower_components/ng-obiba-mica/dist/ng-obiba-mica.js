/*!
 * ng-obiba-mica - v1.0.0
 * https://github.com/obiba/ng-obiba-mica

 * License: GNU Public License version 3
 * Date: 2016-02-10
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
    'TaxonomiesSearchResource': 'ws/taxonomies/_search',
    'TaxonomiesResource': 'ws/taxonomies/_filter',
    'TaxonomyResource': 'ws/taxonomy/:taxonomy/_filter',
    'VocabularyResource': 'ws/taxonomy/:taxonomy/vocabulary/:vocabulary/_filter',
    'JoinQuerySearchResource': 'ws/:type/_rql?query=:query',
    'JoinQueryCoverageResource': 'ws/variables/_coverage?query=:query'
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
    'obiba.mica.graphics',
    'obiba.mica.localized'
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
    'obiba.alert',
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
  NOT: 'not',
  FACET: 'facet',
  LOCALE: 'locale',
  AGGREGATE: 'aggregate',
  BUCKET: 'bucket',

  /* leaf criteria nodes */
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

/* exported VOCABULARY_TYPES */
var VOCABULARY_TYPES = {
  STRING: 'string',
  INTEGER: 'integer'
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

  this.selectedTerm = function (value) {
    if (!criteria.selectedTerms) {
      criteria.selectedTerms = [];
    }

    criteria.selectedTerms.push(value);
    return this;
  };

  this.selectedTerms = function (values) {
    criteria.selectedTerms = values;
    return this;
  };

  /**
   * This is
   */
  function prepareForLeaf() {
    criteria.id = criteria.taxonomy.name + '.' + criteria.vocabulary.name;

    if (criteria.term) {
      criteria.id += '.' + criteria.term.name;

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
  }

  this.build = function () {
    if (criteria.taxonomy && criteria.vocabulary) {
      prepareForLeaf();
    }
    return criteria;
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

    var foundCount = 0;
    var builder = new CriteriaItemBuilder(self.LocalizedValues, self.lang)
      .type(node.name)
      .target(self.target)
      .taxonomy(targetTaxonomy)
      .vocabulary(targetVocabulary)
      .rqlQuery(node)
      .parent(parentItem);

    if (targetVocabulary.terms) {
      targetVocabulary.terms.some(function (term) {
        if (targetTerms.indexOf(term.name) !== -1) {
          builder.selectedTerm(term).build();
          foundCount++;

          // stop searching
          return foundCount === targetTerms.length;
        }
      });
    }

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
  console.log('CriteriaBuilder.visitLeaf');

  var field = node.args[0];
  var values = node.args[1];
  var searchInfo = this.fieldToVocabulary(field);
  var item =
    this.buildLeafItem(searchInfo.taxonomy,
      searchInfo.vocabulary,
      values instanceof Array ? values : [values],
      node,
      parentItem);

  parentItem.children.push(item);
};

/**
 * Returns all the criterias found
 * @returns {Array}
 */
CriteriaBuilder.prototype.getRootItem = function (/*node*/) {
  return this.rootItem;
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
      this.visitCondition(node, parentItem);
      break;

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
      this.visitLeaf(node, parentItem);
      break;
    case RQL_NODE.MATCH:
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
          case RQL_NODE.NOT:
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

    this.variableQuery = function () {
      return new RqlQuery(QUERY_TARGETS.VARIABLE);
    };

    this.eqQuery = function (field, term) {
      var query = new RqlQuery(RQL_NODE.EQ);
      query.args.push(term);
      return query;
    };

    this.inQuery = function (field, terms) {
      var hasValues = terms && terms.length > 0;
      var name = hasValues ? RQL_NODE.IN : RQL_NODE.EXISTS;
      var query = new RqlQuery(name);
      query.args.push(field);

      if (hasValues) {
        query.args.push(terms);
      }

      return query;
    };

    this.updateInQuery = function (query, terms, missing) {
      var hasValues = terms && terms.length > 0;
      query.name = hasValues ? RQL_NODE.IN : (missing ? RQL_NODE.MISSING : RQL_NODE.EXISTS);

      if (hasValues) {
        query.args[1] = terms;
      } else {
        query.args.splice(1, 1);
      }

      return query;
    };

    /**
     * Helper finding the vocabulary field, return name if none was found
     *
     * @param taxonomy
     * @param vocabulary
     * @returns {*}
     */
    this.vocabularyFieldName = function (taxonomy, vocabulary) {
      return taxonomy.name + '.' + vocabulary.name;
    };

    /**
     * Creates a RqlQuery from an item
     *
     * @param item
     * @returns {RqlQuery}
     */
    this.buildRqlQuery = function (item) {
      // TODO take care of other type (min, max, in, ...)
      return this.inQuery(this.vocabularyFieldName(item.taxonomy, item.vocabulary), item.term ? item.term.name : []);
    };

    /**
     * Adds a new query to the parent query node
     *
     * @param parentQuery
     * @param query
     * @returns {*}
     */
    this.addQuery = function (parentQuery, query) {

      if (parentQuery.args.length === 0) {
        parentQuery.args.push(query);
      } else {
        var parentIndex = findValidParentNode(parentQuery);

        if (parentIndex === -1) {
          parentQuery.args.push(query);
        } else {
          var oldArg = parentQuery.args.splice(parentIndex, 1).pop();
          var orQuery = new RqlQuery(RQL_NODE.OR);
          orQuery.args.push(oldArg, query);
          parentQuery.args.push(orQuery);
        }
      }

      return parentQuery;
    };

    this.updateQuery = function (query, values, missing) {

      switch (query.name) {
        case RQL_NODE.IN:
        case RQL_NODE.EXISTS:
        case RQL_NODE.MISSING:
          this.updateInQuery(query, values, missing);
          break;
      }

    };

    this.vocabularyType = function(vocabulary) {
      var type = VOCABULARY_TYPES.STRING;
      if (vocabulary.attributes) {
        vocabulary.attributes.some(function(attribute){
          if (attribute.key === 'type') {
            type = attribute.value;
            return true;
          }

          return false;
        });
      }

      return type;
    };
  }])


  .service('RqlQueryService', [
    '$q',
    'TaxonomiesResource',
    'LocalizedValues',
    'RqlQueryUtils',
    function ($q, TaxonomiesResource, LocalizedValues, RqlQueryUtils) {
      var taxonomiesCache = {
        variable: null,
        dataset: null,
        study: null,
        network: null
      };

      function isLeafCriteria(item) {
        switch (item.type) {
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
        var children = query.args;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);
        if (index === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parentQuery.args.splice(index, 1);

        if (children) {
          if (children instanceof Array) {
            parentQuery.args = parentQuery.args.concat(children);
          } else {
            parentQuery.args.push(children);
          }
        }

        if (parent.parent !== null && parentQuery.args.length === 0) {
          deleteNode(parent);
        }
      }

      function deleteNodeCriteriaWithOrphans(item) {
        var parent = item.parent;

        var query = item.rqlQuery;
        var children = query.args;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);
        if (index === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parentQuery.args.splice(index, 1);

        if (children) {
          if (children instanceof Array) {
            parentQuery.args = parentQuery.args.concat(children);
          } else {
            parentQuery.args.push(children);
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

        if ([RQL_NODE.OR, RQL_NODE.AND, RQL_NODE.NAND].indexOf(parent.type) !== -1) {
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

        return new CriteriaItemBuilder(LocalizedValues, lang)
          .target(target)
          .taxonomy(taxonomy)
          .vocabulary(vocabulary)
          .term(term)
          .build();
      };

      /**
       * Adds new item to the item tree
       *
       * @param rootItem
       * @param item
       */
      this.addCriteriaItem = function (rootRql, newItem) {
        var target = rootRql.args.filter(function (query) {
          return newItem.target === query.name;
        }).pop();

        if (!target) {
          target = new RqlQuery(RQL_NODE[newItem.target.toUpperCase()]);
          rootRql.args.push(target);
        }

        var rqlQuery = RqlQueryUtils.buildRqlQuery(newItem);
        return RqlQueryUtils.addQuery(target, rqlQuery);
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
          deferred.resolve(builder.getRootItem());
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

        if (rootRql.args.length === 0) {
          deferred.resolve(rootItem);
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
          self.builders(node.name, node, rootItem, lang).then(function (item) {
            rootItem.children.push(item);
            resolvedCount++;
            if (resolvedCount === queries.length) {
              deferred.resolve(rootItem);
            }
          });
        });

        return deferred.promise;
      };

      /**
       * Append the aggregate and facet for criteria term listing.
       *
       * @param type
       * @param query
       * @param taxonomy
       * @param vocabulary
       * @returns the new query
       */
      this.prepareCriteriaTermsQuery = function (target, query, taxonomy, vocabulary) {
        var parsedQuery = new RqlParser().parse(query);
        var aggregate = new RqlQuery('aggregate');
        var facet = new RqlQuery('facet');
        aggregate.args.push(RqlQueryUtils.vocabularyFieldName(taxonomy, vocabulary));
        parsedQuery.args.some(function (arg) {
          if (arg.name === target) {
            arg.args.push(aggregate);
            return true;
          }
          return false;
        });

        parsedQuery.args.push(facet);

        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      /**
       * Append the aggregate and bucket operations to the variable.
       *
       * @param query
       * @param bucketArgs
       * @returns the new query
       */
      this.prepareCoverageQuery = function (query, bucketArgs) {
        var parsedQuery = new RqlParser().parse(query);
        var aggregate = new RqlQuery('aggregate');
        var bucket = new RqlQuery('bucket');
        bucketArgs.forEach(function (b) {
          bucket.args.push(b);
        });
        aggregate.args.push(bucket);
        var variable;
        parsedQuery.args.forEach(function (arg) {
          if (!variable && arg.name === 'variable') {
            variable = arg;
          }
        });
        if(!variable) {
          variable = new RqlQuery('variable');
          parsedQuery.args.push(variable);
        }
        variable.args.push(aggregate);
        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      this.prepareGraphicsQuery = function (query, aggregateArgs) {
        var parsedQuery = new RqlParser().parse(query);
        // aggregate
        var aggregate = new RqlQuery('aggregate');
        aggregateArgs.forEach(function (a) {
          aggregate.args.push(a);
        });
        // limit
        var limit = new RqlQuery('limit');
        limit.args.push(0);
        limit.args.push(0);
        // study
        var study;
        parsedQuery.args.forEach(function (arg) {
          if (arg.name === 'study') {
            study = arg;
          }
        });
        if (!study) {
          study = new RqlQuery('study');
          parsedQuery.args.push(study);
        }
        study.args.push(aggregate);
        study.args.push(limit);
        // facet
        parsedQuery.args.push(new RqlQuery('facet'));
        return parsedQuery.serializeArgs(parsedQuery.args);
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

/* global CRITERIA_ITEM_EVENT */
/* global QUERY_TARGETS */
/* global QUERY_TYPES */
/* global RQL_NODE */

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

var DISPLAY_TYPES = {
  LIST: 'list',
  COVERAGE: 'coverage',
  GRAPHICS: 'graphics'
};

angular.module('obiba.mica.search')

  .controller('SearchController', [
    '$scope',
    '$timeout',
    '$routeParams',
    '$location',
    'TaxonomiesSearchResource',
    'TaxonomiesResource',
    'TaxonomyResource',
    'VocabularyResource',
    'ngObibaMicaSearchTemplateUrl',
    'JoinQuerySearchResource',
    'JoinQueryCoverageResource',
    'AlertService',
    'ServerErrorUtils',
    'LocalizedValues',
    'ObibaSearchConfig',
    'RqlQueryService',

    function ($scope,
              $timeout,
              $routeParams,
              $location,
              TaxonomiesSearchResource,
              TaxonomiesResource,
              TaxonomyResource,
              VocabularyResource,
              ngObibaMicaSearchTemplateUrl,
              JoinQuerySearchResource,
              JoinQueryCoverageResource,
              AlertService,
              ServerErrorUtils,
              LocalizedValues,
              ObibaSearchConfig,
              RqlQueryService) {

      $scope.settingsDisplay = ObibaSearchConfig.getOptions();

      function onError(response) {
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

      function validateDisplay(display) {
        if (!display || !DISPLAY_TYPES[display.toUpperCase()]) {
          throw new Error('Invalid display: ' + display);
        }
      }

      function getDefaultQueryType() {
        if ($scope.settingsDisplay.variables.showSearchTab) {
          return QUERY_TYPES.VARIABLES;
        }
        else {
          var result = Object.keys($scope.settingsDisplay).filter(function (key) {
            return $scope.settingsDisplay[key].showSearchTab === 1;
          });
          console.log(result);
          return result[result.length - 1];
        }
      }

      function validateQueryData() {
        try {
          var search = $location.search();
          var type = search.type || getDefaultQueryType();
          var display = search.display || DISPLAY_TYPES.LIST;
          var query = search.query || '';
          validateType(type);
          validateDisplay(display);

          $scope.search.type = type;
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

      function executeSearchQuery() {
        if (validateQueryData()) {
          // build the criteria UI
          RqlQueryService.createCriteria($scope.search.rqlQuery, $scope.lang).then(function(rootItem) {
            // criteria UI is updated here
            $scope.search.criteria = rootItem;
          });

          $scope.search.loading = true;
          switch ($scope.search.display) {
            case DISPLAY_TYPES.LIST:
              JoinQuerySearchResource[$scope.search.type]({query: $scope.search.query},
                function onSuccess(response) {
                  $scope.search.result.list = response;
                  $scope.search.loading = false;
                },
                onError);
              break;
            case DISPLAY_TYPES.COVERAGE:
              JoinQueryCoverageResource.get({query: RqlQueryService.prepareCoverageQuery($scope.search.query, ['studyIds'])},
                function onSuccess(response) {
                  $scope.search.result.coverage = response;
                  $scope.search.loading = false;
                },
                onError);
              break;
            case DISPLAY_TYPES.GRAPHICS:
              JoinQuerySearchResource.studies({
                  query: RqlQueryService.prepareGraphicsQuery($scope.search.query,
                    ['methods.designs', 'populations.selectionCriteria.countriesIso', 'populations.dataCollectionEvents.bioSamples', 'numberOfParticipants.participant.number'])
                },
                function onSuccess(response) {
                  $scope.search.result.graphics = response;
                  $scope.search.loading = false;
                },
                onError);
              break;
          }
        }
      }

      var closeTaxonomies = function () {
        angular.element('#taxonomies').collapse('hide');
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
          angular.element('#taxonomies').collapse('show');
        } else if ($scope.taxonomies.target === target) {
          closeTaxonomies();
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

      /**
       * Updates the URL location triggering a query execution
       */
      var refreshQuery = function() {
        var query = new RqlQuery().serializeArgs($scope.search.rqlQuery.args);
        var search = $location.search();
        // TODO bug when there other queries such as locale etc
        if ('' === query) {
          delete search.query;
        } else {
          search.query = query;
        }
        $location.search(search).replace();
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
        return TaxonomiesSearchResource.get({
          query: query
        }).$promise.then(function (response) {
          if (response) {
            var results = [];
            var total = 0;
            var size = 10;
            response.forEach(function (bundle) {
              var target = bundle.target;
              var taxonomy = bundle.taxonomy;
              if (taxonomy.vocabularies) {
                taxonomy.vocabularies.forEach(function (vocabulary) {
                  if (vocabulary.terms) {
                    vocabulary.terms.forEach(function (term) {
                      if (results.length < size) {
                        results.push(RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, term, $scope.lang));
                      }
                      total++;
                    });
                  } else {
                    if (results.length < size) {
                      results.push(RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, null, $scope.lang));
                    }
                    total++;
                  }
                });
              }
            });
            if (total > results.length) {
              var note = {
                query: query,
                total: total,
                size: size,
                message: 'Showing ' + size + ' / ' + total,
                status: 'has-warning'
              };
              results.push(note);
            }
            return results;
          } else {
            return [];
          }
        });
      };

      /**
       * Propagates a Scope change that results in criteria panel update
       * @param item
       */
      var selectCriteria = function (item) {
        if (item.id) {
          RqlQueryService.addCriteriaItem($scope.search.rqlQuery, item);
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

      var navigateTaxonomy = function (taxonomy, vocabulary) {
        var toFilter = ($scope.taxonomies.taxonomy && !taxonomy) || ($scope.taxonomies.vocabulary && !vocabulary);
        $scope.taxonomies.taxonomy = taxonomy;
        $scope.taxonomies.vocabulary = vocabulary;
        if (toFilter) {
          filterTaxonomies($scope.taxonomies.search.text);
        }
      };

      /**
       * Callback used in the views
       *
       * @param target
       * @param taxonomy
       * @param vocabulary
       * @param term
       */
      var selectTerm = function (target, taxonomy, vocabulary, term) {
        selectCriteria(RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, term, $scope.lang));
      };

      var updateTerm = function() {
        refreshQuery();
      };

      var onTypeChanged = function (type) {
        if (type) {
          validateType(type);
          var search = $location.search();
          search.type = type;
          $location.search(search).replace();
        }
      };

      var onDisplayChanged = function (display) {
        if (display) {
          validateDisplay(display);
          var search = $location.search();
          search.display = display;
          $location.search(search).replace();
        }
      };

      /**
       * Removes the item from the criteria tree
       * @param item
       */
      var removeCriteriaItem = function(item) {
        RqlQueryService.removeCriteriaItem(item);
        refreshQuery();
      };

      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.lang = 'en';

      $scope.search = {
        query: null,
        rqlQuery: null,
        type: null,
        result: {
          list: null,
          coverage: null,
          graphics: null
        },
        criteria: [],
        loading: false
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
      $scope.searchCriteria = searchCriteria;
      $scope.selectCriteria = selectCriteria;
      $scope.searchKeyUp = searchKeyUp;
      $scope.filterTaxonomiesKeyUp = filterTaxonomiesKeyUp;
      $scope.navigateTaxonomy = navigateTaxonomy;
      $scope.selectTaxonomyTarget = selectTaxonomyTarget;
      $scope.selectTerm = selectTerm;
      $scope.removeCriteriaItem = removeCriteriaItem;
      $scope.refreshQuery = refreshQuery;
      $scope.closeTaxonomies = closeTaxonomies;
      $scope.onTypeChanged = onTypeChanged;
      $scope.onDisplayChanged = onDisplayChanged;
      $scope.taxonomiesShown = false;
      $scope.updateTerm = updateTerm;

      //// TODO replace with angular code
      angular.element('#taxonomies').on('show.bs.collapse', function () {
        $scope.taxonomiesShown = true;
      });
      angular.element('#taxonomies').on('hide.bs.collapse', function () {
        $scope.taxonomiesShown = false;
      });

      $scope.$watch('search', function () {
        executeSearchQuery();
      });

      $scope.$on('$locationChangeSuccess', function (newLocation, oldLocation) {
        if (newLocation !== oldLocation) {
          executeSearchQuery();
        }
      });

    }])

  .controller('SearchResultController', [
    '$scope',
    'ObibaSearchConfig',
    function ($scope,
              ObibaSearchConfig) {

      $scope.settingsDisplay = ObibaSearchConfig.getOptions();

      $scope.selectDisplay = function (display) {
        console.log('Display', display);
        $scope.display = display;
        $scope.$parent.onDisplayChanged(display);
      };
      $scope.selectTarget = function (type) {
        console.log('Target', type);
        $scope.type = type;
        $scope.$parent.onTypeChanged(type);
      };
      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.DISPLAY_TYPES = DISPLAY_TYPES;

      $scope.$watch('type', function () {
        $scope.activeTarget = {
          networks: ($scope.type === QUERY_TYPES.NETWORKS && $scope.settingsDisplay.networks.showSearchTab) || false,
          studies: ($scope.type === QUERY_TYPES.STUDIES && $scope.settingsDisplay.studies.showSearchTab) || false,
          datasets: ($scope.type === QUERY_TYPES.DATASETS && $scope.settingsDisplay.datasets.showSearchTab) || false,
          variables: ($scope.type === QUERY_TYPES.VARIABLES && $scope.settingsDisplay.variables.showSearchTab) || false
        };
      });

      $scope.$watch('display', function () {
        $scope.activeDisplay = {
          list: $scope.display === DISPLAY_TYPES.LIST || false,
          coverage: $scope.display === DISPLAY_TYPES.COVERAGE || false,
          graphics: $scope.display === DISPLAY_TYPES.GRAPHICS || false
        };
      });

    }])

  .controller('CriterionLogicalController', [
    '$scope',
    function ($scope) {
      $scope.updateLogical = function(operator) {
        $scope.item.rqlQuery.name = operator;
        $scope.$emit(CRITERIA_ITEM_EVENT.refresh);
      };
    }])

  .controller('CriterionDropdownController', [
    '$scope',
    'RqlQueryService',
    'LocalizedValues',
    'JoinQuerySearchResource',
    'RqlQueryUtils',
    function ($scope, RqlQueryService, LocalizedValues, JoinQuerySearchResource, RqlQueryUtils) {

      console.log('TYPE', RqlQueryUtils.vocabularyType($scope.criterion.vocabulary));
      var isSelected = function (name) {
        return $scope.selectedTerms.indexOf(name) !== -1;
      };

      var toggleSelection = function (event, term) {
        $scope.state.dirty = true;

        if (isSelected(term.name)) {
          $scope.selectedTerms = $scope.selectedTerms.filter(function (name) {
            return name !== term.name;
          });
        } else {
          $scope.selectedTerms.push(term.name);
        }

        RqlQueryUtils.updateQuery($scope.criterion.rqlQuery, $scope.selectedTerms);
      };

      var localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };

      var truncate = function (text, size) {
        var max = size || 40;
        return text.length > max ? text.substring(0, max) + '...' : text;
      };

      var closeDropdown = function() {
        if (!$scope.state.open) {
          return;
        }

        var wasDirty = $scope.state.dirty;
        $scope.state.open = false;
        $scope.state.dirty = false;
        $scope.$apply();
        if (wasDirty) {
          // trigger a query update
          console.log('Send event',CRITERIA_ITEM_EVENT.selected);
          $scope.$emit(CRITERIA_ITEM_EVENT.selected);
        }
      };

      var openDropdown = function () {
        if ($scope.state.open) {
          closeDropdown();
          return;
        }

        $scope.state.open = true;

        var target = $scope.criterion.target;
        var joinQuery =
          RqlQueryService.prepareCriteriaTermsQuery(
            target,
            $scope.query,
            $scope.criterion.taxonomy,
            $scope.criterion.vocabulary);

        JoinQuerySearchResource[targetToType(target)]({query: joinQuery}).$promise.then(function () {
          $scope.state.open = true;
        });
      };

      var updateFilter = function() {
        RqlQueryUtils.updateQuery($scope.criterion.rqlQuery, [], RQL_NODE.MISSING === $scope.selectedFilter);
        $scope.state.dirty = true;
      };

      var remove = function() {
        $scope.$emit(CRITERIA_ITEM_EVENT.deleted, $scope.criterion);
      };

      var isInFilter = function() {
        return $scope.selectedFilter === RQL_NODE.IN;
      };

      $scope.selectedTerms = $scope.criterion.selectedTerms && $scope.criterion.selectedTerms.map(function (term) {
        return term.name;
      }) || [];

      $scope.RQL_NODE = RQL_NODE;
      $scope.state = {open: false, dirty: false};

      $scope.selectedFilter = $scope.criterion.type;
      $scope.remove = remove;
      $scope.openDropdown = openDropdown;
      $scope.closeDropdown = closeDropdown;
      $scope.toggleSelection = toggleSelection;
      $scope.isSelected = isSelected;
      $scope.updateFilter = updateFilter;
      $scope.localize = localize;
      $scope.truncate = truncate;
      $scope.isInFilter = isInFilter;
    }])

  .controller('CoverageResultTableController', [
    '$scope',
    function ($scope) {

      function processCoverageResponse() {
        var response = $scope.result;
        var taxonomyHeaders = [];
        var vocabularyHeaders = [];
        var termHeaders = [];
        var rows = {};
        var footers = {
          total: []
        };
        if (response.taxonomies) {
          var termsCount = 0;
          response.taxonomies.forEach(function (taxo) {
            var taxonomyTermsCount = 0;
            if (taxo.vocabularies) {
              taxo.vocabularies.forEach(function (voc) {
                if (voc.terms) {
                  voc.terms.forEach(function (trm) {
                    termsCount++;
                    taxonomyTermsCount++;
                    termHeaders.push({
                      taxonomy: taxo.taxonomy,
                      vocabulary: voc.vocabulary,
                      term: trm.term
                    });
                    footers.total.push(trm.hits);
                    if (trm.buckets) {
                      trm.buckets.forEach(function (bucket) {
                        if (!(bucket.field in rows)) {
                          rows[bucket.field] = {};
                        }
                        if (!(bucket.value in rows[bucket.field])) {
                          rows[bucket.field][bucket.value] = {
                            field: bucket.field,
                            title: bucket.title,
                            description: bucket.description,
                            hits: {}
                          };
                        }
                        // store the hits per field, per value at the position of the term
                        rows[bucket.field][bucket.value].hits[termsCount] = bucket.hits;
                      });
                    }
                  });
                  vocabularyHeaders.push({
                    taxonomy: taxo.taxonomy,
                    vocabulary: voc.vocabulary,
                    termsCount: voc.terms.length
                  });
                }
              });
              taxonomyHeaders.push({
                taxonomy: taxo.taxonomy,
                termsCount: taxonomyTermsCount
              });
            }
          });
        }

        // compute totalHits for each row
        Object.keys(rows).forEach(function (field) {
          Object.keys(rows[field]).forEach(function (value) {
            var hits = rows[field][value].hits;
            rows[field][value].totalHits = Object.keys(hits).map(function (idx) {
              return hits[idx];
            }).reduce(function (a, b) {
              return a + b;
            });
          });
        });

        $scope.table = {
          taxonomyHeaders: taxonomyHeaders,
          vocabularyHeaders: vocabularyHeaders,
          termHeaders: termHeaders,
          rows: rows,
          footers: footers,
          totalHits: response.totalHits,
          totalCount: response.totalCount
        };
      }

      $scope.$watch('result', function () {
        if ($scope.result) {
          processCoverageResponse();
        } else {
          $scope.table = null;
        }
      });

      $scope.showMissing = true;
      $scope.toggleMissing = function (value) {
        $scope.showMissing = value;
      };
      $scope.keys = Object.keys;

    }])

  .controller('GraphicsResultController', ['GraphicChartsConfig', 'GraphicChartsUtils',
    '$scope',
    function (GraphicChartsConfig, GraphicChartsUtils, $scope) {
      //var aggs = ['methods.designs', 'populations.selectionCriteria.countriesIso', 'populations.dataCollectionEvents.bioSamples', 'numberOfParticipants.participant.number']
      $scope.$watch('result', function (result) {
        if (result) {
          var geoStudies = GraphicChartsUtils.getArrayByAggregation('populations-selectionCriteria-countriesIso', result.studyResultDto, 'country', $scope.lang);
          geoStudies.unshift(['Country', 'Nbr of Studies']);

          var methodDesignStudies = GraphicChartsUtils.getArrayByAggregation('methods-designs', result.studyResultDto, null, $scope.lang);
          methodDesignStudies.unshift(['Study design', 'Number of studies']);

          var bioSamplesStudies = GraphicChartsUtils.getArrayByAggregation('populations-dataCollectionEvents-bioSamples', result.studyResultDto, null, $scope.lang);
          bioSamplesStudies.unshift(['Collected biological samples', 'Number of studies']);

          $scope.chartObjects = {
            geoChartOptions: {
              chartObject: {
                options: GraphicChartsConfig.getOptions().ChartsOptions.geoChartOptions.options,
                type: 'GeoChart',
                data: geoStudies
              }
            },
            studiesDesigns: {
              chartObject: {
                options: GraphicChartsConfig.getOptions().ChartsOptions.studiesDesigns.options,
                type: 'BarChart',
                data: methodDesignStudies
              }
            },
            biologicalSamples: {
              chartObject: {
                options : GraphicChartsConfig.getOptions().ChartsOptions.biologicalSamples.options,
                type: 'PieChart',
                data: bioSamplesStudies
              }
            }
          };

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

/* global RQL_NODE */

/* exported CRITERIA_ITEM_EVENT */
var CRITERIA_ITEM_EVENT = {
  selected: 'event:select-criteria-item',
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
        taxonomy: '=',
        vocabulary: '=',
        lang: '=',
        onNavigate: '='
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

  .directive('networksResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/networks-search-result-table-template.html'
    };
  }])

  .directive('datasetsResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/datasets-search-result-table-template.html'
    };
  }])

  .directive('studiesResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/studies-search-result-table-template.html'
    };
  }])

  .directive('variablesResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '='
      },
      templateUrl: 'search/views/list/variables-search-result-table-template.html'
    };
  }])

  .directive('coverageResultTable', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        result: '=',
        loading: '='
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
        loading: '='
      },
      controller: 'GraphicsResultController',
      templateUrl: 'search/views/graphics/graphics-search-result-template.html'
    };
  }])

  .directive('resultPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        type: '=',
        display: '=',
        result: '=',
        lang: '=',
        loading: '=',
        onTypeChanged: '='
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
        onRemove: '=',
        onSelect: '=',
        onRefresh: '='
      },
      template: '<span ng-repeat="child in item.children"><criteria-target item="child"></criteria-target></span>',
      link: function(scope) {
        scope.$on(CRITERIA_ITEM_EVENT.deleted, function(event, item){
          scope.onRemove(item);
        });

        scope.$on(CRITERIA_ITEM_EVENT.selected, function(){
          scope.onSelect();
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
        item: '='
      },
      template: '<span ng-repeat="child in item.children" ><criteria-node item="child"></criteria-node></span>',
      link: function(scope) {
        console.log('criteriaTarget', scope.item);
      }
    };
  }])

  .directive('criteriaNode', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        item: '='
      },
      controller: 'CriterionLogicalController',
      templateUrl: 'search/views/criteria/criteria-node-template.html',
      link: function(scope) {
        console.log('criteriaNode', scope.item);
      }
    };
  }])

  /**
   * This directive is responsible to build the proper type of drop-down leaf
   *
   * TODO needs more specialization
   */
  .directive('criteriaLeaf', ['$compile',
    function($compile){
      return {
        restrict: 'EA',
        replace: true,
        scope: {
          item: '=',
          parentType: '='
        },
        template: '<span></span>',
        link: function(scope, element) {
          console.log('criteriaLeaf', scope);

          var template = '';
          if (scope.item.type === RQL_NODE.OR || scope.item.type === RQL_NODE.AND || scope.item.type === RQL_NODE.NAND) {
            template = '<criteria-node item="item"></criteria-node>';
            $compile(template)(scope, function(cloned){
              element.append(cloned);
            });
          } else {
            template = '<span criterion-dropdown criterion="item"></span>';
            $compile(template)(scope, function(cloned){
              element.append(cloned);
            });
          }
        }
      };
    }])

  .directive('criterionDropdown', ['$document', function ($document) {
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
          var isChild = document.querySelector('#'+$scope.criterion.vocabulary.name+'-dropdown').contains(event.target);
          if (!isChild) {
            $scope.closeDropdown();
          }
        };

        $document.on('click', onDocumentClick);
        $element.on('$destroy', function () {
          $document.off('click', onDocumentClick);
        });
      }
    };
  }])

  .directive('criteriaPanel', [function () {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        criteria: '=',
        query: '='
      },
      controller: 'CriteriaPanelController',
      templateUrl: 'search/views/criteria-panel-template.html'
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
          $scope.ItemDataJSon = GraphicChartsUtils.getArrayByAggregation($scope.chartAggregationName, StudiesData[$scope.chartEntityDto], $scope.fieldTransformer, $scope.lang);
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
        entityType: null,
        ChartsOptions: {
          geoChartOptions: {
            options: {
              backgroundColor: {fill: 'transparent'},
              title: 'Distribution of studies by participants countries of residence',
              colors: [
                '#4db300',
                '#409400',
                '#317000',
                '#235200'
              ],
              width: 500,
              height: 300
            }
          },
          studiesDesigns: {
            options: {
              backgroundColor: {fill: 'transparent'},
              title: 'Distribution of studies by study design',
              colors: ['#006600',
                '#009900',
                '#009966',
                '#009933',
                '#66CC33'],
              width: 500,
              height: 300
            }
          },
          biologicalSamples: {
            options: {
              backgroundColor: {fill: 'transparent'},
              title: 'Distribution of studies by Biological Samples',
              colors: ['#006600',
                '#009900',
                '#009966',
                '#009933',
                '#66CC33'],
              width: 500,
              height: 300
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
  .service('GraphicChartsUtils', [
    'CountriesIsoUtils',
    function (CountriesIsoUtils) {
      this.getArrayByAggregation = function (aggregationName, entityDto, fieldTransformer, lang) {
        var arrayData = [];
        if (!entityDto) {
          return arrayData;
        }

        angular.forEach(entityDto.aggs, function (aggregation) {
          var itemName = [];
          if (aggregation.aggregation === aggregationName) {
            var i = 0;
            angular.forEach(aggregation['obiba.mica.TermsAggregationResultDto.terms'], function (term) {
              switch (fieldTransformer) {
                // TODO countries are already translated on server side (term title)
                case 'country':
                  itemName.name = CountriesIsoUtils.findByCode(term.key.toUpperCase(), lang);
                  break;
                default :
                  itemName.name = term.title;
                  break;
              }
              if (term.count) {
                arrayData[i] = [itemName.name, term.count];
                i++;
              }
            });
          }
        });

        return arrayData;
      };
    }])
  .service('GraphicChartsQuery', [function () {
    this.queryDtoBuilder = function (entityIds) {
      if (!(entityIds) || entityIds === 'NaN') {
        return '{"studyQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0}},"locale":"en","withFacets":true}';
      }
      else {
        return '{"studyQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0}},"networkQueryDto":{"from":0,"size":0,"sort":{"field":"acronym.en","order":0},"filteredQuery":{"obiba.mica.LogicalFilterQueryDto.filter":{"fields":[{"field":{"field":"id","obiba.mica.TermsFilterQueryDto.terms":{"values":["' + entityIds + '"]}},"op":1}]}}},"locale":"en","withFacets":true}';
      }
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

  .directive('localized', [function () {
    return {
      restrict: 'AE',
      scope: {
        value: '=',
        lang: '='
      },
      template: '<span ng-repeat="localizedValue in value | filter:{lang:lang}">{{localizedValue.value}}</span>'
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
          return values;
        }
      };

      this.forLocale = function (values, lang) {
        return this.for(values, lang, 'locale', 'text');
      };

      this.forLang = function (values, lang) {
        return this.for(values, lang, 'lang', 'value');
      };
    });
;angular.module('templates-ngObibaMica', ['access/views/data-access-request-form.html', 'access/views/data-access-request-histroy-view.html', 'access/views/data-access-request-list.html', 'access/views/data-access-request-profile-user-modal.html', 'access/views/data-access-request-submitted-modal.html', 'access/views/data-access-request-validation-modal.html', 'access/views/data-access-request-view.html', 'attachment/attachment-input-template.html', 'attachment/attachment-list-template.html', 'graphics/views/charts-directive.html', 'localized/localized-input-group-template.html', 'localized/localized-input-template.html', 'localized/localized-textarea-template.html', 'search/views/classifications/taxonomies-view.html', 'search/views/classifications/taxonomy-panel-template.html', 'search/views/classifications/taxonomy-template.html', 'search/views/classifications/term-panel-template.html', 'search/views/classifications/vocabulary-panel-template.html', 'search/views/coverage/coverage-search-result-table-template.html', 'search/views/criteria/criteria-node-template.html', 'search/views/criteria/criterion-dropdown-template.html', 'search/views/criteria/target-template.html', 'search/views/graphics/graphics-search-result-template.html', 'search/views/list/datasets-search-result-table-template.html', 'search/views/list/networks-search-result-table-template.html', 'search/views/list/studies-search-result-table-template.html', 'search/views/list/variables-search-result-table-template.html', 'search/views/search-result-panel-template.html', 'search/views/search.html']);

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

angular.module("search/views/classifications/taxonomies-view.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/taxonomies-view.html",
    "<div class=\"panel panel-default\">\n" +
    "  <div class=\"panel-heading\">\n" +
    "    <div class=\"row\">\n" +
    "      <div class=\"col-md-8 voffset1\">\n" +
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
    "      <div class=\"col-md-4\">\n" +
    "        <div class=\"form-inline pull-right\">\n" +
    "          <div class=\"form-group\">\n" +
    "            <span class=\"input-group input-group-sm no-padding-top\">\n" +
    "              <span class=\"input-group-addon\"><i class=\"glyphicon glyphicon-filter\"></i></span>\n" +
    "              <input ng-keyup=\"filterTaxonomiesKeyUp($event)\" ng-model=\"taxonomies.search.text\" type=\"text\"\n" +
    "                class=\"form-control ng-pristine ng-untouched ng-valid\" aria-describedby=\"study-search\">\n" +
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
    "        <div class=\"col-md-4\">\n" +
    "          <div taxonomy-panel taxonomy=\"taxonomies.all[$index]\" lang=\"lang\" on-navigate=\"navigateTaxonomy\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div taxonomy-panel taxonomy=\"taxonomies.all[$index + 1]\" lang=\"lang\" on-navigate=\"navigateTaxonomy\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
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
    "        <div class=\"col-md-4\">\n" +
    "          <div vocabulary-panel taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabularies[$index]\"\n" +
    "            lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div vocabulary-panel taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabularies[$index + 1]\"\n" +
    "            lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div vocabulary-panel taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.taxonomy.vocabularies[$index + 2]\"\n" +
    "            lang=\"lang\" on-navigate=\"navigateTaxonomy\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div ng-if=\"taxonomies.taxonomy && taxonomies.vocabulary\">\n" +
    "      <p class=\"help-block\" ng-repeat=\"label in taxonomies.vocabulary.description\" ng-if=\"label.locale === lang\">\n" +
    "        {{label.text}}\n" +
    "      </p>\n" +
    "      <div ng-repeat=\"term in taxonomies.vocabulary.terms\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div term-panel target=\"taxonomies.target\" taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.vocabulary\"\n" +
    "            term=\"taxonomies.vocabulary.terms[$index]\" lang=\"lang\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div term-panel target=\"taxonomies.target\" taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.vocabulary\"\n" +
    "            term=\"taxonomies.vocabulary.terms[$index + 1]\" lang=\"lang\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4\">\n" +
    "          <div term-panel target=\"taxonomies.target\" taxonomy=\"taxonomies.taxonomy\" vocabulary=\"taxonomies.vocabulary\"\n" +
    "            term=\"taxonomies.vocabulary.terms[$index + 2]\" lang=\"lang\" on-select=\"selectTerm\"></div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/classifications/taxonomy-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/taxonomy-panel-template.html",
    "<div>\n" +
    "  <h4 ng-repeat=\"label in taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "    <a href ng-click=\"onNavigate(taxonomy)\">{{label.text}}</a>\n" +
    "  </h4>\n" +
    "  <p class=\"help-block\" ng-repeat=\"label in taxonomy.description\" ng-if=\"label.locale === lang\">\n" +
    "    {{label.text}}\n" +
    "  </p>\n" +
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
    "  <h4 ng-repeat=\"label in term.title\" ng-if=\"label.locale === lang\">\n" +
    "    {{label.text}}\n" +
    "    <small>\n" +
    "    <a href ng-click=\"onSelect(target, taxonomy, vocabulary, term)\">\n" +
    "      <i class=\"fa fa-plus-circle\" title=\"{{'add-query' | translate}}\"></i>\n" +
    "    </a>\n" +
    "    </small>\n" +
    "  </h4>\n" +
    "  <p ng-repeat=\"label in term.description\" ng-if=\"label.locale === lang\">\n" +
    "    <span class=\"help-block\">{{label.text}}</span>\n" +
    "  </p>\n" +
    "</div>");
}]);

angular.module("search/views/classifications/vocabulary-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/vocabulary-panel-template.html",
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
    "</div>");
}]);

angular.module("search/views/coverage/coverage-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/coverage/coverage-search-result-table-template.html",
    "<div>\n" +
    "  <p class=\"help-block\" ng-if=\"!loading && table.taxonomyHeaders.length === 0\" translate>no-coverage</p>\n" +
    "\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "\n" +
    "\n" +
    "  <div class=\"table-responsive\" ng-if=\"table.taxonomyHeaders.length > 0\">\n" +
    "\n" +
    "    <div class=\"pull-right\">\n" +
    "      <a href ng-click=\"toggleMissing(false)\" ng-if=\"showMissing\" translate>coverage-hide-missing</a>\n" +
    "      <a href ng-click=\"toggleMissing(true)\" ng-if=\"!showMissing\" translate>coverage-show-missing</a>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"clearfix\"></div>\n" +
    "\n" +
    "    <table class=\"table table-bordered table-striped\">\n" +
    "      <thead>\n" +
    "      <tr>\n" +
    "        <th rowspan=\"2\" translate>study.label</th>\n" +
    "        <th ng-repeat=\"header in table.vocabularyHeaders\" title=\"{{header.vocabulary.descriptions[0].value}}\"\n" +
    "          colspan=\"{{header.termsCount}}\">\n" +
    "          {{header.vocabulary.titles[0].value}}\n" +
    "        </th>\n" +
    "        <th rowspan=\"2\" translate>all</th>\n" +
    "      </tr>\n" +
    "      <tr>\n" +
    "        <th ng-repeat=\"header in table.termHeaders\" title=\"{{header.term.descriptions[0].value}}\">\n" +
    "          {{header.term.titles[0].value}}\n" +
    "        </th>\n" +
    "      </tr>\n" +
    "      </thead>\n" +
    "      <tbody>\n" +
    "\n" +
    "      <tr ng-repeat=\"row in table.rows.studyIds\" ng-if=\"showMissing || table.termHeaders.length == keys(row.hits).length\">\n" +
    "        <td>\n" +
    "          <a href title=\"{{row.description}}\">{{row.title}}</a>\n" +
    "          <a href ng-if=\"false\" class=\"pull-right\"><i class=\"fa fa-plus-square\"></i></a>\n" +
    "        </td>\n" +
    "        <td ng-repeat=\"h in table.termHeaders\">\n" +
    "          <span class=\"label label-info\" ng-if=\"row.hits[$index + 1]\">{{row.hits[$index + 1]}}</span>\n" +
    "          <span ng-if=\"!row.hits[$index + 1]\">0</span>\n" +
    "        </td>\n" +
    "        <th>\n" +
    "          <a href>{{row.totalHits}}</a>\n" +
    "        </th>\n" +
    "      </tr>\n" +
    "\n" +
    "      </tbody>\n" +
    "      <tfoot>\n" +
    "      <tr>\n" +
    "        <th translate>all</th>\n" +
    "        <th ng-repeat=\"hit in table.footers.total\">\n" +
    "          <a href>{{hit}}</a>\n" +
    "        </th>\n" +
    "        <th>\n" +
    "          <a href>{{table.totalHits}}</a>\n" +
    "        </th>\n" +
    "      </tr>\n" +
    "      </tfoot>\n" +
    "    </table>\n" +
    "  </div>\n" +
    "\n" +
    "      <!--<pre>-->\n" +
    "<!--{{table | json}}-->\n" +
    "      <!--</pre>-->\n" +
    "</div>");
}]);

angular.module("search/views/criteria/criteria-node-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criteria-node-template.html",
    "<span>\n" +
    "  <span ng-if=\"item.children.length > 0\">\n" +
    "    <criteria-leaf item=\"item.children[0]\" parent-type=\"$parent.item.type\"></criteria-leaf>\n" +
    "\n" +
    "    <div class=\"btn-group\" uib-dropdown is-open=\"status.isopen\">\n" +
    "      <button id=\"single-button\" type=\"button\" class=\"btn btn-default btn-xs\" uib-dropdown-toggle ng-disabled=\"disabled\">\n" +
    "        {{item.type | translate}} <span class=\"caret\"></span>\n" +
    "      </button>\n" +
    "      <ul uib-dropdown-menu role=\"menu\" aria-labelledby=\"single-button\">\n" +
    "        <li role=\"menuitem\" ng-if=\"item.type !== 'or'\"><a href ng-click=\"updateLogical('or')\" translate>or</a></li>\n" +
    "        <li role=\"menuitem\" ng-if=\"item.type !== 'and'\"><a href ng-click=\"updateLogical('and')\" translate>and</a></li>\n" +
    "        <li role=\"menuitem\" ng-if=\"item.type !== 'nand'\"><a href ng-click=\"updateLogical('nand')\" translate>nand</a></li>\n" +
    "      </ul>\n" +
    "    </div>\n" +
    "    <criteria-leaf item=\"item.children[1]\" parent-type=\"$parent.item.type\"></criteria-leaf>\n" +
    "\n" +
    "  </span>\n" +
    "  <span ng-if=\"item.children.length === 0\">\n" +
    "    <criteria-leaf item=\"item\" parent-type=\"item.parent.type\"></criteria-leaf>\n" +
    "  </span>\n" +
    "</span>");
}]);

angular.module("search/views/criteria/criterion-dropdown-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criterion-dropdown-template.html",
    "<span id=\"{{criterion.vocabulary.name}}-dropdown\" class='btn-group btn-info' ng-class='{open: state.open}'>\n" +
    "  <button class=\"btn btn-info btn-xs dropdown\"\n" +
    "          id=\"{{criterion.vocabulary.name}}-button\"\n" +
    "          ng-click=\"openDropdown()\"\n" +
    "          title=\"{{localize(criterion.vocabulary.title)}}\">\n" +
    "    {{truncate(localize(criterion.vocabulary.title))}}\n" +
    "    <span class='fa fa-caret-down'></span>\n" +
    "  </button>\n" +
    "  <button class='btn btn-xs btn-danger' ng-click='remove(criterion.id)'>\n" +
    "    <span class='fa fa-times'></span>\n" +
    "  </button>\n" +
    "\n" +
    "\n" +
    "  <ul class=\"dropdown-menu query-dropdown-menu\" aria-labelledby=\"{{criterion.vocabulary.name}}-button\">\n" +
    "    <li class=\"btn-group\" >\n" +
    "      <ul class=\"criteria-radio-list\">\n" +
    "        <li>\n" +
    "          <label>\n" +
    "            <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.EXISTS}}\">\n" +
    "            {{'any' | translate}}\n" +
    "          </label>\n" +
    "        </li>\n" +
    "        <li>\n" +
    "          <label>\n" +
    "            <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.MISSING}}\">\n" +
    "            {{'none' | translate}}\n" +
    "          </label>\n" +
    "        </li>\n" +
    "        <li>\n" +
    "          <label>\n" +
    "            <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.IN}}\">\n" +
    "            {{'in' | translate}}\n" +
    "          </label>\n" +
    "        </li>\n" +
    "      </ul>\n" +
    "    </li>\n" +
    "    <li ng-show=\"isInFilter()\" class='divider'></li>\n" +
    "    <li ng-show=\"isInFilter()\"  ng-repeat='term in criterion.vocabulary.terms'>\n" +
    "      <a href ng-click='toggleSelection($event, term)' title=\"{{localize(term.title)}}\">\n" +
    "        {{truncate(localize(term.title))}} <span ng-show=\"isSelected(term.name)\"\n" +
    "                                                 class=\"fa fa-check pull-right\"></span>\n" +
    "      </a>\n" +
    "    </li>\n" +
    "  </ul>\n" +
    "</span>\n" +
    "\n" +
    "");
}]);

angular.module("search/views/criteria/target-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/target-template.html",
    "<span></span>");
}]);

angular.module("search/views/graphics/graphics-search-result-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/graphics/graphics-search-result-template.html",
    "<div>\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "<div class=\"row\">\n" +
    "  <div ng-repeat=\"chart in chartObjects \">\n" +
    "    <div class=\"col-md-6\" google-chart chart=\"chart.chartObject\">\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>\n" +
    "</div>");
}]);

angular.module("search/views/list/datasets-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/list/datasets-search-result-table-template.html",
    "<div>\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "\n" +
    "  <div ng-show=\"summaries.length > 0\">\n" +
    "    <div class=\"row voffset2\">\n" +
    "      <div class=\"col-xs-4\">\n" +
    "      </div>\n" +
    "      <div class=\"col-xs-8\">\n" +
    "        <dir-pagination-controls class=\"pull-right\"></dir-pagination-controls>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"table-responsive\">\n" +
    "      <table class=\"table table-bordered table-striped\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th translate>acronym</th>\n" +
    "          <th translate>name</th>\n" +
    "          <th translate>type</th>\n" +
    "          <th translate>networks</th>\n" +
    "          <th translate>studies</th>\n" +
    "          <th translate>variables</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "\n" +
    "        <tr ng-repeat=\"summary in summaries\">\n" +
    "          <td>\n" +
    "            <a href>\n" +
    "              <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.type\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.datasetCountStats'].networks}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.datasetCountStats'].studies}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.datasetCountStats'].variables}}\n" +
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
    "\n" +
    "  <div ng-show=\"summaries.length > 0\">\n" +
    "\n" +
    "    <div class=\"row voffset2\">\n" +
    "      <div class=\"col-xs-4\">\n" +
    "      </div>\n" +
    "      <div class=\"col-xs-8\">\n" +
    "        <dir-pagination-controls class=\"pull-right\"></dir-pagination-controls>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"table-responsive\">\n" +
    "      <table class=\"table table-bordered table-striped\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th translate>acronym</th>\n" +
    "          <th translate>name</th>\n" +
    "          <th translate>studies</th>\n" +
    "          <th translate colspan=\"2\">datasets</th>\n" +
    "          <th translate>variables</th>\n" +
    "        </tr>\n" +
    "        <tr>\n" +
    "          <th></th>\n" +
    "          <th></th>\n" +
    "          <th></th>\n" +
    "          <th translate>study</th>\n" +
    "          <th translate>harmonization</th>\n" +
    "          <th></th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "\n" +
    "        <tr ng-repeat=\"summary in summaries\">\n" +
    "          <td>\n" +
    "            <a href>\n" +
    "              <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.networkCountStats'].studies}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.networkCountStats'].studyDatasets}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.networkCountStats'].harmonizationDatasets}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.networkCountStats'].variables}}\n" +
    "          </td>\n" +
    "        </tr>\n" +
    "        </tbody>\n" +
    "      </table>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/list/studies-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/list/studies-search-result-table-template.html",
    "<div>\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "\n" +
    "  <div ng-show=\"summaries.length > 0\">\n" +
    "\n" +
    "    <div class=\"row voffset2\">\n" +
    "      <div class=\"col-xs-4\">\n" +
    "      </div>\n" +
    "      <div class=\"col-xs-8\">\n" +
    "        <dir-pagination-controls class=\"pull-right\"></dir-pagination-controls>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"table-responsive\">\n" +
    "      <table class=\"table table-bordered table-striped\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th translate>acronym</th>\n" +
    "          <th translate>name</th>\n" +
    "          <th translate>design</th>\n" +
    "          <th translate>participants</th>\n" +
    "          <th translate>networks</th>\n" +
    "          <th translate colspan=\"2\">datasets</th>\n" +
    "          <th translate>variables</th>\n" +
    "        </tr>\n" +
    "        <tr>\n" +
    "          <th></th>\n" +
    "          <th></th>\n" +
    "          <th></th>\n" +
    "          <th></th>\n" +
    "          <th></th>\n" +
    "          <th translate>study</th>\n" +
    "          <th translate>harmonization</th>\n" +
    "          <th></th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "\n" +
    "        <tr ng-repeat=\"summary in summaries\">\n" +
    "          <td>\n" +
    "            <a href>\n" +
    "              <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary.designs.join(', ')}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary.targetNumber.number}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.studyCountStats'].networks}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.studyCountStats'].studyDatasets}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.studyCountStats'].harmonizationDatasets}}\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            {{summary['obiba.mica.CountStatsDto.studyCountStats'].variables}}\n" +
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
    "\n" +
    "  <div ng-show=\"summaries.length > 0\">\n" +
    "    <div class=\"row voffset2\">\n" +
    "      <div class=\"col-xs-4\">\n" +
    "      </div>\n" +
    "      <div class=\"col-xs-8\">\n" +
    "        <dir-pagination-controls class=\"pull-right\"></dir-pagination-controls>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div class=\"table-responsive\">\n" +
    "      <table class=\"table table-bordered table-striped\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th translate>name</th>\n" +
    "          <th translate>label</th>\n" +
    "          <th translate>study</th>\n" +
    "          <th translate>dataset</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "\n" +
    "        <tr ng-repeat=\"summary in summaries\">\n" +
    "          <td>\n" +
    "            <a href>\n" +
    "              {{summary.name}}\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.variableLabel\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.studyAcronym\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <a href>\n" +
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

angular.module("search/views/search-result-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-panel-template.html",
    "<div>\n" +
    "\n" +
    "  <uib-tabset class=\"voffset2\" type=\"tabs\">\n" +
    "\n" +
    "    <uib-tab heading=\"{{'list' | translate}}\" active=\"activeDisplay.list\" ng-click=\"selectDisplay(DISPLAY_TYPES.LIST)\">\n" +
    "\n" +
    "      <uib-tabset class=\"voffset2\" type=\"pills\">\n" +
    "\n" +
    "        <!-- Variables -->\n" +
    "        <uib-tab ng-show=\"settingsDisplay.variables.showSearchTab\" active=\"activeTarget.variables\" ng-click=\"selectTarget(QUERY_TYPES.VARIABLES)\"\n" +
    "          heading=\"{{'variables' | translate}} ({{result.list.variableResultDto.totalHits}})\">\n" +
    "          <variables-result-table  loading=\"loading\"\n" +
    "            summaries=\"result.list.variableResultDto['obiba.mica.DatasetVariableResultDto.result'].summaries\"></variables-result-table>\n" +
    "        </uib-tab>\n" +
    "\n" +
    "        <!-- Datasets -->\n" +
    "        <uib-tab ng-show=\"settingsDisplay.datasets.showSearchTab\" active=\"activeTarget.datasets\" ng-click=\"selectTarget(QUERY_TYPES.DATASETS)\"\n" +
    "          heading=\"{{'datasets' | translate}} ({{result.list.datasetResultDto.totalHits}})\">\n" +
    "          <datasets-result-table  loading=\"loading\"\n" +
    "            summaries=\"result.list.datasetResultDto['obiba.mica.DatasetResultDto.result'].datasets\"></datasets-result-table>\n" +
    "        </uib-tab>\n" +
    "\n" +
    "        <!-- Studies -->\n" +
    "        <uib-tab ng-show=\"settingsDisplay.studies.showSearchTab\" active=\"activeTarget.studies\" ng-click=\"selectTarget(QUERY_TYPES.STUDIES)\"\n" +
    "          heading=\"{{'studies' | translate}} ({{result.list.studyResultDto.totalHits}})\">\n" +
    "          <studies-result-table  loading=\"loading\"\n" +
    "            summaries=\"result.list.studyResultDto['obiba.mica.StudyResultDto.result'].summaries\"></studies-result-table>\n" +
    "        </uib-tab>\n" +
    "\n" +
    "        <!-- Networks -->\n" +
    "        <uib-tab ng-show=\"settingsDisplay.networks.showSearchTab\" active=\"activeTarget.networks\" ng-click=\"selectTarget(QUERY_TYPES.NETWORKS)\"\n" +
    "          heading=\"{{'networks' | translate}} ({{result.list.networkResultDto.totalHits}})\">\n" +
    "          <networks-result-table  loading=\"loading\"\n" +
    "            summaries=\"result.list.networkResultDto['obiba.mica.NetworkResultDto.result'].networks\"></networks-result-table>\n" +
    "        </uib-tab>\n" +
    "      </uib-tabset>\n" +
    "\n" +
    "    </uib-tab>\n" +
    "\n" +
    "    <uib-tab heading=\"{{'coverage' | translate}}\" active=\"activeDisplay.coverage\"\n" +
    "      ng-click=\"selectDisplay(DISPLAY_TYPES.COVERAGE)\">\n" +
    "      <coverage-result-table result=\"result.coverage\" loading=\"loading\" class=\"voffset2\"></coverage-result-table>\n" +
    "\n" +
    "    </uib-tab>\n" +
    "\n" +
    "    <uib-tab heading=\"{{'graphics' | translate}}\" active=\"activeDisplay.graphics\"\n" +
    "      ng-click=\"selectDisplay(DISPLAY_TYPES.GRAPHICS)\">\n" +
    "      <graphics-result result=\"result.graphics\" loading=\"loading\" class=\"voffset2\"></graphics-result>\n" +
    "    </uib-tab>\n" +
    "\n" +
    "  </uib-tabset>\n" +
    "\n" +
    "</div>");
}]);

angular.module("search/views/search.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search.html",
    "<div>\n" +
    "  <!--<h2 translate>search</h2>-->\n" +
    "  <div ng-if=\"headerTemplateUrl\" ng-include=\"headerTemplateUrl\"></div>\n" +
    "\n" +
    "  <obiba-alert id=\"SearchController\"></obiba-alert>\n" +
    "\n" +
    "  <!-- Classifications region -->\n" +
    "  <div>\n" +
    "    <!--<div>-->\n" +
    "    <div class=\"row\">\n" +
    "      <div class=\"col-xs-3\"></div>\n" +
    "      <div class=\"col-xs-6\">\n" +
    "        <script type=\"text/ng-template\" id=\"customTemplate.html\">\n" +
    "          <a ng-if=\"match.model.id\">\n" +
    "            <span title=\"{{match.model.target + '-classifications' | translate}}\">\n" +
    "              <i class=\"{{'i-obiba-' + match.model.target}}\"></i>\n" +
    "            </span>\n" +
    "            <span title=\"{{match.model.itemDescription}}\">{{match.model.itemTitle}}</span>\n" +
    "            <small class=\"help-block no-margin hoffset3\" title=\"{{match.model.itemParentDescription}}\">\n" +
    "              {{match.model.itemParentTitle}}\n" +
    "            </small>\n" +
    "          </a>\n" +
    "          <a ng-if=\"!match.model.id\" class=\"{{match.model.status}}\">\n" +
    "            <small class=\"help-block no-margin\">\n" +
    "              {{match.model.message}}\n" +
    "            </small>\n" +
    "          </a>\n" +
    "        </script>\n" +
    "        <a href>\n" +
    "        <span class=\"input-group input-group-sm no-padding-top\">\n" +
    "          <input type=\"text\" ng-model=\"selectedCriteria\" placeholder=\"{{'search.placeholder' | translate}}\"\n" +
    "            uib-typeahead=\"criteria for criteria in searchCriteria($viewValue)\"\n" +
    "            typeahead-min-length=\"2\"\n" +
    "            typeahead-loading=\"documents.search.active\"\n" +
    "            typeahead-template-url=\"customTemplate.html\"\n" +
    "            typeahead-on-select=\"selectCriteria($item)\"\n" +
    "            class=\"form-control\">\n" +
    "          <span class=\"input-group-addon\"><i class=\"glyphicon glyphicon-search\"></i></span>\n" +
    "        </span>\n" +
    "        </a>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div class=\"row\">\n" +
    "      <div class=\"col-xs-3\"></div>\n" +
    "      <div class=\"col-xs-6\">\n" +
    "        <ul class=\"nav nav-pills\">\n" +
    "          <li ng-show=\"settingsDisplay.variables.showSearchTab\" ng-class=\"{'active': taxonomies.target === 'variable' && taxonomiesShown}\"\n" +
    "            title=\"{{'variable-classifications' | translate}}\">\n" +
    "            <a ng-click=\"selectTaxonomyTarget('variable')\" translate>variables</a>\n" +
    "          </li>\n" +
    "          <li ng-show=\"settingsDisplay.datasets.showSearchTab\" ng-class=\"{'active': taxonomies.target === 'dataset' && taxonomiesShown}\"\n" +
    "            title=\"{{'dataset-classifications' | translate}}\">\n" +
    "            <a ng-click=\"selectTaxonomyTarget('dataset')\" translate>datasets</a>\n" +
    "          </li>\n" +
    "          <li ng-show=\"settingsDisplay.studies.showSearchTab\" ng-class=\"{'active': taxonomies.target === 'study' && taxonomiesShown}\"\n" +
    "            title=\"{{'study-classifications' | translate}}\">\n" +
    "            <a ng-click=\"selectTaxonomyTarget('study')\" translate>studies</a>\n" +
    "          </li>\n" +
    "          <li ng-show=\"settingsDisplay.networks.showSearchTab\" ng-class=\"{'active': taxonomies.target === 'network' && taxonomiesShown}\"\n" +
    "            title=\"{{'network-classifications' | translate}}\">\n" +
    "            <a ng-click=\"selectTaxonomyTarget('network')\" translate>networks</a>\n" +
    "          </li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div id=\"taxonomies\" class=\"collapse\">\n" +
    "      <div ng-include=\"'search/views/classifications/taxonomies-view.html'\"></div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <!-- Search criteria region -->\n" +
    "  <div class=\"voffset3\">\n" +
    "    <div class=\"row\">\n" +
    "      <div class=\"col-xs-12\">\n" +
    "        <div criteria-root item=\"search.criteria\" on-remove=\"removeCriteriaItem\" on-select=\"updateTerm\" on-refresh=\"refreshQuery\"></div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <!-- Results region -->\n" +
    "  <div class=\"voffset3\">\n" +
    "    <result-panel display=\"search.display\" type=\"search.type\" result=\"search.result\" loading=\"search.loading\" on-type-changed=\"onTypeChanged\"></result-panel>\n" +
    "  </div>\n" +
    "</div>");
}]);
