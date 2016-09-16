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
package org.icgc.dcc.submission.server.config;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.common.ega.client.EGAClient;
import org.icgc.dcc.common.hadoop.fs.DccFileSystem2;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.server.service.DictionaryService;
import org.icgc.dcc.submission.validation.ValidationExecutor;
import org.icgc.dcc.submission.validation.accession.AccessionValidator;
import org.icgc.dcc.submission.validation.accession.core.AccessionDictionary;
import org.icgc.dcc.submission.validation.accession.ega.EGAFileAccessionValidator;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.first.FirstPassValidator;
import org.icgc.dcc.submission.validation.key.KeyValidator;
import org.icgc.dcc.submission.validation.norm.NormalizationValidator;
import org.icgc.dcc.submission.validation.pcawg.PCAWGValidator;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGDictionary;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGSampleSheet;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategyFactory;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategyFactoryProvider;
import org.icgc.dcc.submission.validation.primary.PrimaryValidator;
import org.icgc.dcc.submission.validation.primary.core.RestrictionContext;
import org.icgc.dcc.submission.validation.primary.core.RestrictionType;
import org.icgc.dcc.submission.validation.primary.planner.Planner;
import org.icgc.dcc.submission.validation.primary.report.ByteOffsetToLineNumber;
import org.icgc.dcc.submission.validation.primary.restriction.CodeListRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RangeFieldRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RegexRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RequiredRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction;
import org.icgc.dcc.submission.validation.rgv.ReferenceGenomeValidator;
import org.icgc.dcc.submission.validation.rgv.reference.HtsjdkReferenceGenome;
import org.icgc.dcc.submission.validation.sample.SampleTypeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Module that wires together components of the validation subsystem.
 */
@Slf4j
@Configuration
public class ValidationConfig extends AbstractConfig {

  /**
   * Validator specifications.
   * <p>
   * See {@code application.yaml}.
   */
  private static final String FIRST_PASS_VALIDATOR_CONFIG_VALUE = "fpv";
  private static final String PRIMARY_VALIDATOR_CONFIG_VALUE = "pv";
  private static final String PCAWG_VALIDATOR_CONFIG_VALUE = "pcawg";
  private static final String KEY_VALIDATOR_CONFIG_VALUE = "kv";
  private static final String REFERENCE_GENOME_VALIDATOR_CONFIG_VALUE = "rgv";
  private static final String SAMPLE_TYPE_VALIDATOR_CONFIG_VALUE = "sample";
  private static final String NORMALIZATION_VALIDATOR_CONFIG_VALUE = "nv";
  private static final String ACCESSION_VALIDATOR_CONFIG_VALUE = "accession";

  @Autowired
  FileSystem fileSystem;

  @PostConstruct
  public void init() {
    ByteOffsetToLineNumber.setFileSystem(fileSystem);
  }

  @Bean
  public Planner planner(Set<RestrictionType> restrictions) {
    return new Planner(restrictions);
  }

  @Bean
  public SubmissionPlatformStrategyFactoryProvider submissionPlatformStrategyFactoryProvider() {
    return singleton(SubmissionPlatformStrategyFactoryProvider.class);
  }

  @Bean
  public SubmissionPlatformStrategyFactory submissionPlatformStrategyFactory() {
    return submissionPlatformStrategyFactoryProvider().get();
  }

  @Bean
  public ValidationExecutor validationExecutor(SubmissionProperties properties) {
    val maxValidating = properties.getValidator().getMaxSimultaneous();

    return new ValidationExecutor(maxValidating);
  }

  @Bean
  @Order(1)
  public RestrictionType discreteValuesRestrictionType() {
    return singleton(DiscreteValuesRestriction.Type.class);
  }

  @Bean
  @Order(2)
  public RestrictionType rangeFieldRestrictionType() {
    return singleton(RangeFieldRestriction.Type.class);
  }

  @Bean
  @Order(3)
  public RestrictionType requiredRestrictionType() {
    return singleton(RequiredRestriction.Type.class);
  }

  @Bean
  @Order(4)
  public RestrictionType codeListRestrictionType() {
    return singleton(CodeListRestriction.Type.class);
  }

  @Bean
  @Order(5)
  public RestrictionType regexRestrictionType() {
    return singleton(RegexRestriction.Type.class);
  }

  @Bean
  @Order(6)
  public RestrictionType scriptRestrictionType() {
    return singleton(ScriptRestriction.Type.class);
  }

  @Bean
  @Scope(scopeName = "prototype")
  public RestrictionContext restrictionContext(final DictionaryService dictionaryService) {
    return new RestrictionContext() {

      @Override
      public Optional<CodeList> getCodeList(String codeListName) {
        return dictionaryService.getCodeList(codeListName);
      }

    };
  }

