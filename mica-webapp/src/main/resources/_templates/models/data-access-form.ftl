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
      <span class="brand-text ${brandTextClass}" style="font-size: larger">${config.name!"Mica"}</span>
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
