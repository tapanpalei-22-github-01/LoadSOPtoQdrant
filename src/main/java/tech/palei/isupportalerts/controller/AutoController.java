package tech.palei.isupportalerts.controller;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import tech.palei.isupportalerts.service.SopService;

@Slf4j
@RestController
@RequestMapping("/api/auto")
public class AutoController {

    private ChatClient chatClient;

    @Autowired
    SopService sopService;

    @Autowired
    VectorStore vectorStore;

    String stringOfTitles;

    private record AnalysisResponse(String result, String action) {};
    private record AlertCodeWiseUserMessage(String alertCode, String userMessage) {};
    private List<AlertCodeWiseUserMessage> alertCodeWiseUserMessages=new ArrayList<>();
    
    AutoController(ChatClient.Builder chatClientBuilder,ToolCallbackProvider toolCallbackProvider ) {
        //Advisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.chatClient = chatClientBuilder
        .defaultToolCallbacks(toolCallbackProvider)
        //.defaultAdvisors(List.of(messageChatMemoryAdvisor,new SimpleLoggerAdvisor()))
        .defaultAdvisors(List.of(new SimpleLoggerAdvisor()))
        .build();
    }

    @GetMapping(value="/chat",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    //@GetMapping(value="/chat")
    public Flux<String> chat(
            @RequestParam String message,
            @RequestParam(required = false) String heading) {

        // Get List of Titles from qdrant DB using SOP service.
        stringOfTitles=sopService.StringOfChildTitles();

        //Prepare a list of alert codes
        alertCodeWiseUserMessages=chatClient.prompt()
        .system(s -> s.text("""
            Identify list of alert codes from the user message. 
            Here is an example of Alert Code EDG_002. it has Three capital letter followed by an underscore character and then followed by 3 digit number.
            Transform an user message and output in a JSON Array format as shown below. 
            for example:
            input user message is: Please analyze and fix the issues EDG_002, ITS_001 and MTP_003.
            The expected result in JSON Array format is:
                [
                    {{
                        "alertCode": "write the alert code here",
                        "userMessage": "write the user message related to that alert code here. make sure to include the alert code in the user message"
                    }},
                    {{
                        "alertCode": "write the next alert code here",
                        "userMessage": "write the user message related to that alert code here. make sure to include the alert code in the user message"
                    }}
                ]

        """).param("titles", stringOfTitles))
        .user(message)
        .call()
        .entity(new ParameterizedTypeReference<List<AlertCodeWiseUserMessage>>() {});

        return Flux.fromIterable(alertCodeWiseUserMessages)
            .flatMapSequential(alertCodeWiseUserMessage -> {
                log.info(alertCodeWiseUserMessage.alertCode() + ": " + alertCodeWiseUserMessage.userMessage());
                AtomicBoolean started = new AtomicBoolean(false);
                return processAnAlertCode(alertCodeWiseUserMessage.alertCode(),alertCodeWiseUserMessage.userMessage())
                    .map(analysisResponse -> {
                        String output="";
                        if(!started.get() && analysisResponse.result().startsWith("Processing")){
                            output ="\nProcessing..";
                            started.set(true);
                        } else if(analysisResponse.result().startsWith("Processing")){
                            output ="..";
                        } else {
                            output = "\nAnalysis Result: " + analysisResponse.result() + "\n"
                                    + "\nNext Action: " + analysisResponse.action() + "\n";
                        }
                        return output;
                    });
            });

    }

    public Flux<AnalysisResponse> processAnAlertCode(String alertCode,String message){
        return processAnAlertCode(alertCode,message,null);
    }
    
    public Flux<AnalysisResponse> processAnAlertCode(String alertCode,String message,String heading){
        return Flux.create(sink -> {
        //process each Alert Code
        String title=chatClient.prompt()
        .system(s -> s.text("""
            {alertCode}
            List Of Titles:
            {titles}

            Return the title that contains that 3 digit number at the beginning in the list of title given above. Make sure to return the original title name including the 3 digit number.          
        """).param("titles", stringOfTitles)
            .param("alertCode", alertCode)
            )
        .user(message)
        .call()
        .content();

        log.info("matchedTitle: {}"+title);
        sink.next(new AnalysisResponse("SOP Page Title: ", title));


        log.info("Received message: {}, title: {}, heading: {}", message, title, heading);
        SearchRequest.Builder searchBuilder = SearchRequest.builder()
            .query(message)
            .topK(5);

        // Build filter expression only for provided params
        if (title != null && heading != null) {
            searchBuilder.filterExpression("title == '" + title + "' && heading == '" + heading + "'");
        } else if (title != null) {
            searchBuilder.filterExpression("title == '" + title + "'");
        } else if (heading != null) {
            searchBuilder.filterExpression("heading == '" + heading + "'");
        }

        List<Document> docs = vectorStore.similaritySearch(searchBuilder.build());
        log.info("No Of Doc: {}", docs.size());

        String stepsForAnalysis = docs.stream()
            .filter(doc -> doc.getMetadata().get("heading") != null 
                        && doc.getMetadata().get("heading").toString().toLowerCase().contains("analyze")  )
            .map(doc -> doc.getText())
            .collect(Collectors.joining("\n\n"));
            //
        
        sink.next(new AnalysisResponse("Steps for Analysis: ", stepsForAnalysis));

        String stepsForFix = docs.stream()
            .filter(doc -> doc.getMetadata().get("heading") != null 
                        && doc.getMetadata().get("heading").toString().toLowerCase().contains("fix")  )
            .map(doc -> doc.getText())
            .collect(Collectors.joining("\n\n"));

        sink.next(new AnalysisResponse("Steps to Fix: ", stepsForFix));

        String guide = docs.stream().map(doc -> doc.getText())
            //.map(Document::getContent)
            .collect(Collectors.joining("\n\n"));

        log.info("Guide: {}", guide);

        String initialContext = """
                You are a helpful assistant. 
                Identify all the availabe toos in mcpserver. Always use these tools for any actions. Do not take any action or run any command which is not in the tools list.
                Always use all available information in INITIAL CONTEXT:, GUIDELINE: and FINDINGS: for decision making for NEXT ACTION:.
                Follow one step at a time in the GUIDELINE:, verify against FINDINGS:, return the result and the next action.
                Please respond in json format as shown in example below:
                {
                    "result": "your analysis findings and the execution result of the command",
                    "action": "your decision on the next action to be taken. if no further action is needed then set action as DONE."
                }
            """;

      
        int loopLimit=5;
        //return Flux.create(sink -> {

            ScheduledExecutorService scheduler =
                    Executors.newSingleThreadScheduledExecutor();

            AtomicBoolean finished = new AtomicBoolean(false);

            scheduler.scheduleAtFixedRate(() -> {
                if (!finished.get()) {
                    sink.next(new AnalysisResponse("Processing...", "Please wait..."));
                }
            }, 0, 2, TimeUnit.SECONDS);

            // ✅ MOVE blocking work OFF the request thread
            Schedulers.boundedElastic().schedule(() -> {
                try {

                    int loopCount = 0;
                    boolean isDone = false;
                    String findings = "";
                    String userMessage = message;

                    while (!isDone && loopCount < loopLimit) {

                        AnalysisResponse analysisResponse =
                                chatClient.prompt()
                                        .system("""
                                            INITIAL CONTEXT:
                                            %s
                                            GUIDELINE:
                                            %s
                                            FINDINGS:
                                            %s
                                        """.formatted(initialContext, guide, findings))
                                        .user("""
                                            NEXT ACTION:
                                            %s
                                        """.formatted(userMessage))
                                        .call()
                                        .entity(AnalysisResponse.class);

                        sink.next(analysisResponse);

                        findings += "\n" + analysisResponse.result();
                        userMessage = "\n" + analysisResponse.action();

                        if ("DONE".equalsIgnoreCase(analysisResponse.action())) {
                            isDone = true;
                        }

                        loopCount++;
                    }

                    finished.set(true);
                    sink.complete();

                } catch (Exception e) {
                    sink.error(e);
                } finally {
                    scheduler.shutdown();
                }
            });
        });


    }

    

}
