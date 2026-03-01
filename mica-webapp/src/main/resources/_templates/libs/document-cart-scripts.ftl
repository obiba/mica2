<#assign set = sets.variablesCart.set/>
<#assign studiesSet = sets.studiesCart.set/>
<#assign networksSet = sets.networksCart.set/>

<#include "document-set-scripts.ftl">

<script>
  function onCompareStudies() {
    const selections = studiesCartStorage.getSelections();
    window.location = '${contextPath}/compare?type=studies' + (selections.length === 0 ? '' : '&ids=' + selections.join(','));
  }

  function onCompareNetworks() {
    const selections = networksCartStorage.getSelections();
    window.location = '${contextPath}/compare?type=networks' + (selections.length === 0 ? '' : '&ids=' + selections.join(','));
  }

  function onClickAddToSet(id, name) {
    addToSet(id, name);
  }

  function onClickAddToNewSet() {
    const input = document.getElementById('newVariableSetName');

    if ((input.value || "").trim().length > 0) {
      addToSet(null, input.value.trim());
    }
  }

  function onKeyUpAddToNewSet(event) {
    const keyCode = event.keyCode ? event.keyCode : event.which ? event.which : event.charCode;
    const input = document.getElementById('newVariableSetName');
    const button = document.getElementById('addToNewSetButton');

    if (input) {
      if (button && (input.value || "").trim().length > 0) {
        button.className = "btn btn-success";
      } else {
        button.className = "btn btn-success disabled";
      }

      if (keyCode === 13) { // enter Key
        event.stopPropagation();
        event.preventDefault();
        addToSet(null, input.value.trim());
      }
    }
  }

  function normalizeSetName(set) {
    if (set.name.startsWith('dar:')) {
      return set.name.replace('dar:', '') + ' [' + Mica.tr['data-access-request'] + ']';
    }
    return set.name;
  }

  function addToSet(setId, setName) {
    const onsuccess = (set, oldSet) => {
      if (set.count === oldSet.count) {
        MicaService.toastInfo(Mica.tr['no-variable-added-set'].replace('{{arg0}}', '"' + normalizeSetName(set) + '"'));
      } else {
        MicaService.toastSuccess(Mica.tr['variables-added-to-set'].replace('{0}', (set.count - oldSet.count).toLocaleString(Mica.locale)).replace('{1}', '"' + normalizeSetName(set) + '"'));
      }
      VariablesSetService.showSetsCount($('#list-count'), sets => {
        $('#add-set-divider').show();
        const choicesElem = $('#add-set-choices');
        choicesElem.empty();
        sets.filter(set => !set.locked).forEach(set => {
          const btn = "<button type=\"button\" class=\"dropdown-item\"" +
                  "            onclick=\"onClickAddToSet('" + set.id + "', '" + normalizeSetName(set) + "')\">" +
                  "          " + normalizeSetName(set) + " <span class=\"badge text-bg-light float-end\">" + set.count + "</span>" +
                  "</button>";
          choicesElem.append(btn);
        });
        $('#newVariableSetName').val('');
      });
    };

    const selections = variablesCartStorage.getSelections();
    if (selections.length === 0) {
      VariablesSetService.addQueryToSet(setId, setName, 'variable(in(Mica_variable.sets,${set.id}),limit(0,100000),fields(variableType))', onsuccess);
    } else {
      VariablesSetService.addToSet(setId, setName, selections, onsuccess);
    }
  }

  $(function () {
    const newVariableSetNameInput = document.getElementById('newVariableSetName');
    if (newVariableSetNameInput) {
      newVariableSetNameInput.addEventListener('keyup', onKeyUpAddToNewSet);
    }

    const listsDropdownMenu = document.getElementById('listsDropdownMenu');
    if (listsDropdownMenu) {
      listsDropdownMenu.addEventListener('click', event => event.stopPropagation());
    }
  });
</script>
