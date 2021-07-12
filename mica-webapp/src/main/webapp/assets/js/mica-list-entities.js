class StringLocalizer {
  static __localizeInternal(entries, locale) {
    const result = (Array.isArray(entries) ? entries : [entries]).filter((entry) => entry && (locale === entry.lang || locale === entry.locale)).pop();

    if (result) {
      let value = result.value ? result.value : result.text;
      return value ? value : null;
    }
    return null;
  }

  static localize(entries) {
    if (entries) {
      const result = StringLocalizer.__localizeInternal(entries, Mica.locale)
        || StringLocalizer.__localizeInternal(entries, Mica.defaultLocale)
        || StringLocalizer.__localizeInternal(entries, 'und');

      return result ? result : '';
    } else {
      return '';
    }
  }
}

// Register all filters

Vue.filter("ellipsis", (input, n, link) => {
  if (input.length <= n) { return input; }
  const subString = input.substr(0, n-1); // the original check
  const anchor = link ? ` <a href="${link}">...</a>` : " ...";
  return subString.substr(0, subString.lastIndexOf(" ")) + anchor;
});

Vue.filter("readmore", (input, link, text) => {
  return `${input} <a href="${link}" class="clearfix btn-link">${text}</a>`;
});

Vue.filter("concat", (input, suffix) => {
  return input + suffix;
});

Vue.filter("localize-string", (input) => {
  if (typeof input === "string") return input;
  return StringLocalizer.localize(input);
});

Vue.filter("markdown", (input) => {
  return marked(input);
});

Vue.filter("localize-number", (input) => {
  return (input || 0).toLocaleString();
});

Vue.filter("translate", (key) => {
  let value = Mica.tr[key];
  return typeof value === "string" ? value : key;
});

const DEFAULT_PAGE_SIZES = [10,20,50,100];
const DEFAULT_PAGE_SIZE = DEFAULT_PAGE_SIZES[2];

/**
 * Base class for all entities (Srtudies, Networks, Datasets)
 */
class ObibaEntitiesService {

  __getResource(url, onsuccess, onfailure) {
    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        if (onsuccess) {
          onsuccess(response.data);
        }
      })
      .catch(response => {
        if (onfailure) {
          // onfailure(response);
          console.error(`Failed to retrieve ${studyId} networks: ${response}`);
        }
      });
  }

  __parse() {
    const search = window.location.search;
    let urlParts = {}

    if (search) {
      const index = search.indexOf('?')
      if (index > -1) {
        const params = search.substring(index + 1).split('&');
        params.forEach(param => {
          const parts = param.split('=');
          urlParts[parts[0]] = parts[1] || "";
        });
      }
    }

    return urlParts;
  }

  prepareQuery(locale, from, size) {
    const urlParts = this.__parse();
    let tree = new RQL.QueryTree(RQL.Parser.parseQuery(urlParts.query), {containers: ['variable', 'dataset', 'study', 'network']});
    let targetQuery = tree.search((name) => name === this.target);
    if (!targetQuery)  {
      targetQuery = new RQL.Query(this.target);
      tree.addQuery(null, targetQuery);
    }

    let limitQuery = tree.search((name,args,parent) => name === 'limit' && targetQuery.name === parent.name);
    if (!limitQuery) {
      tree.addQuery(targetQuery, new RQL.Query('limit', [0, DEFAULT_PAGE_SIZE]));
    } else if (from !== undefined) {
      limitQuery.args[0] = from;
      limitQuery.args[1] = size;
    }

    tree.addQuery(targetQuery, new RQL.Query('fields', this.fields));

    let sortQuery = tree.search((name,args,parent) => name === 'sort' && targetQuery.name === parent.name);
    if (!sortQuery) {
      tree.addQuery(targetQuery, new RQL.Query('sort', ['name']));
    }

    tree.addQuery(null, new RQL.Query('locale', [locale]));

    return tree;
  }

  updateLocation(tree, replace) {
    tree.findAndDeleteQuery((name) => 'fields' === name);
    tree.findAndDeleteQuery((name) => 'locale' === name);
    const query = tree.serialize();

    if (replace) {
      window.history.replaceState(null, "", `?query=${query}`);
    } else {
      window.history.pushState(null, "", `?query=${query}`);
    }
  }

  getLimitQueryValues(tree) {
    let limits = {from: 0, size: DEFAULT_PAGE_SIZE};
    const limitQuery = tree.search((name,args,parent) => name === 'limit' && parent.name === this.target);
    if (limitQuery && limitQuery.args) {
      limits.from = limitQuery.args[0];
      limits.size = limitQuery.args[1];
    }

    return limits;
  }

  getFilterQueryValue(tree) {
    const filterQuery = tree.search((name, args, parent) => name === 'filter' && args[0].name === 'match' && parent.name === this.target);
    if (filterQuery && filterQuery.args) {
      return filterQuery.args[0].args[0];
    } else {
      return '';
    }
  }

  updateFilter(tree, text) {
    if (text && text.length > 1) {
      let targetQuery = tree.search((name) => name === this.target);
      if (!targetQuery)  {
        targetQuery = new RQL.Query(this.target);
        tree.addQuery(null, targetQuery);
      }

      const filterQuery = tree.search((name, args, parent) => name === 'filter' && args[0].name === 'match' && parent.name === this.target);
      if (!filterQuery) {
        tree.addQuery(targetQuery, new RQL.Query('filter', [new RQL.Query('match', [text])]));
      } else {
        filterQuery.args[0].args[0] = text
      }
    } else {
      tree.findAndDeleteQuery((name) => 'filter' === name);
    }
  }

  getEntities(query, onsuccess, onfailure) {
    let url = `/ws/${this.resourcePath}/_rql?query=${query}`;
    this.__getResource(url, onsuccess, onfailure);
  }

  getSuggestions(text, locale, onsuccess, onfailure) {
    let url = `/ws/${this.resourcePath}/_suggest?query=${text}&locale=${locale}`;
    this.__getResource(url, onsuccess, onfailure);
  }

  updateSort(tree, sort) {
    let targetQuery = tree.search((name) => name === this.target);
    if (!targetQuery)  {
      targetQuery = new RQL.Query(this.target);
      tree.addQuery(null, targetQuery);
    }

    let sortQuery = tree.search((name, args, parent) => name === 'sort' && targetQuery.name === parent.name);
    if (!sortQuery) {
      tree.addQuery(targetQuery, new RQL.Query('sort', [sort]));
    } else {
      sortQuery.args[0] = sort;
    }
  }
}

