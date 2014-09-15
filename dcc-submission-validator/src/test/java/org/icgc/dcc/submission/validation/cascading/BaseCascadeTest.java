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
package org.icgc.dcc.submission.validation.cascading;

import static cascading.flow.FlowDef.flowDef;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.flow.FlowProcess;
import cascading.flow.local.LocalFlowConnector;
import cascading.operation.FunctionCall;
import cascading.operation.NoOp;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.scheme.local.TextLine;
import cascading.tap.Tap;
import cascading.tap.local.StdInTap;
import cascading.tap.local.StdOutTap;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleEntryCollector;
import cascading.tuple.TupleEntryIterator;

@SuppressWarnings("rawtypes")
@Slf4j
public abstract class BaseCascadeTest {

  protected Future<Throwable> executeCascade(ExecutorService executor, final Runnable testAction) {
    log.info("[Main] Submitting action...");
    Future<Throwable> future = executor.submit(new Callable<Throwable>() {

      @Override
      public Throwable call() throws Exception {
        log.info("[Executor] Creating cascade...");
        val cascade = createCascade(testAction);
        try {
          log.info("[Executor] Awaiting cascade...");
          cascade.complete();
          log.info("[Executor] Cascade complete");
        } catch (Throwable t) {
          log.error("[Executor] Exception:", t);

          log.error("[Executor] Stopping cascade");
          cascade.stop();
          log.error("[Executor] Cascade stopped");

          return t;
        }

        return null;
      }

    });

    log.info("[Main] Returning future");
    return future;
  }

  private static Cascade createCascade(Runnable testAction) {
    return new CascadeConnector().connect(createFlow(testAction));
  }

  private static Flow<?> createFlow(Runnable testAction) {
    // Local
    return new LocalFlowConnector().connect(createFlowDef(testAction));
  }

  private static FlowDef createFlowDef(Runnable testAction) {
    // flowName is arbitrary
    val flowName = "testFlow";

    // Data flow: source -> tail -> sink
    return flowDef()
        .setName(flowName)
        .addSource(flowName, createSource())
        .addTail(createPipe(flowName, testAction))
        .addSink(flowName, createSink());
  }

  private static Pipe createPipe(String name, Runnable testAction) {
    // We want control of a single flow operation on a singleton tuple
    return new Each(name, new TestOperation(testAction));
  }

  private static Tap createSource() {
    // We want to create a single tuple
    return new TestInputTap();
  }

  private static Tap createSink() {
    // We want to ignore the output
    return new TestOutputTap();
  }

  private static final class TestInputTap extends StdInTap {

    private TestInputTap() {
      // Arbitrary scheme
      super(new TextLine());
    }

    @Override
    public TupleEntryIterator openForRead(FlowProcess<Properties> flowProcess, InputStream inputStream)
        throws IOException {
      // We want control of tuple iteration
      return new TestTupleEntryIterator();
    }

  }

  private static final class TestTupleEntryIterator extends TupleEntryIterator {

    TupleEntry entry;

    public TestTupleEntryIterator() {
      // Arbitrary
      super(Fields.ALL);
    }

    @Override
    public boolean hasNext() {
      // Once only
      return entry == null;
    }

    @Override
    public TupleEntry next() {
      // Once only
      return entry = new TupleEntry();
    }

    @Override
    public void remove() {
      // No-op
    }

    @Override
    public void close() throws IOException {
      // No-op
    }

  }

  @RequiredArgsConstructor
  private static class TestOperation extends NoOp {

    // Run in the middle of a flow
    final Runnable testAction;

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      log.info("[Operation] Processing...");
      testAction.run();
      log.info("[Operation] processing");
    }

  }

  private static class TestOutputTap extends StdOutTap {

    private TestOutputTap() {
      // Arbitrary scheme
      super(new TextLine());
    }

    @Override
    public TupleEntryCollector openForWrite(FlowProcess<Properties> flowProcess, OutputStream output) {
      return new TupleEntryCollector() {

        @Override
        protected void collect(TupleEntry tupleEntry) throws IOException {
          // No-op
        }

      };
    }

  }

}
