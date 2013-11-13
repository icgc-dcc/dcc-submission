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
package org.icgc.dcc.hadoop.cascading;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

import cascading.tuple.Tuple;

public class Tuples2Test {

  @Test
  public void test_sameContent() {
    assertFalse(Tuples2.sameContent(
        null,
        null));
    assertFalse(Tuples2.sameContent(
        new Tuple("a", "b"),
        new Tuple("a", "c")));
    assertFalse(Tuples2.sameContent(
        new Tuple("a", "b", null),
        new Tuple("a", "c", null)));

    assertTrue(Tuples2.sameContent(
        new Tuple(),
        new Tuple()));
    assertTrue(Tuples2.sameContent(
        new Tuple((String) null),
        new Tuple((String) null)));
    assertTrue(Tuples2.sameContent(
        new Tuple("a", "b"),
        new Tuple("a", "b")));
    assertTrue(Tuples2.sameContent(
        new Tuple("a", "b", null),
        new Tuple("a", "b", null)));
  }
}
