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
package org.icgc.dcc.submission.validation.primary.core;

import java.util.List;

import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.validation.primary.restriction.CodeListRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RangeFieldRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RegexRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RequiredRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction;

import com.google.common.collect.ImmutableList;

public interface RestrictionType {

  public static final List<Class<? extends RestrictionType>> TYPES =
      ImmutableList.<Class<? extends RestrictionType>> of(
          DiscreteValuesRestriction.Type.class,
          RangeFieldRestriction.Type.class,
          RequiredRestriction.Type.class,
          CodeListRestriction.Type.class,
          RegexRestriction.Type.class,
          ScriptRestriction.Type.class);

  public String getType();

  public boolean builds(String type);

  public FlowType flowType();

  public RestrictionTypeSchema getSchema();

  public PlanElement build(String projectKey, Field field, Restriction restriction);

}
