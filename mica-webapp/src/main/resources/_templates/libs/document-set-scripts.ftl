<!-- Script for the cart and the documents list pages -->
<script src="${assetsPath}/js/mica-tables.js"></script>

<script>
  const variablesCartStorage = new MicaSetStorage('cart');

  // cart
  <#if cartEnabled>
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

    const dataTableOpts = {
      "paging": true,
      lengthMenu: [10, 20, 50, 100],
      pageLength: 20,
      "lengthChange": true,
      "searching": false,
      "ordering": false,
      "autoWidth": true,
      "language": {
        "url": "${assetsPath}/i18n/datatables.${.lang}.json"
      },
      "processing": true,
      "serverSide": true,
      columnDefs: [{ // the checkbox
        orderable: false,
        className: 'select-checkbox',
        targets: 0
      }, { // the ID
          visible: false,
          searchable: false,
          targets: 1
      }],
      <#if !set.locked || isAdministrator>
      select: {
        style: 'multi',
        selector: 'td:first-child',
        info: false
      },
      </#if>
      "ajax": function(data, callback) {
        VariablesSetService.search('${set.id}', data.start, data.length, function(response) {
          $('#loadingSet').hide();
          $('#setTable').show();
          if (response.variableResultDto && response.variableResultDto['obiba.mica.DatasetVariableResultDto.result']) {
            const result = response.variableResultDto;
            let rows = [];
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
            callback({
              data: rows,
              recordsTotal: result.totalHits,
              recordsFiltered: result.totalHits
            });
          }
        });
      },
      "fixedHeader": true,
      dom: "<'row'<'col-sm-3'l><'col-sm-3'f><'col-sm-6'p>><'row'<'table-responsive col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>",
      "info": true
    };

    initSelectDataTable($("#setTable").DataTable(dataTableOpts), {
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

  });
</script>
