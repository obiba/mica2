<!DOCTYPE html>
<html lang="en" xmlns:v-bind="http://www.w3.org/1999/xhtml">
<head>
  <title>Example | Data Access ${dar.id}</title>
  <#include "libs/head.ftl">
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
                      <input type="submit" class="btn btn-primary" value="Submit">
                      <button type="button" class="btn btn-success" ng-click="validate()">Validate</button>
                      <button type="button" class="btn btn-default" ng-click="goBack()">Cancel</button>
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
<script src="${pathPrefix}/bower_components/angular-schema-form/dist/schema-form.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form/dist/bootstrap-decorator.js"></script>
<script src="${pathPrefix}/bower_components/angular-schema-form-bootstrap/bootstrap-decorator.min.js"></script>
<script src="https://unpkg.com/axios/dist/axios.min.js"></script>
<script>
    $('#form-menu').addClass('active').attr('href', '#');
    angular.module('formModule', ['schemaForm'])
        .controller('FormController', function($scope) {
            $scope.schema = {};
            $scope.form = [];
            $scope.model = {};
            axios
                .get('${pathPrefix}/ws/config/data-access-form')
                .then(response => {
                  let dto = response.data;
                  $scope.schema = JSON.parse(dto.schema);
                  $scope.form = JSON.parse(dto.definition);

                  console.dir($scope.schema);
                  console.dir($scope.form);
                  $scope.$broadcast('schemaFormRedraw')
                });
        });
</script>
</body>
</html>
