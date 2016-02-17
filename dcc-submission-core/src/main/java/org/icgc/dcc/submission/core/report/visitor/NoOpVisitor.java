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
package org.icgc.dcc.submission.core.report.visitor;

import lombok.NonNull;

import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.ErrorReport;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.core.report.ReportElement;
import org.icgc.dcc.submission.core.report.ReportVisitor;

/**
 * All operations have no side-effects.
 * <p>
 * Useful for lazy subclass-ers or when a {@link ReportElement} interface needs to be satisfied.
 */
public class NoOpVisitor implements ReportVisitor {

  @Override
  public void visit(@NonNull Report report) {
    // No-op
  }

  @Override
  public void visit(@NonNull DataTypeReport dataTypeReport) {
    // No-op
  }

  @Override
  public void visit(@NonNull FileTypeReport fileTypeReport) {
    // No-op
  }

  @Override
  public void visit(@NonNull FileReport fileReport) {
    // No-op
  }

  @Override
  public void visit(@NonNull ErrorReport errorReport) {
    // No-op
  }

}
