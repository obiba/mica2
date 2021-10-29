<script>
  $(function () {
    const errorMessages = {
      "server.error.bad-request": "<@message "server.error.bad-request"/>",
      "server.error.bad-captcha": "<@message "server.error.bad-captcha"/>",
    };
    const requiredFields = [
      { name: "name", title: "<@message "contact-name"/>" },
      { name: "email", title: "<@message "contact-email"/>" },
      { name: "subject", title: "<@message "contact-subject"/>" },
      { name: "message", title: "<@message "contact-message"/>" },
      { name: "g-recaptcha-response", title: "<@message "captcha"/>" }
    ];
    UserService.contact("#form", requiredFields, function() {
      let form = document.querySelector("#form");
      if (form) {
        window.location.href = window.location.origin + "/page/contact-success"
      }
    }, function (messageItems) {
      var alertId = "#alertFailure";
      var msg = "";
      if (Array.isArray(messageItems)) {
        msg = "<@message "contact-fields-required"/>: " + messageItems.map(item => item.title).join(", ");
        messageItems.map(item => item.name).forEach(itemName => {
          $('#contact-' + itemName).addClass('is-invalid');
        });
      } else if (messageItems.title && errorMessages[messageItems.title]) {
        msg = errorMessages[messageItems.title];
        $('#contact-' + messageItems.name).addClass('is-invalid');
      }
      $(alertId).html('<small>' + msg + '</small>').removeClass("d-none");
      setTimeout(function() {
        $(alertId).addClass("d-none");
      }, 5000);
    });
  });
</script>
