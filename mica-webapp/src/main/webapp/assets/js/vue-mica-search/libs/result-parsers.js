class GraphicsResultParser {
  constructor(normalizePath) {
    this.normalizePath = normalizePath;
  }

  static #VALID_CHOROPLETH_COLORSCALE_NAMES = ['Blackbody', 'Bluered', 'Blues', 'Cividis', 'Earth', 'Electric', 'Greens', 'Greys', 'Hot', 'Jet', 'Picnic', 'Portland', 'Rainbow', 'RdBu', 'Reds', 'Viridis', 'YlGnBu', 'YlOrRd'];

  static #DEFAULT_GRAPH_PROCESSORS = {
    bar: {
      /**
       * @param input
       * @param colors String or Array
       */
      processData(input, colors) {

        const x = [];
        const y = [];

        input.values.forEach(val => {
          x.push(val.count);
          y.push(val.title);
        });

        const width = Array(x.length).fill(x.length* 0.1);

        return [{
          type: "bar",
          orientation: "h",
          marker: {
            color: colors
          },
          x: x.reverse(),
          y: y.reverse(),
          width
        }];
      },
      layoutObject: {
        margin: {
          t: 20,
          b: 40
        },
        yaxis: {
          automargin: true,
          ticksuffix: ' '
        }
      }
    },
    pie: {
      /**
       * @param input
       * @param colors Array
       */
      processData(input, colors) {
        const values = [];
        const labels = [];

        input.values.forEach(val => {
          values.push(val.count);
          labels.push(val.title);
        });

        return [{
          type: "pie",
          sort: false,
          marker: {
            colors: colors
          },
          hoverinfo: "label+value",
          values,
          labels
        }];
      },
      layoutObject: {
        margin: {
          t: 50,
          b: 40
        }
      }
    },
    geo: {
      /**
       * @param input
       * @param colors String or Array
       */
      processData(input, colors) {
        const z = [];
        const locations = [];
        const text = [];

        input.values.forEach(val => {
          z.push(val.count);
          locations.push(val.key);
          text.push(val.title);
        });

        const trace = {
          type: "choropleth",
          locations,
          text,
          z,
          zmax: Math.max(...z),
          zmin: 0,
          hoverinfo: "text+z",
          colorbar: {
            thickness: 10,
            ypad: 50
          }
        }

        if (Array.isArray(colors)) {
          trace.colorscale = [[0, "#f3f3f3"]].concat(colors.map((color, index) => [((index + 1) / colors.length), color]));
        } else if (GraphicsResultParser.#VALID_CHOROPLETH_COLORSCALE_NAMES.indexOf(colors) > -1) {
          trace.colorscale = colors;
          trace.reversescale = true;
        } else {
          trace.colorscale = "Blues";
          trace.reversescale = true;
        }

        return [trace];
      },
      layoutObject: {
        geo: {
          showframe: false,
          showcoastlines: false,
          countrywidth: 0.25,
          showcountries: true,
          projection: {
            type: "robinson",
          }
        },
        margin: {
          t: 0,
          r: -20,
          b: 0,
          l: 0
        }
      }
    }
  };

  static #isCorrectVocabulary(vocabulary, name) {
    return vocabulary && (vocabulary.name === name || vocabulary.attributes.filter(a => a.key === "alias" && a.value === name)[0]);
  }

  static #getPlotlyType(type) {
    if (type === 'bar' || type === 'horizontalBar') {
      return 'bar';
    } else if (type === 'pie' || type === 'doughnut') {
      return 'pie';
    } else if (type === 'choropleth') {
      return 'geo';
    }
  }

  static #parseForChart(chartData, options) {
    const studyVocabulary = (options.taxonomy || {vocabularies: []}).vocabularies.filter(vocabulary => GraphicsResultParser.#isCorrectVocabulary(vocabulary, options.agg))[0];

    if (studyVocabulary) {
      const terms = studyVocabulary.terms.map(term => term.name);
      chartData.sort((a, b) => {
        return terms.indexOf(a.key) - terms.indexOf(b.key);
      });
    }

    const processor = GraphicsResultParser.#DEFAULT_GRAPH_PROCESSORS[GraphicsResultParser.#getPlotlyType(options.type || 'bar')];
    return [processor.processData({key: options.agg, values: chartData, title: options.title}, options.colors || options.backgroundColor), processor.layoutObject];
  }

  static #parseForTable(vocabulary, chartData, forSubAggData) {
    return chartData.filter(term => term.count>0).map(term => {
      let row = {
        vocabulary: vocabulary.replace(/model-/, ""),
        key: term.key,
        title: term.title,
        count: term.count
      };

      if (forSubAggData) {
        const subAgg = term.aggs.filter((agg) => agg.aggregation === forSubAggData.agg)[0];
        row.subAgg = (subAgg[forSubAggData.dataKey] || {data: {}}).data[forSubAggData.data] || 0;
      }

      return row;
    });
  }

  parse(chartData, chartOptions, totalHits) {
    if (!chartData) {
      return;
    }

    const tr = Vue.filter('translate') || (value => value);
    const labelStudies = tr('studies');
    const aggData = chartData[chartOptions.dataKey];

    let [data, layout] = typeof chartOptions.parseForChart === 'function'
      ? chartOptions.parseForChart(aggData, chartOptions, totalHits)
      : GraphicsResultParser.#parseForChart(aggData, chartOptions, totalHits);

    const tableCols = [chartOptions.title, labelStudies];

    if (chartOptions.subAgg) {
      tableCols.push(chartOptions.subAgg.title);
    }

    const tableRows = typeof chartOptions.parseForTable === 'function'
      ? chartOptions.parseForTable(chartOptions.vocabulary, aggData, chartOptions.subAgg, totalHits)
      : GraphicsResultParser.#parseForTable(chartOptions.vocabulary, aggData, chartOptions.subAgg, totalHits);

    const plotData = {
      data: data,
      layout: layout
    };

    return [plotData, {cols: tableCols, rows: tableRows}];

  }
}

