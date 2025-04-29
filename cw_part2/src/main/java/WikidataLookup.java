import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;



public class WikidataLookup extends LookupService{
	
	//Help: //https://www.wikidata.org/w/api.php?action=help&modules=wbsearchentities
	//Example: https://www.wikidata.org/w/api.php?action=wbsearchentities&search=virgin%20media&language=en
	
		
	private final String REST_URL = "https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&";
		
	private final String limit = "limit";
	private final String search = "search";
	private final String language = "language";	
	//Not supported
	//private final String QueryClass = "QueryClass";
	
	private int hits=10;
	private String lang="en";
	
	protected URL buildRequestURL(String query, int max_hits, String lang)
			throws URISyntaxException, MalformedURLException {
		URIBuilder ub = new URIBuilder(getREST_URL());
		//ub.addParameter(QueryClass, cls_type);
		ub.addParameter(limit, String.valueOf(max_hits));
		ub.addParameter(language, lang);
		ub.addParameter(search, query);
		return ub.build().toURL();
		
	}

	@Override
	protected String getREST_URL() {		
		return REST_URL;
	}


	public Set<KGEntity> getKGEntities(String query, String cls_type, int max_hits, String language)
			throws JsonProcessingException, IOException, URISyntaxException {
		return getKGEntities(query, max_hits, language);
	}
	
	
	
	public Set<KGEntity> getKGEntities(String query) throws JsonProcessingException, IOException, URISyntaxException{
		return getKGEntities(query, hits, lang);
	}
	
	
	/**
	 * 
	 * @param query
	 * @param max_hits
	 * @param language
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public Set<KGEntity> getKGEntities(String query, int max_hits, String language)
			throws JsonProcessingException, IOException, URISyntaxException {
		
		Set<KGEntity> entities = new HashSet<KGEntity>();
		
		
		URL urlToGet = buildRequestURL(query, max_hits, language);
		
		//System.out.println(urlToGet);
		//System.out.println(getRequest(urlToGet));
		
		
		for (JsonNode result : jsonToNode(getRequest(urlToGet)).get("search")){
			
			//System.out.println(result.toString());
			
			KGEntity ent = new KGEntity();
			if (result.has("concepturi"))
				ent.setId(result.get("concepturi").asText());
			
			if (result.has("label"))
				ent.setName(result.get("label").asText());
			if (result.has("description"))
				ent.setDescription(result.get("description").asText());
			entities.add(ent);
			
			//entities.add(result.get("concepturi").asText());
		}
		
		return entities;
	}
	
	
	
	
	
	public static void main(String[] args){
		WikidataLookup lookup = new WikidataLookup();
		
		String keywords;
		keywords="Chicago Bulls";
		keywords="Congo";
		
		try {
			
			//Look up for entities matching the string "keywords" 
			Set<KGEntity> entities = lookup.getKGEntities(keywords);
			System.out.println("Number of candidates found: " + entities.size());
			
			for (KGEntity ent : entities){
				System.out.println(ent);
			}
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	
	
}