  /**
   * Near clone of {@link LoaderModule} - maintain both at the same time until DCC-1876 is addressed.
   * <p>
   * TODO: This is temporary until DCC-1876 is addressed.
   * <p>
   * TODO: address hard-codings
   */
  @Bean
  public DccFileSystem2 submissionFileSystem2(SubmissionProperties properties, FileSystem fileSystem) {
    val rootDir = properties.getFs().getRoot();
    val hdfs = properties.getFs().getUrl().startsWith("hdfs");

    return new DccFileSystem2(fileSystem, rootDir, hdfs);
  }

  @Bean
  public Set<Validator> validators(SubmissionProperties properties, DccFileSystem2 submissionFileSystem2,
      Planner planner) {
    // Bind common components

    // Set binder will preserve bind order as iteration order for injectees
    val validators = Sets.<Validator> newLinkedHashSet();

    // Bind validators and their execution ordering
    val values = properties.getValidators();
    if (!values.isEmpty()) {
      log.info("Binding validators in the following order: {}", values);

      // Externally configured validators and validator ordering
      for (val value : values) {
        if (value.equals(FIRST_PASS_VALIDATOR_CONFIG_VALUE)) {
          validators.add(firstPassValidator());
        } else if (value.equals(PRIMARY_VALIDATOR_CONFIG_VALUE)) {
          validators.add(primaryValidator(planner));
        } else if (value.equals(KEY_VALIDATOR_CONFIG_VALUE)) {
          validators.add(keyValidator());
        } else if (value.equals(PCAWG_VALIDATOR_CONFIG_VALUE)) {
          validators.add(pcawgValidator(properties));
        } else if (value.equals(REFERENCE_GENOME_VALIDATOR_CONFIG_VALUE)) {
          validators.add(referenceGenomeValidator(properties));
        } else if (value.equals(SAMPLE_TYPE_VALIDATOR_CONFIG_VALUE)) {
          validators.add(sampleTypeValidator());
        } else if (value.equals(NORMALIZATION_VALIDATOR_CONFIG_VALUE)) {
          validators.add(normalizationValidator(properties, submissionFileSystem2));
        } else if (value.equals(ACCESSION_VALIDATOR_CONFIG_VALUE)) {
          validators.add(accessionValidator(properties));
        } else {
          checkState(false, "Invalid validator specification '%s'", value);
        }
      }
    } else {
      // Default validators and validator ordering
      validators.add(firstPassValidator());
      validators.add(primaryValidator(planner));
      validators.add(keyValidator());
      validators.add(pcawgValidator(properties));
      validators.add(referenceGenomeValidator(properties));
      validators.add(sampleTypeValidator());
      validators.add(normalizationValidator(properties, submissionFileSystem2));
      validators.add(accessionValidator(properties));
    }

    return validators;
  }

  private static Validator firstPassValidator() {
    return new FirstPassValidator();
  }

  private static Validator keyValidator() {
    return new KeyValidator();
  }

  private static Validator primaryValidator(Planner planner) {
    return new PrimaryValidator(planner);
  }

  @SneakyThrows
  private static Validator pcawgValidator(SubmissionProperties properties) {
    val dictionaryUrl =
        firstNonNull(properties.getPcawg().getDictionaryUrl(), PCAWGDictionary.DEFAULT_PCAWG_DICTIONARY_URL);
    log.info("Using PCAWG dictionary url: {}", dictionaryUrl);

    val sampleSheetUrl =
        firstNonNull(properties.getPcawg().getSampleSheetUrl(), PCAWGSampleSheet.DEFAULT_PCAWG_SAMPLE_SHEET_URL);
    log.info("Using PCAWG sample sheet url: {}", sampleSheetUrl);

    return new PCAWGValidator(new PCAWGDictionary(dictionaryUrl), new PCAWGSampleSheet(sampleSheetUrl));
  }

  private static Validator referenceGenomeValidator(SubmissionProperties properties) {
    val fastaFilePath = properties.getReference().getFasta();

    return new ReferenceGenomeValidator(new HtsjdkReferenceGenome(fastaFilePath));
  }

  private static Validator sampleTypeValidator() {
    return new SampleTypeValidator();
  }

  private static Validator normalizationValidator(SubmissionProperties properties,
      DccFileSystem2 submissionFileSystem2) {
    return NormalizationValidator.getDefaultInstance(submissionFileSystem2, properties.getNormalizer());
  }

  private static Validator accessionValidator(SubmissionProperties properties) {
    val dictionaryUrl =
        firstNonNull(properties.getAccession().getDictionaryUrl(),
            AccessionDictionary.DEFAULT_ACCESSION_DICTIONARY_URL);
    log.info("Using accession dictionary url: {}", dictionaryUrl);
    val egaClient = new EGAClient(properties.getEga().getUsername(), properties.getEga().getPassword());
    val egaValidator = new EGAFileAccessionValidator(egaClient);

    return new AccessionValidator(new AccessionDictionary(dictionaryUrl), egaValidator);
  }

}
