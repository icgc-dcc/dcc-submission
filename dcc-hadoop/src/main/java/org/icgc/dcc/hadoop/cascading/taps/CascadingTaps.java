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
package org.icgc.dcc.hadoop.cascading.taps;

import cascading.tap.SinkMode;
import cascading.tap.Tap;
import cascading.tuple.Fields;

/**
 * Taps for cascading.
 */
public interface CascadingTaps {

  Tap<?, ?, ?> getLines(String path);

  Tap<?, ?, ?> getLines(String path, SinkMode sinkMode);

  Tap<?, ?, ?> getNoCompressionTsvNoHeader(String path);

  Tap<?, ?, ?> getNoCompressionTsvWithHeader(String path);

  Tap<?, ?, ?> getNoCompressionTsvWithHeader(String path, Fields fields);

  Tap<?, ?, ?> getDecompressingTsvWithHeader(String path);

  Tap<?, ?, ?> getDecompressingLinesNoHeader(String path, Fields numField);

  Tap<?, ?, ?> getDecompressingLinesNoHeader(String path, Fields numField, Fields lineField);

  /**
   * TODO: compression for local.
   */
  Tap<?, ?, ?> getCompressingJson(String path);

  Tap<?, ?, ?> getNoCompressionNoHeaderNonStrictTsv(String path);

}
