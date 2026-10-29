<#macro commentScripts isPrivate="false">
  <!-- EasyMDE -->
  <script src="${assetsPath}/libs/node_modules/easymde/dist/easymde.min.js"></script>

  <script>
      var easyMDEDefaults = {
          autoDownloadFontAwesome: false,
          renderingConfig: {
              singleLineBreaks: false,
              codeSyntaxHighlighting: false,
          },
          toolbar: ["bold", "italic",
              {
                  name: "heading",
                  className: "fa fa-heading",
                  action: EasyMDE.toggleHeadingSmaller,
                  title: "Heading"
              },
              "code", "quote", "unordered-list", "ordered-list", "link",
              {
                  name: "image",
                  className: "fa fa-image",
                  action: EasyMDE.drawImage,
                  title: "Insert Image"

              }, "table", "|", "preview"],
          status: false,
          // prefixed button classes ("mde-table"...), the plain names clash with Bootstrap classes (.table, .link...)
          toolbarButtonClassPrefix: "mde"
      };
      $(function () {
          let newCommentMDE = new EasyMDE(
              Object.assign(easyMDEDefaults,
                  {element: $("#comment-add-write-text")[0]})
          );
          $("#send-comment").click(function() {
              console.log(newCommentMDE.value());
              DataAccessService.sendComment('${dar.id}', newCommentMDE.value(), ${isPrivate})
          });
      });
  </script>

</#macro>

