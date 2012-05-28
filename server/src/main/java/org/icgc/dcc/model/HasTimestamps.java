package org.icgc.dcc.model;

import java.util.Date;

public interface HasTimestamps {

  public Date getLastUpdate();

  public Date getCreated();
}
