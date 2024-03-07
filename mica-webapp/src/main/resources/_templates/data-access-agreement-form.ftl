<!-- Macros -->
<#include "models/data-access-form.ftl">
<#include "models/profile.ftl"/>

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <#include "libs/data-access-form-head.ftl">
  <title>${config.name!""} | <@message "data-access-agreement"/> ${agreement.id}</title>
</head>
<body id="data-access-agreement-page" ng-app="formModule" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed">
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
              <span class="text-white-50"><@message "data-access-agreement"/> /</span> ${agreement.id}
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
            <div id="data-access-agreement-callout" class="callout callout-info">
              <p>
                <@message "data-access-agreement-callout"/>
              </p>
            </div>
          </div>
          <!-- /.col-12 -->
        </div>
        <!-- /.row -->
      </#if>

        <div class="row" ng-controller="FormController">
          <div class="col-sm-12 <#if dataAccessInstructionsEnabled>col-lg-8<#else>col-lg-12</#if>">
            <div class="card card-primary card-outline">
              <div class="card-header d-print-none">
                <h3 class="card-title"><@message "agreement-form"/></h3>
                <div ng-cloak>
                  <#if agreementPermissions?seq_contains("EDIT")>
                    <span class="float-right border-left ml-2 pl-2" ng-if="schema.readOnly">
                      <a class="btn btn-primary" href="${agreement.id}?edit=true"><i class="fas fa-pen"></i> <@message "edit"/></a>
                    </span>
                    <span class="float-right border-left ml-2 pl-2" ng-hide="schema.readOnly">
                      <a class="btn btn-primary" href="#" ng-click="save('${dar.id}', 'agreement', '${agreement.id}')"><@message "save"/></a>
                      <a class="btn btn-default" href="${agreement.id}"><@message "cancel"/></a>
                    </span>
                  </#if>
                  <#if agreementPermissions?seq_contains("EDIT_STATUS")>
                    <span class="float-right">
                      <#if agreement.status == "OPENED">
                        <button type="button" class="btn btn-info" ng-if="schema.readOnly" data-toggle="modal"
                                data-target="#modal-submit"><@message "submit"/></button>
                        <button type="button" class="btn btn-success"
                                ng-click="validate()"><@message "validate"/></button>
                      <#elseif agreement.status == "APPROVED" && !accessConfig.approvedFinal>
                        <button type="button" class="btn btn-primary border-left ml-2 pl-2" data-toggle="modal"
                                data-target="#modal-reopen"><@message "reopen"/></button>
                      </#if>
                    </span>
                  </#if>
                  <span class="float-right <#if agreementPermissions?seq_contains("EDIT_STATUS") && agreement.status == "OPENED">border-right mr-2 pr-2</#if>" ng-if="schema.readOnly">
                    <#if isAdministrator || isDAO>
                      <a href="${contextPath}/ws/data-access-request/${dar.id}/agreement/${agreement.id}/_word?lang=${.lang}" class="btn btn-default">
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
                  <@dataAccessFormPrintHeader form=agreement type="data-access-agreement"/>
                </div>
                <form name="forms.requestForm" class="bootstrap3">
                  <div sf-schema="schema" sf-form="form" sf-model="model"></div>
                </form>
                <div class="d-none d-print-block">
                  <@dataAccessFormPrintFooter form=agreement/>
                </div>
              </div>
              <#if agreementPermissions?seq_contains("EDIT")>
                <div class="card-footer" ng-hide="schema.readOnly" ng-cloak>
                  <span class="float-right">
                    <a class="btn btn-primary" href="#" ng-click="save('${dar.id}', 'agreement', '${agreement.id}')"><@message "save"/></a>
                    <a class="btn btn-default" href="${agreement.id}"><@message "cancel"/></a>
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
                    <p><@message "confirm-agreement-submission-text"/></p>
                  </div>
                  <div class="modal-footer justify-content-between">
                    <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                    <button type="button" class="btn btn-primary" data-dismiss="modal"
                            ng-click="approveAgreement('${dar.id}', '${agreement.id}')"><@message "confirm"/></button>
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
                    <p><@message "confirm-agreement-rejection-text"/></p>
                  </div>
                  <div class="modal-footer justify-content-between">
                    <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                    <button type="button" class="btn btn-primary" data-dismiss="modal"
                            onclick="DataAccessService.reject('${dar.id}', 'agreement', '${agreement.id}')"><@message "confirm"/></button>
                  </div>
                </div>
                <!-- /.modal-content -->
              </div>
              <!-- /.modal-dialog -->
            </div>
            <!-- /.modal -->

            <!-- Confirm reopen modal -->
            <div class="modal fade" id="modal-reopen">
              <div class="modal-dialog">
                <div class="modal-content">
                  <div class="modal-header">
                    <h4 class="modal-title"><@message "confirm-reopen-title"/></h4>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                      <span aria-hidden="true">&times;</span>
                    </button>
                  </div>
                  <div class="modal-body">
                    <p><@message "confirm-agreement-reopen-text"/></p>
                  </div>
                  <div class="modal-footer justify-content-between">
                    <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
                    <button type="button" class="btn btn-primary" data-dismiss="modal"
                            onclick="DataAccessService.reopen('${dar.id}', 'agreement', '${agreement.id}')"><@message "confirm"/></button>
                  </div>
                </div>
                <!-- /.modal-content -->
              </div>
              <!-- /.modal-dialog -->
            </div>
            <!-- /.modal -->

          </div>

          <div class="col-sm-12 col-lg-4">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "applicant"/></h3>
              </div>
              <div class="card-body">
                  <@userProfile profile=applicant/>
              </div>
                <#if !dar.archived>
                  <div class="card-footer">
                    <a href="/data-access-comments/${dar.id}"><@message "send-message"/> <i
                        class="fas fa-arrow-circle-right"></i></a>
                  </div>
                </#if>
            </div>
          </div>
          <!-- /.col-6 -->
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
    $('#agreement-form-menu').addClass('active').attr('href', '#');
    $('#agreement-form-menu-${agreement.id}').addClass('active');
</script>
<#include "libs/data-access-form-scripts.ftl">

</body>
</html>
