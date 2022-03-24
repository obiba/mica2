const RqlQuery = {
  template: `
  <div class="btn-group btn-sm dropdown my-0">
    <button type="button" class="btn btn-info btn-sm dropdown-toggle" data-toggle="dropdown">
      <i class="fa fa-info-circle" v-bind:title="vocabulary.title | localize-string"></i>
      <span class="pl-1">{{ getCriterionAsString() }}</span>
    </button>

    <div ref="menu" class="dropdown-menu" style="width: 25em;">
      <div class="container" v-bind:title="vocabulary.description | localize-string"><i class="fa fa-info-circle"></i> {{ vocabulary.title | localize-string }}</div>
      <div class="dropdown-divider"></div>

      <template v-if="criterion.type === 'TERMS'">

      <div class="container" v-if="!termQueryIsReadOnly">
        <div class="form-check">
          <input class="form-check-input" type="radio" v-bind:id="'radio-' + vocabulary.name + '-all'" v-bind:name="vocabulary.name + '-terms-choice'" value="exists" v-model="criterion.operator" v-on:change="onInput()">
          <label class="form-check-label" v-bind:for="'radio-' + vocabulary.name + '-all'">{{ "search.any" | translate }}</label>
        </div>
        <div class="form-check">
          <input class="form-check-input" type="radio" v-bind:id="'radio-' + vocabulary.name + '-none'" v-bind:name="vocabulary.name + '-terms-choice'" value="missing" v-model="criterion.operator" v-on:change="onInput()">
          <label class="form-check-label" v-bind:for="'radio-' + vocabulary.name + '-none'">{{ "search.none" | translate }}</label>
        </div>
        <div class="form-check">
          <input class="form-check-input" type="radio" v-bind:id="'radio-' + vocabulary.name + '-in'" v-bind:name="vocabulary.name + '-terms-choice'" value="in" v-model="criterion.operator" v-on:change="onInput()">
          <label class="form-check-label" v-bind:for="'radio-' + vocabulary.name + '-in'">{{ "search.in" | translate }}</label>
        </div>
        <div class="form-check">
          <input class="form-check-input" type="radio" v-bind:id="'radio-' + vocabulary.name + '-not-in'" v-bind:name="vocabulary.name + '-terms-choice'" value="out" v-model="criterion.operator" v-on:change="onInput()">
          <label class="form-check-label" v-bind:for="'radio-' + vocabulary.name + '-not-in'">{{ "search.out" | translate }}</label>
        </div>
      </div>
      <div class="container" v-else>
        {{ ( "search." + criterion.operator ) | translate }}
      </div>

      <div class="dropdown-divider"></div>

      <div class="container" v-if="!termQueryIsReadOnly">
        <div class="input-group mb-2">
          <input type="text" class="form-control" v-model="termsFilter">
          <div class="input-group-append">
            <span class="input-group-text">{{ "search.filter" | translate }}</span>
          </div>
        </div>
        <ul class="list-unstyled" style="max-height: 24em; overflow-y: auto;">
          <li v-for="term in checkedTerms" v-bind:key="term.name">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" v-bind:id="vocabulary.name + '-' + term.name" v-bind:value="term.name" v-bind:name="vocabulary.name + 'terms[]'" v-model="criterion.value" v-on:change="onInput()">
              <label class="form-check-label" v-bind:for="vocabulary.name + '-' + term.name" v-bind:title="term.description | localize-string">{{ term.title | localize-string }}</label>
            </div>
          </li>

          <li v-for="term in uncheckedTerms" v-bind:key="term.name">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" v-bind:id="vocabulary.name + '-' + term.name" v-bind:value="term.name" v-bind:name="vocabulary.name + 'terms[]'" v-model="criterion.value" v-on:change="onInput()">
              <label class="form-check-label" v-bind:for="vocabulary.name + '-' + term.name" v-bind:title="term.description | localize-string">{{ term.title | localize-string }}</label>
            </div>
          </li>
        </ul>
      </div>
      <div class="container" v-else>
        <li v-for="term in checkedTerms" v-bind:key="term.name">
          <label class="form-check-label" v-bind:for="vocabulary.name + '-' + term.name" v-bind:title="term.description | localize-string">{{ term.title | localize-string }}</label>
        </li>
      </div>

      </template>

      <template v-else-if="criterion.type === 'NUMERIC'">

      <div class="container" v-if="!termQueryIsReadOnly">
        <div class="form-group">
          <label v-bind:for="vocabulary.name + 'from'">{{ "search.from" | translate }}</label>
          <input type="number" class="form-control" v-bind:id="vocabulary.name + '-from'" v-model="criterion.value[0]" v-on:change="onInput()">
        </div>
        <div class="form-group">
          <label v-bind:for="vocabulary.name + 'to'">{{ "search.to" | translate }}</label>
          <input type="number" class="form-control" v-bind:id="vocabulary.name + '-to'" v-model="criterion.value[1]" v-on:change="onInput()">
        </div>
      </div>
      <div class="container" v-else>
        <div>
          <label v-bind:for="vocabulary.name + 'from'">{{ "search.from" | translate }}</label> {{criterion.value[0]}}
        </div>

        <div>
          <label v-bind:for="vocabulary.name + 'to'">{{ "search.to" | translate }}</label> {{criterion.value[1]}}
        </div>
      </div>

      </template>

      <template v-else>

      <div class="container" v-if="!termQueryIsReadOnly">
        <input type="text" class="form-control" v-model="criterion.value" v-on:change="onInput()">
      </div>
      <div class="container" v-else>
        {{criterion.value}}
      </div>

      </template>
    </div>

    <button type="button" class="btn btn-secondary btn-sm" v-if="!termQueryIsReadOnly" v-on:click="onRemove()"><span aria-hidden="true">&times;</span></button>
  </div>
  `,
  name: "rql-query",
  props: {
    vocabulary: {
      type: Object,
      required: true
    },
    query: Object
  },
  data() {
    return {
      termsFilter: ""
    };
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
    termQueryIsReadOnly() { // for Mica_study.className for example
      let uiTermsReadOnlyVocabularyAttributes = (this.vocabulary || {attributes: [{"key": "uiTermsReadOnly", "value": "false"}]}).attributes.find(attr => attr.key === 'uiTermsReadOnly');
      return uiTermsReadOnlyVocabularyAttributes && uiTermsReadOnlyVocabularyAttributes.value === "true";
    },
    terms() {
      const localizeStringFunction = Vue.filter("localize-string") || ((val) => val[0].text);

      return (this.vocabulary.terms || []).filter(term => {
        return (!this.termsFilter || this.termsFilter.trim().length === 0) || localizeStringFunction(term.title).toLowerCase().indexOf(this.termsFilter.toLowerCase()) > -1;
      });
    },
    checkedTerms() {
      return this.terms.filter(t => this.criterion.value.indexOf(t.name) > -1);
    },
    uncheckedTerms() {
      return this.terms.filter(t => this.criterion.value.indexOf(t.name) === -1);
    }
  },
  methods: {
    getCriterionAsString() {
      const localizeStringFunction = Vue.filter("localize-string") || ((val) => val[0].text);
      return this.criterion.asString(localizeStringFunction);
    },
    onInput() {
      this.$emit("update-query", this.criterion);
    },
    onRemove() {
      this.$emit("remove-query", this.criterion);
    }
  },
  mounted() {
    const menuReference = this.$refs.menu;
    if (menuReference) {
      menuReference.addEventListener("click", event => event.stopPropagation());
    }
  }
};

