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
package org.icgc.dcc.submission.loader.db.postgres;

import static java.lang.String.format;
import static org.icgc.dcc.common.core.util.Separators.DOT;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.submission.loader.util.Fields.DONOR_ID_FIELD_NAME;
import static org.icgc.dcc.submission.loader.util.Fields.PROJECT_ID;
import static org.icgc.dcc.submission.loader.util.Fields.PROJECT_NAME;
import static org.icgc.dcc.submission.loader.util.Fields.PROJECT_STATE;
import static org.icgc.dcc.submission.loader.util.Tables.PROJECT_TABLE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.submission.loader.db.DatabaseService;
import org.icgc.dcc.submission.loader.meta.SubmissionMetadataService;
import org.icgc.dcc.submission.loader.meta.TypeDefGraph;
import org.icgc.dcc.submission.loader.model.Project;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.ImmutableList;

@Slf4j
@RequiredArgsConstructor
public class PostgresDatabaseService implements DatabaseService {

  @NonNull
  private final SubmissionMetadataService submissionMetadataService;
  @NonNull
  private final JdbcTemplate jdbcTemplate;
  @NonNull
  private final TypeDefGraph typeDefGraph;

  @Override
  public void initializeDb(@NonNull String release, @NonNull Iterable<Project> projects) {
    createSchema(release);
    createTables(release);
    populateProjects(release, projects);
  }

  @Override
  public void finalizeDb(@NonNull String release) {
    populateDonorId(release);
  }

  private void populateDonorId(String release) {
    for (val type : getNoDonorIdTypes()) {
      val sql = createUpdateQuery(release, type);
      if (sql.isPresent()) {
        jdbcTemplate.execute(sql.get());
      }
    }
  }

  private Optional<String> createUpdateQuery(String release, String type) {
    log.debug("Creating update donor_id query for release {} and type {}", release, type);
    val parent = submissionMetadataService.getParent(type);

    // Exclude meth_array_probes
    if (parent.isEmpty()) {
      return Optional.empty();
    }

    val sql = new StringBuilder();
    val tableName = getTableName(release, type);
    sql.append("UPDATE " + tableName + " child ");
    sql.append("SET " + DONOR_ID_FIELD_NAME + " = ( ");

    // The parents should be updated already. The first one will be used.
    val parentType = parent.iterator().next();
    val parentTableName = getTableName(release, parentType);
    sql.append("SELECT parent." + DONOR_ID_FIELD_NAME + " FROM " + parentTableName + " parent ");
    sql.append(" WHERE ");

    val childPks = submissionMetadataService.getChildPrimaryKey(type, parentType);
    val parentPks = submissionMetadataService.getParentPrimaryKey(type, parentType);
    val parentChildJoinClause = createParentChildJoinClause(childPks, parentPks);
    sql.append(parentChildJoinClause + " )");

    val query = sql.toString();
    log.debug("Update donor_id query: {}", query);

    return Optional.of(query);
  }

  private List<String> getNoDonorIdTypes() {
    val fileTypes = ImmutableList.<String> builder();
    val order = typeDefGraph.topologicalOrder();

    while (order.hasNext()) {
      val typeDef = order.next();
      val type = typeDef.getType();
      val typeFields = submissionMetadataService.getFields(type);
      if (typeFields.containsKey(DONOR_ID_FIELD_NAME) == false) {
        fileTypes.add(type);
      }
    }

    return fileTypes.build();
  }

  private void createSchema(String release) {
    val sql = "CREATE SCHEMA IF NOT EXISTS " + release.toLowerCase();
    jdbcTemplate.execute(sql);
  }

  private void createTables(String release) {
    initializeProjectTable(release);
    getFileTypes(release).stream()
        .forEach(fileType -> initializeTable(release, fileType));
  }

  private void initializeProjectTable(String release) {
    dropTable(release, PROJECT_TABLE);

    val tableName = getTableName(release, PROJECT_TABLE);
    val sql = "CREATE TABLE " + tableName + " ( "
        + PROJECT_ID + " varchar(7) PRIMARY KEY, "
        + PROJECT_NAME + " varchar(200), "
        + PROJECT_STATE + " varchar(20) "
        + ")";

    log.debug("{}", sql);
    jdbcTemplate.execute(sql);
  }

  private void initializeTable(String release, String type) {
    dropTable(release, type);
    createTable(release, type);
    createIndex(release, type);
  }

  private void createIndex(String release, String type) {
    val indexName = Joiners.UNDERSCORE.join(release, type, "idx");
    dropIndex(indexName);

    val tableName = getTableName(release, type);

    val pks = submissionMetadataService.getPrimaryKey(type);
    val joinedPks = Joiners.COMMA.join(pks);

    val sql = "CREATE INDEX " + indexName + " ON " + tableName + " ( " + joinedPks + " ) ";
    log.debug("{}", sql);

    jdbcTemplate.execute(sql);
  }

  private void dropIndex(String indexName) {
    val sql = "DROP INDEX IF EXISTS " + indexName;
    jdbcTemplate.execute(sql);
  }

  private void createTable(String release, String type) {
    val sqlBuilder = new StringBuilder();
    val tableName = getTableName(release, type);
    sqlBuilder.append("CREATE TABLE " + tableName + " (");

    val pks = submissionMetadataService.getPrimaryKey(type);
    boolean hasPreviousField = false;
    boolean hasDonorId = false;
    for (val entry : submissionMetadataService.getFields(type).entrySet()) {
      val fieldName = entry.getKey();
      if (hasPreviousField) {
        sqlBuilder.append(", ");
      }
      hasPreviousField = true;

      sqlBuilder.append(fieldName + " varchar(10000)");

      if (pks.contains(fieldName)) {
        sqlBuilder.append(" NOT NULL");
      }

      if (fieldName.equals("donor_id")) {
        hasDonorId = true;
      }
    }

    // Add project_id
    sqlBuilder.append(", " + PROJECT_ID + " varchar(7)");
    // Add donor_id
    if (!hasDonorId) {
      sqlBuilder.append(", " + DONOR_ID_FIELD_NAME + " varchar(500)");
    }

    sqlBuilder.append(" )");

    val sql = sqlBuilder.toString();
    log.debug(sql);

    jdbcTemplate.execute(sql);
  }

  private void populateProjects(String release, Iterable<Project> projects) {
    log.debug("Populating projects table...");
    val sql = "INSERT INTO " + getTableName(release, PROJECT_TABLE) + " VALUES (?, ?, ?)";
    for (val project : projects) {
      log.debug("{}", project);
      jdbcTemplate.update(sql, new Object[] { project.getProjectId(), project.getProjectName(),
          project.getState().getName() });
    }
  }

  private String getTableName(String release, String type) {
    return release.toLowerCase() + DOT + type;
  }

  private void dropTable(String release, String type) {
    val tableName = getTableName(release, type);
    jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName + " CASCADE");
  }

  private List<String> getFileTypes(String release) {
    return submissionMetadataService.getFileTypes().stream()
        .map(ft -> ft.getType())
        .collect(toImmutableList());
  }

  private String createParentChildJoinClause(List<String> childPks, Map<String, String> parentPks) {
    val joinClause = new StringBuilder();

    boolean hasPrevCondition = false;
    for (val childPk : childPks) {
      if (hasPrevCondition) {
        joinClause.append(" AND ");
      }

      joinClause.append(format("child.%s = parent.%s", childPk, parentPks.get(childPk)));
      hasPrevCondition = true;
    }

    // Add project_id
    joinClause.append(format(" AND child.%s = parent.%s", PROJECT_ID, PROJECT_ID));

    return joinClause.toString();
  }

}
