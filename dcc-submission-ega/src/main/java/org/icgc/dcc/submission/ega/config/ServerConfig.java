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
package org.icgc.dcc.submission.ega.config;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.concurrent.ConcurrentMap;

import org.icgc.dcc.common.core.mail.Mailer;
import org.icgc.dcc.common.ega.client.EGAAPIClient;
import org.icgc.dcc.common.ega.client.EGAFTPClient;
import org.icgc.dcc.common.ega.dataset.EGADatasetMetaArchiveResolver;
import org.icgc.dcc.common.ega.dataset.EGADatasetMetaReader;
import org.icgc.dcc.common.ega.dump.EGAMetadataDumper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.google.common.cache.CacheBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class ServerConfig {

  @Async
  @EventListener
  public void start(ApplicationReadyEvent event) {
    log.info("**** Started!");
  }

  /**
   * Server wide mail configuration.
   */
  @Configuration
  public static class EGAConfig {

    @Value("${ega.api.userName}")
    String apiUserName;
    @Value("${ega.api.password}")
    String apiPassword;
    @Value("${ega.ftp.userName}")
    String ftpUserName;
    @Value("${ega.ftp.password}")
    String ftpPassword;

    @Bean
    public EGAMetadataDumper dumper() {
      return new EGAMetadataDumper(reader());
    }

    @Bean
    public EGADatasetMetaReader reader() {
      return new EGADatasetMetaReader(api(), archiveResolver());
    }

    @Bean
    public EGAAPIClient api() {
      return new EGAAPIClient(apiUserName, apiPassword).login();
    }

    @Bean
    public EGAFTPClient ftp() {
      return new EGAFTPClient(ftpUserName, ftpPassword);
    }

    @Bean
    public EGADatasetMetaArchiveResolver archiveResolver() {
      return new EGADatasetMetaArchiveResolver(api(), ftp());
    }

  }

  /**
   * Server wide caching configuration.
   */
  @Configuration
  @EnableCaching
  public static class CacheConfig extends CachingConfigurerSupport {

    /**
     * Constants.
     */
    private static final int CACHE_TTL_MINUTES = 60;

    @Override
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager() {

        @Override
        protected Cache createConcurrentMapCache(String name) {
          return new ConcurrentMapCache(name, createStore(), false);
        }

        /**
         * @return Guava cache instance with a suitable TTL.
         */
        private ConcurrentMap<Object, Object> createStore() {
          return CacheBuilder
              .newBuilder()
              .expireAfterWrite(CACHE_TTL_MINUTES, MINUTES)
              .maximumSize(100)
              .build()
              .asMap();
        }

      };
    }

    @Override
    public KeyGenerator keyGenerator() {
      return new SimpleKeyGenerator();
    }

  }

  /**
   * Server wide mail configuration.
   */
  @Configuration
  public static class MailerConfig {

    @Bean
    public Mailer mailer(
        @Value("${mailer.smtp.host}") String host,
        @Value("${mailer.smtp.recipient}") String recipient) {
      return Mailer.builder().host(host).recipient(recipient).build();
    }

  }

}
