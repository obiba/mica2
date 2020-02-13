<#macro commentScripts isPrivate="false">
  <!-- SimpleMDE -->
  <script src="${pathPrefix!".."}/bower_components/simplemde/dist/simplemde.min.js"></script>

  <script>
      var simpleMDEDefaults = {
          autoDownloadFontAwesome: false,
          renderingConfig: {
              singleLineBreaks: false,
              codeSyntaxHighlighting: false,
          },
          toolbar: ["bold", "italic",
              {
                  name: "heading",
                  className: "fa fa-heading",
                  action: SimpleMDE.toggleHeadingSmaller,
                  title: "Heading"
              },
              "code", "quote", "unordered-list", "ordered-list", "link",
              {
                  name: "image",
                  className: "fa fa-image",
                  action: SimpleMDE.drawImage,
                  title: "Insert Image"

              }, "table", "|", "preview"],
          status: false
      };
      $(function () {
          let newCommentMDE = new SimpleMDE(
              Object.assign(simpleMDEDefaults,
                  {element: $("#comment-add-write-text")[0]})
          );
          $("#send-comment").click(function() {
              console.log(newCommentMDE.value());
              micajs.dataAccess.sendComment(${dar.id}, newCommentMDE.value(), ${isPrivate})
          });
      });
  </script>

</#macro>