const RqlNode = {
  template: `
  <div class="d-flex flex-wrap">
    <template v-if="isNode(firstArg)">
    <rql-node v-bind:name="firstArg.name" v-bind:args="firstArg.args" v-bind:taxonomy="taxonomy" v-on:update-node="onUpdateNode($event)" v-bind:advanced-mode="advancedMode" v-on:update-query="updateQuery($event, firstArg.taxonomyName)" v-on:remove-query="removeQuery($event, firstArg.taxonomyName)"></rql-node>
    </template>

    <template v-else>
    <rql-query v-if="firstArg && firstArg.vocabulary && firstArgIsShown" v-bind:vocabulary="firstArg.vocabulary" v-bind:query="firstArg.associatedQuery" v-on:update-query="updateQuery($event, firstArg.taxonomyName)" v-on:remove-query="removeQuery($event, firstArg.taxonomyName)"></rql-query>
    </template>

    <span v-if="advancedMode && otherArgs.length > 0 && firstArgIsShown" class="d-flex my-auto">
      <div class="dropdown">
        <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">{{ "search." + name | translate }}</button>

        <div class="dropdown-menu">
          <button class="dropdown-item" type="button" v-if="name !== 'and'" v-on:click="updateNodeName('and')">{{ "search.and" | translate }}</button>
          <button class="dropdown-item" type="button" v-if="name !== 'or'" v-on:click="updateNodeName('or')">{{ "search.or" | translate }}</button>
        </div>
      </div>
    </span>

    <span v-for="(arg, index) in otherArgs" v-bind:key="index" class="d-flex">
      <template v-if="isNode(arg)">
      <rql-node v-bind:name="arg.name" v-bind:args="arg.args" v-bind:taxonomy="taxonomy" v-on:update-node="onUpdateNode($event)" v-bind:advanced-mode="advancedMode" v-on:update-query="updateQuery($event, arg.taxonomyName)" v-on:remove-query="removeQuery($event, arg.taxonomyName)"></rql-node>
      </template>

      <template v-else>
      <rql-query v-if="arg && arg.vocabulary" v-bind:vocabulary="arg.vocabulary" v-bind:query="arg.associatedQuery" v-on:update-query="updateQuery($event, arg.taxonomyName)" v-on:remove-query="removeQuery($event, arg.taxonomyName)"></rql-query>
      </template>
    </span>
  </div>
  `,
  name: "rql-node",
  props: {
    advancedMode: {
      type: Boolean,
      default: false
    },
    name: String,
    args: Array,
    taxonomy: [Object, Array]
  },
  computed: {
    firstArg() {
      return this.getFirstArg();
    },
    firstArgIsShown() {
      let firstArg = this.getFirstArg();
      if (!this.isNode(firstArg)) {
        let uiHideInBuilderVocabularyAttributes = (firstArg.vocabulary || {attributes: [{"key": "uiHideInBuilder", "value": "false"}]}).attributes.find(attr => attr.key === 'uiHideInBuilder');
        return !uiHideInBuilderVocabularyAttributes || uiHideInBuilderVocabularyAttributes.value === "false";
      } else {
        let splitFirstArgToQueries = Criterion.splitQuery(firstArg);
        if (splitFirstArgToQueries.length === 1) {
          let loneQuery = this.asInput(splitFirstArgToQueries[0]);
          let uiHideInBuilderVocabularyAttributes = (loneQuery.vocabulary || {attributes: [{"key": "uiHideInBuilder", "value": "false"}]}).attributes.find(attr => attr.key === 'uiHideInBuilder');
          return !uiHideInBuilderVocabularyAttributes || uiHideInBuilderVocabularyAttributes.value === "false";
        }
        return true;
      }
    },
    otherArgs() {
      if (this.isNode()) {
        const others = this.args.slice(1);
        return others.map(other => {
          if (this.isNode(other)) {
            return this.other;
          } else {
            return this.asInput(other);
          }
        });
      } else {
        return [];
      }
    }
  },
  components: {
    RqlQuery
  },
  methods: {
    getFirstArg() {
      let result = null;

      if (this.isNode()) {
        const arg = this.args.slice(0, 1)[0];
        result = this.isNode(arg) ? arg : this.asInput(arg);
      } else {
        const query = new RQL.Query(this.name);
        query.args = this.args;
        result = this.asInput(query);
      }

      return result;
    },
    isNode(arg) {
      return Criterion.NODE_NAMES.indexOf((arg || this).name) > -1;
    },
    asInput(arg) {
      const vocabulary = Criterion.associatedVocabulary(this.taxonomy, arg);
      if (vocabulary) {
        const taxonomyName = Criterion.associatedTaxonomyName(this.taxonomy, vocabulary);
        return {name: vocabulary.name, taxonomyName, vocabulary, associatedQuery: arg};
      }

      return arg;
    },
    inputIsShown(input) {
      let uiHideInBuilderVocabularyAttributes = (input.vocabulary || {attributes: [{"key": "uiHideInBuilder", "value": "false"}]}).attributes.find(attr => attr.key === 'uiHideInBuilder');
      return !uiHideInBuilderVocabularyAttributes || uiHideInBuilderVocabularyAttributes.value === "false";
    },
    updateQuery(payload, taxonomyName) {
      this.$emit("update-query", { data: (payload.data || payload), taxonomyName: (payload.taxonomyName || taxonomyName) });
    },
    removeQuery(payload, taxonomyName) {
      this.$emit("remove-query", { data: (payload.data || payload), taxonomyName: (payload.taxonomyName || taxonomyName) });
    },
    onUpdateNode(payload) {
      this.$emit("update-node", payload);
    },
    updateNodeName(nodeName) {
      const query = new RQL.Query(this.name);
      query.args = this.args;

      this.$emit("update-node", {newName: nodeName, query});
    }
  }
};

