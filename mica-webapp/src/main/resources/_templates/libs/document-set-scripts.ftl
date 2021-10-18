<!-- Script for the cart and the documents list pages -->
<script src="${assetsPath}/js/mica-tables.js"></script>

<script>
  const variablesCartStorage = new MicaSetStorage('cart');
  const studiesCartStorage = new MicaSetStorage('scart');
  const networksCartStorage = new MicaSetStorage('ncart');

  // cart
  <#if variablesCartEnabled>
  const onVariablesCartAdd = function(id) {
    VariablesSetService.addQueryToCart('variable(in(Mica_variable.sets,' + id + '),limit(0,10000),fields(variableType))', function(cart, oldCart) {
      VariablesSetService.showCount('#cart-count', cart, '${.lang}');
      if (cart.count === oldCart.count) {
        MicaService.toastInfo("<@message "sets.cart.no-variable-added"/>");
      } else {
        MicaService.toastSuccess("<@message "variables-added-to-cart"/>".replace('{0}', (cart.count - oldCart.count).toLocaleString('${.lang}')));
      }
    });
  };
  </#if>

  $(function () {

    // clear any previous selections from local storage
    variablesCartStorage.deselectAll();
    studiesCartStorage.deselectAll();
    networksCartStorage.deselectAll();

    // base documents data table options
    const dataTableOpts = {
      paging: true,
      lengthMenu: [10, 20, 50, 100],
      pageLength: 20,
      lengthChange: true,
      searching: false,
      ordering: false,
      autoWidth: true,
      language: {
        url: "${assetsPath}/i18n/datatables.${.lang}.json"
      },
      processing: true,
      serverSide: true,
      columnDefs: [{ // the checkbox
        orderable: false,
        className: 'select-checkbox',
        targets: 0
      }, { // the ID
          visible: false,
          searchable: false,
          targets: 1
      }],
      select: {
        style: 'multi',
        selector: 'td:first-child',
        info: false
      },
      fixedHeader: true,
      dom: "<'row'<'col-sm-3'l><'col-sm-3'f><'col-sm-6'p>><'row'<'table-responsive col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>",
      info: true
    };

    // variables set
    const variablesDataTableOpts = {...dataTableOpts};
    variablesDataTableOpts.ajax = function(data, callback) {
      VariablesSetService.search('${set.id}', data.start, data.length, function(response) {
        $('#loadingSet').hide();
        $('#setTable').show();
        let result = undefined;
        let rows = [];
        if (response.variableResultDto && response.variableResultDto['obiba.mica.DatasetVariableResultDto.result']) {
          result = response.variableResultDto;
          const summaries = result['obiba.mica.DatasetVariableResultDto.result'].summaries;
          for (const i in summaries) {
            let row = [];
            const summary = summaries[i];
            // checkbox
            if (${set.locked?c} && !${isAdministrator?c}) {
              row.push('');
            } else {
              row.push('<i class="far fa-square"></i>');
            }
            // ID
            row.push(summary.id);
            row.push('<a href="${contextPath}/variable/' + summary.id + '">' + summary.name + '</a>');
            row.push(summary.variableLabel ? LocalizedValues.forLang(summary.variableLabel, '${.lang}') : '');
            <#if config.studyDatasetEnabled && config.harmonizationDatasetEnabled>
            row.push(summary.variableType);
            </#if>
            <#if !config.singleStudyEnabled>
            row.push('<a href="${contextPath}/study/' + summary.studyId + '">' + LocalizedValues.forLang(summary.studyAcronym, '${.lang}') + '</a>');
            </#if>
            row.push('<a href="${contextPath}/dataset/' + summary.datasetId + '">' + LocalizedValues.forLang(summary.datasetAcronym, '${.lang}') + '</a>');
            rows.push(row);
          }
        }
        callback({
          data: rows,
          recordsTotal: result.totalHits,
          recordsFiltered: result.totalHits
        });
      });
    };
    <#if set.locked && !isAdministrator>
    delete variablesDataTableOpts.select;
    </#if>
    initSelectDataTable($("#setTable").DataTable(variablesDataTableOpts), {
      isSelected: function(id) {
        return variablesCartStorage.selected(id);
      },
      onSelectionChanged: function (ids, selected) {
        if (selected) {
          variablesCartStorage.selectAll(ids);
        } else {
          variablesCartStorage.deselectAll(ids);
        }
        const count = variablesCartStorage.getSelections().length;
        if (count === 0) {
          $('.selection-count').hide();
          $('#delete-all-message').show();
          $('#delete-selected-message').hide();
        } else {
          $('.selection-count').text(count.toLocaleString('${.lang}')).show();
          $('#delete-all-message').hide();
          $('#delete-selected-message').show();
        }
      }
    });
    $('#setTable').css('width', '100%').hide();

    <#if studiesSet??>
    // studies set
    const studyTypeLabels = {
      'individual-study': "<@message "search.study.individual"/>",
      'harmonization-study': "<@message "search.study.harmonization"/>"
    }
    const studiesDataTableOpts = {...dataTableOpts};
    studiesDataTableOpts.ajax = function(data, callback) {
      StudiesSetService.search('${studiesSet.id}', data.start, data.length, function(response) {
        $('#loadingStudiesSet').hide();
        $('#studiesSetTable').show();
        let result = undefined;
        let rows = [];
        if (response.studyResultDto && response.studyResultDto['obiba.mica.StudyResultDto.result']) {
          result = response.studyResultDto;
          const summaries = result['obiba.mica.StudyResultDto.result'].summaries;
          for (const i in summaries) {
            let row = [];
            const summary = summaries[i];
            // checkbox
            row.push('<i class="far fa-square"></i>');
            // ID
            row.push(summary.id);
            row.push('<a href="${contextPath}/study/' + summary.id + '">' + LocalizedValues.forLang(summary.acronym, '${.lang}') + '</a>');
            row.push(LocalizedValues.forLang(summary.name, '${.lang}'));
            row.push(studyTypeLabels[summary.studyResourcePath]);
            rows.push(row);
          }
        }
        callback({
          data: rows,
          recordsTotal: result ? result.totalHits : 0,
          recordsFiltered: result ? result.totalHits : 0
        });
      });
    };
    studiesDataTableOpts.columnDefs = [{ // the checkbox
      orderable: false,
      className: 'studies-select-checkbox',
      targets: 0
    }, { // the ID
      visible: false,
      searchable: false,
      targets: 1
    }];
    initSelectDataTable($("#studiesSetTable").DataTable(studiesDataTableOpts), {
      className: 'studies-select-checkbox',
      isSelected: function(id) {
        return studiesCartStorage.selected(id);
      },
      onSelectionChanged: function (ids, selected) {
        if (selected) {
          studiesCartStorage.selectAll(ids);
        } else {
          studiesCartStorage.deselectAll(ids);
        }
        const count = studiesCartStorage.getSelections().length;
        if (count === 0) {
          $('.studies-selection-count').hide();
          $('#delete-all-studies-message').show();
          $('#delete-selected-studies-message').hide();
        } else {
          $('.studies-selection-count').text(count.toLocaleString('${.lang}')).show();
          $('#delete-all-studies-message').hide();
          $('#delete-selected-studies-message').show();
        }
      }
    });
    $('#studiesSetTable').css('width', '100%').hide();
    </#if>

    <#if networksSet??>
    // networks set
    const networksDataTableOpts = {...dataTableOpts};
    networksDataTableOpts.ajax = function(data, callback) {
      NetworksSetService.search('${networksSet.id}', data.start, data.length, function(response) {
        $('#loadingNetworksSet').hide();
        $('#networksSetTable').show();
        let result = undefined;
        let rows = [];
        if (response.networkResultDto && response.networkResultDto['obiba.mica.NetworkResultDto.result']) {
          result = response.networkResultDto;
          const summaries = result['obiba.mica.NetworkResultDto.result'].networks;
          for (const i in summaries) {
            let row = [];
            const summary = summaries[i];
            // checkbox
            row.push('<i class="far fa-square"></i>');
            // ID
            row.push(summary.id);
            row.push('<a href="${contextPath}/network/' + summary.id + '">' + LocalizedValues.forLang(summary.acronym, '${.lang}') + '</a>');
            row.push(LocalizedValues.forLang(summary.name, '${.lang}'));
            rows.push(row);
          }
        }
        callback({
          data: rows,
          recordsTotal: result ? result.totalHits : 0,
          recordsFiltered: result ? result.totalHits : 0
        });
      });
    };
    initSelectDataTable($("#networksSetTable").DataTable(networksDataTableOpts), {
      isSelected: function(id) {
        return networksCartStorage.selected(id);
      },
      onSelectionChanged: function (ids, selected) {
        if (selected) {
          networksCartStorage.selectAll(ids);
        } else {
          networksCartStorage.deselectAll(ids);
        }
        const count = networksCartStorage.getSelections().length;
        if (count === 0) {
          $('.networks-selection-count').hide();
          $('#delete-all-networks-message').show();
          $('#delete-selected-networks-message').hide();
        } else {
          $('.networks-selection-count').text(count.toLocaleString('${.lang}')).show();
          $('#delete-all-networks-message').hide();
          $('#delete-selected-networks-message').show();
        }
      }
    });
    $('#networksSetTable').css('width', '100%').hide();
    </#if>

  });
</script>
