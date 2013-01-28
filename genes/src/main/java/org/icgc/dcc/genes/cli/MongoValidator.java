/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.genes.cli;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

public class MongoValidator implements IValueValidator<MongoURI> {

  public void validate(String name, MongoURI mongoUri) throws ParameterException {
    try {
      Mongo mongo = new Mongo(mongoUri);
      try {
        Socket socket = mongo.getMongoOptions().socketFactory.createSocket();
        socket.connect(mongo.getAddress().getSocketAddress());
        socket.close();
      } catch(IOException ex) {
        throw new ParameterException("Invalid option: " + name + ": " + mongoUri + " is not accessible");
      } finally {
        mongo.close();
      }
    } catch(UnknownHostException e) {
      throw new ParameterException("Invalid option: " + name + ": " + mongoUri + " is not accessible");
    }
  }

}