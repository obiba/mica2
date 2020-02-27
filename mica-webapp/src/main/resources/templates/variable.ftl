<!-- Macros -->
<#include "libs/header.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | ${variable.name}</title>
</head>
<body class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header titlePrefix=(type?lower_case + "-variable") title=variable.name subtitle="" breadcrumb=[["..", "home"], ["../dataset/" + variable.datasetId, variable.datasetAcronym[.lang]!variable.datasetId], [variable.name]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <p>
            <#if label?? && label[.lang]??>
              ${label[.lang]}
            <#else>
              <span class="text-muted"><@message "no-label"/></span>
            </#if>
          </p>
        </div>

        <div class="row">
          <div class="col-xs-12 col-lg-6">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "overview"/></h3>
              </div>
              <div class="card-body">
                <dl class="row">
                  <dt class="col-sm-4"><@message "value-type"/></dt>
                  <dd class="col-sm-8"><@message variable.valueType + "-type"/></dd>
                  <dt class="col-sm-4"><@message "nature"/></dt>
                  <dd class="col-sm-8"><@message variable.nature?lower_case + "-nature"/></dd>
                  <dt class="col-sm-4"><@message "entity-type"/></dt>
                  <dd class="col-sm-8">${variable.entityType}</dd>
                  <#if variable.referencedEntityType?? && variable.referencedEntityType?length gt 0>
                    <dt class="col-sm-4"><@message "referenced-entity-type"/></dt>
                    <dd class="col-sm-8">${variable.referencedEntityType}</dd>
                  </#if>
                  <#if variable.unit?? && variable.unit?length gt 0>
                    <dt class="col-sm-4"><@message "unit"/></dt>
                    <dd class="col-sm-8">${variable.unit}</dd>
                  </#if>
                  <#if variable.mimeType?? && variable.mimeType?length gt 0>
                    <dt class="col-sm-4"><@message "mime-type"/></dt>
                    <dd class="col-sm-8">${variable.mimeType}</dd>
                  </#if>
                  <#if variable.repeatable?? && variable.repeatable>
                    <dt class="col-sm-4"><@message "repeatable"/></dt>
                    <dd class="col-sm-8"><i class="fas fa-check"></i></dd>
                    <dt class="col-sm-4"><@message "occurrence-group"/></dt>
                    <dd class="col-sm-8">${variable.occurrenceGroup}</dd>
                  </#if>
                </dl>

                <#if variable.categories?? && variable.categories?size != 0>
                  <h5><@message "categories"/></h5>

                  <table class="table table-striped table-sm">
                    <thead>
                    <tr>
                      <th><@message "name"/></th>
                      <th><@message "label"/></th>
                      <th><@message "missing"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list variable.categories as category>
                      <tr>
                        <td>${category.name}</td>
                        <td>
                          <#if category.attributes?? && category.attributes.label??>
                            ${category.attributes.label[.lang]!""}
                          </#if>
                        </td>
                        <td><#if category.missing><i class="fas fa-check"></i></#if></td>
                      </tr>
                    </#list>
                    </tbody>
                  </table>
                </#if>
              </div>
              <div class="card-footer">
                <span><@message "defined-in-dataset"/></span>
                <a class="btn btn-success ml-2" href="../dataset/${variable.datasetId}">
                  <#if type == "Collected">
                    <i class="ion ion-grid"></i>
                  <#else>
                    <i class="ion ion-gear-b"></i>
                  </#if>
                  ${variable.datasetAcronym[.lang]!variable.datasetId}
                </a>
              </div>
            </div>
          </div>

          <#if annotations?? && annotations?size != 0>
            <div class="col-xs-12 col-lg-6">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "annotations"/></h3>
                </div>
                <div class="card-body">
                  <dl class="row">
                    <#list annotations as annotation>
                      <dt class="col-sm-4" title="${annotation.vocabularyDescription[.lang]}">${annotation.vocabularyTitle[.lang]}</dt>
                      <dd class="col-sm-8" title="${annotation.termDescription[.lang]}">${annotation.termTitle[.lang]}</dd>
                    </#list>
                  </dl>
                </div>
                <div class="card-footer">
                  <a href="#">
                    <@message "find-similar-variables"/> <i class="fas fa-search"></i>
                  </a>
                </div>
              </div>
            </div>
          </#if>
        </div>

        <div class="row">
          <div class="col-12">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "summary-statistics"/></h3>
              </div>
              <div class="card-body">
                <img id="loadingSummary" src="../assets/images/loading.gif">
                <div id="categoricalSummary" style="display: none">
                  <div class="row">
                    <div class="col-xs-12 col-lg-6">
                      <dl>
                        <dt>N</dt>
                        <dd><span id="totalCount" class="badge badge-info"></span></dd>
                        <dt><@message "frequencies"/></dt>
                        <dd>
                          <table id="frequencyTable" class="table table-striped table-sm">
                            <thead>
                            <tr>
                              <th><@message "value"/></th>
                              <th><@message "frequency"/></th>
                              <th>%</th>
                              <th><@message "missing"/></th>
                            </tr>
                            </thead>
                            <tbody id="frequencyValues">
                            </tbody>
                          </table>
                        </dd>
                      </dl>
                    </div>
                    <div class="col-xs-12 col-lg-6">
                      <canvas id="frequencyChart"></canvas>
                    </div>
                  </div>
                </div>
                <div id="continuousSummary" style="display: none">
                  <div class="row">
                    <div class="col-xs-12 col-lg-4">
                      <dl class="row">
                        <dt class="col-sm-4">N</dt>
                        <dd class="col-sm-8"><span id="n" class="badge badge-info"></span></dd>
                        <dt class="col-sm-4"><@message "n-values"/></dt>
                        <dd id="n-values" class="col-sm-8"></dd>
                        <dt class="col-sm-4"><@message "n-missings"/></dt>
                        <dd id="n-missings" class="col-sm-8"></dd>
                        <dt class="col-sm-4"><@message "mean"/></dt>
                        <dd id="mean" class="col-sm-8"></dd>
                        <dt class="col-sm-4"><@message "stdDev"/></dt>
                        <dd id="stdDev" class="col-sm-8"></dd>
                        <dt class="col-sm-4"><@message "median"/></dt>
                        <dd id="median" class="col-sm-8"></dd>
                        <dt class="col-sm-4"><@message "variance"/></dt>
                        <dd id="variance" class="col-sm-8"></dd>
                        <dt class="col-sm-4"><@message "min"/></dt>
                        <dd id="min" class="col-sm-8"></dd>
                        <dt class="col-sm-4"><@message "max"/></dt>
                        <dd id="max" class="col-sm-8"></dd>
                      </dl>
                    </div>
                    <div class="col-xs-12 col-lg-8">
                      <canvas id="histogramChart"></canvas>
                    </div>
                  </div>
                </div>
                <div id="noSummary" style="display: none">
                  <span class="text-muted"><@message "no-variable-summary"/></span>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

  <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">
<#include "libs/variable-scripts.ftl">

</body>
</html>
