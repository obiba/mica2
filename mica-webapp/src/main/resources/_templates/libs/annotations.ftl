
<#macro entityAnnotations annotations={}>
  <div class="row d-none show-on-content-loaded">
    <div class="col-12">
      <div class="card card-primary card-outline">
        <div class="card-header">
          <h3 class="card-title pt-2"><@message "classifications" /></h3>
          <ul class="nav nav-pills ml-auto float-right">
            <li class="nav-item"><a class="nav-link active" href="#summary" data-toggle="tab">
                <i class="fas fa-grip-lines"></i></a>
            </li>
            <li class="nav-item"><a class="nav-link " href="#detail" data-toggle="tab">
                <i class="fas fa-grip-horizontal"></i></a>
            </li>
          </ul>
        </div>
        <div class="card-body">
          <div class="tab-content">
            <div class="tab-pane active" id="summary">
                <#list annotations as taxonomy, taxonomyItem>
                    <@entityAnnotationsAccordion taxonomy=taxonomy taxonomyItem=taxonomyItem index=taxonomy?index detailed=false />
                </#list>
            </div>
            <div class="tab-pane" id="detail">
                <#list annotations as taxonomy, taxonomyItem>
                    <@entityAnnotationsAccordion taxonomy=taxonomy taxonomyItem=taxonomyItem index=taxonomy?index detailed=true />
                </#list>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</#macro>

<#macro entityAnnotationsAccordion taxonomy={} taxonomyItem={} index=0 detailed=true>
  <div class="accordion my-2" id="annotationsAccordion_${index}">
    <div class="card mb-0 ${taxonomy}">
        <#assign taxonomyLocalized = localize(taxonomyItem.title) />
        <#assign taxonomyId = taxonomyLocalized?replace(" ", "_") />
        <#assign collapsedClass = (index == 0)?then('collapsed', '') />
        <#assign showClass = (index == 0)?then('show', '') />

      <div class="card-header">
        <button type="button" class="${'btn btn-link btn-block text-navy text-left pl-0 ' + collapsedClass}"
                data-toggle="collapse"
                data-target="#${taxonomyId}">${taxonomyLocalized} <@itemCount item=taxonomyItem/></button>
      </div>
      <div id="${taxonomyId}" class="${'collapse ' + showClass}" data-parent="#annotationsAccordion_${index}">
        <div class="card-body">
            <#if detailed>
                <@entityAnnotationsDetail taxonomyId=taxonomyId vocabularies=taxonomyItem.vocabularies />
            <#else>
                <@entityAnnotationsSummary vocabularies=taxonomyItem.vocabularies />
            </#if>
        </div>
      </div>
    </div>
  </div>
</#macro>

<#macro entityAnnotationsDetail taxonomyId="" vocabularies={}>
    <#list vocabularies as vocabulary, vocabularyItem>
        <#if !vocabularyItem.missing>
            <#assign vocabularyId = taxonomyId + 'vocabulary' + vocabulary?index />
          <p class="mb-1 pl-1 py-2 bg-light font-weight-bold"><@vocabularyColorLabel vocabulary=vocabulary/> ${localize(vocabularyItem.title)} <@itemCount item=vocabularyItem/></p>
          <div class="pt-1 pb-1 pl-1">
            <div class="row ">
                <#list vocabularyItem.terms as termItem>
                  <div class="col-6 font-weight-normal">${localize(termItem.title)} <@itemCount item=termItem/></div>
                </#list>
            </div>
          </div>
        </#if>
    </#list>
</#macro>

<#macro entityAnnotationsSummary vocabularies={}>
  <dl class="row striped mt-0 mb-1 " style="columns: 2">
      <#list vocabularies as vocabulary, vocabularyItem>
        <dt class="col-12 mb-2">
            <@vocabularyColorLabel vocabulary=vocabulary/>
          <span class="<#if vocabularyItem.missing>text-muted</#if>">${localize(vocabularyItem.title)} <@itemCount item=vocabularyItem/> </span>
          <span><i class="fas float-right <#if vocabularyItem.missing>text-muted fa-minus<#else>text-success fa-check</#if>"></i></span>
        </dt>
      </#list>
  </dl>
</#macro>

<#macro itemCount item={}>
    <#if item.count gt -1>
      <span class="pl-1 text-muted small" style="cursor: default" title="<@message 'number-of-studies-annotated'/>">${item.count}</span>
    </#if>
</#macro>

<#macro vocabularyColorLabel vocabulary="">
  <span class="p-1 mr-1 ${vocabulary}"></span>
</#macro>
