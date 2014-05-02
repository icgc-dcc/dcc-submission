/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.core.fi;

import static com.google.common.collect.ImmutableList.of;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.ConsequenceType.FRAMESHIFT_VARIANT;
import static org.icgc.dcc.core.model.ConsequenceType.INITIATOR_CODON_VARIANT;
import static org.icgc.dcc.core.model.ConsequenceType.MISSENSE;
import static org.icgc.dcc.core.model.ConsequenceType.NON_CONSERVATIVE_MISSENSE_VARIANT;
import static org.icgc.dcc.core.model.ConsequenceType.STOP_GAINED;
import static org.icgc.dcc.core.model.ConsequenceType.STOP_LOST;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.core.model.ConsequenceType;

/**
 * Composite functional impacts.
 * 
 * @see https://wiki.oicr.on.ca/display/DCCSOFT/SSM+functional+impact+annotation
 */
@Getter
@RequiredArgsConstructor(access = PRIVATE)
public enum CompositeImpactCategory implements ImpactPredictorCategory {

  HIGH("High"),
  MEDIUM("Medium"),
  LOW("Low"),
  UNKNOWN("Unknown");

  private static final List<ConsequenceType> NON_MISSENSE_HIGH_IMPACT_CONSEQUENCE_TYPES = of(
      FRAMESHIFT_VARIANT,
      NON_CONSERVATIVE_MISSENSE_VARIANT,
      INITIATOR_CODON_VARIANT,
      STOP_GAINED,
      STOP_LOST);

  private final String id;

  public static CompositeImpactCategory byId(@NonNull String id) {
    for (val value : values()) {
      if (value.getId().equals(id)) {
        return value;
      }
    }

    throw new IllegalArgumentException("Unknown id '" + id + "'  for " + CompositeImpactCategory.class);
  }

  public static CompositeImpactCategory calculate(ConsequenceType consequenceType,
      Map<ImpactPredictorType, ImpactPredictorCategory> predictions) {
    if (!isMissense(consequenceType)) {
      return isNonMissenseHighImpact(consequenceType) ? HIGH : UNKNOWN;
    } else {
      if (predictions == null || predictions.isEmpty()) {
        return UNKNOWN;
      }

      val fathmmCategory = predictions.get(ImpactPredictorType.FATHMM);
      val maCategory = predictions.get(ImpactPredictorType.MUTATION_ASSESSOR);

      if (FathmmImpactCategory.DAMAGING == fathmmCategory || MutationAssessorImpactCategory.HIGH == maCategory) {
        return HIGH;
      } else if (maCategory == MutationAssessorImpactCategory.MEDIUM) {
        return MEDIUM;
      } else {
        return LOW;
      }
    }
  }

  private static boolean isNonMissenseHighImpact(ConsequenceType consequenceType) {
    return NON_MISSENSE_HIGH_IMPACT_CONSEQUENCE_TYPES.contains(consequenceType);
  }

  private static boolean isMissense(ConsequenceType consequenceType) {
    return MISSENSE.equals(consequenceType);
  }

  @Override
  public int getPriority() {
    return values().length - ordinal();
  }

  @Override
  public ImpactPredictorType getPredictorType() {
    return ImpactPredictorType.COMPOSITE;
  }

}
