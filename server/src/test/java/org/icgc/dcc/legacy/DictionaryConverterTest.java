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
package org.icgc.dcc.legacy;

import java.io.IOException;

import org.junit.Test;

/**
 * 
 */
public class DictionaryConverterTest {

	@Test
	public void test() throws IOException {
		DictionaryConverter dc = new DictionaryConverter();
		dc.readDictionary("src/test/resources/source/");
		dc.saveToJSON("src/test/resources/dictionary.json");

		/*
		 * File testFile = new File("src/test/resources/dictionary.json"); File
		 * refFile = new File("src/main/resources/dictionary.json");
		 * 
		 * ObjectMapper mapper = new ObjectMapper(); JsonNode testTree =
		 * mapper.readTree(FileUtils .readFileToString(testFile)); JsonNode
		 * refTree = mapper.readTree(FileUtils.readFileToString(refFile));
		 * 
		 * assertTrue(refTree.equals(testTree));
		 */
	}

}
