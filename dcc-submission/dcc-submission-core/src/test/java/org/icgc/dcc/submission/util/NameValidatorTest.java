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
package org.icgc.dcc.submission.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.icgc.dcc.submission.core.util.NameValidator;
import org.junit.Test;

/**
 * 
 */
public class NameValidatorTest {

  @Test
  public void test_name_validation() {

    assertTrue(NameValidator.validateEntityName("abc123"));

    assertTrue(NameValidator.validateEntityName("abc_123_p9k"));

    assertFalse(NameValidator.validateEntityName("!@#123"));

    assertTrue(NameValidator.validateEntityName("ABCabc123"));

    assertFalse(NameValidator.validateEntityName("a2"));

    assertFalse(NameValidator.validateEntityName("a␡b")); // non-printable

    assertTrue(NameValidator.validateProjectId("809_0"));

    assertTrue(NameValidator.validateProjectId("809.0"));
  }
}
