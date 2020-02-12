<!-- Macros -->
<#include "models/data-access-form.ftl">

<!DOCTYPE html>
<html lang="${.lang}" xmlns:v-bind="http://www.w3.org/1999/xhtml">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | Data Access Form ${dar.id}</title>
</head>
<body ng-app="formModule" class="hold-transition sidebar-mini">
<!-- Site wrapper -->
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Main Sidebar Container -->
  <aside class="main-sidebar sidebar-dark-primary">
    <!-- Brand Logo -->
    <a href="${pathPrefix}/bower_components/admin-lte/index3.html" class="brand-link bg-white">
      <img src="${pathPrefix}/bower_components/admin-lte/dist/img/AdminLTELogo.png"
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
              <span class="text-white-50">Data Access Form /</span> ${dar.id}
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

      <div class="row d-print-none">
        <div class="col-12">
          <div class="callout callout-info">
            <p>
              <@message "data-access-form-callout"/>
            </p>
          </div>
        </div>
        <!-- /.col-12 -->
      </div>
      <!-- /.row -->

      <div class="row" ng-controller="FormController">
        <div class="col-lg-8">
          <div class="card card-primary card-outline">
            <div class="card-header d-print-none">
              <h3 class="card-title"><@message "application-form"/></h3>
              <div>
                <#if permissions?seq_contains("EDIT")>
                  <span class="float-right border-left ml-2 pl-2" ng-if="schema.readOnly">
                    <a class="btn btn-primary" href="${dar.id}?edit=true"><i class="fas fa-pen"></i> <@message "edit"/></a>
                  </span>
                  <span class="float-right border-left ml-2 pl-2" ng-hide="schema.readOnly">
                    <a class="btn btn-primary" href="#" ng-click="save('${dar.id}')"><@message "save"/></a>
                    <a class="btn btn-default" href="${dar.id}"><@message "cancel"/></a>
                  </span>
                </#if>
                <#if permissions?seq_contains("EDIT_STATUS")>
                  <span class="float-right">
                    <#if dar.status == "OPENED" || dar.status == "CONDITIONALLY_APPROVED">
                      <button type="button" class="btn btn-info" ng-disabled="!schema.readOnly" data-toggle="modal" data-target="#modal-submit"><@message "submit"/></button>
                      <button type="button" class="btn btn-success" ng-click="validate()"><@message "validate"/></button>
                    <#elseif (dar.status == "SUBMITTED" && accessConfig.withReview)>
                      <button type="button" class="btn btn-primary" onclick="micajs.dataAccess.review('${dar.id}')"><@message "review"/></button>
                    <#elseif dar.status == "REVIEWED" || (dar.status == "SUBMITTED" && !accessConfig.withReview)>
                      <button type="button" class="btn btn-success" data-toggle="modal" data-target="#modal-approve"><@message "approve"/></button>
                      <#if accessConfig.withConditionalApproval>
                        <button type="button" class="btn btn-warning" data-toggle="modal" data-target="#modal-condition"><@message "conditionallyApprove"/></button>
                      </#if>
                      <button type="button" class="btn btn-danger" data-toggle="modal" data-target="#modal-reject"><@message "reject"/></button>
                    <#elseif dar.status == "APPROVED" && !accessConfig.approvedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal" data-target="#modal-cancel-approve"><@message "cancel-approval"/></button>
                    <#elseif dar.status == "REJECTED" && !accessConfig.rejectedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal" data-target="#modal-cancel-reject"><@message "cancel-rejection"/></button>
                    </#if>
                  </span>
                </#if>
              </div>
            </div>
            <div class="card-body">
              <form name="forms.requestForm">
                <div sf-schema="schema" sf-form="form" sf-model="model"></div>
              </form>
            </div>
          </div>

          <!-- Confirm submission modal -->
          <div class="modal fade" id="modal-submit">
            <div class="modal-dialog">
              <div class="modal-content">
                <div class="modal-header">
                  <h4 class="modal-title"><@message "confirm-submission-title"/></h4>
                  <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                  </button>
                </div>
                <div class="modal-body">
                  <p><@message "confirm-submission-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal" ng-click="submit('${dar.id}')"><@message "confirm"/></button>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->

          <!-- Confirm approval modal -->
          <div class="modal fade" id="modal-approve">
            <div class="modal-dialog">
              <div class="modal-content">
                <div class="modal-header">
                  <h4 class="modal-title"><@message "confirm-approval-title"/></h4>
                  <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                  </button>
                </div>
                <div class="modal-body">
                  <p><@message "confirm-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="micajs.dataAccess.approve('${dar.id}')"><@message "confirm"/></button>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->

          <!-- Confirm conditional approval modal -->
          <div class="modal fade" id="modal-condition">
            <div class="modal-dialog">
              <div class="modal-content">
                <div class="modal-header">
                  <h4 class="modal-title"><@message "confirm-conditional-approval-title"/></h4>
                  <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                  </button>
                </div>
                <div class="modal-body">
                  <p><@message "confirm-conditional-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="micajs.dataAccess.conditionallyApprove('${dar.id}')"><@message "confirm"/></button>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->

          <!-- Confirm rejection modal -->
          <div class="modal fade" id="modal-reject">
            <div class="modal-dialog">
              <div class="modal-content">
                <div class="modal-header">
                  <h4 class="modal-title"><@message "confirm-rejection-title"/></h4>
                  <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                  </button>
                </div>
                <div class="modal-body">
                  <p><@message "confirm-rejection-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="micajs.dataAccess.reject('${dar.id}')"><@message "confirm"/></button>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->

          <!-- Confirm cancel approval modal -->
          <div class="modal fade" id="modal-cancel-approve">
            <div class="modal-dialog">
              <div class="modal-content">
                <div class="modal-header">
                  <h4 class="modal-title"><@message "confirm-cancel-approval-title"/></h4>
                  <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                  </button>
                </div>
                <div class="modal-body">
                  <p><@message "confirm-cancel-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <#if accessConfig.withReview>
                    <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="micajs.dataAccess.review('${dar.id}')"><@message "confirm"/></button>
                  <#else>
                    <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="micajs.dataAccess.submit('${dar.id}')"><@message "confirm"/></button>
                  </#if>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->

          <!-- Confirm cancel rejection modal -->
          <div class="modal fade" id="modal-cancel-reject">
            <div class="modal-dialog">
              <div class="modal-content">
                <div class="modal-header">
                  <h4 class="modal-title"><@message "confirm-cancel-rejection-title"/></h4>
                  <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                  </button>
                </div>
                <div class="modal-body">
                  <p><@message "confirm-cancel-rejection-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                    <#if accessConfig.withReview>
                      <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="micajs.dataAccess.review('${dar.id}')"><@message "confirm"/></button>
                    <#else>
                      <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="micajs.dataAccess.submit('${dar.id}')"><@message "confirm"/></button>
                    </#if>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->

        </div>
        <div class="col-lg-4 d-print-none">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title"><@message "help"/></h3>
            </div>
            <div class="card-body">
              <@dataAccessFormHelp dar=dar/>
            </div>
          </div>
        </div>
      </div>

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
<#include "libs/data-access-form-scripts.ftl">

</body>
</html>