/**
 * Datasets servive class
 */
class ObibaDatasetsService extends ObibaEntitiesService {

  static newInstance(type) {
    return new ObibaDatasetsService(type);
  }

  constructor(type) {
    super();
    this.type = type;
  }

  get target() {
    return 'dataset';
  }

  get resourcePath() {
    return 'datasets';
  }


  get fields() {
    return ['acronym.*','name.*','description.*','variableType','studyTable.studyId','studyTable.project','studyTable.table','studyTable.populationId','studyTable.dataCollectionEventId','harmonizationTable.studyId','harmonizationTable.project','harmonizationTable.table','harmonizationTable.populationId']
  }

  prepareQuery(locale, from, size) {
    let tree = super.prepareQuery(locale, from, size);

    let targetQuery = tree.search((name) => name === this.target);
    if (!targetQuery) {
      throw new Error(`Target query ${this.target} not found.`)
    }

    let classNameQuery;
    if (this.type === 'datasets') {
      classNameQuery = tree.search((name, agrs) => agrs.indexOf('Mica_dataset.className') > -1);
      if (classNameQuery) {
        tree.deleteQuery(classNameQuery);
      }
    } else {
      const classType = this.type === 'collected-datasets' ? 'StudyDataset' : 'HarmonizationDataset';

      // add className query
      classNameQuery = tree.search((name, args, parent) => args.indexOf('Mica_dataset.className') > -1 && parent.name === this.target);
      if (!classNameQuery) {
        classNameQuery = new RQL.Query('in');
        tree.addQuery(targetQuery, classNameQuery);
      }

      classNameQuery.name = 'in';
      classNameQuery.args = ['Mica_dataset.className', classType];
    }

    return tree;
  }
}

/**
 * Studies servive class
 */
class ObibaStudiesService extends ObibaEntitiesService {

  static newInstance(type) {
    return new ObibaStudiesService(type);
  }

  constructor(type) {
    super();
    this.type = type;
  }

  get target() {
    return 'study';
  }

  get resourcePath() {
    return 'studies';
  }

  get fields() {
    return ['acronym.*','name.*','objectives.*','logo','model.methods.design','model.numberOfParticipants.participant']
  }

  prepareQuery(locale, from, size) {
    let tree = super.prepareQuery(locale, from, size);

    let targetQuery = tree.search((name) => name === this.target);
    if (!targetQuery) {
      throw new Error(`Target query ${this.target} not found.`)
    }

    let classNameQuery;
    if (this.type === 'studies') {
      classNameQuery = tree.search((name, agrs) => agrs.indexOf('Mica_study.className') > -1);
      if (classNameQuery) {
        tree.deleteQuery(classNameQuery);
      }
    } else {
      const classType = this.type === 'individual-studies' ? 'Study' : 'HarmonizationStudy';

      // add className query
      classNameQuery = tree.search((name, args, parent) => args.indexOf('Mica_study.className') > -1 && parent.name === this.target);
      if (!classNameQuery) {
        classNameQuery = new RQL.Query('in');
        tree.addQuery(targetQuery, classNameQuery);
      }

      classNameQuery.name = 'in';
      classNameQuery.args = ['Mica_study.className', classType];
    }

    return tree;
  }

  getEntities(query, onsuccess, onfailure) {
    const savedCallback = onsuccess;
    const newOnsuccess = (response) => {
      let dto = response.studyResultDto;
      if (dto && dto['obiba.mica.StudyResultDto.result']) {
        dto['obiba.mica.StudyResultDto.result'].summaries.forEach((summary) => {
          if (summary.content) {
            summary.model = JSON.parse(summary.content);
          }
        })
      }

      if (savedCallback) {
        savedCallback(response);
      }
    };

    super.getEntities(query, newOnsuccess, onfailure);
  }
}

/**
 * Networks service class
 */
class ObibaNetworksService extends ObibaEntitiesService {

  static newInstance() {
    return new ObibaNetworksService();
  }

  get target() {
    return 'network';
  }

  get resourcePath() {
    return 'networks';
  }

  get fields() {
    return ['acronym.*','name.*','description.*','studyIds','logo']
  }
}

const BUTTON_PREVIOUS = 'button-previous';
const BUTTON_FIRST = 'button-first';
const BUTTON_LAST = 'button-last';
const BUTTON_NEXT = 'button-next';
const BUTTON_PAGE = 'button-page';
const BUTTON_ELLIPSIS_FIRST = 'button-ellipsis-first';
const BUTTON_ELLIPSIS_LAST = 'button-ellipsis-last';

