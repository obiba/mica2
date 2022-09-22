<!-- Datepicker -->
<script src="${assetsPath}/libs/node_modules/bootstrap-datepicker/dist/js/bootstrap-datepicker.min.js"></script>
<!-- Autocomplete -->
<script src="${assetsPath}/libs/node_modules/bootstrap-4-autocomplete/dist/bootstrap-4-autocomplete.min.js"></script>
<script>
  $(function () {
    $('#dashboard-menu').addClass('active').attr('href', '#');
    $("#amendments").DataTable(dataTablesSortOpts);
    $('#start-date').datepicker({
      locale: '${.lang}',
      format: 'yyyy-mm-dd'
    });
    $('#start-date-submit').click(function() {
      DataAccessService.setStartDate('${dar.id}', $('#start-date').val());
    });
    var suggestions = {
      <#list suggestedCollaborators as suggestion>
      '${suggestion}': '${suggestion}',
      </#list>
    };
    $('#collaborator-email').autocomplete({
      source: suggestions,
      treshold: 2
    });
  });
</script>
