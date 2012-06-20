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
package org.icgc.dcc.model.dictionary.visitor;

import java.util.ArrayList;

import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.Restriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;

/**
 * Visits every {@code Dictionary}-related objects and creates a clone for the corresponding object
 * 
 * TODO: in progress
 */
public class DictionaryCloneVisitor extends BaseDictionaryVisitor {

  private static final Logger log = LoggerFactory.getLogger(DictionaryCloneVisitor.class);

  private Dictionary dictionaryClone;

  private Dictionary currentDictionary;

  private FileSchema currentFileSchema;

  private Field currentField;

  public Dictionary getDictionaryClone() {
    return dictionaryClone;
  }

  @Override
  public void visit(Dictionary dictionary) {
    log.info("clone visiting dictionary " + dictionary.getVersion());

    dictionaryClone = new Dictionary();
    dictionaryClone.setVersion(dictionary.getVersion());
    dictionaryClone.setState(dictionary.getState());
    currentDictionary = dictionaryClone;
  }

  @Override
  public void visit(FileSchema fileSchema) {
    log.info("clone visiting fileSchema " + fileSchema.getName());

    FileSchema fileSchemaClone = new FileSchema();
    fileSchemaClone.setName(fileSchema.getName());
    fileSchemaClone.setLabel(fileSchema.getLabel());
    fileSchemaClone.setPattern(fileSchema.getPattern());
    fileSchemaClone.setRole(fileSchema.getRole());
    fileSchemaClone.setUniqueFields(new ArrayList<String>(fileSchema.getUniqueFields()));

    currentDictionary.addFile(fileSchemaClone);
    currentFileSchema = fileSchemaClone;
  }

  @Override
  public void visit(Field field) {
    log.info("clone visiting field " + field.getName());
    Field cloneField = new Field();

    cloneField.setName(field.getName());
    cloneField.setLabel(field.getLabel());
    cloneField.setValueType(field.getValueType());

    currentFileSchema.addField(cloneField);
    currentField = cloneField;
  }

  @Override
  public void visit(Restriction restriction) {
    log.info("clone visiting restriction " + restriction.getType());

    Restriction cloneRestriction = new Restriction();
    cloneRestriction.setType(restriction.getType());
    cloneRestriction.setConfig(new BasicDBObject(restriction.getConfig()));

    currentField.addRestriction(cloneRestriction);
  }
}
