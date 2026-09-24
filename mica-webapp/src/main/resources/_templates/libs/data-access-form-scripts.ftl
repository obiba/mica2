<!-- Data access schemaform and dependencies -->

<script src="${contextPath}/bower_components/angular/angular.js"></script>
<script src="${contextPath}/bower_components/objectpath/lib/ObjectPath.js"></script>
<script src="${contextPath}/bower_components/marked/lib/marked.js"></script>
<script src="${contextPath}/bower_components/tv4/tv4.js"></script>
<script src="${contextPath}/bower_components/angular-sanitize/angular-sanitize.js"></script>
<script src="${contextPath}/bower_components/angular-marked/dist/angular-marked.js"></script>
<script src="${contextPath}/bower_components/angular-strap/dist/angular-strap.js"></script>
<script src="${contextPath}/bower_components/angular-strap/dist/angular-strap.tpl.js"></script>
<script src="${contextPath}/bower_components/moment/moment.js"></script>
<script src="${contextPath}/bower_components/moment/min/locales.min.js"></script>
<script src="${contextPath}/bower_components/angular-moment/angular-moment.js"></script>
<script src="${contextPath}/bower_components/angular-resource/angular-resource.js"></script>
<script src="${contextPath}/bower_components/angular-schema-form/dist/schema-form.js"></script>
<script src="${contextPath}/bower_components/angular-schema-form-bootstrap/bootstrap-decorator.min.js"></script>
<script src="${contextPath}/bower_components/angular-schema-form-ui-ace/bootstrap-ui-ace.min.js"></script>
<script src="${contextPath}/bower_components/angular-schema-form-datetimepicker/schema-form-date-time-picker.min.js"></script>
<script src="${contextPath}/bower_components/sf-localized-string/dist/sf-localized-string.min.js"></script>
<script src="${contextPath}/bower_components/sf-obiba-file-upload/dist/sf-obiba-file-upload.min.js"></script>
<script src="${contextPath}/bower_components/sf-checkboxgroup/dist/sf-checkboxgroup.min.js"></script>
<script src="${contextPath}/bower_components/filesize/lib/filesize.min.js"></script>
<script src="${contextPath}/bower_components/angular-bootstrap/ui-bootstrap-tpls.js"></script>
<script src="${contextPath}/bower_components/ng-file-upload/ng-file-upload.js"></script>
<script src="${contextPath}/bower_components/sf-typeahead/dist/sf-typeahead.min.js"></script>
<script src="${contextPath}/bower_components/angular-translate/angular-translate.js"></script>
<script src="${contextPath}/bower_components/angular-translate-loader-static-files/angular-translate-loader-static-files.js"></script>
<script src="${contextPath}/bower_components/sf-obiba-countries-ui-select/dist/sf-obiba-countries-ui-select.js"></script>
<script src="${contextPath}/bower_components/sf-obiba-selection-tree/dist/sf-obiba-selection-tree.js"></script>
<script src="${contextPath}/bower_components/sf-radio-group-collection/dist/sf-radio-group-collection.js"></script>

<script>
    const formSchema = ${formConfig.schema!"{}"};
    formSchema.readOnly = ${formConfig.readOnly?c};
    const formDefinition = ${formConfig.definition!"['*']"};
    const formModel = ${formConfig.model!"{}"};
    const formMessages = {
        validationSuccess: "<@message "form-validation-success"/>",
        validationError: "<@message "form-validation-error"/>",
        validationErrorOnSubmit: "<@message "form-validation-submit-error"/>",
        errorOnSave: "<@message "form-save-error"/>"
    };
</script>

<script src="${assetsPath}/js/mica-data-access-form.js"></script>
