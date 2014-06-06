package org.obiba.mica.web.model;

import javax.validation.constraints.NotNull;

import org.obiba.mica.service.file.TempFile;
import org.springframework.stereotype.Component;

@Component
class TempFileDtos {

  @NotNull
  Mica.TempFileDto asDto(@NotNull TempFile tempFile) {
    return Mica.TempFileDto.newBuilder().setName(tempFile.getName()).setSize(tempFile.getSize())
        .setMd5(tempFile.getMd5()).build();
  }

}
