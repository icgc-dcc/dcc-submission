/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.genes.extra;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;

import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

@Slf4j
public class PathwayHierarchyParser {

  /**
   * Reactome XML name constants.
   */
  private static final String REACTOME_PATHWAY_ELEMENT_NAME = "Pathway";
  private static final String REACTOME_DB_ID_ATTRIBUTE_NAME = "dbId";
  private static final String REACTOME_DISPLAY_NAME_ATTRIBUTE_NAME = "displayName";
  private static final String REACTOME_HAS_DIAGRAM_ATTRIBUTE_NAME = "hasDiagram";

  /**
   * Default Reactome web service URl.
   */
  private static final String DEFAULT_REACTOME_PATHWAY_HIERARCHY_URL =
      "http://www.reactome.org/ReactomeRESTfulAPI/RESTfulWS/pathwayHierarchy/homo+sapiens";

  /**
   * Parses the supplied Reactome pathway hierarchy {@code url} to produce a mapping from pathway names to a set of
   * {@link PathwaySegment} lists.
   * 
   * @param url the URL to parse
   * @return the mapping
   * @throws IOException
   */
  public HashMultimap<String, List<PathwaySegment>> parse(URL url) throws IOException {
    log.info("Parsing '{}'...", url);
    val document = parseDocument(url);
    val segments = new Stack<PathwaySegment>();
    val results = HashMultimap.<String, List<PathwaySegment>> create();

    process(document.getDocumentElement(), segments, results);

    return results;
  }

  public HashMultimap<String, List<PathwaySegment>> parse() throws IOException {
    return parse(new URL(DEFAULT_REACTOME_PATHWAY_HIERARCHY_URL));
  }

  private static void process(Node node, Stack<PathwaySegment> segments, Multimap<String, List<PathwaySegment>> results) {
    val children = node.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
      val child = children.item(i);

      if (isPathwayElement(child)) {
        val segment = createPathwaySegment(child);

        // Keep track of path from root
        results.put(segment.getName(), ImmutableList.copyOf(segments));

        // Recurse
        segments.push(segment);
        process(child, segments, results);
        segments.pop();
      }
    }
  }

  private static PathwaySegment createPathwaySegment(Node currentNode) {
    // Extract
    val attributes = currentNode.getAttributes();
    val id = attributes.getNamedItem(REACTOME_DB_ID_ATTRIBUTE_NAME).getNodeValue();
    val name = attributes.getNamedItem(REACTOME_DISPLAY_NAME_ATTRIBUTE_NAME).getNodeValue();
    val hasDiagram = attributes.getNamedItem(REACTOME_HAS_DIAGRAM_ATTRIBUTE_NAME);

    if (hasDiagram != null) {
      return new PathwaySegment(id, name, null, hasDiagram.getNodeValue());
    }
    return new PathwaySegment(id, name, null, "false");
  }

  private static boolean isPathwayElement(Node currentNode) {
    return currentNode.getNodeType() == Node.ELEMENT_NODE
        && currentNode.getNodeName().equals(REACTOME_PATHWAY_ELEMENT_NAME);
  }

  @SneakyThrows
  private static Document parseDocument(URL url) throws IOException {
    @Cleanup
    val inputStream = url.openStream();

    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
  }

  /**
   * A node in of a pathway hierarchy path
   */
  @Data
  @AllArgsConstructor
  public static class PathwaySegment {

    private String id;
    private String name;
    private String reactomeId;
    private String hasDiagram;

  }

}
