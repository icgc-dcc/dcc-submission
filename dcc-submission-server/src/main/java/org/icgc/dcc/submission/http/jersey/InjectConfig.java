/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.http.jersey;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Used to register an {@code InjectionResolver} that will resolve Guice's {@code Inject} annotation.
 */
@Configuration
public class InjectConfig {

  @Bean
  public InjectionResolver<?> injectionResolver(ApplicationContext context) {
    return new SpringInjectionResolver(context);
  }

  @Singleton
  @RequiredArgsConstructor
  public static class SpringInjectionResolver implements InjectionResolver<Autowired> {

    @NonNull
    private final ApplicationContext context;

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
      if (type instanceof Class) {
        return context.getBean((Class<?>) type);
      }
      throw new IllegalStateException(String.format("don't know how to inject type %s (%s)", type,
          type == null ? null : type.getClass()));
    }
  }

}
