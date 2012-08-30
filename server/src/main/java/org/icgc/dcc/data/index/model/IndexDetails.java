package org.icgc.dcc.data.index.model;

public class IndexDetails {

  public String _index;

  public String _type;

  public Object _id;

  public IndexDetails(String index, String type) {
    this._index = index;
    this._type = type;
  }
}
