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
</#macro>

<#macro rightmenus>
    <#if user??>
      <#if config?? && config.repositoryEnabled && (config.studyDatasetEnabled || config.harmonizationDatasetEnabled)>
        <!--li class="nav-item">
          <a href="${pathPrefix!".."}/cart" class="nav-link">
            <i class="fas fa-shopping-basket"></i>
            <span class="badge badge-danger navbar-badge">3</span>
          </a>
        </li-->
      </#if>
      <#if config?? && config.locales?size != 1>
        <li class="nav-item dropdown">
          <a id="userMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"> ${.lang?upper_case}</a>
          <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
            <#list config.locales as locale>
              <li><a href="#" onclick="micajs.changeLanguage('${locale.language}')" class="dropdown-item">${locale.language?upper_case}</a></li>
            </#list>
          </ul>
        </li>
      </#if>

      <li class="nav-item dropdown">
        <a id="userMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"><i class="fas fa-user"></i> ${user.fullName}</a>
        <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
          <li><a href="${pathPrefix!".."}/profile" class="dropdown-item"><@message "profile"/></a></li>
          <li><a href="#" onclick="micajs.signout('${pathPrefix!".."}');" class="dropdown-item"><@message "sign-out"/></a></li>
        </ul>
      </li>
    <#elseif config??>
        <#if config.locales?size != 1>
          <li class="nav-item dropdown">
            <a id="userMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"> ${.lang?upper_case}</a>
            <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
                <#list config.locales as locale>
                  <li><a id="lang-${locale.language}" href="#" onclick="micajs.changeLanguage('${locale.language}')" class="dropdown-item">${locale.language?upper_case}</a></li>
                </#list>
            </ul>
          </li>
        </#if>
      <li class="nav-item">
        <a class="nav-link" href="${pathPrefix!".."}/signin<#if rc.requestUri != "/" && !rc.requestUri?starts_with("/reset-password") && !rc.requestUri?starts_with("/just-registered") && !rc.requestUri?starts_with("/error") && !rc.requestUri?starts_with("/signin")>?redirect=${rc.requestUri}</#if>"><@message "sign-in"/></a>
      </li>
      <#if config.signupEnabled>
        <li class="nav-item">
          <a class="btn btn-outline-primary" href="${pathPrefix!".."}/signup"><@message "sign-up"/></a>
        </li>
      </#if>
    </#if>
</#macro>
