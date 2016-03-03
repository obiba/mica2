/*!
 * ng-obiba-mica - v1.0.0
 * https://github.com/obiba/ng-obiba-mica

 * License: GNU Public License version 3
 * Date: 2016-03-03
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
    'DatasetPage': '#/:type/:dataset'
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
    })

  .service('GraphicChartsConfigurations', function(){

    this.getClientConfig = function(){
      return true;
    };

    this.setClientConfig = function(){
      return true;
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
        list: {header: null, footer: null},
        view: {header: null, footer: null},
        form: {header: null, footer: null}
      }
    ));
  }])
  .config(['$provide', '$injector', function ($provide) {
    $provide.provider('ngObibaMicaSearch', function () {
      var localeResolver = ['LocalizedValues', function (LocalizedValues) {
        return LocalizedValues.getLocal();
      }], options = {
        taxonomyTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
        searchTabsOrder: [DISPLAY_TYPES.LIST, DISPLAY_TYPES.COVERAGE, DISPLAY_TYPES.GRAPHICS],
        resultTabsOrder: [QUERY_TARGETS.VARIABLE, QUERY_TARGETS.DATASET, QUERY_TARGETS.STUDY, QUERY_TARGETS.NETWORK],
        listLabel: 'search.list',
        coverageLabel: 'search.coverage',
        graphicsLabel: 'search.graphics',
        variables: {
          showSearchTab: true,
          searchLabel: 'search.variable.searchLabel',
          noResultsLabel: 'search.variable.noResults',
          variablesColumn: {
            showVariablesStudiesColumn: true,
            showVariablesDatasetsColumn: true,
            showDatasetsStudiesColumn: true,
            showDatasetsVariablesColumn: true
          }
        },
        datasets: {
          showSearchTab: true,
          showDatasetsSearchFilter: true,
          searchLabel: 'search.variable.searchLabel',
          noResultsLabel: 'search.dataset.noResults',
          datasetsColumn: {
            showDatasetsTypeColumn: true,
            showDatasetsNetworkColumn: true,
            showDatasetsStudiesColumn: true,
            showDatasetsVariablesColumn: true
          }
        },
        studies: {
          showSearchTab: true,
          searchLabel: 'search.variable.searchLabel',
          noResultsLabel: 'search.study.noResults',
          showStudiesSearchFilter: true, studiesColumn: {
            showStudiesDesignColumn: true,
            showStudiesQuestionnaireColumn: true,
            showStudiesPmColumn: true,
            showStudiesBioColumn: true,
            showStudiesOtherColumn: true,
            showStudiesParticipantsColumn: true,
            showStudiesNetworksColumn: true,
            showStudiesDatasetsColumn: true,
            showStudiesHarmonizedDatasetsColumn: true,
            showStudiesVariablesColumn: true
          }
        },
        networks: {
          showSearchTab: true,
          searchLabel: 'search.variable.searchLabel',
          noResultsLabel: 'search.network.noResults',
          networksColumn: {
            showNetworksStudiesColumn: true,
            showNetworksStudyDatasetColumn: true,
            showNetworksHarmonizedDatasetColumn: true,
            showNetworksVariablesColumn: true
          }
        }
      };

      this.setLocaleResolver = function(resolver) {
        localeResolver = resolver;
      };

      this.setOptions = function (value) {
        options = angular.merge(options, value);
        //NOTICE: angular.merge merges arrays by position. Overwriting manually.
        options.taxonomyTabsOrder = value.taxonomyTabsOrder || options.taxonomyTabsOrder;
        options.searchTabsOrder = value.searchTabsOrder || options.searchTabsOrder;
        options.resultTabsOrder = value.resultTabsOrder || options.resultTabsOrder;
      };

      this.$get = ['$q', '$injector', function ngObibaMicaSearchFactory($q, $injector) {
        return {
          getLocale: function(success, error) {
            return $q.when($injector.invoke(localeResolver), success, error);
          },
          getOptions: function() {
            return options;
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
    return function(elements, regex, fields) {
      var out = [];

      try {
        var pattern = new RegExp(regex, 'i');
        out = elements.filter(function(element) {
          return fields.some(function(field){
            return pattern.test(element[field]);
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
  generate: function(taxonomy, vocabulary, term) {
    return taxonomy && vocabulary ?
      taxonomy.name + '.' + vocabulary.name + (term ? '.' + term.name : '') :
      undefined;
  }
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

  this.leafItemMap[item.id] = item;

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

    this.vocabularyTermNames = function(vocabulary) {
      return vocabulary && vocabulary.terms ? vocabulary.terms.map(function(term) {
        return term.name;
      }) : [];
    };

    this.hasTargetQuery = function(rootRql) {
      return rootRql.args.filter(function(query) {
          switch (query.name) {
            case RQL_NODE.VARIABLE:
            case RQL_NODE.DATASET:
            case RQL_NODE.STUDY:
            case RQL_NODE.NETWORK:
              return true;
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

    this.matchQuery = function(field, queryString) {
      var query = new RqlQuery(RQL_NODE.MATCH);
      query.args.push(queryString || '*');
      query.args.push(field);
      return query;
    };

    this.updateMatchQuery = function(query, queryString) {
      query.args[0] = queryString || '*';
      return query;
    };

    this.rangeQuery = function (field, from, to) {
      var query = new RqlQuery(RQL_NODE.BETWEEN);
      query.args.push(field);
      self.updateRangeQuery(query, from, to);
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

    this.mergeInQueryArgValues = function (query, terms) {
      var hasValues = terms && terms.length > 0;
      query.name = hasValues ? RQL_NODE.IN : RQL_NODE.EXISTS;

      if (hasValues) {
        var current = query.args[1];
        if (!current) {
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
          var orQuery = new RqlQuery(logicalOp || RQL_NODE.OR);
          orQuery.args.push(oldArg, query);
          parentQuery.args.push(orQuery);
        }
      }

      return parentQuery;
    };

    this.updateQueryArgValues = function(query, terms) {
      switch (query.name) {
        case RQL_NODE.IN:
        case RQL_NODE.EXISTS:
          this.mergeInQueryArgValues(query, terms);
          break;
      }

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

    this.isTermsVocabulary = function(vocabulary) {
      return self.vocabularyType(vocabulary) === VOCABULARY_TYPES.STRING && vocabulary.terms;
    };

    this.isMatchVocabulary = function(vocabulary) {
      return self.vocabularyType(vocabulary) === VOCABULARY_TYPES.STRING && !vocabulary.terms;
    };

    this.isNumericVocabulary = function(vocabulary) {
      return !vocabulary.terms && (self.vocabularyType(vocabulary) === VOCABULARY_TYPES.INTEGER || self.vocabularyType(vocabulary) === VOCABULARY_TYPES.DECIMAL);
    };

    this.isRangeVocabulary = function(vocabulary) {
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
        return query.args.filter(function(arg){
          return arg.name === target;
        }).pop();
      }

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
            parentQuery.args.splice.apply(parentQuery.args, [index, 0].concat(children));
          } else {
            parentQuery.args.splice(index, 0, children);
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
            parentQuery.args.splice.apply(parentQuery.args, [index, 0].concat(children));
          } else {
            parentQuery.args.splice(index, 0, children);
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
          }).$promise.then(function(taxonomy) {
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

        var rqlQuery = RqlQueryUtils.buildRqlQuery(newItem);
        return RqlQueryUtils.addQuery(target, rqlQuery, logicalOp);
      };

      /**
       * Update an exising item to the item tree
       *
       * @param rootItem
       * @param item
       */
      this.updateCriteriaItem = function (existingItem, newItem) {
        RqlQueryUtils.updateQueryArgValues(
          existingItem.rqlQuery,
          newItem.term ? [newItem.term.name] : RqlQueryUtils.vocabularyTermNames(newItem.vocabulary));
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
        var parsedQuery = new RqlParser().parse(query);
        var targetQuery = parsedQuery.args.filter(function (node) {
          return node.name === item.target;
        }).pop();

        if (targetQuery) {
          targetQuery.args.push(RqlQueryUtils.aggregate([RqlQueryUtils.criteriaId(item.taxonomy, item.vocabulary)]));
          targetQuery.args.push(RqlQueryUtils.limit(0, 0));
        }

        parsedQuery.args.push(new RqlQuery(RQL_NODE.FACET));

        if (lang) {
          RqlQueryUtils.addLocaleQuery(parsedQuery, lang);
        }

        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      this.prepareSearchQuery = function(type, query, pagination, lang, sort) {
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

        if(sort) {
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

        if(variable.args.length>0 && variable.args[0].name !== 'limit') {
          var variableType = new RqlQuery('in');
          variableType.args.push('Mica_variable.variableType');
          if(bucketArg === BUCKET_TYPES.NETWORK || bucketArg === BUCKET_TYPES.DATASCHEMA) {
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
            var keys = aggs && aggs.map(function(agg){
              return agg.key;
            }) || [];

            if (aggs) {
              // Add the missing terms not present in the aggs list
              var missingTerms = [];

              terms.forEach(function(term) {
                if (keys.length === 0 || keys.indexOf(term.name) === -1) {
                  missingTerms.push({count: 0,
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
            return terms.map(function(term) {
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
            var child = parentAgg.children.filter(function(child){
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
                  addMissingTerms(filteredAgg['obiba.mica.RangeAggregationResultDto.ranges'],criterion.vocabulary) :
                  addMissingTerms(filteredAgg['obiba.mica.TermsAggregationResultDto.terms'],criterion.vocabulary);
              }
            } else {
              var vocabularyAgg = filteredAgg.children.filter(function (agg) {
                return agg.aggregation === alias;
              }).pop();

              if (vocabularyAgg) {
                return RqlQueryUtils.isRangeVocabulary(criterion.vocabulary) ?
                  addMissingTerms(getChildAggragations(filteredAgg, 'obiba.mica.RangeAggregationResultDto.ranges'),criterion.vocabulary):
                  addMissingTerms(getChildAggragations(filteredAgg, 'obiba.mica.TermsAggregationResultDto.terms'),criterion.vocabulary);
              }
            }
          }
        }

        return addMissingTerms([], criterion.vocabulary);
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

    this.VariablePage = function(id) {
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
/* global BUCKET_TYPES */
/* global RQL_NODE */
/* global DISPLAY_TYPES */
/* global CriteriaIdGenerator */
/* global targetToType */

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
    'ngObibaMicaSearch',
    'JoinQuerySearchResource',
    'JoinQueryCoverageResource',
    'AlertService',
    'ServerErrorUtils',
    'LocalizedValues',
    'RqlQueryService',
    'RqlQueryUtils',
    'SearchContext',
    function ($scope,
              $timeout,
              $routeParams,
              $location,
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
              SearchContext) {
      $scope.options = ngObibaMicaSearch.getOptions();
      $scope.taxonomyTypeMap = { //backwards compatibility for pluralized naming in configs.
        variable: 'variables',
        study: 'studies',
        network: 'networks',
        dataset: 'datasets'
      };
      var taxonomyTypeInverseMap = Object.keys($scope.taxonomyTypeMap).reduce(function (prev, k) {
        prev[$scope.taxonomyTypeMap[k]] = k;
        return prev;
      }, {});

      $scope.lang = LocalizedValues.getLocal();
      $scope.metaTaxonomy = TaxonomyResource.get({
        target: 'taxonomy',
        taxonomy: 'Mica_taxonomy'
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
              .filter(function (t) { return t; })
              .map(function (t) { return t.trim(); });
        }

        var taxonomyTabsOrderParam = getTabsOrderParam('taxonomyTabsOrder');
        $scope.taxonomyTabsOrder = (taxonomyTabsOrderParam || $scope.options.taxonomyTabsOrder).filter(function (t) {
          return searchTaxonomyDisplay[t];
        });

        var searchTabsOrderParam = getTabsOrderParam('searchTabsOrder');
        $scope.searchTabsOrder = searchTabsOrderParam || $scope.options.searchTabsOrder;

        var resultTabsOrderParam = getTabsOrderParam('resultTabsOrder');
        $scope.resultTabsOrder = (resultTabsOrderParam || $scope.options.resultTabsOrder).filter(function (t) {
          return searchTaxonomyDisplay[t];
        });

        $scope.metaTaxonomy.$promise.then(function(metaTaxonomy){
          $scope.taxonomyTabsOrder.forEach(function(target) {
            var targetVocabulary = metaTaxonomy.vocabularies.filter(function(vocabulary){
              return vocabulary.name === target;
            }).pop();
            if(targetVocabulary && targetVocabulary.terms) {
              targetVocabulary.terms.forEach(function(term) {
                term.target = target;
                var title = term.title.filter(function (t) { return t.locale === $scope.lang; })[0];
                var description = term.description ? term.description.filter(function (t) { return t.locale === $scope.lang; })[0] : undefined;
                term.locale = {
                  title: title,
                  description: description
                };
                if(term.terms) {
                  term.terms.forEach(function(trm) {
                    var title = trm.title.filter(function (t) { return t.locale === $scope.lang; })[0];
                    var description = trm.description ? trm.description.filter(function (t) { return t.locale === $scope.lang; })[0] : undefined;
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

      initSearchTabs();

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

      function validateBucket(bucket) {
        if (!bucket || !BUCKET_TYPES[bucket.toUpperCase()]) {
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

      function getDefaultBucketType() {
        // TODO settings
        return BUCKET_TYPES.STUDY;
      }

      function getDefaultDisplayType() {
        return $scope.searchTabsOrder[0] || DISPLAY_TYPES.LIST;
      }

      function validateQueryData() {
        try {
          var search = $location.search();
          var type = $scope.resultTabsOrder.indexOf(taxonomyTypeInverseMap[search.type]) > -1 ? search.type : getDefaultQueryType();
          var bucket = search.bucket || getDefaultBucketType();
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

      function executeSearchQuery() {
        if (validateQueryData()) {
          // build the criteria UI
          RqlQueryService.createCriteria($scope.search.rqlQuery, $scope.lang).then(function (result) {
            // criteria UI is updated here
            $scope.search.criteria = result.root;
            if ($scope.search.criteria && $scope.search.criteria.children) {
              $scope.search.criteria.children.sort(function (a, b) {
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
            $scope.search.criteriaItemMap = result.map;
          });

          var localizedQuery =
            RqlQueryService.prepareSearchQuery(
              $scope.search.type,
              $scope.search.rqlQuery,
              $scope.search.pagination,
              $scope.lang,
              $scope.search.type === 'variables' ? 'name' : 'acronym.' + $scope.lang
            );

          $scope.search.loading = true;
          switch ($scope.search.display) {
            case DISPLAY_TYPES.LIST:
              $scope.search.executedQuery = localizedQuery;
              JoinQuerySearchResource[$scope.search.type]({query: localizedQuery},
                function onSuccess(response) {
                  $scope.search.result.list = response;
                  $scope.search.loading = false;
                },
                onError);
              break;
            case DISPLAY_TYPES.COVERAGE:
              $scope.search.executedQuery = RqlQueryService.prepareCoverageQuery(localizedQuery, $scope.search.bucket);
              JoinQueryCoverageResource.get({query: $scope.search.executedQuery},
                function onSuccess(response) {
                  $scope.search.result.coverage = response;
                  $scope.search.loading = false;
                },
                onError);
              break;
            case DISPLAY_TYPES.GRAPHICS:
              $scope.search.executedQuery = RqlQueryService.prepareGraphicsQuery(localizedQuery,
                ['methods.designs', 'populations.selectionCriteria.countriesIso', 'populations.dataCollectionEvents.bioSamples', 'numberOfParticipants.participant.number']);
              JoinQuerySearchResource.studies({query: $scope.search.executedQuery},
                function onSuccess(response) {
                  $scope.search.result.graphics = response;
                  $scope.search.loading = false;
                },
                onError);
              break;
          }
        }
      }

      ngObibaMicaSearch.getLocale(function (locales) {
        if (angular.isArray(locales)) {
          $scope.tabs = locales;
          $scope.setLocale(locales[0]);
        } else {
          $scope.setLocale(locales || $scope.lang);
        }
      });

      $scope.setLocale = function (locale) {
        $scope.lang = locale;
        SearchContext.setLocale($scope.lang);
        executeSearchQuery();
      };

      var closeTaxonomies = function () {
        angular.element('#taxonomies').collapse('hide');
      };

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
            var title = v.title.filter(function (t) { return t.locale === $scope.lang; })[0];
            var description = v.description ? v.description.filter(function (t) { return t.locale === $scope.lang; })[0] : undefined;

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
          }, function onSuccess(taxonomies) {
            $scope.taxonomies.all = taxonomies;
            groupTaxonomies(taxonomies, $scope.taxonomies.target);
            $scope.taxonomies.search.active = false;
          });
        }
      };

      var showTaxonomy = function(target, name) {
        if (!$scope.taxonomiesShown) {
          angular.element('#taxonomies').collapse('show');
        }
        $scope.taxonomies.target = target;
        $scope.taxonomies.search.active = false;
        $scope.taxonomies.all = null;
        $scope.taxonomies.taxonomy = null;
        $scope.taxonomies.vocabulary = null;
        $scope.taxonomies.term = null;
        TaxonomyResource.get({
          target: target,
          taxonomy: name
        }, function onSuccess(response) {
          $scope.taxonomies.taxonomy = response;
          $scope.taxonomies.search.active = false;
        });
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
          query: query, locale: $scope.lang
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
      var selectCriteria = function (item, logicalOp) {
        if (item.id) {
          var id = CriteriaIdGenerator.generate(item.taxonomy, item.vocabulary);
          var existingItem = $scope.search.criteriaItemMap[id];
          if (existingItem) {
            RqlQueryService.updateCriteriaItem(existingItem, item);

          } else {
            RqlQueryService.addCriteriaItem($scope.search.rqlQuery, item, logicalOp);
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

      var navigateTaxonomy = function (taxonomy, vocabulary, term) {
        var toFilter = ($scope.taxonomies.taxonomy && !taxonomy) || ($scope.taxonomies.vocabulary && !vocabulary);
        $scope.taxonomies.taxonomy = taxonomy;
        $scope.taxonomies.vocabulary = vocabulary;
        $scope.taxonomies.term = term;

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
        if (vocabulary && RqlQueryUtils.isNumericVocabulary(vocabulary)) {
          selectCriteria(RqlQueryService.createCriteriaItem($scope.taxonomies.target, taxonomy, vocabulary, null, $scope.lang));
          return;
        }

        selectCriteria(RqlQueryService.createCriteriaItem(target, taxonomy, vocabulary, term, $scope.lang));
      };

      var onTypeChanged = function (type) {
        if (type) {
          validateType(type);
          var search = $location.search();
          search.type = type;
          $location.search(search).replace();
        }
      };

      var onBucketChanged = function (bucket) {
        if (bucket) {
          validateBucket(bucket);
          var search = $location.search();
          search.bucket = bucket;
          $location.search(search).replace();
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
          $location.search(search).replace();
        }
      };

      var onUpdateCriteria = function (item, type) {
        if (type) {
          onTypeChanged(type);
        }

        onDisplayChanged(DISPLAY_TYPES.LIST);

        selectCriteria(item, RQL_NODE.AND);
      };

      /**
       * Removes the item from the criteria tree
       * @param item
       */
      var removeCriteriaItem = function (item) {
        RqlQueryService.removeCriteriaItem(item);
        refreshQuery();
      };

      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.BUCKET_TYPES = BUCKET_TYPES;

      $scope.search = {
        pagination: {},
        query: null,
        rqlQuery: null,
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

      $scope.documents = {
        search: {
          text: null,
          active: false
        }
      };

      $scope.taxonomies = {
        all: [],
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
      $scope.navigateTaxonomy = navigateTaxonomy;
      $scope.showTaxonomy = showTaxonomy;
      $scope.selectTaxonomyTarget = selectTaxonomyTarget;
      $scope.selectTerm = selectTerm;
      $scope.removeCriteriaItem = removeCriteriaItem;
      $scope.refreshQuery = refreshQuery;
      $scope.closeTaxonomies = closeTaxonomies;
      $scope.onTypeChanged = onTypeChanged;
      $scope.onBucketChanged = onBucketChanged;
      $scope.onDisplayChanged = onDisplayChanged;
      $scope.onUpdateCriteria = onUpdateCriteria;
      $scope.onPaginate = onPaginate;
      $scope.taxonomiesShown = false;

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
        initSearchTabs();
        if (newLocation !== oldLocation) {
          executeSearchQuery();
        }
      });
    }])

  .controller('SearchResultController', [
    '$scope',
    'ngObibaMicaSearch',
    function ($scope,
              ngObibaMicaSearch) {

      $scope.targetTypeMap = $scope.$parent.taxonomyTypeMap;
      $scope.QUERY_TARGETS = QUERY_TARGETS;
      $scope.QUERY_TYPES = QUERY_TYPES;
      $scope.options = ngObibaMicaSearch.getOptions();
      $scope.activeDisplay = {};
      $scope.activeDisplay[$scope.display] = true;
      $scope.activeTarget = {};
      $scope.activeTarget[$scope.type] = true;

      $scope.selectDisplay = function (display) {
        $scope.activeDisplay = {};
        $scope.activeDisplay[display] = true;
        $scope.display = display;
        $scope.$parent.onDisplayChanged(display);
      };

      $scope.selectTarget = function (type) {
        $scope.activeTarget = {};
        $scope.activeTarget[type] = true;
        $scope.type = type;
        $scope.$parent.onTypeChanged(type);
      };

      $scope.$watch('type', function (target) {
        $scope.activeTarget = {};
        $scope.activeTarget[target] = true;
      });

      $scope.$watch('display', function (display) {
        $scope.activeDisplay = {};
        $scope.activeDisplay[display] = true;
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

      $scope.state = new CriterionState();
      $scope.localize = function (values) {
        return LocalizedValues.forLocale(values, $scope.criterion.lang);
      };
      $scope.localizeCriterion = function () {
        var rqlQuery = $scope.criterion.rqlQuery;
        if (rqlQuery.name === RQL_NODE.IN && $scope.criterion.selectedTerms && $scope.criterion.selectedTerms.length > 0) {
          return $scope.criterion.selectedTerms.map(function (t) {
            if (!$scope.criterion.vocabulary.terms) {
              return t;
            }
            var found = $scope.criterion.vocabulary.terms.filter(function (arg) {
              return arg.name === t;
            }).pop();
            return found ? LocalizedValues.forLocale(found.title, $scope.criterion.lang) : t;
          }).join(' | ');
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
            operation = '';
            break;
          case RQL_NODE.MATCH:
            operation = ':match('+rqlQuery.args[0]+')';
            break;
        }
        return LocalizedValues.forLocale($scope.criterion.vocabulary.title, $scope.criterion.lang) + operation;
      };
      $scope.vocabularyType = function (vocabulary) {
        return RqlQueryUtils.vocabularyType(vocabulary);
      };
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
        var selected = [];
        Object.keys($scope.checkboxTerms).forEach(function (key) {
          if ($scope.checkboxTerms[key]) {
            selected.push(key);
          }
        });
        RqlQueryUtils.updateQuery($scope.criterion.rqlQuery, selected);
      };

      var updateFilter = function () {
        RqlQueryUtils.updateQuery($scope.criterion.rqlQuery, [], RQL_NODE.MISSING === $scope.selectedFilter);
        $scope.state.dirty = true;
      };

      var isInFilter = function () {
        return $scope.selectedFilter === RQL_NODE.IN;
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
              $scope.checkboxTerms[term.key] =
                $scope.criterion.selectedTerms && $scope.criterion.selectedTerms.indexOf(term.key) !== -1;
            });

            $scope.terms = $filter('orderBySelection')($scope.terms, $scope.checkboxTerms);
          }
        });
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
      $scope.updateSelection = updateSelection;
    }])

  .controller('CoverageResultTableController', [
    '$scope',
    '$location',
    'PageUrlService',
    'RqlQueryService',
    function ($scope, $location, PageUrlService, RqlQueryService) {
      $scope.showMissing = true;
      $scope.toggleMissing = function (value) {
        $scope.showMissing = value;
      };

      $scope.selectBucket = function (bucket) {
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

      function getBucketUrl(bucket, id) {
        switch (bucket) {
          case BUCKET_TYPES.STUDY:
          case BUCKET_TYPES.DCE:
            return PageUrlService.studyPage(id);
          case BUCKET_TYPES.NETWORK:
            return PageUrlService.networkPage(id);
          case BUCKET_TYPES.DATASCHEMA:
            return PageUrlService.datasetPage(id,'harmonization');
          case BUCKET_TYPES.DATASET:
            return PageUrlService.datasetPage(id,'study');
        }

        return '';
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
            rowSpan = appendRowSpan(id);
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
              url: PageUrlService.studyPopulationPage(ids[0], ids[1]),
              description: descriptions[2],
              rowSpan: 1
            });
          } else {
            cols.ids[row.value].push({
              id: row.value,
              url: getBucketUrl($scope.bucket, row.value),
              title: row.title,
              description: row.description,
              rowSpan: 1
            });
          }

        });

        // adjust the rowspans
        if ($scope.bucket === BUCKET_TYPES.DCE) {
          $scope.result.rows.forEach(function (row) {
            if (cols.ids[row.value][0].rowSpan > 0) {
              cols.ids[row.value][0].rowSpan = rowSpans[cols.ids[row.value][0].id];
            }
            if (cols.ids[row.value][1].rowSpan > 0) {
              cols.ids[row.value][1].rowSpan = rowSpans[cols.ids[row.value][1].id];
            }
          });
        }

        return cols;
      }

      $scope.BUCKET_TYPES = BUCKET_TYPES;

      $scope.downloadUrl = function () {
        return PageUrlService.downloadCoverage($scope.query);
      };

      $scope.$watch('result', function () {
        $scope.table = {};
        $scope.table.cols = [];
        if ($scope.result && $scope.result.rows) {
          $scope.table = $scope.result;
          $scope.table.cols = splitIds();
        }
      });

      $scope.updateDisplay = function() {
        $location.search('display', DISPLAY_TYPES.LIST);
      };

      $scope.updateCriteria = function (id, type) {
        var targetMap = {};
        targetMap[BUCKET_TYPES.NETWORK] = QUERY_TARGETS.NETWORK;
        targetMap[BUCKET_TYPES.STUDY] = QUERY_TARGETS.STUDY;
        targetMap[BUCKET_TYPES.DCE] = QUERY_TARGETS.VARIABLE;
        targetMap[BUCKET_TYPES.DATASCHEMA] = QUERY_TARGETS.DATASET;
        targetMap[BUCKET_TYPES.DATASET] = QUERY_TARGETS.DATASET;
        var vocabulary = $scope.bucket === BUCKET_TYPES.DCE ? 'dceIds' : 'id';

        RqlQueryService.createCriteriaItem(targetMap[$scope.bucket], 'Mica_' + targetMap[$scope.bucket], vocabulary, id).then(function (item) {
          $scope.onUpdateCriteria(item, type);
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
        var entries = GraphicChartsUtils.getArrayByAggregation(vocabulary, dtoObject),
          data = entries.map(function(e) {return [e.title, e.value]; });

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

        return false;
      };

      var charOptions = GraphicChartsConfig.getOptions().ChartsOptions;

      $scope.updateCriteria = function(key, vocabulary) {
        RqlQueryService.createCriteriaItem('study', 'Mica_study', vocabulary, key).then(function (item) {
          $scope.onUpdateCriteria(item, 'studies');
        });
      };

      $scope.$watch('result', function (result) {
        $scope.chartObjects = {};
        $scope.noResults = true;

        if (result && result.studyResultDto.totalHits) {
          $scope.noResults = false;
          var geoStudies = setChartObject('populations-selectionCriteria-countriesIso',
            result.studyResultDto,
            [$filter('translate')(charOptions.geoChartOptions.header[0]), $filter('translate')(charOptions.geoChartOptions.header[1])],
            $filter('translate')(charOptions.geoChartOptions.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.geoChartOptions.options);

          var methodDesignStudies = setChartObject('methods-designs',
            result.studyResultDto,
            [$filter('translate')(charOptions.studiesDesigns.header[0]), $filter('translate')(charOptions.studiesDesigns.header[1])],
            $filter('translate')(charOptions.studiesDesigns.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.studiesDesigns.options);

          var bioSamplesStudies = setChartObject('populations-dataCollectionEvents-bioSamples',
            result.studyResultDto,
            [$filter('translate')(charOptions.biologicalSamples.header[0]), $filter('translate')(charOptions.biologicalSamples.header[1])],
            $filter('translate')(charOptions.biologicalSamples.title) + ' (N = ' + result.studyResultDto.totalHits + ')',
            charOptions.biologicalSamples.options);

          if (geoStudies) {
            angular.extend($scope.chartObjects,
              {
                geoChartOptions: {
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
          if (methodDesignStudies) {
            angular.extend($scope.chartObjects, {
              studiesDesigns: {
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
          if (bioSamplesStudies) {
            angular.extend($scope.chartObjects, {
              biologicalSamples: {
                chartObject: {
                  options: bioSamplesStudies.options,
                  type: 'PieChart',
                  data: bioSamplesStudies.data,
                  vocabulary: bioSamplesStudies.vocabulary,
                  entries: bioSamplesStudies.entries
                }
              }
            });
          }
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

/* global RQL_NODE */

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
        onSelect: '='
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

  .directive('networksResultTable', ['PageUrlService', 'ngObibaMicaSearch', 'RqlQueryService', function (PageUrlService, ngObibaMicaSearch, RqlQueryService) {
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
          RqlQueryService.createCriteriaItem('network', 'Mica_network', 'id', id).then(function (item) {
            scope.onUpdateCriteria(item, type);
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

  .directive('studiesResultTable', ['PageUrlService', 'ngObibaMicaSearch', 'TaxonomyResource', 'RqlQueryService', function (PageUrlService, ngObibaMicaSearch, TaxonomyResource, RqlQueryService) {
    return {
      restrict: 'EA',
      replace: true,
      scope: {
        summaries: '=',
        loading: '=',
        onUpdateCriteria: '='
      },
      templateUrl: 'search/views/list/studies-search-result-table-template.html',
      link: function(scope) {
        scope.taxonomy = {};
        scope.designs = {};

        TaxonomyResource.get({
          target: 'study',
          taxonomy: 'Mica_study'
        }).$promise.then(function (taxonomy) {
            scope.taxonomy = taxonomy;
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
          RqlQueryService.createCriteriaItem('study', 'Mica_study', 'id', id).then(function(item) {
            scope.onUpdateCriteria(item, type);
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
        onUpdateCriteria: '='
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
        onUpdateCriteria: '='
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
        onRemove: '=',
        onRefresh: '='
      },
      template: '<div ng-repeat="child in item.children">' +
      '<criteria-target item="child" query="$parent.query"></criteria-target>' +
      '</div>',
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
        query: '='
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
        query: '='
      },
      controller: 'CriterionLogicalController',
      templateUrl: 'search/views/criteria/criteria-node-template.html'
    };
  }])

  /**
   * This directive creates a hierarchical structure matching that of a RqlQuery tree.
   */
  .directive('criteriaLeaf', ['$compile',
    function($compile){
      return {
        restrict: 'EA',
        replace: true,
        scope: {
          item: '=',
          query: '=',
          parentType: '='
        },
        template: '<span></span>',
        link: function(scope, element) {
          var template = '';
          if (scope.item.type === RQL_NODE.OR || scope.item.type === RQL_NODE.AND || scope.item.type === RQL_NODE.NAND || scope.item.type === RQL_NODE.NOR) {
            template = '<criteria-node item="item" query="query"></criteria-node>';
            $compile(template)(scope, function(cloned){
              element.append(cloned);
            });
          } else {
            template = '<criterion-dropdown criterion="item" query="query"></criterion-dropdown>';
            $compile(template)(scope, function(cloned){
              element.append(cloned);
            });
          }
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
          var isChild = document.querySelector('#'+$scope.criterion.id.replace('.','-')+'-dropdown').contains(event.target);
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
        fieldTransformer: '@',
        chartType: '@',
        chartAggregationName: '@',
        chartEntityDto: '@',
        chartOptionsName: '@',
        chartOptions: '=',
        chartHeader: '=',
        chartTitle: '=',
        chartTableOptions: '=',
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
      fieldTransformer: '@',
      chartType: '@',
      chartAggregationName: '@',
      chartEntityDto: '@',
      chartOptionsName: '@',
      chartOptions: '=',
      chartHeader: '=',
      chartTitle: '=',
      chartTableOptions: '=',
      chartSelectGraphic: '='
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
    'GraphicChartsConfig',
    'GraphicChartsUtils',
    'GraphicChartsData',
    function ($rootScope,
              $scope,
              $filter,
              GraphicChartsConfig,
              GraphicChartsUtils,
              GraphicChartsData) {


      $scope.$watch('chartSelectGraphic', function (newValue) {
        if (newValue) {
          GraphicChartsData.getData(function (StudiesData) {
            if (StudiesData) {
              $scope.ItemDataJSon = GraphicChartsUtils.getArrayByAggregation($scope.chartAggregationName, StudiesData[$scope.chartEntityDto])
                .map(function(t) {
                  return [t.title, t.value];
                });
              if ($scope.ItemDataJSon) {
                if ($scope.chartType === 'Table') {
                  $scope.chartObject = {};
                  $scope.chartObject.header = [$filter('translate')($scope.chartHeader[0]), $filter('translate')($scope.chartHeader[1])];
                  $scope.chartObject.type = $scope.chartType;
                  $scope.chartObject.data = $scope.ItemDataJSon;
                }
                else {
                  $scope.ItemDataJSon.unshift([$filter('translate')($scope.chartHeader[0]), $filter('translate')($scope.chartHeader[1])]);
                  $scope.chartObject = {};
                  $scope.chartObject.type = $scope.chartType;
                  $scope.chartObject.data = $scope.ItemDataJSon;
                  $scope.chartObject.options = {backgroundColor: {fill: 'transparent'}};
                  angular.extend($scope.chartObject.options, $scope.chartOptions);
                  $scope.chartObject.options.title = $filter('translate')($scope.chartTitle) + ' (N=' + StudiesData.studyResultDto.totalHits + ')';
                }
              }
            }
          });
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
            header : ['graphics.study-design', 'graphics.nbr-studies'],
            title : 'graphics.study-design-chart-title',
            options: {
              legend: {position: 'none'},
              backgroundColor: {fill: 'transparent'},
              colors: ['#b8cbed',
                '#e5edfb',
                '#cfddf5',
                '#a0b8e2',
                '#88a4d4']
            }
          },
          biologicalSamples: {
            header : ['graphics.bio-samples', 'graphics.nbr-studies'],
            title : 'graphics.bio-samples-chart-title',
            options: {
              backgroundColor: {fill: 'transparent'},
              colors: ['#b8cbed',
                '#e5edfb',
                '#cfddf5',
                '#a0b8e2',
                '#88a4d4'],
              pieSliceTextStyle: {color: '#000000'}
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
    function () {
      this.getArrayByAggregation = function (aggregationName, entityDto) {
        var arrayData = [];

        if (!entityDto) {
          return arrayData;
        }

        angular.forEach(entityDto.aggs, function (aggregation) {
          if (aggregation.aggregation === aggregationName) {
            var i = 0;
            angular.forEach(aggregation['obiba.mica.TermsAggregationResultDto.terms'], function (term) {
              if (term.count) {
                arrayData[i] = {title: term.title, value: term.count, key: term.key};
                i++;
              }
            });
          }
        });

        return arrayData;
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
        ['methods.designs', 'populations.selectionCriteria.countriesIso', 'populations.dataCollectionEvents.bioSamples', 'numberOfParticipants.participant.number']);
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
          return result[0][keyValue];
        }
      };

      this.forLocale = function (values, lang) {
        return this.for(values, lang, 'locale', 'text');
      };

      this.forLang = function (values, lang) {
        return this.for(values, lang, 'lang', 'value');
      };

      this.getLocal = function () {
        return 'en';
      };
    });
;angular.module('templates-ngObibaMica', ['access/views/data-access-request-form.html', 'access/views/data-access-request-histroy-view.html', 'access/views/data-access-request-list.html', 'access/views/data-access-request-profile-user-modal.html', 'access/views/data-access-request-submitted-modal.html', 'access/views/data-access-request-validation-modal.html', 'access/views/data-access-request-view.html', 'attachment/attachment-input-template.html', 'attachment/attachment-list-template.html', 'graphics/views/charts-directive.html', 'graphics/views/tables-directive.html', 'localized/localized-input-group-template.html', 'localized/localized-input-template.html', 'localized/localized-textarea-template.html', 'search/views/classifications/taxonomies-view.html', 'search/views/classifications/taxonomy-panel-template.html', 'search/views/classifications/taxonomy-template.html', 'search/views/classifications/term-panel-template.html', 'search/views/classifications/vocabulary-panel-template.html', 'search/views/coverage/coverage-search-result-table-template.html', 'search/views/criteria/criteria-node-template.html', 'search/views/criteria/criteria-target-template.html', 'search/views/criteria/criterion-dropdown-template.html', 'search/views/criteria/criterion-match-template.html', 'search/views/criteria/criterion-numeric-template.html', 'search/views/criteria/criterion-string-terms-template.html', 'search/views/criteria/target-template.html', 'search/views/graphics/graphics-search-result-template.html', 'search/views/list/datasets-search-result-table-template.html', 'search/views/list/networks-search-result-table-template.html', 'search/views/list/pagination-template.html', 'search/views/list/search-result-pagination-template.html', 'search/views/list/studies-search-result-table-template.html', 'search/views/list/variables-search-result-table-template.html', 'search/views/search-result-coverage-template.html', 'search/views/search-result-graphics-template.html', 'search/views/search-result-list-dataset-template.html', 'search/views/search-result-list-network-template.html', 'search/views/search-result-list-study-template.html', 'search/views/search-result-list-template.html', 'search/views/search-result-list-variable-template.html', 'search/views/search-result-panel-template.html', 'search/views/search.html']);

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
    "  <div google-chart chart=\"chartObject\" style=\"min-height:350px; width:100%;\">\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("graphics/views/tables-directive.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("graphics/views/tables-directive.html",
    "<div>\n" +
    "  <div style=\"margin:25px;\">\n" +
    "    <table class=\"table table-striped table-bordered\">\n" +
    "      <thead>\n" +
    "      <th ng-repeat=\"header in chartObject.header\">{{header}}</th>\n" +
    "      </thead>\n" +
    "      <tbody>\n" +
    "      <tr ng-repeat=\"data in chartObject.data | orderBy:'-this[1]'\">\n" +
    "        <td ng-repeat=\"row in data\">{{row}}</td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "    </table>\n" +
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
    "      <div class=\"col-md-8\">\n" +
    "        <ol class=\"breadcrumb no-margin no-padding pull-left\">\n" +
    "          <li ng-if=\"!taxonomies.taxonomy\">\n" +
    "            {{'all-' + taxonomies.target + '-classifications' | translate}}\n" +
    "          </li>\n" +
    "          <li ng-if=\"taxonomies.taxonomy\">\n" +
    "            <a href ng-click=\"navigateTaxonomy()\">{{'all-' + taxonomies.target + '-classifications' | translate}}</a>\n" +
    "          </li>\n" +
    "          <li ng-if=\"taxonomies.taxonomy\">\n" +
    "            <span ng-repeat=\"label in taxonomies.taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "              {{label.text}}\n" +
    "            </span>\n" +
    "          </li>\n" +
    "        </ol>\n" +
    "      </div>\n" +
    "      <div class=\"col-md-4\">\n" +
    "        <span ng-click=\"closeTaxonomies()\" title=\"{{'close' | translate}}\" class=\"pull-right\"><i\n" +
    "          class=\"fa fa-close\"></i></span>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "  <div class=\"panel-body\">\n" +
    "    <div ng-if=\"taxonomies.search.active\" class=\"loading\"></div>\n" +
    "\n" +
    "    <div ng-if=\"!taxonomies.taxonomy\">\n" +
    "      <div ng-repeat=\"group in taxonomyGroups\">\n" +
    "        <h4 ng-if=\"group.title\">{{group.title}}</h4>\n" +
    "        <p class=\"help-block\" ng-if=\"group.description\">{{group.description}}</p>\n" +
    "        <div ng-if=\"!taxonomies.taxonomy\">\n" +
    "          <div ng-repeat=\"taxonomy in group.taxonomies\" ng-if=\"$index % 3 == 0\" class=\"row\">\n" +
    "            <div class=\"col-md-4\">\n" +
    "              <div taxonomy-panel taxonomy=\"group.taxonomies[$index]\" lang=\"lang\" on-navigate=\"navigateTaxonomy\"></div>\n" +
    "            </div>\n" +
    "            <div class=\"col-md-4\">\n" +
    "              <div taxonomy-panel taxonomy=\"group.taxonomies[$index + 1]\" lang=\"lang\"\n" +
    "                on-navigate=\"navigateTaxonomy\"></div>\n" +
    "            </div>\n" +
    "            <div class=\"col-md-4\">\n" +
    "              <div taxonomy-panel taxonomy=\"group.taxonomies[$index + 2]\" lang=\"lang\"\n" +
    "                on-navigate=\"navigateTaxonomy\"></div>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "\n" +
    "    <div ng-if=\"taxonomies.taxonomy\">\n" +
    "      <div class=\"row\">\n" +
    "        <div class=\"col-md-4 height3\" scroll-to-top=\"taxonomies.taxonomy\">\n" +
    "          <h5 ng-repeat=\"label in taxonomies.taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "            {{label.text}}\n" +
    "          </h5>\n" +
    "          <p class=\"help-block\" ng-repeat=\"label in taxonomies.taxonomy.description\" ng-if=\"label.locale === lang\">\n" +
    "            {{label.text}}\n" +
    "          </p>\n" +
    "          <ul class=\"nav nav-pills nav-stacked\" ng-if=\"taxonomies.taxonomy.vocabularies\">\n" +
    "            <li ng-repeat=\"vocabulary in taxonomies.taxonomy.vocabularies\"\n" +
    "              class=\"{{taxonomies.vocabulary.name === vocabulary.name ? 'active' : ''}}\">\n" +
    "              <a href ng-click=\"navigateTaxonomy(taxonomies.taxonomy, vocabulary)\">\n" +
    "                <span ng-repeat=\"label in vocabulary.title\" ng-if=\"label.locale === lang\">\n" +
    "                  {{label.text}}\n" +
    "                </span>\n" +
    "                <span ng-if=\"!vocabulary.title\">\n" +
    "                  {{vocabulary.name}}\n" +
    "                </span>\n" +
    "              </a>\n" +
    "            </li>\n" +
    "          </ul>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4 height3\" scroll-to-top=\"taxonomies.vocabulary\">\n" +
    "          <div ng-if=\"taxonomies.vocabulary\">\n" +
    "            <h5 ng-repeat=\"label in taxonomies.vocabulary.title\" ng-if=\"label.locale === lang\">\n" +
    "              {{label.text}}\n" +
    "            </h5>\n" +
    "            <p class=\"help-block\" ng-repeat=\"label in taxonomies.vocabulary.description\" ng-if=\"label.locale === lang\">\n" +
    "              {{label.text}}\n" +
    "            </p>\n" +
    "            <div>\n" +
    "              <a href class=\"btn btn-default btn-xs\"\n" +
    "                ng-click=\"selectTerm(taxonomies.target, taxonomies.taxonomy, taxonomies.vocabulary)\">\n" +
    "                <i class=\"fa fa-plus-circle\"></i>\n" +
    "                <span translate>add-query</span>\n" +
    "              </a>\n" +
    "            </div>\n" +
    "            <ul class=\"nav nav-pills nav-stacked\" ng-if=\"taxonomies.vocabulary.terms\">\n" +
    "              <li ng-repeat=\"term in taxonomies.vocabulary.terms\"\n" +
    "                class=\"{{taxonomies.term.name === term.name ? 'active' : ''}}\">\n" +
    "                <a href ng-click=\"navigateTaxonomy(taxonomies.taxonomy, taxonomies.vocabulary, term)\">\n" +
    "                <span ng-repeat=\"label in term.title\" ng-if=\"label.locale === lang\">\n" +
    "                  {{label.text}}\n" +
    "                </span>\n" +
    "                <span ng-if=\"!term.title\">\n" +
    "                  {{term.name}}\n" +
    "                </span>\n" +
    "                </a>\n" +
    "              </li>\n" +
    "            </ul>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-4 height3\" scroll-to-top=\"taxonomies.term\">\n" +
    "          <div ng-if=\"taxonomies.term\">\n" +
    "            <h5 ng-repeat=\"label in taxonomies.term.title\" ng-if=\"label.locale === lang\">\n" +
    "              {{label.text}}\n" +
    "            </h5>\n" +
    "            <p ng-repeat=\"label in taxonomies.term.description\" ng-if=\"label.locale === lang\">\n" +
    "              <span class=\"help-block\">{{label.text}}</span>\n" +
    "            </p>\n" +
    "            <div>\n" +
    "              <a href class=\"btn btn-default btn-xs\"\n" +
    "                ng-click=\"selectTerm(taxonomies.target, taxonomies.taxonomy, taxonomies.vocabulary, taxonomies.term)\">\n" +
    "                <i class=\"fa fa-plus-circle\"></i>\n" +
    "                <span translate>add-query</span>\n" +
    "              </a>\n" +
    "            </div>\n" +
    "          </div>\n" +
    "        </div>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "</div>");
}]);

angular.module("search/views/classifications/taxonomy-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/classifications/taxonomy-panel-template.html",
    "<div>\n" +
    "  <h5 ng-repeat=\"label in taxonomy.title\" ng-if=\"label.locale === lang\">\n" +
    "    <a href ng-click=\"onNavigate(taxonomy)\">{{label.text}}</a>\n" +
    "  </h5>\n" +
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
    "    <a href ng-click=\"onNavigate(taxonomy, vocabulary)\" ng-if=\"vocabulary.terms\">{{label.text}}</a>\n" +
    "    <span ng-if=\"!vocabulary.terms\">{{label.text}}</span>\n" +
    "    <a href ng-click=\"onSelect(target, taxonomy, vocabulary)\">\n" +
    "      <small ng-if=\"vocabulary.terms\"><i class=\"fa fa-plus-circle\" title=\"{{'add-query' | translate}}\"></i></small>\n" +
    "      <i ng-if=\"!vocabulary.terms\" class=\"fa fa-plus-circle\" title=\"{{'add-query' | translate}}\"></i>\n" +
    "    </a>\n" +
    "  </h4>\n" +
    "  <p class=\"help-block\" ng-repeat=\"label in vocabulary.description\" ng-if=\"label.locale === lang\">\n" +
    "    {{label.text}}\n" +
    "  </p>\n" +
    "</div>");
}]);

angular.module("search/views/coverage/coverage-search-result-table-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/coverage/coverage-search-result-table-template.html",
    "<div>\n" +
    "  <div class=\"pull-left\" ng-if=\"!loading && table.taxonomyHeaders.length\">\n" +
    "    <span translate>search.coverage-group-by</span>\n" +
    "    <div class=\"btn-group\" uib-dropdown is-open=\"status.isopen\">\n" +
    "      <button type=\"button\" class=\"btn btn-primary btn-sm\" uib-dropdown-toggle ng-disabled=\"disabled\">\n" +
    "        {{'search.coverage-buckets.' + bucket | translate}} <span class=\"caret\"></span>\n" +
    "      </button>\n" +
    "      <ul uib-dropdown-menu role=\"menu\">\n" +
    "        <li role=\"menuitem\" ng-if=\"bucket !== BUCKET_TYPES.STUDY\">\n" +
    "          <a href ng-click=\"selectBucket(BUCKET_TYPES.STUDY)\" translate>search.coverage-buckets.study</a>\n" +
    "        </li>\n" +
    "        <li role=\"menuitem\" ng-if=\"bucket !== BUCKET_TYPES.DCE\">\n" +
    "          <a href ng-click=\"selectBucket(BUCKET_TYPES.DCE)\" translate>search.coverage-buckets.dce</a>\n" +
    "        </li>\n" +
    "        <li role=\"menuitem\" ng-if=\"bucket !== BUCKET_TYPES.DATASET\">\n" +
    "          <a href ng-click=\"selectBucket(BUCKET_TYPES.DATASET)\" translate>search.coverage-buckets.dataset</a>\n" +
    "        </li>\n" +
    "        <li role=\"menuitem\" ng-if=\"bucket !== BUCKET_TYPES.NETWORK\">\n" +
    "          <a href ng-click=\"selectBucket(BUCKET_TYPES.NETWORK)\" translate>search.coverage-buckets.network</a>\n" +
    "        </li>\n" +
    "        <li role=\"menuitem\" ng-if=\"bucket !== BUCKET_TYPES.DATASCHEMA\">\n" +
    "          <a href ng-click=\"selectBucket(BUCKET_TYPES.DATASCHEMA)\" translate>search.coverage-buckets.dataschema</a>\n" +
    "        </li>\n" +
    "      </ul>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"pull-right\" ng-if=\"table.taxonomyHeaders.length > 0\">\n" +
    "    <a target=\"_self\" class=\"btn btn-info btn-responsive\" ng-href=\"{{downloadUrl()}}\">\n" +
    "      <i class=\"fa fa-download\"></i> {{'download' | translate}}\n" +
    "    </a>\n" +
    "    <!--<a href ng-click=\"toggleMissing(false)\" ng-if=\"showMissing\" translate>search.coverage-hide-missing</a>-->\n" +
    "    <!--<a href ng-click=\"toggleMissing(true)\" ng-if=\"!showMissing\" translate>search.coverage-show-missing</a>-->\n" +
    "  </div>\n" +
    "\n" +
    "  <div class=\"clearfix\"></div>\n" +
    "\n" +
    "  <p class=\"help-block\" ng-if=\"!loading && !table.taxonomyHeaders\" translate>search.no-coverage</p>\n" +
    "\n" +
    "  <div ng-if=\"loading\" class=\"loading\"></div>\n" +
    "\n" +
    "  <div class=\"table-responsive\" ng-if=\"table.taxonomyHeaders.length > 0\">\n" +
    "    <table class=\"table table-bordered table-striped\">\n" +
    "      <thead>\n" +
    "      <tr>\n" +
    "        <th rowspan=\"{{bucket === BUCKET_TYPES.DCE ? 1 : 2}}\" colspan=\"{{table.cols.colSpan}}\" translate>{{'search.coverage-buckets.' + bucket}}</th>\n" +
    "        <th ng-repeat=\"header in table.vocabularyHeaders\" colspan=\"{{header.termsCount}}\">\n" +
    "          <span\n" +
    "            uib-popover=\"{{header.entity.descriptions[0].value}}\"\n" +
    "            popover-title=\"{{header.entity.titles[0].value}}\"\n" +
    "            popover-placement=\"bottom\"\n" +
    "            popover-trigger=\"mouseenter\">\n" +
    "          {{header.entity.titles[0].value}}\n" +
    "            </span>\n" +
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
    "            </span>\n" +
    "        </th>\n" +
    "      </tr>\n" +
    "      </thead>\n" +
    "      <tbody>\n" +
    "      <tr ng-repeat=\"row in table.rows\" ng-if=\"showMissing || table.termHeaders.length == keys(row.hits).length\">\n" +
    "        <td ng-repeat=\"col in table.cols.ids[row.value]\" rowspan=\"{{col.rowSpan}}\" ng-if=\"col.rowSpan > 0\">\n" +
    "          <a href=\"{{col.url ? col.url : ''}}\"\n" +
    "            uib-popover-html=\"col.description === col.title ? null : col.description\"\n" +
    "            popover-title=\"{{col.title}}\"\n" +
    "            popover-placement=\"bottom\"\n" +
    "            popover-trigger=\"mouseenter\">{{col.title}}</a>\n" +
    "        </td>\n" +
    "        <td ng-repeat=\"h in table.termHeaders\">\n" +
    "          <a href ng-click=\"updateCriteria(row.value, 'variables')\"><span class=\"label label-info\" ng-if=\"row.hits[$index]\">{{row.hits[$index]}}</span></a>\n" +
    "          <span ng-if=\"!row.hits[$index]\">0</span>\n" +
    "        </td>\n" +
    "      </tr>\n" +
    "      </tbody>\n" +
    "      <tfoot>\n" +
    "      <tr>\n" +
    "        <th colspan=\"{{table.cols.colSpan}}\" translate>all</th>\n" +
    "        <th ng-repeat=\"header in table.termHeaders\" title=\"{{header.entity.descriptions[0].value}}\">\n" +
    "          <a href ng-click=\"updateDisplay()\">{{header.hits}}</a>\n" +
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
    "    <criteria-leaf item=\"item.children[0]\" parent-type=\"$parent.item.type\" query=\"query\"></criteria-leaf>\n" +
    "\n" +
    "    <div class=\"btn-group voffset1\" uib-dropdown is-open=\"status.isopen\">\n" +
    "      <button type=\"button\" class=\"btn btn-default btn-xs\" uib-dropdown-toggle ng-disabled=\"disabled\">\n" +
    "        {{item.type | translate}} <span class=\"caret\"></span>\n" +
    "      </button>\n" +
    "      <ul uib-dropdown-menu role=\"menu\" aria-labelledby=\"single-button\">\n" +
    "        <li role=\"menuitem\" ng-if=\"item.type !== 'or'\"><a href ng-click=\"updateLogical('or')\" translate>or</a></li>\n" +
    "        <li role=\"menuitem\" ng-if=\"item.type !== 'and'\"><a href ng-click=\"updateLogical('and')\" translate>and</a></li>\n" +
    "        <li role=\"menuitem\" ng-if=\"item.type !== 'nor'\"><a href ng-click=\"updateLogical('nor')\" translate>nor</a></li>\n" +
    "        <li role=\"menuitem\" ng-if=\"item.type !== 'nand'\"><a href ng-click=\"updateLogical('nand')\" translate>nand</a></li>\n" +
    "      </ul>\n" +
    "    </div>\n" +
    "    <criteria-leaf item=\"item.children[1]\" parent-type=\"$parent.item.type\" query=\"query\"></criteria-leaf>\n" +
    "\n" +
    "  </span>\n" +
    "  <span ng-if=\"item.children.length === 0\">\n" +
    "    <criteria-leaf item=\"item\" parent-type=\"item.parent.type\" query=\"query\"></criteria-leaf>\n" +
    "  </span>\n" +
    "</span>");
}]);

angular.module("search/views/criteria/criteria-target-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criteria-target-template.html",
    "<table style=\"border:none;\" class=\"voffset2\">\n" +
    "  <tbody>\n" +
    "  <tr>\n" +
    "    <td>\n" +
    "      <div class=\"voffset1\" title=\"{{'search.' + item.target + '-where' | translate}}\">\n" +
    "        <i class=\"{{'i-obiba-' + item.target}}\">&nbsp;</i>\n" +
    "      </div>\n" +
    "    </td>\n" +
    "    <td>\n" +
    "      <criteria-node item=\"child\" query=\"$parent.query\" ng-repeat=\"child in item.children\"></criteria-node>\n" +
    "    </td>\n" +
    "  </tr>\n" +
    "  </tbody>\n" +
    "</table>");
}]);

angular.module("search/views/criteria/criterion-dropdown-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criterion-dropdown-template.html",
    "<div id=\"{{criterion.id.replace('.','-')}}-dropdown\" class='btn-group btn-info voffset1' ng-class='{open: state.open}'>\n" +
    "\n" +
    "  <button class=\"btn btn-info btn-xs dropdown\"\n" +
    "    ng-click=\"openDropdown()\"\n" +
    "    title=\"{{localizeCriterion()}}\">\n" +
    "    <span uib-popover=\"{{localize(criterion.vocabulary.description ? criterion.vocabulary.description : criterion.vocabulary.title)}}\"\n" +
    "          popover-title=\"{{criterion.vocabulary.description ? localize(criterion.vocabulary.title) : null}}\"\n" +
    "          popover-placement=\"bottom\"\n" +
    "          popover-trigger=\"mouseenter\">\n" +
    "    <i class=\"fa fa-info-circle\"> </i>\n" +
    "  </span>\n" +
    "    <span>\n" +
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

angular.module("search/views/criteria/criterion-match-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criterion-match-template.html",
    "<ul class=\"dropdown-menu query-dropdown-menu\" aria-labelledby=\"{{criterion.vocabulary.name}}-button\">\n" +
    "  <li class=\"criteria-list-item\">\n" +
    "    <form novalidate>\n" +
    "      <div  >\n" +
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
    "  <li class=\"btn-group\">\n" +
    "    <ul class=\"criteria-list-item\">\n" +
    "      <li>\n" +
    "        <label>\n" +
    "          <input ng-click=\"updateSelection()\" type=\"radio\" ng-model=\"selectMissing\" ng-value=\"false\">\n" +
    "          {{'any' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "      <li>\n" +
    "        <label>\n" +
    "          <input ng-click=\"updateSelection()\" type=\"radio\" ng-model=\"selectMissing\" ng-value=\"true\">\n" +
    "          {{'none' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "    </ul>\n" +
    "  </li>\n" +
    "  <li ng-show=\"!selectMissing\" class='divider'></li>\n" +
    "  <li ng-show=\"!selectMissing\" class=\"btn-group criteria-list-item\">\n" +
    "    <form novalidate>\n" +
    "      <div class=\"form-group\">\n" +
    "        <label for=\"{{criterion.vocabulary.name}}-from\" translate>from</label>\n" +
    "        <input type=\"number\" class=\"form-control\" id=\"{{criterion.vocabulary.name}}-from\" placeholder=\"{{min}}\" ng-model=\"from\">\n" +
    "      </div>\n" +
    "      <div class=\"form-group\">\n" +
    "        <label for=\"{{criterion.vocabulary.name}}-to\" translate>to</label>\n" +
    "        <input type=\"number\" class=\"form-control\" id=\"{{criterion.vocabulary.name}}-to\" placeholder=\"{{max}}\" ng-model=\"to\">\n" +
    "      </div>\n" +
    "    </form>\n" +
    "  </li>\n" +
    "</ul>");
}]);

angular.module("search/views/criteria/criterion-string-terms-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/criteria/criterion-string-terms-template.html",
    "<ul class=\"dropdown-menu query-dropdown-menu\" aria-labelledby=\"{{criterion.vocabulary.name}}-button\">\n" +
    "  <li class=\"btn-group\">\n" +
    "    <ul class=\"criteria-list-item\">\n" +
    "      <li>\n" +
    "        <label>\n" +
    "          <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.EXISTS}}\">\n" +
    "          {{'any' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "      <li>\n" +
    "        <label>\n" +
    "          <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.MISSING}}\">\n" +
    "          {{'none' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "      <li>\n" +
    "        <label>\n" +
    "          <input ng-click=\"updateFilter()\" type=\"radio\" ng-model=\"selectedFilter\" value=\"{{RQL_NODE.IN}}\">\n" +
    "          {{'in' | translate}}\n" +
    "        </label>\n" +
    "      </li>\n" +
    "    </ul>\n" +
    "  </li>\n" +
    "  <li ng-show=\"isInFilter()\" class='divider'></li>\n" +
    "  <li class=\"criteria-list-item\" ng-show=\"state.loading\">\n" +
    "    <p class=\"voffset2 loading\">\n" +
    "    </p>\n" +
    "  </li>\n" +
    "  <li ng-show=\"isInFilter()\">\n" +
    "    <ul ng-show=\"!state.loading\" class=\"no-padding criteria-list-terms\">\n" +
    "      <li class=\"criteria-list-item\" ng-show=\"terms && terms.length>10\">\n" +
    "        <span class=\"input-group input-group-sm no-padding-top\">\n" +
    "          <input ng-model=\"searchText\" type=\"text\" class=\"form-control\" aria-describedby=\"term-search\">\n" +
    "          <span class=\"input-group-addon\" id=\"term-search\"><i class=\"glyphicon glyphicon-search\"></i></span>\n" +
    "        </span>\n" +
    "      </li>\n" +
    "      <li ng-show=\"terms && terms.length>10\"></li>\n" +
    "      <li class=\"criteria-list-item\"\n" +
    "          ng-show=\"isInFilter()\"\n" +
    "          ng-repeat=\"term in terms | regex:searchText:['key','title','description']\"\n" +
    "          uib-popover=\"{{term.description ? term.description : term.title}}\"\n" +
    "          popover-title=\"{{term.description ? term.title : null}}\"\n" +
    "          popover-placement=\"bottom\"\n" +
    "          popover-trigger=\"mouseenter\">\n" +
    "          <span>\n" +
    "            <label class=\"control-label\">\n" +
    "              <input ng-model=\"checkboxTerms[term.key]\"\n" +
    "                     type=\"checkbox\"\n" +
    "                     ng-click=\"updateSelection()\">\n" +
    "              <span>{{truncate(term.title)}}</span>\n" +
    "            </label>\n" +
    "          </span>\n" +
    "          <span class=\"pull-right\">\n" +
    "            <span class=\"agg-term-count\" ng-show=\"term.count !== 0\">{{term.count}}</span>\n" +
    "            <span class=\"agg-term-count-default\" ng-show=\"term.count === 0\">{{term.default}}</span>\n" +
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
    "  <div class=\"row voffset2\" ng-if=\"!loading && noResults\">\n" +
    "    <div class=\"col-md-12\">\n" +
    "      <p translate>search.noResults</p>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "  <div ng-repeat=\"chart in chartObjects\" class=\"panel panel-default\">\n" +
    "    <div class=\"panel-heading\">\n" +
    "      {{chart.chartObject.options.title}}\n" +
    "    </div>\n" +
    "    <div class=\"panel-body\">\n" +
    "      <div class=\"row\">\n" +
    "        <div class=\"col-md-6\">\n" +
    "          <div google-chart chart=\"chart.chartObject\" style=\"min-height:350px; width:100%;\"></div>\n" +
    "        </div>\n" +
    "        <div class=\"col-md-6\">\n" +
    "          <div class=\"table-responsive\" ng-if=\"chart.chartObject.data && chart.chartObject.data.length>1\">\n" +
    "            <table class=\"table table-bordered table-striped\">\n" +
    "              <thead>\n" +
    "              <tr>\n" +
    "                <th>{{chart.chartObject.data[0][0]}}</th>\n" +
    "                <th>{{chart.chartObject.data[0][1]}}</th>\n" +
    "              </tr>\n" +
    "              </thead>\n" +
    "              <tbody>\n" +
    "              <tr ng-repeat=\"row in chart.chartObject.entries\">\n" +
    "                <td>{{row.title}}</td>\n" +
    "                <td><a href ng-click=\"updateCriteria(row.key, chart.chartObject.vocabulary)\">{{row.value}}</a></td>\n" +
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
    "    <div class=\"table-responsive\">\n" +
    "      <table class=\"table table-bordered table-striped\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th translate>acronym</th>\n" +
    "          <th translate>name</th>\n" +
    "          <th translate ng-if=\"optionsCols.showDatasetsTypeColumn\">type</th>\n" +
    "          <th translate ng-if=\"optionsCols.showDatasetsNetworkColumn\">networks</th>\n" +
    "          <th translate ng-if=\"optionsCols.showDatasetsStudiesColumn\">studies</th>\n" +
    "          <th translate ng-if=\"optionsCols.showDatasetsVariablesColumn\">variables</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "        <tr ng-if=\"!summaries || !summaries.length\">\n" +
    "          <td colspan=\"6\">{{options.noResultsLabel | translate}}</td>\n" +
    "        </tr>\n" +
    "        <tr ng-repeat=\"summary in summaries\" ng-init=\"lang = $parent.$parent.lang\">\n" +
    "          <td>\n" +
    "            <a ng-href=\"{{PageUrlService.datasetPage(summary.id, summary.variableType)}}\">\n" +
    "              <localized value=\"summary.acronym\" lang=\"lang\"></localized>\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.name\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showDatasetsTypeColumn\">\n" +
    "            <localized value=\"classNames[summary.variableType + 'Dataset']\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showDatasetsNetworkColumn\">\n" +
    "            {{summary['obiba.mica.CountStatsDto.datasetCountStats'].networks}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showDatasetsStudiesColumn\">\n" +
    "            {{summary['obiba.mica.CountStatsDto.datasetCountStats'].studies}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showDatasetsVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'variables')\">{{summary['obiba.mica.CountStatsDto.datasetCountStats'].variables}}</a>\n" +
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
    "    <div class=\"table-responsive\">\n" +
    "      <table class=\"table table-bordered table-striped\" ng-init=\"lang = $parent.$parent.lang\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th rowspan=\"2\" translate>acronym</th>\n" +
    "          <th rowspan=\"2\" translate>name</th>\n" +
    "          <th rowspan=\"2\" translate ng-if=\"optionsCols.showNetworksStudiesColumn\">studies</th>\n" +
    "          <th translate\n" +
    "              ng-attr-colspan=\"{{optionsCols.showNetworksStudyDatasetColumn + optionsCols.showNetworksHarmonizedDatasetColumn}}\"\n" +
    "              ng-if=\"optionsCols.showNetworksStudyDatasetColumn || optionsCols.showNetworksHarmonizedDatasetColumn\">\n" +
    "            datasets\n" +
    "          </th>\n" +
    "          <th rowspan=\"2\" translate ng-if=\"optionsCols.showNetworksVariablesColumn\">variables</th>\n" +
    "        </tr>\n" +
    "        <tr>\n" +
    "          <th translate ng-if=\"optionsCols.showNetworksStudyDatasetColumn\">search.study.label</th>\n" +
    "          <th translate ng-if=\"optionsCols.showNetworksHarmonizedDatasetColumn\">search.harmonization</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "        <tr ng-if=\"!summaries || !summaries.length\">\n" +
    "          <td colspan=\"6\">{{options.noResultsLabel | translate}}</td>\n" +
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
    "            {{summary['obiba.mica.CountStatsDto.networkCountStats'].studies || '-'}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showNetworksStudyDatasetColumn\">\n" +
    "            {{summary['obiba.mica.CountStatsDto.networkCountStats'].studyDatasets || '-'}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showNetworksHarmonizedDatasetColumn\">\n" +
    "            {{summary['obiba.mica.CountStatsDto.networkCountStats'].harmonizationDatasets || '-'}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showNetworksVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'variables')\">{{summary['obiba.mica.CountStatsDto.networkCountStats'].variables}}</a>\n" +
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
    "    <div class=\"table-responsive\">\n" +
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
    "              ng-attr-colspan=\"{{optionsCols.showStudiesDatasetsColumn + optionsCols.showStudiesHarmonizedDatasetsColumn}}\"\n" +
    "              ng-if=\"optionsCols.showStudiesDatasetsColumn || optionsCols.showStudiesHarmonizedDatasetsColumn\">datasets\n" +
    "          </th>\n" +
    "          <th rowspan=\"2\" translate ng-if=\"optionsCols.showStudiesVariablesColumn\">variables</th>\n" +
    "        </tr>\n" +
    "        <tr>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesQuestionnaireColumn\">search.study.quest</th>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesPmColumn\">search.study.pm</th>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesBioColumn\">search.study.bio</th>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesOtherColumn\">search.study.others</th>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesDatasetsColumn\">search.study.label</th>\n" +
    "          <th translate ng-if=\"optionsCols.showStudiesHarmonizedDatasetsColumn\">search.harmonization</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "        <tr ng-if=\"!summaries || !summaries.length\">\n" +
    "          <td colspan=\"12\">{{options.noResultsLabel | translate}}</td>\n" +
    "        </tr>\n" +
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
    "            {{summary.targetNumber.number}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesNetworksColumn\">\n" +
    "            {{summary['obiba.mica.CountStatsDto.studyCountStats'].networks || '-'}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesDatasetsColumn\">\n" +
    "            {{summary['obiba.mica.CountStatsDto.studyCountStats'].studyDatasets || '-'}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesHarmonizedDatasetsColumn\">\n" +
    "            {{summary['obiba.mica.CountStatsDto.studyCountStats'].harmonizationDatasets || '-'}}\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showStudiesVariablesColumn\">\n" +
    "            <a href ng-click=\"updateCriteria(summary.id, 'variables')\">{{summary['obiba.mica.CountStatsDto.studyCountStats'].variables}}</a>\n" +
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
    "    <div class=\"table-responsive\">\n" +
    "      <table class=\"table table-bordered table-striped\" ng-init=\"lang = $parent.$parent.lang\">\n" +
    "        <thead>\n" +
    "        <tr>\n" +
    "          <th translate>name</th>\n" +
    "          <th translate>search.variable.label</th>\n" +
    "          <th translate ng-if=\"optionsCols.showVariablesStudiesColumn\">search.study.label</th>\n" +
    "          <th translate ng-if=\"optionsCols.showVariablesDatasetsColumn\">search.dataset.label</th>\n" +
    "        </tr>\n" +
    "        </thead>\n" +
    "        <tbody>\n" +
    "        <tr ng-if=\"!summaries || !summaries.length\">\n" +
    "          <td colspan=\"4\">{{options.noResultsLabel | translate}}</td>\n" +
    "        </tr>\n" +
    "        <tr ng-repeat=\"summary in summaries\">\n" +
    "          <td>\n" +
    "            <a href=\"{{PageUrlService.VariablePage(summary.id)}}\">\n" +
    "              {{summary.name}}\n" +
    "            </a>\n" +
    "          </td>\n" +
    "          <td>\n" +
    "            <localized value=\"summary.variableLabel\" lang=\"lang\"></localized>\n" +
    "          </td>\n" +
    "          <td ng-if=\"optionsCols.showVariablesStudiesColumn\">\n" +
    "            <a ng-href=\"{{PageUrlService.studyPage(summary.studyId)}}\">\n" +
    "              <localized value=\"summary.studyAcronym\" lang=\"lang\"></localized>\n" +
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
    "<div class=\"tab-pane\" ng-class=\"{active: activeDisplay.coverage}\">\n" +
    "  <coverage-result-table result=\"result.coverage\" loading=\"loading\" bucket=\"bucket\" query=\"query\"\n" +
    "      class=\"voffset2\" on-update-criteria=\"onUpdateCriteria\"></coverage-result-table>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search-result-graphics-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-graphics-template.html",
    "<div class=\"tab-pane\" ng-class=\"{active: activeDisplay.graphics}\">\n" +
    "  <graphics-result on-update-criteria=\"onUpdateCriteria\" result=\"result.graphics\" loading=\"loading\" class=\"voffset2\"></graphics-result>\n" +
    "</div>");
}]);

angular.module("search/views/search-result-list-dataset-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-list-dataset-template.html",
    "<div class=\"tab-pane\" ng-show=\"options.datasets.showSearchTab\" ng-class=\"{active: activeTarget.datasets}\">\n" +
    "  <span ng-if=\"resultTabsOrder.length === 1\">{{'datasets' | translate}} ({{result.list.datasetResultDto.totalHits}})</span>\n" +
    "  <span search-result-pagination\n" +
    "      class=\"pull-right\"\n" +
    "      target=\"QUERY_TARGETS.DATASET\"\n" +
    "      total-hits=\"result.list.datasetResultDto.totalHits\"\n" +
    "      on-change=\"onPaginate\"></span>\n" +
    "  <span class=\"clearfix\"></span>\n" +
    "  <datasets-result-table loading=\"loading\" on-update-criteria=\"onUpdateCriteria\"\n" +
    "      summaries=\"result.list.datasetResultDto['obiba.mica.DatasetResultDto.result'].datasets\"></datasets-result-table>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search-result-list-network-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-list-network-template.html",
    "<div class=\"tab-pane\" ng-show=\"options.networks.showSearchTab\" ng-class=\"{active: activeTarget.networks}\">\n" +
    "  <span ng-if=\"resultTabsOrder.length === 1\">{{'networks' | translate}} ({{result.list.networkResultDto.totalHits}})</span>\n" +
    "  <span search-result-pagination\n" +
    "      class=\"pull-right\"\n" +
    "      target=\"QUERY_TARGETS.NETWORK\"\n" +
    "      total-hits=\"result.list.networkResultDto.totalHits\"\n" +
    "      on-change=\"onPaginate\"></span>\n" +
    "  <span class=\"clearfix\"></span>\n" +
    "  <networks-result-table loading=\"loading\" on-update-criteria=\"onUpdateCriteria\"\n" +
    "      summaries=\"result.list.networkResultDto['obiba.mica.NetworkResultDto.result'].networks\"></networks-result-table>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search-result-list-study-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-list-study-template.html",
    "<div class=\"tab-pane\" ng-show=\"options.studies.showSearchTab\" ng-class=\"{'active': activeTarget.studies}\">\n" +
    "  <span ng-if=\"resultTabsOrder.length === 1\">{{'studies' | translate}} ({{result.list.studyResultDto.totalHits}})</span>\n" +
    "  <span search-result-pagination\n" +
    "      class=\"pull-right\"\n" +
    "      target=\"QUERY_TARGETS.STUDY\"\n" +
    "      total-hits=\"result.list.studyResultDto.totalHits\"\n" +
    "      on-change=\"onPaginate\"></span>\n" +
    "  <span class=\"clearfix\"></span>\n" +
    "  <studies-result-table loading=\"loading\" on-update-criteria=\"onUpdateCriteria\"\n" +
    "      summaries=\"result.list.studyResultDto['obiba.mica.StudyResultDto.result'].summaries\"></studies-result-table>\n" +
    "</div>");
}]);

angular.module("search/views/search-result-list-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-list-template.html",
    "<div class=\"tab-pane\" ng-class=\"{active: activeDisplay.list}\">\n" +
    "  <ul class=\"nav nav-pills voffset2\" ng-if=\"resultTabsOrder.length > 1\">\n" +
    "    <li role=\"presentation\" ng-repeat=\"res in resultTabsOrder\"\n" +
    "        ng-class=\"{active: activeTarget[targetTypeMap[res]]}\"\n" +
    "        ng-if=\"options[targetTypeMap[res]].showSearchTab\"><a href\n" +
    "        ng-click=\"selectTarget(targetTypeMap[res])\">{{targetTypeMap[res] | translate}} ({{result.list[res +\n" +
    "      'ResultDto'].totalHits}})</a></li>\n" +
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
    "<div class=\"tab-pane\" ng-show=\"options.variables.showSearchTab\" ng-class=\"{active: activeTarget.variables}\">\n" +
    "  <span ng-if=\"resultTabsOrder.length === 1\">{{'variables' | translate}} ({{result.list.variableResultDto.totalHits}})</span>\n" +
    "  <span search-result-pagination\n" +
    "      class=\"pull-right\"\n" +
    "      target=\"QUERY_TARGETS.VARIABLE\"\n" +
    "      total-hits=\"result.list.variableResultDto.totalHits\"\n" +
    "      on-change=\"onPaginate\"></span>\n" +
    "  <span class=\"clearfix\"></span>\n" +
    "  <variables-result-table loading=\"loading\"\n" +
    "      summaries=\"result.list.variableResultDto['obiba.mica.DatasetVariableResultDto.result'].summaries\"></variables-result-table>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search-result-panel-template.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search-result-panel-template.html",
    "<div>\n" +
    "  <ul class=\"nav nav-tabs voffset2\" ng-if=\"searchTabsOrder.length > 1\">\n" +
    "    <li role=\"presentation\" ng-repeat=\"tab in searchTabsOrder\" ng-class=\"{active: activeDisplay[tab]}\"><a href\n" +
    "        ng-click=\"selectDisplay(tab)\">{{ options[ tab + 'Label'] | translate}}</a></li>\n" +
    "  </ul>\n" +
    "  <div class=\"tab-content\">\n" +
    "    <ng-include include-replace ng-repeat=\"tab in searchTabsOrder\"\n" +
    "        src=\"'search/views/search-result-' + tab + '-template.html'\"></ng-include>\n" +
    "  </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/search.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search.html",
    "<div>\n" +
    "  <!--<h2 translate>search</h2>-->\n" +
    "  <div ng-if=\"headerTemplateUrl\" ng-include=\"headerTemplateUrl\"></div>\n" +
    "\n" +
    "  <obiba-alert id=\"SearchController\"></obiba-alert>\n" +
    "\n" +
    "  <!-- Nav tabs -->\n" +
    "  <ul class=\"nav nav-tabs\" role=\"tablist\" ng-if=\"tabs && tabs.length>1\">\n" +
    "    <li ng-repeat=\"tab in tabs\" role=\"presentation\" ng-class=\"{ active: tab === lang }\"><a href role=\"tab\"\n" +
    "      ng-click=\"setLocale(tab)\">{{'language.' + tab | translate}}</a></li>\n" +
    "  </ul>\n" +
    "\n" +
    "  <!-- Classifications region -->\n" +
    "  <div class=\"{{tabs && tabs.length>1 ? 'tab-content voffset4' : ''}}\">\n" +
    "    <!--<div>-->\n" +
    "    <div class=\"row\">\n" +
    "      <div class=\"col-md-3\"></div>\n" +
    "      <div class=\"col-md-6\">\n" +
    "        <script type=\"text/ng-template\" id=\"customTemplate.html\">\n" +
    "          <a ng-if=\"match.model.id\">\n" +
    "            <span title=\"{{match.model.target + '-classifications' | translate}}\">\n" +
    "              <i class=\"{{'i-obiba-' + match.model.target}}\"></i>\n" +
    "            </span>\n" +
    "            <span\n" +
    "              uib-popover-html=\"match.model.itemDescription | uibTypeaheadHighlight:query\"\n" +
    "              popover-title=\"{{match.model.itemTitle}}\"\n" +
    "              popover-placement=\"bottom\"\n" +
    "              popover-trigger=\"mouseenter\"\n" +
    "              ng-bind-html=\"match.model.itemTitle | uibTypeaheadHighlight:query\">\n" +
    "            </span>\n" +
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
    "      <div class=\"col-md-3\"></div>\n" +
    "      <div class=\"col-md-6\">\n" +
    "        <ul class=\"nav nav-pills\">\n" +
    "          <li ng-repeat=\"t in taxonomyNav\" title=\"{{t.locale.description.text}}\">\n" +
    "            <a href ng-click=\"showTaxonomy(t.target, t.name)\" ng-if=\"!t.terms\">{{t.locale.title.text}}</a>\n" +
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
    "          </li>\n" +
    "        </ul>\n" +
    "      </div>\n" +
    "    </div>\n" +
    "    <div id=\"taxonomies\" class=\"collapse\">\n" +
    "      <div ng-include=\"'search/views/classifications/taxonomies-view.html'\" class=\"voffset2\"></div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "  <!-- Search criteria region -->\n" +
    "  <div class=\"row\">\n" +
    "    <div class=\"col-md-12\">\n" +
    "      <div criteria-root item=\"search.criteria\" query=\"search.query\" on-remove=\"removeCriteriaItem\"\n" +
    "        on-refresh=\"refreshQuery\"></div>\n" +
    "    </div>\n" +
    "  </div>\n" +
    "\n" +
    "\n" +
    "  <!-- Results region -->\n" +
    "  <div class=\"voffset3\" ng-if=\"search.query\">\n" +
    "    <result-panel display=\"search.display\"\n" +
    "      type=\"search.type\"\n" +
    "      bucket=\"search.bucket\"\n" +
    "      query=\"search.executedQuery\"\n" +
    "      result=\"search.result\"\n" +
    "      loading=\"search.loading\"\n" +
    "      on-update-criteria=\"onUpdateCriteria\"\n" +
    "      on-type-changed=\"onTypeChanged\"\n" +
    "      on-bucket-changed=\"onBucketChanged\"\n" +
    "      on-paginate=\"onPaginate\"\n" +
    "      search-tabs-order=\"searchTabsOrder\"\n" +
    "      result-tabs-order=\"resultTabsOrder\"\n" +
    "      lang=\"lang\"></result-panel>\n" +
    "  </div>\n" +
    "</div>");
}]);
