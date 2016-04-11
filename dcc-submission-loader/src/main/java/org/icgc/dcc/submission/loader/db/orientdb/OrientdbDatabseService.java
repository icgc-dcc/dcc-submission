/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.loader.db.orientdb;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.submission.loader.util.DatabaseFields.PROJECT_ID_FIELD_NAME;
import static org.icgc.dcc.submission.loader.util.Strings.capitalize;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.submission.loader.db.DatabaseService;
import org.icgc.dcc.submission.loader.meta.SubmissionMetadataService;
import org.icgc.dcc.submission.loader.model.Project;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OrientdbDatabseService implements DatabaseService {

  private static final String INDEX_SUFFIX = "_idx";

  @NonNull
  private final ODatabaseDocumentTx db;
  @NonNull
  private final SubmissionMetadataService submissionMetadataService;

  @Override
  public void initializeDb(String release, Iterable<Project> projects) {
    initializeSchema();
    setInsertMode();
  }

  @Override
  public void finalizeDb(String release) {
    createTypeIndices();
    resetDbMode();
  }

  public void initializeSchema() {
    if (isInitialized()) {
      log.info("The database is already initialized. Skipping creation...");
    } else {
      val fileTypes = getFileTypes();
      fileTypes.stream()
          .forEach(ft -> initializeClassSchema(submissionMetadataService, ft));

      val reverseFileTypes = Lists.newArrayList(fileTypes);
      Collections.reverse(reverseFileTypes);
      reverseFileTypes.stream()
          .forEach(ft -> setParents(db, submissionMetadataService.getParent(ft), ft));
    }
  }

  public void createTypeIndex(@NonNull String type) {
    if (indexExists(type)) {
      log.info("Index for class '{}' already exists. Skipping creation...", type);
      return;
    }

    val pks = submissionMetadataService.getPrimaryKey(type);
    if (size(pks) == 1) {
      return;
    }

    val schema = capitalize(type);
    val joinedPks = Joiners.COMMA.join(pks);
    val sql = "CREATE INDEX " + type + INDEX_SUFFIX + " ON " + schema + "(" + joinedPks + ") NOTUNIQUE";
    log.info("Executing query: {}", sql);
    db.command(new OCommandSQL(sql)).execute();
  }

  private boolean indexExists(String type) {
    val schema = getSchema(type);
    val index = schema.getClassIndex(type + INDEX_SUFFIX);

    return index != null;
  }

  private void setInsertMode() {
    db.declareIntent(new OIntentMassiveInsert());
  }

  private void resetDbMode() {
    db.declareIntent(null);
  }

  private void createTypeIndices() {
    submissionMetadataService.getFileTypes().stream()
        .map(fileType -> fileType.getType())
        .forEach(type -> createTypeIndex(type));
  }

  private void initializeClassSchema(
      SubmissionMetadataService submissionMetadataService, String type) {
    log.info("Initializing schema for type '{}'", type);
    val schemaName = capitalize(type);
    val schema = createSchema(schemaName);
    schema.setStrictMode(true);

    val schemaFields = submissionMetadataService.getFields(type);
    setMetadataFields(schema, schemaFields);

    val children = submissionMetadataService.getChildren(type);
    setChildren(schema, children);
  }

  private void setParents(ODatabaseDocumentTx db, Collection<String> parents, String type) {
    val schemaName = capitalize(type);
    val childSchema = getSchema(schemaName);
    for (val parent : parents) {
      val parentSchema = getSchema(capitalize(parent));
      childSchema.createProperty(parent, OType.LINK, parentSchema);
    }
  }

  private static void setMetadataFields(OClass schema, Map<String, String> fields) {
    for (val entry : fields.entrySet()) {
      val fieldName = entry.getKey();
      val valueType = convertValue(entry.getValue());
      schema.createProperty(fieldName, valueType);
    }

    // Add synthetic project ID property
    schema.createProperty(PROJECT_ID_FIELD_NAME, OType.STRING);
  }

  private void setChildren(OClass schema, Collection<String> children) {
    for (val child : children) {
      val childSchemaName = capitalize(child);
      checkState(exists(db, childSchemaName), "Failed to set child '%s' for schema '%s'. Child schema is not defined",
          childSchemaName, schema.getName());
      val childSchema = getSchema(childSchemaName);
      schema.createProperty(child, OType.LINKSET, childSchema);
    }
  }

  private List<String> getFileTypes() {
    val typeDefs = newArrayList(submissionMetadataService.getFileTypes());
    Collections.reverse(typeDefs);

    return typeDefs.stream()
        .map(td -> td.getType())
        .collect(toImmutableList());
  }

  private static OType convertValue(String valueType) {
    return OType.STRING;
  }

  private OClass createSchema(String className) {
    return db.getMetadata().getSchema().createClass(className);
  }

  private OClass getSchema(String className) {
    return db.getMetadata().getSchema().getClass(className);
  }

  private boolean isInitialized() {
    return exists(db, "Donor");
  }

  static boolean exists(ODatabaseDocumentTx db, String className) {
    return db.getMetadata().getSchema().getClass(className) != null;
  }

}
