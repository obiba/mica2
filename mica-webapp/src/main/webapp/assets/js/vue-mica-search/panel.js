const RqlPanelVocabulary = {
  template: `
  <div>
    <template v-if="criterion.type === 'TERMS'">

      <ul class="list-unstyled row">
        <li class="list-item col-sm-4" v-for="term in terms" v-bind:key="term.name" v-bind:title="localizeString(term.description)">
          <div class="form-check">
            <input class="form-check-input" type="checkbox" v-bind:id="vocabulary.name + '-' + term.name" v-bind:value="term.name" v-model="criterion.value" v-on:change="onInput()">
            <label class="form-check-label text-break" v-bind:for="vocabulary.name + '-' + term.name">{{ localizeString(term.title) }}</label>
          </div>
        </li>
      </ul>

      <div v-if="showMoreLess()" class="float-end">
        <button type="button" class="btn btn-link btn-sm" v-on:click="switchMoreLess()">
          <span v-if="!showAll" aria-hidden="true"><i class="fas fa-caret-down"></i> {{ translate("more") }}</span>
          <span v-if="showAll" aria-hidden="true"><i class="fas fa-caret-up"></i> {{ translate("less") }}</span>
        </button>
      </div>

    </template>

    <template v-else-if="criterion.type === 'NUMERIC'">
      <div class="row">
        <div class="form-group col-6 d-inline-block">
          <label v-bind:for="vocabulary.name + 'from'">{{ translate("search.from") }}</label>
          <input type="number" class="form-control" v-bind:id="vocabulary.name + '-from'" v-model="criterion.value[0]" v-on:change="onInput()">
        </div>
        <div class="form-group col-6 d-inline-block">
          <label v-bind:for="vocabulary.name + 'to'">{{ translate("search.to") }}</label>
          <input type="number" class="form-control" v-bind:id="vocabulary.name + '-to'" v-model="criterion.value[1]" v-on:change="onInput()">
        </div>
      </div>

    </template>

    <template v-else>

      <input type="text" class="form-control" v-model="criterion.value" v-on:change="onInput()">

    </template>
  </div>
  `,
  name: 'rql-panel-vocabulary',
  props: {
    vocabulary: {
      type: Object,
      required: true
    },
    query: Object,
    termsFilter: String
  },
  data: function () {
    return {
      showAll: false
    }
  },
  computed: {
    criterion() {
      let output = null;
      if (this.vocabulary) {
        output = new Criterion(this.vocabulary);

        if (this.query) {
          output.query = this.query;
        }
      }

      return output;
    },
    filteredTerms() {
      if (this.criterion.type !== "TERMS") return [];

      const localizeStringFunction = MicaFilters.localizeString;

      return (this.vocabulary.terms || []).filter(term => {
        return (!this.termsFilter || this.termsFilter.trim().length === 0) ||
        localizeStringFunction(this.vocabulary.title).toLowerCase().indexOf(this.termsFilter.toLowerCase()) > -1 ||
        term.name.toLowerCase().indexOf(this.termsFilter.toLowerCase()) > -1 ||
        localizeStringFunction(term.title).toLowerCase().indexOf(this.termsFilter.toLowerCase()) > -1;
      });
    },
    terms() {
      let terms = this.filteredTerms;

      if (!this.showAll && terms.length > 12) {
        terms = terms.slice(0,12);
      }

      return terms;
    }
  },
  watch: {
    query(val) {
      if (val) this.criterion.query = val;
    }
  },
  methods: {
    onInput() {
      this.$emit("update-query", this.criterion);
    },
    switchMoreLess() {
      this.showAll = !this.showAll;
    },
    showMoreLess() {
      return this.filteredTerms.length > 12;
    }
  }
};

