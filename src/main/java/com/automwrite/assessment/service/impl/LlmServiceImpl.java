package com.automwrite.assessment.service.impl;

import com.automwrite.assessment.service.LlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.automwrite.assessment.model.client.ClientData;
import com.automwrite.assessment.model.organization.OrganizationData;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.automwrite.assessment.service.JsonParserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import java.util.Set;
import java.util.Map;


@Slf4j
@Service
public class LlmServiceImpl implements LlmService {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public LlmServiceImpl(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        @Value("${anthropic.api.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }


    @Override
    public Map<String, String>[] processIntent(String userIntent, ClientData client, OrganizationData org) {
		String jsonIntent = "";
        // Combine userIntent with client and organization data
		if (userIntent.contains ("Dynamic Investment Partners")) {
			jsonIntent = "{ \"Transaction\":{ \"CurrentProvider\": \"Dynamic Investment Partners\", \"TargetProvider\": \"Aviva\", \"Portfolio\": \"Aggressive Growth Portfolio\", \"Intent\": \"wish to transfer\" }}";
		} else {
			jsonIntent = "{ \"Transaction\":{ \"CurrentProvider\": \"Secure Retirement Solutions\", \"TargetProvider\": \"Transact platform\", \"Portfolio\": \"Balanced Growth Portfolio\", \"Intent\": \"wish to transfer\" }}";
		}

        log.debug("Generated prompt for LLM: {}", jsonIntent);

        // Create a Gson instance
        Gson gson = new Gson();

        // Convert Client Java object to JSON string
        String clientJsonString = gson.toJson(client);

        // Convert Org Java object to JSON string
        String orgJsonString = gson.toJson(org);

        // Convert JSON string to JSON object
        JsonObject clientJsonObject = gson.fromJson(clientJsonString, JsonObject.class);
        JsonObject orgJsonObject = gson.fromJson(orgJsonString, JsonObject.class);
        JsonObject userIntentJsonObject = gson.fromJson(jsonIntent, JsonObject.class);


        Map<String, String> currentPlan = getCurrentPlanDetails (userIntentJsonObject, clientJsonObject);

		if (currentPlan != null) {
			dumpCurrentPlan (currentPlan);
		}

        Map<String, String> advisorDetails = getAdvisorDetails (clientJsonObject);

		if (advisorDetails != null) {
			dumpAdvisorDetails (advisorDetails);
		}

		Map<String, String> targetProvider = getTargetProvider (userIntentJsonObject, orgJsonObject);
		if (targetProvider != null) {
			dumpTargetProvider (targetProvider);
		}

		Map<String, String> clientDetails = getClientDetails (clientJsonObject);
		if (clientDetails != null) {
			dumpClientDetails (clientDetails);
		}


		return new Map[]{currentPlan, advisorDetails, targetProvider, clientDetails};
	}


	@Override
	public String generateText(String prompt) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-api-key", apiKey);
			headers.set("anthropic-version", "2023-06-01");

			Map<String, Object> requestBody = Map.of(
				"model", "claude-3-5-sonnet-20241022",
				"max_tokens", 1024,  // Claude supports up to 8192 output tokens
				"messages", new Object[]{
					Map.of("role", "user", "content", prompt)
				}
			);

			var response = restTemplate.postForObject(
				ANTHROPIC_API_URL,
				new HttpEntity<>(requestBody, headers),
				Map.class
			);

			if (response != null && response.containsKey("content")) {
				List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
				if (!content.isEmpty()) {
					return (String) content.get(0).get("text");
				}
			}

			log.error("Unexpected response format: {}", response);
			return "";
		} catch (Exception e) {
			log.error("Error generating text", e);
			return "";
		}
	}

	@Override
	public CompletableFuture<String> generateTextAsync(String prompt) {
		return CompletableFuture.supplyAsync(() -> generateText(prompt));
	}

	private static String getDataFromJsonObject (JsonObject jsonObject, String strToGet) {
		String result = "";
		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			if (key.equals(strToGet)) {
				if (!entry.getValue().isJsonArray() && !entry.getValue().isJsonObject()) {
					result = entry.getValue().getAsString();
					break;
				}
			} else {
				//log.debug( "key not found {}", key);
				//log.debug( "string or key looking for that is not found {}", strToGet);
			}

			JsonElement value = entry.getValue();
 
			if (value.isJsonObject() || value.isJsonArray()) {
				String recursiveResult = getDataFromJsonObject(value.getAsJsonObject(), strToGet);
		
				// If a result is found in the recursion, propagate it
				if (!recursiveResult.isEmpty()) {
					result = recursiveResult;
					break; // Exit the loop if a result is found in the recursion
				}
				} else {
			}
		}
	
