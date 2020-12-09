<!DOCTYPE html>
<html lang="${.lang}">
<head>
    <#include "libs/head.ftl">
  <title>${config.name!""} | Data Access Documents ${dar.id}</title>
</head>
<body id="data-access-documents-page" class="hold-transition sidebar-mini layout-fixed layout-navbar-fixed">
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
              <span class="text-white-50"><@message "data-access-documents"/> /</span> ${dar.id}
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

      <#if dataAccessCalloutsEnabled>
        <div class="row">
          <div class="col-12">
            <div id="data-access-documents-callout" class="callout callout-info">
              <p>
                <@message "data-access-documents-callout"/>
              </p>
            </div>
          </div>
          <!-- /.col-12 -->
        </div>
        <!-- /.row -->
      </#if>

      <div class="row">
        <div class="col-sm-12 col-lg-8">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title"><@message "documents"/></h3>
              <#if !dar.archived>
                <div class="float-right">
                  <a href="#" class="btn btn-primary" data-toggle="modal" data-target="#modal-upload">
                    <i class="fas fa-upload"></i> <@message "upload-document"/></a>
                </div>
              </#if>
            </div>
            <div class="card-body">
              <!-- Format Number of Bytes in SI Units -->
              <#function si num>
                <#assign order     = num?round?c?length />
                <#assign thousands = ((order - 1) / 3)?floor />
                <#if (thousands < 0)><#assign thousands = 0 /></#if>
                <#assign siMap = [ {"factor": 1, "unit": "B"}, {"factor": 1000, "unit": "KB"}, {"factor": 1000000, "unit": "MB"}, {"factor": 1000000000, "unit":"GB"} ]/>
                <#assign siStr = (num / (siMap[thousands].factor))?string("0.#") + siMap[thousands].unit />
                <#return siStr />
              </#function>
              <div class="table-responsive">
                <table id="documents" class="table table-bordered table-striped">
                  <thead>
                  <tr>
                    <th><@message "name"/></th>
                    <th><@message "upload-date"/></th>
                    <th><@message "size"/></th>
                    <#if !dar.archived>
                      <th></th>
                    </#if>
                  </tr>
                  </thead>
                  <tbody>
                  <#list dar.attachments as attachment>
                    <tr>
                      <td><a href="${contextPath}/ws/data-access-request/${dar.id}/attachments/${attachment.id}/_download" download="${attachment.name}">${attachment.name}</a></td>
                      <td data-sort="${attachment.createdDate.toString(datetimeFormat)}" class="moment-datetime">${attachment.createdDate.toString(datetimeFormat)}</td>
                      <td>${si(attachment.size)}</td>
                      <#if !dar.archived>
                        <td>
                          <a href="#" onclick="DataAccessService.deleteAttachment('${dar.id}','${attachment.id}')"><i class="fas fa-trash text-danger"></i></a>
                        </td>
                      </#if>
                    </tr>
                  </#list>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
        <!-- /.col-6 -->
      </div>
      <!-- /.row -->

    </section>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <!-- Action addition modal -->
  <div class="modal fade" id="modal-upload">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><@message "upload-document"/></h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <form>
            <div class="form-group">
              <label for="file-field"><@message "select-file-to-upload"/></label>
              <input type="file" id="file-field" class="form-control-file" onchange="handleFiles(this.files)">
              <input type="hidden" id="file-id">
            </div>
            <div>
              <div id="progress" class="progress active" style="display: none">
                <div id="progress-bar" class="progress-bar bg-success progress-bar-striped" role="progressbar" aria-valuenow="20" aria-valuemin="0" aria-valuemax="100" style="width: 0%">
                  <span id="progress-text"></span>
                </div>
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
          <button type="button" class="btn btn-primary" id="upload-document-submit"><@message "submit"/></button>
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
<script>
  $(function () {
    $('#documents-menu').addClass('active').attr('href', '#');
    $("#documents").DataTable(dataTablesSortSearchOpts);
    $('#upload-document-submit').click(function() {
      var fileId = $('#file-id').val();
      if (fileId) {
        DataAccessService.attachFile('${dar.id}', fileId);
      }
    }).prop('disabled', true);
  });
  var handleFiles = function(files) {
    for (var i = 0; i < files.length; i++) {
      var file = files[i];
      $('#progress-text').text(file.name);
      $('#progress-bar').css('width', '0%');
      $('#progress').show();
      FilesService.uploadTempFile(file, function(fileId) {
        $('#file-id').val(fileId);
      }, function(percentCompleted) {
        $('#progress-bar').css('width', percentCompleted + '%');
        $('#upload-document-submit').prop('disabled', false);
      });
    }
  }
</script>
</body>
</html>
