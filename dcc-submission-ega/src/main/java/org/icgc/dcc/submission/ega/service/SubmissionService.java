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
package org.icgc.dcc.submission.ega.service;

import static org.icgc.dcc.common.core.json.JsonNodeBuilders.object;
import static org.icgc.dcc.common.core.util.Splitters.COLON;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.NonNull;
import lombok.val;

//@Service
public class SubmissionService {

  @Autowired
  JdbcTemplate template;

  public List<ObjectNode> getRawAccessions(@NonNull String release) {
    val schema = release.toLowerCase();
    val repo = "EGA";

    // @formatter:off
    val sql = 
      "\n"+
      " SELECT\n"+
      "   DISTINCT *\n"+
      " FROM \n"+
      "   (\n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".cnsm_m       WHERE raw_data_repository = \'" + repo + "\' UNION\n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".exp_array_m  WHERE raw_data_repository = \'" + repo + "\' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".exp_seq_m    WHERE raw_data_repository = \'" + repo + "\' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".jcn_m        WHERE raw_data_repository = \'" + repo + "\' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".meth_array_m WHERE raw_data_repository = \'" + repo + "\' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".meth_seq_m   WHERE raw_data_repository = \'" + repo + "\' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".mirna_seq_m  WHERE raw_data_repository = \'" + repo + "\' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".pexp_m       WHERE raw_data_repository = \'" + repo + "\' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".sgv_m        WHERE raw_data_repository = \'" + repo + "\' UNION\n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".ssm_m        WHERE raw_data_repository = \'" + repo + "\' UNION\n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + schema + ".stsm_m       WHERE raw_data_repository = \'" + repo + "\'\n"+
      "   ) AS i\n"+
      " ";
    
    // @formatter:on
    return template.query(sql, (rs, i) -> object()
        .with("projectId", rs.getString(1))
        .with("donorId", rs.getString(2))
        .with("rawAccession", COLON.splitToList(rs.getString(3)))
        .with("analyzedSampleId", rs.getString(4))
        .end());
  }

}
