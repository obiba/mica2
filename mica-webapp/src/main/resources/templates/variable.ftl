<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/variable.ftl">

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
    <@header titlePrefix=(type?lower_case + "-variable") title=variable.name subtitle=""/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <p>
            <#if label??>
              <#if label[.lang]??>
                ${label[.lang]}
              <#elseif label["und"]??>
                ${label["und"]}
              </#if>
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
                            <#if category.attributes.label[.lang]??>
                              ${category.attributes.label[.lang]}
                            <#elseif category.attributes.label["und"]??>
                              ${category.attributes.label["und"]}
                            </#if>
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

          <div class="col-xs-12 col-lg-6">
            <#if annotations?? && annotations?size != 0>
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "annotations"/></h3>
                </div>
                <div class="card-body">
                  <dl class="row">
                    <#list annotations as annotation>
                      <dt class="col-sm-4" title="<#if annotation.vocabularyDescription??>${annotation.vocabularyDescription[.lang]!""}</#if>">
                        ${annotation.vocabularyTitle[.lang]}
                      </dt>
                      <dd class="col-sm-8" title="<#if annotation.termDescription??>${annotation.termDescription[.lang]!""}</#if>">
                        <#if annotation.termTitle[.lang]??>
                          ${annotation.termTitle[.lang]}
                        <#elseif annotation.termTitle["und"]??>
                          <span class="marked">${annotation.termTitle["und"]}</span>
                        </#if>
                      </dd>
                    </#list>
                  </dl>
                </div>
                <div class="card-footer">
                  <a href="#">
                    <@message "find-similar-variables"/> <i class="fas fa-search"></i>
                  </a>
                </div>
              </div>
            </#if>

          </div>
        </div>

        <#if type == "Harmonized" && harmoAnnotations??>
          <div class="row">
            <div class="col-12">
              <div class="card card-<#if harmoAnnotations.hasStatus()>${harmoAnnotations.statusClass}<#else>info</#if> card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "harmonization"/>
                  <#if harmoAnnotations.hasStatus()>
                    <span class=" badge badge-${harmoAnnotations.statusClass}">
                      ${harmoAnnotations.statusValueTitle[.lang]!harmoAnnotations.statusValue!"-"}
                    </span>

                  </#if>
                  </h3>
                </div>
                <div class="card-body">
                  <#if !harmoAnnotations.hasStatusDetail() && !harmoAnnotations.hasAlgorithm() && !harmoAnnotations.hasComment()>
                    <span class="text-muted"><@message "no-harmonization-description"/></span>
                  <#else>
                    <dl>
                      <#if harmoAnnotations.hasStatusDetail()>
                        <dt title="${harmoAnnotations.statusDetailDescription[.lang]!""}">
                          ${harmoAnnotations.statusDetailTitle[.lang]!"Status detail"}
                        </dt>
                        <dd title="${harmoAnnotations.statusDetailValueDescription[.lang]!""}">
                          ${harmoAnnotations.statusDetailValueTitle[.lang]!harmoAnnotations.statusDetailValue!"-"}
                        </dd>
                      </#if>

                      <#if harmoAnnotations.hasAlgorithm()>
                        <dt title="${harmoAnnotations.algorithmDescription[.lang]!""}">
                          ${harmoAnnotations.algorithmTitle[.lang]!"Algorithm"}
                        </dt>
                        <dd>
                          <span class="marked mt-3">${harmoAnnotations.algorithmValue!""}</span>
                        </dd>
                      </#if>

                      <#if harmoAnnotations.hasComment()>
                        <dt title="${harmoAnnotations.commentDescription[.lang]!""}">
                          ${harmoAnnotations.commentTitle[.lang]!"Comment"}
                        </dt>
                        <dd>
                          <span class="marked">${harmoAnnotations.commentValue!""}</span>
                        </dd>
                      </#if>
                    </dl>
                  </#if>
                </div>
              </div>
            </div>
          </div>
        </#if>

        <div class="row">
          <div class="col-12">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "summary-statistics"/></h3>
              </div>
              <div class="card-body">
                <@variableSummary variable=variable/>
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
