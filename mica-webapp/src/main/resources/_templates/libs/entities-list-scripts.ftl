<script>
  const Mica = {
    config: ${configJson!"{}"},
    locale: "${.lang}",
    defaultLocale: "${defaultLang}"
  };

  Mica.tr = {
    "collected-dataset": "<@message "collected-dataset"/>",
    "collected-datasets": "<@message "collected-datasets"/>",
    "harmonized-dataset": "<@message "harmonized-dataset"/>",
    "harmonized-datasets": "<@message "harmonized-datasets"/>",
    "collected-variable": "<@message "collected-variable"/>",
    "collected-variables": "<@message "collected-variables"/>",
    "harmonized-variable": "<@message "harmonized-variable"/>",
    "harmonized-variables": "<@message "harmonized-variables"/>",
    "number-participants": "<@message "number-participants"/>",
    "cohort_study": "<@message "study_taxonomy.vocabulary.methods-design.term.cohort_study.title"/>",
    "case_control": "<@message "study_taxonomy.vocabulary.methods-design.term.case_control.title"/>",
    "case_only": "<@message "study_taxonomy.vocabulary.methods-design.term.case_only.title"/>",
    "cross_sectional": "<@message "study_taxonomy.vocabulary.methods-design.term.cross_sectional.title"/>",
    "clinical_trial": "<@message "study_taxonomy.vocabulary.methods-design.term.clinical_trial.title"/>",
    "other": "<@message "study_taxonomy.vocabulary.methods-design.term.other.title"/>",
    "listing-typeahead-placeholder": "<@message "global.list-search-placeholder"/>",
  };
</script>
<script src="${assetsPath}/libs/node_modules/vue/dist/vue.js"></script>
<script src="${assetsPath}/libs/node_modules/rql/dist/rql.js"></script>
<script src="${assetsPath}/js/mica-list-entities.js"></script>
