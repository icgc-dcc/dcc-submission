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
package org.icgc.dcc.validation.cascading;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.serializer.WritableSerialization;
import org.apache.hadoop.mapred.JobConf;
import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.cascading.TupleState.TupleError;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Tuple;
import cascading.tuple.hadoop.TupleSerialization;
import cascading.tuple.hadoop.io.HadoopTupleInputStream;
import cascading.tuple.hadoop.io.HadoopTupleOutputStream;
import cascading.tuple.io.TupleInputStream;
import cascading.tuple.io.TupleOutputStream;

import com.google.common.collect.Iterables;
import com.mysema.util.FileUtils;

/**
 * 
 */
public class TupleStateSerializationTest extends CascadingTestCase {
  File file;

  @Test
  public void test_tuple_state_serialization() throws IOException {
    JobConf jobConf = new JobConf();

    // setup job config with proper serialization settings
    jobConf.set("io.serializations",
        TupleStateSerialization.class.getName() + "," + WritableSerialization.class.getName());
    jobConf.set("cascading.serialization.tokens",
        "1000=" + BooleanWritable.class.getName() + ",10001=" + Text.class.getName());

    TupleSerialization tupleSerialization = new TupleSerialization(jobConf);

    // output tuple state serialization
    TupleOutputStream output =
        new HadoopTupleOutputStream(new FileOutputStream(file, false), tupleSerialization.getElementWriter());

    TupleState testState = new TupleState();
    testState.reportError(ValidationErrorCode.OUT_OF_RANGE_ERROR);

    Tuple outputTuple = new Tuple(testState);
    output.writeTuple(outputTuple);

    output.close();

    // input for tuple state serialization
    TupleInputStream input =
        new HadoopTupleInputStream(new FileInputStream(file), tupleSerialization.getElementReader());

    Tuple inputTuple = input.readTuple();

    // check the first item in the tuple is a TupleState object
    assertTrue(inputTuple.getObject(0) instanceof TupleState);

    TupleState resultState = (TupleState) inputTuple.getObject(0);
    // make sure invalid state is serialized
    assertEquals(testState.isInvalid(), resultState.isInvalid());

    TupleError[] testErrors = Iterables.toArray(testState.getErrors(), TupleError.class);
    TupleError[] resultErrors = Iterables.toArray(resultState.getErrors(), TupleError.class);
    // check TupleError list is serialized
    assertEquals(testErrors.length, resultErrors.length);
  }

  @Override
  protected void setUp() throws Exception {
    file = new File("src/test/resources/tupleState.test");
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtils.delete(file);
  }

}