const RqlQueryBuilder = {
  template: `
  <div v-bind:class="target" class="d-flex">
    <span v-if="showTarget" class="my-auto text-muted" v-show="items.length > 0">
      <h4 class="mb-0"><i class="align-middle io" v-bind:class="targetIcon"></i></h4>
    </span>

    <rql-node v-if="showTarget" v-for="(arg, index) in query.args" v-bind:key="index" v-bind:name="arg.name" v-bind:args="arg.args" v-bind:taxonomy="taxonomy" v-bind:advanced-mode="advancedMode" v-on:update-node="updateNode($event)" v-on:update-query="updateNodeQuery($event)" v-on:remove-query="removeNodeQuery($event)"></rql-node>
  </div>
  `,
  name: "rql-query-builder",
  props: {
    advancedMode: {
      type: Boolean,
      default: false
    },
    target: {
      type: String,
      required: true
    },
    query: Object,
    taxonomy: [Object, Array]
  },
  computed: {
    showTarget() {
      let itemsThatCanBeShown = this.items.filter(item => {
        let uiHideInBuilderVocabularyAttributes = (item.vocabulary || {attributes: [{"key": "uiHideInBuilder", "value": "false"}]}).attributes.find(attr => attr.key === 'uiHideInBuilder');
        return !uiHideInBuilderVocabularyAttributes || uiHideInBuilderVocabularyAttributes.value === "false";
      });
      return itemsThatCanBeShown.length > 0;
    },
    inputs() {
      return Criterion.splitQuery(this.query);
    },
    items() {
      if (!this.taxonomy || !this.query) return [];

      let result = [];

      if (Array.isArray(this.taxonomy)) {
        this.taxonomy.forEach((t) => {
          let found = (t.vocabularies || []).filter(vocabulary => this.hasAssociatedQuery(vocabulary));
          result = result.concat(found);
        });
      } else {
        result = (this.taxonomy.vocabularies || []).filter(vocabulary => this.hasAssociatedQuery(vocabulary));
      }

      return result.map(vocabulary => {
        let associatedQuery = this.getAssociatedQuery(vocabulary);
        return {name: vocabulary.name, taxonomyName: this.getAssociatedTaxonomyName(vocabulary), vocabulary, associatedQuery};
      });
    },
    targetIcon() {
      return "io-" + this.target;
    }
  },
  components: {
    RqlNode
  },
  methods: {
    getAssociatedTaxonomyName(test) {
      return Criterion.associatedTaxonomyName(this.taxonomy, test);
    },
    getAssociatedQuery(vocabulary) {
      if (this.query) {
        return Criterion.associatedQuery(vocabulary, this.inputs);
      }

      return undefined;
    },
    hasAssociatedQuery(vocabulary) {
      return this.getAssociatedQuery(vocabulary) && true;
    },
    updateQuery(payload, taxonomyName) {
      const query = payload.asQuery(taxonomyName);

      if ((["missing", "exists"].indexOf(payload.operator) === -1 && payload.value.length === 0) || (payload.type === "NUMERIC" && query.args[1].length === 0)) {
        this.$emit("remove-query", {target: this.target, query});
      } else {
        this.$emit("update-query", {target: this.target, query});
      }
    },
    removeQuery(payload, taxonomyName) {
      this.$emit("remove-query", {target: this.target, query: payload.asQuery(taxonomyName)});
    },
    updateNodeQuery(payload) {
      this.updateQuery(payload.data, payload.taxonomyName);
    },
    removeNodeQuery(payload) {
      this.removeQuery(payload.data, payload.taxonomyName);
    },
    updateNode(payload) {
      this.$emit("update-node", {query: payload.query, newName: payload.newName, target: this.target});
    }
  }
};

Vue.component(RqlQueryBuilder.name, RqlQueryBuilder);