class VariablesResultParser {

  constructor(normalizePath) {
    this.normalizePath = normalizePath;
  }

  parse(data, micaConfig, localize, displayOptions, studyTypeSelection) {
    const variablesResult = data.variableResultDto;
    const tr = Vue.filter('translate') || (value => value);
    const taxonomyTitle = Vue.filter('taxonomy-title') || (value => value);

    let columnKey = 'variableColumns';
    if (studyTypeSelection) {
      if (studyTypeSelection.study) {
        columnKey = 'variableColumnsIndividual';
      } else if(studyTypeSelection.harmonization) {
        columnKey = 'variableColumnsHarmonization';
      }
    }

    if (!variablesResult) {
      throw new Error("No variable results available.");
    }

    if (variablesResult.totalHits < 1) return { totalHits: 0 };

    const result = variablesResult["obiba.mica.DatasetVariableResultDto.result"];

    if (!result) {
      throw new Error("Invalid JSON.");
    }

    let parsed = {
      data: [],
      totalHits: variablesResult.totalHits
    }

    const summaries = result.summaries || [];

    summaries.forEach(summary => {

      let path = this.normalizePath(`/variable/${summary.id}`);
      let row = ['<i class="far fa-square"></i>', `<a data-item-id="${summary.id}" href="${path}">${summary.name}</a>`,];

      (displayOptions[columnKey] || displayOptions.variableColumns).forEach(column => {
        switch (column) {
          case 'label':
          case 'label+description': {
            let labelElem = marked(localize(summary.variableLabel));
            if (column === 'label+description' && summary.description) {
              labelElem =  "<i class='fa fa-info-circle text-muted float-left mr-2 mt-1' data-toggle='tooltip' data-html='true' title='" + marked(localize(summary.description)) + "'></i> " + labelElem;
            }
            row.push(labelElem);
            break;
          }
          case 'valueType': {
            row.push(tr(summary.valueType + '-type'));
            break;
          }
          case 'annotations': {
            const annotations = (summary.annotations || []).reduce(
              (acc, annotation) =>
                ("" !== acc ? `${acc}<br>` : "") + "<span><i class='fa fa-tag text-muted'></i> " +
                taxonomyTitle.apply(null, [`${annotation.taxonomy}.${annotation.vocabulary}.${annotation.value}`]) + "</span>",
              ""
            );
            row.push(annotations);
            break;
          }
          case 'type': {
            if (micaConfig.isCollectedDatasetEnabled && micaConfig.isHarmonizedDatasetEnabled) {
              row.push(tr(summary.variableType.toLowerCase()));
            }
            break;
          }
          case 'study': {
            if (!micaConfig.isSingleStudyEnabled) {
              path = this.normalizePath(`/study/${summary.studyId}`);
              row.push(`<a href="${path}">${localize(summary.studyAcronym)}</a>`);
            }
            break;
          }

          case 'population': {
            path = this.normalizePath(`/study/${summary.studyId}`);
            if (summary.populationName) {
              row.push(`<a href="${path}#population/${summary.populationId}">${localize(summary.populationName)}</a>`);
            } else {
              row.push('-');
            }
            break;
          }
          case 'dce':
          case 'data-collection-event': {
            path = this.normalizePath(`/study/${summary.studyId}`);
            if (summary.dceName) {
              row.push(`<a href="${path}#population/${summary.populationId}/data-collection-event/${summary.dceId}">${localize(summary.dceName)}</a>`);
            } else {
              row.push('-');
            }
            break;
          }
          case 'dataset': {
            path = this.normalizePath(`/dataset/${summary.datasetId}`);
            row.push(`<a href="${path}">${localize(summary.datasetAcronym)}</a>`);
            break;
          }
          default:
            row.push('');
            console.debug('Wrong variable table column: ' + column);
        }
      });

      parsed.data.push(row);
    });

    return parsed;
  }
}

