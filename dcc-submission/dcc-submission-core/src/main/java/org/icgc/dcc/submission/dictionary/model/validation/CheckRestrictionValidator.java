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
package org.icgc.dcc.submission.dictionary.model.validation;

import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.submission.core.util.Constants.RegexRestriction_NAME;

import java.util.regex.PatternSyntaxException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.icgc.dcc.submission.dictionary.model.Restriction;

import com.mongodb.BasicDBObject;

/**
 * Really just laid the ground here for DCC-904.
 * <p>
 * TODO: use interface instead of Restriction directly
 */
public class CheckRestrictionValidator implements ConstraintValidator<CheckRestriction, Restriction> {

  @Override
  public void initialize(CheckRestriction constraintAnnotation) {
  }

  @Override
  public boolean isValid(Restriction restriction, ConstraintValidatorContext constraintContext) {

    // TODO: address properly instead of nesting (DCC-904)
    if (restriction != null) {
      String type = restriction.getType();
      if (type != null) {
        if (RegexRestriction_NAME.equals(type)) { // TODO: this really should go in an enum
          BasicDBObject config = restriction.getConfig();
          Object object = config.get("pattern");
          if (object instanceof String) {
            String pattern = (String) object;
            try {
              compile(pattern);
            } catch (PatternSyntaxException e) { // must be a pattern that compiles
              return false;
            }
            return true;
          } else { // must be a String if "pattern"
            return false;
          }
        } else { // TODO: other types
          return true;
        }
      } else { // type is null
        return false;
      }
    } else { // no restrictions
      return true;
    }
  }
}