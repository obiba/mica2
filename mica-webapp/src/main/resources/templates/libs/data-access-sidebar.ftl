<!-- Function to get the color corresponding to a status -->
<#function statusColor status>
    <#if status == "OPENED">
        <#local txtColor = "primary"/>
    <#elseif status == "APPROVED">
        <#local txtColor = "success"/>
    <#elseif status == "REJECTED">
        <#local txtColor = "danger"/>
    <#elseif status == "SUBMITTED">
        <#local txtColor = "info"/>
    <#elseif status == "REVIEWED">
        <#local txtColor = "info"/>
    <#elseif status == "CONDITIONALLY_APPROVED">
        <#local txtColor = "warning"/>
    <#else>
        <#local txtColor = "info"/>
    </#if>
    <#return txtColor/>
</#function>

<!-- Current user privilegies -->
<#assign isAdministrator = user.roles?seq_contains("mica-administrator")/>
<#assign isDAO = user.roles?seq_contains("mica-data-access-officer")/>

  <!-- Main Sidebar Container -->
<aside class="main-sidebar sidebar-dark-primary">
  <!-- Brand Logo -->
  <a href="../bower_components/admin-lte/index3.html" class="brand-link bg-white">
    <img src="../bower_components/admin-lte/dist/img/AdminLTELogo.png"
         alt="Logo"
         class="brand-image img-circle elevation-3"
         style="opacity: .8">
    <span class="brand-text font-weight-light">${config.name!""}</span>
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
          <#if accessConfig.amendmentsEnabled && dar.status.toString() == "APPROVED">
            <li class="nav-item has-treeview <#if amendment??>menu-open</#if>">
              <a id="amendment-form-menu" href="#" class="nav-link">
                <i class="nav-icon fas fa-file-import"></i>
                <p>
                    <@message "amendments"/>
                  <i class="right fas fa-angle-left"></i>
                  <span class="badge badge-info right">${amendments?size}</span>
                </p>
              </a>
              <ul class="nav nav-treeview">
                  <#list amendments as amendment>
                    <li class="nav-item">
                      <a id="amendment-form-menu-${amendment.id}" href="../data-access-amendment-form/${amendment.id}" class="nav-link">
                        <i class="fas fa-circle nav-icon text-${statusColor(amendment.status.toString())}"
                           title="<@message amendment.status.toString()/>"></i>
                        <p>${amendment.id}</p>
                      </a>
                    </li>
                  </#list>
                  <#if user.username == dar.applicant || isAdministrator>
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
        <li class="nav-item">
          <a id="comments-menu" href="../data-access-comments/${dar.id}" class="nav-link">
            <i class="fas fa-comments nav-icon"></i>
            <p><@message "comments"/></p>
          </a>
        </li>
          <#if isAdministrator || isDAO>
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

    <!-- /.sidebar-menu -->
  </div>
  <!-- /.sidebar -->

</aside>

<!-- Confirm amendment addition modal -->
<div class="modal fade" id="modal-amendment-add">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h4 class="modal-title">Confirm Amendment Creation</h4>
        <button type="button" class="close" data-dismiss="modal" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
      <div class="modal-body">
        <p>Please confirm that you want to create an amendment to this data access request.</p>
      </div>
      <div class="modal-footer justify-content-between">
        <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
        <button type="button" class="btn btn-primary" data-dismiss="modal"
                onclick="micajs.dataAccess.createAmendment('${dar.id}')">Confirm
        </button>
      </div>
    </div>
    <!-- /.modal-content -->
  </div>
  <!-- /.modal-dialog -->
</div>
<!-- /.modal -->

