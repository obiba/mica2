
<#macro entityAnnotations annotations={}>
  <div class="row">
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
  <div class="accordion my-2" id="annoationsAccordion">
    <div class="card mb-0">
        <#assign taxonomyLocalized = localize(taxonomyItem.title) />
        <#assign taxonomyId = taxonomyLocalized?replace(" ", "_") />
        <#assign collapsedClass = (index == 0)?then('collapsed', '') />
        <#assign showClass = (index == 0)?then('show', '') />

      <div class="card-header">
        <button type="button" class="${'btn btn-link btn-block text-navy text-left pl-0 ' + collapsedClass}"
                data-toggle="collapse"
                data-target="#${taxonomyId}">${taxonomyLocalized} <@itemCount item=taxonomyItem/></button>
      </div>
      <div id="${taxonomyId}" class="${'collapse ' + showClass}" data-parent="#annoationsAccordion">
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
        <#assign vocabularyId = taxonomyId + 'vocabulary' + vocabulary?index />
      <p class="mb-1 pl-1 py-2 bg-light font-weight-bold">${localize(vocabularyItem.title)} <@itemCount item=vocabularyItem/></p>
      <div class="pt-1 pb-1 pl-1">
        <div class="row ">
            <#list vocabularyItem.terms as termItem>
              <div class="col-6 font-weight-normal">${localize(termItem.title)} <@itemCount item=termItem/></div>
            </#list>
        </div>
      </div>
    </#list>
</#macro>

<#macro entityAnnotationsSummary vocabularies={}>
  <dl class="row striped mt-0 mb-1 " style="columns: 2">
      <#list vocabularies as vocabulary, vocabularyItem>
        <dt class="col-12" style="border-left: #36A2EB solid 10px; margin-bottom: 3px">
          <span></span>${localize(vocabularyItem.title)} <@itemCount item=vocabularyItem/></span>
        </dt>
      </#list>
  </dl>
</#macro>

<#macro itemCount item={}>
    <#if item.count gt -1>
      <span class="ml-1 text-muted"><small>(${item.count})</small></span>
    </#if>
</#macro>
