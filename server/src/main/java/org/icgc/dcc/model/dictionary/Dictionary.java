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
package org.icgc.dcc.model.dictionary;

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.icgc.dcc.model.BaseEntity;
import org.icgc.dcc.model.HasName;
import org.icgc.dcc.model.dictionary.visitor.DictionaryElement;
import org.icgc.dcc.model.dictionary.visitor.DictionaryVisitor;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PrePersist;

/**
 * Describes a dictionary that contains {@code FileSchema}ta and that may be used by some releases
 */
@Entity
public class Dictionary extends BaseEntity implements HasName, DictionaryElement, Serializable {

  @Indexed(unique = true)
  private String version;

  private DictionaryState state;

  private List<FileSchema> files;

  public Dictionary() {
    super();
    this.state = DictionaryState.OPENED;
    this.files = new ArrayList<FileSchema>();
  }

  public Dictionary(Dictionary dictionary) {
    this();

    // TODO: visitor way
    this.version = dictionary.version;
    this.state = dictionary.state;
    this.files = new ArrayList<FileSchema>();
    for(FileSchema fileSchema : dictionary.files) {
      this.files.add(new FileSchema(fileSchema));
    }
  }

  public Dictionary(String version) {
    super();
    this.version = version;
    this.state = DictionaryState.OPENED;
  }

  @Override
  public void accept(DictionaryVisitor dictionaryVisitor) {
    dictionaryVisitor.visit(this);
    for(FileSchema fileSchema : files) {
      fileSchema.accept(dictionaryVisitor);
    }
  }

  @PrePersist
  public void updateTimestamp() {
    lastUpdate = new Date();
  }

  public void close() {
    this.state = DictionaryState.CLOSED;
  }

  @Override
  public String getName() {
    return this.version;
  }

  public String getVersion() {
    return version;
  }

  public DictionaryState getState() {
    return state;
  }

  public List<FileSchema> getFiles() {
    return files;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setState(DictionaryState state) {
    this.state = state;
  }

  public void setFiles(List<FileSchema> files) {
    this.files = files;
  }

  public FileSchema getFileSchema(String fileName) {
    FileSchema result = null;
    for(FileSchema fileSchema : this.files) {
      if(fileSchema.getName().equals(fileName)) {
        result = fileSchema;
        break;
      }
    }
    checkState(result != null);
    return result;
  }

}
