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

import java.util.HashMap;
import java.util.Map;

import org.icgc.dcc.model.dictionary.ValueType;

/**
 * 
 */
public class ValueTypeConverter {
	private final Map<String, ValueType> valueTypeMap = new HashMap<String, ValueType>();

	public ValueTypeConverter() {
		// initialize map
		this.valueTypeMap.put("INTEGER", ValueType.INTEGER);
		this.valueTypeMap.put("FLOAT(3,2)", ValueType.DECIMAL);
		this.valueTypeMap.put("FLOAT(5,2)", ValueType.DECIMAL);
		this.valueTypeMap.put("TEXT", ValueType.TEXT);
		this.valueTypeMap.put("VARCHAR(128)", ValueType.TEXT);
		this.valueTypeMap.put("VARCHAR(1024)", ValueType.TEXT);
		this.valueTypeMap.put("VARCHAR(16)", ValueType.TEXT);
		this.valueTypeMap.put("VARCHAR(256)", ValueType.TEXT);
		this.valueTypeMap.put("VARCHAR(32)", ValueType.TEXT);
		this.valueTypeMap.put("VARCHAR(512)", ValueType.TEXT);
		this.valueTypeMap.put("VARCHAR(64)", ValueType.TEXT);
	}

	public Map<String, ValueType> getMap() {
		return this.valueTypeMap;
	}
}
