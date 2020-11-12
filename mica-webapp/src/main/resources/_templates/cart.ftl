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

    <!-- Confirm delete modal -->
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
                    onclick="VariablesSetService.deleteVariables('${user.variablesCart.id}', variablesCartStorage.getSelections(), function() { window.location.reload(); })"><@message "confirm"/>
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
        <div class="callout callout-info">
          <p><@message "sets.cart.help"/></p>
        </div>

        <div class="card card-info card-outline">
          <div class="card-header">
            <h3 class="card-title"><@message "variables"/></h3>
            <#if user.variablesCart?? && user.variablesCart.count gt 0>
              <div class="float-right">
                <#if showCartDownload>
                  <#if showCartViewDownload>
                    <div class="btn-group" role="group">
                      <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        <i class="fas fa-download"></i> <@message "download"/>
                      </button>
                      <div class="dropdown-menu">
                        <a class="dropdown-item" href="${contextPath}/ws/variables/set/${user.variablesCart.id}/documents/_export" download><@message "download-cart-ids"/></a>
                        <a class="dropdown-item" href="${contextPath}/ws/variables/set/${user.variablesCart.id}/documents/_opal" download><@message "download-cart-views"/></a>
                      </div>
                    </div>
                  <#else>
                    <a href="../ws/variables/set/${user.variablesCart.id}/documents/_opal" download class="btn btn-primary ml-2">
                      <i class="fas fa-download"></i> <@message "download"/>
                    </a>
                  </#if>
                </#if>
                <button id="delete-all" type="button" class="btn btn-danger ml-2" data-toggle="modal" data-target="#modal-delete">
                  <i class="fas fa-trash"></i> <@message "delete"/> <span id="selection-count" class="badge badge-light"></span>
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
              <div class="text-muted"><@message "sets.cart.no-variables"/></div>
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
<#if user.variablesCart?? && user.variablesCart.count gt 0>
  <#assign set = user.variablesCart.set/>
  <#include "libs/document-set-scripts.ftl">
</#if>

</body>
</html>
