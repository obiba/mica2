<!DOCTYPE html>
<html lang="en" xmlns:v-bind="http://www.w3.org/1999/xhtml">
<head>
  <title>Example | Data Access ${dar.id}</title>
  <#include "libs/head.ftl">
  <style>
    .visible-print {
      display: none;
    }
    .has-error {
      color: #E74C3C;
    }
    .has-error input {
      border-color: #E74C3C;
    }
  </style>
</head>
<body ng-app="formModule" class="hold-transition sidebar-mini">
<!-- Site wrapper -->
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Main Sidebar Container -->
  <aside class="main-sidebar sidebar-dark-primary">
    <!-- Brand Logo -->
    <a href="${pathPrefix}/bower_components/admin-lte/index3.html" class="brand-link bg-white">
      <img src="${pathPrefix}/bower_components/admin-lte/dist/img/AdminLTELogo.png"
           alt="Logo"
           class="brand-image img-circle elevation-3"
           style="opacity: .8">
      <span class="brand-text font-weight-light">Example</span>
    </a>

    <!-- Sidebar -->
    <div class="sidebar">
      <!-- Sidebar user (optional) -->
      <div class="user-panel mt-3 pb-3 mb-3 d-flex">
        <div class="info">
          <a href="#" class="d-block">${dar.applicant} - ${dar.status}</a>
        </div>
      </div>

      <!-- Sidebar Menu -->
      <#include "libs/data-access-sidebar.ftl">
      <!-- /.sidebar-menu -->
    </div>
    <!-- /.sidebar -->
  </aside>

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <section class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row">
          <div class="col-sm-6">
            <h1 class="m-0">
              <span class="text-white-50">Data Access Form /</span> ${dar.id}
            </h1>
          </div>
          <div class="col-sm-6">

          </div>
        </div>
      </div><!-- /.container-fluid -->
    </section>

    <!-- Main content -->
    <section class="content">

      <div class="row">
        <div class="col-12">
          <div class="callout callout-info">
            <p>
              This the main data access request form.
            </p>
          </div>

          <div class="row">
            <div class="col-lg-12">
              <div class="card card-primary card-outline">
                <div class="card-header">
                  <h3 class="card-title">Form</h3>
                </div>
                <div class="card-body">
                  <div ng-controller="FormController">
                    <form>
                      <div sf-schema="schema" sf-form="form" sf-model="model"></div>
                      <div class="mt-5">
                        <input type="submit" class="btn btn-primary" value="Submit">
                        <button type="button" class="btn btn-success" ng-click="validate()">Validate</button>
                        <button type="button" class="btn btn-default" ng-click="goBack()">Cancel</button>
                      </div>

                    </form>
                  </div>
                </div>
              </div>
            </div>
          </div>


        </div>
        <!-- /.col-12 -->
      </div>
      <!-- /.row -->

    </section>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <#include "libs/footer.ftl">

  <!-- Control Sidebar -->
  <aside class="control-sidebar control-sidebar-dark">
    <!-- Control sidebar content goes here -->
  </aside>
  <!-- /.control-sidebar -->
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<script src="${pathPrefix}/bower_components/angular/angular.js"></script>
<script src="${pathPrefix}/bower_components/objectpath/lib/ObjectPath.js"></script>
<script src="${pathPrefix}/bower_components/marked/lib/marked.js"></script>
<script src="${pathPrefix}/bower_components/tv4/tv4.js"></script>
<script src="${pathPrefix}/bower_components/angular-sanitize/angular-sanitize.js"></script>
<script src="${pathPrefix}/bower_components/angular-marked/dist/angular-marked.js"></script>
<script src="${pathPrefix}/bower_components/angular-strap/dist/angular-strap.js"></script>
<script src="${pathPrefix}/bower_components/angular-strap/dist/angular-strap.tpl.js"></script>
<script src="${pathPrefix}/bower_components/moment/moment.js"></script>
<script src="${pathPrefix}/bower_components/moment/min/locales.min.js"></script>
<script src="${pathPrefix}/bower_components/angular-moment/angular-moment.js"></script>
<script src="${pathPrefix}/bower_components/angular-resource/angular-resource.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form/dist/schema-form.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form/dist/bootstrap-decorator.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form-bootstrap/bootstrap-decorator.min.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form-ui-ace/bootstrap-ui-ace.min.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form-datetimepicker/schema-form-date-time-picker.min.js"></script>
<script src="${pathPrefix}/bower_components/sf-localized-string/dist/sf-localized-string.min.js"></script>
<script src="${pathPrefix}/bower_components/sf-obiba-file-upload/dist/sf-obiba-file-upload.min.js"></script>
<script src="${pathPrefix}/bower_components/sf-checkboxgroup/dist/sf-checkboxgroup.min.js"></script>
<script src="${pathPrefix}/bower_components/filesize/lib/filesize.min.js"></script>

