<!-- Macros -->
<#include "models/data-access-form.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <#include "libs/data-access-form-head.ftl">
  <title>${config.name!""} | <@message "data-access-form"/> ${dar.id}</title>
</head>
<body id="data-access-form-page" ng-app="formModule" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed">
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
              <span class="text-white-50"><@message "data-access-form"/> /</span> ${dar.id}
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
            <div id="data-access-form-callout" class="callout callout-info">
              <p>
                  <@message "data-access-form-callout"/>
              </p>
            </div>
          </div>
          <!-- /.col-12 -->
        </div>
        <!-- /.row -->
      </#if>

      <div class="row" ng-controller="FormController">
        <div class="col-sm-12 <#if dataAccessInstructionsEnabled || (dar.variablesSet?? || variablesEnabled)>col-lg-8<#else>col-lg-12</#if>">
          <div class="card card-primary card-outline">
            <div class="card-header d-print-none">
              <h3 class="card-title">
                <#if accessConfig.preliminaryEnabled><@message "main-form"/><#else><@message "application-form"/></#if>
              </h3>
              <div ng-cloak>
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
                      <button type="button" class="btn btn-info" ng-hide="!schema.readOnly" data-toggle="modal"
                              data-target="#modal-submit"><@message "submit"/></button>
                      <button type="button" class="btn btn-success"
                              ng-click="validate()"><@message "validate"/></button>
                    <#elseif dar.status == "APPROVED" && !accessConfig.approvedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal"
                              data-target="#modal-cancel-approve"><@message "cancel-approval"/></button>
                    <#elseif dar.status == "REJECTED" && !accessConfig.rejectedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal"
                              data-target="#modal-cancel-reject"><@message "cancel-rejection"/></button>
                    <#else>
                      <button type="button" class="btn btn-primary"
                              onclick="DataAccessService.reopen('${dar.id}')"><@message "reopen"/></button>
                      <#if (dar.status == "SUBMITTED" && accessConfig.withReview)>
                        <button type="button" class="btn btn-primary"
                                onclick="DataAccessService.review('${dar.id}')"><@message "review"/></button>
                      <#elseif dar.status == "REVIEWED" || (dar.status == "SUBMITTED" && !accessConfig.withReview)>
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
                <span class="float-right <#if permissions?seq_contains("EDIT_STATUS")>border-right mr-2 pr-2</#if>" ng-if="schema.readOnly">
                  <#if diffs??>
                    <button type="button" class="btn btn-outline-info" data-toggle="modal"
                            data-target="#modal-diff"><i class="fas fa-code-branch"></i> <@message "form-diff"/></button>
                  </#if>
                  <#if accessConfig.downloadPdf>
                    <#if isAdministrator || isDAO>
                      <div class="btn-group">
                        <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                          <i class="fas fa-download"></i> <@message "download"/>
                        </button>
                        <div class="dropdown-menu dropdown-menu-right">
                          <a class="dropdown-item" href="${contextPath}/ws/data-access-request/${dar.id}/_pdf?lang=${.lang}">
                            <i class="fas fa-file-pdf"></i> <@message "form"/>
                          </a>
                          <a class="dropdown-item" href="${contextPath}/ws/data-access-request/${dar.id}/files/_download">
                            <i class="fas fa-file-archive"></i> <@message "files"/>
                          </a>
                        </div>
                      </div>
                    <#else>
                      <a href="${contextPath}/ws/data-access-request/${dar.id}/_pdf?lang=${.lang}" class="btn btn-default">
                        <i class="fas fa-file-pdf"></i> <@message "download"/>
                      </a>
                    </#if>
                  <#else>
                    <#if isAdministrator || isDAO>
                      <div class="btn-group">
                        <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                          <i class="fas fa-download"></i> <@message "download"/>
                        </button>
                        <div class="dropdown-menu dropdown-menu-right">
                          <a class="dropdown-item" href="${contextPath}/ws/data-access-request/${dar.id}/_word?lang=${.lang}">
                            <i class="fas fa-file-word"></i> <@message "form"/>
                          </a>
                          <a class="dropdown-item" href="${contextPath}/ws/data-access-request/${dar.id}/files/_download">
                            <i class="fas fa-file-archive"></i> <@message "files"/>
                          </a>
                        </div>
                      </div>
                    </#if>
                    <a href="#" onclick="window.print()" class="btn btn-default">
                      <i class="fas fa-print"></i> <@message "global.print"/>
                    </a>
                  </#if>
                </span>
              </div>
            </div>
            <div class="card-body">
              <div class="d-none d-print-block">
                <@dataAccessFormPrintHeader form=dar type="data-access-request"/>
              </div>
              <form name="forms.requestForm" class="bootstrap3">
                <div sf-schema="schema" sf-form="form" sf-model="model" sf-options="sfOptions"></div>
              </form>
              <div class="d-none d-print-block">
                <@dataAccessFormPrintFooter form=dar/>
              </div>
            </div>
            <#if permissions?seq_contains("EDIT")>
              <div class="card-footer" ng-hide="schema.readOnly" ng-cloak>
                <span class="float-right">
                  <a class="btn btn-primary" href="#" ng-click="save('${dar.id}')"><@message "save"/></a>
                  <a class="btn btn-default" href="${dar.id}"><@message "cancel"/></a>
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
                  <p><@message "confirm-submission-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          ng-click="submit('${dar.id}')"><@message "confirm"/></button>
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
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="DataAccessService.approve('${dar.id}')"><@message "confirm"/></button>
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
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="DataAccessService.conditionally('${dar.id}')"><@message "confirm"/></button>
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
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="DataAccessService.reject('${dar.id}')"><@message "confirm"/></button>
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
                      <button type="button" class="btn btn-primary" data-dismiss="modal"
                              onclick="DataAccessService.review('${dar.id}')"><@message "confirm"/></button>
                    <#else>
                      <button type="button" class="btn btn-primary" data-dismiss="modal"
                              onclick="DataAccessService.submit('${dar.id}')"><@message "confirm"/></button>
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
                      <button type="button" class="btn btn-primary" data-dismiss="modal"
                              onclick="DataAccessService.review('${dar.id}')"><@message "confirm"/></button>
                    <#else>
                      <button type="button" class="btn btn-primary" data-dismiss="modal"
                              onclick="DataAccessService.submit('${dar.id}')"><@message "confirm"/></button>
                    </#if>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->
        </div>

        <#if dataAccessInstructionsEnabled || (dar.variablesSet?? || variablesEnabled)>
          <div class="col-sm-12 col-lg-4 d-print-none">

            <#if dar.variablesSet?? || variablesEnabled>
              <div class="card card-primary card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "variables"/></h3>
                </div>
                <div class="card-body">
                  <#if dar.variablesSet??>
                    <a class="btn btn-info" href="${contextPath}/list/${dar.variablesSet.id}">
                      <i class="far fa-list-alt"></i>
                      <span><@message "list-linked-variables"/></span>
                      <span class="badge badge-light">${dar.variablesSet.identifiers?size}</span>
                    </a>
                    <#if permissions?seq_contains("EDIT")>
                      <a class="ml-3" href="javascript:void(0)" onclick="DataAccessService.unlinkVariables('${dar.id}')">
                        <i class="fas fa-unlink"></i> <@message "unlink-variables"/>
                      </a>
                    </#if>
                  <#elseif variablesEnabled && permissions?seq_contains("EDIT")>
                    <a class="btn btn-primary" href="javascript:void(0)" onclick="DataAccessService.linkVariables('${dar.id}')">
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
                    <@dataAccessFormHelp dar=dar/>
                </div>
              </div>
            </#if>

          </div>
        </#if>

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
<#include "libs/data-access-scripts.ftl">
<script>
    $(function () {
        $('#form-menu').addClass('active');//.attr('href', '#');
    });
</script>
<#include "libs/data-access-form-scripts.ftl">

</body>
</html>
