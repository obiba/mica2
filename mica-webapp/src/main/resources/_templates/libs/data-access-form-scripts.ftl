<!-- Data access form: schema, definition and model (t() tokens resolved by the server) -->
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

<!-- Vue + Quasar json-form bundle, see mica-webapp/src/main/vue/data-access-form -->
<script type="module" src="${assetsPath}/js/data-access-form/mica-data-access-form.js"></script>
<script type="module">
    MicaDataAccessForm.mount('#data-access-form', {
        schema: formSchema,
        definition: formDefinition,
        model: formModel,
        readOnly: formSchema.readOnly,
        lang: '${.lang}',
        contextPath: contextPath,
        messages: formMessages
    });
</script>