<script src="${pathPrefix}/bower_components/angular-bootstrap/ui-bootstrap-tpls.js"></script>
<script src="${pathPrefix}/bower_components/ng-file-upload/ng-file-upload.js"></script>


<script src="${pathPrefix}/bower_components/sf-typeahead/dist/sf-typeahead.min.js"></script>
<script src="${pathPrefix}/bower_components/sf-obiba-countries-ui-select/dist/sf-obiba-countries-ui-select.js"></script>
<script src="${pathPrefix}/bower_components/sf-radio-group-collection/dist/sf-radio-group-collection.js"></script>

<script>

    var attachmentTemplate = "<button ng-hide=\"{{disabled}}\" type=\"button\" class=\"btn btn-primary btn-xs\" aria-hidden=\"true\" ngf-multiple=\"{{multiple}}\" ngf-select\n" +
            "        ngf-change=\"onFileSelect($files)\" translate>file.upload.button\n" +
            "</button>\n" +
            "\n" +
            "<table ng-show=\"files.length\" class=\"table table-bordered table-striped\">\n" +
            "  <thead>\n" +
            "  <tr>\n" +
            "    <th translate>data-access-request.default.documents.title</th>\n" +
            "    <th class=\"col-xs-2\"><span class=\"pull-right\" translate>file.upload.date</span></th>\n" +
            "    <th translate>size</th>\n" +
            "    <th ng-show=\"deleteAttachments\" translate>actions</th>\n" +
            "  </tr>\n" +
            "  </thead>\n" +
            "  <tbody>\n" +
            "  <tr ng-repeat=\"file in files\">\n" +
            "    <td>\n" +
            "      {{file.fileName}}\n" +
            "      <uib-progressbar ng-show=\"file.showProgressBar\" class=\"progress-striped\" value=\"file.progress\">\n" +
            "        {{file.progress}}%\n" +
            "      </uib-progressbar>\n" +
            "    </td>\n" +
            "    <td>\n" +
            "      <span class=\"pull-right\" ng-if=\"file.timestamps\" title=\"{{ file.timestamps.created | amDateFormat: 'lll' }}\">{{file.timestamps.created | amCalendar }}</span>\n" +
            "    </td>\n" +
            "    <td style=\"width:1%;\">\n" +
            "        <span class=\"pull-right\" style=\"white-space: nowrap;\">\n" +
            "          {{file.size | bytes}}\n" +
            "        </span>\n" +
            "    </td>\n" +
            "    <td style=\"width:20px;\" ng-show=\"deleteAttachments\">\n" +
            "      <a ng-show=\"file.id\" ng-click=\"deleteFile(file.id)\" class=\"action\">\n" +
            "        <i class=\"fa fa-trash-o\"></i>\n" +
            "      </a>\n" +
            "      <a ng-show=\"file.tempId\" ng-click=\"deleteTempFile(file.tempId)\" class=\"action\">\n" +
            "        <i class=\"fa fa-trash-o\"></i>\n" +
            "      </a>\n" +
            "    </td>\n" +
            "  </tr>\n" +
            "  </tbody>\n" +
            "</table>";

    var attachmentListTemplate = "<div>\n" +
            "  <span ng-if=\"!hasAttachments && emptyMessage\"><em>{{emptyMessage}}</em></span>\n" +
            "  <table ng-if=\"hasAttachments\" class=\"table table-bordered table-striped\" >\n" +
            "    <thead>\n" +
            "    <tr>\n" +
            "      <th translate>data-access-request.default.documents.title</th>\n" +
            "      <th class=\"col-xs-2\"><span class=\"pull-right\" translate>file.upload.date</span></th>\n" +
            "      <th translate>size</th>\n" +
            "    </tr>\n" +
            "    </thead>\n" +
            "    <tbody>\n" +
            "    <tr ng-repeat=\"attachment in attachments\">\n" +
            "      <th>\n" +
            "        <a target=\"_self\" ng-href=\"{{attachment.href}}\"\n" +
            "           download=\"{{attachment.fileName}}\">{{attachment.fileName}}\n" +
            "        </a>\n" +
            "      </th>\n" +
            "      <td><span class=\"pull-right\" ng-if=\"attachment.timestamps\" title=\"{{ attachment.timestamps.created | amDateFormat: 'lll' }}\">{{attachment.timestamps.created | amCalendar }}</span></td>\n" +
            "      <td style=\"width:1%;\">\n" +
            "        <span class=\"pull-right\" style=\"white-space: nowrap;\">\n" +
            "          {{attachment.size | bytes}}\n" +
            "        </span>\n" +
            "      </td>\n" +
            "    </tr>\n" +
            "    </tbody>\n" +
            "  </table>\n" +
            "\n" +
            "</div>";

    angular.module('ngObibaMica', ['ngResource'])
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

      .directive('attachmentList', [function() {
        return {
          restrict: 'E',
          scope: {
            hrefBuilder: '=',
            files: '=',
            emptyMessage: '='
          },
          template: attachmentListTemplate,
          link: function(scope) {
            scope.attachments = [];
            scope.hrefBuilder = scope.hrefBuilder || function(a) { return a.id; };
            scope.hasAttachments = false;

            scope.$watch('files', function(val) {
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
            onError:'=',
            deleteAttachments: '<'
          },
          template: attachmentTemplate,
          controller: 'AttachmentCtrl'
        };
      }])
      .controller('AttachmentCtrl', ['$scope', '$timeout', '$log', 'Upload', 'TempFileResource', 'ngObibaMicaUrl',
        function ($scope, $timeout, $log, Upload, TempFileResource, ngObibaMicaUrl) {
          if($scope.deleteAttachments === undefined || $scope.deleteAttachments === null){
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
                                $timeout(function () { attachment.showProgressBar = false; }, 1000);
                              }
                      );
                    })
                    .error(function(response){
                      $log.error('File upload failed: ', JSON.stringify(response, null, 2));
                      var index = $scope.files.indexOf(attachment);
                      if (index !== -1) {
                        $scope.files.splice(index, 1);
                      }

                      if ($scope.onError){
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
        'TempFileUploadResource': '../ws/files/temp',
        'TempFileResource': '../ws/files/temp/:id'
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

    $('#form-menu').addClass('active').attr('href', '#');
    let formSchema = ${form.schema!"{}"};
    let formDefinition = ${form.definition!"['*']"};
    let formModel = ${form.model!"{}"};
    angular.module('formModule', ['schemaForm', 'hc.marked', 'angularMoment', 'schemaForm-datepicker', 'schemaForm-timepicker', 'schemaForm-datetimepicker', 'sfObibaFileUpload', 'ngFileUpload', 'ui.bootstrap'])
        .config(['$provide', function($provide) {
          $provide.provider('ngObibaMicaUrl', NgObibaMicaUrlProvider);
        }])
        .factory(['markedProvider', function(markedProvider) {
            markedProvider.setOptions({
                gfm: true,
                tables: true,
                sanitize: true
            });
        }])
        .controller('FormController', ['$scope', function ($scope) {
            $scope.schema = formSchema;
            $scope.form = formDefinition;
            $scope.model = formModel;
            $scope.validate = function() {
              $scope.$broadcast('schemaFormValidate');
            };
        }]);
</script>
</body>
</html>
