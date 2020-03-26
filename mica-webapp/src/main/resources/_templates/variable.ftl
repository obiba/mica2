<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/variable.ftl">
<#include "models/population.ftl">
<#include "models/dce.ftl">

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
    <#assign title = variable.name/>
    <#if opalTable?? && opalTable.name??>
      <#assign title = (variable.name + " [" + localize(opalTable.name) + "]")/>
    </#if>
    <@header titlePrefix=(type?lower_case + "-variable") title=title subtitle=""/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <#if variable.attributes?? && variable.attributes.label??>
            <p>${localize(variable.attributes.label)}</p>
          <#else>
            <p class="text-muted"><@message "no-label"/></p>
          </#if>
          <#if variable.attributes?? && variable.attributes.description??>
            <p class="text-muted"><i class="fas fa-info-circle"></i> ${localize(variable.attributes.description)}</p>
          </#if>
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
                  <dl>
                    <dt><@message "categories"/></dt>
                    <dd>
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
                              <#if category.attributes??>
                                ${localize(category.attributes.label)}
                              </#if>
                            </td>
                            <td><#if category.missing><i class="fas fa-check"></i></#if></td>
                          </tr>
                        </#list>
                        </tbody>
                      </table>
                    </dd>
                  </dl>
                </#if>
              </div>
            </div>
          </div>

          <div class="col-xs-12 col-lg-6">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "definition"/></h3>
              </div>
              <div class="card-body">
                <dl class="row">
                  <dt class="col-sm-4"><@message "dataset"/></dt>
                  <dd class="col-sm-8">
                    <a class="btn btn-success" href="../dataset/${variable.datasetId}">
                      <#if type == "Collected">
                        <i class="${datasetIcon}"></i>
                      <#else>
                        <i class="${harmoDatasetIcon}"></i>
                      </#if>
                      ${localize(variable.datasetAcronym, variable.datasetId)}
                    </a>
                  </dd>

                  <dt class="col-sm-4"><@message "study"/></dt>
                  <dd class="col-sm-8"><a href="../study/${study.id}">${localize(study.acronym, study.id)}</a></dd>
                  <dt class="col-sm-4"><@message "population"/></dt>
                  <dd class="col-sm-8">
                    <a href="#" data-toggle="modal" data-target="#modal-${population.id}">${localize(population.name, population.id)}</a>
                    <@populationDialog id=population.id population=population></@populationDialog>
                  </dd>
                  <#if dce??>
                    <dt class="col-sm-4"><@message "data-collection-event"/></dt>
                    <dd class="col-sm-8">
                      <#assign dceId="${population.id}-${dce.id}">
                      <a href="#" data-toggle="modal" data-target="#modal-${dceId}">${localize(dce.name, dce.id)}</a>
                      <@dceDialog id=dceId dce=dce></@dceDialog>
                    </dd>
                  </#if>
                  <#if type == "Harmonized">
                    <dt class="col-sm-4"><@message "dataschema-variable"/></dt>
                    <dd class="col-sm-8"><a href="../variable/${variable.datasetId}:${variable.name}:Dataschema" class="btn btn-primary"><i class="${variableIcon}"></i> ${variable.name}</a></dd>
                  </#if>
                  <#if opalTable?? && (opalTable.name?? || opalTable.description??)>
                    <dt class="col-sm-4"><@message "datasource-info"/></dt>
                    <dd class="col-sm-8">
                      <#if opalTable.name??>
                        [${localize(opalTable.name)}]
                      </#if>
                      <#if opalTable.description??>
                        <span class="text-muted">${localize(opalTable.description)}</span>
                      </#if>
                    </dd>
                  </#if>
                </dl>
              </div>
            </div>
          </div>
        </div>


        <div class="row">
          <div class="col-sm-12 col-lg-6">
            <#if annotations?? && annotations?size != 0>
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "annotations"/></h3>
                </div>
                <div class="card-body">
                  <dl class="row">
                    <#list annotations as annotation>
                      <dt class="col-sm-4" title="<#if annotation.vocabularyDescription??>${localize(annotation.vocabularyDescription)}</#if>">
                        ${localize(annotation.vocabularyTitle)}
                      </dt>
                      <dd class="col-sm-8" title="<#if annotation.termDescription??>${localize(annotation.termDescription)}</#if>">
                        <span class="marked">${localize(annotation.termTitle)}</span>
                      </dd>
                    </#list>
                  </dl>
                </div>
                <div class="card-footer">
                  <a href="../search#lists?type=variables&query=${query}">
                    <@message "find-similar-variables"/> <i class="fas fa-search"></i>
                  </a>
                </div>
              </div>
            </#if>
          </div>
          <div class="col-sm-12 col-lg-6">
            <#if type == "Harmonized">
              <div class="card card-${harmoAnnotations.statusClass} card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "harmonization"/>
                  <#if harmoAnnotations.hasStatus()>
                    <span class=" badge badge-${harmoAnnotations.statusClass}">
                      ${localize(harmoAnnotations.statusValueTitle, harmoAnnotations.statusValue!"-")}
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
                        <dt title="${localize(harmoAnnotations.statusDetailDescription)}">
                          ${localize(harmoAnnotations.statusDetailTitle, "Status detail")}
                        </dt>
                        <dd title="${localize(harmoAnnotations.statusDetailValueDescription)}">
                          ${localize(harmoAnnotations.statusDetailValueTitle, harmoAnnotations.statusDetailValue!"-")}
                        </dd>
                      </#if>

                      <#if harmoAnnotations.hasAlgorithm()>
                        <dt title="${localize(harmoAnnotations.algorithmDescription)}">
                          ${localize(harmoAnnotations.algorithmTitle, "Algorithm")}
                        </dt>
                        <dd>
                          <span class="marked mt-3">${harmoAnnotations.algorithmValue!""}</span>
                        </dd>
                      </#if>

                      <#if harmoAnnotations.hasComment()>
                        <dt title="${localize(harmoAnnotations.commentDescription)}">
                          ${localize(harmoAnnotations.commentTitle, "Comment")}
                        </dt>
                        <dd>
                          <span class="marked">${harmoAnnotations.commentValue!""}</span>
                        </dd>
                      </#if>
                    </dl>
                  </#if>
                </div>
              </div>
            </#if>
          </div>
        </div>

        <div class="row">
          <div class="col-12">
            <div class="card card-info card-outline">
              <div class="card-header">
                <h3 class="card-title"><@message "summary-statistics"/></h3>
              </div>
              <div class="card-body">
                <#if user?? || !config.variableSummaryRequiresAuthentication>
                  <@variableSummary variable=variable/>
                <#else>
                  <@message "sign-in-for-variable-statistics"/>
                  <a href="../signin?redirect=/variable/${variable.id}" class="btn btn-info"><@message "sign-in"/></a>
                </#if>
              </div>
            </div>
          </div>
        </div>

        <#if type == "Dataschema">
          <div class="row">
            <div class="col-12">
              <div class="card card-info card-outline">
                <div class="card-header">
                  <h3 class="card-title"><@message "harmonized-variables"/></h3>
                </div>
                <div class="card-body">
                  <img id="loadingHarmonizedVariables" src="/assets/images/loading.gif">

                  <table id="harmonizedVariables" class="table table-striped" style="display: none">
                    <thead>
                      <tr>
                        <th><@message "variable"/></th>
                        <th><@message "study"/></th>
                        <th><@message "data-collection-event"/></th>
                        <th><@message "status"/></th>
                        <th><@message "status-detail"/></th>
                        <th><@message "comment"/></th>
                      </tr>
                    </thead>
                    <tbody></tbody>
                  </table>
                  <div id="noHarmonizedVariables" style="display: none">
                    <span class="text-muted"><@message "no-harmonized-variables"/></span>
                  </div>
                </div>
              </div>
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
<#include "libs/variable-scripts.ftl">

</body>
</html>