/**
 * Simple widget used for selecting pagination page size.
 * Client code must have a '<select>' element with an ID that is passed to this widget for proper rendering.
 */
class OBiBaPageSizeSelector {
  constructor(elementId, pageSizes, pageSize, pageSizeChangeCB) {
    this.elementId = elementId;
    this.pageSizes = pageSizes || [10, 20, 50, 100];
    this.pageSize = pageSizes.indexOf(pageSize) > 0 ? pageSize : pageSizes[0];
    this.pageSizeChangeCB = pageSizeChangeCB;
    this.__createOptions();
  }

  __createOptions() {
    const parent = document.querySelector(`select#${this.elementId}`);
    parent.addEventListener('change', this.__onPageSizeChanged.bind(this));

    this.pageSizes.forEach(pageSize => {
      parent.insertAdjacentHTML('beforeend', `<option id="PAGE-SIZE-${pageSize}" value="${pageSize}">${pageSize}</option>`);
    });

    parent.value = this.pageSize;
  }

  __onPageSizeChanged(event) {
    if (this.pageSizeChangeCB) {
      this.pageSizeChangeCB({id: this.elementId, size: parseInt(event.target.value)});
    }
  }

  update(pageSize) {
    if (this.pageSizes.indexOf(pageSize) < 0) {
      throw new Error("Invalid pageSize");
    }

    const parent = document.querySelector(`select#${this.elementId}`);
    this.pageSize = pageSize;
    parent.value = this.pageSize;
  }
}

/**
 * Simple widget for content pagination.
 * Client code must have a '<nav id="obiba-pagination"><ul class="pagination"></ul></nav>' element with an ID that is
 * passed to this widget for proper rendering.
 */
class OBiBaPagination {

  constructor(elementId, useFixedFirstLast, onPageChangeCB) {
    this.elementId = elementId;
    this.page = 1;
    this.useFixedFirstLast = useFixedFirstLast;
    this.onPageChangeCB = onPageChangeCB ? onPageChangeCB : () => ({});
    this.numberOfPages = 0; // Fields initialized in OBiBaPagination::update()
    // TODO make 'numberOfButtons' configurable
    this.numberOfButtons = 3; // # of buttons showing the page number excluding (<<, <, ..., >, >>)
    this.pageButtons = [1,2,3];  // used to control the DEFAULT_BUTTONS
    this.hasFirstLastButtons = false;
  }

  __cleanup() {
    // Most browsers is supposed to remove EventListeners as well
    document.querySelectorAll(`#${this.elementId} ul [id^="button-"]`).forEach(button => {
      switch (button.id) {
        case BUTTON_FIRST:
          button.removeEventListener('click', this.__onFirstClick);
          break;
        case BUTTON_PREVIOUS:
          button.removeEventListener('click', this.__onPreviousClick);
          break;
        case BUTTON_NEXT:
          button.removeEventListener('click', this.__onFirstClick);
          break;
        case BUTTON_ELLIPSIS_FIRST:
        case BUTTON_ELLIPSIS_LAST:
          break;
        case BUTTON_LAST:
          button.removeEventListener('click', this.__onLastClick);
          break;
        default:
          button.removeEventListener('click', this.__onPageClick);
          break;
      }

      button.closest('li').remove();
    });
  }

  __initializeSelection(start, count) {
    return Array(count).fill().map((_, idx) => start + idx);
  }

  __updateSelection(start, count) {
    if (this.numberOfPages > this.numberOfButtons ) {
      this.pageButtons = this.__initializeSelection(start, count);
    }
  }

  __createButton(parent, id, data, label, clickHandler) {
    parent.insertAdjacentHTML(
      'beforeend',
      `<li class="page-item"><a id="${id}" class="page-link" href="javascript:void(0)">${label}</a></li>`
    );

    const button = document.querySelector(`#${this.elementId} #${id}`);

    if (data) {
      button.dataset.page = data;
    }

    if (clickHandler) {
      button.addEventListener('click', clickHandler);
    }
  }

  __addClass(id, clazz) {
    const button = document.querySelector(`#${this.elementId} #${id}`);
    button.closest('li').classList.add(clazz);
  }

  __removeClass(id, clazz) {
    const button = document.querySelector(`#${this.elementId} #${id}`);
    button.closest('li').classList.remove(clazz);
  }

  __enableButton(id, enable) {
    if (enable) {
      this.__removeClass(id, 'disabled');
    } else {
      this.__addClass(id, 'disabled')
    }
  }

  __visible(id, show) {
    if (show) {
      this.__removeClass(id,'d-none');
    } else {
      this.__addClass(id,'d-none');
    }
  }

  __deactivateButton(id) {
    this.__removeClass(id, 'active');
  }

  __activateButton(id) {
    this.__addClass(id, 'active');
  }

