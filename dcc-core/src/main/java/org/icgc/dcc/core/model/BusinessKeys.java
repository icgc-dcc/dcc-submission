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
package org.icgc.dcc.core.model;

import java.util.List;

import com.google.common.collect.ImmutableList;

import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.core.model.FieldNames.OBSERVATION_ASSEMBLY_VERSION;
import static org.icgc.dcc.core.model.FieldNames.OBSERVATION_CHROMOSOME;
import static org.icgc.dcc.core.model.FieldNames.OBSERVATION_CHROMOSOME_END;
import static org.icgc.dcc.core.model.FieldNames.OBSERVATION_CHROMOSOME_START;
import static org.icgc.dcc.core.model.FieldNames.OBSERVATION_MUTATION;
import static org.icgc.dcc.core.model.FieldNames.OBSERVATION_MUTATION_TYPE;

/**
 * Contains business keys from the standpoint of the loader entities (not the submission files').
 */
public class BusinessKeys {

  //@formatter:off
  public static final String PROJECT = "project";
  public static final String DONOR = "donor";
  public static final String SPECIMEN = "specimen";
  public static final String SAMPLE = "sample";
  public static final String MUTATION = "mutation";

  public static final List<String> MUTATION_BUSINESS_KEY_META_PART = newArrayList(OBSERVATION_ASSEMBLY_VERSION);
  public static final List<String> MUTATION_BUSINESS_KEY_PRIMARY_PART = newArrayList(
      OBSERVATION_CHROMOSOME, 
      OBSERVATION_CHROMOSOME_START,
      OBSERVATION_CHROMOSOME_END, 
      OBSERVATION_MUTATION_TYPE, 
      OBSERVATION_MUTATION);

  public static final List<String> MUTATION_BUSINESS_KEY = ImmutableList.<String> builder()
      .addAll(MUTATION_BUSINESS_KEY_PRIMARY_PART).addAll(MUTATION_BUSINESS_KEY_META_PART).build();
  // @formatter:on
}
