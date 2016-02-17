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
package org.icgc.dcc.submission.core.report;

/**
 * A classical visitor operating on composite {@link ReportElement} nodes only.
 * <p>
 * Visitor traversal is in the standard post-order walk pattern:
 * 
 * <pre>
 * 
 *  .- report ------------------->               15 
 *  |                                        /        \
 *  +- dataTypeReport ----------->         7           14   
 *   |                                   /   \        /   \
 *   +- fileTypeReport----------->      3     6     10     13  
 *    |                                / \   / \    / \   / \ 
 *    + fileReport -------------->    1   2 4   5  8   9 11  12
 * </pre>
 * 
 */
public interface ReportVisitor {

  /**/void visit(Report report);

  /*  */void visit(DataTypeReport dataTypeReport);

  /*     */void visit(FileTypeReport fileTypeReport);

  /*       */void visit(FileReport fileReport);

  /*          */void visit(ErrorReport errorReport);

}
