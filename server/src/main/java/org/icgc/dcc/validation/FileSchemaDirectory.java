/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;

/**
 * A directory that contains files associated with {@code FileSchema}. Each {@code FileSchema} is expected to have at
 * most one file in this directory.
 */
public class FileSchemaDirectory {

  private final Path directory;

  private final FileSystem fs;

  public FileSchemaDirectory(FileSystem fs, Path source) {
    checkArgument(source != null);
    checkArgument(fs != null);
    this.directory = source;
    this.fs = fs;
  }

  public boolean hasFile(final FileSchema fileSchema) {
    List<Path> paths = matches(fileSchema);
    if(paths != null && paths.size() > 1) {
      Map<String, Object> params = new LinkedHashMap<String, Object>();
      params.put("columnName", "FileLevelError");
      params.put("value", paths.toString());
      throw new PlanningException(fileSchema.getName(), ValidationErrorCode.TOO_MANY_FILES_ERROR, params);
    }
    return paths != null && paths.size() > 0;
  }

  private List<Path> matches(final FileSchema fileSchema) {
    if(fileSchema.getPattern() == null) {
      return null;
    }
    return HadoopUtils.lsFile(fs, directory.toString(), Pattern.compile(fileSchema.getPattern()));
  }
}
