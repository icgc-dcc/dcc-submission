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
package org.icgc.dcc.submission.validation.first.util;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.core.RowChecker;
import org.icgc.dcc.submission.validation.first.file.RowNoOpChecker;
import org.icgc.dcc.submission.validation.first.file.RowCharsetChecker;
import org.icgc.dcc.submission.validation.first.file.RowColumnChecker;
import org.icgc.dcc.submission.validation.first.file.RowCountChecker;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;

/**
 * Made non-final for power mock.
 */
@NoArgsConstructor(access = PRIVATE)
public class RowCheckers {

  public static RowChecker getDefaultRowChecker(ValidationContext validationContext, FPVFileSystem fs) {
    val chain =
        new RowColumnChecker(
            new RowCharsetChecker(
                new RowCountChecker(
                    new RowNoOpChecker(validationContext, fs) // Leaf checker
                )
            )
        );

    return chain;
  }
}