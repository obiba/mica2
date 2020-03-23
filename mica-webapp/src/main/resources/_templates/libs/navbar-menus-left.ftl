<#macro leftmenus>
  <li class="nav-item">
    <a href="${pathPrefix!".."}" class="nav-link"><@message "home"/></a>
  </li>
  <#if config?? && config.repositoryEnabled>
    <li class="nav-item dropdown">
      <a id="repoMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"><@message "repository"/></a>
      <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
        <#if config.networkEnabled>
          <li>
            <#if config.singleNetworkEnabled>
              <a href="${pathPrefix!".."}/network/_" class="dropdown-item"><@message "the-network"/></a>
            <#else>
              <a href="${pathPrefix!".."}/networks" class="dropdown-item"><@message "networks"/></a>
            </#if>
          </li>
        </#if>
        <li>
          <#if config.singleStudyEnabled>
            <a href="${pathPrefix!".."}/study/_" class="dropdown-item"><@message "the-study"/></a>
          <#else>
            <a href="${pathPrefix!".."}/studies" class="dropdown-item"><@message "studies"/></a>
          </#if>
        </li>
        <#if config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
          <li><a href="${pathPrefix!".."}/datasets" class="dropdown-item"><@message "datasets"/></a></li>
        </#if>
      </ul>
    </li>
    <li class="nav-item">
      <a href="${pathPrefix!".."}/search${defaultSearchState}" class="btn btn-success"><@message "search"/> <i class="fas fa-search"></i></a>
    </li>
  </#if>
  <#if config?? &&config.dataAccessEnabled>
    <li class="nav-item dropdown">
      <a id="studiesMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"><@message "research"/></a>
      <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
        <li><a href="${pathPrefix!".."}/data-access-process" class="dropdown-item"><@message "data-access-process"/></a></li>
        <li><a href="${pathPrefix!".."}/projects" class="dropdown-item"><@message "approved-projects"/></a></li>
      </ul>
    </li>
    <li class="nav-item">
      <a href="${pathPrefix!".."}/data-accesses" class="btn btn-warning"><@message "data-access"/> <i class="fas fa-arrow-circle-right"></i></a>
    </li>
  </#if>
  <#include "../models/navbar-menus-left.ftl"/>
</#macro>
