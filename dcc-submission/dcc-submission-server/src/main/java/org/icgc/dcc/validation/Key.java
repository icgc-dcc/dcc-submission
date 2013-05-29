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

import static org.icgc.dcc.validation.CascadingStrategy.SEPARATOR;

import java.io.Serializable;
import java.util.Arrays;

import org.icgc.dcc.dictionary.model.FileSchema;

import com.google.common.base.Joiner;

/**
 * Holds a reference to trimmed content. Used to plan outputs from the internal flow and inputs for the external flow.
 */
public class Key implements Serializable {

  private static final char FIELD_SEPARATOR = '-';

  private final FileSchema schema;

  private final String[] fields;

  public Key(FileSchema schema, String... fields) {
    this.schema = schema;
    this.fields = fields;
  }

  public FileSchema getSchema() {
    return schema;
  }

  public String[] getFields() {
    return fields;
  }

  public String getName() {
    return schema.getName() + SEPARATOR + Joiner.on(FIELD_SEPARATOR).join(fields);
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj instanceof Key == false) {
      return super.equals(obj);
    }
    Key rhs = (Key) obj;
    return this.schema.equals(rhs.schema) && Arrays.equals(fields, rhs.fields);
  }

  @Override
  public int hashCode() {
    int hashCode = schema.hashCode();
    hashCode += 37 * Arrays.hashCode(fields);
    return hashCode;
  }
}
