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
package org.icgc.dcc.submission.service;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.submission.service.AbstractService.MAX_ATTEMPTS;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.concurrent.Callable;

import lombok.val;

import org.icgc.dcc.submission.core.model.DccConcurrencyException;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.matchers.Contains;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class AbstractServiceTest {

  /**
   * Class under test.
   */
  @InjectMocks
  AbstractService service;

  /**
   * Dependencies.
   */
  @Mock
  MailService mailService;

  @Test
  public void test_withRetry_pass_first() {
    val description = "@@@test@@@";
    val expected = 10;
    Optional<Integer> actual = service.withRetry(description, new Callable<Integer>() {

      @Override
      public Integer call() throws Exception {
        return expected;
      }

    });

    assertThat(expected).isEqualTo(actual.get());
    verifyZeroInteractions(mailService);
  }

  @Test
  public void test_withRetry_pass_half() {
    val description = "@@@test@@@";
    val expected = 10;
    Optional<Integer> actual = service.withRetry(description, new Callable<Integer>() {

      int i = 1;

      @Override
      public Integer call() throws Exception {
        if (i == MAX_ATTEMPTS / 2) {
          return expected;
        } else {
          i++;
          throw optimisticLockException();
        }
      }

    });

    assertThat(expected).isEqualTo(actual.get());
    verifyZeroInteractions(mailService);
  }

  @Test(expected = DccConcurrencyException.class)
  public void test_withRetry_fail_lock() {
    val description = "@@@test@@@";
    try {
      service.withRetry(description, new Callable<Integer>() {

        @Override
        public Integer call() throws Exception {
          throw optimisticLockException();
        }

      });
    } finally {
      verify(mailService).sendSupportProblem(argThat(new Contains(description)), argThat(new Contains(description)));
    }
  }

  @Test(expected = NullPointerException.class)
  public void test_withRetry_fail_unknown() {
    val description = "@@@test@@@";
    service.withRetry(description, new Callable<Integer>() {

      @Override
      public Integer call() throws Exception {
        throw new NullPointerException();
      }

    });
  }

  private DccModelOptimisticLockException optimisticLockException() {
    return new DccModelOptimisticLockException("@@@exception@@@");
  }

}
