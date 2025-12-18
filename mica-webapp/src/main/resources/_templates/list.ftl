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
        <#if sets?? && sets.variablesLists?? && sets.variablesLists?size gt 0>
          <ul class="nav nav-pills nav-sidebar flex-column">
            <#list sets.variablesLists as variableList>
              <#assign variableListActiveClass = (variableList.id == set.id)?then("active", "") />
              <li class="nav-item">
                <a class="nav-link ${variableListActiveClass}" href="${contextPath}/list/${variableList.id}">
                  <i class="fa-regular fa-circle nav-icon"></i>
                  <p>
                    <span title="${listName(variableList)} <#if variableList.name?starts_with("dar:")>[<@message "data-access-request"/>]</#if>">
                        ${listName(variableList)?truncate_c(20, "...")}
                    </span>
                    <#if variableList.name?starts_with("dar:")>
                      <i class="fa-solid fa-link ms-2"></i>
                    <#else>
                      <span></span>
                    </#if>
                    <#if variableList.locked>
                      <i class="fa-solid fa-lock ms-2"></i>
                    </#if>
                    <span class="badge badge text-bg-light right">${variableList.identifiers?size}</span>
                  </p>
                </a>
              </li>
            </#list>
          </ul>
        <#else >
          <span class="ps-2 text-white-50">
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
  <div class="content-wrapper" id="query-vue-container">
    <!-- Content Header (Page header) -->
    <div class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row">
          <div class="col-sm-12">
            <h1 class="m-0 float-start">
              <span class="text-white-50"><@message "search.list"/> /</span>
                ${listName(set)}
                <#if set.name?starts_with("dar:")>
                  [<@message "data-access-request"/>]
                </#if>
            </h1>
            <#if !set.locked || isAdministrator>
              <button type="button" class="btn btn-danger ms-4" data-bs-toggle="modal" data-bs-target="#modal-delete-list">
                <i class="fa-solid fa-trash"></i> <@message "delete"/>
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
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p id="delete-all-message"><@message "list-confirm-deletion-text"/></p>
            <p id="delete-selected-message" style="display: none;"><@message "list-selected-confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
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
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p><@message "list-confirm-complete-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
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
            <button class="btn btn-info" onclick="location.href='${contextPath}/data-access-form/${set.name?replace("dar:", "")}'">
              <i class="fa-solid fa-link"></i>
                <#if set.name?matches(".+-F\\d+$")>
                  <@message "data-access-feasibility"/>
                <#elseif set.name?matches(".+-A\\d+$")>
                  <@message "data-access-amendment"/>
                <#else>
                  <@message "data-access-request"/>
                </#if>
            </button>
          </div>
        <#else>
          <div id="list-callout" class="callout callout-info">
            <p><@message "sets.set.help"/></p>
          </div>
        </#if>

        <div class="card card-info card-outline">
          <div class="card-header">

            <#if config.harmonizationDatasetEnabled && config.studyDatasetEnabled>
            <div class="float-start">
              <ul class="nav nav-pills" id="studyClassNameChoice" :title="countWarning ? '<@message "count-warning"/>' : ''" role="tablist" v-cloak>
                <li class="nav-item" role="presentation">
                  <a class="nav-link active" id="individual-tab" @click="onStudyClassNameChange('Study')" href="" data-bs-toggle="tab" role="tab" aria-controls="home" aria-selected="true"><@message "individual-search"/> <span :class="{ 'badge text-bg-warning': countWarning, 'badge text-bg-light': !countWarning }" class="badge right">{{individualSubCount}}</span></a>
                </li>
                <li class="nav-item" role="presentation">
                  <a class="nav-link" id="harmonization-tab" @click="onStudyClassNameChange('HarmonizationStudy')" href="" data-bs-toggle="tab" role="tab" aria-controls="profile" aria-selected="false"><@message "harmonization-search"/> <span :class="{ 'badge text-bg-warning': countWarning, 'badge text-bg-light': !countWarning }" class="badge right">{{harmonizationSubCount}}</span></a>
                </li>
              </ul>
            </div>
            </#if>

            <div class="float-end">
              <button class="btn btn-success ms-2" onclick="onVariablesCartAdd('${set.id}')">
                <i class="fa-solid fa-cart-plus"></i> <@message "sets.cart.add-to-cart"/>
              </button>
              <#if showCartDownload>
                <#if showCartViewDownload>
                  <div class="btn-group ms-2" role="group">
                    <button type="button" class="btn btn-primary dropdown-toggle" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                      <i class="fa-solid fa-download"></i> <@message "download"/>
                    </button>
                    <div class="dropdown-menu">
                      <a class="dropdown-item" href="${contextPath}/ws/variables/set/${set.id}/documents/_report?locale=${.locale}" download><@message "download-cart-report"/></a>
                      <a class="dropdown-item" href="${contextPath}/ws/variables/set/${set.id}/documents/_export" download><@message "download-cart-ids"/></a>
                      <a class="dropdown-item" href="${contextPath}/ws/variables/set/${set.id}/documents/_opal" download><@message "download-cart-views"/></a>
                    </div>
                  </div>
                <#else>
                  <a href="${contextPath}/ws/variables/set/${set.id}/documents/_report?locale=${.locale}" download class="btn btn-primary ms-2">
                    <i class="fa-solid fa-download"></i> <@message "download"/>
                  </a>
                </#if>
              </#if>
              <#if !set.locked || isAdministrator>
                <button id="delete-all" type="button" class="btn btn-danger ms-2" data-bs-toggle="modal" data-bs-target="#modal-delete">
                  <i class="fa-solid fa-trash"></i> <@message "delete"/> <span class="badge badge text-bg-light selection-count"></span>
                </button>
              </#if>
            </div>
          </div>
          <div class="card-body">
            <#if set?? && set.identifiers?size gt 0>
              <div id="loadingSet" class="spinner-border spinner-border-sm" role="status"></div>
              <div class="mt-3 text-muted" v-show="!hasResult"><@message "empty-list"/></div>
              <div v-show="hasResult" class="clearfix mb-3">
                <div class="float-start">
                  <div class="d-inline-block">
                    <div class="d-inline-flex">
                      <span class="me-2">
                        <select class="form-select" id="obiba-page-size-selector-top"></select>
                      </span>
                      <nav id="obiba-pagination-top" aria-label="Top pagination" class="mt-0">
                        <ul class="pagination mb-0"></ul>
                      </nav>
                    </div>
                  </div>
                </div>
                <#if config.setsSearchEnabled>
                  <div class="float-end">
                    <a class="btn btn-info ms-2" v-if="studyClassName != 'HarmonizationStudy'" href="${contextPath}/individual-search#lists?type=variables&query=variable(in(Mica_variable.sets,${set.id})),study(in(Mica_study.className,Study))">
                      <i class="fa-solid fa-search"></i>
                    </a>
                    <a class="btn btn-info ms-2" v-else href="${contextPath}/harmonization-search#lists?type=variables&query=variable(in(Mica_variable.sets,${set.id})),study(in(Mica_study.className,HarmonizationStudy))">
                      <i class="fa-solid fa-search"></i>
                    </a>
                  </div>
                </#if>
              </div>
              <variables-result v-show="hasResult" :show-checkboxes="hasCheckboxes"></variables-result>
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
