<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <title>${config.name!""} | Data Accesses</title>
  <#include "libs/head.ftl">
</head>
<body class="hold-transition layout-top-nav layout-navbar-fixed">
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
            <h1 class="m-0">Data Access</h1>
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
              This the list of the data access requests.
          </p>
        </div>

        <div class="row">
          <div class="col-lg-12">
            <div class="card card-primary card-outline">
              <div class="card-header">
                <h3 class="card-title">Data Access Requests</h3>
                <div class="float-right"><a href="#" class="btn btn-primary"><i class="fas fa-plus"></i> Add</a></div>
              </div>
              <div class="card-body">
                <table id="dars" class="table table-bordered table-striped">
                  <thead>
                  <tr>
                    <th>ID</th>
                    <th>Applicant</th>
                    <th>Title</th>
                    <th>Last Update</th>
                    <th>Submission Date</th>
                    <th>Pending Amendments</th>
                    <th>Total Amendments</th>
                    <th>Status</th>
                  </tr>
                  </thead>
                  <tbody>
                  <#list dars as dar>
                    <tr>
                      <td><a href="../data-access/${dar.id}">${dar.id}</a></td>
                      <td>${dar.applicant}</td>
                      <td>${dar.title!""}</td>
                      <td>${dar.lastUpdate.toString("yyyy-MM-dd hh:mm")}</td>
                      <td>${dar.submissionDate!""}</td>
                      <td>${dar.pendingAmendments}</td>
                      <td>${dar.totalAmendments}</td>
                      <td><@message dar.status.toString()/></td>
                    </tr>
                  </#list>
                  </tbody>
                </table>
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
<!-- page script -->
<script>
    $(function () {
        $("#dars").DataTable(dataTablesSortSearchOpts);
    });
</script>

</body>
</html>
