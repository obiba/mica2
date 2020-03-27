<#macro rightmenus>
  <#include "../models/navbar-menus-right.ftl"/>
  <#if user??>
    <#if config?? && config.repositoryEnabled && (config.studyDatasetEnabled || config.harmonizationDatasetEnabled)>
      <!--li class="nav-item">
        <a href="/cart" class="nav-link">
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
        <li><a href="/profile" class="dropdown-item"><@message "profile"/></a></li>
        <li><a href="#" onclick="micajs.signout();" class="dropdown-item"><@message "sign-out"/></a></li>
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
    <#if !config.openAccess || config.dataAccessEnabled>
      <li class="nav-item">
        <a class="nav-link" href="/signin<#if rc.requestUri != "/" && !rc.requestUri?starts_with("/forgot-password") && !rc.requestUri?starts_with("/just-registered") && !rc.requestUri?starts_with("/error") && !rc.requestUri?starts_with("/signin")>?redirect=${rc.requestUri}</#if>"><@message "sign-in"/></a>
      </li>
    </#if>
    <#if config.signupEnabled>
      <li class="nav-item">
        <a class="btn btn-outline-primary" href="/signup"><@message "sign-up"/></a>
      </li>
    </#if>
  </#if>
</#macro>