  __createButtons() {
    let parent = document.querySelector(`#${this.elementId} ul`);

    // Previous button
    this.__createButton(parent, BUTTON_PREVIOUS, null, this.useFixedFirstLast ? '&lsaquo;' : 'Previous', this.__onPreviousClick.bind(this));
    this.__enableButton(BUTTON_PREVIOUS, false);

    // create first Ellipsis button
    this.__createButton(parent, BUTTON_ELLIPSIS_FIRST, null, '...');
    this.__enableButton(BUTTON_ELLIPSIS_FIRST, false);
    this.__visible(BUTTON_ELLIPSIS_FIRST, false);

    // Page buttons
    this.pageButtons.forEach((p, index) => {
      this.__createButton(parent,  `${BUTTON_PAGE}-${p}`, p, `${p}`, this.__onPageClick.bind(this));
      if (index === 0) {
        this.__activateButton(`${BUTTON_PAGE}-${p}`);
      }
    });

    // create last Ellipsis button
    this.__createButton(parent, BUTTON_ELLIPSIS_LAST, null, '...');
    this.__enableButton(BUTTON_ELLIPSIS_LAST, false);
    this.__visible(BUTTON_ELLIPSIS_LAST, this.numberOfPages > this.numberOfButtons);

    // Next button
    this.__createButton(parent, BUTTON_NEXT, null, this.useFixedFirstLast ? '&rsaquo;' : 'Next', this.__onNextClick.bind(this));

    // First/Last
    if (this.useFixedFirstLast) {
      this.__createFixedFirstLastButtons();

    } else {
      this.__createFirstLastButtons();
    }
  }

  __createFixedFirstLastButtons() {
    let button = document.querySelector(`#${this.elementId} #${BUTTON_PREVIOUS}`);
    button.closest('li').insertAdjacentHTML(
      'beforebegin',
      `<li class="page-item"><a id="${BUTTON_FIRST}" class="page-link" href="javascript:void(0)">&laquo;</a></li>`
    );

    button = document.querySelector(`#${this.elementId} #${BUTTON_FIRST}`);
    button.dataset.page = 1;
    this.__visible(BUTTON_FIRST, true);
    button.addEventListener('click', this.__onFirstClick.bind(this));

    button = document.querySelector(`#${this.elementId} #${BUTTON_NEXT}`);
    button.closest('li').insertAdjacentHTML(
      'afterend',
      `<li class="page-item"><a id="${BUTTON_LAST}" class="page-link" href="javascript:void(0)">&raquo;</a></li>`
    );
    button = document.querySelector(`#${this.elementId} #${BUTTON_LAST}`);
    button.dataset.page = this.numberOfPages;
    this.__visible(BUTTON_LAST, true);
    button.addEventListener('click', this.__onLastClick.bind(this));
  }

  __createFirstLastButtons() {
    let button = document.querySelector(`#${this.elementId} #${BUTTON_PREVIOUS}`);
    button.closest('li').insertAdjacentHTML(
      'afterend',
      `<li class="page-item"><a id="${BUTTON_FIRST}" class="page-link" href="javascript:void(0)">1</a></li>`
    );

    button = document.querySelector(`#${this.elementId} #${BUTTON_FIRST}`);
    button.dataset.page = 1;
    this.__visible(BUTTON_FIRST, false);
    button.addEventListener('click', this.__onFirstClick.bind(this));

    button = document.querySelector(`#${this.elementId} #${BUTTON_NEXT}`);
    button.closest('li').insertAdjacentHTML(
      'beforebegin',
      `<li class="page-item"><a id="${BUTTON_LAST}" class="page-link" href="javascript:void(0)">${this.numberOfPages}</a></li>`
    );
    button = document.querySelector(`#${this.elementId} #${BUTTON_LAST}`);
    button.dataset.page = this.numberOfPages;
    this.__visible(BUTTON_LAST, false);
    button.addEventListener('click', this.__onLastClick.bind(this));
  }

  __updateVisibility() {
    const pagination = document.querySelector(`#${this.elementId}`);
    if (this.numberOfPages === 1) {
      pagination.classList.add('d-none');
    } else {
      pagination.classList.remove('d-none');
    }
  }

  __updateButtons() {
    const anchors = document.querySelectorAll(`#${this.elementId} ul [id^="${BUTTON_PAGE}"]`);
    anchors.forEach((a, index) => {
      this.__deactivateButton(a.id);
      a.dataset.page = this.pageButtons[index];
      a.dataset.page = this.pageButtons[index];
      a.innerHTML = a.dataset.page;
    });

    const selected = this.pageButtons.indexOf(this.page);
    this.__activateButton(anchors[selected].id);
  }

  __doPageChange(page) {
    const isFirstPage = page === 1;
    const isFirstSelection = this.pageButtons.indexOf(page) === 0;
    const isLastSelection = this.pageButtons.indexOf(page) === this.numberOfButtons - 1;
    const isLastPage = page === this.numberOfPages;
    this.page = page;

    this.__enableButton(BUTTON_PREVIOUS, this.numberOfPages  > 1);
    this.__enableButton(BUTTON_NEXT, this.numberOfPages  > 1);

    if (isFirstPage) {
      this.__enableButton(BUTTON_PREVIOUS,false);
      this.__updateSelection(1, this.numberOfButtons);
    } else if (isLastPage) {
      this.__enableButton(BUTTON_NEXT, false);
      this.__updateSelection(this.numberOfPages-2, this.numberOfPages);
    } else if (isLastSelection || isFirstSelection) {
      this.__updateSelection(page-1, this.numberOfButtons);
    }

    if (this.numberOfPages > this.numberOfButtons) {
      this.__visible(BUTTON_ELLIPSIS_FIRST, page > 2)
      this.__visible(BUTTON_ELLIPSIS_LAST, this.numberOfPages - page > 1)
    }

    this.__updateFirstLastButtons(page);
    this.__updateButtons();
  }

  __doPageChangeAndCallback(page) {
    this.__doPageChange(page);

    if (this.onPageChangeCB) {
      this.onPageChangeCB({
        id: this.elementId,
        size: this.pageSize,
        total: this.total,
        from: (this.page - 1) * this.pageSize,
        to: (this.page) * this.pageSize
      });
    }
  }

