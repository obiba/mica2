<!DOCTYPE html>
<html lang="${.lang}">
<head>
    <#include "libs/head.ftl">
  <title>${config.name!""} | Data Access Feasibility ${dar.id}</title>
</head>
<body class="hold-transition sidebar-mini">
<!-- Site wrapper -->
<div class="wrapper">

  <!-- Navbar -->
    <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Sidebar -->
    <#include "libs/data-access-sidebar.ftl">
  <!-- /.sidebar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <section class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row">
          <div class="col-sm-6">
            <h1 class="m-0">
              <span class="text-white-50">Data Access Feasibility /</span> ${dar.id}
            </h1>
          </div>
          <div class="col-sm-6">
              <#include "libs/data-access-breadcrumb.ftl">
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
              <@message "data-access-feasibility-callout"/>
            </p>
          </div>
        </div>
        <!-- /.col-12 -->
      </div>
      <!-- /.row -->

      <div class="row">
        <div class="col-lg-6">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title">To Do</h3>
            </div>
            <div class="card-body">
              TODO
            </div>
          </div>
        </div>
        <!-- /.col-6 -->
        <div class="col-6">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title">Recent History</h3>
            </div>
            <div class="card-body">
              TODO
            </div>
            <div class="card-footer">
              <a href="${pathPrefix}/data-access-history/${dar.id}">More info <i class="fas fa-arrow-circle-right"></i></a>
            </div>
          </div>
        </div>
        <!-- /.col-6 -->
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
<script>
    $('#feasibility-menu').addClass('active').attr('href', '#')
</script>
</body>
</html>
