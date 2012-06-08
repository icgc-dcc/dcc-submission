package org.icgc.dcc.filesystem.exception;

public class ReleaseFileSystemException extends RuntimeException {
  private static final long serialVersionUID = -7689630986237099649L;

  public ReleaseFileSystemException(Exception e) {
    super(e);
  }

  public ReleaseFileSystemException(String message) {
    super(message);
  }

  public ReleaseFileSystemException(String message, Exception e) {
    super(message, e);
  }
}
