'use strict';

mica.attachment

  .directive('attachmentInput', ['$upload', '$timeout', '$log', 'TempFileResource',
    function ($upload, $timeout, $log, TempFileResource) {
    return {
      restrict: 'E',
      require: '^form',
      scope: {
        multiple: '@',
        accept: '@',
        files: '='
      },
      templateUrl: 'app/commons/attachment/attachment-input-template.html',
      controller: function ($scope) {
        $scope.onFileSelect = function ($file) {
          $scope.uploadedFiles = $file;
          $scope.uploadedFiles.forEach(function (file) {
            uploadFile(file);
          });
        };

        var uploadFile = function (file) {
          $log.debug('file', file);

          var attachment = {
            showProgressBar: true,
            lang: 'en',
            progress: 0,
            file: file,
            fileName: file.name,
            size: file.size
          };

          if($scope.multiple) {
            $scope.files.push(attachment);
          } else {
            $scope.files.splice(0, $scope.files.length);
            $scope.files.push(attachment);
          }

          $scope.upload = $upload
            .upload({
              url: '/ws/files/temp',
              method: 'POST',
              file: file
            })
            .progress(function (evt) {
              attachment.progress = parseInt(100.0 * evt.loaded / evt.total);
            })
            .success(function (data, status, getResponseHeaders) {
              var parts = getResponseHeaders().location.split('/');
              var fileId = parts[parts.length - 1];
              TempFileResource.get(
                {id: fileId},
                function (tempFile) {
                  $log.debug('tempFile', tempFile);
                  attachment.id = tempFile.id;
                  attachment.md5 = tempFile.md5;
                  attachment.justUploaded = true;
                  // wait for 1 second before hiding progress bar
                  $timeout(function () { attachment.showProgressBar = false; }, 1000);
                }
              );
            });
        };

        $scope.deleteTempFile = function (tempFileId) {
          TempFileResource.delete(
            {id: tempFileId},
            function () {
              for (var i = $scope.files.length; i--;) {
                var attachment = $scope.files[i];
                if (attachment.justUploaded && attachment.id === tempFileId) {
                  $scope.files.splice(i, 1);
                }
              }
            }
          );
        };

        $scope.deleteFile = function (fileId) {
          for(var i = $scope.files.length; i--;) {
            if($scope.files[i].id === fileId) {
              $scope.files.splice(i, 1);
            }
          }
        };
      }
    };
  }]);
