package org.icgc.dcc.core.model;

import java.util.Date;

public class Timestamped implements HasTimestamps {

  protected Date created;

  protected Date lastUpdate;

  @Override
  public Date getLastUpdate() {
    return lastUpdate;
  }

  @Override
  public Date getCreated() {
    return created;
  }

}
