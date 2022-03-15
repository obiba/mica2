<!-- Macros -->
<#include "libs/header.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${localize(dataset.acronym)} | <@message "dataset.crosstab.title"/></title>
</head>
<body id="dataset-crosstab-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
    <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header titlePrefix=(type?lower_case + "-dataset-crosstab") title=(localize(dataset.acronym)) subtitle=localize(dataset.name) breadcrumb=[["/", "home"], ["/datasets", "datasets"], [localize(dataset.acronym)]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <p>
            <@message "dataset-crosstab-callout"/>
          </p>
        </div>

        <div class="card card-info card-outline">
          <div class="card-header">
            <h3 class="card-title"><@message "query"/></h3>
          </div>
          <div class="card-body">
            <div class="row">
              <div class="col-3">
                <select id="select-var1" class="form-control select2" style="width: 100%;"></select>
              </div>
              <div class="col-1 text-center">
                <i class="fa fa-2x fa-times"></i>
              </div>
              <div class="col-3">
                <select id="select-var2" class="form-control select2" style="width: 100%;"></select>
              </div>
              <div class="col-3">
                <a id="submit" class="btn btn-primary" href="#"><@message "submit"/></a>
                <button id="clear" class="btn btn-default" onclick="clearCrosstab()"><@message "clear"/></button>
              </div>
            </div>
          </div>
          <div class="card-footer">
              <#if type == "Harmonized"><@message "associated-protocol"/><#else><@message "associated-dataset"/></#if>
            <a class="btn btn-success ml-2" href="${contextPath}/dataset/${dataset.id}">
              <#if type == "Collected">
                <i class="${datasetIcon}"></i>
              <#else>
                <i class="${harmoDatasetIcon}"></i>
              </#if>
              ${localize(dataset.acronym, dataset.id)}
            </a>
          </div>
        </div>

        <div id="results" class="card" style="display: none;">
          <div class="card-header">
            <h3 class="card-title"><@message "results"/></h3>
            <a id="download" href="#" class="btn btn-primary float-right">
              <i class="fas fa-download"></i> <@message "download"/>
            </a>
            <button id="transpose" class="btn btn-default float-right mr-2" onclick="transposeCrosstab()">
              <i class="fas fa-exchange-alt"></i>
              <@message "transpose"/>
            </button>
          </div>
          <div class="card-body">
            <div id="loadingCrosstab" class="spinner-border spinner-border-sm" role="status"></div>
            <div id="privacy-alert" class="alert alert-warning" style="display: none;">
              <i class="fas fa-exclamation-triangle"></i> <@message "privacy-threshold-applies"/>
            </div>
            <div id="result-panel" style="overflow-x: auto;">
              <div style="display: none;" class="mb-4">
                <select id="select-study" class="form-control select2 float-right" style="width: 100%;"></select>
              </div>
              <div class="table-responsive">
                <table id="crosstab" class="table table-striped"></table>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- /.container -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

    <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/dataset-crosstab-scripts.ftl">

</body>
</html>
