package org.obiba.mica.study.rest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.obiba.mica.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class TestImportMain {
	
	private static final Logger log = LoggerFactory.getLogger(TestImportMain.class);
	
	public static void main(String[] args) {
		
		StudiesImportResource resource = new StudiesImportResource();
		
		try {
			
			/*String rawContent = resource.getRawContent(
					//"https://recap.inesctec.pt/pub", "gfcg", "password", 
					//"https://recap-test.inesctec.pt/pub", "testaccess", "password", 
					"https://recap-test.inesctec.pt/pub", "gfcg", "password", 
					null, "/ws/draft/individual-study/epibel" );*/
			
			Map<String, Object> remoteContent = resource.getJSONContent(
					//"https://recap.inesctec.pt/pub", "gfcg", "password", 
					//"https://recap-test.inesctec.pt/pub", "testaccess", "password", 
					"https://recap-test.inesctec.pt/pub", "gfcg", "password", 
					null, "/ws/config/individual-study/form-custom" );
					
			Map<String, Object> remoteContent2 = resource.getJSONContent(
					"https://recap-preterm.inesctec.pt/pub", "administrator", "correctduckpretermsubject", 
					//"http://localhost:8082", "administrator", "password", 
					null, "/ws/config/individual-study/form-custom" );
			
			
			String schema = (String)remoteContent.get("schema");
			String definition = (String)remoteContent.get("definition");
			
			String schema2 = (String)remoteContent2.get("schema");
			String definition2 = (String)remoteContent2.get("definition");
			
			
			testJSONCompare(schema, schema2);
			
			//testJSONCompare(definition, definition2);
			
			
		
		} catch (IOException e) {
			
			e.printStackTrace();
			
		} catch (URISyntaxException e) {
			
			e.printStackTrace();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}

	private static void testJSONCompare(String remoteContent, String remoteContent2)
			throws IOException, JsonParseException, JsonMappingException, JsonProcessingException, InterruptedException {
		ObjectMapper mapper = new ObjectMapper();
		
		for (int i = 0; i < 1; i++) {
			
		    JsonNode jsonNode = mapper.readValue(remoteContent, JsonNode.class);
		    String jsonString = mapper.writeValueAsString(remoteContent);
		    String jsonPretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(remoteContent);
		    String jsonReplace = remoteContent.replace("\\n", "");
		    JsonNode jsonNodeReplace = mapper.readValue(jsonReplace, JsonNode.class);
		    JsonNode jsonNodePretty = (new ObjectMapper()).readValue(jsonPretty, JsonNode.class);
		    JsonNode jsonNodeString = (new ObjectMapper()).readValue(jsonString, JsonNode.class);
		    String toRawConcat = toRawConcat(remoteContent);
		    
			
			JsonNode jsonNode2 = mapper.readValue(remoteContent2, JsonNode.class);
		    String jsonString2 = mapper.writeValueAsString(remoteContent2);
		    String jsonPretty2 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(remoteContent2);
		    String jsonReplace2 = remoteContent2.replace("\\n", "");
		    JsonNode jsonNodeReplace2 = mapper.readValue(jsonReplace2, JsonNode.class);
		    JsonNode jsonNodePretty2 = (new ObjectMapper() ).readValue(jsonPretty2, JsonNode.class);
		    JsonNode jsonNodeString2 = (new ObjectMapper() ).readValue(jsonString2, JsonNode.class);
		    String toRawConcat2 = toRawConcat(remoteContent2);
		    
		    
			
		    log.debug("remoteContent    {}", remoteContent );
			log.debug("remoteContent2   {}\n", remoteContent2 );
			
			log.debug("jsonString       {}", jsonString );
			log.debug("jsonString2      {}\n", jsonString2 );
			
			log.debug("jsonNode         {}", jsonNode );
			log.debug("jsonNode2        {}\n", jsonNode2 );
			
			log.debug("jsonPretty       {}", jsonPretty );
			log.debug("jsonPretty2      {}\n", jsonPretty2 );
			
			log.debug("jsonReplace      {}", jsonReplace );
			log.debug("jsonReplace2     {}\n", jsonReplace2 );
			
			log.debug("jsonNodeReplace  {}", jsonNodeReplace );
			log.debug("jsonNodeReplace2 {}\n", jsonNodeReplace2 );
			
			log.debug("jsonNodePretty   {}", jsonNodePretty );
			log.debug("jsonNodePretty2  {}\n", jsonNodePretty2 );
			
			log.debug("jsonNodeString   {}", jsonNodeString );
			log.debug("jsonNodeString2  {}\n", jsonNodeString2 );
			
			log.debug("toRawConcat      {}", toRawConcat );
			log.debug("toRawConcat2     {}\n", toRawConcat2 );
			
			/*if (mapper.readTree(remoteContent.toString()).equals( mapper.readTree(remoteContent2.toString())) ) {
			
			
			//if (remoteContent.compareTo(remoteContent2) == 0) {
				
				log.info(Boolean.TRUE.toString());
			} else {
				log.info(Boolean.FALSE.toString());
			}*/
		}
	}
	
	
	// Takes input and returns a string with all elements concatenated.
	// withExactBigDecimals(true) makes sure trailing zeros (e.g. 5.50) will be
	// preserved
	static String toRawConcat(String input) throws IOException {
		
	    ObjectMapper mapper = new ObjectMapper()
	        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
	        .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
	        .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
	    
	    JsonNode node = mapper.readTree(input);
	    
	    return nodeToString(node);
	}

	// Removes whitespaces from a string
	static String removeWs(String input) {
	    return input.replaceAll("\\s+", "");
	}

	// Inspects the node type and returns the node contents as a string
	private static String nodeToString(JsonNode node) {
	    switch (node.getNodeType()) {
	        case NULL:
	        case BOOLEAN:
	        case STRING: return removeWs(node.asText());
	        case NUMBER: return node.decimalValue().toString();
	        case ARRAY: {
	                StringBuilder s = new StringBuilder("");
	                
	                Iterator<JsonNode> it = node.elements();
	                while (it.hasNext()) {
	                    s.append( nodeToString(it.next()) );
	                }
	                
	                return s.toString();
	            }
	        case OBJECT:
	            {
	            	StringBuilder s = new StringBuilder("");
	            	
	                Iterator<Entry<String, JsonNode>> it = node.fields();
	                while (it.hasNext()) {
	                    Entry<String, JsonNode> sub = it.next();
	                    s.append( removeWs(sub.getKey()) + nodeToString(sub.getValue()) );
	                }
	                
	                return s.toString();
	            }
	        default:
	        	
	            throw new UnsupportedOperationException("Node type " + node.getNodeType() + " not supported");
	    }
	}
}		
