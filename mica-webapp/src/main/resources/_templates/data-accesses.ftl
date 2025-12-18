<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/data-access.ftl"/>
<#include "models/profile.ftl"/>

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "data-access-requests"/></title>
</head>
<body id="data-accesses-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header title="data-access-requests"/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <#if user.otpEnabled?? && !user.otpEnabled>
          <div class="alert alert-info">
            <h6><i class="icon fas fa-lock"></i> <@message "security-info"/></h6>
              <@message "2fa-info"/>
              <a href="${contextPath}/profile" class="ms-2">
                  <i class="fa fa-arrow-right"></i> <@message "profile"/>
              </a>
          </div>
        </#if>

        <div id="data-accesses-callout" class="callout callout-info">
          <p>
            <#if isAdministrator || isDAO>
              <@message "data-access-requests-admin-callout"/>
            <#else>
              <@message "data-access-requests-callout"/>
            </#if>
          </p>
        </div>

        <div class="card card-primary card-outline card-outline-tabs">
          <#if users??>
            <div class="card-header p-0 border-bottom-0 ">
              <ul class="nav nav-tabs" id="tabs-tab" role="tablist">
                <li class="nav-item">
                  <a class="nav-link active" id="tabs-dars-tab" data-bs-toggle="pill" href="#tabs-dars" role="tab" aria-controls="tabs-dars" aria-selected="true">
                    <@message "data-access-requests"/> <span class="badge badge-info">${dars?size}</span>
                  </a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" id="tabs-users-tab" data-bs-toggle="pill" href="#tabs-users" role="tab" aria-controls="tabs-users" aria-selected="false">
                    <@message "registered-users"/> <span class="badge badge-info">${users?size}</span>
                  </a>
                </li>
              </ul>
            </div>
          <#else>
            <div class="card-header">
              <h3 class="card-title"><@message "my-data-access-requests"/></h3>
            </div>
          </#if>
          <div class="card-body">

            <#macro darList>
              <div class="mb-3">
                <button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#modal-add">
                  <i class="fa-solid fa-plus"></i> <@message "new-data-access-request"/>
                </button>
              </div>
              <#if dars?? && dars?size gt 0>
                <div class="table-responsive">
                  <table id="dars" class="table table-bordered table-striped">
                    <thead>
                    <tr>
                      <th>ID</th>
                      <#if isAdministrator || isDAO>
                        <th><@message "applicant"/></th>
                      </#if>
                      <th><@message "title"/></th>
                      <th><@message "last-update"/></th>
                      <th><@message "submission-date"/></th>
                      <#if accessConfig.feasibilityEnabled>
                        <th><@message "feasibilities-pending-total"/></th>
                      </#if>
                      <#if accessConfig.amendmentsEnabled>
                        <th><@message "amendments-pending-total"/></th>
                      </#if>
                      <#if accessConfig.agreementEnabled>
                        <th><@message "agreements-pending-total"/></th>
                      </#if>
                      <#if accessConfig.preliminaryEnabled>
                        <th><@message "preliminary-status"/></th>
                      </#if>
                      <th><@message "status"/></th>
                      <#if dataAccessArchiveEnabled>
                        <th><@message "archived"/></th>
                      </#if>
                    </tr>
                    </thead>
                    <tbody>
                    <#list dars as dar>
                      <tr>
                        <td><a href="${contextPath}/data-access/${dar.id}">${dar.id}</a></td>
                        <#if isAdministrator || isDAO>
                          <td>
                            <a href="#" data-bs-toggle="modal" data-bs-target="#modal-${dar.applicant?replace("\\W", "-", "r")}">${applicants[dar.applicant].fullName}</a>
                          </td>
                        </#if>
                        <td>${dar.title!""}</td>
                        <td data-sort="${dar.lastUpdate.toString()}" class="moment-datetime">${dar.lastUpdate.toString()}</td>
                        <td data-sort="<#if dar.submitDate??>${dar.submitDate.toString()}</#if>" class="moment-datetime"><#if dar.submitDate??>${dar.submitDate.toString()}</#if></td>
                        <#if accessConfig.feasibilityEnabled>
                          <td>${dar.pendingFeasibilities}/${dar.totalFeasibilities}</td>
                        </#if>
                        <#if accessConfig.amendmentsEnabled>
                          <td>${dar.pendingAmendments}/${dar.totalAmendments}</td>
                        </#if>
                        <#if accessConfig.agreementEnabled>
                          <td>${dar.pendingAgreements}/${dar.totalAgreements}</td>
                        </#if>
                        <#if accessConfig.preliminaryEnabled>
                          <td><i class="fa-solid fa-circle text-${statusColor(dar.preliminary.status.toString())}"></i> <@message dar.preliminary.status.toString()/></td>
                        </#if>
                        <td><i class="fa-solid fa-circle text-${statusColor(dar.status.toString())}"></i> <@message dar.status.toString()/></td>
                        <#if dataAccessArchiveEnabled>
                          <td><#if dar.archived><i class="fa-solid fa-check"></i></#if></td>
                        </#if>
                      </tr>
                    </#list>
                    </tbody>
                  </table>
                </div>
              <#else>
                <div class="mt-3 text-muted"><@message "no-data-access-requests"/></div>
              </#if>
            </#macro>

            <#if users??>
              <div class="tab-content" id="tabs-tabContent">
                <div class="tab-pane fade show active" id="tabs-dars" role="tabpanel" aria-labelledby="tabs-dars-tab">
                  <@darList/>
                </div>
                <div class="tab-pane fade" id="tabs-users" role="tabpanel" aria-labelledby="tabs-users-tab">
                  <#if users?? && users?size gt 0>
                    <div class="table-responsive">
                      <table id="users" class="table table-bordered table-striped">
                        <thead>
                        <tr>
                          <th><@message "full-name"/></th>
                          <th><@message "groups"/></th>
                          <th><@message "createdDate"/></th>
                          <th><@message "lastLogin"/></th>
                          <@userProfileTHs/>
                        </tr>
                        </thead>
                        <tbody>
                        <#list users as profile>
                          <tr>
                            <td>
                              <a href="#" data-bs-toggle="modal" data-bs-target="#modal-${profile.username?replace("\\W", "-", "r")}">${profile.fullName}</a>
                            </td>
                            <td>
                              <#list profile.groups as group>
                                <span class="badge badge-info">${group}</span>
                              </#list>
                            </td>
                            <td data-sort="${profile.attributes["createdDate"].toString()}" class="moment-datetime">${profile.attributes["createdDate"].toString()}</td>
                            <td data-sort="<#if profile.attributes["lastLogin"]??>${profile.attributes["lastLogin"].toString()}</#if>" class="moment-datetime"><#if profile.attributes["lastLogin"]??>${profile.attributes["lastLogin"].toString()}</#if></td>
                            <@userProfileTDs profile=profile/>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </div>
                  <#else>
                    <span class="text-muted"><@message "no-users"/></span>
                  </#if>
                </div>
                <#if users?? && users?size gt 0>
                  <#list users as profile>
                    <@userProfileDialog profile=profile/>
                  </#list>
                </#if>
              </div>
            <#else>
              <@darList/>
            </#if>
          </div>

          <!-- /.card -->
        </div>

      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <!-- Confirm addition modal -->
  <div class="modal fade" id="modal-add">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><@message "confirm-creation"/></h4>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
            
          </button>
        </div>
        <div class="modal-body">
          <p><@message "confirm-data-access-request-creation"/></p>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-default" data-bs-dismiss="modal"><@message "cancel"/></button>
          <button type="button" class="btn btn-primary" data-bs-dismiss="modal" onclick="DataAccessService.create()"><@message "confirm"/></button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->

  <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/data-access-scripts.ftl">
<!-- page script -->
<script>
    $(function () {
      let options = dataTablesDefaultOpts;
      <#if users??>
        options.order = [[3, 'desc']];
        $("#dars").DataTable(options);
      <#else>
        options = dataTablesSortSearchOpts;
        options.order = [[2, 'desc']];
        $("#dars").DataTable(options);
      </#if>
      $("#users").DataTable(dataTablesDefaultOpts);
    });
</script>

</body>
</html>
