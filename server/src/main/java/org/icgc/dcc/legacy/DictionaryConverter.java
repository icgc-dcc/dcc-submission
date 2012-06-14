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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

/**
 * 
 */
public class DictionaryConverter {

	public Dictionary readDictionary(String folder) throws IOException {
		Dictionary dictionary = new Dictionary("1.0");
		File tsvFolder = new File(folder);
		File[] tsvFiles = tsvFolder.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".tsv");
			}

		});

		List<FileSchema> fileSchemas = new ArrayList<FileSchema>();

		for (File tsvFile : tsvFiles) {
			fileSchemas.add(this.readFileSchema(tsvFile));
		}

		dictionary.setFiles(fileSchemas);

		return dictionary;
	}

	private FileSchema readFileSchema(File tsvFile) throws IOException {
		String FileSchemaName = FilenameUtils
				.removeExtension(tsvFile.getName());
		FileSchema fileSchema = new FileSchema(FileSchemaName);

		String fileSchemaText = Files.toString(tsvFile, Charsets.UTF_8);

		Iterable<String> lines = Splitter.on('\n').trimResults()
				.omitEmptyStrings().split(fileSchemaText);

		Iterator<String> lineIterator = lines.iterator();
		// Read TSV header
		if (lineIterator.hasNext()) {
			this.readTSVHeader(lineIterator.next());
		}
		// Read field
		List<Field> fields = new ArrayList<Field>();
		while (lineIterator.hasNext()) {
			Field field = this.readField(lineIterator.next());
			fields.add(field);
		}
		fileSchema.setFields(fields);

		return fileSchema;
	}

	private Field readField(String line) {
		Field field = new Field();
		Iterable<String> values = Splitter.on('\t').trimResults()
				.omitEmptyStrings().split(line);

		String name = values.iterator().next();
		field.setName(name);

		return field;
	}

	private Iterable<String> readTSVHeader(String line) {
		return Splitter.on('\t').trimResults().omitEmptyStrings().split(line);
	}
}
