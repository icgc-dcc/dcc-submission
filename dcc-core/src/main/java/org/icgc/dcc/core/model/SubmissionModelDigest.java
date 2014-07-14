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
package org.icgc.dcc.core.model;

import java.util.List;
import java.util.Map;

import lombok.Value;

import org.icgc.dcc.core.model.FileTypes.FileType;

import com.google.common.base.Optional;

/**
 * A digest of the submission model.
 */
@Value
public class SubmissionModelDigest {

  String dictionaryVersion;
  Map<FileType, FileModelDigest> files;
  Map<FileType, Join> joins;
  Map<FileType, List<String>> pks;
  Map<FileType, List<String>> fks;
  Map<String, String> mapping;
  List<String> extensions;

  @Value
  public static class FileModelDigest {

    Map<String, FieldModelDigest> fields;

    @Value
    public static class FieldModelDigest {

      Class<?> type; // TODO: use enum?
      boolean controlled;
      Optional<Map<String, String>> mapping;

    }
  }

  @Value
  public static class Join {

    FileType type;
    boolean innerJoin; // TODO: enum

  }

}
