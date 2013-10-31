/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.normalization;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.readLines;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_ANALYSIS_ID;

import java.io.File;
import java.util.List;

import lombok.SneakyThrows;

import org.icgc.dcc.submission.normalization.steps.AlleleMasking;
import org.icgc.dcc.submission.normalization.steps.FinalCounting;
import org.icgc.dcc.submission.normalization.steps.InitialCounting;
import org.icgc.dcc.submission.normalization.steps.MutationRebuilding;
import org.icgc.dcc.submission.normalization.steps.PrimaryKeyGeneration;
import org.icgc.dcc.submission.normalization.steps.RedundantObservationRemoval;
import org.icgc.dcc.submission.normalization.steps.hacks.HackFieldDiscarding;
import org.icgc.dcc.submission.normalization.steps.hacks.HackNewFieldsSynthesis;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class NormalizerTest {

  @SneakyThrows
  @Test
  public void test_normalize() {
    new File("/tmp/deleteme").delete(); // TODO: improve
    String projectKey = "dummy_project";

    new Normalizer( // TODO: service
        projectKey,
        new ImmutableList.Builder<NormalizationStep>() // Order matters for some steps

            .add(new InitialCounting())

            .add(new HackFieldDiscarding("mutation")) // Hack
            .add(new HackNewFieldsSynthesis("mutated_from_allele", "mutated_to_allele")) // Hack

            // Must happen before rebuilding the mutation
            .add(new AlleleMasking())

            // Must happen after allele masking
            .add(new RedundantObservationRemoval(
                getGroup(),
                SUBMISSION_OBSERVATION_ANALYSIS_ID))
            .add(new MutationRebuilding())

            // Must happen after removing duplicates and allele masking
            .add(new PrimaryKeyGeneration())

            .add(new HackFieldDiscarding("mutated_from_allele")) // Hack
            .add(new HackFieldDiscarding("mutated_to_allele")) // Hack

            .add(new FinalCounting())

            .build())
        .normalize(null); // TODO: actually report

    List<String> result = readLines(new File("/tmp/deleteme"), UTF_8); // TODO: improve
    List<String> ref = readLines(new File("/home/tony/git/git0/data-submission/ref"), UTF_8); // TODO: improve
    int refSize = ref.size();
    int resultSize = result.size();

    checkState(resultSize == refSize, resultSize + ", " + refSize);
    for (int i = 0; i < refSize; i++) {
      String resultLine = removeRandomUUID(result.get(i));
      String refLine = removeRandomUUID(ref.get(i));
      checkState(resultLine.equals(refLine), "\n\t" + resultLine + "\n\t" + refLine);
    }
  }

  private ImmutableList<String> getGroup() {
    List<String> group = newArrayList( // TODO: get from dictionary
        "analysis_id", "analyzed_sample_id", "chromosome", "chromosome_end", "chromosome_start",
        "chromosome_strand", "control_genotype", "db_xref", "expressed_allele", "is_annotated", "mutation",
        "mutation_id", "mutation_type", "note", "probability", "quality_score", "read_count",
        "reference_genome_allele", "refsnp_allele", "refsnp_strand", "tumour_genotype", "uri",
        "verification_platform", "verification_status", "xref_ensembl_var_id");
    group.remove(SUBMISSION_OBSERVATION_ANALYSIS_ID);
    group.remove("mutation"); // Hack
    return ImmutableList.<String> copyOf(group);
  }

  private String removeRandomUUID(String row) {
    return row.replaceAll("\t[^\t]*$", ""); // TODO: improve
  }

}
