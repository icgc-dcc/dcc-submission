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
package org.icgc.dcc.hadoop.util;

import static org.icgc.dcc.hadoop.util.HadoopConstants.BZIP2_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.DEFAULT_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.GZIP_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.LZOP_CODEC_PROPERTY_VALUE;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Describes the different types of compression in hadoop, along with their corresponding codecs.
 */
@RequiredArgsConstructor
public enum HadoopCompression {
  NONE(DEFAULT_CODEC_PROPERTY_VALUE),
  GZIP(GZIP_CODEC_PROPERTY_VALUE),
  BZIP2(BZIP2_CODEC_PROPERTY_VALUE),
  LZO(LZOP_CODEC_PROPERTY_VALUE);

  @Getter
  private final String codec;

  public boolean isEnabled() {
    return this != NONE;
  }

}