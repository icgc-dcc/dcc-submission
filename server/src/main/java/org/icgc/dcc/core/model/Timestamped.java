package org.icgc.dcc.core.model;

import java.util.Date;

public class Timestamped implements HasTimestamps {

  protected Date created;

  protected Date lastUpdate;

  @Override
  public Date getLastUpdate() {
    // Date is mutable
    return lastUpdate != null ? new Date(lastUpdate.getTime()) : null;
  }

  @Override
  public Date getCreated() {
    // Date is mutable
    return created != null ? new Date(created.getTime()) : null;
  }

}
