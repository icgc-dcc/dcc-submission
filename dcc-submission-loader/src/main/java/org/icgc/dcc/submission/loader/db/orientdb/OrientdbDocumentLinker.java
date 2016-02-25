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

import static java.lang.String.format;
import static org.icgc.dcc.submission.loader.util.DatabaseFields.PROJECT_ID_FIELD_NAME;
import static org.icgc.dcc.submission.loader.util.Strings.capitalize;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.loader.meta.SubmissionMetadataService;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.sql.OCommandSQL;

@Slf4j
@RequiredArgsConstructor
public class OrientdbDocumentLinker {

  /**
   * Dependencies.
   */
  @NonNull
  private final SubmissionMetadataService submissionMetadataService;
  @NonNull
  private final ODatabaseDocumentTx db;

  public void linkDocuments(@NonNull String type, @NonNull Iterable<String> parents) {
    for (val parent : parents) {
      // Update parent documents
      val updateParentSql = getUpdateQuery(type, parent, true);
      log.info("Executing query: {}", updateParentSql);
      val recordsUpdated = db.command(new OCommandSQL(updateParentSql)).execute();
      log.info("Updated {} record(s)", recordsUpdated);

      // Update children documents
      val updateChildrenSql = getUpdateQuery(type, parent, false);
      log.info("Executing query: {}", updateChildrenSql);
      val childrenRecordsUpdated = db.command(new OCommandSQL(updateChildrenSql)).execute();
      log.info("Updated {} record(s)", childrenRecordsUpdated);
    }
  }

  /**
   * Creates an update query that links parents to children or vice versa depending on the {@code parentToChild} flag
   */
  private String getUpdateQuery(String type, String parent, boolean parentToChild) {
    val childPks = submissionMetadataService.getChildPrimaryKey(type, parent);
    val parentPks = submissionMetadataService.getParentPrimaryKey(type, parent);
    val parentSchema = capitalize(parent);
    val childSchema = capitalize(type);

    val updateQueryBuilder = new StringBuilder();
    if (parentToChild) {
      updateQueryBuilder.append(format("UPDATE %s SET %s =", parentSchema, type));
      updateQueryBuilder.append(format("(SELECT FROM %s WHERE ", childSchema));
    } else {
      updateQueryBuilder.append(format("UPDATE %s SET %s =", childSchema, parent));
      updateQueryBuilder.append(format("(SELECT FROM %s WHERE ", parentSchema));
    }

    val parentChildJoinClause = createParentChildJoinClause(childPks, parentPks);
    updateQueryBuilder.append(parentChildJoinClause);

    val projectClause = format(" AND %s = $parent.$current.%s )", PROJECT_ID_FIELD_NAME, PROJECT_ID_FIELD_NAME);
    updateQueryBuilder.append(projectClause);

    return updateQueryBuilder.toString();
  }

  private static String createParentChildJoinClause(List<String> childPks, Map<String, String> parentPks) {
    val joinClause = new StringBuilder();

    boolean hasPrevCondition = false;
    for (val childPk : childPks) {
      if (hasPrevCondition) {
        joinClause.append(" AND ");
      }

      joinClause.append(format("%s = $parent.$current.%s", parentPks.get(childPk), childPk));
      hasPrevCondition = true;
    }

    return joinClause.toString();
  }

}
