<!-- DCE macros -->

<!-- DCE model -->
<#macro dceModel dce>
  <dl class="row">
    <#if dce.model.dataSources?? && dce.model.dataSources?size != 0>
      <dt class="col-sm-4" title="<@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.description"/>">
        <@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.title"/>
      </dt>
      <dd class="col-sm-8">
        <ul class="ps-3">
          <#list dce.model.dataSources as item>
            <li>
              <#assign txt = "study_taxonomy.vocabulary.populations-dataCollectionEvents-dataSources.term." + item + ".title"/>
              <@message txt/>
              <#if item == "others" && dce.model.otherDataSources??>
                : ${localize(dce.model.otherDataSources)}
              </#if>
            </li>
          </#list>
        </ul>
      </dd>
    </#if>

    <#if dce.model.bioSamples?? && dce.model.bioSamples?size != 0>
      <dt class="col-sm-4" title="<@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-bioSamples.description"/>">
          <@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-bioSamples.title"/>
      </dt>
      <dd class="col-sm-8">
        <ul class="ps-3">
          <#list dce.model.bioSamples as item>
            <li>
              <#assign txt = "study_taxonomy.vocabulary.populations-dataCollectionEvents-bioSamples.term." + item + ".title"/>
              <@message txt/>
              <#if item == "tissues" && dce.model.tissueTypes??>
                : ${localize(dce.model.tissueTypes)}
              <#elseif item == "others" && dce.model.otherBioSamples??>
                : ${localize(dce.model.otherBioSamples)}
              </#if>
            </li>
          </#list>
        </ul>
      </dd>
    </#if>

    <#if dce.model.administrativeDatabases?? && dce.model.administrativeDatabases?size != 0>
      <dt class="col-sm-4" title="<@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-administrativeDatabases.description"/>">
          <@message "study_taxonomy.vocabulary.populations-dataCollectionEvents-administrativeDatabases.title"/>
      </dt>
      <dd class="col-sm-8">
        <ul class="ps-3">
          <#list dce.model.administrativeDatabases as item>
            <li>
              <#assign txt = "study_taxonomy.vocabulary.populations-dataCollectionEvents-administrativeDatabases.term." + item + ".title"/>
              <@message txt/>
            </li>
          </#list>
        </ul>
      </dd>
    </#if>
  </dl>
</#macro>

<!-- Files -->
<#macro dceFilesBrowser id>
  <dl id="study-${id}-files-app-container" style="display: none;" class="row">
    <dt class="col-sm-12">
      <@message "files"/>
    </dt>
    <dd class="col-sm-12">
      <div id="study-${id}-files-app" class="mt-2">
        <@filesBrowser/>
      </div>
    </dd>
  </dl>
</#macro>

<!-- DCE modal dialog -->
<#macro dceDialog id dce>
  <div class="modal fade" id="modal-${id}">
    <div class="modal-dialog modal-lg">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title">${localize(dce.name)}</h4>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close">
            
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3 marked">
            <template>${localize(dce.description)}</template>
          </div>
          <dl class="row">
            <#if dce.start??>
              <dt class="col-sm-4">
                <@message "start-date"/>
              </dt>
              <dd class="col-sm-8">
                <div>${dce.start.yearMonth!""}</div>
              </dd>
            </#if>
            <#if dce.end??>
              <dt class="col-sm-4">
                <@message "end-date"/>
              </dt>
              <dd class="col-sm-8">
                <div>${dce.end.yearMonth!""}</div>
              </dd>
            </#if>
          </dl>
          <@dceModel dce=dce/>
          <#if showStudyDCEFiles>
            <@dceFilesBrowser id=id/>
          </#if>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary" data-bs-dismiss="modal"><@message "close"/></button>
        </div>
      </div>
      <!-- /.modal-content -->
    </div>
    <!-- /.modal-dialog -->
  </div>
  <!-- /.modal -->
</#macro>

<!-- Population model -->
<#macro dceList population>

  <!-- DCE list -->
  <#if population.dataCollectionEvents?? && population.dataCollectionEvents?size != 0>
    <#if population.dataCollectionEvents?size == 1>
      <#assign dce = population.dataCollectionEvents[0]/>
      <#assign dceId="${population.id}-${dce.id}">
      <h5><@message "study.data-collection-event"/></h5>
      <div class="mb-3 marked">
        <template>${localize(dce.description)}</template>
      </div>
      <dl class="row">
        <#if dce.start??>
          <dt class="col-sm-4">
            <@message "start-date"/>
          </dt>
          <dd class="col-sm-8">
            <div>${dce.start.yearMonth!""}</div>
          </dd>
        </#if>
        <#if dce.end??>
          <dt class="col-sm-4">
            <@message "end-date"/>
          </dt>
          <dd class="col-sm-8">
            <div>${dce.end.yearMonth!""}</div>
          </dd>
        </#if>
      </dl>
      <@dceModel dce=dce/>
      <#if showStudyDCEFiles>
        <@dceFilesBrowser id=dceId/>
      </#if>
    <#else>
      <h5><@message "study.data-collection-events"/></h5>
      <div class="table-responsive">
        <table id="population-${population.id}-dces" class="table table-bordered table-striped">
          <thead>
          <tr>
            <th>#</th>
            <th><@message "name"/></th>
            <th><@message "description"/></th>
            <th><@message "study.start"/></th>
            <th><@message "study.end"/></th>
          </tr>
          </thead>
          <tbody>
          <#list population.dataCollectionEventsSorted as dce>
            <#assign dceId="${population.id}-${dce.id}">
            <tr>
              <td>${dce.weight}</td>
              <td>
                <a href="#" data-bs-toggle="modal" data-bs-target="#modal-${dceId}">
                  ${localize(dce.name)}
                </a>
                <@dceDialog id=dceId dce=dce></@dceDialog>
              </td>
              <td class="marked">${localize(dce.description)?trim?truncate(200, "...")}</td>
              <td><#if dce.start?? && dce.start.yearMonth??>${dce.start.yearMonth}</#if></td>
              <td><#if dce.end?? && dce.end.yearMonth??>${dce.end.yearMonth}</#if></td>
            </tr>
          </#list>
          </tbody>
        </table>
      </div>
    </#if>
  </#if>
</#macro>
