<script>
  UserService.signin("form", "otp", function (response, banned) {
    if (response.status === 401 && response.headers["www-authenticate"] === "X-Obiba-TOTP") {
      $("#otp").val("");
      $("#alertOtpFailure").addClass("d-none");
      $("#signInCard").hide();
      $("#2faCard").show();
      if (response.data?.image) {
        $("#2faImage").show();
        $("#qr-img").attr("src", response.data.image);
      } else {
        $("#2faImage").hide();
        $("#qr-img").attr("src", "");
      }
      if (response.data?.email) {
        $("#2faEmail").show();
      } else {
        $("#2faEmail").hide();
      }
      // the separator only means something when there are instructions above it
      $("#2faSeparator").toggle($("#2faImage").is(":visible") || $("#2faEmail").is(":visible"));
    } else {
      $("#otp").val("");
      let alertId;
      if (!banned && $("#2faCard").is(":visible")) {
        // password was already accepted to get here: only the code can be wrong
        alertId = "#alertOtpFailure";
      } else {
        $("#signInCard").show();
        $("#2faCard").hide();
        $("#2faImage").hide();
        $("#qr-img").attr("src", "");
        alertId = banned ? "#alertBanned" : "#alertFailure";
      }
      $(alertId).removeClass("d-none");
      setTimeout(function () {
        $(alertId).addClass("d-none");
      }, 5000);
    }
  });

  const validateOtp = () => {
    $("#form").submit();
  };

  const cancelOtp = () => {
    $("#otp").val("");
    $("#alertOtpFailure").addClass("d-none");
    $("#signInCard").show();
    $("#2faCard").hide();
    $("#2faImage").hide();
    $("#qr-img").attr("src", "");
  };

  $('#otp').keypress((e) => {
    if (e.which === 13) {
      validateOtp();
    }
  });
</script>
