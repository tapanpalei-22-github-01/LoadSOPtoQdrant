package tech.palei.isupportalerts.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import tech.palei.isupportalerts.utils.LoggingInterceptor;
import tech.palei.isupportalerts.utils.SopUtil;
@Slf4j
@Service
public class SopService {

    @Autowired
    VectorStore vectorStore;

    @Value("${sop-service.base-url}")
    String baseUrl;
    @Value("${sop-service.username}")
    String username;    
    @Value("${sop-service.password}")
    String password;

    @Value("${sop-service.confluence-space-key}")
    String defaultSpaceKey; 
    @Value("${sop-service.title}")
    String defaultTitle;    
    String apiEndpoint; 

    private static Logger logger=LoggerFactory.getLogger("retrieveSopDocument.class");

    public String StringOfChildTitles(){
        List<String> titles=listChildTitles();
        StringBuilder sbTitle = new StringBuilder();
        for (String title:titles){
            sbTitle.append("\n"+title+"\n");
        }
        return sbTitle.toString();
    }

    public List<String> listChildTitles() {
        
        return listChildTitles(defaultSpaceKey,defaultTitle);
    }

    public List<String> listChildTitles(String spaceKey,String title) {

        // Retreive the page ID first by title and spaceKey, then use the page ID to retrieve child pages
        apiEndpoint="/rest/api/content/search?cql=title=\""+title +"\"&spaceKey="+spaceKey;       

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers ->
                        headers.setBasicAuth("tpalei", password))
                .requestInterceptors(interceptors ->
                        interceptors.add(new LoggingInterceptor()))
                .build();

        String response = restClient.get()
                .uri(apiEndpoint)
                .retrieve()
                .body(String.class);

        System.out.println(response);
        
        // ✅ Extract results[0].id
        
        String pageId = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(response);
            pageId = root.path("results").get(0).path("id").asText();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        logger.info("pageId: {}"+ pageId);

        // Requesting for Child Pages using pageId

        apiEndpoint="/rest/api/content/"+pageId+"/child/page";
        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers ->
                        headers.setBasicAuth(username, password))
                .requestInterceptors(interceptors ->
                        interceptors.add(new LoggingInterceptor()))
                .build();
        
        response = restClient.get()
                .uri(apiEndpoint)
                .retrieve()
                .body(String.class);

        try {
            root = mapper.readTree(response);
        } catch (JsonMappingException e) {

            e.printStackTrace();
        } catch (JsonProcessingException e) {

            e.printStackTrace();
        }

        List<String> titles = new ArrayList<>();

        JsonNode results = root.get("results");
        if (results != null && results.isArray()) {
            for (JsonNode item : results) {
                
                titles.add(item.get("title").asText());
            }
        }


        logger.info("titles: {}",titles);

        return titles;
    }

    public String loadSopDocument(String userSpaceKey,String userTitle) {
        List<String> childPageTitles=listChildTitles(userSpaceKey,userTitle);

        for(String pageTitle:childPageTitles){
            loadSopDocumentByTitle(userSpaceKey, pageTitle);
        }
        return "Load Successful";
    }

    public void loadSopDocumentByTitle(String userSpaceKey,String userTitle) {

        log.info("Retrieving SOP document from Confluence");
        String spaceKey=(userSpaceKey!=null?userSpaceKey:defaultSpaceKey);
        String title=(userTitle!=null?userTitle:defaultTitle);
        apiEndpoint="/rest/api/content?spaceKey="+spaceKey+"&title="+title+"&expand=body.storage";


        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers ->
                        headers.setBasicAuth(username, password))
                .requestInterceptors(interceptors ->
                        interceptors.add(new LoggingInterceptor()))
                .build();

        String response = restClient.get()
                .uri(apiEndpoint)
                .retrieve()
                .body(String.class);

        if(response == null) {
            logger.info("No response return from API call: {}"+apiEndpoint);
            
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(response);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // get the HTML content out of the response
        String content =
            root.path("results")
                .get(0)
                .path("body")
                .path("storage")
                .path("value")
                .asText();

        //split the HTML content by headings using chunkHtml()
        //chunkHtml() return map of (heading,content)
        //
        List<org.springframework.ai.document.Document> documents = SopUtil.chunkHtml(content).entrySet().stream()
            .map(entry -> {
                log.info("Chunk ID: {}", entry.getKey());

                // Deterministic ID: same chunk always maps to same Qdrant point (idempotent)
                String idSource = title + "|" + entry.getKey();
                String docId = java.util.UUID.nameUUIDFromBytes(idSource.getBytes()).toString();

                // text = the actual chunk content (required by Spring AI Document)
                // metadata = title, heading, source page (searchable/filterable later)
                Map<String, Object> metadata = Map.of(
                    "source",  "confluence",
                    "spaceKey",spaceKey,
                    "title",   title,
                    "heading", entry.getKey()
                );

                return org.springframework.ai.document.Document.builder()
                    .id(docId)
                    .text(entry.getValue())   // stored as "text" in Qdrant payload
                    .metadata(metadata)       // stored as extra payload fields
                    .build();
            })
            .toList();

        // VectorStore automatically embeds each document and upserts into Qdrant
        vectorStore.add(documents);
        log.info("Stored {} chunks into vector store", documents.size());
        //return content;        
        
        // Document doc = Jsoup.parse(response, "", Parser.xmlParser());
        

        // String cleanedText = doc.toString()
        //     .replaceAll("\\n{2,}", "\n\n")
        //     .trim();


        //     ObjectMapper mapper = new ObjectMapper();
        //     JsonNode root = null;
        //     try {
        //         root = mapper.readTree(cleanedText);
        //     } catch (JsonMappingException e) {
                
        //         e.printStackTrace();
        //     } catch (JsonProcessingException e) {
                
        //         e.printStackTrace();
        //     }

        //     JsonNode storageValueNode = root
        //             .path("results")
        //             .path(0)
        //             .path("body")
        //             .path("storage")
        //             .path("value");
            
        //     cleanedText=storageValueNode.asText();
            
            

        // log.info("Retrieved SOP "+cleanedText);

        // return cleanedText;

        //return doc.toString();
        
    }   
}
