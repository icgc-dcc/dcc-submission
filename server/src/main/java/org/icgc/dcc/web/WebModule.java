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
package org.icgc.dcc.web;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.server.ResourceConfig;
import org.icgc.dcc.http.jersey.BasicHttpAuthenticationRequestFilter;
import org.icgc.dcc.web.mapper.DuplicateNameExceptionMapper;
import org.icgc.dcc.web.mapper.InvalidNameExceptionMapper;
import org.icgc.dcc.web.mapper.ReleaseExceptionMapper;
import org.icgc.dcc.web.mapper.UnsatisfiedPreconditionExceptionMapper;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class WebModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RootResources.class).asEagerSingleton();
    bind(Validator.class).toInstance(Validation.buildDefaultValidatorFactory().getValidator());
  }

  /**
   * Used to register resources in {@code Jersey}. This is required because {@code Jersey} cannot use Guice to discover
   * resources.
   */
  public static class RootResources {
    @SuppressWarnings("unchecked")
    @Inject
    public RootResources(ResourceConfig config) {
      config.register(ValidatingJacksonJsonProvider.class, MessageBodyReader.class, MessageBodyWriter.class);

      config.addClasses(AdminResource.class);
      config.addClasses(ProjectResource.class);
      config.addClasses(ReleaseResource.class);
      config.addClasses(NextReleaseResource.class);
      config.addClasses(DictionaryResource.class);
      config.addClasses(CodeListResource.class);
      config.addClasses(BasicHttpAuthenticationRequestFilter.class);
      config.addClasses(UnsatisfiedPreconditionExceptionMapper.class);
      config.addClasses(ReleaseExceptionMapper.class);
      config.addClasses(InvalidNameExceptionMapper.class);
      config.addClasses(DuplicateNameExceptionMapper.class);
      config.addClasses(UserResource.class);
      config.addClasses(SeedResource.class); // TODO be sure to remove this from production environment (see DCC-819)
    }
  }

}
