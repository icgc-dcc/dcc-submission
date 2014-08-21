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

import static org.icgc.dcc.hadoop.cascading.taps.GenericSchemes.TSV_DELIMITER;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.RecordReader;

import cascading.scheme.Scheme;
import cascading.tuple.Fields;

/**
 * Prefer {@link GenericSchemes} or {@link LocalSchemes}/{@link DistributedSchemes}.
 */
public class LegacySchemes {

  /**
   * Allows empty lines and skip header.
   */
  public static Scheme<Properties, InputStream, OutputStream, ?, ?> newLocalLooseTsvScheme() {
    return newLocalLooseTsvScheme(getTextDelimitedSourceFields(), skipHeader(), TSV_DELIMITER);
  }

  /**
   * Built by looking at the 3 parameters argument from cascading's source code.
   */
  private static Scheme<Properties, InputStream, OutputStream, ?, ?> newLocalLooseTsvScheme(Fields fields,
      boolean hasHeader, String delimiter) {
    return new cascading.scheme.local.TextDelimited(fields, hasHeader, hasHeader, delimiter, looseMode(), null, null,
        true);
  }

  /**
   * Allows empty lines and skip header.
   */
  @SuppressWarnings("rawtypes")
  // TODO: address warning
  public static Scheme<JobConf, RecordReader, OutputCollector, ?, ?> newHadoopLooseTsvScheme() {
    return newHadoopLooseTsvScheme(getTextDelimitedSourceFields(), skipHeader(), TSV_DELIMITER);
  }

  /**
   * Built by looking at the 3 parameters argument from cascading's source code.
   */
  @SuppressWarnings("rawtypes")
  // TODO: address warning
  private static Scheme<JobConf, RecordReader, OutputCollector, ?, ?> newHadoopLooseTsvScheme(Fields fields,
      boolean hasHeader, String delimiter) {
    return new cascading.scheme.hadoop.TextDelimited(fields, null, hasHeader, hasHeader, delimiter, looseMode(), null,
        null, true);
  }

  /**
   * MUST explicitly set source fields to UNKNOWN for them to be set based on header... It otherwise defaults them to
   * ALL, which according to TextDelimited.retrieveSourceFields() will not use the header to set the source fields and
   * is in contradiction with the TextDelimited class documentation (at least as of version 2.1.3).
   */
  private static Fields getTextDelimitedSourceFields() {
    return Fields.UNKNOWN;
  }

  /**
   * Will allow skipping the first line.
   */
  private static boolean skipHeader() {
    return true;
  }

  /**
   * Will allow empty lines to be read without erroring out. "loose" as opposed to "strict".
   */
  private static boolean looseMode() {
    return false;
  }

}
