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

import static org.icgc.dcc.submission.http.jersey.BasicHttpAuthenticationFilter.DEFAULT_AUTH_HOST;
import static org.icgc.dcc.submission.http.jersey.BasicHttpAuthenticationFilter.HTTP_AUTH_PREFIX;
import static org.icgc.dcc.submission.http.jersey.BasicHttpAuthenticationFilter.WWW_AUTHENTICATE_REALM;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.subject.Subject;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.icgc.dcc.submission.security.UsernamePasswordAuthenticator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.net.HttpHeaders;

@RunWith(MockitoJUnitRunner.class)
public class BasicHttpAuthenticationFilterTest {

  /**
   * Test data.
   */
  private static final String TEST_USERNAME = "brett";
  private static final char[] TEST_PASSWORD = "brettspasswd".toCharArray();
  private static final String TEST_HOST = DEFAULT_AUTH_HOST;

  /**
   * Class under test.
   */
  @InjectMocks
  private BasicHttpAuthenticationFilter authenticationFilter;

  /**
   * Collaborators.
   */
  @Mock
  private Subject mockSubject;
  @Mock
  private SecurityContext mockSecurityContext;
  @Mock
  private UsernamePasswordAuthenticator mockAuthenticator;
  @Mock
  private Request mockRequest;
  @Mock
  private MultivaluedMap<String, String> mockHeaders;
  @Mock
  private ContainerRequestContext mockRequestContext;
  @Mock
  private ContainerResponseContext mockResponseContext;
  @Mock
  private UriInfo mockUriInfo;

  @Before
  public void setUp() {
    // Create some behaviour
    when(mockRequestContext.getRequest()).thenReturn(mockRequest);
    when(mockRequestContext.getHeaders()).thenReturn(mockHeaders);
    when(mockRequestContext.getSecurityContext()).thenReturn(mockSecurityContext);
    when(mockRequestContext.getUriInfo()).thenReturn(mockUriInfo);
    when(mockRequestContext.getProperty(anyString())).thenReturn(Thread.currentThread().getName());
    when(mockUriInfo.getPath()).thenReturn("/fake/path");

    when(mockAuthenticator.authenticate(TEST_USERNAME, TEST_PASSWORD, TEST_HOST)).thenReturn(mockSubject);
  }

  @After
  public void tearDown() {
    // Cleanup thread locals and thread names
    authenticationFilter.filter(mockRequestContext, mockResponseContext);
  }

  @Test
  public void test_filter_handlesCorrectAuthorizationHeader() throws IOException {
    // (generate using: $ echo -n "brett:brettspasswd" | base64)
    when(mockHeaders.getFirst(HttpHeaders.AUTHORIZATION))
        .thenReturn(String.format("%s YnJldHQ6YnJldHRzcGFzc3dk", HTTP_AUTH_PREFIX));

    executeFilter();

    // Make sure there was a login attempt and capture the token
    // Assert username and password match
    verify(this.mockAuthenticator).authenticate(TEST_USERNAME, TEST_PASSWORD, TEST_HOST);
    verify(mockRequestContext, Mockito.never()).abortWith(any(Response.class));
  }

  @Test
  public void test_filter_handlesIncorrectAuthorizationHeader() throws IOException {
    // (generate using: $ echo -n "brett:NOTbrettspasswd" | base64)
    when(mockHeaders.getFirst(HttpHeaders.AUTHORIZATION))//
        .thenReturn(String.format("%s YnJldHQ6Tk9UYnJldHRzcGFzc3dk", HTTP_AUTH_PREFIX)); // encodes "brett:brettspasswd"
                                                                                         // in base64
    executeFilter();

    verify(this.mockAuthenticator).authenticate(TEST_USERNAME, "NOTbrettspasswd".toCharArray(), TEST_HOST);

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(mockRequestContext).abortWith(response.capture());
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getValue().getStatus());
    assertEquals(String.format("%s realm=\"%s\"", HTTP_AUTH_PREFIX, WWW_AUTHENTICATE_REALM),
        ((OutboundJaxrsResponse) response.getValue())
            .getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  public void test_filter_handlesMissingHeader() throws IOException {
    // This test is testing that the header is absent
    when(mockHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

    executeFilter();

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(mockRequestContext).abortWith(response.capture());
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getValue().getStatus());
    assertEquals(String.format("%s realm=\"%s\"", HTTP_AUTH_PREFIX, WWW_AUTHENTICATE_REALM),
        ((OutboundJaxrsResponse) response.getValue())
            .getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  public void test_filter_handlesMalformedAuthorizationHeader1() throws IOException {
    testMalformedHeader(String.format("%sX YnJldHQ6YnJldHRzcGFzc3dkCg==", HTTP_AUTH_PREFIX));
  }

  @Test
  public void test_filter_handlesMalformedAuthorizationHeader2() throws IOException {
    testMalformedHeader(String.format("%s YnJldHQvYnJldHRzcGFzc3dkCg==", HTTP_AUTH_PREFIX));
  }

  @Test
  public void test_filter_handlesMalformedAuthorizationHeader3() throws IOException {
    testMalformedHeader(HTTP_AUTH_PREFIX);
  }

  /**
   * Common test fixture for a malformed header
   */
  private void testMalformedHeader(String malformed) throws IOException {
    when(mockHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(malformed);

    executeFilter();

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(mockRequestContext).abortWith(response.capture());
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getValue().getStatus());
  }

  // Exercise the code we're testing
  private void executeFilter() throws IOException {
    authenticationFilter.filter(mockRequestContext);
  }

}
