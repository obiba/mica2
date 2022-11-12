<!-- Macros -->
<#include "models/data-access-form.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <#include "libs/data-access-form-head.ftl">
  <title>${config.name!""} | <@message "data-access-preliminary"/> ${preliminary.id}</title>
</head>
<body id="data-access-preliminary-page" ng-app="formModule" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed">
<!-- Site wrapper -->
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Sidebar -->
  <#include "libs/data-access-sidebar.ftl">
  <!-- /.sidebar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <section class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row">
          <div class="col-sm-6">
            <h1 class="m-0">
              <span class="text-white-50"><@message "data-access-preliminary"/> /</span> ${preliminary.id}
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

      <#if dar.archived>
        <div class="ribbon-wrapper ribbon-xl">
          <div class="ribbon bg-warning text-xl">
              <@message "archived-title"/>
          </div>
        </div>
      </#if>

      <#if dataAccessCalloutsEnabled>
        <div class="row d-print-none">
          <div class="col-12">
            <div id="data-access-preliminary-callout" class="callout callout-info">
              <p>
                <@message "data-access-preliminary-callout"/>
              </p>
            </div>
          </div>
          <!-- /.col-12 -->
        </div>
        <!-- /.row -->
      </#if>

      <div class="row" ng-controller="FormController">
        <div class="col-sm-12 <#if dataAccessInstructionsEnabled || (preliminary.variablesSet?? || preliminaryVariablesEnabled)>col-lg-8<#else>col-lg-12</#if>">
          <div class="card card-primary card-outline">
            <div class="card-header d-print-none">
              <h3 class="card-title"><@message "preliminary-form"/></h3>
              <div ng-cloak>
                <#if preliminaryPermissions?seq_contains("EDIT")>
                  <span class="float-right border-left ml-2 pl-2" ng-if="schema.readOnly">
                    <a class="btn btn-primary" href="${preliminary.id}?edit=true"><i class="fas fa-pen"></i> <@message "edit"/></a>
                  </span>
                  <span class="float-right border-left ml-2 pl-2" ng-hide="schema.readOnly">
                    <a class="btn btn-primary" href="#" ng-click="save('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "save"/></a>
                    <a class="btn btn-default" href="${preliminary.id}"><@message "cancel"/></a>
                  </span>
                </#if>
                <#if preliminaryPermissions?seq_contains("EDIT_STATUS")>
                  <span class="float-right border-left ml-2 pl-2">
                    <#if preliminary.status == "OPENED">
                      <button type="button" class="btn btn-info" ng-hide="!schema.readOnly" data-toggle="modal"
                              data-target="#modal-submit"><@message "submit"/></button>
                      <button type="button" class="btn btn-success"
                              ng-click="validate()"><@message "validate"/></button>
                    <#elseif preliminary.status == "APPROVED" && !accessConfig.approvedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal"
                              data-target="#modal-cancel-approve"><@message "cancel-approval"/></button>
                    <#elseif preliminary.status == "REJECTED" && !accessConfig.rejectedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal"
                              data-target="#modal-cancel-reject"><@message "cancel-rejection"/></button>
                    <#else>
                      <button type="button" class="btn btn-primary"
                              onclick="DataAccessService.reopen('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "reopen"/></button>
                      <#if (preliminary.status == "SUBMITTED" && accessConfig.withReview)>
                        <button type="button" class="btn btn-primary"
                              onclick="DataAccessService.review('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "review"/></button>
                      <#elseif preliminary.status == "REVIEWED" || (preliminary.status == "SUBMITTED" && !accessConfig.withReview)>
                        <button type="button" class="btn btn-success" data-toggle="modal"
                              data-target="#modal-approve"><@message "approve"/></button>
                        <#if accessConfig.withConditionalApproval>
                          <button type="button" class="btn btn-warning" data-toggle="modal"
                                data-target="#modal-condition"><@message "conditionallyApprove"/></button>
                        </#if>
                        <button type="button" class="btn btn-danger" data-toggle="modal"
                                data-target="#modal-reject"><@message "reject"/></button>
                      </#if>
                    </#if>
                  </span>
                </#if>
                <span class="float-right" ng-if="schema.readOnly">
                  <#if diffs??>
                    <button type="button" class="btn btn-outline-info" data-toggle="modal"
                            data-target="#modal-diff"><i class="fas fa-code-branch"></i> <@message "form-diff"/></button>
                  </#if>
                  <a href="#" onclick="window.print()" class="btn btn-default">
                    <i class="fas fa-print"></i> <@message "global.print"/>
                  </a>
                </span>
              </div>
            </div>
            <div class="card-body">
              <div class="d-none d-print-block">
                <@dataAccessFormPrintHeader form=preliminary type="data-access-preliminary"/>
              </div>
              <form name="forms.requestForm" class="bootstrap3">
                <div sf-schema="schema" sf-form="form" sf-model="model"></div>
              </form>
              <div class="d-none d-print-block">
                <@dataAccessFormPrintFooter form=preliminary/>
              </div>
            </div>
            <#if preliminaryPermissions?seq_contains("EDIT")>
              <div class="card-footer" ng-hide="schema.readOnly" ng-cloak>
                <span class="float-right">
                  <a class="btn btn-primary" href="#" ng-click="save('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "save"/></a>
                  <a class="btn btn-default" href="${preliminary.id}"><@message "cancel"/></a>
                </span>
              </div>
            </#if>
          </div>

          <!-- Submission diffs modal -->
          <#if diffs??>
            <@diffsModal/>
          </#if>

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
                  <p><@message "confirm-preliminary-submission-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          ng-click="submit('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-preliminary-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="DataAccessService.approve('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-preliminary-rejection-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="DataAccessService.reject('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-preliminary-cancel-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <#if accessConfig.withReview>
                    <button type="button" class="btn btn-primary" data-dismiss="modal"
                            onclick="DataAccessService.review('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "confirm"/></button>
                  <#else>
                    <button type="button" class="btn btn-primary" data-dismiss="modal"
                            onclick="DataAccessService.submit('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-preliminary-cancel-rejection-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <#if accessConfig.withReview>
                    <button type="button" class="btn btn-primary" data-dismiss="modal"
                            onclick="DataAccessService.review('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "confirm"/></button>
                  <#else>
                    <button type="button" class="btn btn-primary" data-dismiss="modal"
                            onclick="DataAccessService.submit('${dar.id}', 'preliminary', '${preliminary.id}')"><@message "confirm"/></button>
                  </#if>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->
        </div>

        <#if dataAccessInstructionsEnabled || (preliminary.variablesSet?? || preliminaryVariablesEnabled)>
          <div class="col-sm-12 col-lg-4 d-print-none">

            <#if preliminary.variablesSet?? || preliminaryVariablesEnabled>
              <div class="card card-primary card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "variables"/></h3>
                </div>
                <div class="card-body">
                    <#if preliminary.variablesSet??>
                      <a class="btn btn-info" href="${contextPath}/list/${preliminary.variablesSet.id}">
                        <i class="far fa-list-alt"></i>
                        <span><@message "list-linked-variables"/></span>
                        <span class="badge badge-light">${preliminary.variablesSet.identifiers?size}</span>
                      </a>
                      <#if preliminaryPermissions?seq_contains("EDIT")>
                        <a class="ml-3" href="javascript:void(0)" onclick="DataAccessService.unlinkVariables('${preliminary.parentId}', 'preliminary', '${preliminary.id}')">
                          <i class="fas fa-unlink"></i> <@message "unlink-variables"/>
                        </a>
                      </#if>
                    <#elseif preliminaryVariablesEnabled && preliminaryPermissions?seq_contains("EDIT")>
                      <a class="btn btn-primary" href="javascript:void(0)" onclick="DataAccessService.linkVariables('${preliminary.parentId}', 'preliminary', '${preliminary.id}')">
                        <i class="fas fa-link"></i> <@message "link-cart-variables"/> <i class="fas fa-shopping-cart fa-xs"></i>
                      </a>
                    <#else>
                      <span><@message "no-linked-variables"/></span>
                    </#if>
                </div>
              </div>
            </#if>

            <#if dataAccessInstructionsEnabled>
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "instructions"/></h3>
                </div>
                <div class="card-body">
                    <@dataAccessPreliminaryFormHelp preliminary=preliminary/>
                </div>
              </div>
            </#if>

          </div>
        </#if>
      </div>
      <!-- /.row -->

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
<#include "libs/data-access-scripts.ftl">
<script>
    $('#preliminary-form-menu').addClass('active').attr('href', '#');
    $('#preliminary-form-menu-${preliminary.id}').addClass('active');
</script>
<#include "libs/data-access-form-scripts.ftl">

</body>
</html>
