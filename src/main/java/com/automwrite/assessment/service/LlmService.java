package com.automwrite.assessment.service;
import java.util.concurrent.CompletableFuture;
import com.automwrite.assessment.model.client.ClientData;
import com.automwrite.assessment.model.organization.OrganizationData;
import java.util.Map;

import java.util.concurrent.CompletableFuture;

public interface LlmService {

    String generateText(String prompt);

    CompletableFuture<String> generateTextAsync(String prompt);

    Map<String, String>[]  processIntent(String userIntent, ClientData client, OrganizationData org);

}
