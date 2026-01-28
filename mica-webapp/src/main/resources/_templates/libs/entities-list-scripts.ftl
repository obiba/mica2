<script>
  const Mica = {
    config: ${configJson!"{}"},
    locale: "${.lang}",
    defaultLocale: "${defaultLang}"
  };

  Mica.tr = {
    "network": "<@message "network"/>",
    "networks": "<@message "networks"/>",
    "study": "<@message "study"/>",
    "studies": "<@message "studies"/>",
    "variable": "<@message "variable"/>",
    "variables": "<@message "variables"/>",
    "individual-study": "<@message "individual-study"/>",
    "individual-studies": "<@message "individual-studies"/>",
    "harmonization-study": "<@message "harmonization-study"/>",
    "harmonization-studies": "<@message "harmonization-studies"/>",
    "collected-dataset": "<@message "collected-dataset"/>",
    "collected-datasets": "<@message "collected-datasets"/>",
    "harmonized-dataset": "<@message "harmonized-dataset"/>",
    "harmonized-datasets": "<@message "harmonized-datasets"/>",
    "collected-variable": "<@message "collected-variable"/>",
    "collected-variables": "<@message "collected-variables"/>",
    "harmonized-variable": "<@message "harmonized-variable"/>",
    "harmonized-variables": "<@message "harmonized-variables"/>",
    "study-variable": "<@message "study-variable"/>",
    "study-variables": "<@message "study-variables"/>",
    "harmonization-study-variable": "<@message "harmonization-study-variable"/>",
    "harmonization-study-variables": "<@message "harmonization-study-variables"/>",
    "study-with-variables": "<@message "study-with-variables"/>",
    "studies-with-variables": "<@message "studies-with-variables"/>",
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
<script src="${assetsPath}/libs/node_modules/@vue/compat/dist/vue.global.js"></script>
<script>
  // Configure Vue 3 compat mode to allow Vue 2 APIs
  if (Vue && Vue.configureCompat) {
    Vue.configureCompat({
      MODE: 2  // Use Vue 2 compatibility mode
    });
  }

  // Global translate filter for compatibility with existing components
  // This will be removed once all components are migrated
  if (Vue && Vue.filter) {
    Vue.filter("translate", (key) => {
      let value = Mica.tr[key];
      return typeof value === "string" ? value : key;
    });
  }
</script>
<script src="${assetsPath}/libs/node_modules/rql/dist/rql.js"></script>
<script src="${assetsPath}/js/mica-list-entities.js"></script>
