/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.pcawg.external;

import static org.icgc.dcc.common.core.util.Jackson.DEFAULT;

import java.net.URL;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * See https://wiki.nci.nih.gov/display/TCGA/TCGA+Barcode+to+UUID+Web+Service+User%27s+Guide
 */
@RequiredArgsConstructor
public class TCGAClient {

  /**
   * Constants.
   */
  private static final String TCGA_BASE_URL = "https://tcga-data.nci.nih.gov";

  @NonNull
  private final String baseUrl;

  public TCGAClient() {
    this(TCGA_BASE_URL);
  }

  @NonNull
  public String getUUID(String barcode) {
    val mappingUrl = getBarcodeMappingURL(barcode);
    val mapping = getMapping(mappingUrl);

    return getMappingUUID(mapping);
  }

  @NonNull
  public String getBarcode(String uuid) {
    val mappingUrl = getUUIDMappingURL(uuid);
    val mapping = getMapping(mappingUrl);

    return getMappingBarcode(mapping);
  }

  private String getBarcodeMappingURL(String barcode) {
    return getMappingURL("/barcode" + "/" + barcode);
  }

  private String getUUIDMappingURL(String uuid) {
    return getMappingURL("/uuid" + "/" + uuid);
  }

  private String getMappingURL(String path) {
    return baseUrl + "/uuid/uuidws/mapping" + "/json" + path;
  }

  private static String getMappingUUID(JsonNode mapping) {
    return mapping.path("uuidMapping").path("uuid").asText();
  }

  private static String getMappingBarcode(JsonNode mapping) {
    return mapping.path("barcode").asText();
  }

  @SneakyThrows
  private static JsonNode getMapping(String url) {
    return DEFAULT.readTree(new URL(url));
  }

}