  __updateFirstLastButtons(page) {
    if (this.hasFirstLastButtons) {
      this.__visible(BUTTON_FIRST, this.pageButtons[0] > 2 || page > 2);
      this.__visible(BUTTON_ELLIPSIS_FIRST, this.pageButtons[0] > 2);
      this.__visible(BUTTON_LAST, this.numberOfPages - this.pageButtons[2] > 2 || this.numberOfPages - page > 1);
      this.__visible(BUTTON_ELLIPSIS_LAST, this.numberOfPages - this.pageButtons[2] > 1);
    } else if (this.useFixedFirstLast) {
      this.__enableButton(BUTTON_LAST, page < this.numberOfPages);
      this.__enableButton(BUTTON_FIRST, page > 1);
    }
  }

  __onPageClick(event) {
    const page = parseInt(event.target.dataset.page);
    this.__doPageChangeAndCallback(page);
  }

  __onPreviousClick(event) {
    this.__doPageChangeAndCallback(this.page-1);
  }

  __onFirstClick(event) {
    this.__doPageChangeAndCallback(1);
  }

  __onLastClick(event) {
    this.__doPageChangeAndCallback(this.numberOfPages);
  }

  __onNextClick(event) {
    this.__doPageChangeAndCallback(this.page+1);
  }

  __normalizePage(page) {
    let normalizedPage = page;
    if (page < 1) normalizedPage = 1;
    else if (page > this.numberOfPages) normalizedPage = this.numberOfPages;

    if (this.pageButtons.indexOf(normalizedPage) < 0) {
      // update selection before changing page
      this.__updateSelection(normalizedPage-2,normalizedPage);
    }

    return normalizedPage;
  }

  update(total, pageSize, page) {
    this.__cleanup();
    this.total = total;
    this.pageSize = pageSize;
    this.numberOfPages = Math.ceil(this.total / this.pageSize);
    this.numberOfButtons = Math.min(this.numberOfPages, 3);
    this.pageButtons = this.__initializeSelection(1,this.numberOfButtons);
    this.hasFirstLastButtons = !this.useFixedFirstLast && 2 < this.numberOfPages - this.numberOfButtons; // creates first/last button
    this.__createButtons();
    this.__doPageChange(this.__normalizePage(page));
    this.__updateVisibility();
  }

  gotoPage(page) {
    this.__doPageChange(this.__normalizePage(page));
  }

  gotoFrom(from) {
    if (from % this.pageSize !== 0) {
      throw new Error(`Invalid from value: ${from}. The valid value must be divisible by ${this.pageSize}.`);
    }

    this.gotoPage((from/this.pageSize)+1)
  }

  changePageSize(pageSize) {
    this.pageSize = pageSize;
    this.__cleanup();
    this.update(this.total, this.pageSize, this.page);
  }

  toString() {
    return `
      Total: ${this.total}
      Number of Pages: ${this.numberOfPages}
      Number of Buttons: ${this.numberOfPages}
      Page Size: ${this.pageSize}
      Page: ${this.page}
      Selections: ${this.pageButtons}
    `;
  }

}

/**
 * Component for rendering stat count
 */
const StatItemComponent = {
  props: {
    count: Number,
    singular: String,
    plural: String,
    url: String
  },
  template: `
    <a v-if="count" v-bind:href="url" class="btn btn-sm btn-link col text-left">
      <span class="h6 pb-0 mb-0 d-block">{{count | localize-number}}</span>
      <span class="text-muted"><small>{{count < 2 ? this.singular : this.plural}}</small></span>
    </a>
  `
};

/**
 * Component for rendering variable stat count
 */
const VariableStatItemComponent =  {
  props: {
    type: String,
    stats: Object,
    url: String
  },
  data() {
    return {
      count: 0,
      countLabel: null
    }
  },
  template: `
    <a v-if="count" v-bind:href="url" class="btn btn-sm btn-link col text-left">
      <span class="h6 pb-0 mb-0 d-block">{{count | localize-number}}</span>
      <span class="text-muted"><small>{{this.countLabel}}</small></span>
    </a>
  `,
  mounted: function() {
    let prefix = this.type === 'individual-study' ? 'collected' : 'harmonized';
    this.count = this.stats.variables;
    this.countLabel = this.count < 2 ? Mica.tr[`${prefix}-variable`] : Mica.tr[`${prefix}-variables`];
  }
};

/**
 * Component for rendering dataset stat count
 */
const DatasetStatItemComponent =  {
  props: {
    type: String,
    stats: Object
  },
  data() {
    return {
      count: 0,
      countLabel: null
    }
  },
  template: `
    <a v-if="count" href="javascript:void(0)" style="cursor: initial;" class="btn btn-sm col text-left">
      <span class="h6 pb-0 mb-0 d-block">{{count | localize-number}}</span>
      <span class="text-muted"><small>{{this.countLabel}}</small></span>
    </a>
  `,
  mounted: function() {
    let key = this.type === 'individual-study' ? 'studyDatasets' : 'harmonizationDatasets';
    let prefix = this.type === 'individual-study' ? 'collected' : 'harmonized';

    this.count = this.stats[key];
    this.countLabel = this.count < 2 ? Mica.tr[`${prefix}-dataset`] : Mica.tr[`${prefix}-datasets`];
  }
};

/**
 * Component for rendering a sorting widget
 */
