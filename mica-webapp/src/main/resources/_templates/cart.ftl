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
  <div class="content-wrapper">
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
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p id="delete-all-message"><@message "cart-confirm-deletion-text"/></p>
            <p id="delete-selected-message" style="display: none;"><@message "cart-selected-confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal"
                    onclick="VariablesSetService.deleteVariables('${user.variablesCart.id}', variablesCartStorage.getSelections(), function() { window.location.replace('${contextPath}/cart?type=variables'); })"><@message "confirm"/>
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
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p id="delete-all-studies-message"><@message "studies-cart-confirm-deletion-text"/></p>
            <p id="delete-selected-studies-message" style="display: none;"><@message "studies-cart-selected-confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal"
                    onclick="StudiesSetService.deleteStudies('${user.studiesCart.id}', studiesCartStorage.getSelections(), function() { window.location.replace('${contextPath}/cart?type=studies'); })"><@message "confirm"/>
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
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p id="delete-all-networks-message"><@message "networks-cart-confirm-deletion-text"/></p>
            <p id="delete-selected-networks-message" style="display: none;"><@message "networks-cart-selected-confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal"
                    onclick="NetworksSetService.deleteNetworks('${user.networksCart.id}', networksCartStorage.getSelections(), function() { window.location.replace('${contextPath}/cart?type=networks'); })"><@message "confirm"/>
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
                  <a id="variables-tab" class="nav-link <#if showCartType == "variables">active</#if>"
                     <#if showCartType == "variables">href="#tab_variables" data-toggle="tab"<#else>href="${contextPath}/cart?type=variables"</#if>>
                      <@message "variables"/>
                    <span class="badge badge-light">${user.variablesCart.count}</span>
                  </a>
                </li>
              </#if>
              <#if studiesCartEnabled>
                <li class="nav-item">
                  <a id="studies-tab" class="nav-link <#if showCartType == "studies">active</#if>"
                     <#if showCartType == "studies">href="#tab_studies" data-toggle="tab"<#else>href="${contextPath}/cart?type=studies"</#if>>
                      <@message "studies"/>
                    <span class="badge badge-light">${user.studiesCart.count}</span>
                  </a>
                </li>
              </#if>
              <#if networksCartEnabled>
                <li class="nav-item">
                  <a id="networks-tab" class="nav-link <#if showCartType == "networks">active</#if>"
                     <#if showCartType == "networks">href="#tab_networks" data-toggle="tab"<#else>href="${contextPath}/cart?type=networks"</#if>>
                      <@message "networks"/>
                    <span class="badge badge-light">${user.networksCart.count}</span>
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
              <h3 class="card-title"><@message "variables"/></h3>
                <#if user.variablesCart?? && user.variablesCart.count gt 0>
                  <div class="float-right">

                    <#if canCreateDAR>
                      <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#modal-add">
                        <i class="fas fa-plus"></i> <@message "new-data-access-request"/>
                      </button>
                    </#if>

                    <#if user.variablesLists?size lt maxNumberOfSets>
                      <div class="btn-group ml-2" role="group">
                        <button type="button" class="btn btn-success dropdown-toggle" data-toggle="dropdown">
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
                              <#if user.variablesLists?has_content>
                                  <#list user.variablesLists as variableList>
                                      <#if !variableList.locked>
                                        <button type="button" class="dropdown-item"
                                                onclick="onClickAddToSet('${variableList.id}', '${variableList.name}')">
                                            ${listName(variableList)} <#if variableList.name?starts_with("dar:")>[<@message "data-access-request"/>]</#if> <span class="badge badge-light float-right">${variableList.identifiers?size}</span>
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
                        <div class="btn-group ml-2" role="group">
                          <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            <i class="fas fa-download"></i> <@message "download"/>
                          </button>
                          <div class="dropdown-menu">
                            <a class="dropdown-item" href="${contextPath}/ws/variables/set/${user.variablesCart.id}/documents/_report?locale=${.locale}" download><@message "download-cart-export"/></a>
                            <a class="dropdown-item" href="${contextPath}/ws/variables/set/${user.variablesCart.id}/documents/_opal" download><@message "download-cart-views"/></a>
                          </div>
                        </div>
                      <#else>
                        <a href="${contextPath}/ws/variables/set/${user.variablesCart.id}/documents/_report?locale=${.locale}" download class="btn btn-primary ml-2">
                          <i class="fas fa-download"></i> <@message "download-cart-export"/>
                        </a>
                      </#if>
                    </#if>
                    <button id="delete-all" type="button" class="btn btn-danger ml-2" data-toggle="modal" data-target="#modal-delete">
                      <i class="fas fa-trash"></i> <@message "delete"/> <span class="badge badge-light selection-count"></span>
                    </button>
                      <#if config.setsSearchEnabled>
                        <a class="btn btn-info ml-2" href="${contextPath}/search#lists?type=variables&query=variable(in(Mica_variable.sets,${user.variablesCart.id}))">
                          <i class="fas fa-search"></i>
                        </a>
                      </#if>
                  </div>
                </#if>
            </div>
            <div class="card-body">
              <#if user.variablesCart?? && user.variablesCart.count gt 0>
                <div id="loadingSet" class="spinner-border spinner-border-sm" role="status"></div>
                <div class="table-responsive">
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
              <h3 class="card-title"><@message "studies"/></h3>
                <#if user.studiesCart?? && user.studiesCart.count gt 0>
                  <div class="float-right">
                    <#if studiesCompareEnabled>
                      <button type="button" class="btn btn-info ml-2" onclick="onCompareStudies()">
                        <i class="fas fa-grip-lines-vertical"></i> <@message "compare"/> <span class="badge badge-light studies-selection-count"></span>
                      </button>
                    </#if>
                    <#if showCartDownload>
                      <a href="${contextPath}/ws/studies/set/${user.studiesCart.id}/documents/_report?locale=${.locale}" download class="btn btn-primary ml-2">
                        <i class="fas fa-download"></i> <@message "download-cart-export"/>
                      </a>
                    </#if>
                    <button id="delete-all" type="button" class="btn btn-danger ml-2" data-toggle="modal" data-target="#modal-delete-studies">
                      <i class="fas fa-trash"></i> <@message "delete"/> <span class="badge badge-light studies-selection-count"></span>
                    </button>
                      <#if config.setsSearchEnabled>
                        <a class="btn btn-info ml-2" href="${contextPath}/search#lists?type=studies&query=study(in(Mica_study.sets,${user.studiesCart.id}))">
                          <i class="fas fa-search"></i>
                        </a>
                      </#if>
                  </div>
                </#if>
            </div>
            <div class="card-body">
              <#if user.studiesCart?? && user.studiesCart.count gt 0>
                <div id="loadingStudiesSet" class="spinner-border spinner-border-sm" role="status"></div>
                <div class="table-responsive">
                  <table id="studiesSetTable" class="table table-striped">
                    <thead>
                    <tr>
                      <th><i class="far fa-square"></i></th>
                      <th></th>
                      <th><@message "acronym"/></th>
                      <th><@message "name"/></th>
                      <#if config.studyDatasetEnabled && config.harmonizationDatasetEnabled>
                        <th><@message "type"/></th>
                      </#if>
                    </tr>
                    </thead>
                    <tbody></tbody>
                  </table>
                </div>
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
              <h3 class="card-title"><@message "networks"/></h3>
                <#if user.networksCart?? && user.networksCart.count gt 0>
                  <div class="float-right">
                    <#if networksCompareEnabled>
                      <button type="button" class="btn btn-info ml-2" onclick="onCompareNetworks()">
                        <i class="fas fa-grip-lines-vertical"></i> <@message "compare"/> <span class="badge badge-light networks-selection-count"></span>
                      </button>
                    </#if>
                    <#if showCartDownload>
                      <a href="${contextPath}/ws/networks/set/${user.networksCart.id}/documents/_report?locale=${.locale}" download class="btn btn-primary ml-2">
                        <i class="fas fa-download"></i> <@message "download-cart-export"/>
                      </a>
                    </#if>
                    <button id="delete-all" type="button" class="btn btn-danger ml-2" data-toggle="modal" data-target="#modal-delete-networks">
                      <i class="fas fa-trash"></i> <@message "delete"/> <span class="badge badge-light networks-selection-count"></span>
                    </button>
                      <#if config.setsSearchEnabled>
                        <a class="btn btn-info ml-2" href="${contextPath}/search#lists?type=networks&query=network(in(Mica_network.sets,${user.networksCart.id}))">
                          <i class="fas fa-search"></i>
                        </a>
                      </#if>
                  </div>
                </#if>
            </div>
            <div class="card-body">
                <#if user.networksCart?? && user.networksCart.count gt 0>
                  <div id="loadingNetworksSet" class="spinner-border spinner-border-sm" role="status"></div>
                  <div class="table-responsive">
                    <table id="networksSetTable" class="table table-striped">
                      <thead>
                      <tr>
                        <th><i class="far fa-square"></i></th>
                        <th></th>
                        <th><@message "acronym"/></th>
                        <th><@message "name"/></th>
                      </tr>
                      </thead>
                      <tbody></tbody>
                    </table>
                  </div>
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
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p><@message "confirm-data-access-request-creation-from-cart"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="DataAccessService.createFromCart()"><@message "confirm"/></button>
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
