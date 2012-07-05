package org.icgc.dcc.core.model;

import java.util.Date;

public class Timestamped implements HasTimestamps {

  protected Date created;

  protected Date lastUpdate;

  @Override
  public Date getLastUpdate() {
    // Date is mutable
    return new Date(lastUpdate.getTime());
  }

  @Override
  public Date getCreated() {
    // Date is mutable
    return new Date(created.getTime());
  }

}
