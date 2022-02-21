<!-- Dataset page macros -->

<!-- Dataset model template -->
<#macro datasetModel dataset type>
</#macro>

<!-- Files -->
<#macro datasetFilesBrowser dataset>
  <div id="files-app-container" style="display: none;" class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title"><@message "files"/></h3>
    </div>
    <div class="card-body">

      <div id="files-app">
        <@filesBrowser/>
      </div>

    </div>
  </div>
</#macro>

<!-- Variables classifications -->
<#macro variablesClassifications dataset>
  <div id="loadingClassifications" class="spinner-border spinner-border-sm" role="status"></div>
  <div id="classificationsContainer" style="display: none;" class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title"><@message "variables-classifications"/></h3>
    </div>
    <div class="card-body">
      <div class="row">
        <div class="col" id="chartsContainer"></div>
      </div>
      <div id="noVariablesClassifications" style="display: none">
        <span class="text-muted"><@message "no-variables-classifications"/></span>
      </div>
    </div>
  </div>
</#macro>

<#macro harmonizationProtocolGeneralInfo dataset>
  <dl class="row mt-3">
      <#if dataset.model.version??>
        <dt class="col-sm-3">
            <@message "harmonization-protocol.version"/>
        </dt>
        <dd class="col-sm-9">
            ${dataset.model.version}
        </dd>
      </#if>

      <#if dataset.model.participants??>
        <dt class="col-sm-3">
            <@message "harmonization-protocol.participants"/>
        </dt>
        <dd class="col-sm-9">
            ${dataset.model.participants}
        </dd>
      </#if>

      <#if dataset.model.prospectiveRetrospective??>
        <dt class="col-sm-3">
            <@message "harmonization-protocol.prospective-retrospective.title"/>
        </dt>
        <dd class="col-sm-9">
            <@message "harmonization-protocol.prospective-retrospective.enum." + dataset.model.prospectiveRetrospective/>
        </dd>
      </#if>

      <#if dataset.model.qualitativeQuantitative??>
        <dt class="col-sm-3">
            <@message "harmonization-protocol.qualitative-quantitative.title"/>
        </dt>
        <dd class="col-sm-9">
            <@message "harmonization-protocol.qualitative-quantitative.enum." + dataset.model.qualitativeQuantitative/>
        </dd>
      </#if>
  </dl>
</#macro>
