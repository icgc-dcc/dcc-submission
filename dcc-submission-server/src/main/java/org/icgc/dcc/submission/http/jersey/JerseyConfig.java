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

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.server.ResourceConfig;
import org.icgc.dcc.submission.config.AbstractConfig;
import org.icgc.dcc.submission.web.mapper.DuplicateNameExceptionMapper;
import org.icgc.dcc.submission.web.mapper.InvalidNameExceptionMapper;
import org.icgc.dcc.submission.web.mapper.ReleaseExceptionMapper;
import org.icgc.dcc.submission.web.mapper.UnhandledExceptionMapper;
import org.icgc.dcc.submission.web.mapper.UnsatisfiedPreconditionExceptionMapper;
import org.icgc.dcc.submission.web.provider.ValidatingJacksonJsonProvider;
import org.icgc.dcc.submission.web.resource.CodeListResource;
import org.icgc.dcc.submission.web.resource.DictionaryResource;
import org.icgc.dcc.submission.web.resource.NextReleaseResource;
import org.icgc.dcc.submission.web.resource.ProjectResource;
import org.icgc.dcc.submission.web.resource.ReleaseResource;
import org.icgc.dcc.submission.web.resource.SeedResource;
import org.icgc.dcc.submission.web.resource.SystemResource;
import org.icgc.dcc.submission.web.resource.UserResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.val;

@Configuration
public class JerseyConfig extends AbstractConfig {

  @Bean
  public ResourceConfig resourceConfig() {
    val config = new ResourceConfig();

    // Providers
    config.register(ValidatingJacksonJsonProvider.class, MessageBodyReader.class, MessageBodyWriter.class);

    // Resources
    config.register(SystemResource.class);
    config.register(ProjectResource.class);
    config.register(ReleaseResource.class);
    config.register(NextReleaseResource.class);
    config.register(DictionaryResource.class);
    config.register(CodeListResource.class);
    config.register(UserResource.class);
    config.register(SeedResource.class); // TODO be sure to remove this from production environment (see DCC-819)

    // Filters
    config.register(VersionFilter.class);
    config.register(CorsFilter.class);
    config.register(BasicHttpAuthenticationFilter.class);

    // Exception mappers
    config.register(UnsatisfiedPreconditionExceptionMapper.class);
    config.register(ReleaseExceptionMapper.class);
    config.register(InvalidNameExceptionMapper.class);
    config.register(DuplicateNameExceptionMapper.class);
    config.register(UnhandledExceptionMapper.class);

    return config;
  }

}
