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

import java.util.Map;
import java.util.TreeMap;

import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.Restriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visits every {@code Dictionary}-related objects and creates an index of the elements for efficient lookup.
 * 
 * TODO: in progress
 */
public class DictionaryIndexVisitor extends BaseDictionaryVisitor {

  private static final Logger log = LoggerFactory.getLogger(DictionaryIndexVisitor.class);

  @SuppressWarnings("unused")
  private final Map<String, Dictionary> dictionaries;

  public DictionaryIndexVisitor() {
    dictionaries = new TreeMap<String, Dictionary>();
  }

  @Override
  public void visit(Dictionary dictionary) {
    log.info("index visiting dictionary " + dictionary.getVersion());
    // TODO
  }

  @Override
  public void visit(FileSchema fileSchema) {
    log.info("index visiting fileSchema " + fileSchema.getName());
  }

  @Override
  public void visit(Field field) {
    log.info("index visiting field " + field.getName());
  }

  @Override
  public void visit(Restriction restriction) {
    log.info("index visiting restriction " + restriction.getType());
  }

  public boolean hasFileSchema(String name) {
    // TODO
    return false;
  }

  public FileSchema getFileSchema(String name) {
    return null;
  }

  public boolean hasField(String fileSchemaName, String fieldName) {
    return false;
  }

  public Field getField(String fileSchemaName, String fieldName) {
    return null;
  }

  public Iterable<String> getFieldNames(String name) {
    return null;
  }
}
