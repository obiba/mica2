<!-- Macros -->
<#include "models/compare.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} |
    <#if type == "studies">
      <@message "compare-studies-title"/>
    <#elseif type == "networks">
      <@message "compare-networks-title"/>
    </#if>
  </title>
</head>
<body id="compare-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="app-wrapper d-flex flex-column min-vh-100">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
 <div class="app-main flex-fill">
    <!-- Content Header (Page header) -->
    <div class="content-header bg-info mb-4">
      <div class="container">
        <div class="row mb-2">
          <div class="col-sm-6">
            <h1 class="m-0">
              <#if type == "studies">
                <@message "compare-studies-title"/>
              <#elseif type == "networks">
                <@message "compare-networks-title"/>
              </#if>
            </h1>
          </div><!-- /.col -->
          <div class="col-sm-6">

          </div><!-- /.col -->
        </div><!-- /.row -->
      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <p>
            <#if type == "studies">
              <@message "compare-studies-text"/>
            <#elseif type == "networks">
              <@message "compare-networks-text"/>
            </#if>
          </p>
        </div>

        <#if type == "studies">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title"><@message "studies"/></h3>
            </div>
            <div class="card-body">
              <#if ids?size gt (individualStudies?size + harmonizationStudies?size)>
                <div class="alert alert-warning alert-dismissible">
                  <button type="button" class="btn-close" data-bs-dismiss="alert" aria-hidden="true">×</button>
                  <@messageArgs code="compare-max-items" args=["${config.maxItemsPerCompare}"]/>
                </div>
              </#if>

              <#if (individualStudies?size + harmonizationStudies?size) == 0>
                <span class="text-muted"><@message "compare-studies-none"/></span>
              <#else>
                <#if individualStudies?size gt 0 && harmonizationStudies?size gt 0>
                  <ul id="studies-tabs" class="nav nav-pills mb-3">
                    <#if individualStudies?size gt 0>
                      <li class="nav-item">
                        <a id="individual-studies-tab" class="nav-link active" href="#tab_individual_studies" data-bs-toggle="tab">
                            <@message "individual"/>
                          <span class="badge badge text-bg-light">${individualStudies?size}</span>
                        </a>
                      </li>
                    </#if>
                    <#if harmonizationStudies?size gt 0>
                      <li class="nav-item">
                        <a id="harmonization-studies-tab" class="nav-link" href="#tab_harmonization_studies" data-bs-toggle="tab">
                            <@message "harmonization"/>
                          <span class="badge badge text-bg-light">${harmonizationStudies?size}</span>
                        </a>
                      </li>
                    </#if>
                  </ul>
                </#if>
                <div class="tab-content">
                  <div class="tab-pane active" id="tab_individual_studies">
                    <#if individualQuery??>
                      <div class="float-end">
                        <a class="btn btn-sm btn-info ms-2" href="${contextPath}/individual-search#lists?type=${type}&query=${individualQuery}">
                          <i class="fa-solid fa-search"></i>
                        </a>
                      </div>
                    </#if>
                    <#if individualStudies?size gt 0>
                      <table id="compare-individual-studies" class="table table-responsive table-striped">
                        <thead>
                        <tr>
                          <th></th>
                          <#list individualStudies as study>
                            <th><a href="${contextPath}/study/${study.id}" target="_blank">${localize(study.acronym)}</a></th>
                          </#list>
                        </tr>
                        </thead>
                        <tbody>
                          <@individualStudiesCompareModel studies=individualStudies/>
                        </tbody>
                      </table>
                    </#if>
                  </div>
                  <div class="tab-pane <#if individualStudies?size == 0>active</#if>" id="tab_harmonization_studies">
                    <#if harmonizationQuery??>
                      <div class="float-end">
                        <a class="btn btn-sm btn-info ms-2" href="${contextPath}/harmonization-search#lists?type=${type}&query=${harmonizationQuery}">
                          <i class="fa-solid fa-search"></i>
                        </a>
                      </div>
                    </#if>
                    <#if harmonizationStudies?size gt 0>
                      <table id="compare-harmonization-studies" class="table table-responsive table-striped">
                        <thead>
                        <tr>
                          <th></th>
                            <#list harmonizationStudies as study>
                              <th><a href="${contextPath}/study/${study.id}" target="_blank">${localize(study.acronym)}</a></th>
                            </#list>
                        </tr>
                        </thead>
                        <tbody>
                        <@harmonizationStudiesCompareModel studies=harmonizationStudies/>
                        </tbody>
                      </table>
                    </#if>
                  </div>
                </div>
              </#if>
            </div>
          </div>
        </#if>

        <#if type == "networks">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title"><@message "networks"/></h3>
            </div>
            <div class="card-body">
              <#if ids?size gt networks?size>
                <div class="alert alert-warning alert-dismissible">
                  <button type="button" class="btn-close" data-bs-dismiss="alert" aria-hidden="true">×</button>
                    <@messageArgs code="compare-max-items" args=["${config.maxItemsPerCompare}"]/>
                </div>
              </#if>
              <#if networks?size == 0>
                <span class="text-muted"><@message "compare-networks-none"/></span>
              <#else>
                <#if query??>
                  <div class="float-end">
                    <a class="btn btn-sm btn-info ms-2" href="${contextPath}/search#lists?type=${type}&query=${query}">
                      <i class="fa-solid fa-search"></i>
                    </a>
                  </div>
                </#if>
                <table id="compare-networks" class="table table-responsive table-striped">
                <thead>
                <tr>
                  <th></th>
                  <#list networks as network>
                    <th><a href="${contextPath}/network/${network.id}" target="_blank">${localize(network.acronym)}</a></th>
                  </#list>
                </tr>
                </thead>
                <tbody>
                  <@networksCompareModel networks=networks/>
                </tbody>
              </table>
              </#if>
            </div>
          </div>
        </#if>

      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

    <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/document-compare-scripts.ftl">

</body>
</html>
