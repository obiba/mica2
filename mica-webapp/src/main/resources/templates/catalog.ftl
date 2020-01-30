<!DOCTYPE html>
<html lang="en">
<head>
  <title>Example | Catalog</title>
  <#include "libs/head.ftl">

  <link rel="stylesheet" href="styles/famfamfam-flags.css">
  <link rel="stylesheet" href="../bower_components/ng-obiba/dist/css/ng-obiba.css" />
  <link rel="stylesheet" href="../bower_components/ng-obiba-mica/dist/css/ng-obiba-mica.css" />

</head>
<body ng-app="mica.search" ng-controller="SearchController" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <div class="content-header bg-info mb-4">
      <div class="container">
        <div class="row mb-2">
          <div class="col-sm-6">
            <h1 class="m-0">Search</h1>
          </div><!-- /.col -->
          <div class="col-sm-6">

          </div><!-- /.col -->
        </div><!-- /.row -->
      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <div class="callout callout-info">
          <p>
            Search the catalog of networks, studies, datasets and variables.
          </p>
        </div>

        <div class="container alert-fixed-position">
          <obiba-alert id="MainController"></obiba-alert>
        </div>

        <div class="alert-growl-container">
          <obiba-alert id="MainControllerGrowl"></obiba-alert>
        </div>


        <div class="row">
          <div class="col-lg-12">
            <div class="card card-info card-outline">
              <div class="card-body">
                <div ng-view=""></div>

              </div>
            </div>
          </div>
        </div>

      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/search-scripts.ftl">

</body>
</html>
