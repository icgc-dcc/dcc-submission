package uk.ac.ebi.mydas.examples;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import org.junit.Test;

import uk.ac.ebi.mydas.exceptions.DataSourceException;
import uk.ac.ebi.mydas.model.DasAnnotatedSegment;
import uk.ac.ebi.mydas.model.DasFeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class ElasticSearchParserTest {

	@Test
	public void testParse() throws JsonProcessingException, IOException, DataSourceException {
		VariantParser parser = new VariantParser();
		ArrayList<String> featureid = new ArrayList<String>();
		featureid.add("8332");
		//featureid.add("832_0");
		Collection<DasAnnotatedSegment> segments = parser.parseByFeatureId(featureid);
		for(DasAnnotatedSegment segment:segments){
			//System.out.println(segment);
			for(DasFeature feature:segment.getFeatures()){
				System.out.println(feature);
			}
			System.out.println(featureid);
		}
		/*for(DasFeature feature:parser.parseBySegmentId("12", 1, 1000000000).getFeatures()){
			System.out.println(feature);
		}*/
		//System.out.println(parser.parseBySegmentId("12", 1, 1000000000));
		/*System.out.println(gene);
		System.out.println(gene.path("symbol").asText());
		System.out.println(gene.path("start").asLong());
		for (JsonNode transcript : gene.path("transcripts")){
			System.out.println(transcript.path("length").asLong());

		}*/
		
	}
	
}
