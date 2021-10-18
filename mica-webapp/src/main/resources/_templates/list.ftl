<!-- Macros -->
<#include "models/list.ftl">

<!DOCTYPE html>
<html lang="${.lang}" xmlns:v-bind="http://www.w3.org/1999/xhtml">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${listName(set)}</title>
</head>
<body id="list-page" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed">
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
              <#assign variableListActiveClass = (variableList.id == set.id)?then("active", "") />
              <li class="nav-item">
                <a class="nav-link ${variableListActiveClass}" href="${contextPath}/list/${variableList.id}">
                  <i class="far fa-circle nav-icon"></i>
                  <p>
                    <span title="${listName(variableList)} <#if variableList.name?starts_with("dar:")>[<@message "data-access-request"/>]</#if>">
                        ${listName(variableList)?truncate_c(20, "...")}
                    </span>
                    <#if variableList.name?starts_with("dar:")>
                      <i class="fas fa-link ml-2"></i>
                    <#else>
                      <span></span>
                    </#if>
                    <#if variableList.locked>
                      <i class="fas fa-lock ml-2"></i>
                    </#if>
                    <span class="badge badge-light right">${variableList.identifiers?size}</span>
                  </p>
                </a>
              </li>
            </#list>
          </ul>
        <#else >
          <span class="pl-2 text-white-50">
            <em>
              <@message "no-personal-list"/>
            </em>
          </span>
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
        <div class="row">
          <div class="col-sm-12">
            <h1 class="m-0 float-left">
              <span class="text-white-50"><@message "search.list"/> /</span>
                ${listName(set)}
                <#if set.name?starts_with("dar:")>
                  [<@message "data-access-request"/>]
                </#if>
            </h1>
            <#if !set.locked || isAdministrator>
              <button type="button" class="btn btn-danger ml-4" data-toggle="modal" data-target="#modal-delete-list">
                <i class="fas fa-trash"></i> <@message "delete"/>
              </button>
            </#if>
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
            <h4 class="modal-title"><@message "cart-confirm-deletion-title"/> (${listName(set)})</h4>
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
            <h4 class="modal-title"><@message "cart-confirm-deletion-title"/> (${listName(set)})</h4>
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

        <#if set.name?starts_with("dar:")>
          <div id="dar-list-callout" class="callout callout-info">
            <p><@message "sets.set.dar-help"/></p>
            <btn class="btn btn-info" onclick="location.href='${contextPath}/data-access-form/${set.name?replace("dar:", "")}'">
              <i class="fas fa-link"></i>
                <#if set.name?matches(".+-F\\d+$")>
                  <@message "data-access-feasibility"/>
                <#elseif set.name?matches(".+-A\\d+$")>
                  <@message "data-access-amendment"/>
                <#else>
                  <@message "data-access-request"/>
                </#if>
            </btn>
          </div>
        <#else>
          <div id="list-callout" class="callout callout-info">
            <p><@message "sets.set.help"/></p>
          </div>
        </#if>

        <div class="card card-info card-outline">
          <div class="card-header">
            <h3 class="card-title"><@message "variables"/></h3>
            <div class="float-right">
              <button class="btn btn-success ml-2" onclick="onVariablesCartAdd('${set.id}')">
                <i class="fas fa-cart-plus"></i> <@message "sets.cart.add-to-cart"/>
              </button>
              <#if showCartDownload>
                <#if showCartViewDownload>
                  <div class="btn-group" role="group">
                    <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                      <i class="fas fa-download"></i> <@message "download"/>
                    </button>
                    <div class="dropdown-menu">
                      <a class="dropdown-item" href="${contextPath}/ws/variables/set/${set.id}/documents/_report?locale=${.locale}" download><@message "download-cart-report"/></a>
                      <a class="dropdown-item" href="${contextPath}/ws/variables/set/${set.id}/documents/_export" download><@message "download-cart-ids"/></a>
                      <a class="dropdown-item" href="${contextPath}/ws/variables/set/${set.id}/documents/_opal" download><@message "download-cart-views"/></a>
                    </div>
                  </div>
                <#else>
                  <a href="${contextPath}/ws/variables/set/${set.id}/documents/_report?locale=${.locale}" download class="btn btn-primary ml-2">
                    <i class="fas fa-download"></i> <@message "download"/>
                  </a>
                </#if>
              </#if>
              <#if !set.locked || isAdministrator>
                <button id="delete-all" type="button" class="btn btn-danger ml-2" data-toggle="modal" data-target="#modal-delete">
                  <i class="fas fa-trash"></i> <@message "delete"/> <span class="badge badge-light selection-count"></span>
                </button>
              </#if>
              <#if config.setsSearchEnabled>
                <a class="btn btn-info ml-2" href="${contextPath}/search#lists?type=variables&query=variable(in(Mica_variable.sets,${set.id}))">
                  <i class="fas fa-search"></i>
                </a>
              </#if>
            </div>
          </div>
          <div class="card-body">
            <#if set?? && set.identifiers?size gt 0>
              <div id="loadingSet" class="spinner-border spinner-border-sm" role="status"></div>
              <div class="table-responsive">
                <table id="setTable" class="table table-striped">
                  <thead>
                  <tr>
                    <th>
                    <#if !set.locked || isAdministrator>
                      <i class="far fa-square"></i>
                    </#if>
                    </th>
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
              <div class="text-muted"><@message "empty-list"/></div>
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
