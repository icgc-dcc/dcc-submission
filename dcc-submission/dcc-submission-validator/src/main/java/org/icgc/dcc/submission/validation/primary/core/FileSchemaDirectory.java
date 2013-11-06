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
package org.icgc.dcc.submission.validation.primary.core;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.hdfs.HadoopUtils;
import org.icgc.dcc.submission.validation.core.ErrorType;
import org.icgc.dcc.submission.validation.primary.PlanningFileLevelException;

import com.google.common.collect.Lists;

/**
 * A directory that contains files associated with {@code FileSchema}. Each {@code FileSchema} is expected to have at
 * most one file in this directory.
 */
@Slf4j
@RequiredArgsConstructor
public class FileSchemaDirectory {

  @NonNull
  private final FileSystem fs;
  @NonNull
  private final Path directory;

  public String getDirectoryPath() {
    return directory.toUri().getPath();
  }

  public boolean hasFile(final FileSchema fileSchema) {
    val paths = matches(fileSchema.getPattern());
    if (paths == null) {
      return false;
    }

    if (paths.size() > 1) {
      List<String> pathNames = Lists.newArrayList();
      for (Path path : paths) {
        pathNames.add(path.getName());
      }
      throw new PlanningFileLevelException(paths.get(0).getName().toString(), ErrorType.TOO_MANY_FILES_ERROR,
          fileSchema.getName(), pathNames);
    }

    if (paths.size() > 0) {
      checkCompression(fileSchema, paths.get(0));
    }

    return paths.size() == 1;
  }

  /**
   * FIXME: https://jira.oicr.on.ca/browse/DCC-1233.
   */
  private void checkCompression(FileSchema fileSchema, Path path) {
    try {
      val magicNumber = new byte[3];
      val BZ2_MAGIC_NUMBER = new byte[] { 0x42, 0x5A, 0x68 };
      val BZ2_EXTENSION = ".bz2";

      @Cleanup
      DataInputStream testis = fs.open(path);
      testis.readFully(magicNumber);

      if (Arrays.equals(magicNumber, BZ2_MAGIC_NUMBER) != path.getName().endsWith(BZ2_EXTENSION)) {
        throw new PlanningFileLevelException(path.getName().toString(), ErrorType.COMPRESSION_CODEC_ERROR,
            fileSchema.getName());
      }
    } catch (IOException e) {
      log.error("Problem opening file to check compression scheme", e);
      throw new PlanningFileLevelException(path.getName().toString(), ErrorType.COMPRESSION_CODEC_ERROR,
          fileSchema.getName());
    }
  }

  private List<Path> matches(final String pattern) {
    if (pattern == null) {
      return null;
    }

    return HadoopUtils.lsFile(fs, directory, Pattern.compile(pattern));
  }

}
