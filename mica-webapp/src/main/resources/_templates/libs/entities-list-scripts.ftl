<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<script>
  const Mica = {
    config: ${configJson!"{}"},
    locale: "${.lang}",
    defaultLocale: "${defaultLang}"
  };

  Mica.tr = {
    "network": "${messageJs("network")}",
    "networks": "${messageJs("networks")}",
    "study": "${messageJs("study")}",
    "studies": "${messageJs("studies")}",
    "variable": "${messageJs("variable")}",
    "variables": "${messageJs("variables")}",
    "individual-study": "${messageJs("individual-study")}",
    "individual-studies": "${messageJs("individual-studies")}",
    "harmonization-study": "${messageJs("harmonization-study")}",
    "harmonization-studies": "${messageJs("harmonization-studies")}",
    "collected-dataset": "${messageJs("collected-dataset")}",
    "collected-datasets": "${messageJs("collected-datasets")}",
    "harmonized-dataset": "${messageJs("harmonized-dataset")}",
    "harmonized-datasets": "${messageJs("harmonized-datasets")}",
    "collected-variable": "${messageJs("collected-variable")}",
    "collected-variables": "${messageJs("collected-variables")}",
    "harmonized-variable": "${messageJs("harmonized-variable")}",
    "harmonized-variables": "${messageJs("harmonized-variables")}",
    "study-variable": "${messageJs("study-variable")}",
    "study-variables": "${messageJs("study-variables")}",
    "harmonization-study-variable": "${messageJs("harmonization-study-variable")}",
    "harmonization-study-variables": "${messageJs("harmonization-study-variables")}",
    "study-with-variables": "${messageJs("study-with-variables")}",
    "studies-with-variables": "${messageJs("studies-with-variables")}",
    "number-participants": "${messageJs("number-participants")}",
    "cohort_study": "${messageJs("study_taxonomy.vocabulary.methods-design.term.cohort_study.title")}",
    "case_control": "${messageJs("study_taxonomy.vocabulary.methods-design.term.case_control.title")}",
    "case_only": "${messageJs("study_taxonomy.vocabulary.methods-design.term.case_only.title")}",
    "cross_sectional": "${messageJs("study_taxonomy.vocabulary.methods-design.term.cross_sectional.title")}",
    "clinical_trial": "${messageJs("study_taxonomy.vocabulary.methods-design.term.clinical_trial.title")}",
    "other": "${messageJs("study_taxonomy.vocabulary.methods-design.term.other.title")}",
    "listing-typeahead-placeholder": "${messageJs("global.list-search-placeholder")}",
  };
</script>
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.global.js"></script>
<script src="${assetsPath}/libs/node_modules/rql/dist/rql.js"></script>
<script src="${assetsPath}/js/mica-list-entities.js"></script>
</#outputformat>
