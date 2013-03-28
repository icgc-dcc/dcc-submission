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
package org.icgc.dcc.validation;

import org.icgc.dcc.core.AbstractDccModule;
import org.icgc.dcc.validation.factory.CascadingStrategyFactory;
import org.icgc.dcc.validation.report.ByteOffsetToLineNumber;
import org.icgc.dcc.validation.restriction.CodeListRestriction;
import org.icgc.dcc.validation.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.validation.restriction.RangeFieldRestriction;
import org.icgc.dcc.validation.restriction.RegexRestriction;
import org.icgc.dcc.validation.restriction.RequiredRestriction;
import org.icgc.dcc.validation.service.ValidationQueueManagerService;
import org.icgc.dcc.validation.service.ValidationService;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

/**
 * Module for the ({@code ValidationQueueManagerService})
 */
public class ValidationModule extends AbstractDccModule {

  private Multibinder<RestrictionType> restrictionTypes;

  @Override
  protected void configure() {
    bindService(ValidationQueueManagerService.class);
    bind(ValidationService.class);
    bind(Planner.class).to(DefaultPlanner.class);
    bind(CascadingStrategyFactory.class).toProvider(CascadingStrategyFactoryProvider.class).in(Singleton.class);
    restrictionTypes = bindRestrictionTypes();
  }

  /**
   * Any restrictions added in here should also be added in {@link ValidationTestModule} for testing.
   */
  private Multibinder<RestrictionType> bindRestrictionTypes() {
    Multibinder<RestrictionType> types = Multibinder.newSetBinder(binder(), RestrictionType.class);
    bindRestriction(types, DiscreteValuesRestriction.Type.class);
    bindRestriction(types, RangeFieldRestriction.Type.class);
    bindRestriction(types, RequiredRestriction.Type.class);
    bindRestriction(types, CodeListRestriction.Type.class);
    bindRestriction(types, RegexRestriction.Type.class);
    requestStaticInjection(ByteOffsetToLineNumber.class);
    return types;
  }

  private void bindRestriction(Multibinder<RestrictionType> types, Class<? extends RestrictionType> type) {
    types.addBinding().to(type).in(Singleton.class);
  }

}