class StudiesResultParser {

  constructor(normalizePath, locale) {
    this.normalizePath = normalizePath;
    this.locale = locale;
  }

  static #getNumberOfParticipants(content) {
     const numberOfParticipants = content['numberOfParticipants'];
     if (numberOfParticipants) {
      const participant = numberOfParticipants['participant'];
      if (participant) {
        return participant.number || '-';
      }
     }

     return '-';
  }

  parse(data, micaConfig, localize, displayOptions, studyTypeSelection) {
    const studiesResult = data.studyResultDto;

    let columnKey = 'studyColumns';
    if (studyTypeSelection) {
      if (studyTypeSelection.study) {
        columnKey = 'studyColumnsIndividual';
      } else if(studyTypeSelection.harmonization) {
        columnKey = 'studyColumnsHarmonization';
      }
    }

    if (!studiesResult) {
      throw new Error("No network results available.");
    }

    if (studiesResult.totalHits < 1) return { totalHits: 0 };

    const result = studiesResult["obiba.mica.StudyResultDto.result"];

    if (!result) {
      throw new Error("Invalid JSON.");
    }

    let parsed = {
      data: [],
      totalHits: studiesResult.totalHits
    }

    const taxonomyFilter = Vue.filter('taxonomy-title') || (title => title);
    const checkIcon = `<i class="fa fa-check">`;
    const summaries = result.summaries || [];

    summaries.forEach(summary => {

      const type = summary.studyResourcePath === 'harmonization-study'
        ? taxonomyFilter.apply(null, ['Mica_study.className.HarmonizationStudy'])
        : taxonomyFilter.apply(null, ['Mica_study.className.Study']) ;

      const stats = summary['obiba.mica.CountStatsDto.studyCountStats'] || {};
      const content = JSON.parse(summary.content);
      const dataSources = summary.dataSources || [];
      const hasDatasource = (dataSources, id) => dataSources.indexOf(id) > -1;
      const design = summary.design ? taxonomyFilter.apply(null, [`Mica_study.methods-design.${summary.design}`]) : '-';
      let anchor = (type, value, studyType) =>
        `<a href="" class="query-anchor" data-study-type="${studyType}" data-target="study" data-target-id="${summary.id}" data-type="${type}">${value.toLocaleString(this.locale)}</a>`;

      let path = this.normalizePath(`/study/${summary.id}`);
      let row = [`<a href="${path}">${localize(summary.acronym)}</a>`];

      (displayOptions[columnKey] || displayOptions.studyColumns).forEach(column => {
        switch (column) {
          case 'name': {
            row.push(localize(summary.name));
            break;
          }
          case 'type': {
            if (micaConfig.isCollectedDatasetEnabled && micaConfig.isHarmonizedDatasetEnabled) {
              row.push(type);
            }
            break;
          }
          case 'study-design': {
            row.push(design);
            break;
          }
          case 'data-sources-available': {
            row.push(hasDatasource(dataSources, "questionnaires") ? checkIcon : "-");
            row.push(hasDatasource(dataSources, "physical_measures") ? checkIcon : "-");
            row.push(hasDatasource(dataSources, "biological_samples") ? checkIcon : "-");
            row.push(hasDatasource(dataSources, "others") ? checkIcon : "-");
            break;
          }
          case 'participants': {
            row.push(StudiesResultParser.#getNumberOfParticipants(content));
            break;
          }
          case 'networks': {
            if (micaConfig.isNetworkEnabled && !micaConfig.isSingleNetworkEnabled) {
              row.push(stats.networks ? anchor("networks", stats.networks, "") : "-");
            }
            break;
          }
          case 'individual': {
            if (micaConfig.isCollectedDatasetEnabled) {
              row.push(stats.studyDatasets
                ? anchor("datasets", stats.studyDatasets, "Study")
                : "-");
              row.push(stats.studyVariables
                ? anchor("variables", stats.studyVariables, "Study")
                : "-");
            }
            break;
          }
          case 'harmonization': {
            if (micaConfig.isHarmonizedDatasetEnabled) {
              row.push(stats.harmonizationDatasets
                ? anchor("datasets", stats.harmonizationDatasets, "HarmonizationStudy")
                : "-");
              row.push(stats.dataschemaVariables
                ? anchor("variables", stats.dataschemaVariables, "HarmonizationStudy")
                : "-");
            }
            break;
          }
          case 'datasets': {
            if (micaConfig.isCollectedDatasetEnabled) {
              row.push(stats.studyDatasets
                ? anchor("datasets", stats.studyDatasets, "Study")
                : "-");
            }
            if (micaConfig.isHarmonizedDatasetEnabled) {
              row.push(stats.dataschemaDatasets
                ? anchor("datasets", stats.dataschemaDatasets, "HarmonizationStudy")
                : "-");
            }
            break;
          }
          case 'variables': {
            if (micaConfig.isCollectedDatasetEnabled) {
              row.push(stats.studyVariables
                ? anchor("variables", stats.studyVariables, "Study")
                : "-");
            }
            if (micaConfig.isHarmonizedDatasetEnabled) {
              row.push(stats.dataschemaVariables
                ? anchor("variables", stats.dataschemaVariables, "HarmonizationStudy")
                : "-");
            }
            break;
          }
          default:
            row.push('');
            console.debug('Wrong study table column: ' + column);
        }
      });

      parsed.data.push(row);
    });

    return parsed;
  }
}