const EntitiesSortingComponent = {
  template: `
    <div class="sorting position-relative float-left">
      <div class="dropdown">
        <button type="button" class="btn btn-outline-primary dropdown-toggle" data-toggle="dropdown">
          <span v-html="sortLabel(selectedChoice)"></span>
        </button>

        <div class="dropdown-menu">
          <button class="dropdown-item" type="button" v-for="option in options" :key="option.key" v-html="option.label" @click="changeSort(option.key)"></button>
        </div>
      </div>
    </div>
  `,
  props: {
    optionsTranslations: Object,
    initialChoice: String
  },
  data() {
    return {
      selectedChoice: this.initialChoice || "name",
    };
  },
  watch: {
    initialChoice(value, old) {
      if (value !== old) {
        this.selectedChoice = value || "name";
      }
    }
  },
  computed: {
    options() {
      const output = [];
      Object.keys(this.optionsTranslations).forEach(k => {
        output.push({ key: k, label: `${this.optionsTranslations[k]} <i class="fas fa-long-arrow-alt-up"></i>` });
        output.push({ key: `-${k}`, label: `${this.optionsTranslations[k]} <i class="fas fa-long-arrow-alt-down"></i>` });
      });

      return output;
    }
  },
  methods: {
    changeSort(choice) {
      this.selectedChoice = choice;
      this.$emit("sort-update", this.selectedChoice);
    },
    sortLabel(choice) {
      return this.options.filter(option => option.key === choice)[0].label;
    }
  }
};

/**
 * Typeahead UI component
 */
const TypeaheadComponent = {
  template: `
    <div class="typeahead w-100 position-relative">
      <div class="input-group">
        <input type="text" :placeholder="'listing-typeahead-placeholder'   | translate" class="form-control form-control-sm" v-model="text" @keyup="typing($event)">
        <div class="input-group-append">
          <button type="button" class="btn btn-primary btn-sm" @click="select(text)"><i class="fas fa-filter"></i></button>
        </div>
        <button type="button" class="close position-absolute" style="right: 2em; top: 0.25em;" v-if="text.length > 0" @click="clear"><span aria-hidden="true">&times;</span></button>
      </div>

      <div class="list-group position-absolute mt-1 ml-1 shadow" style="z-index: 100; overflow-y: auto; max-height: 16em;" v-if="showChoices && typeaheadItems.length > 0">
        <button type="button" class="list-group-item list-group-item-action" :class="{ active: index === currentIndexSelection }" v-for="(item, index) in typeaheadItems" :key="item" v-html="highlight(item)" @click="select(quote(item))"></button>
      </div>
    </div>
  `,
  props: {
    items: Array,
    externalText: String,
  },
  watch: {
    externalText(value, old) {
      if (value && value !== old) {
        this.text = value;
      }
    },
  },
  data() {
    return {
      text: this.externalText || "",
      showChoices: false,
      currentIndexSelection: -1,
    };
  },
  computed: {
    typeaheadItems() {
      return (this.items || []).filter((item) => item.toLowerCase().indexOf(this.text.toLowerCase()) >= 0);
    },
  },
  methods: {
    typing(event) {
      if (event.keyCode === 13) {
        this.select(this.text);
        return;
      }

      if (event.keyCode === 27) { // escape
        this.clear();
        return;
      }

      if (event.keyCode === 37 || event.keyCode === 38 || event.keyCode === 39 || event.keyCode === 40) { // arrows
        this.changeIndexSelection(event.keyCode);
        return;
      }

      this.$emit("typing", this.cleanUnclosedDoubleQuotes(this.text));

      this.showChoices = true;
      this.currentIndexSelection = -1;
    },
    select(selected) {
      this.text = this.quote(this.currentIndexSelection === -1 ? selected : this.items[this.currentIndexSelection]);
      this.$emit("select", this.text);

      this.showChoices = false;
      this.currentIndexSelection = -1;
    },
    clear() {
      this.select('');
    },
    changeIndexSelection(keyCode) {
      if (keyCode === 37 || keyCode === 38) { // up
        if (this.currentIndexSelection <= 0) {
          this.currentIndexSelection = this.typeaheadItems.length;
        }

        this.currentIndexSelection--;
      } else if (keyCode === 39 || keyCode === 40) { // down
        if (this.currentIndexSelection >= this.typeaheadItems.length - 1) {
          this.currentIndexSelection = -1;
        }

        this.currentIndexSelection++;
      }
    },
    highlight(item) {
      let output = item;
      const index = item.toLowerCase().indexOf(this.text.toLowerCase());
      if (index >= 0) {
        output = `${output.substring(0, index)}<strong>${output.substring(index, index + this.text.length)}</strong>${output.substring(index + this.text.length)}`;
      }

      return output;
    },
    quote(text) {
      if ((text || "").trim().length > 0) {
        return `"${text.trim().replace(/^"|"$/g, "").replace(/"/, '\\"')}"`;
      }

      return text;
    },
    cleanUnclosedDoubleQuotes(text) {
      let output = (text || "").trim();
      const doubleQuotesRegxp = /"/g;
      const instancesOfDoubleQuoteCharacters = (output.match(doubleQuotesRegxp) || []).length;

      if (instancesOfDoubleQuoteCharacters % 2 !== 0) {
        return output.replace(doubleQuotesRegxp, "");
      }

      return output;
    },
  },
};

/**
 * Base Vue App for Entities (Studies, Networks, Datasets)
 *
 */
const NAVIGATION_POSITIONS = ['bottom', 'top'];

