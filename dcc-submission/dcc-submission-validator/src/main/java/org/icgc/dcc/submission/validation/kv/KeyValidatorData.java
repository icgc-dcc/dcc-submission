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

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.icgc.dcc.submission.validation.kv.Keys.Tuple;

/**
 * 
 */
public class KeyValidatorData {

  // TODO: further split
  FileDigest donorOriginalDigest;
  FileDigest specimenOriginalDigest;
  FileDigest sampleOriginalDigest;
  FileDigest ssmMOriginalDigest;
  FileDigest ssmPOriginalDigest;
  FileDigest cnsmMOriginalDigest;
  FileDigest cnsmPOriginalDigest;
  FileDigest cnsmSOriginalDigest;

  FileDigest donorNewDigest;
  FileDigest specimenNewDigest;
  FileDigest sampleNewDigest;
  FileDigest ssmMNewDigest;
  FileDigest ssmPNewDigest;
  FileDigest cnsmMNewDigest;
  FileDigest cnsmPNewDigest;
  FileDigest cnsmSNewDigest;

  List<Tuple> donorUniqueOriginalErrors = newArrayList();
  List<Tuple> specimenUniqueOriginalErrors = newArrayList();
  List<Tuple> sampleUniqueOriginalErrors = newArrayList();
  List<Tuple> ssmMUniqueOriginalErrors = newArrayList();
  // List<Tuple> ssmPUniqueOriginalErrors = newArrayList();
  List<Tuple> cnsmMUniqueOriginalErrors = newArrayList();
  List<Tuple> cnsmPUniqueOriginalErrors = newArrayList();
  // List<Tuple> cnsmSUniqueOriginalErrors = newArrayList();

  List<Tuple> donorUniqueNewErrors = newArrayList();
  List<Tuple> specimenUniqueNewErrors = newArrayList();
  List<Tuple> sampleUniqueNewErrors = newArrayList();
  List<Tuple> ssmMUniqueNewErrors = newArrayList();
  // List<Tuple> ssmPUniqueNewErrors = newArrayList();
  List<Tuple> cnsmMUniqueNewErrors = newArrayList();
  List<Tuple> cnsmPUniqueNewErrors = newArrayList();
  // List<Tuple> cnsmSUniqueNewErrors = newArrayList();

  List<Tuple> donorRelationErrors = newArrayList();
  List<Tuple> specimenRelationErrors = newArrayList();
  List<Tuple> sampleRelationErrors = newArrayList();
  List<Tuple> ssmMRelationErrors = newArrayList();
  List<Tuple> ssmPRelationErrors = newArrayList();
  List<Tuple> cnsmMRelationErrors = newArrayList();
  List<Tuple> cnsmPRelationErrors = newArrayList();
  List<Tuple> cnsmSRelationErrors = newArrayList();

  List<Keys> donorSurjectivityErrors = newArrayList();
  List<Keys> specimenSurjectivityErrors = newArrayList();
  List<Keys> sampleSurjectivityErrors = newArrayList();
  List<Keys> ssmMSurjectivityErrors = newArrayList();
  List<Keys> cnsmMSurjectivityErrors = newArrayList();
}
