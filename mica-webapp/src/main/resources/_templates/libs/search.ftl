<!-- Search Page Macros -->

<#macro searchResultTabs>
  <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
    <li class="nav-item">
      <a class="nav-link active" id="variables-tab" data-toggle="pill" href="#variables" role="tab" @click="onSelectResult('variables', 'variable')"
         aria-controls="variables" aria-selected="true"><@message "variables"/> <span id="variable-count" class="badge badge-light">{{counts.variables}}</span></a>
    </li>
    <li class="nav-item">
      <a class="nav-link" id="datasets-tab" data-toggle="pill" href="#datasets" role="tab" @click="onSelectResult('datasets', 'dataset')"
         aria-controls="datasets" aria-selected="false"><@message "datasets"/> <span id="dataset-count" class="badge badge-light">{{counts.datasets}}</span></a>
    </li>
  </#if>
  <#if !config.singleStudyEnabled>
    <li class="nav-item">
      <a class="nav-link" id="studies-tab" data-toggle="pill" href="#studies" role="tab" @click="onSelectResult('studies', 'study')"
         aria-controls="studies" aria-selected="false"><@message "studies"/> <span id="study-count" class="badge badge-light">{{counts.studies}}</span></a>
    </li>
  </#if>
  <#if config.networkEnabled && !config.singleNetworkEnabled>
    <li class="nav-item">
      <a class="nav-link" id="networks-tab" data-toggle="pill" href="#networks" role="tab" @click="onSelectResult('networks', 'network')"
         aria-controls="networks" aria-selected="false"><@message "networks"/> <span id="network-count" class="badge badge-light">{{counts.networks}}</span></a>
    </li>
  </#if>
</#macro>
