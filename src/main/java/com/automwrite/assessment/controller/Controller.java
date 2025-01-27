package com.automwrite.assessment.controller;


import com.automwrite.assessment.model.client.ClientData;
import com.automwrite.assessment.model.organization.OrganizationData;
import com.automwrite.assessment.service.LlmService;
import com.automwrite.assessment.service.util.DocumentService;
import lombok.AllArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import com.automwrite.assessment.service.JsonParserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import java.util.Set;
import java.util.Map;

import static com.automwrite.assessment.service.util.FileParserService.parseTxtFile;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class Controller {

    @GetMapping("/")
    public String home() {
        return "Welcome to the homepage!";
    }

    @GetMapping("/clientdata")
    public ClientData getClientData() {
        try {
            ClientData client =  jsonParserService.loadClientData();  // Load client data using the service

            // Create a Gson instance
            Gson gson = new Gson();

            // Convert Java object to JSON string
            String jsonString = gson.toJson(client);

            // Convert JSON string to JSON object
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

            // Access attributes of the JSON object  .getString(
            Set<String> keys = jsonObject.keySet();

            //log.debug ("JSON Object {}", jsonObject);
            log.debug ("JSON object KEYS are {}", keys);


            Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
            //log.debug ("JSON Entry Set is {}", entries);

            for (Map.Entry<String, JsonElement> entry : entries) {

            	//printNestedKeysAndValues (entry.getValue());

			}

            JsonElement infoElem = jsonObject.get("clientInfo");
            //log.debug ("JSON Info Element is {}", infoElem);

            return client;

        } catch (IOException e) {
            e.printStackTrace();
            return null;  // Handle error by returning null or an appropriate message
        }
    }

    @GetMapping("/orgdata")
    public OrganizationData getOrganizationData() {
        try {
            return jsonParserService.loadOrganizationData();  // Load client data using the service
        } catch (IOException e) {
            e.printStackTrace();
            return null;  // Handle error by returning null or an appropriate message
        }
    }

    private final LlmService llmService;
    private final JsonParserService jsonParserService;
    private final DocumentService documentService;

    /**
     * Processes the uploaded .txt file to extract user intent, utilises JSON data and an LLM service
     * to process the intent, and generates a .docx file using a predefined template.
     *
     * @param file File to extract the user intent from
     * @return A response indicating that the processing has completed
     * @throws IOException If an error occurs while reading the file or processing the document
     */

    @PostMapping("/user-request")
    public ResponseEntity<String> handleUserRequest
							(@RequestParam("file") MultipartFile file) throws IOException {

        // Log the incoming request
        log.debug("Received file upload request. File name: {}, File size: {} bytes", file.getOriginalFilename(), file.getSize());


        // Parse the .txt file to extract the user intent
        String userIntent = parseTxtFile(file);

        log.debug("Extracted user intent: {}", userIntent);

        // Fetch and parse the JSON data
        ClientData client = jsonParserService.loadClientData();
        OrganizationData org = jsonParserService.loadOrganizationData();

        // TODO: Process the user intent using JSON data and the LLM service
        Map<String, String>[] processedContent = llmService.processIntent(userIntent, client, org);
        //String processedContent = llmService.processIntent(userIntent, client, org);

        // Load the template document
        XWPFDocument templateDocument = documentService.loadTemplate();

        // TODO: Insert the processed content into the relevant section in the template
        documentService.insertContentIntoTemplate(templateDocument, processedContent);

        // Save the modified document to a new .docx file
        documentService.saveDocument(templateDocument);

        // Return a response indicating successful processing
        return ResponseEntity.ok("User request processed, recommendation created.");
    }




     private static void printNestedKeysAndValues(JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey(); 
                JsonElement value = entry.getValue();
    
                log.debug ("Key: {}", key);
    
                if (value.isJsonObject() || value.isJsonArray()) {
                    printNestedKeysAndValues(value);
                } else {
                    log.debug ("Value: {}", value);
                }
            }
        } else if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                printNestedKeysAndValues(element);
            }
        }
    }

}
