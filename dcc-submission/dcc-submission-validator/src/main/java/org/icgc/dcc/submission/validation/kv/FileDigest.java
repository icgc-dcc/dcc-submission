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
package org.icgc.dcc.submission.validation.kv;

import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

import org.icgc.dcc.submission.validation.kv.Keys.Tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Value
public class FileDigest { // TODO: use optionals?

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // TODO: encapsulate in other object?
  private final SubmissionType submissionType;
  private final FileType fileType;
  private final String path; // TODO: optional
  private final boolean placeholder;

  // TODO: change to arrays?
  private final Set<Keys> pks;
  private final Set<Keys> fks;
  private final List<Tuple> tuples;

  public static FileDigest getEmptyInstance(SubmissionType submissionType, FileType fileType) { // TODO: null for
                                                                                                // donor fk?
    val placeholder = true;
    return new FileDigest(
        submissionType, fileType, null, placeholder,
        Sets.<Keys> newTreeSet(),
        Sets.<Keys> newTreeSet(),
        Lists.<Tuple> newArrayList());
  }

  @Override
  public String toString() {
    return toJsonSummaryString();
  }

  @SneakyThrows
  public String toJsonSummaryString() {
    return "\n" + MAPPER
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(this); // TODO: show sample only (first and last 10 for instance) + excluding nulls
  }

  public boolean pksContains(@NonNull Keys keys) {// TODO: consider removing such time consuming checks?
    return pks.contains(keys);
  }
}