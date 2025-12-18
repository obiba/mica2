<#macro rightmenus>
  <#include "../models/navbar-menus-right.ftl"/>
  <#if cartEnabled>
    <li class="nav-item">
      <a href="${contextPath}/cart<#if defaultCartType??>?type=${defaultCartType}</#if>" class="nav-link" title="<@message "sets.cart.title"/>">
        <i class="fa-solid fa-shopping-cart"></i>
        <span id="cart-count" class="badge badge-danger navbar-badge"></span>
      </a>
    </li>
    <#if listsEnabled && user??>
      <li class="nav-item">
        <a href="${contextPath}/lists" class="nav-link" title="<@message "sets.set.title"/>">
          <i class="fa-regular fa-list-alt"></i>
          <span id="list-count" class="badge badge-danger navbar-badge" <#if !(sets?? && sets.variablesLists?has_content)>style="display: none"</#if>>
            <#if sets?? && sets.variablesLists?has_content>${sets.variablesLists?size}</#if>
          </span>
        </a>
      </li>
    </#if>
  </#if>
  <#if contactEnabled>
    <li class="nav-item">
      <a href="${contextPath}/contact" class="nav-link" title="<@message "contact-menu"/>">
        <i class="fa-solid fa-envelope"></i>
      </a>
    </li>
  </#if>
  <#if user??>
    <#if isAdministrator || isReviewer || isEditor || hasPermissionOnAnyDraftDocument>
      <li class="nav-item">
        <a href="${contextPath}/administration" class="nav-link">
          <@message "administration"/>
        </a>
      </li>
    </#if>
    <#if config?? && config.locales?size != 1>
      <li class="nav-item dropdown">
        <a id="userMenu" href="#" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"> ${.lang?upper_case}</a>
        <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
          <#list config.locales as locale>
            <li><a href="#" onclick="UserService.changeLanguage('${locale.language}')" class="dropdown-item">${locale.language?upper_case}</a></li>
          </#list>
        </ul>
      </li>
    </#if>
    <li class="nav-item dropdown">
      <a id="userMenu" href="#" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"><i class="fa-solid fa-user"></i> ${user.fullName}</a>
      <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
        <li><a href="${contextPath}/profile" class="dropdown-item"><@message "profile"/></a></li>
        <li><a href="${contextPath}/signout" class="dropdown-item"><@message "sign-out"/></a></li>
      </ul>
    </li>
  <#elseif config??>
    <#if config.locales?size != 1>
      <li class="nav-item dropdown">
        <a id="userMenu" href="#" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"> ${.lang?upper_case}</a>
        <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
            <#list config.locales as locale>
              <li><a id="lang-${locale.language}" href="#" onclick="UserService.changeLanguage('${locale.language}')" class="dropdown-item">${locale.language?upper_case}</a></li>
            </#list>
        </ul>
      </li>
    </#if>
    <#if showSignin>
      <li class="nav-item">
        <a class="nav-link" href="${contextPath}/signin"><@message "sign-in"/></a>
      </li>
      <#if config.signupEnabled>
        <li class="nav-item">
          <a class="btn btn-outline-primary" href="${contextPath}/signup"><@message "sign-up"/></a>
        </li>
      </#if>
    </#if>
  </#if>
</#macro>
