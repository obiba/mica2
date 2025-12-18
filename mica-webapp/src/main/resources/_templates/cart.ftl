<!-- Macros -->
<#include "models/list.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "sets.cart.title"/></title>
</head>
<body id="cart-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper" id="query-vue-container">
    <!-- Content Header (Page header) -->
    <div class="content-header bg-info mb-4">
      <div class="container">
        <div class="row mb-2">
          <div class="col-sm-6">
            <h1 class="m-0"><@message "sets.cart.title"/></h1>
          </div><!-- /.col -->
          <div class="col-sm-6">

          </div><!-- /.col -->
        </div><!-- /.row -->
      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content-header -->

    <!-- Confirm delete variables modal -->
    <div class="modal fade" id="modal-delete">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "cart-confirm-deletion-title"/></h4>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p id="delete-all-message"><@message "cart-confirm-deletion-text"/></p>
            <p id="delete-selected-message" style="display: none;"><@message "cart-selected-confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
                    onclick="VariablesSetService.deleteVariables('${sets.variablesCart.id}', variablesCartStorage.getSelections(), function() { window.location.replace('${contextPath}/cart?type=variables'); })"><@message "confirm"/>
            </button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->

    <!-- Confirm delete studies modal -->
    <div class="modal fade" id="modal-delete-studies">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "cart-confirm-deletion-title"/></h4>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p id="delete-all-studies-message"><@message "studies-cart-confirm-deletion-text"/></p>
            <p id="delete-selected-studies-message" style="display: none;"><@message "studies-cart-selected-confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
                    onclick="StudiesSetService.deleteStudies('${sets.studiesCart.id}', studiesCartStorage.getSelections(), function() { window.location.replace('${contextPath}/cart?type=studies'); })"><@message "confirm"/>
            </button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->

    <!-- Confirm delete networks modal -->
    <div class="modal fade" id="modal-delete-networks">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "cart-confirm-deletion-title"/></h4>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p id="delete-all-networks-message"><@message "networks-cart-confirm-deletion-text"/></p>
            <p id="delete-selected-networks-message" style="display: none;"><@message "networks-cart-selected-confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
                    onclick="NetworksSetService.deleteNetworks('${sets.networksCart.id}', networksCartStorage.getSelections(), function() { window.location.replace('${contextPath}/cart?type=networks'); })"><@message "confirm"/>
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
      <div class="container">

        <#if (variablesCartEnabled && studiesCartEnabled) || (variablesCartEnabled && networksCartEnabled) || (studiesCartEnabled && networksCartEnabled)>
          <div>
            <ul id="cart-tabs" class="nav nav-pills mb-3">
              <#if variablesCartEnabled>
                <li class="nav-item">
                  <a id="variables-tab" @click="onTabChanged('variables')" class="nav-link <#if showCartType == "variables">active</#if>"
                     <#if showCartType == "variables">href="#tab_variables" data-bs-toggle="tab"<#else>href="${contextPath}/cart?type=variables"</#if>>
                      <@message "variables"/>
                    <span class="badge badge-light">${sets.variablesCart.count}</span>
                  </a>
                </li>
              </#if>
              <#if studiesCartEnabled>
                <li class="nav-item">
                  <a id="studies-tab" @click="onTabChanged('studies')" class="nav-link <#if showCartType == "studies">active</#if>"
                     <#if showCartType == "studies">href="#tab_studies" data-bs-toggle="tab"<#else>href="${contextPath}/cart?type=studies"</#if>>
                      <@message "studies"/>
                    <span class="badge badge-light">${sets.studiesCart.count}</span>
                  </a>
                </li>
              </#if>
              <#if networksCartEnabled>
                <li class="nav-item">
                  <a id="networks-tab" @click="onTabChanged('networks')" class="nav-link <#if showCartType == "networks">active</#if>"
                     <#if showCartType == "networks">href="#tab_networks" data-bs-toggle="tab"<#else>href="${contextPath}/cart?type=networks"</#if>>
                      <@message "networks"/>
                    <span class="badge badge-light">${sets.networksCart.count}</span>
                  </a>
                </li>
              </#if>
            </ul>
          </div>
        </#if>

        <#if variablesCartEnabled && showCartType == "variables">
          <div id="tab_variables">

            <div id="cart-callout" class="callout callout-info">
              <p><@message "sets.cart.help"/></p>
            </div>

            <div class="card card-info card-outline">
            <div class="card-header">
              <#if sets.variablesCart?? && sets.variablesCart.count gt 0>

                <#if config.harmonizationDatasetEnabled && config.studyDatasetEnabled>
                  <div class="float-start">
                    <ul class="nav nav-pills" id="studyClassNameChoice" :title="countWarning ? '<@message "count-warning"/>' : ''" role="tablist" v-cloak>
                      <li class="nav-item" role="presentation">
                        <a class="nav-link active" id="individual-tab" @click="onStudyClassNameChange('Study')" href="" data-bs-toggle="tab" role="tab" aria-controls="home" aria-selected="true"><@message "individual-search"/> <span :class="{ 'badge-warning': countWarning, 'badge-light': !countWarning }" class="badge right">{{individualSubCount}}</span></a>
                      </li>
                      <li class="nav-item" role="presentation">
                        <a class="nav-link" id="harmonization-tab" @click="onStudyClassNameChange('HarmonizationStudy')" href="" data-bs-toggle="tab" role="tab" aria-controls="profile" aria-selected="false"><@message "harmonization-search"/> <span :class="{ 'badge-warning': countWarning, 'badge-light': !countWarning }" class="badge right">{{harmonizationSubCount}}</span></a>
                      </li>
                    </ul>
                  </div>
                <#else>
                  <h3 class="card-title mt-2"><@message "variables"/></h3>
                </#if>

                <div class="float-end">
                  <#if canCreateDAR>
                    <#if user??>
                      <button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#modal-add">
                        <i class="fa-solid fa-plus"></i> <@message "new-data-access-request"/>
                      </button>
                    <#else>
                      <button type="button" onclick="location.href='${contextPath}/signin?redirect=${contextPath}/cart';" class="btn btn-primary">
                        <i class="fa-solid fa-plus"></i> <@message "sign-in-new-data-access-request"/>
                      </button>
                    </#if>
                  </#if>

                  <#if sets.variablesLists?? && sets.variablesLists?size lt maxNumberOfSets>
                    <div class="btn-group ms-2" role="group">
                      <button type="button" class="btn btn-success dropdown-toggle" data-bs-toggle="dropdown">
                          <@message "sets.add.button.set-label"/>
                        <span class="badge badge-light selection-count"></span>
                      </button>
                      <div id="listsDropdownMenu" class="dropdown-menu" style="min-width: 24em;">
                        <div class="px-3 py-3">

                          <div class="form-group mb-0">
                            <div class="input-group">
                              <input id="newVariableSetName" type="text" class="form-control" placeholder="<@message "sets.add.modal.create-new"/>">
                              <div class="input-group-append">
                                <button id="addToNewSetButton" class="btn btn-success disabled" type="button" onclick="onClickAddToNewSet()">
                                  <i class="fa fa-plus"></i> <@message "global.add"/>
                                </button>
                              </div>
                            </div>
                          </div>

                        </div>
                        <div id="add-set-divider" class="dropdown-divider" <#if !user.variablesLists?has_content>style="display: none"</#if>></div>
                        <div id="add-set-choices">
                          <#if sets.variablesLists?has_content>
                            <#list sets.variablesLists as variableList>
                              <#if !variableList.locked>
                                <button type="button" class="dropdown-item"
                                        onclick="onClickAddToSet('${variableList.id}', '${variableList.name}')">
                                    ${listName(variableList)} <#if variableList.name?starts_with("dar:")>[<@message "data-access-request"/>]</#if> <span class="badge badge-light float-end">${variableList.identifiers?size}</span>
                                </button>
                              </#if>
                            </#list>
                          </#if>
                        </div>
                      </div>
                    </div>
                  </#if>

                  <#if showCartDownload>
                    <#if showCartViewDownload>
                      <div class="btn-group ms-2" role="group">
                        <button type="button" class="btn btn-primary dropdown-toggle" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                          <i class="fa-solid fa-download"></i> <@message "download"/>
                        </button>
                        <div class="dropdown-menu">
                          <a class="dropdown-item" href="${contextPath}/ws/variables/set/${sets.variablesCart.id}/documents/_report?locale=${.locale}" download><@message "download-cart-export"/></a>
                          <a class="dropdown-item" href="${contextPath}/ws/variables/set/${sets.variablesCart.id}/documents/_opal" download><@message "download-cart-views"/></a>
                        </div>
                      </div>
                    <#else>
                      <a href="${contextPath}/ws/variables/set/${sets.variablesCart.id}/documents/_report?locale=${.locale}" download class="btn btn-primary ms-2">
                        <i class="fa-solid fa-download"></i> <@message "download-cart-export"/>
                      </a>
                    </#if>
                  </#if>
                  <button id="delete-all" type="button" class="btn btn-danger ms-2" data-bs-toggle="modal" data-bs-target="#modal-delete">
                    <i class="fa-solid fa-trash"></i> <@message "delete"/> <span class="badge badge-light selection-count"></span>
                  </button>
                </div>
              <#else>
                <h3 class="card-title mt-2"><@message "variables"/></h3>
              </#if>
            </div>
            <div class="card-body">
              <#if sets.variablesCart?? && sets.variablesCart.count gt 0>
                <div id="loadingSet" class="spinner-border spinner-border-sm" role="status"></div>
                <div class="mt-3 text-muted" v-show="!hasResult"><@message "empty-list"/></div>
                <div v-show="hasResult" class="clearfix mb-3">
                  <div class="float-start">
                    <div class="d-inline-block">
                      <div class="d-inline-flex">
                        <span class="me-2">
                          <select class="custom-select" id="obiba-page-size-selector-top"></select>
                        </span>
                        <nav id="obiba-pagination-top" aria-label="Top pagination" class="mt-0">
                          <ul class="pagination mb-0"></ul>
                        </nav>
                      </div>
                    </div>
                  </div>
                  <#if config.setsSearchEnabled>
                    <div class="float-end">
                      <a class="btn btn-info ms-2" v-if="studyClassName != 'HarmonizationStudy'" href="${contextPath}/individual-search#lists?type=variables&query=variable(in(Mica_variable.sets,${sets.variablesCart.id})),study(in(Mica_study.className,Study))">
                        <i class="fa-solid fa-search"></i>
                      </a>
                      <a class="btn btn-info ms-2" v-else href="${contextPath}/harmonization-search#lists?type=variables&query=variable(in(Mica_variable.sets,${sets.variablesCart.id})),study(in(Mica_study.className,HarmonizationStudy))">
                        <i class="fa-solid fa-search"></i>
                      </a>
                    </div>
                  </#if>
                </div>
                <variables-result v-show="hasResult" :show-checkboxes="hasCheckboxes"></variables-result>
              <#else>
                <div class="text-muted"><@message "sets.cart.no-variables"/></div>
              </#if>
            </div>
          </div>

          </div>
        </#if>
        <#if studiesCartEnabled && showCartType == "studies">
          <div id="tab_studies">

            <div id="cart-callout" class="callout callout-info">
              <p><@message "sets.cart.studies-help"/></p>
            </div>

            <div class="card card-info card-outline">
            <div class="card-header">
              <#if sets.studiesCart?? && sets.studiesCart.count gt 0>
                <#if config.harmonizationDatasetEnabled>
                  <div class="float-start">
                    <ul class="nav nav-pills" id="studyClassNameChoice" :title="countWarning ? '<@message "count-warning"/>' : ''" role="tablist" v-cloak>
                      <li class="nav-item" role="presentation">
                        <a class="nav-link active" id="individual-tab" @click="onStudyClassNameChange('Study')" href="" data-bs-toggle="tab" role="tab" aria-controls="home" aria-selected="true"><@message "individual-search"/> <span :class="{ 'badge-warning': countWarning, 'badge-light': !countWarning }" class="badge right">{{individualSubCount}}</span></a>
                      </li>
                      <li class="nav-item" role="presentation">
                        <a class="nav-link" id="harmonization-tab" @click="onStudyClassNameChange('HarmonizationStudy')" href="" data-bs-toggle="tab" role="tab" aria-controls="profile" aria-selected="false"><@message "harmonization-search"/> <span :class="{ 'badge-warning': countWarning, 'badge-light': !countWarning }" class="badge right">{{harmonizationSubCount}}</span></a>
                      </li>
                    </ul>
                  </div>
                <#else>
                  <h3 class="card-title mt-2"><@message "studies"/></h3>
                </#if>
                <div class="float-end">
                  <#if studiesCompareEnabled>
                    <button type="button" class="btn btn-info ms-2" onclick="onCompareStudies()">
                      <i class="fa-solid fa-grip-lines-vertical"></i> <@message "compare"/> <span class="badge badge-light studies-selection-count"></span>
                    </button>
                  </#if>
                  <#if showCartDownload>
                    <a href="${contextPath}/ws/studies/set/${sets.studiesCart.id}/documents/_report?locale=${.locale}" download class="btn btn-primary ms-2">
                      <i class="fa-solid fa-download"></i> <@message "download-cart-export"/>
                    </a>
                  </#if>
                  <button id="delete-all" type="button" class="btn btn-danger ms-2" data-bs-toggle="modal" data-bs-target="#modal-delete-studies">
                    <i class="fa-solid fa-trash"></i> <@message "delete"/> <span class="badge badge-light studies-selection-count"></span>
                  </button>
                </div>
              <#else>
                <h3 class="card-title mt-2"><@message "studies"/></h3>
              </#if>
            </div>
            <div class="card-body">
              <#if sets.studiesCart?? && sets.studiesCart.count gt 0>
                <div id="loadingSet" class="spinner-border spinner-border-sm" role="status"></div>
                <div class="mt-3 text-muted" v-show="!hasResult"><@message "empty-list"/></div>
                <div v-show="hasResult" class="clearfix mb-3">
                  <div class="float-start">
                    <div class="d-inline-block">
                      <div class="d-inline-flex">
                        <span class="me-2">
                          <select class="custom-select" id="obiba-page-size-selector-top"></select>
                        </span>
                        <nav id="obiba-pagination-top" aria-label="Top pagination" class="mt-0">
                          <ul class="pagination mb-0"></ul>
                        </nav>
                      </div>
                    </div>
                  </div>
                  <#if config.setsSearchEnabled>
                    <div class="float-end">
                      <a class="btn btn-info ms-2" v-if="studyClassName != 'HarmonizationStudy'" href="${contextPath}/individual-search#lists?type=studies&query=study(and(in(Mica_study.sets,${sets.studiesCart.id}),in(Mica_study.className,Study)))">
                        <i class="fa-solid fa-search"></i>
                      </a>
                      <a class="btn btn-info ms-2" v-else href="${contextPath}/harmonization-search#lists?type=studies&query=study(and(in(Mica_study.sets,${sets.studiesCart.id}),in(Mica_study.className,HarmonizationStudy)))">
                        <i class="fa-solid fa-search"></i>
                      </a>
                    </div>
                  </#if>
                </div>
                <studies-result v-show="hasResult"></studies-result>
              <#else>
                <div class="text-muted"><@message "sets.cart.no-studies"/></div>
              </#if>
            </div>
          </div>

        </div>
        </#if>
        <#if networksCartEnabled && showCartType == "networks">
          <div id="tab_networks">

            <div id="cart-callout" class="callout callout-info">
              <p><@message "sets.cart.networks-help"/></p>
            </div>

            <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title mt-2"><@message "networks"/></h3>
              <#if sets.networksCart?? && sets.networksCart.count gt 0>
                <div class="float-end">
                  <#if networksCompareEnabled>
                    <button type="button" class="btn btn-info ms-2" onclick="onCompareNetworks()">
                      <i class="fa-solid fa-grip-lines-vertical"></i> <@message "compare"/> <span class="badge badge-light networks-selection-count"></span>
                    </button>
                  </#if>
                  <#if showCartDownload>
                    <a href="${contextPath}/ws/networks/set/${sets.networksCart.id}/documents/_report?locale=${.locale}" download class="btn btn-primary ms-2">
                      <i class="fa-solid fa-download"></i> <@message "download-cart-export"/>
                    </a>
                  </#if>
                  <button id="delete-all" type="button" class="btn btn-danger ms-2" data-bs-toggle="modal" data-bs-target="#modal-delete-networks">
                    <i class="fa-solid fa-trash"></i> <@message "delete"/> <span class="badge badge-light networks-selection-count"></span>
                  </button>
                </div>
              </#if>
            </div>
            <div class="card-body">
              <#if sets.networksCart?? && sets.networksCart.count gt 0>
                <div id="loadingSet" class="spinner-border spinner-border-sm" role="status"></div>
                <div class="mt-3 text-muted" v-show="!hasResult"><@message "empty-list"/></div>
                <div v-show="hasResult" class="clearfix mb-3">
                  <div class="float-start">
                    <div class="d-inline-block">
                      <div class="d-inline-flex">
                        <span class="me-2">
                          <select class="custom-select" id="obiba-page-size-selector-top"></select>
                        </span>
                        <nav id="obiba-pagination-top" aria-label="Top pagination" class="mt-0">
                          <ul class="pagination mb-0"></ul>
                        </nav>
                      </div>
                    </div>
                  </div>
                  <#if config.setsSearchEnabled>
                    <div class="float-end">
                      <a class="btn btn-info ms-2" href="${contextPath}/search#lists?type=networks&query=network(in(Mica_network.sets,${sets.networksCart.id}))">
                        <i class="fa-solid fa-search"></i>
                      </a>
                    </div>
                  </#if>
                </div>
                <networks-result v-show="hasResult"></networks-result>
              <#else>
                <div class="text-muted"><@message "sets.cart.no-networks"/></div>
              </#if>
            </div>
          </div>

        </div>
        </#if>

      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <#if canCreateDAR && showCartType == "variables">
    <!-- Confirm DAR addition modal -->
    <div class="modal fade" id="modal-add">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "confirm-creation"/></h4>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p><@message "confirm-data-access-request-creation-from-cart"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal" onclick="DataAccessService.createFromCart()"><@message "confirm"/></button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->
  </#if>

  <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#if canCreateDAR>
  <#include "libs/data-access-scripts.ftl">
</#if>
<#include "libs/document-cart-scripts.ftl">

</body>
</html>
