<!-- Compare page macros -->

<!-- Individual studies compare model template -->
<#macro individualStudiesCompareModel studies>
  <tr id="name">
    <td class="fw-bold">
        <@message "name"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('name')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>${localize(study.name)}</td>
    </#list>
  </tr>
  <tr id="objectives">
    <td class="fw-bold">
        <@message "study.objectives"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('objectives')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <small class="marked truncate truncate-300">
          <template>${localize(study.objectives)}</template></small>
      </td>
    </#list>
  </tr>
  <tr id="start-year">
    <td class="fw-bold">
        <@message "study.start-year"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('start-year')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.startYear??>
            ${study.model.startYear?c}
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="end-year">
    <td class="fw-bold">
        <@message "study.end-year"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('end-year')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.endYear??>
            ${study.model.endYear?c}
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="funding">
    <td class="fw-bold">
        <@message "funding"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('funding')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>${localize(study.funding)}</td>
    </#list>
  </tr>
  <tr id="website">
    <td class="fw-bold">
      <@message "website"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('website')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.website??>
          <a href="${study.model.website}" target="_blank">${study.model.website}</a>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="methods-design">
    <td class="fw-bold">
      <@message "study_taxonomy.vocabulary.methods-design.title"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('methods-design')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.methods?? && study.model.methods.design??>
          <#assign text = "study_taxonomy.vocabulary.methods-design.term." + study.model.methods.design + ".title"/>
          <@message text/>
          <#if study.model.methods.design == "other" && study.model.methods.otherDesign??>
            : ${localize(study.model.methods.otherDesign)}
          </#if>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="methods-follow-up">
    <td class="fw-bold">
        <@message "study.follow-up"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('methods-follow-up')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.methods?? && study.model.methods.followUpInfo??>
          <small>${localize(study.model.methods.followUpInfo)}</small>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="marker-paper">
    <td class="fw-bold">
      <@message "study.marker-paper"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('marker-paper')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.markerPaper??>
          <small>${study.model.markerPaper}</small>
          <#if study.model.pubmedId??>
            <div>
              <a href="http://www.ncbi.nlm.nih.gov/pubmed/${study.model.pubmedId}" target="_blank">PUBMED ${study.model.pubmedId}</a>
            </div>
          </#if>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="methods-recruitments">
    <td class="fw-bold">
      <@message "study.recruitment-sources.label"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('methods-recruitments')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.methods?? && study.model.methods.recruitments??>
          <#assign first = true/>
          <#list study.model.methods.recruitments as type>
            <#assign text = "study_taxonomy.vocabulary.methods-recruitments.term." + type + ".title"/>
            <#if !first> | </#if>
            <@message text/>
            <#if type == "other" && study.model.methods.otherRecruitment??>
              : ${localize(study.model.methods.otherRecruitment)}
            </#if>
            <#assign first = false/>
          </#list>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="numberOfParticipants-participants">
    <td class="fw-bold">
        <@message "numberOfParticipants.participants"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('numberOfParticipants-participants')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.numberOfParticipants??>
          <#if study.model.numberOfParticipants.participant.number??>
            ${study.model.numberOfParticipants.participant.number}
            <#if study.model.numberOfParticipants.participant.noLimit == true>
              (<@message "numberOfParticipants.no-limit"/>)
            </#if>
          <#else>
            <#if study.model.numberOfParticipants.participant.noLimit?? && study.model.numberOfParticipants.participant.noLimit == true>
              <@message "numberOfParticipants.no-limit"/>
            </#if>
          </#if>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="numberOfParticipants-sample">
    <td class="fw-bold">
      <@message "numberOfParticipants.sample"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('numberOfParticipants-sample')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.numberOfParticipants??>
          <#if study.model.numberOfParticipants.sample.number??>
            ${study.model.numberOfParticipants.sample.number}
            <#if study.model.numberOfParticipants.sample.noLimit == true>
              (<@message "numberOfParticipants.no-limit"/>)
            </#if>
          <#else>
            <#if study.model.numberOfParticipants.sample.noLimit?? && study.model.numberOfParticipants.sample.noLimit == true>
              <@message "numberOfParticipants.no-limit"/>
            </#if>
          </#if>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="populations">
    <td class="fw-bold">
      <@message "study.populations"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.populations??>
          <#assign first = true/>
          <#list study.populations as population>
            <#if !first> | </#if>
            ${localize(population.name)}
            <#assign first = false/>
          </#list>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="populations-gender">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "population.gender"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-gender')"><i class="fa-solid fa-times"></i></a></small>
    </td>
      <#list studies as study>
        <td>
          <#if study.populations??>
            <#assign first = true/>
            <#list study.populations as population>
              <#if !first> | </#if>
              <#if population.model.selectionCriteria?? && population.model.selectionCriteria.gender??>
                <#assign text = "study.selection-criteria.gender." + population.model.selectionCriteria.gender/>
                <@message text/>
              <#elseif study.populations?size gt 1>
                <@message "global.not-applicable"/>
              </#if>
              <#assign first = false/>
            </#list>
          </#if>
        </td>
      </#list>
  </tr>
  <tr id="populations-ageMin">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "population.ageMin"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-ageMin')"><i class="fa-solid fa-times"></i></a></small>
    </td>
      <#list studies as study>
        <td>
          <#if study.populations??>
            <#assign first = true/>
            <#list study.populations as population>
              <#if !first> | </#if>
              <#if population.model.selectionCriteria?? && population.model.selectionCriteria.ageMin??>
                ${population.model.selectionCriteria.ageMin}
              <#elseif study.populations?size gt 1>
                <@message "global.not-applicable"/>
              </#if>
              <#assign first = false/>
            </#list>
          </#if>
        </td>
      </#list>
  </tr>
  <tr id="populations-ageMax">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "population.ageMax"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-ageMax')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.populations??>
          <#assign first = true/>
          <#list study.populations as population>
            <#if !first> | </#if>
            <#if population.model.selectionCriteria?? && population.model.selectionCriteria.ageMax??>
              ${population.model.selectionCriteria.ageMax}
            <#elseif study.populations?size gt 1>
              <@message "global.not-applicable"/>
            </#if>
            <#assign first = false/>
          </#list>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="populations-pregnantWomen">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "study.selection-criteria.pregnant-women"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-pregnantWomen')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.populations??>
          <#assign first = true/>
          <#list study.populations as population>
            <#if !first> | </#if>
            <#if population.model.selectionCriteria?? && population.model.selectionCriteria.pregnantWomen??>
              <#assign firstItem = true/>
              <#list population.model.selectionCriteria.pregnantWomen as item>
                <#if !firstItem>, </#if>
                  <#assign text = "study_taxonomy.vocabulary.populations-selectionCriteria-pregnantWomen.term." + item + ".title"/>
                  <@message text/>
                <#assign firstItem = false/>
              </#list>
            <#elseif study.populations?size gt 1>
              <@message "global.not-applicable"/>
            </#if>
            <#assign first = false/>
          </#list>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="populations-newborn">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "study.selection-criteria.newborn"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-newborn')"><i class="fa-solid fa-times"></i></a></small>
    </td>
      <#list studies as study>
        <td>
          <#if study.populations??>
            <#assign first = true/>
            <#list study.populations as population>
              <#if !first> | </#if>
              <#if population.model.selectionCriteria?? && population.model.selectionCriteria.newborn??>
                <#if population.model.selectionCriteria.newborn == true>
                  <@message "global.yes"/>
                <#else>
                  <@message "global.no"/>
                </#if>
              <#elseif study.populations?size gt 1>
                <@message "global.not-applicable"/>
              </#if>
              <#assign first = false/>
            </#list>
          </#if>
        </td>
      </#list>
  </tr>
  <tr id="populations-twins">
    <td class="fw-bold">
      <@message "study.populations"/> - <@message "study.selection-criteria.twins"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-twins')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.populations??>
          <#assign first = true/>
          <#list study.populations as population>
            <#if !first> | </#if>
            <#if population.model.selectionCriteria?? && population.model.selectionCriteria.twins??>
              <#if population.model.selectionCriteria.twins == true>
                <@message "global.yes"/>
              <#else>
                <@message "global.no"/>
              </#if>
            <#elseif study.populations?size gt 1>
              <@message "global.not-applicable"/>
            </#if>
            <#assign first = false/>
          </#list>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="populations-countriesIso">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "client.label.countries"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-countriesIso')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.populations??>
          <#assign first = true/>
          <#list study.populations as population>
            <#if !first> | </#if>
            <#if population.model.selectionCriteria?? && population.model.selectionCriteria.countriesIso??>
              <#assign firstItem = true/>
              <#list population.model.selectionCriteria.countriesIso as item>
                <#if !firstItem>, </#if>
                <@message item/>
                <#assign firstItem = false/>
              </#list>
            <#elseif study.populations?size gt 1>
              <@message "global.not-applicable"/>
            </#if>
            <#assign first = false/>
          </#list>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="populations-territory">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "study.selection-criteria.territory"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-territory')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <small>
          <#if study.populations??>
            <#assign first = true/>
            <#list study.populations as population>
              <#if !first> | </#if>
              <#if population.model.selectionCriteria?? && population.model.selectionCriteria.territory??>
                ${localize(population.model.selectionCriteria.territory)}
              <#elseif study.populations?size gt 1>
                <@message "global.not-applicable"/>
              </#if>
              <#assign first = false/>
            </#list>
          </#if>
        </small>
      </td>
    </#list>
  </tr>
  <tr id="populations-ethnicOrigin">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "study.selection-criteria.ethnic-origin"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-ethnicOrigin')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.populations??>
          <#assign first = true/>
          <#list study.populations as population>
            <#if !first> | </#if>
            <#if population.model.selectionCriteria?? && population.model.selectionCriteria.ethnicOrigin??>
              <#assign firstItem = true/>
              <#list population.model.selectionCriteria.ethnicOrigin as item>
                <#if !firstItem>, </#if>
                  ${localize(item, "N/A")}
                <#assign firstItem = false/>
              </#list>
            <#elseif study.populations?size gt 1>
              <@message "global.not-applicable"/>
            </#if>
            <#assign first = false/>
          </#list>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="populations-healthStatus">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "study.selection-criteria.health-status"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-healthStatus')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.populations??>
          <#assign first = true/>
          <#list study.populations as population>
            <#if !first> | </#if>
            <#if population.model.selectionCriteria?? && population.model.selectionCriteria.healthStatus??>
              <#assign firstItem = true/>
              <#list population.model.selectionCriteria.healthStatus as item>
                <#if !firstItem>, </#if>
                ${localize(item, "N/A")}
                <#assign firstItem = false/>
              </#list>
            <#elseif study.populations?size gt 1>
              <@message "global.not-applicable"/>
            </#if>
            <#assign first = false/>
          </#list>
        </#if>
      </td>
    </#list>
  </tr>
  <tr id="populations-otherCriteria">
    <td class="fw-bold">
        <@message "study.populations"/> - <@message "population.otherCriteria"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('populations-otherCriteria')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <small>
          <#if study.populations??>
            <#assign first = true/>
            <#list study.populations as population>
              <#if !first> | </#if>
              <#if population.model.selectionCriteria?? && population.model.selectionCriteria.otherCriteria??>
                ${localize(population.model.selectionCriteria.otherCriteria)}
              <#elseif study.populations?size gt 1>
                <@message "global.not-applicable"/>
              </#if>
              <#assign first = false/>
            </#list>
          </#if>
        </small>
      </td>
    </#list>
  </tr>
