<!-- Macros -->
<#include "libs/header.ftl">
<#include "models/datasets.ftl">

<!-- Template variables -->
<#if !type??>
  <#assign title = "datasets">
  <#assign className = "Study,HarmonizationStudy">
  <#assign showTypeColumn = true>
<#elseif type == "Harmonized">
  <#assign title = "harmonized-datasets">
  <#assign className = "HarmonizationStudy">
  <#assign showTypeColumn = false>
<#else>
  <#assign title = "collected-datasets">
  <#assign className = "Study">
  <#assign showTypeColumn = false>
</#if>

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message title/></title>
</head>
<body id="${title}-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">
    <!-- Content Header (Page header) -->
    <@header title=title breadcrumb=[["${contextPath}/", "home"], [title]]/>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div id="${title}-callout" class="callout callout-info">
          <p><@message (title + "-callout")/></p>
        </div>

        <#if datasets?? && datasets?size != 0>
          <div id="${title}-card" class="card card-info card-outline">
            <div class="card-header d-flex p-0">
              <h3 class="card-title p-3"><@message title/></h3>
              <#if datasetListDisplays?size gt 1>
                <ul class="nav nav-pills ml-auto p-2">
                  <#list datasetListDisplays as display>
                    <#if display == "table">
                      <li class="nav-item"><a class="nav-link <#if datasetListDefaultDisplay == "table">active</#if>" href="#table" data-toggle="tab">
                          <i class="fas fa-table"></i></a>
                      </li>
                    </#if>
                    <#if display == "lines">
                      <li class="nav-item"><a class="nav-link <#if datasetListDefaultDisplay == "lines">active</#if>" href="#lines" data-toggle="tab">
                          <i class="fas fa-grip-lines"></i></a>
                      </li>
                    </#if>
                    <#if display == "cards">
                      <li class="nav-item"><a class="nav-link <#if datasetListDefaultDisplay == "cards">active</#if>" href="#cards" data-toggle="tab">
                          <i class="fas fa-grip-horizontal"></i></a>
                      </li>
                    </#if>
                  </#list>
                </ul>
              </#if>
            </div><!-- /.card-header -->

            <div class="card-body">
              <#if config.studyDatasetEnabled && config.harmonizationDatasetEnabled>
                <div class="mb-4">
                  <div role="group" class="btn-group">
                    <button onclick="window.location.href='${contextPath}/datasets';" type="button" class="btn btn-sm btn-info <#if !type??>active</#if>"><@message "all"/></button>
                    <button onclick="window.location.href='${contextPath}/collected-datasets';" type="button" class="btn btn-sm btn-info <#if type?? && type == "Collected">active</#if>"><@message "collected"/></button>
                    <button onclick="window.location.href='${contextPath}/harmonized-datasets';" type="button" class="btn btn-sm btn-info <#if type?? && type == "Harmonized">active</#if>"><@message "harmonized"/></button>
                  </div>
                </div>
              </#if>
              <div class="tab-content">
                <#if datasetListDisplays?seq_contains("table")>
                  <div class="tab-pane <#if datasetListDefaultDisplay == "table">active</#if>" id="table">
                    <div class="table-responsive">
                      <table id="${title}" class="table table-bordered table-striped">
                        <thead>
                        <@datasetTableHeadModel/>
                        </thead>
                        <tbody>
                        <#list datasets as ds>
                          <@datasetTableRowModel dataset=ds/>
                        </#list>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </#if>

                <#if datasetListDisplays?seq_contains("lines")>
                  <div class="tab-pane <#if datasetListDefaultDisplay == "lines">active</#if>" id="lines">
                    <#list datasets as ds>
                      <div class="border-bottom mb-3 pb-3" style="min-height: 150px;">
                        <div class="row">
                          <@datasetLineModel dataset=ds/>
                        </div>
                      </div>
                    </#list>
                  </div>
                </#if>

                <#if datasetListDisplays?seq_contains("cards")>
                  <div class="tab-pane <#if datasetListDefaultDisplay == "cards">active</#if>" id="cards">
                    <@datasetCardModel/>
                  </div>
                </#if>

              </div>
            </div>
          </div>
        <#else>
          <div id="${title}-card" class="card card-info card-outline">
            <div class="card-header d-flex p-0">
              <h3 class="card-title p-3"><@message "datasets"/></h3>
            </div><!-- /.card-header -->
            <div class="card-body">
              <#if config.openAccess || user??>
                <p class="text-muted"><@message "no-datasets"/></p>
              <#else>
                <p class="text-muted"><@message "sign-in-datasets"/></p>
                <button type="button" onclick="location.href='${contextPath}/signin?redirect=${contextPath}/<#if type??>${type?lower_case}-</#if>datasets';" class="btn btn-success btn-lg">
                  <i class="fas fa-sign-in-alt"></i> <@message "sign-in"/>
                </button>
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
<script>
  const Mica = {
    config: ${configJson!"{}"},
    locale: "${.lang}",
    defaultLocale: "${defaultLang}"
  };

  Mica.tr = {
    "collected-dataset": "<@message "collected-dataset"/>",
    "collected-datasets": "<@message "collected-datasets"/>",
    "harmonized-dataset": "<@message "harmonized-dataset"/>",
    "harmonized-datasets": "<@message "harmonized-datasets"/>",
    "collected-variable": "<@message "collected-variable"/>",
    "collected-variables": "<@message "collected-variables"/>",
    "harmonized-variable": "<@message "harmonized-variable"/>",
    "harmonized-variables": "<@message "harmonized-variables"/>",
    "number-participants": "<@message "number-participants"/>",
    "cohort_study": "<@message "study_taxonomy.vocabulary.methods-design.term.cohort_study.title"/>",
    "case_control": "<@message "study_taxonomy.vocabulary.methods-design.term.case_control.title"/>",
    "case_only": "<@message "study_taxonomy.vocabulary.methods-design.term.case_only.title"/>",
    "cross_sectional": "<@message "study_taxonomy.vocabulary.methods-design.term.cross_sectional.title"/>",
    "clinical_trial": "<@message "study_taxonomy.vocabulary.methods-design.term.clinical_trial.title"/>",
    "other": "<@message "study_taxonomy.vocabulary.methods-design.term.other.title"/>",
    "listing-typeahead-placeholder": "<@message "global.list-search-placeholder"/>",
  };
</script>
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/libs/node_modules/rql/dist/rql.js"></script>
<script src="${assetsPath}/js/mica-list-entities.js"></script>
<script>
    $(function () {
      $("#${title}").DataTable(dataTablesDefaultOpts);

      const sortOptionsTranslations = {
        'name': '<@message "global.name"/>',
        'acronym': '<@message "acronym"/>',
        'studyTable.studyId,studyTable.populationWeight,studyTable.dataCollectionEventWeight,acronym': '<@message "global.chronological"/>'
      };

      if (document.querySelector("#cards")) {
        ObibaDatasetsApp.build("#cards", "${title}", "${.lang}", sortOptionsTranslations);
      }
    });
</script>
</body>
</html>
