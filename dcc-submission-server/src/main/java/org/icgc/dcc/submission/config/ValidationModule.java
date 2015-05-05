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
package org.icgc.dcc.submission.config;

import static com.google.common.base.Preconditions.checkState;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.icgc.dcc.common.core.Component.NORMALIZER;
import static org.icgc.dcc.common.core.util.URLs.getUrl;

import java.net.URL;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.common.core.tcga.TCGAClient;
import org.icgc.dcc.common.hadoop.fs.DccFileSystem2;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.service.AbstractDccModule;
import org.icgc.dcc.submission.service.DictionaryService;
import org.icgc.dcc.submission.validation.ValidationExecutor;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.first.FirstPassValidator;
import org.icgc.dcc.submission.validation.key.KeyValidator;
import org.icgc.dcc.submission.validation.norm.NormalizationValidator;
import org.icgc.dcc.submission.validation.pcawg.PCAWGValidator;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGDictionary;
import org.icgc.dcc.submission.validation.pcawg.external.PanCancerClient;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategyFactory;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategyFactoryProvider;
import org.icgc.dcc.submission.validation.primary.PrimaryValidator;
import org.icgc.dcc.submission.validation.primary.core.RestrictionContext;
import org.icgc.dcc.submission.validation.primary.core.RestrictionType;
import org.icgc.dcc.submission.validation.primary.planner.Planner;
import org.icgc.dcc.submission.validation.primary.report.ByteOffsetToLineNumber;
import org.icgc.dcc.submission.validation.rgv.ReferenceGenomeValidator;
import org.icgc.dcc.submission.validation.rgv.reference.PicardReferenceGenome;
import org.icgc.dcc.submission.validation.sample.SampleTypeValidator;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;

/**
 * Module that wires together components of the validation subsystem.
 */
@Slf4j
public class ValidationModule extends AbstractDccModule {

  /**
   * Validator specifications.
   * <p>
   * See {@code application.conf}.
   */
  private static final String FIRST_PASS_VALIDATOR_CONFIG_VALUE = "fpv";
  private static final String PRIMARY_VALIDATOR_CONFIG_VALUE = "pv";
  private static final String PCAWG_VALIDATOR_CONFIG_VALUE = "pcawg";
  private static final String KEY_VALIDATOR_CONFIG_VALUE = "kv";
  private static final String REFERENCE_GENOME_VALIDATOR_CONFIG_VALUE = "rgv";
  private static final String SAMPLE_TYPE_VALIDATOR_CONFIG_VALUE = "sample";
  private static final String NORMALIZATION_VALIDATOR_CONFIG_VALUE = "nv";

  /**
   * Config property names.
   */
  private static final String MAX_VALIDATING_CONFIG_PARAM = "validator.max_simultaneous";
  private static final String FASTA_FILE_PATH_CONFIG_PARAM = "reference.fasta";
  private static final String NORMALIZER_CONFIG_PARAM = NORMALIZER.getId();
  private static final String PCAWG_CONFIG_PARAM = "pcawg";
  private static final String PCAWG_DICTIONARY_URL_CONFIG_PARAM = "dictionary_url";

  /**
   * Default value for maximum number of concurrent validations.
   */
  private static final int DEFAULT_MAX_VALIDATING = 1;

  @Override
  protected void configure() {
    requestStaticInjection(ByteOffsetToLineNumber.class);
    bind(SubmissionPlatformStrategyFactory.class).toProvider(SubmissionPlatformStrategyFactoryProvider.class).in(
        Singleton.class);
    bind(Planner.class).in(Singleton.class);

    // Set binder will preserve bind order as iteration order for injectees
    val types = newSetBinder(binder(), RestrictionType.class);
    for (val type : RestrictionType.TYPES) {
      types.addBinding().to(type).in(Singleton.class);
    }
  }

  @Provides
  @Singleton
  public ValidationExecutor validationExecutor(Config config) {
    val path = MAX_VALIDATING_CONFIG_PARAM;
    val maxValidating = config.hasPath(path) ? config.getInt(path) : DEFAULT_MAX_VALIDATING;

    return new ValidationExecutor(maxValidating);
  }

  @Provides
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
  @Provides
  @Singleton
  public DccFileSystem2 dccFileSystem2(Config config, FileSystem fileSystem) {
    val rootDir = get(config, "fs.root");
    val hdfs = get(config, "fs.url").startsWith("hdfs");

    return new DccFileSystem2(fileSystem, rootDir, hdfs);
  }

  @Provides
  @Singleton
  public Set<Validator> validators(Config config, DccFileSystem2 dccFileSystem2, Planner planner) {
    // Bind common components

    // Set binder will preserve bind order as iteration order for injectees
    val validators = Sets.<Validator> newLinkedHashSet();

    // Bind validators and their execution ordering
    if (config.hasPath("validators")) {
      val values = config.getList("validators").unwrapped();
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
          validators.add(pcawgValidator(config));
        } else if (value.equals(REFERENCE_GENOME_VALIDATOR_CONFIG_VALUE)) {
          validators.add(referenceGenomeValidator(config));
        } else if (value.equals(SAMPLE_TYPE_VALIDATOR_CONFIG_VALUE)) {
          validators.add(sampleTypeValidator());
        } else if (value.equals(NORMALIZATION_VALIDATOR_CONFIG_VALUE)) {
          validators.add(normalizationValidator(config, dccFileSystem2));
        } else {
          checkState(false, "Invalid validator specification '%s'", value);
        }
      }
    } else {
      // Default validators and validator ordering
      validators.add(firstPassValidator());
      validators.add(primaryValidator(planner));
      validators.add(keyValidator());
      validators.add(pcawgValidator(config));
      validators.add(referenceGenomeValidator(config));
      validators.add(sampleTypeValidator());
      validators.add(normalizationValidator(config, dccFileSystem2));
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
  private static Validator pcawgValidator(Config config) {
    val path = PCAWG_CONFIG_PARAM;
    val defaultValue = PCAWGDictionary.DEFAULT_PCAWG_DICTIONARY_URL;

    URL dictionaryUrl = config.hasPath(path) ?
        getPCAWGDictionaryURL(config.getConfig(path), defaultValue) :
        defaultValue;

    log.info("Using PCAWG dictionary url: {}", dictionaryUrl);
    log.info("PCAWG dictionary contents: {}", Resources.toString(dictionaryUrl, UTF_8));

    return new PCAWGValidator(new PanCancerClient(), new TCGAClient(), new PCAWGDictionary(dictionaryUrl));
  }

  private static URL getPCAWGDictionaryURL(Config pcawgConfig, URL defaultValue) {
    val path = PCAWG_DICTIONARY_URL_CONFIG_PARAM;

    return pcawgConfig.hasPath(path) ? getUrl(pcawgConfig.getString(path)) : defaultValue;
  }

  private static Validator referenceGenomeValidator(Config config) {
    val fastaFilePath = get(config, FASTA_FILE_PATH_CONFIG_PARAM);

    return new ReferenceGenomeValidator(new PicardReferenceGenome(fastaFilePath));
  }

  private static Validator sampleTypeValidator() {
    return new SampleTypeValidator();
  }

  private static Validator normalizationValidator(Config config, DccFileSystem2 dccFileSystem2) {
    val normConfig = config.getConfig(NORMALIZER_CONFIG_PARAM);

    return NormalizationValidator.getDefaultInstance(dccFileSystem2, normConfig);
  }

  private static String get(Config config, String name) {
    checkState(config.hasPath(name), "'%s' should be present in the config", name);

    return config.getString(name);
  }

}
