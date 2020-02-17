<!DOCTYPE html>
<html lang="${.lang}">
<head>
    <#include "libs/head.ftl">
  <title>${config.name!""} | Data Access Documents ${dar.id}</title>
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
            <h1 class="m-0">
              <span class="text-white-50">Data Access Documents /</span> ${dar.id}
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

      <div class="row">
        <div class="col-12">
          <div class="callout callout-info">
            <p>
              <@message "data-access-documents-callout"/>
            </p>
          </div>
        </div>
        <!-- /.col-12 -->
      </div>
      <!-- /.row -->

      <div class="row">
        <div class="col-sm-12 col-lg-8">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title"><@message "documents"/></h3>
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
              <table id="documents" class="table table-bordered table-striped">
                <thead>
                <tr>
                  <th><@message "name"/></th>
                  <th><@message "upload-date"/></th>
                  <th><@message "size"/></th>
                  <th></th>
                </tr>
                </thead>
                <tbody>
                <#list dar.attachments as attachment>
                  <tr>
                    <td><a href="#" onclick="micajs.dataAccess.downloadAttachment('${dar.id}','${attachment.id}', '${attachment.name}')">${attachment.name}</a></td>
                    <td class="moment-datetime">${attachment.createdDate.toString(datetimeFormat)}</td>
                    <td>${si(attachment.size)}</td>
                    <td><a href="#" onclick="micajs.dataAccess.deleteAttachment('${dar.id}','${attachment.id}')"><i class="fas fa-trash text-danger"></i></a></td>
                  </tr>
                </#list>
                </tbody>
              </table>
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
    $('#documents-menu').addClass('active').attr('href', '#');
    $("#documents").DataTable(dataTablesSortSearchOpts);
  });
</script>
</body>
</html>
