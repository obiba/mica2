
<#macro entityAnnotations annotations={} showCount=false>
  <#assign taxonomyKeys = annotations?keys?filter(key -> !classificationTaxonomiesToExclude?seq_contains(key)) >

  <div class="row d-none show-on-content-loaded">
    <div class="col-12">
      <div class="card card-primary card-outline">
        <div class="card-header">
          <h3 class="card-title pt-2"><@message "classifications" /></h3>
          <ul class="nav nav-pills ms-auto float-end">
            <li class="nav-item"><a class="nav-link active" title="<@message "summary-view" />" href="#summary" data-bs-toggle="tab">
                <i class="fa-solid fa-grip-lines"></i></a>
            </li>
            <li class="nav-item"><a class="nav-link " title="<@message "detail-view" />" href="#detail" data-bs-toggle="tab">
                <i class="fa-solid fa-grip-horizontal"></i></a>
            </li>
          </ul>
        </div>
        <div class="card-body">
          <div class="tab-content">
            <div class="tab-pane active" id="summary">
                <#list taxonomyKeys as taxonomy>
                  <@entityAnnotationsAccordion taxonomy=taxonomy taxonomyItem=annotations[taxonomy] index=taxonomy?index detailed=false />
                </#list>
            </div>
            <div class="tab-pane" id="detail">
                <#list taxonomyKeys as taxonomy>
                  <@entityAnnotationsAccordion taxonomy=taxonomy taxonomyItem=annotations[taxonomy] index=taxonomy?index detailed=true />
                </#list>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</#macro>

<#macro entityAnnotationsAccordion taxonomy="" taxonomyItem={} index=0 detailed=true>
  <div class="accordion my-2" id="annotationsAccordion_${index}">
    <div class="card mb-0 ${taxonomy}">
        <#assign taxonomyLocalized = localize(taxonomyItem.title) />
        <#assign collapsedClass = (index != 0)?then('collapsed', '') />
        <#assign showClass = (index == 0)?then('show', '') />

      <div class="card-header pe-2">
        <button type="button" class="${'btn btn-block text-navy text-start ps-0 ' + collapsedClass}"
                data-bs-toggle="collapse"
                data-bs-target="#${taxonomy}">${taxonomyLocalized} <span class="badge badge-light"><@itemCount item=taxonomyItem/></span></button>
      </div>
      <div id="${taxonomy}" class="${'collapse ' + showClass}" data-parent="#annotationsAccordion_${index}">
        <div class="card-body">
            <#if detailed>
                <@entityAnnotationsDetail taxonomyId=taxonomy vocabularies=taxonomyItem.vocabularies />
            <#else>
                <@entityAnnotationsSummary vocabularies=taxonomyItem.vocabularies />
            </#if>
        </div>
      </div>
    </div>
  </div>
</#macro>

<#macro entityAnnotationsDetail taxonomyId="" vocabularies={}>
    <#list vocabularies as vocabulary>
        <#if !vocabulary.missing>
            <#assign vocabularyId = taxonomyId + 'vocabulary' + vocabulary?index />
          <p class="mb-1 ps-1 py-2 bg-light font-weight-bold"><@vocabularyColorLabel vocabulary=vocabulary.name/> ${localize(vocabulary.title)} <span class="float-end pe-2"><@itemCount item=vocabulary/></span></p>
          <div class="pt-1 pb-1 ps-1">
            <div class="row ">
                <#list vocabulary.terms as termItem>
                  <div class="col-xs-12 col-md-6 font-weight-normal">${localize(termItem.title)} <span class="float-end"><@itemCount item=termItem/></span></div>
                </#list>
            </div>
          </div>
        </#if>
    </#list>
</#macro>

<#macro entityAnnotationsSummary vocabularies={}>
  <dl class="row mt-0 mb-1 " style="columns: 2">
      <#list vocabularies as vocabulary>
        <dt class="col-12 <#if !vocabulary?is_last>mb-2</#if>">
            <@vocabularyColorLabel vocabulary=vocabulary.name/>
          <span class="<#if vocabulary.missing>text-muted</#if>">${localize(vocabulary.title)}</span>
            <#if vocabulary.missing>
              <span><i class="fa-solid float-end text-muted fa-minus"></i></span>
            <#elseif vocabulary.count == -1>
              <span><i class="fa-solid fa-check float-end text-success"></i></span>
            <#else>
              <span class="float-end"><@itemCount item=vocabulary/></span>
            </#if>
            <#--          <span><i class="fa-solid float-end <#if vocabularyItem.missing>text-muted fa-minus<#else>text-success fa-check</#if>"></i></span>-->
        </dt>
      </#list>
  </dl>
</#macro>

<#macro itemCount item={}>
    <#if item.count gt -1>
      <span class="px-1 text-muted" style="cursor: default" title="<@message 'number-of-studies-annotated'/>">${item.count}</span>
    </#if>
</#macro>

<#macro vocabularyColorLabel vocabulary="">
  <span class="p-1 me-1 ${vocabulary}"></span>
</#macro>