const RqlPanel = {
  template: `
  <div>
    <template v-if="!hasExternalFilter">
    <div class="input-group mb-4">
      <input type="text" class="form-control" :placeholder="translate('search.filter-help')" v-model="panelFilter">
      <div class="input-group-append">
        <span class="input-group-text">{{ translate("search.filter") }}</span>
      </div>
    </div>
    </template>

    <template v-if="Array.isArray(taxonomy)">
    <div v-for="sub in taxonomy" v-bind:key="sub.name">
      <rql-panel v-bind:target="target" v-bind:taxonomy="sub" v-bind:query="query" v-on:update-query="updateQuery" v-on:remove-query="removeQuery" v-bind:external-filter="theFilter" v-bind:has-external-filter="true"></rql-panel>
    </div>
    </template>

    <template v-else>
    <h4 class="mt-3 panel-taxonomy-title" v-if="vocabularies.length > 0" v-bind:title="localizeString(taxonomy.description)">
      {{ localizeString(taxonomy.title) }}
    </h4>

    <p class="text-muted panel-taxonomy-description" v-if="vocabularies.length > 0">{{ localizeString(taxonomy.description) }}</p>

    <div class="row d-flex align-items-stretch">
      <div class="col-12 col-sm-12 col-md-6 d-flex align-items-stretch" v-for="vocabulary in vocabularies" v-bind:key="vocabulary.name">
        <div class="card mb-2 w-100">
          <div class="card-header bg-light">
            <span class="panel-vocabulary-title" v-bind:title="localizeString(vocabulary.description)">{{ localizeString(vocabulary.title) }}</span>
            <span class="float-end">
              <button type="button" v-bind:id="vocabulary.name + '-select-all'" class="btn btn-link btn-sm pt-0 pb-0" v-if="canDoSelectAll(vocabulary)" v-on:click="selectAll(vocabulary)"><span aria-hidden="true">{{ translate("select-all") }}</span></button>
              <button type="button" v-bind:id="vocabulary.name + '-clear-selection'" class="btn btn-link btn-sm pt-0 pb-0" v-if="hasAssociatedQuery(vocabulary)" v-on:click="clear(vocabulary)"><span aria-hidden="true">{{ translate("clear-selection") }}</span></button>
            </span>
          </div>
          <div class="card-body">
            <div v-if="vocabulary.description" class="panel-vocabulary-description text-muted mb-4">
              {{ localizeString(vocabulary.description) }}
            </div>
            <rql-panel-vocabulary v-bind:vocabulary="vocabulary" v-bind:query="getAssociatedQuery(vocabulary)" v-bind:termsFilter="theFilter" v-on:update-query="updateQuery"></rql-panel-vocabulary>
          </div>
        </div>
      </div>
    </div>

    </template>
  </div>
  `,
  name: "rql-panel",
  props: {
    target: {
      type: String,
      required: true
    },
    hasExternalFilter: Boolean,
    externalFilter: String,
    query: Object,
    taxonomy: [Object, Array]
  },
  components: {
    RqlPanelVocabulary
  },
  data() {
    return {
      panelFilter: ""
    };
  },
  computed: {
    inputs() {
      return Criterion.splitQuery(this.query);
    },
    vocabularies() {
      if (!this.taxonomy) return [];

      const localizeStringFunction = MicaFilters.localizeString;

      return (this.taxonomy.vocabularies || [])
      .filter(vocabulary => {
        let found = (vocabulary.attributes || []).filter(attribute => attribute.key === "hidden").map(attribute => attribute.value);
        return found.length === 0 || "true" !== found[0];
      })
      .filter(vocabulary => {
        let passes = (!this.theFilter || this.theFilter.trim().length === 0);
        let vocabularyPasses = passes || localizeStringFunction(vocabulary.title).toLowerCase().indexOf(this.theFilter.toLowerCase()) > -1;
        if ("TERMS" !== Criterion.typeOfVocabulary(vocabulary) && vocabularyPasses) return true;

        let foundTerms = (vocabulary.terms || []).filter(term => passes || term.name.toLowerCase().indexOf(this.theFilter.toLowerCase()) > -1 || localizeStringFunction(term.title).toLowerCase().indexOf(this.theFilter.toLowerCase()) > -1);
        return vocabularyPasses || foundTerms.length > 0;
      });
    },
    theFilter() {
      return this.hasExternalFilter ? this.externalFilter : this.panelFilter;
    }
  },
  methods: {
    getAssociatedQuery(vocabulary) {
      if (this.query) {
        return Criterion.associatedQuery(vocabulary, this.inputs);
      }

      return undefined;
    },
    hasAssociatedQuery(vocabulary) {
      return this.getAssociatedQuery(vocabulary) && true;
    },
    canDoSelectAll(vocabulary) {
      let type = Criterion.typeOfVocabulary(vocabulary);
      if (type === "TERMS") {
        let query = this.getAssociatedQuery(vocabulary);

        if (query) {
          if (query && Array.isArray(query)) {
            return vocabulary.terms.length > (query.reduce((acc, curr) => acc + (curr.args[1] || []).length, 0));
          } else {
            return vocabulary.terms.length > (query.args[1] || []).length;
          }
        }

        return true;
      }

      return false;
    },
    selectAll(payload) {
      let criterion = new Criterion(payload);
      criterion.query = {operator: "exists"};
      this.updateQuery(criterion);
    },
    clear(payload) {
      let criterion = new Criterion(payload);
      let input = this.getAssociatedQuery(payload);

      if (input) {
        criterion.query = input;
        this.removeQuery(criterion);
      }
    },
    updateQuery(payload) {
      if (payload instanceof Criterion) {
        const query = payload.asQuery(this.taxonomy.name);

        if ((payload.type === "NUMERIC" && query.args[1].length === 0) || (payload.type === "TERMS" && payload.value.length === 0)) {
          this.$emit("remove-query", {target: this.target, query});
        } else {
          this.$emit("update-query", {target: this.target, query});
        }
      } else {
        this.$emit("update-query", payload);
      }
    },
    removeQuery(payload) {
      if (payload instanceof Criterion) this.$emit("remove-query", {target: this.target, query: payload.asQuery(this.taxonomy.name)});
      else this.$emit("remove-query", payload);
    }
  }
};

MicaVueApp.component(RqlPanel.name, RqlPanel);
