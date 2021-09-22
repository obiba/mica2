<!-- Language -->
<#assign defaultLang = "en"/>

<!-- Date and Datetime formats -->
<#assign datetimeFormat = "yyyy-MM-dd HH:mm"/>
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

<!-- Cart -->
<#assign cartEnabled = (config?? && config.cartEnabled && (config.studyDatasetEnabled || config.harmonizationDatasetEnabled))/>
<!-- Cart feature is only visible to advanced users -->
<!--#assign cartEnabled = cartEnabled && (isAdministrator || isReviewer || isEditor || isDAO)/-->
<!-- Cart feature is only visible to any authenticated users -->
<!--#assign cartEnabled = cartEnabled && user??/-->
<#assign listsEnabled = user?? && cartEnabled && true/>
<#assign maxNumberOfSets = config.maxNumberOfSets/>
<!-- To download the list of variable IDs (and the Opal views, if enabled) -->
<#assign showCartDownload = (isAdministrator || isReviewer || isEditor || isDAO)/>
<!-- To reinstate the cart as views in Opal -->
<#assign showCartViewDownload = (isAdministrator || isReviewer || isEditor || isDAO)/>

<!-- Contact -->
<#assign contactEnabled = config.contactNotificationsEnabled/>

<!-- Sign in -->
<#assign showSignin = !config.openAccess || config.dataAccessEnabled || cartEnabled/>

<!-- Profile -->
<#assign showProfileRole = false/>
<#assign showProfileGroups = false/>

<!-- Repository list pages -->
<#assign listDisplays = ["lines", "table", "cards"]/> <!-- order matters -->
<#assign listDefaultDisplay = "lines"/> <!-- cards, lines or table -->
<#assign networkListDisplays = listDisplays/>
<#assign networkListDefaultDisplay = listDefaultDisplay/>
<#assign studyListDisplays = listDisplays/>
<#assign studyListDefaultDisplay = listDefaultDisplay/>
<#assign datasetListDisplays = listDisplays/>
<#assign datasetListDefaultDisplay = "cards"/>

<!-- Search -->
<#if config?? && config.singleStudyEnabled>
  <#assign defaultSearchState = "#lists?type=variables"/>
<#else>
  <#assign defaultSearchState = "#lists?type=studies"/>
</#if>
<!-- Download the search results -->
<#assign downloadQueryEnabled = true />
<!-- Show copy query button depending on the role (check built-in ones or by role name). -->
<#assign showCopyQuery = (isAdministrator || isReviewer || isEditor || isDAO)/>
<!--#assign showCopyQuery = (user?? && user.roles?? && user.roles?seq_contains("mica-user"))/-->
<#assign mapName = "world"/>
<!-- Filter and order the charts visible in the search page -->
<#assign searchCharts = ["geographical-distribution-chart", "study-design-chart", "number-participants-chart", "bio-samples-chart", "study-start-year-chart"]/>
<!-- List tabs by type -->
<#assign searchVariableListDisplay = (config.studyDatasetEnabled || config.harmonizationDatasetEnabled)/>
<#assign searchDatasetListDisplay = searchVariableListDisplay/>
<#assign searchStudyListDisplay = !config.singleStudyEnabled/>
<#assign searchNetworkListDisplay = (config.networkEnabled && !config.singleNetworkEnabled)/>
<!-- List tables by type (ID column ('name' or 'acronym') is always first) -->
<#assign searchVariableColumns = ["label+description", "valueType", "annotations", "type", "study", "population", "data-collection-event", "dataset"]/>
<#assign searchDatasetColumns = ["name", "type", "networks", "studies", "variables"]/>
<#assign searchStudyColumns = ["name", "type", "study-design", "data-sources-available", "participants", "networks", "individual", "harmonization"]/>
<#assign searchNetworkColumns = ["name", "studies", "datasets", "variables"]/>
<!-- Extracted fields on search by type -->
<#assign searchVariableFields = ["attributes.label.*", "attributes.description.*", "variableType", "valueType", "categories.*", "unit", "populationId", "dceId", "datasetId", "datasetAcronym", "attributes.Mlstr_area*"]/>
<#assign searchDatasetFields = ["acronym.*","name.*","variableType","studyTable.studyId","studyTable.project","studyTable.table","studyTable.populationId","studyTable.dataCollectionEventId","harmonizationTable.studyId","harmonizationTable.project","harmonizationTable.table","harmonizationTable.populationId"]/>
<#assign searchStudyFields = ["acronym.*","name.*","model.methods.design","populations.dataCollectionEvents.model.dataSources","model.numberOfParticipants.participant"]/>
<#assign searchNetworkFields = ["acronym.*","name.*","studyIds"]/>
<!-- Sort fields on search by type -->
<#assign searchVariableSortFields = ["studyId", "datasetId", "index", "name"]/>
<#assign searchDatasetSortFields = ["studyTable.studyId","studyTable.populationWeight","studyTable.dataCollectionEventWeight","acronym"]/>
<#assign searchStudySortFields = ["acronym"]/>
<#assign searchNetworkSortFields = ["acronym"]/>
<!-- Coverage tab -->
<#assign searchCoverageDisplay = (config.studyDatasetEnabled || config.harmonizationDatasetEnabled)/>
<!-- Graphics tab -->
<#assign searchGraphicsDisplay = (config.networkEnabled && !config.singleStudyEnabled && searchCharts?has_content)/>
<!-- List tab -->
<#assign searchListDisplay = (searchCoverageDisplay || searchGraphicsDisplay)/>
<!-- Search criteria display by type -->
<#assign searchCriteriaMenus = ["variable", "dataset", "study", "network"]/>

<!-- Variable -->
<#assign showHarmonizedVariableSummarySelector = true/>

<!-- Data Access pages -->
<#assign dataAccessInstructionsEnabled = true/>
<#assign dataAccessCalloutsEnabled = true/>
<#assign dataAccessReportTimelineEnabled = true/>
<#assign dataAccessArchiveEnabled = true/>

<!-- Charts -->
<#assign barChartBackgroundColor = "rgb(54, 162, 235)"/>
<#assign barChartBorderColor = "#3c8dbc"/>
<#assign colors = ["#f56954", "#00a65a", "#f39c12", "#00c0ef", "#3c8dbc", "#d2d6de", "#007bff", "#6610f2", "#20c997", "#6f42c1", "#e83e8c", "#dc3545", "#fd7e14", "#ffc107", "#28a745",  "#17a2b8", "#f56954", "#00a65a", "#f39c12", "#00c0ef", "#3c8dbc", "#d2d6de", "#007bff", "#6610f2", "#20c997", "#6f42c1", "#e83e8c", "#dc3545", "#fd7e14", "#ffc107", "#28a745",  "#17a2b8"]>

<!-- Files -->
<#assign showFiles = true/>
<#assign showNetworkFiles = showFiles/>
<#assign showStudyFiles = showFiles/>
<#assign showStudyPopulationFiles = showStudyFiles/>
<#assign showStudyDCEFiles = showStudyFiles/>
<#assign showDatasetFiles = showFiles/>

<!-- Variables classifications charts -->
<!-- if taxonomies list is null or empty, no variables classifications is shown -->
<#assign variablesClassificationsTaxonomies = ["Mlstr_area"]/>
<#assign networkVariablesClassificationsTaxonomies = variablesClassificationsTaxonomies/>
<#assign studyVariablesClassificationsTaxonomies = variablesClassificationsTaxonomies/>
<#assign datasetVariablesClassificationsTaxonomies = variablesClassificationsTaxonomies/>
