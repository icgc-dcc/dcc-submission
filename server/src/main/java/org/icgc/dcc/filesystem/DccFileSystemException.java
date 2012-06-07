package org.icgc.dcc.filesystem;

import org.apache.hadoop.fs.FileSystem;

public class DccFileSystemException extends RuntimeException {
  private static final long serialVersionUID = 1680629270933172614L;

  @SuppressWarnings("unused")
  private final FileSystem fileSystem;

  @SuppressWarnings("unused")
  private final String root;

  public DccFileSystemException(FileSystem fileSystem, String root) {
    this.fileSystem = fileSystem;
    this.root = root;
  }

  @Override
  public String getMessage() {
    return "";// TODO
  }
}
