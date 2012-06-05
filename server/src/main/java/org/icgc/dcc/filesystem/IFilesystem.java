package org.icgc.dcc.filesystem;

import java.util.List;

/**
 * Interface that abstracts away which filesystem is used (hdfs or local)
 * 
 * we may use http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html later instead. we'll likely rename
 * that interface later
 */
public interface IFilesystem {
  List<String> ls();
}