class DatasetsResultParser {

  constructor(normalizePath, locale) {
    this.normalizePath = normalizePath;
    this.locale = locale;
  }

  parse(data, micaConfig, localize, displayOptions, studyTypeSelection) {
    const datasetsResult = data.datasetResultDto;
    const tr = Vue.filter('translate') || (value => value);
    const taxonomyFilter = Vue.filter('taxonomy-title') || (value => value);

    let columnKey = 'datasetColumns';
    if (studyTypeSelection) {
      if (studyTypeSelection.study) {
        columnKey = 'datasetColumnsIndividual';
      } else if(studyTypeSelection.harmonization) {
        columnKey = 'datasetColumnsHarmonization';
      }
    }

    if (!datasetsResult) {
      throw new Error("No dataset results available.");
    }

    if (datasetsResult.totalHits < 1) return { totalHits: 0};

    const result = datasetsResult["obiba.mica.DatasetResultDto.result"];

    if (!result) {
      throw new Error("Invalid JSON.");
    }

    let parsed = {
      data: [],
      totalHits: datasetsResult.totalHits
    }

    const datasets = result.datasets || [];

    datasets.forEach(dataset => {

      let path = this.normalizePath(`/dataset/${dataset.id}`);
      let row = [`<a href="${path}">${localize(dataset.acronym)}</a>`];
      const type = dataset.variableType === 'Dataschema'
        ? taxonomyFilter.apply(null, ['Mica_dataset.className.HarmonizationDataset'])
        : taxonomyFilter.apply(null, ['Mica_dataset.className.StudyDataset']) ;
      const stats = dataset['obiba.mica.CountStatsDto.datasetCountStats'] || {};
      let anchor = (type, value) => `<a href="" class="query-anchor" data-target="dataset" data-target-id="${dataset.id}" data-type="${type}">${value.toLocaleString(this.locale)}</a>`;

      (displayOptions[columnKey] || displayOptions.datasetColumns).forEach(column => {
        switch (column) {
          case 'name': {
            row.push(localize(dataset.name));
            break;
          }
          case 'type': {
            if (micaConfig.isCollectedDatasetEnabled && micaConfig.isHarmonizedDatasetEnabled) {
              row.push(tr(type.toLowerCase()));
            }
            break;
          }
          case 'networks': {
            if (micaConfig.isNetworkEnabled && !micaConfig.isSingleNetworkEnabled) {
              row.push(stats.networks ? anchor('networks', stats.networks) : '-');
            }
            break;
          }
          case 'studies': {
            if (!micaConfig.isSingleStudyEnabled) {
              row.push(stats.studies ? anchor('studies', stats.studies) : '-');
            }
            break;
          }
          case 'variables': {
            row.push(stats.variables ? anchor('variables', stats.variables) : '-');
            break;
          }
          default:
            row.push('');
            console.debug('Wrong dataset table column: ' + column);
        }
      });

      parsed.data.push(row);
    });

    return parsed;
  }
}

