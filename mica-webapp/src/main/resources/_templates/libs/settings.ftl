<!-- Date and Datetime formats -->
<#assign datetimeFormat = "yyyy-MM-dd hh:mm"/>
<#assign dateFormat = "yyyy-MM-dd"/>

<!-- Favicon -->
<#assign faviconPath = "${contextPath}/favicon.ico"/>

<!-- Icons -->
<#assign networkIcon = "io io-network"/>
<#assign studyIcon = "io io-study"/>
<#assign datasetIcon = "io io-dataset"/>
<#assign harmoDatasetIcon = "io io-harmo-dataset"/>
<#assign variableIcon = "io io-variable"/>
<#assign projectIcon = "io io-project"/>
<#assign taxonomyIcon = "io io-taxonomy"/>

<!-- Assets location -->
<#assign assetsPath = "${contextPath}/assets"/>

<!-- Branding -->
<#assign brandImageSrc = "${assetsPath}/images/mica-logo.png"/>
<#assign brandImageClass = "img-circle elevation-3"/>
<#assign brandTextEnabled = true/>
<#assign brandTextClass = "font-weight-light"/>

<!-- Theme -->
<#assign adminLTEPath = "${assetsPath}/libs/node_modules/admin-lte"/>

<!-- Home page settings -->
<#assign networksLink = "${contextPath}/networks"/>
<!--#assign networksLink = "${contextPath}/search#lists?type=networks"/-->
<#assign studiesLink = "${contextPath}/studies"/>
<!--#assign studiesLink = "${contextPath}/search#lists?type=studies"/-->
<#assign datasetsLink = "${contextPath}/datasets"/>
<!--#assign datasetsLink = "${contextPath}/search#lists?type=datasets"/-->
<#assign portalLink = "${config.portalUrl!contextPath}" + "/"/>

<!-- List pages -->
<#assign listDefaultDisplay = "cards"/> <!-- cards or table -->
<#assign networkListDefaultDisplay = listDefaultDisplay/>
<#assign studyListDefaultDisplay = listDefaultDisplay/>
<#assign datasetListDefaultDisplay = listDefaultDisplay/>

<!-- Search page -->
<#if config?? && config.singleStudyEnabled>
  <#assign defaultSearchState = "#lists?type=variables"/>
<#else>
  <#assign defaultSearchState = "#lists?type=studies"/>
</#if>

<!-- Data Access pages -->
<#assign dataAccessInstructionsEnabled = true/>
<#assign dataAccessCalloutsEnabled = true/>
