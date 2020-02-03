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
<script src="${pathPrefix}/bower_components/angular-schema-form/dist/schema-form.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form/dist/bootstrap-decorator.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form-bootstrap/bootstrap-decorator.min.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form-ui-ace/bootstrap-ui-ace.min.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form-datetimepicker/schema-form-date-time-picker.min.js"></script>
<script src="${pathPrefix}/bower_components/sf-localized-string/dist/sf-localized-string.min.js"></script>
<script src="${pathPrefix}/bower_components/sf-obiba-file-upload/dist/sf-obiba-file-upload.min.js"></script>
<script src="${pathPrefix}/bower_components/sf-checkboxgroup/dist/sf-checkboxgroup.min.js"></script>
<script src="${pathPrefix}/bower_components/sf-typeahead/dist/sf-typeahead.min.js"></script>
<script src="${pathPrefix}/bower_components/obiba-country-codes/dist/all.js"></script>
<script src="${pathPrefix}/bower_components/sf-obiba-countries-ui-select/dist/sf-obiba-countries-ui-select.js"></script>
<script src="${pathPrefix}/bower_components/sf-radio-group-collection/dist/sf-radio-group-collection.js"></script>

<script>
    $('#form-menu').addClass('active').attr('href', '#');
    let formSchema = ${form.schema!"{}"};
    let formDefinition = ${form.definition!"['*']"};
    let formModel = ${form.model!"{}"};
    angular.module('formModule', ['schemaForm', 'hc.marked'])
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
