<div class="user-panel mt-3 pb-3 mb-3 d-flex">
  <div>
    <#if dar.status.toString() == "OPENED">
      <#assign txtColor = "text-primary"/>
    <#elseif dar.status.toString() == "APPROVED">
      <#assign txtColor = "text-success"/>
    <#elseif dar.status.toString() == "REJECTED">
      <#assign txtColor = "text-danger"/>
    <#elseif dar.status.toString() == "SUBMITTED">
      <#assign txtColor = "text-info"/>
    <#elseif dar.status.toString() == "REVIEWED">
      <#assign txtColor = "text-info"/>
    <#elseif dar.status.toString() == "CONDITIONALLY_APPROVED">
      <#assign txtColor = "text-warning"/>
    <#else>
      <#assign txtColor = "text-info"/>
    </#if>
    <span title="<@message dar.status.toString()/>"><i class="fas fa-circle fa-2x pl-2 ${txtColor}"></i></span>
  </div>
  <div class="info">
    <a href="#" class="d-inline">${applicant.fullName} </a>
  </div>
</div>

<nav class="mt-2">
  <ul data-widget="treeview" role="menu" data-accordion="false" class="nav nav-pills nav-sidebar flex-column">
    <li class="nav-item">
      <a id="dashboard-menu" href="../data-access/${dar.id}" class="nav-link">
        <i class="fas fa-tachometer-alt nav-icon"></i>
        <p><@message "dashboard"/></p>
      </a>
    </li>
    <li class="nav-item">
      <a id="feasibility-menu" href="../data-access-feasibility/${dar.id}" class="nav-link">
        <i class="far fa-question-circle nav-icon"></i>
        <p><@message "feasibility-inquiry-form"/></p>
      </a>
    </li>
    <li class="nav-item">
      <a id="form-menu" href="../data-access-form/${dar.id}" class="nav-link">
        <i class="fas fa-book nav-icon"></i>
        <p>
          <@message "application-form"/>
          <#if dar.status.toString() == "OPENED" || dar.status.toString() == "CONDITIONALLY_APPROVED">
            <span class="right"><i class="fa fa-pen align-top"></i></span>
          <#elseif dar.status.toString() != "APPROVED" && dar.status.toString() != "REJECTED">
            <span class="right"><i class="fa fa-clock align-top"></i></span>
          </#if>
        </p>
      </a>

    </li>
    <li class="nav-item">
      <a id="documents-menu" href="../data-access-documents/${dar.id}" class="nav-link">
        <i class="fas fa-copy nav-icon"></i>
        <p><@message "documents"/></p>
      </a>
    </li>
    <li class="nav-item">
      <a id="amendments-menu" href="../data-access-amendments/${dar.id}" href="#" class="nav-link">
        <i class="far fa-plus-square nav-icon"></i>
        <p><@message "amendment-forms"/></p>
      </a>
    </li>
    <li class="nav-item">
      <a id="comments-menu" href="../data-access-comments/${dar.id}" class="nav-link">
        <i class="fas fa-comments nav-icon"></i>
        <p><@message "comments"/></p>
      </a>
    </li>
    <#assign admin = (user.roles?seq_contains("mica-administrator") || user.roles?seq_contains("mica-data-access-officer"))/>
    <#if admin>
      <li class="nav-item">
        <a id="private-comments-menu" href="../data-access-private-comments/${dar.id}" class="nav-link">
          <i class="fas fa-lock nav-icon"></i>
          <p><@message "private-comments"/></p>
        </a>
      </li>
    </#if>
    <li class="nav-item">
      <a id="history-menu" href="../data-access-history/${dar.id}" class="nav-link">
        <i class="fas fa-calendar-alt nav-icon"></i>
        <p><@message "history"/></p>
      </a>
    </li>
  </ul>
</nav>
