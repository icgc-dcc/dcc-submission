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
package org.icgc.dcc.submission.loader.record;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.lang.String.format;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CONTROL_GENOTYPE;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_TUMOUR_GENOTYPE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.common.core.json.Jackson;
import org.icgc.dcc.common.core.model.SpecialValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

@RequiredArgsConstructor
public class ExportRecordConverter {

  private static final String MISSING_VALUE = SpecialValue.NOT_APPLICABLE_CODE;
  private static final Splitter ALLELES_SPLITTER = Splitter.on("/");
  private static final Collection<String> MASKED_FILE_TYPES = ImmutableList.of("ssm_p");

  /**
   * Configuration
   */
  private final List<String> controlledFields;
  private final boolean checkMaskedFile;

  private final Map<String, String> storage = Maps.newHashMap();

  public ExportRecordConverter(@NonNull ObjectNode dictionary, @NonNull String type) {
    this.controlledFields = resolveControlledFilds(dictionary, type);
    this.checkMaskedFile = MASKED_FILE_TYPES.contains(type);
  }

  public Map<String, String> convert(@NonNull Map<String, String> record) {
    if (checkMaskedFile && isControlledMutation(record)) {
      return null;
    }

    if (!controlledFields.isEmpty()) {
      storage.clear();
      addOpenData(record);

      return storage;
    }

    return null;
  }

  private boolean isControlledMutation(Map<String, String> record) {
    val referenceGenomeAllele = record.get(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE);
    checkNotNull(referenceGenomeAllele, "Expected %s", referenceGenomeAllele);
    val controlGenotype = record.get(SUBMISSION_OBSERVATION_CONTROL_GENOTYPE);
    checkNotNull(controlGenotype, "Expected %s", controlGenotype);
    val tumourGenotype = record.get(SUBMISSION_OBSERVATION_TUMOUR_GENOTYPE);
    checkNotNull(tumourGenotype, "Expected %s", tumourGenotype);
    val mutatedToAllele = record.get(SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE);
    checkNotNull(mutatedToAllele, "Expected %s", mutatedToAllele);

    return !matchesAllControlAlleles(referenceGenomeAllele, controlGenotype)
        || !matchesAllTumourAllelesButTo(referenceGenomeAllele, tumourGenotype, mutatedToAllele);
  }

  private boolean matchesAllControlAlleles(String referenceGenomeAllele, String controlGenotype) {
    val controlAlleles = getUniqueAlleles(controlGenotype);
    for (val controlAllele : controlAlleles) {
      if (!referenceGenomeAllele.equals(controlAllele)) {
        return false;
      }
    }

    return true;
  }

  private boolean matchesAllTumourAllelesButTo(String referenceGenomeAllele, String tumourGenotype,
      String mutatedToAllele) {
    for (val tumourAllele : getTumourAllelesMinusToAllele(tumourGenotype, mutatedToAllele)) {
      if (!referenceGenomeAllele.equals(tumourAllele)) {
        return false;
      }
    }
    return true;
  }

  private Set<String> getTumourAllelesMinusToAllele(String tumourGenotype, String mutatedToAllele) {
    val alleles = getUniqueAlleles(tumourGenotype);
    val removed = alleles.remove(mutatedToAllele);
    checkState(
        removed,
        "'%s' ('%s') is expected to be in '%s' ('%s') as per primary validation rules",
        mutatedToAllele, SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE, tumourGenotype,
        SUBMISSION_OBSERVATION_TUMOUR_GENOTYPE);
    return alleles;
  }

  private static Set<String> getUniqueAlleles(String controlGenotype) {
    return newLinkedHashSet(ALLELES_SPLITTER.split(controlGenotype));
  }

  private void addOpenData(Map<String, String> record) {
    for (val entry : record.entrySet()) {
      val fieldName = entry.getKey();
      val value = controlledFields.contains(fieldName) ? MISSING_VALUE : entry.getValue();
      storage.put(fieldName, value);
    }
  }

  private static List<String> resolveControlledFilds(ObjectNode dictionary, String type) {
    val files = dictionary.get("files");
    val fileMeta = filterFiles(files, type);

    val controlledFields = ImmutableList.<String> builder();
    for (val field : fileMeta.get("fields")) {
      val controlled = field.get("controlled").asBoolean();
      if (controlled) {
        controlledFields.add(field.get("name").textValue());
      }
    }

    return controlledFields.build();
  }

  private static ObjectNode filterFiles(JsonNode files, String type) {
    for (val file : files) {
      val fileName = file.get("name").textValue();
      if (type.equals(fileName)) {
        return Jackson.asObjectNode(file);
      }
    }

    throw new IllegalArgumentException(format("File failed to resolve file from type %s", type));
  }

}
