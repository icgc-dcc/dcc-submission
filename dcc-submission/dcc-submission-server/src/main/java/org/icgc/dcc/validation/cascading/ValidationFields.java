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
package org.icgc.dcc.validation.cascading;

import java.beans.ConstructorProperties;

import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;
import static org.icgc.dcc.hadoop.cascading.Fields2.concat;

/**
 * An extension of {@code Fields} that always includes the {@code TupleState} field.
 */
public class ValidationFields extends Fields {

  public static final String STATE_FIELD_NAME = "_state";

  public static final String OFFSET_FIELD_NAME = "offset";

  public static final Fields STATE_FIELD = new Fields(STATE_FIELD_NAME);

  /**
   * Extract the {@code TupleState} field from a {@cude TupleEntry}.
   */
  public static TupleState state(TupleEntry te) {
    return (TupleState) te.getObject(STATE_FIELD_NAME);
  }

  @SuppressWarnings("rawtypes")
  @ConstructorProperties({ "fields" })
  public ValidationFields(Comparable... fields) {
    super(concat(fields, STATE_FIELD_NAME));
  }

}
