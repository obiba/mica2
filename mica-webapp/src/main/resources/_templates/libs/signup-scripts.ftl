<script>
  $(function () {
    const errorMessages = {
      'server.error.password.too-short': "<@message "server.error.password.too-short"/>",
      'server.error.bad-request': "<@message "server.error.bad-request"/>",
      'server.error.bad-captcha': "<@message "server.error.bad-captcha"/>",
      'server.error.email-already-assigned': "<@message "server.error.email-already-assigned"/>",
      'error.unhandledException': '<@message "server.error.unhandledException"/>'
    };
    UserService.signup("form", requiredFields, function (message) {
      var alertId = "#alertFailure";
      var msg = message;
      if (Array.isArray(message)) {
        msg = "<@message "sign-up-fields-required"/>: " + message.join(", ");
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