const ObibaEntitiesApp = {
  data() {
    return {
      loading: false,
      locale: 'en',
      pageSizeSelectors: {},
      paginations: {},
      service: null,
      entities: [],
      suggestions: [],
      suggestionTimeoutId: null,
      initialFilter: '',
      total: 0
    }
  },
  mounted: function() {
    window.addEventListener('beforeunload', this.onBeforeUnload.bind(this));
    window.addEventListener('popstate', this.onLocationChanged.bind(this));
    NAVIGATION_POSITIONS.forEach(pos => {
      this.pageSizeSelectors[`obiba-page-size-selector-${pos}`] = new OBiBaPageSizeSelector(`obiba-page-size-selector-${pos}`, DEFAULT_PAGE_SIZES, DEFAULT_PAGE_SIZE, this.onPageSizeChanged)
      this.paginations[`obiba-pagination-${pos}`] = new OBiBaPagination(`obiba-pagination-${pos}`, true, this.onPagination);
    });
  },
  methods: {
    onBeforeUnload: function() {
      window.removeEventListener('beforeunload', this.onBeforeUnload.bind(this));
      window.removeEventListener('popstate', this.onLocationChanged.bind(this));
    },
    ensureEvenEntities: function() {
      if (this.entities.length % 2 !== 0) {
        this.entities.push({name:'', description:'',id:''});
      }
    },
    getResultDtoField: function() {
      throw new Error("getResultDtoField() must be deinfed in subclass");
    },
    getEntities: function(queryTree) {
      this.loading = true;
      this.service.getEntities(queryTree.serialize(), (response) => {
        this.loading = false;
        this.setEntities(response);
        const limits = this.service.getLimitQueryValues(queryTree);
        NAVIGATION_POSITIONS.forEach(pos => {
          this.pageSizeSelectors[`obiba-page-size-selector-${pos}`].update(limits.size)
          this.paginations[`obiba-pagination-${pos}`].update(response[this.getResultDtoField()].totalHits, limits.size,(limits.from/limits.size)+1);
        });
        this.service.updateLocation(queryTree, true);
      });
    },
    onLocationChanged: function (event) {
      // Log the state data to the console
      const queryTree = this.service.prepareQuery(this.locale);
      this.service.getEntities(queryTree.serialize(), (response) => {
        const limits = this.service.getLimitQueryValues(queryTree);
        NAVIGATION_POSITIONS.forEach(pos => {
          this.pageSizeSelectors[`obiba-page-size-selector-${pos}`].update(limits.size);
          this.paginations[`obiba-pagination-${pos}`].update(response[this.getResultDtoField()].totalHits, limits.size,(limits.from/limits.size)+1);
        });
        this.setEntities(response);
      });
    },
    onPageSizeChanged: function(data) {
      const queryTree = this.service.prepareQuery(this.locale, 0, data.size);
      this.service.getEntities(queryTree.serialize(), (response) => {
        NAVIGATION_POSITIONS.forEach(pos => {
          if (data.id !== `obiba-page-size-selector-${pos}`) {
            this.pageSizeSelectors[`obiba-page-size-selector-${pos}`].update(data.size);
          }
          this.paginations[`obiba-pagination-${pos}`].changePageSize(data.size);
        });
        this.service.updateLocation(queryTree);
        this.setEntities(response);
      });
    },
    onPagination: function(data) {
      const queryTree = this.service.prepareQuery(this.locale, data.from, data.size);
      this.service.getEntities(queryTree.serialize(), (response) => {
        NAVIGATION_POSITIONS.forEach(pos => {
          if (data.id !== `obiba-page-size-pagination-${pos}`) {
            this.paginations[`obiba-pagination-${pos}`].update(response[this.getResultDtoField()].totalHits, data.size,(data.from/data.size)+1);
          }
        });
        this.setEntities(response);
        this.service.updateLocation(queryTree);
      });
    },
    onType: function(text) {
      this.suggestions = [];
      if (this.suggestionTimeoutId) {
        clearTimeout(this.suggestionTimeoutId);
      }

      this.suggestionTimeoutId = setTimeout(() => this.service.getSuggestions(text, this.locale, (response) => {
        this.suggestions = Array.isArray(response) ? response : [];
      }), 250);
    },
    onSelect: function(selectedText) {
      const queryTree = this.service.prepareQuery(this.locale);
      this.service.updateFilter(queryTree, selectedText);
      this.getEntities(queryTree);

      this.initialFilter = selectedText;
    },
    onSortUpdate: function(sort) {
      const queryTree = this.service.prepareQuery(this.locale);
      this.service.updateSort(queryTree, sort);

      this.getEntities(queryTree);
    }
  }
}

class ObibaDatasetsApp {

  static build(element, type, locale, sortOptionsTranslations) {
    return new Vue({
      locale,
      el: element,
      extends: ObibaEntitiesApp,
      data() {
        return {
          sortOptionsTranslations,
          initialSort: 'name'
        };
      },
      components: {
        'stat-item' : StatItemComponent,
        'typeahead': TypeaheadComponent,
        'sorting': EntitiesSortingComponent
      },
      mounted: function () {
        this.service =  ObibaDatasetsService.newInstance(type);
        const queryTree = this.service.prepareQuery(locale);
        this.service.updateSort(queryTree, this.initialSort);
        this.getEntities(queryTree);

        this.initialFilter = this.service.getFilterQueryValue(this.service.prepareQuery(locale));
      },
      methods: {
        networks: function(id) {
          return MicaService.normalizeUrl(`/search#lists?type=networks&query=dataset(in(Mica_dataset.id,${id}))`);
        },
        studies: function(id) {
          return MicaService.normalizeUrl(`/search#lists?type=studies&query=dataset(in(Mica_dataset.id,${id}))`);
        },
        variables: function(id) {
          return MicaService.normalizeUrl(`/search#lists?type=variables&query=dataset(in(Mica_dataset.id,${id}))`);
        },
        hasStats: function(dataset) {
          const countStats = dataset['obiba.mica.CountStatsDto.datasetCountStats'];
          return countStats.variables + countStats.studies + countStats.networks > 0
        },
        getResultDtoField: function () {
          return 'datasetResultDto';
        },
        setEntities: function(response) {
          const dto = response.datasetResultDto;
          if (dto && dto['obiba.mica.DatasetResultDto.result'] && dto['obiba.mica.DatasetResultDto.result'].datasets) {
            this.entities = dto['obiba.mica.DatasetResultDto.result'].datasets;
            this.total = dto.totalHits;
            this.ensureEvenEntities();
          }
        }
      }
    });
  }
}

