package org.icgc.dcc.filesystem.exception;

public class DccFileSystemException extends RuntimeException {
  private static final long serialVersionUID = 1680629270933172614L;

  public DccFileSystemException(Exception e) {
    super(e);
  }

  public DccFileSystemException(String message) {
    super(message);
  }

  public DccFileSystemException(String message, Exception e) {
    super(message, e);
  }
}
