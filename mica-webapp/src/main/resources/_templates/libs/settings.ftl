<!-- Date and Datetime formats -->
<#assign datetimeFormat = "yyyy-MM-dd hh:mm"/>
<#assign dateFormat = "yyyy-MM-dd"/>

<!-- Icons -->
<#assign networkIcon = "io io-network"/>
<#assign studyIcon = "io io-study"/>
<#assign datasetIcon = "io io-dataset"/>
<#assign harmoDatasetIcon = "io io-harmo-dataset"/>
<#assign variableIcon = "io io-variable"/>
<#assign projectIcon = "io io-project"/>
<#assign taxonomyIcon = "io io-taxonomy"/>

<!-- Branding -->
<#assign brandImageSrc = "../assets/images/logo.png"/>
<#assign brandImageClass = "img-circle elevation-3"/>
<#assign brandTextEnabled = false/>
<#assign brandTextClass = "font-weight-light"/>

<!-- Theme -->
<#assign adminLTEPath = "/assets/libs/node_modules/admin-lte"/>

<!-- Home page settings -->
<#assign networksLink = "../networks"/>
<!--#assign networksLink = "../search#lists?type=networks"/-->
<#assign studiesLink = "../studies"/>
<!--#assign studiesLink = "../search#lists?type=studies"/-->
<#assign datasetsLink = "../datasets"/>
<!--#assign datasetsLink = "../search#lists?type=datasets"/-->

<!-- List pages settings -->
<#assign listDefaultDisplay = "cards"/> <!-- cards or table -->
<#assign networkListDefaultDisplay = listDefaultDisplay/>
<#assign studyListDefaultDisplay = listDefaultDisplay/>
<#assign datasetListDefaultDisplay = listDefaultDisplay/>

<!-- Search page settings -->
<#assign defaultSearchState = "#lists?type=studies"/>