class ObibaStudiesApp {

  static build(element, type, locale, sortOptionsTranslations) {
    return new Vue({
      el: element,
      extends: ObibaEntitiesApp,
      data() {
        return {
          locale,
          sortOptionsTranslations,
          initialSort: type === 'harmonization-studies' ? '-lastModifiedDate' : 'name'
        };
      },
      components: {
        'dataset-stat-item' : DatasetStatItemComponent,
        'variable-stat-item' : VariableStatItemComponent,
        'typeahead': TypeaheadComponent,
        'sorting': EntitiesSortingComponent
      },
      mounted: function () {
        this.service =  ObibaStudiesService.newInstance(type);

        const queryTree = this.service.prepareQuery(locale);
        this.service.updateSort(queryTree, this.initialSort);
        this.getEntities(queryTree);

        this.initialFilter = this.service.getFilterQueryValue(this.service.prepareQuery(locale));
      },
      methods: {
        variablesUrl: function(study) {
          let variableType = study.studyResourcePath === 'individual-study' ? 'Collected' : 'Dataschema';
          return MicaService.normalizeUrl(`/search#lists?type=variables&query=study(in(Mica_study.id,${study.id})),variable(in(Mica_variable.variableType,${variableType}))`)
        },
        hasStats: function(study) {
          const countStats = study['obiba.mica.CountStatsDto.studyCountStats'];
          const datasetStats = type === 'individual-studies' ? countStats.studyDatasets : countStats.harmonizationDatasets
          let hasModelStats = study.model
            && (study.model.numberOfParticipants && study.model.numberOfParticipants.number
              || study.model.methods && study.model.methods.design);

          return datasetStats + countStats.variables > 0 || hasModelStats;
        },
        getResultDtoField: function () {
          return 'studyResultDto';
        },
        setEntities: function(response) {
          const dto = response.studyResultDto;
          if (dto && dto['obiba.mica.StudyResultDto.result'] && dto['obiba.mica.StudyResultDto.result'].summaries) {
            this.entities = dto['obiba.mica.StudyResultDto.result'].summaries;
            this.total = dto.totalHits;
            this.ensureEvenEntities();
          }
        }
      }
    });
  }
}

class ObibaNetworksApp {

  static build(element, locale, sortOptionsTranslations) {
    return new Vue({
      el: element,
      extends: ObibaEntitiesApp,
      data() {
        return {
          locale,
          sortOptionsTranslations,
          initialSort: "-numberOfStudies",
        };
      },
      components: {
        'stat-item': StatItemComponent,
        'typeahead': TypeaheadComponent,
        'sorting': EntitiesSortingComponent
      },
      mounted: function () {
        this.service = ObibaNetworksService.newInstance();
        const queryTree = this.service.prepareQuery(locale);
        this.service.updateSort(queryTree, this.initialSort);
        this.getEntities(queryTree);

        this.initialFilter = this.service.getFilterQueryValue(this.service.prepareQuery(locale));
      },
      methods: {
        individualStudies: function(id) {
          return MicaService.normalizeUrl(`/search#lists?type=studies&query=network(in(Mica_network.id,${id})),study(in(Mica_study.className,Study))`);
        },
        individualStudiesWithVariables: function(id) {
          return MicaService.normalizeUrl(`/search#lists?type=studies&query=network(in(Mica_network.id,${id})),variable(in(Mica_variable.variableType,Collected))`);
        },
        individualStudyVariables: function(id) {
          return MicaService.normalizeUrl(`/search#lists?type=variables&query=network(in(Mica_network.id,${id})),variable(in(Mica_variable.variableType,Collected))`);
        },
        harmonizationStudies: function(id) {
          return MicaService.normalizeUrl(`/search#lists?type=studies&query=network(in(Mica_network.id,${id})),study(in(Mica_study.className,HarmonizationStudy))`);
        },
        harmonizationStudyVariables: function(id) {
          return MicaService.normalizeUrl(`/search#lists?type=variables&query=network(in(Mica_network.id,${id})),variable(in(Mica_variable.variableType,Dataschema))`);
        },
        hasStats: function(stats) {
          return stats.individualStudies + stats.studiesWithVariables + stats.studyVariables + stats.dataschemaVariables + stats.harmonizationStudies > 0;
        },
        getResultDtoField: function () {
          return 'networkResultDto';
        },
        setEntities: function(response) {
          const dto = response.networkResultDto;
          if (dto && dto['obiba.mica.NetworkResultDto.result'] && dto['obiba.mica.NetworkResultDto.result'].networks) {
            this.entities = dto['obiba.mica.NetworkResultDto.result'].networks;
            this.total = dto.totalHits;
            this.ensureEvenEntities();
          }
        }
      }
    });
  }
}
