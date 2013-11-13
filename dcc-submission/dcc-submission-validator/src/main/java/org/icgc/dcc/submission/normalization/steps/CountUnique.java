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
package org.icgc.dcc.submission.normalization.steps;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.RESULTS;
import static java.lang.String.format;

import org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter;
import org.icgc.dcc.submission.validation.cascading.CascadingFunctions.Counter;
import org.icgc.dcc.submission.validation.cascading.CascadingFunctions.EmitNothing;

import cascading.pipe.Each;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Unique;
import cascading.tap.Tap;
import cascading.tuple.Fields;

/**
 * Performs a unique count for the given field(s) and in a transparent manner to the flow it originates from (and
 * eventually merges to).
 * <p>
 * This uses a trick whereby we split the flow, do the count, filter out all tuples and merge back in the original flow.
 * This is to circumvent the issue with cascading of having to have sink {@link Tap} for a branch to be run (TODO: find
 * better way?).
 */
public final class CountUnique extends SubAssembly {

  CountUnique(Pipe pipe, String stepShortName, Fields fields, NormalizationCounter counter, long increment) {
    Pipe unique = new Pipe(
        format("%s-%s-pipe", stepShortName, counter),
        pipe);

    unique = new Unique(unique, fields);
    unique = new Each(
        unique,
        ALL,
        new Counter(counter, increment),
        RESULTS);

    // Trick to re-join main branch without consequences (else side branch does not get executed)
    unique = new Each(unique, new EmitNothing());

    setTails(new Merge(
        pipe, // Will effectively remain unaltered
        unique));
  }
}
