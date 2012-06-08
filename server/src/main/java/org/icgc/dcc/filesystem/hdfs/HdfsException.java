package org.icgc.dcc.filesystem.hdfs;

public class HdfsException extends RuntimeException {
  private static final long serialVersionUID = -2204845409873632221L;

  public HdfsException(Exception e) {
    super(e);
  }

  public HdfsException(String message) {
    super(message);
  }

  public HdfsException(String message, Exception e) {
    super(message, e);
  }
}
