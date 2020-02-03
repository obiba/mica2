<#macro leftmenus>
  <li class="nav-item">
    <a href="${pathPrefix!".."}/home" class="nav-link"><@message "home"/></a>
  </li>
  <li class="nav-item dropdown">
    <a id="repoMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"><@message "repository"/></a>
    <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
      <li><a href="${pathPrefix!".."}/networks" class="dropdown-item"><@message "networks"/></a></li>
      <li><a href="${pathPrefix!".."}/studies" class="dropdown-item"><@message "studies"/></a></li>
      <li><a href="${pathPrefix!".."}/datasets" class="dropdown-item"><@message "datasets"/></a></li>
    </ul>
  </li>
  <li class="nav-item">
    <a href="${pathPrefix!".."}/search" class="btn btn-success"><@message "search"/> <i class="fas fa-search"></i></a>
  </li>
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
</#macro>

<#macro rightmenus>
    <#if user??>
      <li class="nav-item">
        <a href="${pathPrefix!".."}/cart" class="nav-link">
          <i class="fas fa-shopping-basket"></i>
          <!--<span class="badge badge-danger navbar-badge">3</span>-->
        </a>
      </li>
      <#if config.locales?size != 1>
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
    <#else >
        <#if config.locales?size != 1>
          <li class="nav-item dropdown">
            <a id="userMenu" href="#" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" class="nav-link dropdown-toggle"> ${.lang?upper_case}</a>
            <ul aria-labelledby="dropdownSubMenu1" class="dropdown-menu border-0 shadow">
                <#list config.locales as locale>
                  <li><a id="signout" href="#" onclick="micajs.changeLanguage('${locale.language}')" class="dropdown-item">${locale.language?upper_case}</a></li>
                </#list>
            </ul>
          </li>
        </#if>
      <li class="nav-item">
        <a class="nav-link" href="${pathPrefix!".."}/signin"><@message "sign-in"/></a>
      </li>
      <li class="nav-item">
        <a class="btn btn-outline-primary" href="${pathPrefix!".."}/signup"><@message "sign-up"/></a>
      </li>
    </#if>
</#macro>
