'use strict';

var attachmentTemplate = '<button ng-hide="{{disabled}}" type="button" class="btn btn-primary btn-xs" aria-hidden="true" ngf-multiple="{{multiple}}" ngf-select\n' +
  '        ngf-change="onFileSelect($files)" translate>upload\n' +
  '</button>\n' +
  '\n' +
  '<table ng-show="files.length" class="table table-bordered table-striped">\n' +
  '  <thead>\n' +
  '  <tr>\n' +
  '    <th translate>name</th>\n' +
  '    <th class="col-xs-2"><span class="pull-right" translate>date</span></th>\n' +
  '    <th translate>size</th>\n' +
  '    <th ng-show="deleteAttachments" translate>actions</th>\n' +
  '  </tr>\n' +
  '  </thead>\n' +
  '  <tbody>\n' +
  '  <tr ng-repeat="file in files">\n' +
  '    <td>\n' +
  '      {{file.fileName}}\n' +
  '      <uib-progressbar ng-show="file.showProgressBar" class="progress-striped" value="file.progress">\n' +
  '        {{file.progress}}%\n' +
  '      </uib-progressbar>\n' +
  '    </td>\n' +
  '    <td>\n' +
  '      <span class="pull-right" ng-if="file.timestamps" title="{{ file.timestamps.created | amDateFormat: \'lll\' }}">{{file.timestamps.created | amCalendar }}</span>\n' +
  '    </td>\n' +
  '    <td style="width:1%;">\n' +
  '        <span class="pull-right" style="white-space: nowrap;">\n' +
  '          {{file.size | bytes}}\n' +
  '        </span>\n' +
  '    </td>\n' +
  '    <td style="width:20px;" ng-show="deleteAttachments">\n' +
  '      <a ng-show="file.id" ng-click="deleteFile(file.id)" class="action">\n' +
  '        <i class="fa fa-trash-alt"></i>\n' +
  '      </a>\n' +
  '      <a ng-show="file.tempId" ng-click="deleteTempFile(file.tempId)" class="action">\n' +
  '        <i class="fa fa-trash-alt"></i>\n' +
  '      </a>\n' +
  '    </td>\n' +
  '  </tr>\n' +
  '  </tbody>\n' +
  '</table>';

var attachmentListTemplate = '<div>\n' +
  '  <span ng-if="!hasAttachments && emptyMessage"><em>{{emptyMessage}}</em></span>\n' +
  '  <table ng-if="hasAttachments" class="table table-bordered table-striped" >\n' +
  '    <thead>\n' +
  '    <tr>\n' +
  '      <th translate>name</th>\n' +
  '      <th class="col-xs-2"><span class="pull-right" translate>date</span></th>\n' +
  '      <th translate>size</th>\n' +
  '    </tr>\n' +
  '    </thead>\n' +
  '    <tbody>\n' +
  '    <tr ng-repeat="attachment in attachments">\n' +
  '      <th>\n' +
  '        <a target="_self" ng-href="{{attachment.href}}"\n' +
  '           download="{{attachment.fileName}}">{{attachment.fileName}}\n' +
  '        </a>\n' +
  '      </th>\n' +
  '      <td><span class="pull-right" ng-if="attachment.timestamps" title="{{ attachment.timestamps.created | amDateFormat: \'lll\' }}">{{attachment.timestamps.created | amCalendar }}</span></td>\n' +
  '      <td style="width:1%;">\n' +
  '        <span class="pull-right" style="white-space: nowrap;">\n' +
  '          {{attachment.size | bytes}}\n' +
  '        </span>\n' +
  '      </td>\n' +
  '    </tr>\n' +
  '    </tbody>\n' +
  '  </table>\n' +
  '\n' +
  '</div>';

