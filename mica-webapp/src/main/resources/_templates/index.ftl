<!-- Macros -->
<#include "libs/header.ftl">

<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <title>${config.name!""}</title>
  <#include "libs/head.ftl">
</head>
<body id="index-page" class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="wrapper">

  <!-- Navbar -->
  <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->


  <!-- Content Wrapper. Contains page content -->
  <div class="content-wrapper">

    <div class="jumbotron jumbotron-fluid">
      <div class="container">
        <#if config.repositoryEnabled && config.dataAccessEnabled>
          <h1 class="display-4"><@message "data-portal-title"/></h1>
          <p class="lead"><@message "data-portal-text"/></p>
        <#elseif config.repositoryEnabled>
          <h1 class="display-4"><@message "data-repository-portal-title"/></h1>
          <p class="lead"><@message "data-repository-portal-text"/></p>
        <#elseif config.dataAccessEnabled>
          <h1 class="display-4"><@message "data-access-portal-title"/></h1>
          <p class="lead"><@message "data-access-portal-text"/></p>
        </#if>
      </div>
    </div>

    <!-- Main content -->
    <div class="content">
      <div class="container">

        <#if config.repositoryEnabled>
          <div class="row">

            <#if config.networkEnabled && !config.singleNetworkEnabled>
              <div class="col-lg-3 col-sm-6">
                <!-- small box -->
                <div class="small-box bg-info">
                  <div class="inner">
                    <h3 id="network-hits">-</h3>
                    <p><@message "networks"/></p>
                  </div>
                  <div class="icon">
                    <i class="${networkIcon}"></i>
                  </div>
                  <a href="${networksLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
                </div>
              </div>
              <!-- ./col -->
            </#if>

            <#if !config.singleStudyEnabled>
              <div class="col-lg-3 col-sm-6">
                <!-- small box -->
                <div class="small-box bg-success">
                  <div class="inner">
                    <h3 id="study-hits">-</h3>
                    <p><@message "studies"/></p>
                  </div>
                  <div class="icon">
                    <i class="${studyIcon}"></i>
                  </div>
                  <a href="${studiesLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
                </div>
              </div>
              <!-- ./col -->
            </#if>

            <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
              <div class="col-lg-3 col-sm-6">
                <!-- small box -->
                <div class="small-box bg-warning">
                  <div class="inner">
                    <h3 id="dataset-hits">-</h3>
                    <p><@message "datasets"/></p>
                  </div>
                  <div class="icon">
                    <i class="${datasetIcon}"></i>
                  </div>
                  <a href="${datasetsLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
                </div>
              </div>
              <!-- ./col -->
              <div class="col-lg-3 col-sm-6">
                <!-- small box -->
                <div class="small-box bg-danger">
                  <div class="inner">
                    <h3 id="variable-hits">-</h3>
                    <p><@message "variables"/></p>
                  </div>
                  <div class="icon">
                    <i class="${variableIcon}"></i>
                  </div>
                  <a href="${contextPath}/search#lists?type=variables" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
                </div>
              </div>
              <!-- ./col -->
            </#if>
          </div>

          <div class="callout callout-info">
            <div class="row">
              <div class="col-sm-10">
                <p class="text-justify">
                  <@message "search-portal-callout"/>
                </p>
              </div><!-- /.col -->
              <div class="col-sm-2">
                <div class="text-right">
                  <button type="button"  onclick="location.href='search${defaultSearchState}';" class="btn btn-success btn-lg">
                    <i class="fas fa-search"></i> <@message "search"/>
                  </button>
                </div>
              </div><!-- /.col -->
            </div><!-- /.row -->
          </div>
        </#if>

        <#if config.dataAccessEnabled>
          <div class="callout callout-info">
            <div class="row">
              <div class="col-sm-8">
                <p class="text-justify">
                  <@message "data-access-process-portal-callout"/>
                </p>
              </div><!-- /.col -->
              <div class="col-sm-4">
                <div class="text-right">
                  <button type="button"  onclick="location.href='data-access-process';" class="btn btn-info btn-lg">
                    <i class="fas fa-info-circle"></i> <@message "data-access-process"/>
                  </button>
                </div>
              </div><!-- /.col -->
            </div><!-- /.row -->
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
  micajs.stats('studies', {}, function(stats) {
    $('#network-hits').text(new Intl.NumberFormat('${.lang}').format(stats.networkResultDto.totalHits));
    $('#study-hits').text(new Intl.NumberFormat('${.lang}').format(stats.studyResultDto.totalHits));
    $('#dataset-hits').text(new Intl.NumberFormat('${.lang}').format(stats.datasetResultDto.totalHits));
    $('#variable-hits').text(new Intl.NumberFormat('${.lang}').format(stats.variableResultDto.totalHits));
  });
</script>

</body>
</html>
