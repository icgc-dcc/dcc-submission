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
package org.icgc.dcc.submission.validation.key;

import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_P;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

/**
 * 
 */
public class KVConstants {

  public static final Splitter TAB_SPLITTER = Splitter.on('\t');
  public static final ObjectMapper MAPPER = new ObjectMapper();

  // TODO: move elsewhere
  public static final Map<KVFileType, KVFileType> RELATIONS = // TODO: all necessary?
      new ImmutableMap.Builder<KVFileType, KVFileType>()
          .put(SPECIMEN, DONOR)
          .put(SAMPLE, SPECIMEN)
          .put(SSM_M, SAMPLE)
          .put(SSM_P, SSM_M)
          .put(CNSM_M, SAMPLE)
          .put(CNSM_P, CNSM_M)
          .put(CNSM_S, CNSM_P)
          .build();

  // TODO: translate to Strings rather? + make map per file type/submission type?
  public static final List<Integer> DONOR_PKS = newArrayList(0);
  public static final List<Integer> SPECIMEN_FKS = newArrayList(0);
  public static final List<Integer> SPECIMEN_PKS = newArrayList(1);
  public static final List<Integer> SAMPLE_FKS = newArrayList(1);
  public static final List<Integer> SAMPLE_PKS = newArrayList(0);
  public static final List<Integer> SSM_M_FKS1 = newArrayList(1); // Tumour
  public static final List<Integer> SSM_M_FKS2 = newArrayList(2); // Control
  public static final List<Integer> SSM_M_PKS = newArrayList(0, SSM_M_FKS1.get(0));
  public static final List<Integer> SSM_P_FKS = newArrayList(0, 1);
  public static final List<Integer> CNSM_M_FKS1 = newArrayList(1); // Tumour
  public static final List<Integer> CNSM_M_FKS2 = newArrayList(2); // Control
  public static final List<Integer> CNSM_M_PKS = newArrayList(0, CNSM_M_FKS1.get(0));
  public static final List<Integer> CNSM_P_FKS = newArrayList(0, 1);
  public static final List<Integer> CNSM_P_PKS = newArrayList(0, 1, 2);
  public static final List<Integer> CNSM_S_FKS = newArrayList(0, 1, 2);

}
