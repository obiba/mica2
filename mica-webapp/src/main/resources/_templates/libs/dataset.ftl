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
      <div>
        <div id="chartsContainer"></div>
      </div>
      <div id="noVariablesClassifications" style="display: none">
        <span class="text-muted"><@message "no-variables-classifications"/></span>
      </div>
    </div>
  </div>
</#macro>
