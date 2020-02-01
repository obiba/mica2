'use strict';

// an EventBus is a Vue app without element
// its data are callback functions, registered by event name
const EventBus = new Vue({
  data: {
      callbacks: {}
  },
  methods: {
    register: function(eventName, callback) {
      if (!this.callbacks[eventName]) {
        this.callbacks[eventName] = []
        this.$on(eventName, function(payload) {
          for (let callback of this.callbacks[eventName]) {
            callback(payload)
          }
        });
      }
      this.callbacks[eventName].push(callback);
      //console.dir(this.callbacks)
    },
    unregister: function(eventName) {
      this.callbacks[eventName] = undefined;
    }
  }
});

//
// Search criteria Vue
// sidebar menus for taxonomy selection. Terms selection is delegated to the main app (query builder)
//

// Taxonomy sidebar menu
Vue.component('taxonomy-menu', {
  props: ['taxonomy'],
  template: '<li class="nav-item"><a href="#" class="nav-link" :title="taxonomy.description[0].text" @click.prevent="$emit(\'taxonomy-selection\', taxonomy.name)"><i class="far fa-circle nav-icon"></i><p>{{ taxonomy.title[0].text }}</p></a></li>'
});

new Vue({
  el: '#search-criteria',
  data() {
    return {
      criteriaMenu: {
        items: {
          variable: {
            icon: 'ion ion-pie-graph',
            title: 'Variables',
            menus: []
          },
          dataset: {
            icon: 'ion ion-grid',
            title: 'Datasets',
            menus: []
          },
          study: {
            icon: 'ion ion-folder',
            title: 'Studies',
            menus: []
          },
          network: {
            icon: 'ion ion-filing',
            title: 'Networks',
            menus: []
          },
        },
        order: []
      }
    };
  },
  methods: {
    // make the menu
    onMicaTaxonomies: function(payload) {
      for (let target of payload) {
        this.criteriaMenu.items[target.name].title = target.title[0].text;
        switch(target.name) {
          case 'variable':
            // TODO handle multi level
            this.criteriaMenu.items.variable.menus = target.terms[0].terms;
            break;
          case 'dataset':
          case 'study':
          case 'network':
            this.criteriaMenu.items[target.name].menus = target.terms;
            break;
        }
        if (this.criteriaMenu.items[target.name].menus && this.criteriaMenu.items[target.name].menus.length>0) {
          this.criteriaMenu.order.push(target.name);
        }
      }
    },
    // forward taxonomy selection
    onTaxonomySelection: function(payload) {
      console.dir(payload);
      EventBus.$emit('taxonomy-selection', payload);
    }
  },
  mounted() {
    EventBus.register('mica-taxonomy', this.onMicaTaxonomies);
  }
});

//
// Results Vue
// results display app, with some filtering criteria selection, and requests for query execution
//

$('#variables-tab').click(function(){
  EventBus.$emit('query-type-selection', 'variables-list');
});

$('#datasets-tab').click(function(){
  EventBus.$emit('query-type-selection', 'datasets-list');
});

$('#studies-tab').click(function(){
  EventBus.$emit('query-type-selection', 'studies-list');
});

$('#networks-tab').click(function(){
  EventBus.$emit('query-type-selection', 'networks-list');
});

$('#lists-tab').click(function(){
  EventBus.$emit('query-type-selection', 'lists');
});

$('#coverage-tab').click(function(){
  EventBus.$emit('query-type-selection', 'coverage');
});

$('#graphics-tab').click(function(){
  EventBus.$emit('query-type-selection', 'graphics');
});

new Vue({
  el: '#list-variables',
  data() {
    return {
      result: null
    }
  },
  methods: {
    onResult: function(payload) {
      this.result = payload;
    }
  },
  mounted() {
    EventBus.register('variables-list', this.onResult);
  }
});

new Vue({
  el: '#list-datasets',
  data() {
    return {
      result: null
    }
  },
  methods: {
    onResult: function(payload) {
      this.result = payload;
    }
  },
  mounted() {
    EventBus.register('datasets-list', this.onResult);
  }
});

new Vue({
  el: '#list-studies',
  data() {
    return {
      result: null
    }
  },
  methods: {
    onResult: function(payload) {
      this.result = payload;
    }
  },
  mounted() {
    EventBus.register('studies-list', this.onResult);
  }
});

new Vue({
  el: '#list-networks',
  data() {
    return {
      result: null
    }
  },
  methods: {
    onResult: function(payload) {
      this.result = payload;
    }
  },
  mounted() {
    EventBus.register('networks-list', this.onResult);
  }
});

new Vue({
  el: '#coverage',
  data() {
    return {
      result: null
    }
  },
  methods: {
    onResult: function(payload) {
      this.result = payload;
    }
  },
  mounted() {
    EventBus.register('coverage', this.onResult);
  }
});

new Vue({
  el: '#graphics',
  data() {
    return {
      result: null
    }
  },
  methods: {
    onResult: function(payload) {
      this.result = payload;
    }
  },
  mounted() {
    EventBus.register('graphics', this.onResult);
  }
});

//
// Querybuilder Vue
// main app that orchestrates the query display, criteria selection, query execution and dispatch of the results
//

new Vue({
  el: '#query-builder',
  data() {
    return {
      taxonomies: {},
      message: '',
      queryType: 'variables-list',
      lastList: ''
    }
  },
  methods: {
    // show a modal with all the vocabularies/terms of the selected taxonomy
    // initialized by the query terms and update/trigger the query on close
    onTaxonomySelection: function(payload) {
      this.message = '[' + payload + '] ' + this.taxonomies[payload].title[0].text + ': ';
      this.message = this.message + this.taxonomies[payload].vocabularies.map(voc => voc.title[0].text).join(', ');
    },
    // set the type of query to be executed, on result component selection
    onQueryTypeSelection: function(payload) {
      if (payload === 'lists') {
        this.queryType = this.lastList;  
      } else {
        this.queryType = payload;
        if (payload.endsWith('-list')) {
          this.lastList = payload;
        }
      }
    },
    onExecuteQuery: function() {
      console.log('Executing ' + this.queryType + ' query ...');
      EventBus.$emit(this.queryType, 'I am the result of a ' + this.queryType + ' query')
    }
  },
  mounted() {
    EventBus.register('taxonomy-selection', this.onTaxonomySelection);
    EventBus.register('query-type-selection', this.onQueryTypeSelection);
    // fetch the configured search criteria, in the form of a taxonomy of taxonomies
    axios
      .get('../ws/taxonomy/Mica_taxonomy/_filter?target=taxonomy')
      .then(response => {
        let targets = response.data.vocabularies;
        EventBus.$emit('mica-taxonomy', targets);

        for (let target of targets) {
        // then load the taxonomies
        axios
          .get('../ws/taxonomies/_filter?target=' + target.name)
          .then(response => {
            for (let taxo of response.data) {
              this.taxonomies[taxo.name] = taxo;
            }
          });
        }
      });
  }
});
