class Criterion {
  vocabulary = undefined;
  type = undefined;

  value = undefined;
  _operator = undefined;

  static __isTermsQuery(vocabulary) {
    return Array.isArray(vocabulary.terms) && vocabulary.terms.length > 0;
  }

  static __isNumericQuery(vocabulary) {
    return !Array.isArray(vocabulary.terms) && (vocabulary.attributes || []).filter(attribute => attribute.key === "type" && ["integer", "decimal"].indexOf(attribute.value) > -1).length === 1;
  }

  static __isMatchQuery(vocabulary) {
    return !Array.isArray(vocabulary.terms) && (vocabulary.attributes || []).filter(attribute => (attribute.key === "localized" && "true" === attribute.value) || (attribute.key === "type" && attribute.value === "string")).length > 0;
  }

  __findTerm(vocabulary, termName) {
    const found = (vocabulary.terms || []).filter(term => term.name === termName);
    return found.length > 0 ? found[0] : undefined;
  }

  __stringIsNullOrEmpty(str) {
    if (str === null || str === undefined) return true;
    if (typeof str === "string") return str.trim().length === 0;
    else return false;
  }

  static NODE_NAMES = ["and", "or"];

  static typeOfVocabulary(vocabulary) {
    let type = undefined;

    if (Criterion.__isTermsQuery(vocabulary)) {
      type = "TERMS";
    } else if (Criterion.__isNumericQuery(vocabulary)) {
      type = "NUMERIC";
    } else if(Criterion.__isMatchQuery(vocabulary)) {
      type = "MATCH";
    }

    return type;
  }

  static associatedQuery(vocabulary, inputs) {
    let type = Criterion.typeOfVocabulary(vocabulary);

    return (inputs || []).filter(input => {
      let found = false;

      switch(type) {
        case "TERMS":
        case "NUMERIC":
          found = (input.args[0] || "").split(/\./)[1] === vocabulary.name;

          break;
        case "MATCH":
          found = (input.operator === "match" || input.name === "match") && (input.args[1] || "").split(/\./)[1] === vocabulary.name;

          break;
      }

      return found;
    })[0];
  }

  static associatedTaxonomyName(taxonomy, testVocabulary) {
    if (Array.isArray(taxonomy)) {
      let result = taxonomy.filter((t) => {
        let found = (t.vocabularies || []).filter((vocabulary) => {
          return vocabulary.name === testVocabulary.name;
        });

        return found.length > 0;
      })[0];

      return result ? result.name : undefined;
    } else {
      return taxonomy.name;
    }
  }

  static associatedVocabulary(taxonomy, input) {
    if (Array.isArray(taxonomy)) {
      let result = undefined;

      taxonomy.forEach(t => {
        let found = (t.vocabularies || []).filter((vocabulary) => Criterion.associatedQuery(vocabulary, [input]))[0];
        if (found) {
          result = found;
        }
      });

      return result;
    } else {
      return (taxonomy.vocabularies || []).filter(vocabulary => Criterion.associatedQuery(vocabulary, [input]))[0];
    }
  }

  static splitQuery(query) {
    let output = [];

    if (query) {
      query.walk((name, args) => output.push({operator: name, args}));
    }

    return output;
  }

  constructor(vocabulary) {
    this.vocabulary = vocabulary;

    if (Criterion.__isTermsQuery(this.vocabulary)) {
      this._operator = "in";
      this.value = [];
    } else if (Criterion.__isNumericQuery(this.vocabulary)) {
      this._operator = "between";
      this.value = [];
    } else {
      this._operator = "match";
      this.value = "";
    }

    this.type = Criterion.typeOfVocabulary(vocabulary);
  }

  get operator() {
    return this._operator;
  }

  set operator(value) {
    this._operator = value;

    switch(this.type) {
      case "TERMS":
        if (["missing", "exists"].indexOf(this.operator) > -1) {
          this.value = [...this.terms];
        }

        break;
      case "NUMERIC":
        if (["missing", "exists"].indexOf(this.operator) > -1) {
          this.value = [];
        }

        break;
      default:
        if (["missing", "exists"].indexOf(this.operator) > -1) {
          this.value = "*";
        }

        break;
    }
  }

  get terms() {
    if (this.type === "TERMS") {
      return (this.vocabulary.terms || []).map(term => term.name);
    }

    return null;
  }

