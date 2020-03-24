<!-- Macros -->
<#include "models/data-access-form.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
    <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "data-access-amendment"/> ${amendment.id}</title>
</head>
<body ng-app="formModule" class="hold-transition sidebar-mini">
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
          <div class="col-sm-8">
            <h1 class="m-0 float-left">
              <span class="text-white-50"><@message "data-access-amendment"/> /</span> ${amendment.id}
            </h1>
              <#if amendmentPermissions?seq_contains("DELETE")>
                <button type="button" class="btn btn-danger ml-4" data-toggle="modal" data-target="#modal-delete">
                  <i class="fas fa-trash"></i> <@message "delete"/>
                </button>
              </#if>
          </div>
          <div class="col-sm-4">
              <#include "libs/data-access-breadcrumb.ftl">
          </div>
        </div>
      </div><!-- /.container-fluid -->
    </section>

    <!-- Confirm delete modal -->
    <div class="modal fade" id="modal-delete">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "confirm-deletion-title"/></h4>
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p><@message "confirm-amendment-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal"
                    onclick="micajs.dataAccess.delete('${dar.id}', 'amendment', '${amendment.id}')"><@message "confirm"/>
            </button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->

    <!-- Main content -->
    <section class="content">

      <div class="row d-print-none">
        <div class="col-12">
          <div class="callout callout-info">
            <p>
              <@message "data-access-amendment-form-callout"/>
            </p>
          </div>
        </div>
        <!-- /.col-12 -->
      </div>
      <!-- /.row -->

      <div class="row" ng-controller="FormController">
        <div class="col-sm-12 col-lg-8">
          <div class="card card-primary card-outline">
            <div class="card-header d-print-none">
              <h3 class="card-title"><@message "amendment-form"/></h3>
              <div>
                <#if amendmentPermissions?seq_contains("EDIT")>
                  <span class="float-right border-left ml-2 pl-2" ng-if="schema.readOnly">
                    <a class="btn btn-primary" href="${amendment.id}?edit=true"><i class="fas fa-pen"></i> <@message "edit"/></a>
                  </span>
                  <span class="float-right border-left ml-2 pl-2" ng-hide="schema.readOnly">
                    <a class="btn btn-primary" href="#" ng-click="save('${dar.id}', 'amendment', '${amendment.id}')"><@message "save"/></a>
                    <a class="btn btn-default" href="${amendment.id}"><@message "cancel"/></a>
                  </span>
                </#if>
                <#if amendmentPermissions?seq_contains("EDIT_STATUS")>
                  <span class="float-right">
                    <#if amendment.status == "OPENED" || amendment.status == "CONDITIONALLY_APPROVED">
                      <button type="button" class="btn btn-info" ng-hide="!schema.readOnly" data-toggle="modal"
                              data-target="#modal-submit"><@message "submit"/></button>
                      <button type="button" class="btn btn-success"
                              ng-click="validate()"><@message "validate"/></button>
                    <#elseif amendment.status == "APPROVED" && !accessConfig.approvedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal"
                              data-target="#modal-cancel-approve"><@message "cancel-approval"/></button>
                    <#elseif amendment.status == "REJECTED" && !accessConfig.rejectedFinal>
                      <button type="button" class="btn btn-outline-secondary" data-toggle="modal"
                              data-target="#modal-cancel-reject"><@message "cancel-rejection"/></button>
                    <#else>
                      <button type="button" class="btn btn-primary"
                              onclick="micajs.dataAccess.reopen('${dar.id}', 'amendment', '${amendment.id}')"><@message "reopen"/></button>
                      <#if (amendment.status == "SUBMITTED" && accessConfig.withReview)>
                        <button type="button" class="btn btn-primary"
                                onclick="micajs.dataAccess.review('${dar.id}', 'amendment', '${amendment.id}')"><@message "review"/></button>
                      <#elseif amendment.status == "REVIEWED" || (amendment.status == "SUBMITTED" && !accessConfig.withReview)>
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
              </div>
            </div>
            <div class="card-body">
              <div class="d-none d-print-block">
                <@dataAccessFormPrintHeader form=amendment type="data-access-amendment"/>
              </div>
              <form name="forms.requestForm" class="bootstrap3">
                <div sf-schema="schema" sf-form="form" sf-model="model"></div>
              </form>
              <div class="d-none d-print-block">
                <@dataAccessFormPrintFooter form=amendment/>
              </div>
            </div>
            <#if amendmentPermissions?seq_contains("EDIT")>
              <div class="card-footer" ng-hide="schema.readOnly">
                <span class="float-right">
                  <a class="btn btn-primary" href="#" ng-click="save('${dar.id}', 'amendment', '${amendment.id}')"><@message "save"/></a>
                  <a class="btn btn-default" href="${amendment.id}"><@message "cancel"/></a>
                </span>
              </div>
            </#if>
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
                  <p><@message "confirm-amendment-submission-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          ng-click="submit('${dar.id}', 'amendment', '${amendment.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-amendment-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="micajs.dataAccess.approve('${dar.id}', 'amendment', '${amendment.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-amendment-conditional-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="micajs.dataAccess.conditionallyApprove('${dar.id}', 'amendment', '${amendment.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-amendment-rejection-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                  <button type="button" class="btn btn-primary" data-dismiss="modal"
                          onclick="micajs.dataAccess.reject('${dar.id}', 'amendment', '${amendment.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-amendment-cancel-approval-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                    <#if accessConfig.withReview>
                      <button type="button" class="btn btn-primary" data-dismiss="modal"
                              onclick="micajs.dataAccess.review('${dar.id}', 'amendment', '${amendment.id}')"><@message "confirm"/></button>
                    <#else>
                      <button type="button" class="btn btn-primary" data-dismiss="modal"
                              onclick="micajs.dataAccess.submit('${dar.id}', 'amendment', '${amendment.id}')"><@message "confirm"/></button>
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
                  <p><@message "confirm-amendment-cancel-rejection-text"/></p>
                </div>
                <div class="modal-footer justify-content-between">
                  <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                    <#if accessConfig.withReview>
                      <button type="button" class="btn btn-primary" data-dismiss="modal"
                              onclick="micajs.dataAccess.review('${dar.id}', 'amendment', '${amendment.id}')"><@message "confirm"/></button>
                    <#else>
                      <button type="button" class="btn btn-primary" data-dismiss="modal"
                              onclick="micajs.dataAccess.submit('${dar.id}', 'amendment', '${amendment.id}')"><@message "confirm"/></button>
                    </#if>
                </div>
              </div>
              <!-- /.modal-content -->
            </div>
            <!-- /.modal-dialog -->
          </div>
          <!-- /.modal -->
        </div>

        <#if dataAccessInstructionsEnabled>
          <div class="col-sm-12 col-lg-4 d-print-none">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "instructions"/></h3>
              </div>
              <div class="card-body">
                  <@dataAccessAmendmentFormHelp amendment=amendment/>
              </div>
            </div>
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
<script>
    $(function () {
        $('#amendment-form-menu').addClass('active');
        $('#amendment-form-menu-${amendment.id}').addClass('active');
    });
</script>
<#include "libs/data-access-form-scripts.ftl">

</body>
</html>
