<#-- scripts: values are escaped by hand (js_string), not for HTML -->
<#outputformat "plainText">
<script>
  $(function () {
    const errorMessages = {
      'server.error.password.too-short': "${messageJs("server.error.password.too-short")}",
      'server.error.password.too-long': "${messageJs("server.error.password.too-long")}",
      'server.error.password.too-weak': "${messageJs("server.error.password.too-weak")}",
      'server.error.bad-request': "${messageJs("server.error.bad-request")}",
      'server.error.bad-captcha': "${messageJs("server.error.bad-captcha")}",
      'server.error.email-already-assigned': "${messageJs("server.error.email-already-assigned")}",
      'error.unhandledException': '${messageJs("server.error.unhandledException")}'
    };
    UserService.signup("form", requiredFields, function (message) {
      var alertId = "#alertFailure";
      var msg = message;
      if (Array.isArray(message)) {
        msg = "${messageJs("sign-up-fields-required")}: " + message.join(", ");
      } else if (errorMessages[msg]) {
        msg = errorMessages[msg];
      }
      $(alertId).html('<small>' + msg + '</small>').removeClass("d-none");
      setTimeout(function () {
        $(alertId).addClass("d-none");
      }, 5000);
    });
  });

</script>
</#outputformat>
