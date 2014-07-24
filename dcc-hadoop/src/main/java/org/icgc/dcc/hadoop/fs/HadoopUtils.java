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
package org.icgc.dcc.hadoop.fs;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.io.ByteStreams.copy;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.core.util.Separators.DASH;
import static org.icgc.dcc.core.util.Splitters.TAB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import cascading.tuple.Fields;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.LineReader;

/**
 * Handles all hadoop API related methods - TODO: change to use proxy or decorator pattern?
 */
public class HadoopUtils {

  public static final String MR_PART_FILE_NAME_BASE = "part";
  public static final String MR_PART_FILE_SEPARATOR = DASH;
  public static final String MR_PART_FILE_NAME_FIRST_INDEX = "00000";
  public static final String FIRST_PLAIN_MR_PART_FILE_NAME = Joiner.on(MR_PART_FILE_SEPARATOR)
      .join(MR_PART_FILE_NAME_BASE, MR_PART_FILE_NAME_FIRST_INDEX);

  public static String getConfigurationDescription(Configuration configuration) {
    @Cleanup
    val printWriter = new PrintWriter(new StringWriter());
    dumpConfiguration(configuration, printWriter);

    return printWriter.toString();
  }

  @SneakyThrows
  private static void dumpConfiguration(Configuration configuration, PrintWriter printWriter) {
    Configuration.dumpConfiguration(configuration, printWriter);
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

  @SneakyThrows
  public static void touch(FileSystem fileSystem, String stringPath, InputStream in) {
    Path path = new Path(stringPath);
    try {
      @Cleanup
      FSDataOutputStream out = fileSystem.create(path);
      copy(in, out);
    } catch (IOException e) {
      throw new HdfsException(e);
    }
  }

  @SneakyThrows
  public static boolean exists(
      @NonNull final FileSystem fileSystem,
      @NonNull final Path alternativeFile) {

    return fileSystem.exists(alternativeFile);
  }

  public static boolean isFile(FileSystem fileSystem, @NonNull String stringPath) {
    return isFile(fileSystem, new Path(stringPath));
  }

  public static boolean isFile(FileSystem fileSystem, @NonNull Path path) {
    val fileStatus = getFileStatus(fileSystem, path);
    return fileStatus.isPresent() && fileStatus.get().isFile();
  }

  public static boolean isDirectory(FileSystem fileSystem, @NonNull String stringPath) {
    return isDirectory(fileSystem, new Path(stringPath));
  }

  public static boolean isDirectory(FileSystem fileSystem, @NonNull Path path) {
    val fileStatus = getFileStatus(fileSystem, path);
    return fileStatus.isPresent() && fileStatus.get().isDirectory();
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
   */
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
   * non-recursively.
   */
  private static List<Path> ls(FileSystem fileSystem, Path path, Pattern pattern, boolean file, boolean dir,
      boolean symLink) {
    FileStatus[] listStatus;
    try {
      listStatus = fileSystem.listStatus(path); // This returns full paths, not just file names.
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

  /**
   * Only use for logging and debugging purposes
   */
  @SneakyThrows
  public static List<String> lsRecursive(FileSystem fileSystem, Path path) {
    val files = new ImmutableList.Builder<String>();
    val iterator = fileSystem.listFiles(path, true);
    while (iterator.hasNext()) {
      files.add(iterator.next()
          .getPath()
          .toUri().toString());
    }
    return files.build();
  }

  /**
   * See {@link #lsRecursive(FileSystem, Path)}.
   */
  public static List<String> tree(FileSystem fileSystem, Path path) {
    return lsRecursive(fileSystem, path);
  }

  /**
   * This is not applicable for dir.
   */
  public static long duFile(FileSystem fileSystem, Path filePath) throws IOException {
    FileStatus status = fileSystem.getFileStatus(filePath);
    return status.getLen();
  }

  /**
   * Returns the {@link FileStatus} for the given {@link Path}.
   */
  @SneakyThrows
  public static Optional<FileStatus> getFileStatus(FileSystem fileSystem, Path path) {
    return fileSystem.exists(path) ?
        Optional.of(fileSystem.getFileStatus(path)) :
        Optional.<FileStatus> absent();
  }

  /**
   * See {@link #readSmallTextFile(FileSystem, Path)}.
   */
  public static List<String> catSmallTextFile(FileSystem fileSystem, Path path) {
    return readSmallTextFile(fileSystem, path);
  }

  /**
   * Intended for small files only.
   */
  @SneakyThrows
  public static List<String> readSmallTextFile(FileSystem fileSystem, Path path) {
    @Cleanup
    BufferedReader br = new BufferedReader(new InputStreamReader(fileSystem.open(path)));
    val lines = Lists.<String> newArrayList();
    for (String line; (line = br.readLine()) != null;) {
      lines.add(line);
    }
    return lines;
  }

  @SneakyThrows
  public static Fields getFileHeader(FileSystem fileSystem, String inputFilePath) {

    val inputFile = new Path(inputFilePath);
    val codec = new CompressionCodecFactory(fileSystem.getConf()).getCodec(inputFile);

    @Cleanup
    InputStreamReader reader = new InputStreamReader(
        codec == null ?
            fileSystem
                .open(inputFile) :
            codec.createInputStream(fileSystem.open(inputFile)),
        UTF_8);

    return new Fields(toArray(
        TAB.split(
            new LineReader(reader)
                .readLine()),
        String.class));

  }

  /**
   * TODO: handle compression
   */
  public static String addMRFirstPartSuffix(String filePath) {
    return PATH.join(
        filePath,
        FIRST_PLAIN_MR_PART_FILE_NAME);
  }

}