  set query(input) {
    this._operator = input.operator;
    if (!this._operator) this.operator = input.name;

    switch(this.type) {
      case "TERMS":
        if (["missing", "exists"].indexOf(this.operator) > -1) {
          this.value = [...this.terms];
        } else {
          this.value = Array.isArray(input.args[1]) ? input.args[1] : [input.args[1]];
        }

        break;
      case "NUMERIC":
        if (this.operator === "ge") {
          this.value = [input.args[1]];
        } else if (this.operator === "le") {
          this.value = ["", input.args[1]];
        } else if ((input.operator || input.name) === "between") {
          this.value = [input.args[1][0], input.args[1][1]];
        }

        break;
      default:
        this.value = input.args[0];

        break;
    }
  }

  asQuery(taxonomy) {
    let query = new RQL.Query(this.operator);

    switch(this.type) {
      case "TERMS":
        query.push(`${taxonomy}.${this.vocabulary.name}`);

        if (this.terms > 1 && this.terms.length === this.value.length) {
          query.name = ["missing", "exists"].indexOf(this.operator) > -1 ? this.operator : "exists";
        } else {
          query.name = ["in", "out"].indexOf(this.operator) > -1 ? this.operator : "in";
        }

        if (["missing", "exists"].indexOf(query.name) === -1 || this.terms.length < this.value.length) {
          query.push(this.value);
        }

        break;
      case "NUMERIC":
        query.push(`${taxonomy}.${this.vocabulary.name}`);

        if (["missing", "exists"].indexOf(this.operator) > -1) {
          this.value = [];
        } else {
          if (this.__stringIsNullOrEmpty(this.value[0]) && this.__stringIsNullOrEmpty(this.value[1])) {
            this.value = [];
          } else if (!this.__stringIsNullOrEmpty(this.value[0]) && this.__stringIsNullOrEmpty(this.value[1])) {
            query.name = "ge";
            this.value = parseInt(this.value[0]);
          } else if (this.__stringIsNullOrEmpty(this.value[0]) && !this.__stringIsNullOrEmpty(this.value[1])) {
            query.name = "le";
            this.value = parseInt(this.value[1]);
          } else if (!this.__stringIsNullOrEmpty(this.value[0]) && !this.__stringIsNullOrEmpty(this.value[1])) {
            query.name = "between";
            this.value = this.value.map(val => parseInt(val));
          }
        }

        query.push(this.value);

        break;
      default:
        if (!this.__stringIsNullOrEmpty(this.value)) {
          query.push(this.value);
        } else {
          query.push("");
        }

        query.push(`${taxonomy}.${this.vocabulary.name}`);

        break;
    }

    return query;
  }

  asString(localizeStringFunction) {
    if (["missing", "exists"].indexOf(this.operator) > -1) {
      const text = this.operator === "missing" ? "none" : "any";
      return `${localizeStringFunction(this.vocabulary.title)}:${text}`;
    }

    if (this.type === "TERMS") {
      if ((this.value || []).length > 5) return `${localizeStringFunction(this.vocabulary.title)}:...`;

      const text = (this.value || []).map(val => {
        const term = this.__findTerm(this.vocabulary, val);
        return term ? localizeStringFunction(term.title) : val;
      }).join(" | ");

      return `${text}`;
    } else if (this.type === "NUMERIC") {
      let text = ""

      if (!this.__stringIsNullOrEmpty(this.value[0]) && this.__stringIsNullOrEmpty(this.value[1])) {
        text = `:>${this.value[0]}`;
      } else if (this.__stringIsNullOrEmpty(this.value[0]) && !this.__stringIsNullOrEmpty(this.value[1])) {
        text = `:<${this.value[1]}`;
      } else if (!this.__stringIsNullOrEmpty(this.value[0]) && !this.__stringIsNullOrEmpty(this.value[1])) {
        text = `:[${this.value[0]},${this.value[1]}]`;
      }

      return `${localizeStringFunction(this.vocabulary.title)}${text}`;
    } else {
      let text = "";
      if (!this.__stringIsNullOrEmpty(this.value)) {
        text = `:match(${this.value})`;
      }
      return `${localizeStringFunction(this.vocabulary.title)}${text}`;
    }
  }
}
