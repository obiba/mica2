package org.obiba.mica.web.model;

import javax.validation.constraints.NotNull;

import org.obiba.mica.file.TempFile;
import org.springframework.stereotype.Component;

@Component
class TempFileDtos {

  @NotNull
  Mica.TempFileDto asDto(@NotNull TempFile tempFile) {
    return Mica.TempFileDto.newBuilder().setId(tempFile.getId()).setName(tempFile.getName()).setSize(tempFile.getSize())
        .setMd5(tempFile.getMd5()).build();
  }

}
