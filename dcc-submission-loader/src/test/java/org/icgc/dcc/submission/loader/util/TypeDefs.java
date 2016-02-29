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
package org.icgc.dcc.submission.loader.util;

import java.util.Collections;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.icgc.dcc.submission.loader.model.TypeDef;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeDefs {

  public static final Map<Integer, String> TYPE_ORDER = ImmutableMap.<Integer, String> builder()
      .put(0, "donor")
      .put(1, "specimen")
      .put(2, "biomarker")
      .put(3, "sample")
      .build();

  public static TypeDef donor() {
    return new TypeDef("donor", Collections.emptySet());
  }

  public static TypeDef specimen() {
    return new TypeDef("specimen", ImmutableSet.of("donor"));
  }

  public static TypeDef biomarker() {
    return new TypeDef("biomarker", ImmutableSet.of("donor", "specimen"));
  }

  public static TypeDef sample() {
    return new TypeDef("sample", ImmutableSet.of("specimen"));
  }

}
