<!-- Dataset page macros -->

<!-- Dataset model template -->
<#macro datasetModel dataset type>
</#macro>

<!-- Variables classifications -->
<#macro variablesClassifications dataset>
  <div class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title"><@message "variables-classifications"/></h3>
    </div>
    <div class="card-body">
      <img id="loadingClassifications" src="${assetsPath}/images/loading.gif">
      <div id="classificationsContainer" style="display: none;">
        <div id="chartsContainer"></div>
      </div>
    </div>
  </div>
</#macro>
