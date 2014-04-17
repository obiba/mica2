package org.obiba.mica.web.model;

import org.obiba.mica.domain.Timestamped;

class TimestampsDtos {

  private TimestampsDtos() {}

  static Mica.TimestampsDto asDto(Timestamped timestamped) {
    Mica.TimestampsDto.Builder builder = Mica.TimestampsDto.newBuilder()
        .setCreated(timestamped.getCreatedDate().toString());
    if(timestamped.getLastModifiedDate() != null) builder.setLastUpdate(timestamped.getLastModifiedDate().toString());
    return builder.build();
  }

}
