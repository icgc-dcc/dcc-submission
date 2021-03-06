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
package org.icgc.dcc.submission.server.spring;

import static lombok.AccessLevel.PRIVATE;

import java.net.UnknownHostException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import com.mongodb.MongoException;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
class SpringBootFailureAnalyzers {

  static class UnknownHostFailureAnalyzer extends AbstractFailureAnalyzer<UnknownHostException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, UnknownHostException cause) {
      return new FailureAnalysis(
          "Portal failed to start due to a bad host configuration: \"" + cause.getMessage() + "\"",
          "Ensure the host is reachable on the current network and reconfigure application propertie(s)",
          cause);
    }

  }

  static class MongoFailureAnalyzer extends AbstractFailureAnalyzer<MongoException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MongoException cause) {
      return new FailureAnalysis(
          "Server failed to start due to Mongo error: \"" + cause.getMessage() + "\"",
          "Ensure the database is running and the configuration is set correctly",
          cause);
    }

  }

}
