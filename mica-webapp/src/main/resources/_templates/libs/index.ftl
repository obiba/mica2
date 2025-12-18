<!-- Home page macros -->

<!-- Network box model template -->
<#macro networksBoxModel>
  <!-- small box -->
  <div class="small-box bg-info w-100">
    <div class="inner">
      <h3 id="network-hits">-</h3>
      <p><@message "networks"/></p>
    </div>
    <div class="icon">
      <i class="${networkIcon}"></i>
    </div>
    <a href="${networksLink}" class="small-box-footer"><@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i></a>
  </div>
</#macro>

<!-- Individual studies box model template -->
<#macro individualStudiesBoxModel>
  <!-- small box -->
  <div class="small-box bg-success w-100">
    <div class="inner">
      <h3 id="study-hits">-</h3>
      <p><@message "global.individual-studies"/></p>
    </div>
    <div class="icon">
      <i class="${studyIcon}"></i>
    </div>
    <a href="${studiesLink}" class="small-box-footer"><@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i></a>
  </div>
</#macro>

<!-- Harmonization studies (= harmonization initiatives) box model template -->
<#macro harmonizationStudiesBoxModel>
  <!-- small box -->
  <div class="small-box bg-success w-100">
    <div class="inner">
      <h3 id="initiative-hits">-</h3>
      <p><@message "harmonization-studies"/></p>
    </div>
    <div class="icon">
      <i class="${initiativeIcon}"></i>
    </div>
    <a href="${initiativesLink}" class="small-box-footer"><@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i></a>
  </div>
</#macro>

<!-- Collected datasets box model template -->
<#macro collectedDatasetsBoxModel>
  <!-- small box -->
  <div class="small-box bg-warning w-100">
    <div class="inner">
      <h3 id="dataset-hits">-</h3>
      <p><@message "collected-datasets"/></p>
    </div>
    <div class="icon">
      <i class="${datasetIcon}"></i>
    </div>
    <a href="${datasetsLink}" class="small-box-footer"><@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i></a>
  </div>
</#macro>

<!-- Harmonized datasets (= harmonization protocols) box model template -->
<#macro harmonizedDatasetsBoxModel>
  <!-- small box -->
  <div class="small-box bg-warning w-100">
    <div class="inner">
      <h3 id="protocols-hits">-</h3>
      <p><@message "harmonized-datasets"/></p>
    </div>
    <div class="icon">
      <i class="${harmoDatasetIcon}"></i>
    </div>
    <a href="${protocolsLink}" class="small-box-footer"><@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i></a>
  </div>
</#macro>

<!-- Variables box model template -->
<#macro variablesBoxModel>
  <!-- small box -->
  <div class="small-box bg-danger w-100">
    <div class="inner">
      <h3 id="variable-hits">-</h3>
      <p><@message "variables"/></p>
    </div>
    <div class="icon">
      <i class="${variableIcon}"></i>
    </div>
    <a href="${contextPath}/search#lists?type=variables" class="small-box-footer"><@message "more-info"/> <i class="fa-solid fa-arrow-circle-right"></i></a>
  </div>
</#macro>
