'use strict';

// Taxonomy sidebar menu
Vue.component('taxonomy-menu', {
  props: ['taxonomy'],
  template: '<li class="nav-item"><a href="#" class="nav-link" :title="taxonomy.description[0].text" v-on:click="$emit(taxonomy.name)"><i class="far fa-circle nav-icon"></i><p>{{ taxonomy.title[0].text }}</p></a></li>'
});

new Vue({
  el: '#search-criteria',
  //el: '#app',
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
      },
      taxonomies: {},
      initialized: false
    };
  },
  mounted() {
    axios
      .get('../ws/taxonomy/Mica_taxonomy/_filter?target=taxonomy')
      .then(response => {
        //console.dir(response.data.vocabularies);
        let vocabularies = response.data.vocabularies;
        for (let group of vocabularies) {
          //this.criteriaMenu.order.push(group.name);
          this.criteriaMenu.items[group.name].title = group.title[0].text;
          switch(group.name) {
            case 'variable':
              // TODO handle multi level
              this.criteriaMenu.items.variable.menus = group.terms[0].terms;
              break;
            case 'dataset':
            case 'study':
            case 'network':
              this.criteriaMenu.items[group.name].menus = group.terms;
              break;
          }
          if (this.criteriaMenu.items[group.name].menus && this.criteriaMenu.items[group.name].menus.length>0) {
            this.criteriaMenu.order.push(group.name);
          }
        }
        // then load the taxonomies
        for (let name of this.criteriaMenu.order) {
          axios
            .get('../ws/taxonomies/_filter?target=' + name)
            .then(response => {
              for (let taxo of response.data) {
                this.taxonomies[taxo.name] = taxo;
              }
            });
        }
        this.initialized = true;
      });
  }
});
