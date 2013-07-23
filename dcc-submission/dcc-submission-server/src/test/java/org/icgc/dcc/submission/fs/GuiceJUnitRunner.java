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
package org.icgc.dcc.submission.fs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.icgc.dcc.submission.config.ConfigModule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.typesafe.config.ConfigFactory;

/**
 * adapted (our ConfigModule takes a Config parameter) from http://fabiostrozzi.eu/2011/03/27/junit-tests-easy-guice/
 * 
 * TODO: move elsewhere if acceptable
 */
public class GuiceJUnitRunner extends BlockJUnit4ClassRunner {
  private final Injector injector;

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  public @interface GuiceModules {
    Class<?>[] value();
  }

  @Override
  public Object createTest() throws Exception {
    Object obj = super.createTest();
    injector.injectMembers(obj);
    return obj;
  }

  public GuiceJUnitRunner(Class<?> klass) throws InitializationError {
    super(klass);
    Class<?>[] classes = getModulesFor(klass);
    injector = createInjectorFor(classes);
  }

  private Injector createInjectorFor(Class<?>[] classes) throws InitializationError {
    Module[] modules = new Module[classes.length];
    for(int i = 0; i < classes.length; i++) {
      try {
        Class<?> klass = classes[i];
        if(klass == ConfigModule.class) {
          modules[i] = new ConfigModule(ConfigFactory.load());// TODO: better way?
        } else {
          modules[i] = (Module) klass.newInstance();
        }
      } catch(InstantiationException e) {
        throw new InitializationError(e);
      } catch(IllegalAccessException e) {
        throw new InitializationError(e);
      }
    }
    return Guice.createInjector(modules);
  }

  private Class<?>[] getModulesFor(Class<?> klass) throws InitializationError {
    GuiceModules annotation = klass.getAnnotation(GuiceModules.class);
    if(annotation == null) throw new InitializationError("Missing @GuiceModules annotation for unit test '"
        + klass.getName() + "'");
    return annotation.value();
  }
}
