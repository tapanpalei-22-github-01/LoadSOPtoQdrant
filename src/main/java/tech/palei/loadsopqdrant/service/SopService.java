package tech.palei.loadsopqdrant.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class SopService {

    @Value("${sop-service.base-url}")
    private String baseUrl;
    @Value("${sop-service.api-endpoint}")
    private String apiEndpoint; 
    @Value("${sop-service.username}")
    private String username;    
    @Value("${sop-service.password}")
    private String password;    


        public String retrieveSopDocument() {

        log.info("Retrieving SOP document from Confluence");

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
        Document doc = Jsoup.parse(response, "", Parser.xmlParser());
        

        String cleanedText = doc.text()
            .replaceAll("\\n{2,}", "\n\n")
            .trim();


            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = null;
            try {
                root = mapper.readTree(cleanedText);
            } catch (JsonMappingException e) {
                
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                
                e.printStackTrace();
            }

            JsonNode storageValueNode = root
                    .path("results")
                    .path(0)
                    .path("body")
                    .path("storage")
                    .path("value");
            
            cleanedText=storageValueNode.asText();
            
            

        log.info("Retrieved SOP "+cleanedText);

        return cleanedText;
    }   
}
