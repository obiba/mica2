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
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
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
              <#if studies?size gt 0>
                <div class="float-right">
                  <a class="btn btn-info ml-2" href="${contextPath}/search#lists?type=${type}&query=${query}">
                    <i class="fas fa-search"></i>
                  </a>
                </div>
              </#if>
            </div>
            <div class="card-body">
              <#if ids?size gt studies?size>
                <div class="alert alert-warning alert-dismissible">
                  <button type="button" class="close" data-dismiss="alert" aria-hidden="true">×</button>
                  <@messageArgs code="compare-max-items" args=["${config.maxItemsPerCompare}"]/>
                </div>
              </#if>
              <#if studies?size == 0>
                <span class="text-muted"><@message "compare-studies-none"/></span>
              <#else>
                <table id="compare-studies" class="table table-responsive table-striped">
                <thead>
                <tr>
                  <th></th>
                  <#list studies as study>
                    <th><a href="${contextPath}/study/${study.id}" target="_blank">${localize(study.acronym)}</a></th>
                  </#list>
                </tr>
                </thead>
                <tbody>
                  <tr id="name">
                    <td class="font-weight-bold">
                      <@message "name"/>
                      <small><a href="javascript:void(0)" onclick="onRemoveRow('name')"><i class="fa fa-times"></i></a></small>
                    </td>
                    <#list studies as study>
                      <td>${localize(study.name)}</td>
                    </#list>
                  </tr>
                  <tr id="objectives">
                    <td class="font-weight-bold">
                      <@message "study.objectives"/>
                      <small><a href="javascript:void(0)" onclick="onRemoveRow('objectives')"><i class="fa fa-times"></i></a></small>
                    </td>
                    <#list studies as study>
                      <td>
                        <small class="marked truncate truncate-300">
                          <template>${localize(study.objectives)}</template></small>
                      </td>
                    </#list>
                  </tr>
                  <tr id="start-year">
                    <td class="font-weight-bold">
                      <@message "study.start-year"/>
                      <small><a href="javascript:void(0)" onclick="onRemoveRow('start-year')"><i class="fa fa-times"></i></a></small>
                    </td>
                    <#list studies as study>
                      <td>
                        <#if study.model.startYear??>
                          ${study.model.startYear?c}
                        </#if>
                      </td>
                    </#list>
                  </tr>
                  <tr id="end-year">
                    <td class="font-weight-bold">
                      <@message "study.end-year"/>
                      <small><a href="javascript:void(0)" onclick="onRemoveRow('end-year')"><i class="fa fa-times"></i></a></small>
                    </td>
                    <#list studies as study>
                      <td>
                        <#if study.model.endYear??>
                            ${study.model.endYear?c}
                        </#if>
                      </td>
                    </#list>
                  </tr>
                  <tr id="funding">
                    <td class="font-weight-bold">
                      <@message "funding"/>
                      <small><a href="javascript:void(0)" onclick="onRemoveRow('funding')"><i class="fa fa-times"></i></a></small>
                    </td>
                    <#list studies as study>
                      <td>${localize(study.funding)}</td>
                    </#list>
                  </tr>
                  <tr id="suppl-info">
                    <td class="font-weight-bold">
                      <@message "suppl-info"/>
                      <small><a href="javascript:void(0)" onclick="onRemoveRow('suppl-info')"><i class="fa fa-times"></i></a></small>
                    </td>
                    <#list studies as study>
                      <td><small class="marked truncate truncate-100"><template>${localize(study.model.info)}</template></small></td>
                    </#list>
                  </tr>
                  <tr id="website">
                    <td class="font-weight-bold">
                      <@message "website"/>
                      <small><a href="javascript:void(0)" onclick="onRemoveRow('website')"><i class="fa fa-times"></i></a></small>
                    </td>
                    <#list studies as study>
                      <td>
                        <#if study.model.website??>
                          <a class="d-print-none" href="${study.model.website}" target="_blank">${localize(study.acronym)}</a>
                          <span class="d-none d-print-block">${study.model.website}</span>
                        </#if>
                      </td>
                    </#list>
                  </tr>
                  </tbody>
              </table>
              </#if>
            </div>
          </div>
        </#if>

        <#if type == "networks">
          <div class="card card-info card-outline">
            <div class="card-header">
              <h3 class="card-title"><@message "networks"/></h3>
              <#if networks?size gt 0>
                <div class="float-right">
                  <a class="btn btn-info ml-2" href="${contextPath}/search#lists?type=${type}&query=${query}">
                    <i class="fas fa-search"></i>
                  </a>
                </div>
              </#if>
            </div>
            <div class="card-body">
              <#if ids?size gt networks?size>
                <div class="alert alert-warning alert-dismissible">
                  <button type="button" class="close" data-dismiss="alert" aria-hidden="true">×</button>
                    <@messageArgs code="compare-max-items" args=["${config.maxItemsPerCompare}"]/>
                </div>
              </#if>
              <#if networks?size == 0>
                <span class="text-muted"><@message "compare-networks-none"/></span>
              <#else>
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
                <tr id="name">
                  <td class="font-weight-bold">
                      <@message "name"/>
                    <small><a href="javascript:void(0)" onclick="onRemoveRow('name')"><i class="fa fa-times"></i></a></small>
                  </td>
                  <#list networks as network>
                    <td>${localize(network.name)}</td>
                  </#list>
                </tr>
                <tr id="description">
                  <td class="font-weight-bold">
                      <@message "network.description"/>
                    <small><a href="javascript:void(0)" onclick="onRemoveRow('description')"><i class="fa fa-times"></i></a></small>
                  </td>
                  <#list networks as network>
                    <td>
                      <small class="marked truncate truncate-300">
                        <template>${localize(network.description)}</template></small>
                    </td>
                  </#list>
                </tr>
                <tr id="website">
                  <td class="font-weight-bold">
                      <@message "website"/>
                    <small><a href="javascript:void(0)" onclick="onRemoveRow('website')"><i class="fa fa-times"></i></a></small>
                  </td>
                    <#list networks as network>
                      <td>
                          <#if network.model.website??>
                            <a class="d-print-none" href="${network.model.website}" target="_blank">${localize(network.acronym)}</a>
                            <span class="d-none d-print-block">${network.model.website}</span>
                          </#if>
                      </td>
                    </#list>
                </tr>
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