class NetworksResultParser {

  constructor(normalizePath, locale) {
    this.normalizePath = normalizePath;
    this.locale = locale;
  }

  parse(data, micaConfig, localize, displayOptions, studyTypeSelection) {
    const networksResult = data.networkResultDto;

    let columnKey = 'networkColumns';
    if (studyTypeSelection) {
      if (studyTypeSelection.study) {
        columnKey = 'networkColumnsIndividual';
      } else if(studyTypeSelection.harmonization) {
        columnKey = 'networkColumnsHarmonization';
      }
    }

    if (!networksResult) {
      throw new Error("No network results available.");
    }

    if (networksResult.totalHits < 1) return { totalHits: 0};

    const result = networksResult["obiba.mica.NetworkResultDto.result"];

    if (!result) {
      throw new Error("Invalid JSON.");
    }

    let parsed = {
      data: [],
      totalHits: networksResult.totalHits
    }

    const networks = result.networks || [];

    networks.forEach(network => {
      const stats = network['obiba.mica.CountStatsDto.networkCountStats'] || {};
      let anchor = (type, value, studyType) => `<a href="" class="query-anchor" data-study-type="${studyType}" data-target="network" data-target-id="${network.id}" data-type="${type}">${value.toLocaleString(this.locale)}</a>`;

      let path = this.normalizePath(`/network/${network.id}`);
      let row = [`<a href="${path}">${localize(network.acronym)}</a>`];

      (displayOptions[columnKey] || displayOptions.networkColumns).forEach(column => {
        switch (column) {
          case 'name': {
            row.push(localize(network.name));
            break;
          }
          case 'studies': {
            row.push(stats.studies ? anchor('studies', stats.studies, "") : '-');
            break;
          }
          case 'datasets': {
            if (micaConfig.isCollectedDatasetEnabled) {
              row.push(stats.studyDatasets ? anchor('datasets', stats.studyDatasets, 'Study') : '-');
            }
            if (micaConfig.isHarmonizedDatasetEnabled) {
              row.push(stats.harmonizationDatasets ? anchor('datasets', stats.harmonizationDatasets, 'HarmonizationStudy') : '-');
            }
            break;
          }
          case 'variables': {
            if (micaConfig.isCollectedDatasetEnabled) {
              row.push(stats.studyVariables ? anchor('variables', stats.studyVariables, 'Study') : '-');
            }
            if (micaConfig.isHarmonizedDatasetEnabled) {
              row.push(stats.dataschemaVariables ? anchor('variables', stats.dataschemaVariables, 'HarmonizationStudy') : '-');
            }
            break;
          }
          default:
            row.push('');
            console.debug('Wrong network table column: ' + column);
        }
      });

      parsed.data.push(row);
    });

    return parsed;
  }
}

class IdSplitter {
  constructor(bucket, result, normalizePath) {
    this.bucket = bucket;
    this.result = result;
    this.normalizePath = normalizePath;
    this.rowSpans = {};
    this.minMax = {};
    this.currentYear = new Date().getFullYear();
    this.currentMonth = new Date().getMonth() + 1;
    this.currentYearMonth = this.currentYear + '-' + this.currentMonth;
    this.currentDate = this.#toTime(this.currentYearMonth, true);
  }

