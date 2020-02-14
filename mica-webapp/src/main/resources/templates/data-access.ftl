<!DOCTYPE html>
<html lang="${.lang}">
<head>
    <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "data-access"/> ${dar.id}</title>
</head>
<body class="hold-transition sidebar-mini">
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
                    onclick="micajs.dataAccess.delete('${dar.id}')"><@message "confirm"/>
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
        <div class="col-md-3 col-sm-6 col-12">

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

        <#if lastAmendment??>

          <div class="col-md-3 col-sm-6 col-12">
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
                <span class="info-box-text"><@message "last-amendment-status"/> - <a class="text-white" href="../data-access-amendment-form/${lastAmendment.id}">${lastAmendment.id}</a></span>
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

      <div class="row">
        <div class="col-12">
          <div class="callout callout-info">
            <p>
              This is the dashboard of the data access request.
            </p>
          </div>
        </div>
        <!-- /.col-12 -->
      </div>
      <!-- /.row -->

      <div class="row">
        <div class="col-lg-6">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title">To Do</h3>
            </div>
            <div class="card-body">
              TODO
            </div>
          </div>
        </div>
        <!-- /.col-6 -->
        <div class="col-6">

            <#if user.username != applicant.username>
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "applicant"/></h3>
                  <div class="card-tools">
                    <button type="button" class="btn btn-tool" data-card-widget="collapse"><i class="fas fa-minus"></i>
                    </button>
                  </div>
                </div>
                <div class="card-body">
                  <dl class="row">
                    <dt class="col-sm-4"><@message "full-name"/></dt>
                    <dd class="col-sm-8">${applicant.fullName}</dd>
                    <dt class="col-sm-4"><@message "username"/></dt>
                    <dd class="col-sm-8">${applicant.username}</dd>
                      <#list applicant.attributes?keys as key>
                          <#if key != "realm">
                            <dt class="col-sm-4"><@message key/></dt>
                            <dd class="col-sm-8">
                                <#assign value = applicant.attributes[key]/>
                                <#if key == "email">
                                  <a href="mailto:${value}">${value}</a>
                                <#elseif key == "locale">
                                    ${value?upper_case}
                                <#elseif key == "lastLogin" || key == "createdDate">
                                  <span class="moment-datetime">${value.toString(datetimeFormat)}</span>
                                <#else>
                                    ${value}
                                </#if>
                            </dd>
                          </#if>
                      </#list>
                  </dl>
                </div>
                <div class="card-footer">
                  <a href="${pathPrefix}/data-access-comments/${dar.id}"><@message "send-message"/> <i
                            class="fas fa-arrow-circle-right"></i></a>
                </div>
              </div>
            </#if>

            <#if accessConfig.amendmentsEnabled && dar.status.toString() == "APPROVED">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "amendments"/></h3>
                  <div class="float-right">
                    <#if user.username == dar.applicant || isAdministrator>
                      <button type="button" class="btn btn-primary ml-4" data-toggle="modal" data-target="#modal-amendment-add">
                        <i class="fas fa-plus"></i> <@message "new-amendment"/>
                      </button>
                    </#if>
                  </div>
                </div>
                <div class="card-body">
                  <table id="amendments" class="table table-bordered table-striped">
                    <thead>
                    <tr>
                      <th>ID</th>
                      <th>Last Update</th>
                      <th>Submission Date</th>
                      <th>Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list amendments as amendment>
                    <tr>
                      <td><a href="../data-access-amendment-form/${amendment.id}">${amendment.id}</a></td>
                      <td class="moment-datetime">${amendment.lastModifiedDate.toString(datetimeFormat)}</td>
                      <td class="moment-datetime"><#if amendment.submissionDate??>${dar.submissionDate.toString(datetimeFormat)}</#if></td>
                      <td><@message amendment.status.toString()/></td>
                    </tr>
                    </#list>
                    </tbody>
                  </table>
                </div>
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
        $('#dashboard-menu').addClass('active').attr('href', '#');
        $("#amendments").DataTable(dataTablesSortOpts);
    });
</script>
</body>
</html>
