<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <link rel="stylesheet" href="${assetsPath}/libs/node_modules/bootstrap-datepicker/dist/css/bootstrap-datepicker3.css"></link>
  <title>${config.name!""} | <@message "data-access-history"/> ${dar.id}</title>
</head>
<body id="data-access-history-page" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed sidebar-expand-lg">
<!-- Site wrapper -->
<div class="app-wrapper">

  <!-- Navbar -->
  <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Sidebar -->
  <#include "libs/data-access-sidebar.ftl">
  <!-- /.sidebar -->

  <!-- Content Wrapper. Contains page content -->
 <div class="app-main flex-fill">
    <!-- Content Header (Page header) -->
    <section class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row">
          <div class="col-sm-6">
            <h1 class="m-0">
              <span class="text-white-50"><@message "data-access-history"/> /</span> ${dar.id}
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
      <div class="container-fluid">

      <#if dar.archived>
        <div class="ribbon-wrapper ribbon-xl">
          <div class="ribbon bg-warning text-xl">
              <@message "archived-title"/>
          </div>
        </div>
      </#if>

      <#if dataAccessCalloutsEnabled>
        <div class="row">
          <div class="col-12">
            <div id="data-access-history-callout" class="callout callout-info">
              <p>
                <@message "data-access-history-callout"/>
              </p>
            </div>
          </div>
          <!-- /.col-12 -->
        </div>
        <!-- /.row -->
      </#if>

      <div class="row">
        <div class="col-sm-12 col-lg-6">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title"><@message "status-changes"/></h3>
            </div>
            <div class="card-body">
              <div class="table-responsive">
                <table id="status" class="table table-bordered table-striped">
                  <thead>
                  <tr>
                    <th><@message "form"/></th>
                    <th><@message "status"/></th>
                    <th><@message "author"/></th>
                    <th><@message "date"/></th>
                  </tr>
                  </thead>
                  <tbody>
                  <#list statusChangeEvents?reverse as event>
                    <tr>
                      <td>
                        <#if event.amendment>
                          <a href="${contextPath}/data-access-amendment-form/${event.form.id}"><i class="fa-solid fa-file-import"></i> ${event.form.id}</a>
                        <#elseif event.feasibility>
                          <a href="${contextPath}/data-access-feasibility-form/${event.form.id}"><i class="fa-regular fa-question-circle"></i> ${event.form.id}</a>
                        <#elseif event.preliminary>
                          <a href="${contextPath}/data-access-preliminary-form/${event.form.id}"><i class="fa-regular fa-play-circle"></i> ${event.form.id}</a>
                        <#elseif event.agreement>
                          <a href="${contextPath}/data-access-agreement-form/${event.form.id}"><i class="fa-solid fa-gavel"></i> ${event.form.id}</a>
                        <#else>
                          <a href="${contextPath}/data-access-form/${event.form.id}"><i class="fa-solid fa-book"></i> ${event.form.id}</a>
                        </#if>
                      </td>
                      <td><i class="fa-solid fa-circle text-${statusColor(event.status.toString())}"></i> <@message event.status.toString()/></td>
                      <td>${event.profile.fullName}</td>
                      <td data-sort="${event.date.toString()}" class="moment-datetime">${event.date.toString()}</td>
                    </tr>
                  </#list>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
        <!-- /.col-6 -->
        <#if isAdministrator || isDAO>
          <div class="col-sm-12 col-lg-6">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "actions-log"/></h3>
                <#if !dar.archived>
                  <div class="float-end">
                    <a href="#" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#modal-add">
                      <i class="fa-solid fa-plus"></i> <@message "add"/></a>
                  </div>
                </#if>
              </div>
              <div class="card-body">
                <div class="table-responsive">
                  <table id="actions" class="table table-bordered table-striped">
                    <thead>
                    <tr>
                      <th><@message "action"/></th>
                      <th><@message "author"/></th>
                      <th><@message "date"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list dar.actionLogHistory as act>
                      <tr>
                        <td>${act.action}</td>
                        <td>${act.author}</td>
                        <td data-sort="${act.changedOn.toString()}" class="moment-date">${act.changedOn.toString()}</td>
                      </tr>
                    </#list>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </#if>
        <!-- /.col-6 -->
      </div>
      <!-- /.row -->

      </div><!-- /.container-fluid -->
    </section>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <!-- Action addition modal -->
  <div class="modal fade" id="modal-add">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><@message "add-action"/></h4>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">

          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label><@message "action"/></label>
            <input type="text" id="action-text" class="form-control">
          </div>
          <div class="form-group">
            <label><@message "date"/></label>
            <div class="input-group">
              <input type="text" id="action-date" class="form-control">
              <span class="input-group-text"><i class="fa-solid fa-calendar-alt"></i></span>
            </div>
          </div>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><@message "cancel"/></button>
          <button type="button" class="btn btn-primary" id="action-add-submit"><@message "submit"/></button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->

  <#include "libs/footer.ftl">

  <!-- Control Sidebar -->
  <aside class="control-sidebar control-sidebar-dark">
    <!-- Control sidebar content goes here -->
  </aside>
  <!-- /.control-sidebar -->
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/data-access-scripts.ftl">
<!-- Datepicker -->
<script src="${assetsPath}/libs/node_modules/bootstrap-datepicker/dist/js/bootstrap-datepicker.min.js"></script>
<script>
    $(function () {
        $('#history-menu').addClass('active').attr('href', '#')
        $("#status").DataTable(dataTablesNoSortSearchOpts);
        $("#actions").DataTable(dataTablesNoSortSearchOpts);
        $('#action-date').datepicker({
          locale: '${.lang}',
          format: 'yyyy-mm-dd'
        });
        $('#action-add-submit').click(function() {
          let action = {
            text: $('#action-text').val(),
            date: $('#action-date').val()
          };
          DataAccessService.addAction('${dar.id}', action);
        });
    });
</script>
</body>
</html>