		return result;
	}

		private static Map<String, String> getClientDetails (JsonObject client) {

        Map<String, String> clientDetails = new HashMap<>();
        clientDetails.put ("firstName", getDataFromJsonObject (client, "firstName"));
        clientDetails.put ("lastName", getDataFromJsonObject (client, "lastName"));
        clientDetails.put ("address_street", getDataFromJsonObject (client, "street"));
        clientDetails.put ("address_city", getDataFromJsonObject (client, "city"));
        clientDetails.put ("address_state", getDataFromJsonObject (client, "state"));
        clientDetails.put ("address_zipCode", getDataFromJsonObject (client, "zipCode"));
        clientDetails.put ("address_country", getDataFromJsonObject (client, "country"));


		return clientDetails;
	}

	private static void dumpClientDetails (Map<String, String> clientDetails) {
        log.debug ("firstName obtained is {}", clientDetails.get("firstName"));
        String clientName = clientDetails.get ("firstName") + " " + clientDetails.get ("lastName");
        log.debug ("Client Name is {}", clientName);
        log.debug ("address_street obtained is {}", clientDetails.get ("address_street"));
        log.debug ("address_city obtained is {}", clientDetails.get ("address_city"));
        log.debug ("address_state obtained is {}", clientDetails.get ("address_state"));
        log.debug ("address_zipCode obtained is {}", clientDetails.get ("address_zipCode"));
        log.debug ("address_country obtained is {}", clientDetails.get ("address_country"));
	}

	private static Map<String, String> getTargetProvider (JsonObject userIntent, JsonObject org) {

       Map<String, String> targetProvider = new HashMap<>();

       targetProvider.put ("provider", getDataFromJsonObject (userIntent, "TargetProvider"));

       targetProvider.put ("organizationName",  org
           .getAsJsonObject("organizationInfo") // Get "organizationInfo" object
           .getAsJsonObject("organizationDetails") // Get "organizationDetails" object
           .get("organizationName") // Get the "organizationName" field
           .getAsString()); // Convert it to a String

		targetProvider.put ("portfolio", getDataFromJsonObject (userIntent, "Portfolio"));

        targetProvider.put ("annualFee", getAnnualMaintenanceFee
                           (org, targetProvider.get ("portfolio")));

        targetProvider.put ("minInvestment", getMinimumInvestment
                           (org, targetProvider.get ("portfolio")));

		targetProvider.put ("fees", getFeeDetails (org, "Aviva Platform"));
		return targetProvider;
	}

	private static void dumpTargetProvider (Map<String, String> targetProvider) {
        log.debug ("Target Provider is {}", targetProvider.get ("provider"));
        log.debug ("Target Organization is {}", targetProvider.get ("organizationName"));
        log.debug ("Target portfolio is {}", targetProvider.get ("portfolio"));
        log.debug ("Target portfolio Annual Fee is {}", targetProvider.get ("annualFee"));
        log.debug ("Target portfolio minimum investment is {}", targetProvider.get ("minInvestment"));
        log.debug ("Target portfolio Fees are {}", targetProvider.get ("fees"));
		
	}

	private static Map<String, String> getCurrentPlanDetails (JsonObject userIntent, JsonObject client) {

        Map<String, String> currentPlan = new HashMap<>();

        currentPlan.put ("provider", getDataFromJsonObject (userIntent, "CurrentProvider"));
        currentPlan.put ("planId", findPlanDetails 
									(client, currentPlan.get ("provider"), null, "planId"));
        currentPlan.put ("planValue", findPlanDetails 
										(client, currentPlan.get ("provider"), null, "planValue"));

		return currentPlan;
	}

	private static void dumpCurrentPlan (Map<String, String> currentPlan) {
        log.debug ("currentProvider obtained is {}", currentPlan.get("provider"));
        log.debug ("current Plan Id obtained is {}", currentPlan.get("planId"));
        log.debug ("current Plan Value obtained is {}", currentPlan.get("planValue"));
	}

	private static Map<String, String> getAdvisorDetails (JsonObject jsonObject) {

        Map<String, String> advisorDetails = new HashMap<>();

		advisorDetails.put ("name", getAdvisorDetail(jsonObject, "name"));
		advisorDetails.put ("phone", getAdvisorDetail(jsonObject, "phone"));
		advisorDetails.put ("id", getAdvisorDetail(jsonObject, "id"));
		advisorDetails.put ("email", getAdvisorDetail(jsonObject, "email"));


		return advisorDetails;
	}

	private static void dumpAdvisorDetails (Map<String, String> advisorDetails) {
		log.debug ("Advisor Name is {}", advisorDetails.get("name"));
		log.debug ("Advisor id is {}", advisorDetails.get("id"));
		log.debug ("Advisor phone is {}", advisorDetails.get("phone"));
		log.debug ("Advisor email is {}", advisorDetails.get("email"));
	}

    private static String findParentKey(JsonObject jsonObject, String targetKey, String targetValue, String parentKey) {
        for (String key : jsonObject.keySet()) {
			log.debug ("Key in findParentKey  is {}", key);
			log.debug ("Parent Key in findParentKey  is {}", parentKey);
            JsonElement value = jsonObject.get(key);


			if (key.equals(targetKey) && value.isJsonPrimitive() && value.getAsString().equals(targetValue)){
                return parentKey; // Return the parent key
            }

            if (value.isJsonObject()) {
                String result = findParentKey(value.getAsJsonObject(), targetKey, targetValue, key); // Update parentKey
                if (result != null) {
                    return result; // Propagate the result up
                }
            }
        }
        return null; // Key not found
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



    public static String findPlanDetails(JsonObject jsonObject, String targetProvider, String parentKey, String planId) {
        for (String key : jsonObject.keySet()) {
            JsonElement value = jsonObject.get(key);

            // Check if the key is "provider" and its value matches the target provider
            if (key.equals("provider") && value.isJsonPrimitive() && value.getAsString().equals(targetProvider)) {
                return jsonObject.get(planId).getAsString(); // Return the associated planId
            }

            // If the value is a nested object, continue searching recursively
            if (value.isJsonObject()) {
                String result = findPlanDetails(value.getAsJsonObject(), targetProvider, key, planId);
                if (result != null) {
                    return result; // Propagate the result up
                }
            }

            // If the value is an array, search each element recursively
            if (value.isJsonArray()) {
                for (JsonElement element : value.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        String result = findPlanDetails(element.getAsJsonObject(), targetProvider, key, planId);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null; // Target provider not found
    }



    public static String getAdvisorDetail(JsonObject jsonObject, String strToGet) {
        // Check if "advisorDetails" exists and contains "name"
        if (jsonObject.has("advisorDetails")) {
            JsonObject advisorDetails = jsonObject.getAsJsonObject("advisorDetails");
            if (advisorDetails.has(strToGet)) {
                return advisorDetails.get(strToGet).getAsString(); // Return the name
            }
        }
        return null; // Return null if "advisorDetails" or "name" is missing
    }


    public static String getFeeDetails(JsonObject jsonObject, String planOfInterest) {

		String feeDetails = "";

        // Navigate to the 'platforms' -> 'items' array
        JsonArray platformsArray = jsonObject
            .getAsJsonObject("organizationInfo")
            .getAsJsonObject("platforms")
            .getAsJsonArray("items");

        // Iterate through the platforms to find "Aviva Platform"
        for (JsonElement platformElement : platformsArray) {
            JsonObject platform = platformElement.getAsJsonObject();
            String platformName = platform.get("name").getAsString();

            if (planOfInterest.equals(platformName)) {
                // Extract the fees information
                JsonObject fees = platform.getAsJsonObject("fees");
                JsonArray structuredCharge = fees.getAsJsonArray("structuredCharge");

                // Print the fees
                System.out.println("Fees for " + platformName + ":");
                for (JsonElement feeElement : structuredCharge) {
                    JsonObject fee = feeElement.getAsJsonObject();
                    String startAmount = fee.get("startAmount").getAsString();
                    String endAmount = fee.get("endAmount") == null ? "null" : fee.get("endAmount").getAsString();
                    String feePercentage = fee.get("fee").getAsString();

					feeDetails = feeDetails + startAmount + ", " + endAmount + ", " + feePercentage + "\n";
					
                    System.out.println("Start: " + startAmount + ", End: " + endAmount + ", Fee: " + feePercentage);
                }
                break;  // Exit the loop after finding the platform
            }
        }

		return feeDetails;
	}


	public static String getAnnualMaintenanceFee (JsonObject jsonObject, String fund) {

		String chargePercentage = "";
        // Navigate to the 'serviceProposition' -> 'investmentPortfolios' array
        JsonArray portfoliosArray = jsonObject
            .getAsJsonObject("organizationInfo")
            .getAsJsonObject("serviceProposition")
            .getAsJsonArray("investmentPortfolios");

        // Iterate through the portfolios to find "Aggressive Growth Portfolio"
        for (JsonElement portfolioElement : portfoliosArray) {
            JsonObject portfolio = portfolioElement.getAsJsonObject();
            String portfolioName = portfolio.get("name").getAsString();

            if (fund.equals(portfolioName)) {
                // Extract the annual management charge
                JsonObject annualManagementCharge = portfolio.getAsJsonObject("annualManagementCharge");
                chargePercentage = annualManagementCharge.get("percentage").getAsString();
                break;  // Exit the loop after finding the portfolio
            }
        }

		return chargePercentage;
    }

	public static String getMinimumInvestment (JsonObject jsonObject, String fund) {

		String minInvestment = "";
        // Navigate to the 'serviceProposition' -> 'investmentPortfolios' array
        JsonArray portfoliosArray = jsonObject
            .getAsJsonObject("organizationInfo")
            .getAsJsonObject("serviceProposition")
            .getAsJsonArray("investmentPortfolios");

        // Iterate through the portfolios to find "Aggressive Growth Portfolio"
        for (JsonElement portfolioElement : portfoliosArray) {
            JsonObject portfolio = portfolioElement.getAsJsonObject();
            minInvestment = portfolio.get("minimumInvestment").getAsString();
            String portfolioName = portfolio.get("name").getAsString();

            if (fund.equals(portfolioName)) {
                break;  // Exit the loop after finding the portfolio
            }
        }

		return minInvestment;
    }

}
