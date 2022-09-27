<!-- Macros and functions -->
<#include "data-access.ftl"/>

<!-- Main Sidebar Container -->
<aside class="main-sidebar sidebar-dark-primary">
  <!-- Brand Logo -->
  <a href="${portalLink}" class="brand-link bg-white">
    <img src="${brandImageSrc}"
         alt="Logo"
         class="brand-image ${brandImageClass}"
         style="opacity: .8">
    <span class="brand-text ${brandTextClass}">
      <#if brandTextEnabled>
        ${config.name!""}
      <#else>&nbsp;
      </#if>
    </span>
  </a>

  <!-- Sidebar -->
  <div class="sidebar">
    <!-- Sidebar Menu -->

    <div class="user-panel mt-3 pb-3 mb-3 d-flex">
      <div>
        <span title="<@message dar.status.toString()/>"><i
              class="fas fa-circle fa-2x pl-2 text-${statusColor(dar.status.toString())}"></i></span>
      </div>
      <div class="info">
        <a href="${contextPath}/data-access/${dar.id}" class="d-inline">${mainApplicant.fullName} </a>
      </div>
    </div>

    <nav class="mt-2">
      <ul data-widget="treeview" role="menu" data-accordion="false" class="nav nav-pills nav-sidebar flex-column">
        <li class="nav-item">
          <a id="dashboard-menu" href="${contextPath}/data-access/${dar.id}" class="nav-link">
            <i class="fas fa-tachometer-alt nav-icon"></i>
            <p><@message "dashboard"/></p>
          </a>
        </li>
        <#if accessConfig.feasibilityEnabled>
          <li class="nav-item has-treeview <#if feasibility??>menu-open</#if>">
            <a id="feasibility-form-menu" href="#" class="nav-link">
              <i class="nav-icon far fa-question-circle"></i>
              <p>
                <@message "feasibilities"/>
                <span class="badge badge-info right">${feasibilities?size}</span>
                <#if feasibilities?size != 0 || user.username == dar.applicant || isAdministrator>
                  <i class="fas fa-angle-left right mr-1"></i>
                </#if>
              </p>
            </a>
            <ul class="nav nav-treeview">
              <#list feasibilities as feasibility>
                <li class="nav-item">
                  <a id="feasibility-form-menu-${feasibility.id}" href="${contextPath}/data-access-feasibility-form/${feasibility.id}" class="nav-link">
                    <i class="fas fa-circle nav-icon text-${statusColor(feasibility.status.toString())}"
                       title="<@message feasibility.status.toString()/>"></i>
                    <p>${feasibility.id}</p>
                  </a>
                </li>
              </#list>
              <#if !dar.archived && (user.username == dar.applicant || isAdministrator)>
                <li class="nav-item">
                  <a class="nav-link" data-toggle="modal" data-target="#modal-feasibility-add">
                    <i class="fas fa-plus nav-icon"></i>
                    <p><@message "new-feasibility"/></p>
                  </a>
                </li>
              </#if>
            </ul>
          </li>
        </#if>
        <li class="nav-item">
          <a id="form-menu" href="${contextPath}/data-access-form/${dar.id}" class="nav-link">
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
        <#if accessConfig.amendmentsEnabled && dar.status.toString() == "APPROVED">
          <li class="nav-item has-treeview <#if amendment??>menu-open</#if>">
            <a id="amendment-form-menu" href="#" class="nav-link">
              <i class="nav-icon fas fa-file-import"></i>
              <p>
                <@message "amendments"/>
                <span class="badge badge-info right">${amendments?size}</span>
                <#if amendments?size != 0 || user.username == dar.applicant || isAdministrator>
                  <i class="fas fa-angle-left right mr-1"></i>
                </#if>
              </p>
            </a>
            <ul class="nav nav-treeview">
              <#list amendments as amendment>
                <li class="nav-item">
                  <a id="amendment-form-menu-${amendment.id}" href="${contextPath}/data-access-amendment-form/${amendment.id}" class="nav-link">
                    <i class="fas fa-circle nav-icon text-${statusColor(amendment.status.toString())}"
                       title="<@message amendment.status.toString()/>"></i>
                    <p>${amendment.id}</p>
                  </a>
                </li>
              </#list>
              <#if !dar.archived && (user.username == dar.applicant || isAdministrator)>
                <li class="nav-item">
                  <a class="nav-link" data-toggle="modal" data-target="#modal-amendment-add">
                    <i class="fas fa-plus nav-icon"></i>
                    <p><@message "new-amendment"/></p>
                  </a>
                </li>
              </#if>
            </ul>
          </li>
        </#if>
        <#if accessConfig.agreementEnabled>
          <#if dar.status == "APPROVED">
            <li class="nav-item has-treeview <#if agreement??>menu-open</#if>">
              <a id="agreement-form-menu" href="#" class="nav-link">
                <i class="nav-icon fa fa-gavel"></i>
                <p>
                  <@message "agreements"/>
                  <span class="badge badge-info right">${agreements?size}</span>
                  <#if agreements?size != 0>
                    <i class="fas fa-angle-left right mr-1"></i>
                  </#if>
                </p>
              </a>
              <ul class="nav nav-treeview">
                <#list agreements as agreement>
                  <li class="nav-item">
                    <a id="agreement-form-menu-${agreement.id}" href="${contextPath}/data-access-agreement-form/${agreement.id}" class="nav-link">
                      <i class="fas fa-circle nav-icon text-${statusColor(agreement.status.toString())}"
                         title="<@message agreement.status.toString()/>"></i>
                      <p>${agreement.id}</p>
                      <#if agreement.applicant == user.username>
                        <span class="right"><i class="fa fa-star align-top"></i></span>
                      </#if>
                    </a>
                  </li>
                </#list>
              </ul>
            </li>
          <#else>
            <li class="nav-item">
              <div id="agreement-form-menu" href="#" class="nav-link">
                <i class="nav-icon fa fa-gavel"></i>
                <p>
                  <@message "agreements"/>
                </p>
              </div>
            </li>
          </#if>
        </#if>
        <li class="nav-item">
          <a id="documents-menu" href="${contextPath}/data-access-documents/${dar.id}" class="nav-link">
            <i class="fas fa-copy nav-icon"></i>
            <p><@message "documents"/></p>
            <#if dar.attachments?size != 0>
              <span class="badge badge-info right">${dar.attachments?size}</span>
            </#if>
          </a>
        </li>
        <li class="nav-item">
          <a id="comments-menu" href="${contextPath}/data-access-comments/${dar.id}" class="nav-link">
            <i class="fas fa-comments nav-icon"></i>
            <p><@message "comments"/></p>
            <#if commentsCount != 0>
              <span class="badge badge-info right">${commentsCount}</span>
            </#if>
          </a>
        </li>
          <#if isAdministrator || isDAO || permissions?seq_contains("VIEW_PRIVATE_COMMENTS")>
            <li class="nav-item">
              <a id="private-comments-menu" href="${contextPath}/data-access-private-comments/${dar.id}" class="nav-link">
                <i class="fas fa-lock nav-icon"></i>
                <p><@message "private-comments"/></p>
                <#if privateCommentsCount != 0>
                  <span class="badge badge-info right">${privateCommentsCount}</span>
                </#if>
              </a>
            </li>
          </#if>
        <li class="nav-item">
          <a id="history-menu" href="${contextPath}/data-access-history/${dar.id}" class="nav-link">
            <i class="fas fa-calendar-alt nav-icon"></i>
            <p><@message "history"/></p>
          </a>
        </li>
      </ul>
    </nav>

    <!-- /.sidebar-menu -->
  </div>
  <!-- /.sidebar -->

</aside>

<#if accessConfig.feasibilityEnabled>
  <!-- Confirm feasibility addition modal -->
  <div class="modal fade" id="modal-feasibility-add">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><@message "confirm-creation"/></h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <p><@message "confirm-data-access-feasibility-creation"/></p>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
          <button type="button" class="btn btn-primary" data-dismiss="modal"
                  onclick="DataAccessService.create('${dar.id}', 'feasibility')"><@message "confirm"/>
          </button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#if>

<#if accessConfig.amendmentsEnabled>
  <!-- Confirm amendment addition modal -->
  <div class="modal fade" id="modal-amendment-add">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><@message "confirm-creation"/></h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <p><@message "confirm-data-access-amendment-creation"/></p>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-default" data-dismiss="modal"><@message "cancel"/></button>
          <button type="button" class="btn btn-primary" data-dismiss="modal"
                  onclick="DataAccessService.create('${dar.id}', 'amendment')"><@message "confirm"/>
          </button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#if>