angular.module('ngObibaMica', ['ngResource', 'pascalprecht.translate'])
  .config(['$translateProvider', function($translateProvider) {
    const ngTrLangKey = document.cookie.match("(^|[^;]+)\\s*NG_TRANSLATE_LANG_KEY\\s*=\\s*([^;]+)")
    const language = ngTrLangKey ? decodeURIComponent(ngTrLangKey.pop()).replace(/["']/g,"") : "";
    $translateProvider
      .useStaticFilesLoader({
        prefix: contextPath + '/ws/config/i18n/',
        suffix: '.json'
      })
      .fallbackLanguage('en')
      .useSanitizeValueStrategy('escaped')
      .use(language)
    ;
  }])
  .service('LocaleStringUtils', ['$filter', function ($filter) {
    this.translate = function (key, args) {

      function buildMessageArguments(args) {
        if (args && args instanceof Array) {
          var messageArgs = {};
          args.forEach(function (arg, index) {
            messageArgs['arg' + index] = arg;
          });

          return messageArgs;
        }

        return {};
      }

      return $filter('translate')(key, buildMessageArguments(args));
    };
  }])
  .factory('TempFileResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('TempFileResource'), {}, {
        'get': {method: 'GET'},
        'delete': {method: 'DELETE'}
      });
    }])
  .filter('bytes', function () {
    return function (bytes) {
      return bytes === null || typeof bytes === 'undefined' ? '' : filesize(bytes);
    };
  })

  .directive('attachmentList', [function () {
    return {
      restrict: 'E',
      scope: {
        hrefBuilder: '=',
        files: '=',
        emptyMessage: '='
      },
      template: attachmentListTemplate,
      link: function (scope) {
        scope.attachments = [];
        scope.hrefBuilder = scope.hrefBuilder || function (a) {
          return a.id;
        };
        scope.hasAttachments = false;

        scope.$watch('files', function (val) {
          scope.attachments = [];
          if (val) {
            scope.hasAttachments = val.length > 0;
            scope.attachments = val.map(function (a) {
              var temp = angular.copy(a);
              temp.href = scope.hrefBuilder(a);
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
        files: '=',
        disabled: '=',
        onError: '=',
        deleteAttachments: '<'
      },
      template: attachmentTemplate,
      controller: 'AttachmentCtrl'
    };
  }])
  .controller('AttachmentCtrl', ['$scope', '$timeout', '$log', 'Upload', 'TempFileResource', 'ngObibaMicaUrl',
    function ($scope, $timeout, $log, Upload, TempFileResource, ngObibaMicaUrl) {
      if ($scope.deleteAttachments === undefined || $scope.deleteAttachments === null) {
        $scope.deleteAttachments = true;
      }
      var uploadFile = function (file) {
        $scope.files = $scope.files || [];

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
                attachment.timestamps = {created: new Date()};
                // wait for 1 second before hiding progress bar
                $timeout(function () {
                  attachment.showProgressBar = false;
                }, 1000);
              }
            );
          })
          .error(function (response) {
            $log.error('File upload failed: ', JSON.stringify(response, null, 2));
            var index = $scope.files.indexOf(attachment);
            if (index !== -1) {
              $scope.files.splice(index, 1);
            }

            if ($scope.onError) {
              $scope.onError(attachment);
            }
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

function NgObibaMicaUrlProvider() {
  var registry = {
    'TempFileUploadResource': MicaService.normalizeUrl('/ws/files/temp'),
    'TempFileResource': MicaService.normalizeUrl('/ws/files/temp/:id'),
    'SchemaFormAttachmentDownloadResource': MicaService.normalizeUrl('/ws/:path/form/attachments/:attachmentName/:attachmentId/_download')
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

angular.module('formModule', ['schemaForm', 'hc.marked', 'angularMoment', 'schemaForm-datepicker', 'schemaForm-timepicker', 'schemaForm-datetimepicker', 'sfObibaSelectionTree', 'sfObibaFileUpload', 'ngFileUpload', 'ui.bootstrap'])
  .config(['$provide', function ($provide) {
    $provide.provider('ngObibaMicaUrl', NgObibaMicaUrlProvider);
  }])
  .config(['schemaFormProvider',
    function (schemaFormProvider) {
      schemaFormProvider.postProcess(function (form) {
        form.filter(function (e) {
          return e.hasOwnProperty('wordLimit');
        }).forEach(function (e) {
          e.$validators = {
            wordLimitError: function (value) {
              if (angular.isDefined(value) && value !== null) {
                var wordMax = e.wordLimit;
                var wordMin = 0;
                var range = e.wordLimit.split(':');
                if (range.length > 1) {
                  wordMin = range[0];
                  wordMax = range[1];
                }
                var wordCount = (value.match(/\S+/g) || []).length;
                if (wordCount < parseInt(wordMin)) {
                  return false;
                }
                if (wordCount > parseInt(wordMax)) {
                  return false;
                }
              }
              return true;
            }
          };
        });
        form.filter(function (e) {
          return e.hasOwnProperty('wordMax');
        }).forEach(function (e) {
          e.$validators = {
            wordMaxError: function (value) {
              if (angular.isDefined(value) && value !== null) {
                var wordCount = (value.match(/\S+/g) || []).length;
                if (wordCount > parseInt(e.wordMax)) {
                  return false;
                }
              }
              return true;
            }
          };
        });
        form.filter(function (e) {
          return e.hasOwnProperty('wordMin');
        }).forEach(function (e) {
          e.$validators = {
            wordMinError: function (value) {
              if (angular.isDefined(value) && value !== null) {
                var wordCount = (value.match(/\S+/g) || []).length;
                if (wordCount < parseInt(e.wordMin)) {
                  return false;
                }
              }
              return true;
            }
          };
        });
        return form;
      });
    }])
  .factory(['markedProvider', function (markedProvider) {
    markedProvider.setOptions({
      gfm: true,
      tables: true,
      sanitize: true
    });
  }])
  .controller('FormController', ['$scope', '$filter', '$translate', function ($scope, $filter, $translate) {
    $scope.forms = {};

    const langKey = 'language.'+ $translate.use();
    $translate(['required', langKey], {}).then(function(translations) {
      const languages = {};
      languages[$translate.use()] = translations[langKey];

      $scope.schema = formSchema;
      $scope.form = formDefinition;
      $scope.model = formModel;
      $scope.sfOptions = {
        validationMessage: {
          '302': translations['required']
        },
        formDefaults: {
          languages: languages
        }
      }
    });

    function isRequestFormValid() {
      if ($scope.forms.requestForm.$valid) {
        return true;
      }
      if ($scope.forms.requestForm.$error.schemaForm) {
        return false;
      }
      if ($scope.forms.requestForm.$error["tv4-302"]) {
        // remove false positive
        const errs = $scope.forms.requestForm.$error["tv4-302"]
          .filter(err => !Array.isArray(err.$viewValue));
        return errs.length === 0;
      }
      return true;
    }

    $scope.validate = function () {
      $scope.$broadcast('schemaFormValidate');
      // check if the form is valid
      if (isRequestFormValid()) {
        MicaService.toastSuccess(formMessages.validationSuccess);
      } else {
        // an invalid form can be saved with warning
        MicaService.toastWarning(formMessages.validationError);
      }
    };
    $scope.save = function (id, type, aId) {
      var url = '/ws/data-access-request/' + id + '/model';
      var redirect = '/data-access-form/' + id;
      if (type && aId) {
        url = '/ws/data-access-request/' + id + '/' + type + '/' + aId + '/model';
        redirect = '/data-access-' + type + '-form/' + aId;
      }
      axios.put(
        MicaService.normalizeUrl(url),
        $scope.model,
        {headers: {'Content-Type': 'application/json'}}
      ).then(function() {
        // check if the form was valid
        MicaService.redirect(MicaService.normalizeUrl(redirect));
      }).catch(response =>  {
        MicaService.toastError(formMessages.errorOnSave);
        console.dir(response);
      });
    };
    $scope.submit = function (id, type, aId) {
      $scope.$broadcast('schemaFormValidate');
      // check if the form is valid
      if (isRequestFormValid()) {
        DataAccessService.submit(id, type, aId);
      } else {
        // an invalid form cannot be submitted
        MicaService.toastError(formMessages.validationErrorOnSubmit);
      }
    };
    $scope.approveAgreement = function (id, aId) {
      $scope.$broadcast('schemaFormValidate');
      // check if the form is valid
      if (isRequestFormValid()) {
        DataAccessService.approve(id, 'agreement', aId);
      } else {
        // an invalid form cannot be submitted
        MicaService.toastError(formMessages.validationErrorOnSubmit);
      }
    };
  }]);
