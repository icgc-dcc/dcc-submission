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
package org.icgc.dcc.submission.dictionary.model;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.BasicDBObject;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * TODO: look at ReleaseValidationTest and re-use Bob's super class (DCC-904)
 */
public class RestrictionTest {

  private static Validator validator;

  @BeforeClass
  public static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void test_validRestriction() {
    Restriction restriction = new Restriction();
    restriction.setType("regex");
    BasicDBObject config = new BasicDBObject();
    config.put("pattern", "^$");
    restriction.setConfig(config);

    Set<ConstraintViolation<Restriction>> constraintViolations = validator.validate(restriction);
    assertTrue(constraintViolations.isEmpty());
  }

  @Test
  public void test_missingType() {
    Restriction restriction = new Restriction();

    BasicDBObject config = new BasicDBObject();
    config.put("pattern", "^$");
    restriction.setConfig(config);

    Set<ConstraintViolation<Restriction>> constraintViolations = validator.validate(restriction);
    assertTrue(!constraintViolations.isEmpty());
  }

  @Test
  public void test_invalidPattern() {
    Restriction restriction = new Restriction();
    restriction.setType("regex");
    BasicDBObject config = new BasicDBObject();
    config.put("pattern", "[");
    restriction.setConfig(config);

    Set<ConstraintViolation<Restriction>> constraintViolations = validator.validate(restriction);
    assertTrue(constraintViolations.size() == 1);
    assertEquals("Restriction config must consistent with restriction type", constraintViolations.iterator().next()
        .getMessage());
  }

}
