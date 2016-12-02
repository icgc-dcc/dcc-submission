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

import lombok.val;

@Service
public class SubmissionService {

  @Autowired
  JdbcTemplate template;

  public List<ObjectNode> getRawAccessions() {
    val release = "icgc22";
    val repo = "EGA";

    // @formatter:off
    val sql = 
      "\n"+
      " SELECT\n"+
      "   DISTINCT *\n"+
      " FROM \n"+
      "   (\n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".cnsm_m       WHERE raw_data_repository = \'" + repo + " \' UNION\n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".exp_array_m  WHERE raw_data_repository = \'" + repo + " \' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".exp_seq_m    WHERE raw_data_repository = \'" + repo + " \' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".jcn_m        WHERE raw_data_repository = \'" + repo + " \' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".meth_array_m WHERE raw_data_repository = \'" + repo + " \' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".meth_seq_m   WHERE raw_data_repository = \'" + repo + " \' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".mirna_seq_m  WHERE raw_data_repository = \'" + repo + " \' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".pexp_m       WHERE raw_data_repository = \'" + repo + " \' UNION \n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".sgv_m        WHERE raw_data_repository = \'" + repo + " \' UNION\n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".ssm_m        WHERE raw_data_repository = \'" + repo + " \' UNION\n"+
      "     SELECT project_id, donor_id, raw_data_accession, analyzed_sample_id FROM " + release + ".stsm_m       WHERE raw_data_repository = \'" + repo + " \'\n"+
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
