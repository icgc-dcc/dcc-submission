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
package org.icgc.dcc.submission.validation.sample;

import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;

import java.util.Collection;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.sample.core.MetaFileSampleTypeProcessor;
import org.icgc.dcc.submission.validation.sample.core.Samples;
import org.icgc.dcc.submission.validation.sample.parser.SamplesParser;
import org.icgc.dcc.submission.validation.util.ValidationFileParsers;

import com.google.common.collect.ImmutableList;

/**
 * Validator responsible for ensuring consistent sample types between clinical and experimental meta files.
 * <p>
 * This class assumes that prior {@code Validator}s have ensured that clinical data exists and that all files are
 * welformed.
 * 
 * @see https://jira.oicr.on.ca/browse/DCC-2556
 */
@Slf4j
public class SampleTypeValidator implements Validator {

  @Override
  public String getName() {
    return "Sample Type Validator";
  }

  @Override
  public void validate(ValidationContext context) throws InterruptedException {
    log.info("Starting...");

    // Selective validation filtering
    if (!isValidatable(context)) {
      log.info("Validation not required for '{}'. Skipping...", context.getProjectKey());

      return;
    }

    validateSampleTypes(context);
  }

  private void validateSampleTypes(ValidationContext context) {
    // Resolve the reference for validation
    val samples = SamplesParser.parse(context);

    // Verify each requested feature type in turn against the reference
    for (val featureType : getFeatureTypesWithMetaFiles(context)) {
      validateFeatureSampleTypes(context, featureType, samples);
    }
  }

  @SneakyThrows
  private void validateFeatureSampleTypes(ValidationContext context, FeatureType featureType, Samples samples) {
    val metaFileType = featureType.getMetaFileType();
    val metaFileParser = ValidationFileParsers.newMapFileParser(context, metaFileType);

    // Parse and validate each meta file instance for this feature type in turn
    for (val metaFile : context.getFiles(metaFileType)) {
      val metaFileProcessor = new MetaFileSampleTypeProcessor(metaFileType, metaFile, samples, context);

      // Perform actual validation within the processor
      log.info("Processing {}...", metaFile.toString());
      metaFileParser.parse(metaFile, metaFileProcessor);

      // TODO: Consider moving the this lower in the call stack
      // Allow for user canceling between files
      checkInterrupted(getName());
    }
  }

  private static boolean isValidatable(ValidationContext context) {
    // Must have at least one feature type present on the file system
    return !getFeatureTypesWithMetaFiles(context).isEmpty();
  }

  private static Collection<FeatureType> getFeatureTypesWithMetaFiles(ValidationContext context) {
    val featureTypes = ImmutableList.<FeatureType> builder();

    for (val featureType : DataTypes.getFeatureTypes(context.getDataTypes())) {
      if (hasMetaFile(context, featureType)) {
        featureTypes.add(featureType);
      }
    }

    return featureTypes.build();
  }

  private static boolean hasMetaFile(ValidationContext context, FeatureType featureType) {
    val metaFileType = featureType.getMetaFileType();
    val metaFiles = context.getFiles(metaFileType);

    return !metaFiles.isEmpty();
  }

}