</#macro>

<!-- Individual studies compare model template -->
<#macro harmonizationStudiesCompareModel studies>
  <tr id="harmo-name">
    <td class="fw-bold">
      <@message "name"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('harmo-name')"><i class="fa-solid fa-times"></i></a></small>
    </td>
      <#list studies as study>
        <td>${localize(study.name)}</td>
      </#list>
  </tr>
  <tr id="harmo-objectives">
    <td class="fw-bold">
      <@message "study.objectives"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('harmo-objectives')"><i class="fa-solid fa-times"></i></a></small>
    </td>
      <#list studies as study>
        <td>
          <small class="marked truncate truncate-300">
            <template>${localize(study.objectives)}</template></small>
        </td>
      </#list>
  </tr>
  <tr id="harmo-design">
    <td class="fw-bold">
      <@message "study_taxonomy.vocabulary.harmonizationDesign.title"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('harmo-design')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list studies as study>
      <td>
        <#if study.model.harmonizationDesign??>
          ${localize(study.model.harmonizationDesign)}
        </#if>
      </td>
    </#list>
  </tr>
</#macro>


<!-- Networks compare model template -->
<#macro networksCompareModel networks>
  <tr id="name">
    <td class="fw-bold">
        <@message "name"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('name')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list networks as network>
      <td>${localize(network.name)}</td>
    </#list>
  </tr>
  <tr id="description">
    <td class="fw-bold">
        <@message "network.description"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('description')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list networks as network>
      <td>
        <small class="marked truncate truncate-300">
          <template>${localize(network.description)}</template></small>
      </td>
    </#list>
  </tr>
  <tr id="website">
    <td class="fw-bold">
        <@message "website"/>
      <small><a href="javascript:void(0)" onclick="onRemoveRow('website')"><i class="fa-solid fa-times"></i></a></small>
    </td>
    <#list networks as network>
      <td>
        <#if network.model.website??>
          <a class="d-print-none" href="${network.model.website}" target="_blank">${network.model.website}</a>
        </#if>
      </td>
    </#list>
  </tr>
</#macro>
