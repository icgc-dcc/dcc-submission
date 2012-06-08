package org.icgc.dcc.filesystem.hdfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

/**
 * Handles all hadoop API related methods
 */
public class HadoopUtils {

  public static String getConfigurationDescription(Configuration configuration) throws IOException {
    final Writer writer = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(writer);
    Configuration.dumpConfiguration(configuration, printWriter);
    String content = writer.toString();
    printWriter.close();
    writer.close();
    return content;
  }

  public static void mkdirs(FileSystem fileSystem, String stringPath) {
    Path path = new Path(stringPath);
    boolean mkdirs;
    try {
      mkdirs = fileSystem.mkdirs(path);
    } catch(IOException e) {
      throw new HdfsException(e);
    }
    if(!mkdirs) {
      throw new HdfsException("could not create " + stringPath);
    }
  }

  public static void touch(FileSystem fileSystem, String stringPath, InputStream in) {
    Path path = new Path(stringPath);
    FSDataOutputStream out = null;
    try {
      out = fileSystem.create(path);
      ByteStreams.copy(in, out);
    } catch(IOException e) {
<<<<<<< HEAD
      throw new HdfsException(e);
=======
      throw new HdfsException();
    } finally {
      Closeables.closeQuietly(out);
>>>>>>> a8a41aa36b99e75aac22309bac304ffc6cb26349
    }
  }

  public static void rm(FileSystem fileSystem, String stringPath) {
    rm(fileSystem, stringPath, false);
  }

  public static void rmr(FileSystem fileSystem, String stringPath) {
    rm(fileSystem, stringPath, true);
  }

  private static void rm(FileSystem fileSystem, String stringPath, boolean recursive) {
    boolean delete;
    try {
      Path path = new Path(stringPath);
      delete = fileSystem.delete(path, recursive);
    } catch(IOException e) {
      throw new HdfsException(e);
    }
    if(!delete) {
      throw new HdfsException("could not remove " + stringPath);
    }
  }

  public static boolean checkExistence(FileSystem fileSystem, String stringPath) {
    Path path = new Path(stringPath);
    boolean exists;
    try {
      exists = fileSystem.exists(path);
    } catch(IOException e) {
      throw new HdfsException(e);
    }
    return exists;
  }

  /**
   * non-recursively
   */
  public static List<Path> ls(FileSystem fileSystem, String stringPath, Pattern pattern) {
    Path path = new Path(stringPath);
    FileStatus[] listStatus;
    try {
      listStatus = fileSystem.listStatus(path);
    } catch(IOException e) {
      throw new HdfsException(e);
    }
    List<Path> ls = new ArrayList<Path>();
    for(FileStatus fileStatus : listStatus) {
      String filename = fileStatus.getPath().getName();
      if(null == pattern || pattern.matcher(filename).matches()) {
        ls.add(fileStatus.getPath());
      }
    }
    return ls;
  }

  public static List<Path> ls(FileSystem fileSystem, String stringPath) {
    return ls(fileSystem, stringPath, null);
  }

  public static List<String> toFilenameList(List<Path> pathList) {
    List<String> filenameList = new ArrayList<String>();
    for(Path path : pathList) {
      filenameList.add(path.getName());
    }
    return filenameList;
  }
}
