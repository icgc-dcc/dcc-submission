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
package org.icgc.dcc.hadoop.cascading;

import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.Identifiable.Identifiables.getId;
import static org.icgc.dcc.core.util.Joiners.DASH;
import static org.icgc.dcc.core.util.Strings2.removeTrailingS;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.icgc.dcc.core.model.Identifiable;
import org.icgc.dcc.core.util.Named;

import cascading.cascade.Cascade;
import cascading.flow.hadoop.util.HadoopUtil;

/**
 * Utils methods for {@link Cascade}.
 */
@NoArgsConstructor(access = PRIVATE)
public class Cascades implements Named {

  private static final Cascades INTERNAL = new Cascades();
  private static final String CLASS_NAME = removeTrailingS(Cascades.class.getSimpleName());

  @Override
  public String getName() {
    return CLASS_NAME;
  }

  public static String getName(Identifiable... identifiables) {
    return getName(transform(asList(identifiables), getId()));
  }

  public static String getName(Object... qualifiers) {
    return DASH.join(INTERNAL.getName(), DASH.join(qualifiers));
  }

  public static String getName(Class<?> clazz, Object... qualifiers) {
    return getName(clazz.getSimpleName(), getName(qualifiers));
  }

  @SneakyThrows
  public static String cascadingSerialize(Object object) {
    return HadoopUtil.serializeBase64(object, new JobConf(new Configuration()));
  }

}
