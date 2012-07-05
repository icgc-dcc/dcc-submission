package org.icgc.dcc.core.model;

import java.util.Date;

public interface HasTimestamps {

  public Date getLastUpdate();

  public Date getCreated();
}
