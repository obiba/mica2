<!-- Macros -->
<#include "models/data-access-form.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <#include "libs/data-access-form-head.ftl">
  <title>${config.name!""} | <@message "data-access-feasibility"/> ${feasibility.id}</title>
</head>
<body id="data-access-feasibility-page" ng-app="formModule" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed">
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
              <span class="text-white-50"><@message "data-access-feasibility"/> /</span> ${feasibility.id}
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
            <div id="data-access-feasibility-callout" class="callout callout-info">
              <p>
                <@message "data-access-feasibility-callout"/>
              </p>
            </div>
          </div>
          <!-- /.col-12 -->
        </div>
        <!-- /.row -->
      </#if>

      <div class="row" ng-controller="FormController">
        <div class="col-sm-12 <#if dataAccessInstructionsEnabled || (feasibility.variablesSet?? || feasibilityVariablesEnabled)>col-lg-8<#else>col-lg-12</#if>">
          <div class="card card-primary card-outline">
            <div class="card-header d-print-none">
              <h3 class="card-title"><@message "feasibility-inquiry-form"/></h3>
              <div ng-cloak>
                <#if feasibilityPermissions?seq_contains("EDIT")>
                  <span class="float-right border-left ml-2 pl-2" ng-if="schema.readOnly">
                    <a class="btn btn-primary" href="${feasibility.id}?edit=true"><i class="fas fa-pen"></i> <@message "edit"/></a>
                  </span>
                  <span class="float-right border-left ml-2 pl-2" ng-hide="schema.readOnly">
                    <a class="btn btn-primary" href="#" ng-click="save('${dar.id}', 'feasibility', '${feasibility.id}')"><@message "save"/></a>
                    <a class="btn btn-default" href="${feasibility.id}"><@message "cancel"/></a>
                  </span>
                </#if>
                <#if feasibilityPermissions?seq_contains("EDIT_STATUS")>
                  <span class="float-right border-left ml-2 pl-2">
                    <#if feasibility.status == "OPENED">
                      <button type="button" class="btn btn-info" ng-hide="!schema.readOnly" data-toggle="modal"
                              data-target="#modal-submit"><@message "submit"/></button>
                      <button type="button" class="btn btn-success"
                              ng-click="validate()"><@message "validate"/></button>
                    <#elseif feasibility.status == "SUBMITTED">
                      <button type="button" class="btn btn-primary"
                              onclick="DataAccessService.reopen('${dar.id}', 'feasibility', '${feasibility.id}')"><@message "reopen"/></button>
                      <button type="button" class="btn btn-success" data-toggle="modal"
                              data-target="#modal-approve"><@message "approve"/></button>
                      <button type="button" class="btn btn-danger" data-toggle="modal"
                              data-target="#modal-reject"><@message "reject"/></button>
                    <#elseif feasibility.status == "APPROVED" && !accessConfig.approvedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal"
                              data-target="#modal-cancel-approve"><@message "cancel-approval"/></button>
                    <#elseif feasibility.status == "REJECTED" && !accessConfig.rejectedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal"
                              data-target="#modal-cancel-reject"><@message "cancel-rejection"/></button>
                    </#if>
                  </span>
                </#if>
                <span class="float-right" ng-if="schema.readOnly">
                  <#if diffs??>
                    <button type="button" class="btn btn-outline-info" data-toggle="modal"
                            data-target="#modal-diff"><i class="fas fa-code-branch"></i> <@message "form-diff"/></button>
                  </#if>
                  <#if isAdministrator || isDAO>
                    <a href="${contextPath}/ws/data-access-request/${dar.id}/feasibility/${feasibility.id}/_word?lang=${.lang}" class="btn btn-default">
                      <i class="fas fa-file-word"></i> <@message "download"/>
                    </a>
                  </#if>
                  <a href="#" onclick="window.print()" class="btn btn-default">
                    <i class="fas fa-print"></i> <@message "global.print"/>
                  </a>
                </span>
              </div>
            </div>
            <div class="card-body">
              <div class="d-none d-print-block">
                <@dataAccessFormPrintHeader form=feasibility type="data-access-feasibility"/>
              </div>
              <form name="forms.requestForm" class="bootstrap3">
                <div sf-schema="schema" sf-form="form" sf-model="model"></div>
              </form>
              <div class="d-none d-print-block">
                <@dataAccessFormPrintFooter form=feasibility/>
              </div>
            </div>
            <#if feasibilityPermissions?seq_contains("EDIT")>
              <div class="card-footer" ng-hide="schema.readOnly" ng-cloak>
                <span class="float-right">
                  <a class="btn btn-primary" href="#" ng-click="save('${dar.id}', 'feasibility', '${feasibility.id}')"><@message "save"/></a>
                  <a class="btn btn-default" href="${feasibility.id}"><@message "cancel"/></a>
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
                  <p><@message "confirm-feasibility-submission-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          ng-click="submit('${dar.id}', 'feasibility', '${feasibility.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-feasibility-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="DataAccessService.approve('${dar.id}', 'feasibility', '${feasibility.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-feasibility-rejection-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="DataAccessService.reject('${dar.id}', 'feasibility', '${feasibility.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-feasibility-cancel-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="DataAccessService.submit('${dar.id}', 'feasibility', '${feasibility.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-feasibility-cancel-rejection-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="DataAccessService.submit('${dar.id}', 'feasibility', '${feasibility.id}')"><@message "confirm"/></button>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->
        </div>

        <#if dataAccessInstructionsEnabled || (feasibility.variablesSet?? || feasibilityVariablesEnabled)>
          <div class="col-sm-12 col-lg-4 d-print-none">

            <#if feasibility.variablesSet?? || feasibilityVariablesEnabled>
              <div class="card card-primary card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "variables"/></h3>
                </div>
                <div class="card-body">
                    <#if feasibility.variablesSet??>
                      <a class="btn btn-info" href="${contextPath}/list/${feasibility.variablesSet.id}">
                        <i class="far fa-list-alt"></i>
                        <span><@message "list-linked-variables"/></span>
                        <span class="badge badge-light">${feasibility.variablesSet.identifiers?size}</span>
                      </a>
                      <#if feasibilityPermissions?seq_contains("EDIT")>
                        <a class="ml-3" href="javascript:void(0)" onclick="DataAccessService.unlinkVariables('${feasibility.parentId}', 'feasibility', '${feasibility.id}')">
                          <i class="fas fa-unlink"></i> <@message "unlink-variables"/>
                        </a>
                      </#if>
                    <#elseif feasibilityVariablesEnabled && feasibilityPermissions?seq_contains("EDIT")>
                      <a class="btn btn-primary" href="javascript:void(0)" onclick="DataAccessService.linkVariables('${feasibility.parentId}', 'feasibility', '${feasibility.id}')">
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
                    <@dataAccessFeasibilityFormHelp feasibility=feasibility/>
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
    $('#feasibility-form-menu').addClass('active').attr('href', '#');
    $('#feasibility-form-menu-${feasibility.id}').addClass('active');
</script>
<#include "libs/data-access-form-scripts.ftl">

</body>
</html>
