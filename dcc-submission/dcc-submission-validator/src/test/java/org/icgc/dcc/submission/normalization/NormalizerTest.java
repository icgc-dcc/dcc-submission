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
import java.util.Map.Entry;
import java.util.Set;

import lombok.SneakyThrows;

import org.icgc.dcc.submission.normalization.NormalizationStep;
import org.icgc.dcc.submission.normalization.Normalizer;
import org.icgc.dcc.submission.normalization.steps.AlleleMasking;
import org.icgc.dcc.submission.normalization.steps.FinalCounting;
import org.icgc.dcc.submission.normalization.steps.InitialCounting;
import org.icgc.dcc.submission.normalization.steps.MutationRebuilding;
import org.icgc.dcc.submission.normalization.steps.PreMasking;
import org.icgc.dcc.submission.normalization.steps.PrimaryKeyGeneration;
import org.icgc.dcc.submission.normalization.steps.RedundantObservationRemoval;
import org.icgc.dcc.submission.normalization.steps.hacks.HackFieldDiscarding;
import org.icgc.dcc.submission.normalization.steps.hacks.HackNewFieldsSynthesis;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigMergeable;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigValue;

public class NormalizerTest {

  @SneakyThrows
  @Test
  public void test_normalize() {
    new File("/tmp/deleteme").delete(); // TODO: improve
    String projectKey = "dummy_project";

    Config config = new Config() {

      @Override
      public Config withoutPath(String path) {
        return null;
      }

      @Override
      public Config withOnlyPath(String path) {
        return null;
      }

      @Override
      public Config withFallback(ConfigMergeable other) {
        return null;
      }

      @Override
      public ConfigObject root() {
        return null;
      }

      @Override
      public Config resolve(ConfigResolveOptions options) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Config resolve() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ConfigOrigin origin() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean hasPath(String path) {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public ConfigValue getValue(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public List<String> getStringList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getString(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public List<? extends ConfigObject> getObjectList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ConfigObject getObject(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public List<Number> getNumberList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Number getNumber(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public List<Long> getNanosecondsList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Long getNanoseconds(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public List<Long> getMillisecondsList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Long getMilliseconds(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public List<Long> getLongList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public long getLong(String path) {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public ConfigList getList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public List<Integer> getIntList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public int getInt(String path) {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public List<Double> getDoubleList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public double getDouble(String path) {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public List<? extends Config> getConfigList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Config getConfig(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public List<Long> getBytesList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Long getBytes(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public List<Boolean> getBooleanList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean getBoolean(String path) {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public List<? extends Object> getAnyRefList(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Object getAnyRef(String path) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Set<Entry<String, ConfigValue>> entrySet() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void checkValid(Config reference, String... restrictToPaths) {
        // TODO Auto-generated method stub

      }
    };

    new Normalizer( // TODO: service
        projectKey,
        new ImmutableList.Builder<NormalizationStep>() // Order matters for some steps

            .add(new InitialCounting())

            .add(new HackFieldDiscarding("mutation")) // Hack
            .add(new HackNewFieldsSynthesis("mutated_from_allele", "mutated_to_allele")) // Hack

            // Must happen before rebuilding the mutation
            .add(new PreMasking()) // Must happen no matter what
            .add(new AlleleMasking(config)) // May be skipped (partially or not)

            // Must happen after allele masking
            .add(
                new RedundantObservationRemoval( // May be skipped
                    getGroup(),
                    SUBMISSION_OBSERVATION_ANALYSIS_ID))
            .add(new MutationRebuilding())

            // Must happen after removing duplicates and allele masking
            .add(new PrimaryKeyGeneration())

            .add(new HackFieldDiscarding("mutated_from_allele")) // Hack
            .add(new HackFieldDiscarding("mutated_to_allele")) // Hack

            .add(new FinalCounting())

            .build(),
        config)
        .normalize(); // TODO: actually report

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
