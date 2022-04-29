<!-- Home page macros -->

<#macro homeModel>

  <#if config.repositoryEnabled>
    <div id="repository-metrics">

      <div class="row d-flex align-items-stretch">
      <#if config.networkEnabled && !config.singleNetworkEnabled>
        <div class="col-sm col-xs-6 d-flex align-items-stretch">
          <!-- small box -->
          <div class="small-box bg-info w-100">
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
        <div class="col-sm col-xs-6 d-flex align-items-stretch">
          <!-- small box -->
          <div class="small-box bg-success w-100">
            <div class="inner">
              <h3 id="study-hits">-</h3>
              <p><@message "global.individual-studies"/></p>
            </div>
            <div class="icon">
              <i class="${studyIcon}"></i>
            </div>
            <a href="${studiesLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
          </div>
        </div>
        <!-- ./col -->
      </#if>
      <#if config.harmonizationDatasetEnabled>
        <div class="col-sm col-xs-6 d-flex align-items-stretch">
          <!-- small box -->
          <div class="small-box bg-success w-100">
            <div class="inner">
              <h3 id="initiative-hits">-</h3>
              <p><@message "harmonization-studies"/></p>
            </div>
            <div class="icon">
              <i class="${initiativeIcon}"></i>
            </div>
            <a href="${initiativesLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
          </div>
        </div>
      </#if>
      </div>

      <div class="row d-flex align-items-stretch">
      <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
        <#if config.studyDatasetEnabled>
          <div class="col-sm col-xs-6 d-flex align-items-stretch">
            <!-- small box -->
            <div class="small-box bg-warning w-100">
              <div class="inner">
                <h3 id="dataset-hits">-</h3>
                <p><@message "collected-datasets"/></p>
              </div>
              <div class="icon">
                <i class="${datasetIcon}"></i>
              </div>
              <a href="${datasetsLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
            </div>
          </div>
        </#if>
        <#if config.harmonizationDatasetEnabled>
          <div class="col-sm col-xs-6 d-flex align-items-stretch">
            <!-- small box -->
            <div class="small-box bg-warning w-100">
              <div class="inner">
                <h3 id="protocols-hits">-</h3>
                <p><@message "harmonized-datasets"/></p>
              </div>
              <div class="icon">
                <i class="${harmoDatasetIcon}"></i>
              </div>
              <a href="${protocolsLink}" class="small-box-footer"><@message "more-info"/> <i class="fas fa-arrow-circle-right"></i></a>
            </div>
          </div>
        </#if>
        <!-- ./col -->
        <div class="col-sm col-xs-6 d-flex align-items-stretch">
          <!-- small box -->
          <div class="small-box bg-danger w-100">
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
                <i class="fas fa-sign-in-alt"></i> <@message "sign-in"/>
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
                <i class="fas fa-search"></i> <@message "search"/>
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
              <i class="fas fa-info-circle"></i> <@message "data-access-process"/>
            </button>
          </div>
        </div><!-- /.col -->
      </div><!-- /.row -->
    </div>
  </#if>
</#macro>
