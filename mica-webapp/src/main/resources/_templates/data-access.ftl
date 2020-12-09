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
            <h1 class="m-0 float-left">
              <span class="text-white-50"><@message "data-access"/> /</span> ${dar.id}
            </h1>
              <#if permissions?seq_contains("DELETE")>
                <button type="button" class="btn btn-danger ml-4" data-toggle="modal" data-target="#modal-delete">
                  <i class="fas fa-trash"></i> <@message "delete"/>
                </button>
              </#if>
              <#if dataAccessArchiveEnabled>
                <#if permissions?seq_contains("ARCHIVE")>
                  <button type="button" class="btn btn-warning ml-4" data-toggle="modal" data-target="#modal-archive">
                    <i class="fas fa-box"></i> <@message "archive"/>
                  </button>
                </#if>
                <#if permissions?seq_contains("UNARCHIVE")>
                  <button type="button" class="btn btn-warning ml-4" data-toggle="modal" data-target="#modal-unarchive">
                    <i class="fas fa-box-open"></i> <@message "unarchive"/>
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
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p><@message "confirm-deletion-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal"
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
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p><@message "confirm-archive-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal"
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
            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body">
            <p><@message "confirm-unarchive-text"/></p>
          </div>
          <div class="modal-footer justify-content-between">
            <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
            <button type="button" class="btn btn-primary" data-dismiss="modal"
                    onclick="DataAccessService.unarchive('${dar.id}')"><@message "confirm"/>
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
      <div class="row">
        <div class="col-md-3 col-sm-6">

            <#if dar.status.toString() == "OPENED">
                <#assign boxIcon = "fa fa-pen"/>
                <#assign boxProgress = "10"/>
                <#assign boxText = "data-access-progress-opened"/>
            <#elseif dar.status.toString() == "APPROVED">
                <#assign boxIcon = "fa fa-check"/>
                <#assign boxProgress = "100"/>
                <#assign boxText = "data-access-progress-approved"/>
            <#elseif dar.status.toString() == "REJECTED">
                <#assign boxIcon = "fa fa-ban"/>
                <#assign boxProgress = "100"/>
                <#assign boxText = "data-access-progress-rejected"/>
            <#elseif dar.status.toString() == "SUBMITTED">
                <#assign boxIcon = "far fa-clock"/>
                <#assign boxProgress = "30"/>
                <#assign boxText = "data-access-progress-submitted"/>
            <#elseif dar.status.toString() == "REVIEWED">
                <#assign boxIcon = "far fa-clock"/>
                <#assign boxProgress = "50"/>
                <#assign boxText = "data-access-progress-reviewed"/>
            <#elseif dar.status.toString() == "CONDITIONALLY_APPROVED">
                <#assign boxIcon = "fa fa-pen"/>
                <#assign boxProgress = "80"/>
                <#assign boxText = "data-access-progress-conditionally-approved"/>
            <#else>
                <#assign boxIcon = "far fa-clock"/>
                <#assign boxProgress = "50"/>
                <#assign boxText = ""/>
            </#if>

          <div class="info-box bg-${statusColor(dar.status.toString())}">
            <span class="info-box-icon"><i class="${boxIcon}"></i></span>

            <div class="info-box-content">
              <span class="info-box-text"><@message "status"/></span>
              <span class="info-box-number"><@message dar.status.toString()/></span>

              <div class="progress">
                <div class="progress-bar" style="width: ${boxProgress}%"></div>
              </div>
              <span class="progress-description">
                <small><@message boxText/></small>
              </span>
            </div>
            <!-- /.info-box-content -->
          </div>
          <!-- /.info-box -->
        </div>

        <#if lastFeasibility??>

          <div class="col-md-3 col-sm-6">
            <#if lastFeasibility.status.toString() == "OPENED">
              <#assign boxIcon = "fa fa-pen"/>
              <#assign boxProgress = "10"/>
              <#assign boxText = "data-access-feasibility-progress-opened"/>
            <#elseif lastFeasibility.status.toString() == "APPROVED">
              <#assign boxIcon = "fa fa-check"/>
              <#assign boxProgress = "100"/>
              <#assign boxText = "data-access-feasibility-progress-approved"/>
            <#elseif lastFeasibility.status.toString() == "REJECTED">
              <#assign boxIcon = "fa fa-ban"/>
              <#assign boxProgress = "100"/>
              <#assign boxText = "data-access-feasibility-progress-rejected"/>
            <#elseif lastFeasibility.status.toString() == "SUBMITTED">
              <#assign boxIcon = "far fa-clock"/>
              <#assign boxProgress = "30"/>
              <#assign boxText = "data-access-feasibility-progress-submitted"/>
            <#else>
              <#assign boxIcon = "far fa-clock"/>
              <#assign boxProgress = "50"/>
              <#assign boxText = ""/>
            </#if>

            <div class="info-box bg-${statusColor(lastFeasibility.status.toString())}">
              <span class="info-box-icon"><i class="${boxIcon}"></i></span>

              <div class="info-box-content">
                <span class="info-box-text"><@message "last-feasibility-status"/> - <a class="text-white" href="${contextPath}/data-access-feasibility-form/${lastFeasibility.id}">${lastFeasibility.id}</a></span>
                <span class="info-box-number"><@message lastFeasibility.status.toString()/></span>

                <div class="progress">
                  <div class="progress-bar" style="width: ${boxProgress}%"></div>
                </div>
                <span class="progress-description">
                  <small><@message boxText/></small>
                </span>
              </div>
              <!-- /.info-box-content -->
            </div>
            <!-- /.info-box -->
          </div>
        </#if>

        <#if lastAmendment??>

          <div class="col-md-3 col-sm-6">
            <#if lastAmendment.status.toString() == "OPENED">
              <#assign boxIcon = "fa fa-pen"/>
              <#assign boxProgress = "10"/>
              <#assign boxText = "data-access-amendment-progress-opened"/>
            <#elseif lastAmendment.status.toString() == "APPROVED">
              <#assign boxIcon = "fa fa-check"/>
              <#assign boxProgress = "100"/>
              <#assign boxText = "data-access-amendment-progress-approved"/>
            <#elseif lastAmendment.status.toString() == "REJECTED">
              <#assign boxIcon = "fa fa-ban"/>
              <#assign boxProgress = "100"/>
              <#assign boxText = "data-access-amendment-progress-rejected"/>
            <#elseif lastAmendment.status.toString() == "SUBMITTED">
              <#assign boxIcon = "far fa-clock"/>
              <#assign boxProgress = "30"/>
              <#assign boxText = "data-access-amendment-progress-submitted"/>
            <#elseif lastAmendment.status.toString() == "REVIEWED">
              <#assign boxIcon = "far fa-clock"/>
              <#assign boxProgress = "50"/>
              <#assign boxText = "data-access-amendment-progress-reviewed"/>
            <#elseif lastAmendment.status.toString() == "CONDITIONALLY_APPROVED">
              <#assign boxIcon = "fa fa-pen"/>
              <#assign boxProgress = "80"/>
              <#assign boxText = "data-access-amendment-progress-conditionally-approved"/>
            <#else>
              <#assign boxIcon = "far fa-clock"/>
              <#assign boxProgress = "50"/>
              <#assign boxText = ""/>
            </#if>

            <div class="info-box bg-${statusColor(lastAmendment.status.toString())}">
              <span class="info-box-icon"><i class="${boxIcon}"></i></span>

              <div class="info-box-content">
                <span class="info-box-text"><@message "last-amendment-status"/> - <a class="text-white" href="${contextPath}/data-access-amendment-form/${lastAmendment.id}">${lastAmendment.id}</a></span>
                <span class="info-box-number"><@message lastAmendment.status.toString()/></span>

                <div class="progress">
                  <div class="progress-bar" style="width: ${boxProgress}%"></div>
                </div>
                <span class="progress-description">
                  <small><@message boxText/></small>
                </span>
              </div>
              <!-- /.info-box-content -->
            </div>
            <!-- /.info-box -->
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
              <#if dar.status == "OPENED">

                <h4><@message "opened-title"/></h4>
                <#if user.username == dar.applicant>
                  <p><@message "opened-applicant-text"/></p>
                  <div>
                    <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
                      <i class="fas fa-pen"></i> <@message "application-form"/>
                    </a>
                  </div>
                <#else>
                  <p><@message "opened-dao-text"/></p>
                </#if>

              <#elseif dar.status == "CONDITIONALLY_APPROVED">

                <h4><@message "conditionally-approved-title"/></h4>
                <#if user.username == dar.applicant>
                  <p><@message "conditionally-approved-applicant-text"/></p>
                  <div>
                    <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
                      <i class="fas fa-pen"></i> <@message "application-form"/>
                    </a>
                  </div>
                <#else>
                  <p><@message "conditionally-approved-dao-text"/></p>
                </#if>

              <#elseif dar.status == "SUBMITTED">

                <h4><@message "submitted-title"/></h4>
                <#if user.username == dar.applicant>
                  <p><@message "submitted-applicant-text"/></p>
                <#else>
                  <p><@message "submitted-dao-text"/></p>
                  <div>
                    <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
                      <i class="fas fa-pen"></i> <@message "application-form"/>
                    </a>
                  </div>
                </#if>

              <#elseif dar.status == "REVIEWED">

                <h4><@message "reviewed-title"/></h4>
                <#if user.username == dar.applicant>
                  <p><@message "reviewed-applicant-text"/></p>
                <#else>
                  <p><@message "reviewed-dao-text"/></p>
                  <div>
                    <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
                      <i class="fas fa-pen"></i> <@message "application-form"/>
                    </a>
                  </div>
                </#if>

              <#elseif dar.status == "APPROVED">

                <#if dataAccessReportTimelineEnabled && reportTimeline.endDate??>
                  <h4><@message "report-timeline-title"/></h4>
                  <p><@message "report-timeline-text"/></p>

                  <@dataAccessTimeline dar=dar reportTimeline=reportTimeline/>
                <#else>
                  <h4><@message "approved-title"/></h4>
                  <#if user.username == dar.applicant>
                    <p><@message "approved-applicant-text"/></p>
                  <#else>
                    <p><@message "approved-dao-text"/></p>
                    <div>
                      <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
                        <i class="fas fa-eye"></i> <@message "application-form"/>
                      </a>
                    </div>
                  </#if>
                </#if>

              <#elseif dar.status == "REJECTED">

                <h4><@message "rejected-title"/></h4>
                <#if user.username == dar.applicant>
                  <p><@message "rejected-applicant-text"/></p>
                <#else>
                  <p><@message "rejected-dao-text"/></p>
                  <div>
                    <a href="${contextPath}/data-access-form/${dar.id}" class="btn btn-primary" >
                      <i class="fas fa-eye"></i> <@message "application-form"/>
                    </a>
                  </div>
                </#if>

              </#if>
            </div>
          </div>
        </div>
        <!-- /.col-6 -->
        <div class="col-sm-12 col-lg-6">

          <#if accessConfig.feasibilityEnabled>
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "feasibilities"/></h3>
                <div class="float-right">
                  <#if !dar.archived && (user.username == dar.applicant || isAdministrator)>
                    <button type="button" class="btn btn-primary ml-4" data-toggle="modal" data-target="#modal-feasibility-add">
                      <i class="fas fa-plus"></i> <@message "new-feasibility"/>
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
                          <td data-sort="${feasibility.lastModifiedDate.toString(datetimeFormat)}" class="moment-datetime">${feasibility.lastModifiedDate.toString(datetimeFormat)}</td>
                          <td data-sort="<#if feasibility.submissionDate??>${feasibility.submissionDate.toString(datetimeFormat)}</#if>" class="moment-datetime"><#if feasibility.submissionDate??>${feasibility.submissionDate.toString(datetimeFormat)}</#if></td>
                          <td><i class="fas fa-circle text-${statusColor(feasibility.status.toString())}"></i> <@message feasibility.status.toString()/></td>
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
                <div class="float-right">
                  <#if !dar.archived && (user.username == dar.applicant || isAdministrator)>
                    <button type="button" class="btn btn-primary ml-4" data-toggle="modal" data-target="#modal-amendment-add">
                      <i class="fas fa-plus"></i> <@message "new-amendment"/>
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
                        <td data-sort="${amendment.lastModifiedDate.toString(datetimeFormat)}" class="moment-datetime">${amendment.lastModifiedDate.toString(datetimeFormat)}</td>
                        <td data-sort="<#if amendment.submissionDate??>${amendment.submissionDate.toString(datetimeFormat)}</#if>" class="moment-datetime"><#if amendment.submissionDate??>${amendment.submissionDate.toString(datetimeFormat)}</#if></td>
                        <td><i class="fas fa-circle text-${statusColor(amendment.status.toString())}"></i> <@message amendment.status.toString()/></td>
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
                  <a href="/data-access-comments/${dar.id}"><@message "send-message"/> <i
                            class="fas fa-arrow-circle-right"></i></a>
                </div>
              </#if>
            </div>
          </#if>

        </div>
        <!-- /.col-6 -->
      </div>
      <!-- /.row -->

    </section>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <!-- Start date modal -->
  <div class="modal fade" id="modal-start-date">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><@message "start-date"/></h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label><@message "date"/></label>
            <div class="input-group">
              <input type="text" id="start-date" class="form-control">
              <div class="input-group-append">
                <span class="input-group-text"><i class="fas fa-calendar-alt"></i></span>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
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
<!-- Datepicker -->
<script src="${assetsPath}/libs/node_modules/bootstrap-datepicker/dist/js/bootstrap-datepicker.min.js"></script>
<script>
  $(function () {
    $('#dashboard-menu').addClass('active').attr('href', '#');
    $("#amendments").DataTable(dataTablesSortOpts);
    $('#start-date').datepicker({
      locale: '${.lang}',
      format: 'yyyy-mm-dd'
    });
    $('#start-date-submit').click(function() {
      DataAccessService.setStartDate('${dar.id}', $('#start-date').val());
    });
  });
</script>

</body>
</html>
