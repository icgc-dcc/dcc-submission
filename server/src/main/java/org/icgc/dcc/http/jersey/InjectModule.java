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
package org.icgc.dcc.http.jersey;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Used to register an {@code InjectionResolver} that will resolve Guice's {@code Inject} annotation.
 */
public class InjectModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(GuiceInjectResolver.class).asEagerSingleton();
    bind(GuiceModule.class).asEagerSingleton();
  }

  @Singleton
  public static class GuiceInjectResolver implements InjectionResolver<Inject> {

    private final Injector injector;

    @Inject
    public GuiceInjectResolver(Injector injector) {
      this.injector = injector;
    }

    @Override
    public boolean isConstructorParameterIndicator() {
      return true;
    }

    @Override
    public boolean isMethodParameterIndicator() {
      return false;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
      Type type = injectee.getRequiredType();
      if(type instanceof Class) {
        return injector.getInstance((Class<?>) type);
      }
      throw new IllegalStateException(String.format("don't know how to inject type %s (%s)", type,
          type == null ? null : type.getClass()));
    }
  }

  public static final class GuiceModule extends AbstractBinder {

    private final GuiceInjectResolver guiceResolver;

    @Inject
    public GuiceModule(GuiceInjectResolver guiceResolver, ResourceConfig config) {
      this.guiceResolver = guiceResolver;
      config.addBinders(this);
    }

    @Override
    protected void configure() {
      bind(guiceResolver).to(InjectionResolver.class);
    }
  }

}
