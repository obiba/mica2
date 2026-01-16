<!-- Macros -->
<#include "models/data-access.ftl"/>
<#include "models/profile.ftl"/>

<!DOCTYPE html>
<html lang="${.lang}">
<head>
    <#include "libs/head.ftl">
  <link rel="stylesheet" href="${assetsPath}/libs/node_modules/bootstrap-datepicker/dist/css/bootstrap-datepicker3.css"></link>
  <title>${config.name!""} | <@message "data-access"/> ${dar.id}</title>
</head>
<body id="data-access-page" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed">
<!-- Site wrapper -->
<div class="app-wrapper">

  <!-- Navbar -->
  <#include "libs/aside-navbar.ftl">
  <!-- /.navbar -->

  <!-- Sidebar -->
  <#include "libs/data-access-sidebar.ftl">
  <!-- /.sidebar -->

  <!-- Content Wrapper. Contains page content -->
 <div class="app-main flex-fill">
    <!-- Content Header (Page header) -->
    <section class="content-header bg-info mb-4">
      <div class="container-fluid">
        <div class="row">
          <div class="col-sm-6">
            <h1 class="m-0 float-start">
              <span class="text-white-50"><@message "data-access"/> /</span> ${dar.id}
            </h1>
              <#if permissions?seq_contains("DELETE")>
                <button type="button" class="btn btn-danger ms-4" data-bs-toggle="modal" data-bs-target="#modal-delete">
                  <i class="fa-solid fa-trash"></i> <@message "delete"/>
                </button>
              </#if>
              <#if dataAccessArchiveEnabled>
                <#if permissions?seq_contains("ARCHIVE")>
                  <button type="button" class="btn btn-warning ms-4" data-bs-toggle="modal" data-bs-target="#modal-archive">
                    <i class="fa-solid fa-box"></i> <@message "archive"/>
                  </button>
                </#if>
                <#if permissions?seq_contains("UNARCHIVE")>
                  <button type="button" class="btn btn-warning ms-4" data-bs-toggle="modal" data-bs-target="#modal-unarchive">
                    <i class="fa-solid fa-box-open"></i> <@message "unarchive"/>
                  </button>
                </#if>
              </#if>
          </div>
          <div class="col-sm-6">
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
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p><@message "confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
                    onclick="DataAccessService.delete('${dar.id}')"><@message "confirm"/>
            </button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->

    <!-- Confirm archive modal -->
    <div class="modal fade" id="modal-archive">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "confirm-archive-title"/></h4>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p><@message "confirm-archive-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
                    onclick="DataAccessService.archive('${dar.id}')"><@message "confirm"/>
            </button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->

    <!-- Confirm archive modal -->
    <div class="modal fade" id="modal-unarchive">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "confirm-unarchive-title"/></h4>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p><@message "confirm-unarchive-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
                    onclick="DataAccessService.unarchive('${dar.id}')"><@message "confirm"/>
            </button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->

    <!-- Add collaborator modal -->
    <div class="modal fade" id="modal-collaborator-add">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"><@message "collaborator-invite-title"/></h4>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
              
            </button>
          </div>
          <div class="modal-body">
            <p><@message "collaborator-invite-text"/></p>
            <#if accessConfig.agreementEnabled>
              <p><@message "collaborator-invite-agreement-text"/></p>
            </#if>
            <div class="input-group mb-3">
              <input id="collaborator-email" name="collaborator-email" type="email" class="form-control" placeholder="<@message "email"/>">
              <span class="input-group-text"><i class="fa-solid fa-envelope"></i></span>
            </div>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
                    onclick="DataAccessService.inviteCollaborator('${dar.id}', $('input[name=collaborator-email]').val())"><@message "invite"/>
            </button>
          </div>
        </div>
        <!-- /.modal-content -->
      </div>
      <!-- /.modal-dialog -->
    </div>
    <!-- /.modal -->

    <!-- Confirm collaborator removal modal -->
    <#if permissions?seq_contains("DELETE_COLLABORATORS")>
      <div class="modal fade" id="modal-collaborator-delete">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h4 class="modal-title"><@message "confirm-collaborator-delete-title"/></h4>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
                
              </button>
            </div>
            <div class="modal-body">
              <p><@message "confirm-collaborator-delete-text"/></p>
              <p>
                <strong id="collaborator-to-delete"></strong>
              </p>
            </div>
            <div class="modal-footer justify-content-between">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><@message "cancel"/></button>
              <button type="button" class="btn btn-primary" data-bs-dismiss="modal"
                      onclick="DataAccessService.deleteCollaborator('${dar.id}', $('#collaborator-to-delete').text())"><@message "confirm"/>
              </button>
            </div>
          </div>
          <!-- /.modal-content -->
        </div>
        <!-- /.modal-dialog -->
      </div>
      <!-- /.modal -->
    </#if>

    <!-- Main content -->
    <section class="content">
      <div class="container-fluid">

      <#if dar.archived>
        <div class="ribbon-wrapper ribbon-xl">
          <div class="ribbon bg-warning text-xl">
            <@message "archived-title"/>
          </div>
        </div>
      </#if>

      <div class="row">
        <div class="col-md-3 col-sm-6">
          <@dataAccessInfoBox/>
        </div>

        <#if lastFeasibility??>
          <div class="col-md-3 col-sm-6">
            <@dataAccessLastFeasibilityInfoBox/>
          </div>
        </#if>

        <#if lastAmendment??>
          <div class="col-md-3 col-sm-6">
            <@dataAccessLastAmendmentInfoBox/>
          </div>
        </#if>
      </div>

      <#if dataAccessCalloutsEnabled>
        <div class="row">
          <div class="col-12">
            <div id="data-access-callout" class="callout callout-info">
              <p>
                <@message "data-access-dashboard-callout"/>
              </p>
            </div>
          </div>
          <!-- /.col-12 -->
        </div>
        <!-- /.row -->
      </#if>

      <div class="row">
        <div class="col-sm-12 col-lg-6">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title"><@message "notes-title"/></h3>
            </div>
            <div class="card-body">
              <@dataAccessNotes/>
            </div>
          </div>

          <#if accessConfig.collaboratorsEnabled || collaborators?has_content>
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "collaborators"/></h3>
                <#if accessConfig.collaboratorsEnabled>
                  <div class="float-end">
                    <#if permissions?seq_contains("ADD_COLLABORATORS")>
                      <button type="button" class="btn btn-primary ms-4" data-bs-toggle="modal" data-bs-target="#modal-collaborator-add">
                        <i class="fa-solid fa-plus"></i> <@message "new-collaborator"/>
                      </button>
                    </#if>
                  </div>
                </#if>
              </div>
              <div class="card-body">
                <@dataAccessCollaborators/>
              </div>
            </div>
          </#if>

        </div>
        <!-- /.col-6 -->
        <div class="col-sm-12 col-lg-6">

          <#if accessConfig.feasibilityEnabled>
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "feasibilities"/></h3>
                <div class="float-end">
                  <#if !dar.archived && (user.username == dar.applicant || isAdministrator)>
                    <button type="button" class="btn btn-primary ms-4" data-bs-toggle="modal" data-bs-target="#modal-feasibility-add">
                      <i class="fa-solid fa-plus"></i> <@message "new-feasibility"/>
                    </button>
                  </#if>
                </div>
              </div>
              <div class="card-body">
                <#if feasibilities?? && feasibilities?size gt 0>
                  <div class="table-responsive">
                    <table id="feasibilities" class="table table-bordered table-striped">
                      <thead>
                      <tr>
                        <th>ID</th>
                        <th><@message "last-update"/></th>
                        <th><@message "submission-date"/></th>
                        <th><@message "status"/></th>
                      </tr>
                      </thead>
                      <tbody>
                      <#list feasibilities as feasibility>
                        <tr>
                          <td><a href="${contextPath}/data-access-feasibility-form/${feasibility.id}">${feasibility.id}</a></td>
                          <td data-sort="${feasibility.lastModifiedDate.get().toString()}" class="moment-datetime">${feasibility.lastModifiedDate.get().toString()}</td>
                          <td data-sort="<#if feasibility.lastSubmission?? && feasibility.lastSubmission.changedOn??>${feasibility.lastSubmission.changedOn.toString()}</#if>" class="moment-datetime"><#if feasibility.lastSubmission?? && feasibility.lastSubmission.changedOn??>${feasibility.lastSubmission.changedOn.toString()}</#if></td>
                          <td><i class="fa-solid fa-circle text-${statusColor(feasibility.status.toString())}"></i> <@message feasibility.status.toString()/></td>
                        </tr>
                      </#list>
                      </tbody>
                    </table>
                  </div>
                <#else>
                  <@message "no-feasibilities"/>
                </#if>
              </div>
            </div>
          </#if>

          <#if accessConfig.amendmentsEnabled && dar.status.toString() == "APPROVED">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "amendments"/></h3>
                <div class="float-end">
                  <#if !dar.archived && (user.username == dar.applicant || isAdministrator)>
                    <button type="button" class="btn btn-primary ms-4" data-bs-toggle="modal" data-bs-target="#modal-amendment-add">
                      <i class="fa-solid fa-plus"></i> <@message "new-amendment"/>
                    </button>
                  </#if>
                </div>
              </div>
              <div class="card-body">
                <#if amendments?? && amendments?size gt 0>
                  <div class="table-responsive">
                    <table id="amendments" class="table table-bordered table-striped">
                      <thead>
                      <tr>
                        <th>ID</th>
                        <th><@message "last-update"/></th>
                        <th><@message "submission-date"/></th>
                        <th><@message "status"/></th>
                      </tr>
                      </thead>
                      <tbody>
                      <#list amendments as amendment>
                      <tr>
                        <td><a href="${contextPath}/data-access-amendment-form/${amendment.id}">${amendment.id}</a></td>
                        <td data-sort="${amendment.lastModifiedDate.get().toString()}" class="moment-datetime">${amendment.lastModifiedDate.get().toString()}</td>
                        <td data-sort="<#if amendment.lastSubmission?? && amendment.lastSubmission.changedOn??>${amendment.lastSubmission.changedOn.toString()}</#if>" class="moment-datetime"><#if amendment.lastSubmission?? && amendment.lastSubmission.changedOn??>${amendment.lastSubmission.changedOn.toString()}</#if></td>
                        <td><i class="fa-solid fa-circle text-${statusColor(amendment.status.toString())}"></i> <@message amendment.status.toString()/></td>
                      </tr>
                      </#list>
                      </tbody>
                    </table>
                  </div>
                <#else>
                  <@message "no-amendments"/>
                </#if>
              </div>
            </div>
          </#if>

          <#if user.username != applicant.username>
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "applicant"/></h3>
              </div>
              <div class="card-body">
                <@userProfile profile=applicant/>
              </div>
              <#if !dar.archived>
                <div class="card-footer">
                  <a href="${contextPath}/data-access-comments/${dar.id}"><@message "send-message"/> <i
                            class="fa-solid fa-arrow-circle-right ms-1"></i></a>
                </div>
              </#if>
            </div>
          </#if>

        </div>
        <!-- /.col-6 -->
      </div>
      <!-- /.row -->

      </div><!-- /.container-fluid -->
    </section>
    <!-- /.content -->
  </div>
  <!-- /.app-main -->

  <!-- Start date modal -->
  <div class="modal fade" id="modal-start-date">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><@message "start-date"/></h4>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
            
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label><@message "date"/></label>
            <div class="input-group">
              <input type="text" id="start-date" class="form-control">
              <div class="input-group-append">
                <span class="input-group-text"><i class="fa-solid fa-calendar-alt"></i></span>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><@message "cancel"/></button>
          <button type="button" class="btn btn-primary" id="start-date-submit"><@message "submit"/></button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->

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
<#include "libs/data-access-dashboard-scripts.ftl">

</body>
</html>
