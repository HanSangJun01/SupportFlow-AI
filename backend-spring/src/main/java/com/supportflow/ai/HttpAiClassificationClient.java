package com.supportflow.ai;

import java.util.Objects;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class HttpAiClassificationClient implements AiClassificationClient {

    static final String CLASSIFICATION_PATH = "/api/v1/classifications/tickets";
    static final String UNAVAILABLE_ERROR_CODE = "AI_CLASSIFICATION_UNAVAILABLE";
    static final String INVALID_RESPONSE_ERROR_CODE = "AI_CLASSIFICATION_INVALID_RESPONSE";

    private final RestClient restClient;

    public HttpAiClassificationClient(RestClient.Builder restClientBuilder, AiClassificationProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeout());
        requestFactory.setReadTimeout(properties.getTimeout());
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public TicketClassificationResponse classify(TicketClassificationRequest request) {
        try {
            TicketClassificationResponse response = restClient.post()
                    .uri(CLASSIFICATION_PATH)
                    .body(request)
                    .retrieve()
                    .body(TicketClassificationResponse.class);
            validateResponse(response);
            return response;
        } catch (TicketClassificationException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new TicketClassificationException(
                    UNAVAILABLE_ERROR_CODE,
                    "AI classification service returned HTTP " + exception.getStatusCode().value(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new TicketClassificationException(
                    UNAVAILABLE_ERROR_CODE,
                    "AI classification service is unavailable",
                    exception
            );
        }
    }

    private void validateResponse(TicketClassificationResponse response) {
        if (response == null
                || isBlank(response.category())
                || response.urgency() == null
                || response.sentiment() == null
                || response.priority() == null
                || response.confidence() < 0.0
                || response.confidence() > 1.0
                || isBlank(response.classifierVersion())) {
            throw new TicketClassificationException(
                    INVALID_RESPONSE_ERROR_CODE,
                    "AI classification service returned an invalid response"
            );
        }
    }

    private boolean isBlank(String value) {
        return Objects.toString(value, "").isBlank();
    }
}
