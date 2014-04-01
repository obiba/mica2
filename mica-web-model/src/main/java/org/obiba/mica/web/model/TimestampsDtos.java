package org.obiba.mica.web.model;

import org.obiba.mica.domain.Timestamped;

class TimestampsDtos {

  private TimestampsDtos() {}

  static Mica.TimestampsDto asDto(Timestamped timestamped) {
    Mica.TimestampsDto.Builder builder = Mica.TimestampsDto.newBuilder()
        .setCreated(timestamped.getCreated().toString());
    if(timestamped.getUpdated() != null) builder.setLastUpdate(timestamped.getUpdated().toString());
    return builder.build();
  }

}
