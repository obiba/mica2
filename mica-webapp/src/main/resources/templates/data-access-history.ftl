<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | Data Access History ${dar.id}</title>
</head>
<body class="hold-transition sidebar-mini">
<!-- Site wrapper -->
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Main Sidebar Container -->
  <aside class="main-sidebar sidebar-dark-primary">
    <!-- Brand Logo -->
    <a href="../bower_components/admin-lte/index3.html" class="brand-link bg-white">
      <img src="../bower_components/admin-lte/dist/img/AdminLTELogo.png"
           alt="Logo"
           class="brand-image img-circle elevation-3"
           style="opacity: .8">
      <span class="brand-text font-weight-light">${config.name!""}</span>
    </a>

    <!-- Sidebar -->
    <div class="sidebar">
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
              <span class="text-white-50">Data Access History /</span> ${dar.id}
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
              This is the history of changes and actions of the data access request.
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
              <h3 class="card-title">Status Changes</h3>
            </div>
            <div class="card-body">
              <table id="status" class="table table-bordered table-striped">
                <thead>
                <tr>
                  <th>Status</th>
                  <th>Author</th>
                  <th>Date</th>
                </tr>
                </thead>
                <tbody>
                <#list dar.statusChangeHistory?reverse as chg>
                  <tr>
                    <td><@message chg.to.toString()/></td>
                    <td>${authors[chg.author].fullName}</td>
                    <td class="moment-datetime">${chg.changedOn.toString(datetimeFormat)}</td>
                  </tr>
                </#list>
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <!-- /.col-6 -->
        <div class="col-6">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title">Action Log</h3>
              <div class="float-right"><a href="#" class="btn btn-primary"><i class="fas fa-plus"></i> Add</a></div>
            </div>
            <div class="card-body">
              <table id="actions" class="table table-bordered table-striped">
                <thead>
                <tr>
                  <th>Action</th>
                  <th>Author</th>
                  <th>Date</th>
                </tr>
                </thead>
                <tbody>
                <#list dar.actionLogHistory as act>
                  <tr>
                    <td>${act.action}</td>
                    <td>${act.author}</td>
                    <td class="moment-datetime">${act.changedOn.toString(datetimeFormat)}</td>
                  </tr>
                </#list>
                </tbody>
              </table>
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
    $(function () {
        $('#history-menu').addClass('active').attr('href', '#')
        $(function () {
            $("#status").DataTable(dataTablesNoSortSearchOpts);
            $("#actions").DataTable(dataTablesNoSortSearchOpts);
        });
    });
</script>
</body>
</html>
