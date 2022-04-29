<#macro leftmenus>
  <li id="homeMenu" class="nav-item">
    <a href="${contextPath}/" class="nav-link"><@message "home"/></a>
  </li>
    <#if config??>
        <#if config.repositoryEnabled>
          <li id="repoMenu" class="nav-item dropdown">
            <a href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"><@message "repository"/></a>
            <ul aria-labelledby="repoMenu" class="dropdown-menu border-0 shadow">
                <#if config.networkEnabled>
                  <li>
                      <#if config.singleNetworkEnabled>
                        <a href="${contextPath}/network/_" class="dropdown-item"><@message "the-network"/></a>
                      <#else>
                        <a href="${contextPath}/networks" class="dropdown-item"><@message "networks"/></a>
                      </#if>
                  </li>
                  <li><div class="dropdown-divider"></div></li>
                </#if>
              <li>
                  <#if config.singleStudyEnabled>
                    <a href="${contextPath}/study/_" class="dropdown-item"><@message "the-study"/></a>
                  <#else>
                    <a href="${contextPath}/individual-studies" class="dropdown-item"><@message "global.individual-studies"/></a>
                  </#if>
                  <#if config.studyDatasetEnabled>
              <li><a href="${contextPath}/collected-datasets" class="dropdown-item"><@message "collected-datasets"/></a></li>
                </#if>
              </li>
                <#if config.harmonizationDatasetEnabled>
                  <li><div class="dropdown-divider"></div></li>
                  <li><a href="${contextPath}/harmonization-studies" class="dropdown-item"><@message "global.harmonization-studies"/></a></li>
                  <li><a href="${contextPath}/harmonized-datasets" class="dropdown-item"><@message "harmonized-datasets"/></a></li>
                </#if>
            </ul>
          </li>
            <#if !config.singleStudyEnabled || (config.networkEnabled && !config.singleNetworkEnabled) || config.studyDatasetEnabled || config.harmonizationDatasetEnabled>
              <#if config.harmonizationDatasetEnabled>
                <a href="<#if !config.openAccess && !user??>${contextPath}/signin?redirect=${contextPath}/individual-search${defaultIndividualSearchState?url('UTF-8')}<#else>${contextPath}/individual-search${defaultIndividualSearchState}</#if>" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle btn btn-success text-white"><@message "search"/> <i class="fas fa-search"></i></a>
              <#else>

              <li id="searchMenu" class="nav-item dropdown">
                <a href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle btn btn-success text-white"><@message "search"/> <i class="fas fa-search"></i></a>
                <ul aria-labelledby="searchMenu" class="dropdown-menu border-0 shadow">
                  <li>
                    <a href="<#if !config.openAccess && !user??>${contextPath}/signin?redirect=${contextPath}/individual-search${defaultIndividualSearchState?url('UTF-8')}<#else>${contextPath}/individual-search${defaultIndividualSearchState}</#if>" class="dropdown-item">
                        <@message "individual-search"/>
                    </a>
                  </li>

                  <li>
                    <a href="<#if !config.openAccess && !user??>${contextPath}/signin?redirect=${contextPath}/harmonization-search${defaultHarmonizationSearchState?url('UTF-8')}<#else>${contextPath}/harmonization-search${defaultHarmonizationSearchState}</#if>" class="dropdown-item">
                        <@message "harmonization-search"/>
                    </a>
                  </li>

                </ul>
              </li>

              </#if>
            </#if>
        </#if>

        <#if config.dataAccessEnabled && config.projectEnabled>
          <li id="researchMenu" class="nav-item dropdown">
            <a href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"><@message "research"/></a>
            <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
              <li id="dataAccessProcessMenu" ><a href="${contextPath}/data-access-process" class="dropdown-item"><@message "data-access-process"/></a></li>
              <li id="projectMenu"><a href="${contextPath}/projects" class="dropdown-item"><@message "approved-projects"/></a></li>
            </ul>
          </li>
        <#elseif config.projectEnabled>
          <li id="projectMenu" class="nav-item">
            <a href="${contextPath}/projects" class="nav-link"><@message "approved-projects"/></a>
          </li>
        </#if>

        <#if config.dataAccessEnabled>
          <li id="dataAccessMenu" class="nav-item <#if config.repositoryEnabled && !config.projectEnabled>ml-3</#if>">
            <a href="${contextPath}/data-accesses" class="btn btn-warning"><@message "data-access"/> <i class="fas fa-arrow-circle-right"></i></a>
          </li>
        </#if>
    </#if>
    <#include "../models/navbar-menus-left.ftl"/>
</#macro>
