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

import java.util.List;

import lombok.Value;
import lombok.experimental.Builder;

import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.normalization.steps.RedundantObservationRemoval;

import com.google.common.collect.ImmutableList;

/**
 * Common context object passed to all {@link NormalizationStep}s.
 */
public interface NormalizationContext {

  /**
   * Returns the list of fields on which to group by in order to detect redundant observations.
   */
  List<String> getObservationUniqueFields();

  @Value
  @Builder
  static final class DefaultNormalizationContext implements NormalizationContext {

    /**
     * See {@link NormalizationContext#getObservationUniqueFields()}.
     */
    private final ImmutableList<String> observationUniqueFields;

    /**
     * Creates the default {@link NormalizationContext}.
     */
    static NormalizationContext getNormalizationContext(Dictionary dictionary, SubmissionFileType type) {
      return DefaultNormalizationContext.builder()
          .observationUniqueFields(
              RedundantObservationRemoval.getObservationUniqueFields(
                  dictionary,
                  type))
          .build();
    }
  }
}
