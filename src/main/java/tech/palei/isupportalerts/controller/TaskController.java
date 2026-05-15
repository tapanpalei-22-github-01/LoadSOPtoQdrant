package tech.palei.isupportalerts.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.poi.hdgf.chunks.Chunk.Command;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.networknt.schema.keyword.MinMaxContainsValidator.Analysis;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private ChatClient chatClient;
   // MessageChatMemoryAdvisor
    private ChatMemory chatMemory;
    
    @Autowired
    VectorStore vectorStore;

    private record AnalysisGuide(String instance, List<String> analysisSteps) {};
    private record CommandGuide(String command) {};
    private List<String> commands=new ArrayList<>() ;
    private record CommandResult(String command, String result) {};
    List<CommandResult> commandResults = new ArrayList<>();
    
    TaskController(ChatClient.Builder chatClientBuilder,ToolCallbackProvider toolCallbackProvider, ChatMemory chatMemory ) {
        Advisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        this.chatClient = chatClientBuilder
        .defaultToolCallbacks(toolCallbackProvider)
        .defaultAdvisors(List.of(messageChatMemoryAdvisor,new SimpleLoggerAdvisor()))
        .build();
    }

    @GetMapping("/chat")
    public String chat(
            String message,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String title,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String heading) {

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

        String context = docs.stream().map(doc -> doc.getFormattedContent())
            //.map(Document::getContent)
            .collect(Collectors.joining("\n\n"));

        

        

        AnalysisGuide analysisGuide = chatClient.prompt()
            .system("""
                You are a helpful assistant. 
                Always use all available information from the context to answer the question.
                Please find the instance name for the alert code given by user.
                Please respond analysis steps in json format as shown in example below:
                {
                    "instance": "instance_name",
                    "analysisSteps": [
                        "step 1",
                        "step 2",
                        "step 3"
                    ]
                }

            """)
            .user("""
                CONTEXT:
                %s

                QUESTION:
                %s
            """.formatted(context, message))
            .call().entity(AnalysisGuide.class);

            StringBuilder taskResults = new StringBuilder();

            for(String step : analysisGuide.analysisSteps) {
                CommandGuide commandGuide = chatClient.prompt()
                    .system("""
                        You are a helpful assistant. 
                        Please analyze the following step and resolve the command to run.
                        return the command to run in json format as shown in example below:
                        {
                            "command": "command to run"
                        }
                    """)
                    .user("""
                        STEP:
                        %s
                    """.formatted(step))
                    .call().entity(CommandGuide.class);
                taskResults.append(commandGuide.command).append("\n");
                commands.add(commandGuide.command);
            }
            

            for(String command : commands) {
                CommandResult commandResult = chatClient.prompt()
                    .system("""
                        You are a helpful assistant. 
                        Check for tools available in mcpserver and find the tool that can be used to run the command. 
                        The tool name is "executeCommand" which executes shell command and returns the result.
                        return the command and the result in json format as shown in example below:
                        {
                            "command": "command to run",
                            "result": "result of command execution"
                        }
                    """)
                    .user("""
                        STEP:
                        %s
                    """.formatted(command))
                    .call().entity(CommandResult.class);
                commandResults.add(commandResult);
            }
            
        return commandResults.toString();
        //return analysisGuide.instance + "\n" + String.join("\n", analysisGuide.analysisSteps);
    }
}
