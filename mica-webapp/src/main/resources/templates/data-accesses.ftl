<!-- Macros -->
<#include "libs/header.ftl">
<#include "libs/data-access.ftl"/>

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
    <@header title="data-access-requests"/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <p><@message "data-access-requests-callout"/></p>
        </div>

        <div class="row">
          <div class="col-lg-12">
            <div class="card card-primary card-outline">
              <div class="card-header">
                <h3 class="card-title">Data Access Requests</h3>
                <div class="float-right">
                  <button type="button" class="btn btn-primary ml-4" data-toggle="modal" data-target="#modal-add">
                    <i class="fas fa-plus"></i> <@message "new-data-access-request"/>
                  </button>
                </div>
              </div>
              <div class="card-body">
                <#assign isAdministrator = (user.roles?seq_contains("mica-administrator") || user.roles?seq_contains("mica-data-access-officer"))/>
                <table id="dars" class="table table-bordered table-striped">
                  <thead>
                  <tr>
                    <th>ID</th>
                    <#if isAdministrator>
                      <th>Applicant</th>
                    </#if>
                    <th>Title</th>
                    <th>Last Update</th>
                    <th>Submission Date</th>
                    <#if accessConfig.amendmentsEnabled>
                      <th>Pending Amendments</th>
                      <th>Total Amendments</th>
                    </#if>
                    <th>Status</th>
                  </tr>
                  </thead>
                  <tbody>
                  <#list dars as dar>
                    <tr>
                      <td><a href="../data-access/${dar.id}">${dar.id}</a></td>
                      <#if isAdministrator>
                        <td>${applicants[dar.applicant].fullName}</td>
                      </#if>
                      <td>${dar.title!""}</td>
                      <td class="moment-datetime">${dar.lastUpdate.toString(datetimeFormat)}</td>
                      <td class="moment-datetime"><#if dar.submitDate??>${dar.submitDate.toString(datetimeFormat)}</#if></td>
                      <#if accessConfig.amendmentsEnabled>
                        <td>${dar.pendingAmendments}</td>
                        <td>${dar.totalAmendments}</td>
                      </#if>
                      <td><i class="fas fa-circle text-${statusColor(dar.status.toString())}"></i> <@message dar.status.toString()/></td>
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

  <!-- Confirm addition modal -->
  <div class="modal fade" id="modal-add">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title">Confirm Creation</h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <p>Please confirm that you want to create a new data access request.</p>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
          <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="micajs.dataAccess.create()">Confirm</button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->

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
