/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.fs.hdfs;

import static com.google.common.io.Closeables.closeQuietly;

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

/**
 * Handles all hadoop API related methods - TODO: change to use proxy or decorator pattern?
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
    mkdirs(fileSystem, new Path(stringPath));
  }

  public static void mkdirs(FileSystem fileSystem, Path path) {
    boolean mkdirs;
    try {
      mkdirs = fileSystem.mkdirs(path);
    } catch (IOException e) {
      throw new HdfsException(e);
    }
    if (!mkdirs) {
      throw new HdfsException("could not create " + path);
    }
  }

  public static void touch(FileSystem fileSystem, String stringPath, InputStream in) {
    Path path = new Path(stringPath);
    FSDataOutputStream out = null;
    try {
      out = fileSystem.create(path);
      ByteStreams.copy(in, out);
    } catch (IOException e) {
      throw new HdfsException(e);
    } finally {
      closeQuietly(out);
    }
  }

  public static void rm(FileSystem fileSystem, String stringPath) {
    Path path = new Path(stringPath);
    rm(fileSystem, path);
  }

  public static void rm(FileSystem fileSystem, Path path) {
    rm(fileSystem, path, false);
  }

  public static void rmr(FileSystem fileSystem, String stringPath) {
    Path path = new Path(stringPath);
    rmr(fileSystem, path);
  }

  public static void rmr(FileSystem fileSystem, Path path) {
    rm(fileSystem, path, true);
  }

  private static void rm(FileSystem fileSystem, Path path, boolean recursive) {
    boolean delete;
    try {
      delete = fileSystem.delete(path, recursive);
    } catch (IOException e) {
      throw new HdfsException(e);
    }
    if (!delete) {
      throw new HdfsException("could not remove " + path);
    }
  }

  /**
   * This does not work on HDFS as of yet (see DCC-835).
   * @deprecated
   */
  @Deprecated
  public static void createSymlink(FileSystem fileSystem, Path origin, Path destination) {

    try {
      FileContext.getFileContext(fileSystem.getUri()).createSymlink(origin, destination, false);
    } catch (IOException e) {
      throw new HdfsException(e);
    }
  }

  public static void mv(FileSystem fileSystem, String origin, String destination) {
    Path originPath = new Path(origin);
    Path destinationPath = new Path(destination);
    mv(fileSystem, originPath, destinationPath);
  }

  public static void mv(FileSystem fileSystem, Path originPath, Path destinationPath) {
    boolean rename;
    try {
      rename = fileSystem.rename(originPath, destinationPath);
    } catch (IOException e) {
      throw new HdfsException(e);
    }
    if (!rename) {
      throw new HdfsException(String.format("could not rename %s to %s", originPath, destinationPath));
    }
  }

  public static boolean checkExistence(FileSystem fileSystem, String stringPath) {
    return checkExistence(fileSystem, new Path(stringPath));
  }

  public static boolean checkExistence(FileSystem fileSystem, Path path) {
    boolean exists;
    try {
      exists = fileSystem.exists(path);
    } catch (IOException e) {
      throw new HdfsException(e);
    }
    return exists;
  }

  /**
   * non-recursively
   */
  private static List<Path> ls(FileSystem fileSystem, Path path, Pattern pattern, boolean file, boolean dir,
      boolean symLink) {
    FileStatus[] listStatus;
    try {
      listStatus = fileSystem.listStatus(path);
    } catch (IOException e) {
      throw new HdfsException(e);
    }
    List<Path> ls = new ArrayList<Path>();
    for (FileStatus fileStatus : listStatus) {
      String filename = fileStatus.getPath().getName();
      if (((fileStatus.isFile() && file) || (fileStatus.isSymlink() && symLink) //
      || (fileStatus.isDirectory() && dir)) && (null == pattern || pattern.matcher(filename).matches())) {
        ls.add(fileStatus.getPath());
      }
    }
    return ls;
  }

  public static List<Path> lsFile(FileSystem fileSystem, Path path, Pattern pattern) {
    return ls(fileSystem, path, pattern, true, false, false);
  }

  public static List<Path> lsDir(FileSystem fileSystem, Path path, Pattern pattern) {
    return ls(fileSystem, path, pattern, false, true, false);
  }

  public static List<Path> lsAll(FileSystem fileSystem, Path path, Pattern pattern) {
    return ls(fileSystem, path, pattern, true, true, true);
  }

  public static List<Path> lsFile(FileSystem fileSystem, Path path) {
    return lsFile(fileSystem, path, null);
  }

  public static List<Path> lsDir(FileSystem fileSystem, Path path) {
    return lsDir(fileSystem, path, null);
  }

  public static List<Path> lsAll(FileSystem fileSystem, Path path) {
    return lsAll(fileSystem, path, null);
  }

  public static List<String> toFilenameList(List<Path> pathList) {
    List<String> filenameList = new ArrayList<String>();
    for (Path path : pathList) {
      filenameList.add(path.getName());
    }
    return filenameList;
  }

  public static FileStatus getFileStatus(FileSystem fileSystem, Path path) {
    FileStatus fileStatus = null;
    try {
      fileStatus = fileSystem.getFileStatus(path);
    } catch (IOException e) {
      throw new HdfsException(e);
    }
    return fileStatus;
  }

  /**
   * This is not applicable for dir.
   */
  public static long duFile(FileSystem fileSystem, Path filePath) throws IOException {
    FileStatus status = fileSystem.getFileStatus(filePath);
    return status.getLen();
  }
}
