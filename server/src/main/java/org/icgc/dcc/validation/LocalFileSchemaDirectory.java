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

import java.io.File;
import java.io.FileFilter;

import org.icgc.dcc.dictionary.model.FileSchema;

public class LocalFileSchemaDirectory implements FileSchemaDirectory {

  private final File directory;

  public LocalFileSchemaDirectory(File directory) {
    checkArgument(directory != null);
    this.directory = directory;
  }

  @Override
  public String getFile(FileSchema fileSchema) {
    File[] files = matches(fileSchema);
    if(files == null || files.length == 0) {
      throw new IllegalArgumentException();
    }
    if(files.length > 1) {
      throw new IllegalStateException();
    }
    return files[0].getAbsolutePath();
  }

  @Override
  public boolean hasFile(final FileSchema fileSchema) {
    File[] files = matches(fileSchema);
    return files != null && files.length > 0;
  }

  private File[] matches(final FileSchema fileSchema) {
    if(fileSchema.getPattern() == null) {
      return null;
    }
    return directory.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.getName().contains(fileSchema.getName());
        // checkNotNull(fileSchema.getPattern(), "schema " + fileSchema.getName() + " has no pattern");
        // return Pattern.matches(fileSchema.getPattern(), pathname.getName());
      }
    });
  }

}
