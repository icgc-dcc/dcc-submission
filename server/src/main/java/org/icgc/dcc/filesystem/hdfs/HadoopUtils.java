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
import org.apache.hadoop.fs.FileContext;
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
      throw new HdfsException(e);
    } finally {
      Closeables.closeQuietly(out);
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

  public static void createSymlink(FileSystem fileSystem, String origin, String destination) {
    try {
      Path originPath = new Path(origin);
      Path destinationPath = new Path(destination);

      FileContext.getFileContext(fileSystem.getUri()).createSymlink(originPath, destinationPath, false);
    } catch(IOException e) {
      throw new HdfsException(e);
    }
  }

  public static void mv(FileSystem fileSystem, String origin, String destination) {
    boolean rename;
    try {
      Path originPath = new Path(origin);
      Path destinationPath = new Path(destination);
      rename = fileSystem.rename(originPath, destinationPath);
    } catch(IOException e) {
      throw new HdfsException(e);
    }
    if(!rename) {
      throw new HdfsException(String.format("could not rename %s to %s", origin, destination));
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
  private static List<Path> ls(FileSystem fileSystem, String stringPath, Pattern pattern, boolean file, boolean dir) {
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
      if(((fileStatus.isFile() && file) || (fileStatus.isDirectory() && dir)) && //
          (null == pattern || pattern.matcher(filename).matches())) {
        ls.add(fileStatus.getPath());
      }
    }
    return ls;
  }

  public static List<Path> lsFile(FileSystem fileSystem, String stringPath, Pattern pattern) {
    return ls(fileSystem, stringPath, pattern, true, false);
  }

  public static List<Path> lsDir(FileSystem fileSystem, String stringPath, Pattern pattern) {
    return ls(fileSystem, stringPath, pattern, false, true);
  }

  public static List<Path> lsAll(FileSystem fileSystem, String stringPath, Pattern pattern) {
    return ls(fileSystem, stringPath, pattern, true, true);
  }

  public static List<Path> lsFile(FileSystem fileSystem, String stringPath) {
    return lsFile(fileSystem, stringPath, null);
  }

  public static List<Path> lsDir(FileSystem fileSystem, String stringPath) {
    return lsDir(fileSystem, stringPath, null);
  }

  public static List<Path> lsAll(FileSystem fileSystem, String stringPath) {
    return lsAll(fileSystem, stringPath, null);
  }

  public static List<String> toFilenameList(List<Path> pathList) {
    List<String> filenameList = new ArrayList<String>();
    for(Path path : pathList) {
      filenameList.add(path.getName());
    }
    return filenameList;
  }

  public static FileStatus getFileStatus(FileSystem fileSystem, Path path) {
    FileStatus fileStatus = null;
    try {
      fileStatus = fileSystem.getFileStatus(path);
    } catch(IOException e) {
      throw new HdfsException(e);
    }
    return fileStatus;
  }
}
