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
package org.icgc.dcc.dictionary.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.dictionary.model.FileSchema;

import com.google.common.collect.Sets;

public class DictionaryConsistencyVisitor extends BaseDictionaryVisitor {
  private final List<String> errors = new ArrayList<String>();

  @Override
  public void visit(FileSchema schema) {
    Set<String> fieldNames = Sets.newHashSet(schema.fieldNames());
    for(String uniqueField : schema.getUniqueFields()) {
      if(fieldNames.contains(uniqueField) == false) {
        errors.add("Specified uniqueField does not exist in field list: " + uniqueField);
      }
    }
  }

  public boolean hasErrors() {
    return errors.isEmpty() == false;
  }

  public List<String> getErrors() {
    return errors;
  }
}
