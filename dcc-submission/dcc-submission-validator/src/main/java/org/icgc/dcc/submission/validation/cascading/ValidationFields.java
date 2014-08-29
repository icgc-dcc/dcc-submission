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
package org.icgc.dcc.submission.validation.cascading;

import static org.icgc.dcc.hadoop.cascading.Fields2.concat;
import static org.icgc.dcc.hadoop.cascading.Fields2.field;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

/**
 * An extension of {@code Fields} that always includes the {@code TupleState} field.
 */
public class ValidationFields extends Fields {

  // TODO: hide the String version
  public static final String STATE_FIELD_NAME = "_state";
  public static final String OFFSET_FIELD_NAME = "offset";

  public static final Fields STATE_FIELD = field(STATE_FIELD_NAME);
  public static final Fields OFFSET_FIELD = field(OFFSET_FIELD_NAME);

  /**
   * Extract the {@code TupleState} field from a {@cude TupleEntry}.
   */
  public static TupleState state(TupleEntry tupleEntry) {
    return (TupleState) tupleEntry.getObject(STATE_FIELD_NAME);
  }

  @SuppressWarnings("rawtypes")
  public ValidationFields(Comparable... fields) {
    super(concat(fields, STATE_FIELD_NAME));
  }

}