  static #BUCKET_TYPES = {
    STUDY: 'studyId',
    DCE: 'dceId',
    DATASET: 'datasetId',
  }

  #getBucketUrl(bucket, id) {
    switch (bucket) {
      case IdSplitter.#BUCKET_TYPES.STUDY:
      case IdSplitter.#BUCKET_TYPES.DCE:
        return this.normalizePath(`/study/${id}`);
      case IdSplitter.#BUCKET_TYPES.DATASET:
        return this.normalizePath(`/dataset/${id}`)
    }

    return this.normalizePath('');
  }

  #appendRowSpan(id) {
    let rowSpan;
    if (!this.rowSpans[id]) {
      rowSpan = 1;
      this.rowSpans[id] = 1;
    } else {
      rowSpan = 0;
      this.rowSpans[id] = this.rowSpans[id] + 1;
    }
    return rowSpan;
  }

  #appendMinMax(id, start, end) {
    if (this.minMax[id]) {
      if (start < this.minMax[id][0]) {
        this.minMax[id][0] = start;
      }
      if (end > this.minMax[id][1]) {
        this.minMax[id][1] = end;
      }
    } else {
      this.minMax[id] = [start, end];
    }
  }

  #toTime(yearMonth, start) {
    let res;
    if (yearMonth) {
      if (yearMonth.indexOf('-') > 0) {
        let ym = yearMonth.split('-');
        if (!start) {
          let m = parseInt(ym[1]);
          if (m < 12) {
            ym[1] = m + 1;
          } else {
            ym[0] = parseInt(ym[0]) + 1;
            ym[1] = 1;
          }
        }
        let ymStr = ym[0] + '/' + ym[1] + '/01';
        res = Date.parse(ymStr);
      } else {
        res = start ? Date.parse(yearMonth + '/01/01') : Date.parse(yearMonth + '/12/31');
      }
    }
    return res;
  }

  #getProgress(startYearMonth, endYearMonth) {
    let start = this.#toTime(startYearMonth, true);
    let end = endYearMonth ? this.#toTime(endYearMonth, false) : this.currentDate;
    let current = end < this.currentDate ? end : this.currentDate;
    if (end === start) {
      return 100;
    } else {
      return Math.round(startYearMonth ? 100 * (current - start) / (end - start) : 0);
    }
  }

  splitIds(micaConfig, locale) {

    let cols = {
      colSpan: this.bucket.startsWith('dce') ? (micaConfig.isSingleStudyEnabled ? 2 : 3) : 1,
      ids: {}
    };

    let odd = true;
    let groupId;

    this.result.rows.forEach((row, i) => {
      row.hitsTitles = row.hits.map(function (hit) {
        return hit.toLocaleString(locale);
      });
      cols.ids[row.value] = [];
      if (this.bucket.startsWith('dce')) {
        let ids = row.value.split(':');
        let isHarmo = row.className.indexOf('Harmonization') > -1 || ids[2] === '.'; // would work for both HarmonizationDataset and HarmonizationStudy
        let titles = row.title.split(':');
        let descriptions = row.description.split(':');
        let rowSpan;
        let id;

        // study
        id = ids[0];
        if (!groupId) {
          groupId = id;
        } else if (id !== groupId) {
          odd = !odd;
          groupId = id;
        }
        rowSpan = this.#appendRowSpan(id);
        this.#appendMinMax(id, row.start || this.currentYearMonth, row.end || this.currentYearMonth);
        const studyUrl = this.#getBucketUrl(this.bucket, id);

        cols.ids[row.value].push({
          id: id,
          url: studyUrl,
          title: titles[0],
          description: descriptions[0],
          rowSpan: rowSpan,
          index: i++
        });

        // population
        id = ids[0] + ':' + ids[1];
        const populationUrl = `${studyUrl}#/population/${id}`;

        rowSpan = this.#appendRowSpan(id);
        cols.ids[row.value].push({
          id: id,
          url: populationUrl,
          title: titles[1],
          description: descriptions[1],
          rowSpan: rowSpan,
          index: i++
        });

        // dce
        cols.ids[row.value].push({
          id: isHarmo ? '-' : row.value,
          title: titles[2],
          description: descriptions[2],
          start: row.start,
          current: this.currentYearMonth,
          end: row.end,
          progressClass: odd ? 'info' : 'warning',
          url: isHarmo ? studyUrl : `${populationUrl}/data-collection-event/${row.value}`,
          rowSpan: 1,
          index: i++
        });
      } else {
        cols.ids[row.value].push({
          id: row.value,
          url: this.#getBucketUrl(this.bucket, row.value),
          title: row.title,
          description: row.description,
          min: row.start,
          start: row.start,
          current: this.currentYear,
          end: row.end,
          max: row.end,
          progressStart: 0,
          progress: this.#getProgress(row.start ? row.start + '-01' : this.currentYearMonth, row.end ? row.end + '-12' : this.currentYearMonth),
          progressClass: odd ? 'info' : 'warning',
          rowSpan: 1,
          index: i++
        });
        odd = !odd;
      }
    });

    // adjust the rowspans and the progress
    if (this.bucket.startsWith('dce')) {
      this.result.rows.forEach((row, i) => {
        row.hitsTitles = row.hits.map(function (hit) {
          return hit.toLocaleString(locale);
        });
        if (cols.ids[row.value][0].rowSpan > 0) {
          cols.ids[row.value][0].rowSpan = this.rowSpans[cols.ids[row.value][0].id];
        }
        if (cols.ids[row.value][1].rowSpan > 0) {
          cols.ids[row.value][1].rowSpan = this.rowSpans[cols.ids[row.value][1].id];
        }
        let ids = row.value.split(':');
        if (this.minMax[ids[0]]) {
          let min = this.minMax[ids[0]][0];
          let max = this.minMax[ids[0]][1];
          let start = cols.ids[row.value][2].start || this.currentYearMonth;
          let end = cols.ids[row.value][2].end || this.currentYearMonth;
          let diff = this.#toTime(max, false) - this.#toTime(min, true);
          // set the DCE min and max dates of the study
          cols.ids[row.value][2].min = min;
          cols.ids[row.value][2].max = max;
          // compute the progress
          cols.ids[row.value][2].progressStart = 100 * (this.#toTime(start, true) - this.#toTime(min, true)) / diff;
          cols.ids[row.value][2].progress = 100 * (this.#toTime(end, false) - this.#toTime(start, true)) / diff;
          cols.ids[row.value].index = i;
        }
      });
    }

    return cols;
  }

}

class CoverageResultParser {

  constructor(micaConfig, locale, normalizePath) {
    this.micaConfig = micaConfig;
    this.locale = locale;
    this.normalizePath = normalizePath;
  }

  decorateVocabularyHeaders(headers, vocabularyHeaders) {
    let count = 0, i = 0;
    for (let j = 0; j < vocabularyHeaders.length; j++) {
      if (count >= headers[i].termsCount) {
        i++;
        count = 0;
      }

      count += vocabularyHeaders[j].termsCount;
      vocabularyHeaders[j].taxonomyName = headers[i].entity.name;
    }
  }

  decorateTermHeaders(headers, termHeaders, attr) {
    let idx = 0;
    return headers.reduce(function (result, h) {
      result[h.entity.name] = termHeaders.slice(idx, idx + h.termsCount).map(function (t) {
        if (h.termsCount > 1 && attr === 'vocabularyName') {
          t.canRemove = true;
        }

        t[attr] = h.entity.name;

        return t;
      });

      idx += h.termsCount;
      return result;
    }, {});
  }

  parseHeaders(bucket, result) {
    let table = { cols: [] };
    let vocabulariesTermsMap = {};

    if (result && result.rows) {
      var tableTmp = result;
      tableTmp.cols = new IdSplitter(bucket, result, this.normalizePath).splitIds(this.micaConfig, this.locale);
      table = tableTmp;

      // TODO let filteredRows = [];
      // TODO let nextFilteredRowsPage = 0;
      // TODO $scope.loadMoreRows();

      vocabulariesTermsMap = this.decorateTermHeaders(table.vocabularyHeaders, table.termHeaders, 'vocabularyName');
      this.decorateTermHeaders(table.taxonomyHeaders, table.termHeaders, 'taxonomyName');
      this.decorateVocabularyHeaders(table.taxonomyHeaders, table.vocabularyHeaders);
      // TODO $scope.isFullCoverageImpossibleOrCoverageAlreadyFull();
    }

    return { table, vocabulariesTermsMap};
  }

  parse(data) {
    return data;
  }
}
