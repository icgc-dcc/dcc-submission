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
package org.icgc.dcc.submission.validation;

import org.icgc.dcc.submission.core.AbstractDccModule;
import org.icgc.dcc.submission.dictionary.DictionaryService;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.validation.checker.FirstPassValidator;
import org.icgc.dcc.submission.validation.core.RestrictionContext;
import org.icgc.dcc.submission.validation.core.RestrictionType;
import org.icgc.dcc.submission.validation.planner.DefaultPlanner;
import org.icgc.dcc.submission.validation.planner.Planner;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactory;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactoryProvider;
import org.icgc.dcc.submission.validation.primary.PrimaryValidator;
import org.icgc.dcc.submission.validation.report.ByteOffsetToLineNumber;
import org.icgc.dcc.submission.validation.restriction.CodeListRestriction;
import org.icgc.dcc.submission.validation.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.submission.validation.restriction.RangeFieldRestriction;
import org.icgc.dcc.submission.validation.restriction.RegexRestriction;
import org.icgc.dcc.submission.validation.restriction.RequiredRestriction;
import org.icgc.dcc.submission.validation.restriction.ScriptRestriction;
import org.icgc.dcc.submission.validation.semantic.ReferenceGenomeValidator;
import org.icgc.dcc.submission.validation.service.ValidationService;
import org.icgc.dcc.submission.validation.service.Validator;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

/**
 * Module for the validation subsystem.
 */
public class ValidationModule extends AbstractDccModule {

  /**
   * Config property name.
   */
  private static final String MAX_VALIDATING_CONFIG_PARAM = "validator.max_simultaneous";

  /**
   * Default value for maximum number of concurrent validations.
   */
  private static final int DEFAULT_MAX_VALIDATING = 1;

  @Override
  protected void configure() {
    // TODO: Remove when integration is complete
    bindService(ValidationQueueService.class);
    bind(ValidationService.class);

    // TODO: uncomment when integration is complete
    // bindService(ValidationScheduler.class);
    // bind(ValidationExecutor.class);
    // bind(ValidationExecutor.class).toProvider(new Provider<ValidationExecutor>() {
    //
    // @Inject
    // private Config config;
    //
    // @Override
    // public ValidationExecutor get() {
    // return new ValidationExecutor(getMaxValidating());
    // }
    //
    // private int getMaxValidating() {
    // return config.hasPath(MAX_VALIDATING_CONFIG_PARAM) ?
    // config.getInt(MAX_VALIDATING_CONFIG_PARAM) :
    // DEFAULT_MAX_VALIDATING;
    // }
    //
    // }).in(Singleton.class);

    bind(RestrictionContext.class).toInstance(new RestrictionContext() {

      @Inject
      DictionaryService dictionaryService;

      @Override
      public Optional<CodeList> getCodeList(String codeListName) {
        return dictionaryService.getCodeList(codeListName);
      }

    });
    bind(Planner.class).to(DefaultPlanner.class);
    bind(PlatformStrategyFactory.class).toProvider(PlatformStrategyFactoryProvider.class).in(Singleton.class);
    bindRestrictionTypes();

    // TODO: Enable when integration is complete
    // bindValidators();
  }

  /**
   * Any restrictions added in here should also be added in {@link ValidationTestModule} for testing.
   */
  private void bindRestrictionTypes() {
    Multibinder<RestrictionType> types = Multibinder.newSetBinder(binder(), RestrictionType.class);
    bindRestriction(types, DiscreteValuesRestriction.Type.class);
    bindRestriction(types, RangeFieldRestriction.Type.class);
    bindRestriction(types, RequiredRestriction.Type.class);
    bindRestriction(types, CodeListRestriction.Type.class);
    bindRestriction(types, RegexRestriction.Type.class);
    bindRestriction(types, ScriptRestriction.Type.class);
    requestStaticInjection(ByteOffsetToLineNumber.class);
  }

  private void bindRestriction(Multibinder<RestrictionType> types, Class<? extends RestrictionType> type) {
    types.addBinding().to(type).in(Singleton.class);
  }

  private void bindValidators() {
    Multibinder<Validator> validators = Multibinder.newSetBinder(binder(), Validator.class);
    bindValidator(validators, FirstPassValidator.class);
    bindValidator(validators, PrimaryValidator.class);
    bindValidator(validators, ReferenceGenomeValidator.class);
  }

  private void bindValidator(Multibinder<Validator> validators, Class<? extends Validator> validator) {
    validators.addBinding().to(validator).in(Singleton.class);
  }

}
