<!DOCTYPE html>
<html lang="${.lang}" xmlns:v-bind="http://www.w3.org/1999/xhtml">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${set.name}</title>
</head>
<body id="search-page" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed">
<!-- Site wrapper -->
<div class="wrapper">
  <!-- Navbar -->
  <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Main Sidebar Container -->
  <aside class="main-sidebar sidebar-dark-primary">
    <!-- Brand Logo -->
    <a href="${portalLink}" class="brand-link bg-white">
      <img src="${brandImageSrc}"
           alt="Logo"
           class="brand-image ${brandImageClass}"
           style="opacity: .8">
      <span class="brand-text ${brandTextClass}">
        <#if brandTextEnabled>
          ${config.name!""}
        <#else>&nbsp;
        </#if>
      </span>
    </a>

    <!-- Sidebar -->
    <div class="sidebar">
      <!-- Sidebar user (optional) -->
      <div class="user-panel mt-3 pb-3 mb-3 d-flex">
        <div class="info">
          <a href="#" class="d-block"><@message "sets.set.title"/></a>
        </div>
      </div>

      <!-- Sidebar Menu -->
      <nav class="mt-2">
        <#if user?? && user.variablesLists?? && user.variablesLists?size gt 0>
          <ul class="nav nav-pills nav-sidebar flex-column">
            <#list user.variablesLists as variableList>
              <li class="nav-item">
                <a class="nav-link" href="${contextPath}/list/${variableList.id}">
                  <i class="far fa-circle nav-icon"></i>
                  <p>${variableList.name}</p>
                </a>
              </li>
            </#list>
          </ul>
        </#if>
      </nav>
      <!-- /.sidebar-menu -->
    </div>
    <!-- /.sidebar -->
  </aside>

  <!-- Control Sidebar -->
  <aside class="control-sidebar control-sidebar-dark">
    <!-- Control sidebar content goes here -->
  </aside>
  <!-- /.control-sidebar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <div class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row mb-2">
          <div class="col-sm-6 float-left">
            <h1 class="m-0">
              <span class="text-white-50"><@message "sets.set.title"/> /</span> ${set.name}
              <button type="button" class="btn btn-info btn-sm mb-1" data-toggle="modal" data-target="#modal-delete-list" title="<@message "delete"/>"><i class="fas fa-trash"></i></button>
            </h1>
          </div><!-- /.col -->
          <div class="col-sm-6">

          </div><!-- /.col -->
        </div><!-- /.row -->
      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content-header -->

    <!-- Confirm delete modal -->
    <div class="modal fade" id="modal-delete">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "cart-confirm-deletion-title"/> (${set.name})</h4>
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p id="delete-all-message"><@message "list-confirm-deletion-text"/></p>
            <p id="delete-selected-message" style="display: none;"><@message "list-selected-confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal"
                    onclick="VariablesSetService.deleteVariables('${set.id}', variablesCartStorage.getSelections(), function() { window.location.reload(); })"><@message "confirm"/>
            </button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->

    <!-- Confirm delete list modal -->
    <div class="modal fade" id="modal-delete-list">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "cart-confirm-deletion-title"/> (${set.name})</h4>
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p><@message "list-confirm-complete-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal"
                    onclick="VariablesSetService.deleteSet('${set.id}', function () { window.location.replace('${contextPath}/lists'); })"><@message "confirm"/>
            </button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->

    <!-- Main content -->
    <div class="content">
      <div class="container-fluid">
        <div class="callout callout-info">
          <p><@message "sets.set.help"/></p>
        </div>

        <div class="card card-info card-outline">
          <div class="card-header">
            <h3 class="card-title"><@message "variables"/></h3>
            <div class="float-right">
              <#if showCartDownload>
                <#if showCartViewDownload>
                  <div class="btn-group" role="group">
                    <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                      <i class="fas fa-download"></i> <@message "download"/>
                    </button>
                    <div class="dropdown-menu">
                      <a class="dropdown-item" href="../ws/variables/set/${set.id}/documents/_export" download><@message "download-cart-ids"/></a>
                      <a class="dropdown-item" href="../ws/variables/set/${set.id}/documents/_opal" download><@message "download-cart-views"/></a>
                    </div>
                  </div>
                <#else>
                  <a href="../ws/variables/set/${set.id}/documents/_opal" download class="btn btn-primary ml-2">
                    <i class="fas fa-download"></i> <@message "download"/>
                  </a>
                </#if>
              </#if>
              <button id="delete-all" type="button" class="btn btn-danger ml-2" data-toggle="modal" data-target="#modal-delete">
                <i class="fas fa-trash"></i> <@message "delete"/> <span id="selection-count" class="badge badge-light"></span>
              </button>
              <#if config.setsSearchEnabled>
                <a class="btn btn-info ml-2" href="${contextPath}/search#lists?type=variables&query=variable(in(Mica_variable.sets,${set.id}))">
                  <i class="fas fa-search"></i>
                </a>
              </#if>
            </div>
          </div>
          <div class="card-body">
            <#if set??>
              <div id="loadingSet" class="spinner-border spinner-border-sm" role="status"></div>
              <div>
                <table id="setTable" class="table table-striped">
                  <thead>
                  <tr>
                    <th><i class="far fa-square"></i></th>
                    <th></th>
                    <th><@message "name"/></th>
                    <th><@message "label"/></th>
                    <#if config.studyDatasetEnabled && config.harmonizationDatasetEnabled>
                      <th><@message "type"/></th>
                    </#if>
                    <#if !config.singleStudyEnabled>
                      <th><@message "study"/></th>
                    </#if>
                    <th><@message "dataset"/></th>
                  </tr>
                  </thead>
                  <tbody></tbody>
                </table>
              </div>
            <#else>
              <div class="text-muted"><@message "sets.set.no-variables-added"/></div>
            </#if>
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
<#if set??>
  <#include "libs/document-set-scripts.ftl">
</#if>
</body>
</html>
