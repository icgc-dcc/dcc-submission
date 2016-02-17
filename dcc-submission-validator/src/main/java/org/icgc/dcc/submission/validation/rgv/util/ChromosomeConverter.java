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
package org.icgc.dcc.submission.validation.rgv.util;

import static com.google.common.primitives.Ints.tryParse;
import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public final class ChromosomeConverter {

  public static String convert(String chromosomeCode) {
    val value = tryParse(chromosomeCode);

    val alpha = value == null;
    if (alpha) {
      // CodeList value was passed
      return chromosomeCode;
    }

    if (value >= 1 && value <= 22) {
      // CodeList value and terms are the same
      return chromosomeCode;
    }

    if (value == 23) {
      return "X";
    }
    if (value == 24) {
      return "Y";
    }
    if (value == 25) {
      return "MT";
    }

    throw new IllegalStateException("Could not convert term for code '" + chromosomeCode + "'");
  }

}
