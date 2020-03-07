<script>
  $(function () {
    micajs.stats('datasets', {query: "dataset(in(Mica_dataset.id,${dataset.id}))"}, function (stats) {
      $('#network-hits').text(new Intl.NumberFormat().format(stats.networkResultDto.totalHits));
      $('#study-hits').text(new Intl.NumberFormat().format(stats.studyResultDto.totalHits));
      $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
    });

    <#if type == "Harmonized">
      micajs.dataset.harmonizedVariables('${dataset.id}', 0, 100, function(data) {
        $('#loadingSummary').hide();
        if (data.variableHarmonizations) {
          const theadTr = $('#harmonizedTable > thead > tr');
          const appendStudyTableHeader = function(studySummary, name, description) {
            const nameLocale = localizedString(name);
            const descriptionLocale = localizedString(description);
            const acronym = localizedString(studySummary.acronym);
            const descriptionTag = descriptionLocale.length === 0 ? '' : '<i class="fas fa-info-circle" title="' + descriptionLocale + '"></i>'
            theadTr.append('<th><small><a href="../study/' + studySummary.id + '">'  + acronym + '</a> ' + nameLocale + ' ' + descriptionTag + '</small></th>');
          };

          if (data.studyTable) {
            for (const i in data.studyTable) {
              const st = data.studyTable[i];
              appendStudyTableHeader(st.studySummary, st.name, st.description);
            }
          }
          if (data.harmonizationStudyTable) {
            for (const i in data.harmonizationStudyTable) {
              const st = data.harmonizationStudyTable[i];
              appendStudyTableHeader(st.studySummary, st.name, st.description);
            }
          }

          for (const i in data.variableHarmonizations) {
            $('#harmonizedTable > tbody').append($('<tr />'));
            const variableHarmonization = data.variableHarmonizations[i];
            const name = variableHarmonization.dataschemaVariableRef.name;
            $('#harmonizedTable > tbody > tr:last-child').append('<td><small><a href="../variable/${dataset.id}:' + name + ':Dataschema">' + name + '</a></small></td>');
            for (const j in variableHarmonization.harmonizedVariables) {
              const harmonizedVariable = variableHarmonization.harmonizedVariables[j];
              let iconClass = 'fas fa-minus text-muted';
              if (harmonizedVariable.status === 'complete') {
                iconClass = 'fas fa-check text-success';
              } else if (harmonizedVariable.status === 'impossible') {
                iconClass = 'fas fa-times text-danger';
              } else if (harmonizedVariable.status === 'undetermined') {
                iconClass = 'fas fa-question text-warning';
              }
              const url = harmonizedVariable.harmonizedVariableRef ? '../variable/' + harmonizedVariable.harmonizedVariableRef.id : '#';
              $('#harmonizedTable > tbody > tr:last-child').append('<td><small><a href="' + url + '"><i class="' + iconClass + '"></i></a></small></td>');
            }
          }

          $('#harmonizedTable').show();
          const dataTableOpts = {
            "paging": true,
            "pageLength": 25,
            "lengthChange": true,
            "searching": false,
            "ordering": false,
            "info": false,
            "autoWidth": true,
            "language": {
              "url": "../assets/i18n/datatables.${.lang}.json"
            }
          };
          var table = $("#harmonizedTable").DataTable(dataTableOpts);
          $('#harmonizedTable').on( 'page.dt', function () {
            var info = table.page.info();
            console.dir(info);
          } );
        }


      });
    </#if>
  });
</script>
