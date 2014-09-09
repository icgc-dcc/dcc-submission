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
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.ByteStreams.copy;
import static org.icgc.dcc.core.util.Collections3.sort;
import static org.icgc.dcc.core.util.Jackson.formatPrettyJson;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.core.util.Separators.DASH;

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
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.icgc.dcc.core.util.Separators;

import cascading.tuple.Fields;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.LineReader;

/**
 * Handles all hadoop API related methods - TODO: change to use proxy or decorator pattern?
 * <p>
 * TODO: merge {@link FileOperations} here?
 */
@Slf4j
public class HadoopUtils {

  private static final String MR_PART_FILE_NAME_BASE = "part";
  private static final Joiner on = Joiner.on(DASH);
  private static final int MR_PART_FILE_NAME_FIRST_INDEX = 0;
  private static final String MR_PART_FILE_INDEX_PADDING = "%05d";
  private static final String FIRST_PLAIN_MR_PART_FILE_NAME = getFirstPlainMrPartFileName();

  private static String getFirstPlainMrPartFileName() {
    return getPlainMrPartFileName(MR_PART_FILE_NAME_FIRST_INDEX);
  }

  private static String getPlainMrPartFileName(int index) {
    return on.join(MR_PART_FILE_NAME_BASE, formatPartIndex(index));
  }

  private static String formatPartIndex(int index) {
    return String.format(MR_PART_FILE_INDEX_PADDING, index);
  }

  public static String getConfigurationDescription(Configuration configuration) {
    val stringWriter = new StringWriter();
    @Cleanup
    val printWriter = new PrintWriter(stringWriter);
    dumpConfiguration(configuration, stringWriter);

    return formatPrettyJson(stringWriter.toString());
  }

  @SneakyThrows
  private static FSDataInputStream open(
      @NonNull final FileSystem fileSystem,
      @NonNull final Path path) {
    return fileSystem.open(path);
  }

  @SneakyThrows
  private static void dumpConfiguration(Configuration configuration, StringWriter writer) {
    Configuration.dumpConfiguration(configuration, writer);
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
      @NonNull final Path path) {

    return fileSystem.exists(path);
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

  public static Path recursivelyDeleteDirectoryIfExists(
      @NonNull final FileSystem fileSystem,
      @NonNull final Path dirPath) {
    checkArgument(
        !exists(fileSystem, dirPath)
            || isDirectory(fileSystem, dirPath),
        dirPath);

    // Deleting parent directory if it exists
    if (checkExistence(fileSystem, dirPath)) {
      log.info("Recursively deleting '{}' (content: {})",
          dirPath, lsAll(fileSystem, dirPath));
      rmr(fileSystem, dirPath);
    } else {
      log.info("{} did not already exist.", dirPath);
    }

    return dirPath;
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
   * Intended for small files only.
   */
  @SneakyThrows
  public static List<String> readSmallTextFile(FileSystem fileSystem, Path path) {
    @Cleanup
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            getInputStream(fileSystem, path)));
    val lines = Lists.<String> newArrayList();
    for (String line; (line = reader.readLine()) != null;) {
      lines.add(line);
    }

    return lines;
  }

  @SneakyThrows
  public static Fields getFileHeader(
      @NonNull final FileSystem fileSystem,
      @NonNull final String inputFilePath,
      @NonNull final String separator) {

    val inputFile = new Path(inputFilePath);
    val codec = new CompressionCodecFactory(fileSystem.getConf()).getCodec(inputFile);

    @Cleanup
    InputStreamReader reader = new InputStreamReader(
        codec == null ?
            fileSystem
                .open(inputFile) :
            codec.createInputStream(fileSystem.open(inputFile)),
        UTF_8);

    val splitter = Separators.getCorrespondingSplitter(separator);

    return new Fields(toArray(
        splitter.split(
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

  public static boolean isPartFile(@NonNull final Path filePath) {
    return isPartFilePredicate().apply(filePath);
  }

  private static Predicate<Path> isPartFilePredicate() {
    return new Predicate<Path>() {

      @Override
      public boolean apply(@NonNull final Path filePath) {
        return filePath.getName().startsWith(MR_PART_FILE_NAME_BASE);
      }

    };
  }

  @SneakyThrows
  public static InputStream getInputStream(
      @NonNull final FileSystem fileSystem,
      @NonNull final Path path) {

    if (isFile(fileSystem, path)) {
      return getFileInputStream(fileSystem, path);
    } else {
      val inputSuppliers = new ArrayList<InputSupplier<InputStream>>();
      for (val partFile : getSortedPartFiles(fileSystem, path)) {
        inputSuppliers.add(new InputSupplier<InputStream>() {

          @Override
          public InputStream getInput() throws IOException {
            return getFileInputStream(fileSystem, partFile);
          }
        });
      }

      return ByteStreams.join(inputSuppliers).getInput();
    }
  }

  @SneakyThrows
  private static InputStream getFileInputStream(
      @NonNull final FileSystem fileSystem,
      @NonNull final Path path) {
    val factory = new CompressionCodecFactory(fileSystem.getConf());

    val resolvedPath = FileContext.getFileContext(fileSystem.getUri()).resolvePath(path);
    val codec = factory.getCodec(path);
    val inputStream = open(fileSystem, resolvedPath);

    return codec == null ?
        inputStream :
        codec.createInputStream(inputStream);
  }

  @SuppressWarnings("unchecked")
  // TODO: how to get rid of that warning?
  private static List<Path> getSortedPartFiles(
      @NonNull final FileSystem fileSystem,
      @NonNull final Path inputDir) {
    checkArgument(isDirectory(fileSystem, inputDir));

    return ImmutableList.copyOf(sort(newArrayList(
        filter(
            lsFile(fileSystem, inputDir),
            isPartFilePredicate()))));
  }

}
