<!-- Home page macros -->
<!-- Default model macros -->
<#include "../libs/index.ftl">

<#macro homeModel>

  <#if config.repositoryEnabled>
    <div id="repository-metrics">

      <div class="row d-flex align-items-stretch">
      <#if config.networkEnabled && !config.singleNetworkEnabled>
        <div class="col-sm col-xs-6 d-flex align-items-stretch">
          <@networksBoxModel/>
        </div>
        <!-- ./col -->
      </#if>

      <#if !config.singleStudyEnabled>
        <div class="col-sm col-xs-6 d-flex align-items-stretch">
          <@individualStudiesBoxModel/>
        </div>
        <!-- ./col -->
      </#if>
      <#if config.harmonizationDatasetEnabled>
        <div class="col-sm col-xs-6 d-flex align-items-stretch">
          <@harmonizationStudiesBoxModel/>
        </div>
      </#if>
      </div>

      <div class="row d-flex align-items-stretch">
      <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
        <#if config.studyDatasetEnabled>
          <div class="col-sm col-xs-6 d-flex align-items-stretch">
            <@collectedDatasetsBoxModel/>
          </div>
        </#if>
        <#if config.harmonizationDatasetEnabled>
          <div class="col-sm col-xs-6 d-flex align-items-stretch">
            <@harmonizedDatasetsBoxModel/>
          </div>
        </#if>
        <!-- ./col -->
        <div class="col-sm col-xs-6 d-flex align-items-stretch">
          <@variablesBoxModel/>
        </div>
        <!-- ./col -->
      </div>
      </#if>
    </div>

    <#if !config.openAccess && !user??>
      <div id="sign-in-repository-callout" class="callout callout-info">
        <div class="row">
          <div class="col-sm-10">
            <p class="text-justify">
              <@message "sign-in-repository"/>
            </p>
          </div><!-- /.col -->
          <div class="col-sm-2">
            <div class="text-right">
              <button type="button"  onclick="location.href='${contextPath}/signin';" class="btn btn-primary btn-lg">
                <i class="fa-solid fa-sign-in-alt"></i> <@message "sign-in"/>
              </button>
            </div>
          </div><!-- /.col -->
        </div><!-- /.row -->
      </div>
    </#if>

      <#if !config.singleStudyEnabled || (config.networkEnabled && !config.singleNetworkEnabled) || config.studyDatasetEnabled || config.harmonizationDatasetEnabled >
        <div id="search-portal-callout" class="callout callout-info">
        <div class="row">
          <div class="col-sm-10">
            <p class="text-justify">
              <@message "search-portal-callout"/>
            </p>
          </div><!-- /.col -->
          <div class="col-sm-2">
            <div class="text-right">
              <button type="button"  onclick="location.href='<#if !config.openAccess && !user??>${contextPath}/signin?redirect=${contextPath}/search${defaultSearchState?url('UTF-8')}<#else>${contextPath}/search${defaultSearchState}</#if>';" class="btn btn-success btn-lg">
                <i class="fa-solid fa-search"></i> <@message "search"/>
              </button>
            </div>
          </div><!-- /.col -->
        </div><!-- /.row -->
      </div>
    </#if>
  </#if>

  <#if config.dataAccessEnabled>
    <div id="data-access-process-portal-callout" class="callout callout-info">
      <div class="row">
        <div class="col-sm-8">
          <p class="text-justify">
            <@message "data-access-process-portal-callout"/>
          </p>
        </div><!-- /.col -->
        <div class="col-sm-4">
          <div class="text-right">
            <button type="button"  onclick="location.href='${contextPath}/data-access-process';" class="btn btn-info btn-lg">
              <i class="fa-solid fa-info-circle"></i> <@message "data-access-process"/>
            </button>
          </div>
        </div><!-- /.col -->
      </div><!-- /.row -->
    </div>
  </#if>
</#macro>
