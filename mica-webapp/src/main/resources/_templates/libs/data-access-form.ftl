<!-- Data access (amendment) form page macros -->

<!-- Help section, can be adapted to data access request status -->
<#macro dataAccessFormHelp dar>
  <#if dar.status == "OPENED">
    <@message "data-access-form-opened-help"/>
  <#elseif dar.status == "CONDITIONALLY_APPROVED">
    <@message "data-access-form-conditionally-approved-help"/>
  <#elseif dar.status == "SUBMITTED">
    <@message "data-access-form-submitted-help"/>
  <#elseif dar.status == "REVIEWED">
    <@message "data-access-form-reviewed-help"/>
  <#elseif dar.status == "APPROVED">
    <@message "data-access-form-approved-help"/>
  <#elseif dar.status == "REJECTED">
    <@message "data-access-form-rejected-help"/>
  </#if>
</#macro>

<!-- Help section, can be adapted to data access amendment status -->
<#macro dataAccessAmendmentFormHelp amendment>
    <#if amendment.status == "OPENED">
        <@message "data-access-amendment-form-opened-help"/>
    <#elseif amendment.status == "CONDITIONALLY_APPROVED">
        <@message "data-access-amendment-form-conditionally-approved-help"/>
    <#elseif amendment.status == "SUBMITTED">
        <@message "data-access-amendment-form-submitted-help"/>
    <#elseif amendment.status == "REVIEWED">
        <@message "data-access-amendment-form-reviewed-help"/>
    <#elseif amendment.status == "APPROVED">
        <@message "data-access-amendment-form-approved-help"/>
    <#elseif amendment.status == "REJECTED">
        <@message "data-access-amendment-form-rejected-help"/>
    </#if>
</#macro>

<!-- Help section, can be adapted to data access feasibility status -->
<#macro dataAccessFeasibilityFormHelp feasibility>
  <#if feasibility.status == "OPENED">
    <@message "data-access-feasibility-form-opened-help"/>
  <#elseif feasibility.status == "SUBMITTED">
    <@message "data-access-feasibility-form-submitted-help"/>
  <#elseif feasibility.status == "APPROVED">
    <@message "data-access-feasibility-form-approved-help"/>
  <#elseif feasibility.status == "REJECTED">
    <@message "data-access-feasibility-form-rejected-help"/>
  </#if>
</#macro>

<!-- Form print header and footer, not visible on screen -->
<#macro dataAccessFormPrintHeader form type>
  <div class="clearfix border-bottom pb-3 mb-3">
    <div class="float-left">
      <img src="${brandImageSrc}" alt="Logo" class="brand-image ${brandImageClass} mr-2" style="opacity: .8; max-height: 33px">
      <span class="brand-text ${brandTextClass}" style="font-size: larger">
        <#if brandTextEnabled>
          ${config.name!""}
        </#if>
      </span>
    </div>
    <div class="float-right text-muted">
      <span><@message type/> - ${form.id} - [<@message form.status.toString()/>] - ${applicant.fullName}</span>
    </div>
  </div>
</#macro>
<#macro dataAccessFormPrintFooter form>
  <div class="border-top mt-3 pt-3">
    <div class="float-left">
      <small><span class="moment-datetime text-muted">${.now}</span></small>
    </div>
    <div class="float-right">
      <#if form.submissionDate??>
        <small><@message "form-submitted-on"/> <span class="moment-datetime text-muted">${form.submissionDate.toString()}</span></small>
      </#if>
    </div>
  </div>
</#macro>

<#macro diffsModal>
  <div class="modal fade" id="modal-diff">
    <div class="modal-dialog modal-xl">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title"><@message "form-diff"/></h4>
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
        <div class="modal-body">
          <p>
              <@message "form-diff-current-with-previous-submission"/>
            (<span class="moment-datetime">${diffs.statusChange.changedOn.toString(datetimeFormat)}</span>)
          </p>
          <ul>
            <li class="diff">
              <del>&nbsp;<@message "form-diff-deleted"/>&nbsp;</del>
            </li>
            <li class="diff">
              <ins>&nbsp;<@message "form-diff-replacement"/>&nbsp;</ins>
            </li>
          </ul>
          <div>
            <table id="diffs" class="table table-bordered table-striped diff">
              <thead>
              <tr>
                <th><@message "revisions-difference-field"/></th>
                <th><@message "revisions-difference-right"/></th>
                <th><@message "revisions-difference-left"/></th>
              </tr>
              </thead>
              <tbody>
              <#list diffs.differences as k, v>
                <tr>
                  <td>
                    <#if v[0]??>
                      <div><strong>${v[0]}</strong></div>
                    </#if>
                    <smalll><code>${k}</code></smalll>
                  </td>
                  <td class="marked">${v[1]}</td>
                  <td class="marked">${v[2]}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </div>
        </div>
        <div class="modal-footer justify-content-between">
          <button type="button" class="btn btn-default" data-dismiss="modal"><@message "close"/></button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#macro>

<!-- Variables linking -->
<#assign variablesEnabled = accessConfig.variablesEnabled && config.cartEnabled/>
<#assign feasibilityVariablesEnabled = accessConfig.feasibilityVariablesEnabled && config.cartEnabled/>
<#assign amendmentVariablesEnabled = accessConfig.amendmentVariablesEnabled && config.cartEnabled/>
