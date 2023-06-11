package com.api.bard;

import com.api.bard.model.Answer;
import com.api.bard.model.Question;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BardClientTest {
    private String token;

    @BeforeEach
    public void setup() {
        token = System.getenv("_BARD_API_KEY");
        Assertions.assertNotNull(token);
    }

    /**
     * Simple usage
     */
    @Test
    public void testGetAnswer_happyCase() {
        BardClient bardClient = new BardClient(token);

        // Simplest way to get answer
        Answer answer = bardClient.getAnswer("who are you");
        Assertions.assertNotNull(answer.getAnswer());

        // Get answer with more options, such as conversationId, responseId, choiceId
        Answer answer2 = bardClient.getAnswer(Question.builder()
            .responseId(answer.getResponseId())
            .conversationId(answer.getConversationId())
            .choiceId(answer.getChoices().get(0).getId())
            .question("what's your name").build());
        Assertions.assertNotNull(answer2.getAnswer());
    }

    /**
     * Advanced usage
     */
    @Test
    public void testGetAnswer_customClient() {
        // set custom headers
        Map<String, String> headers = new HashMap<>();
        headers.put("TestHeader", "TestValue");
        headers.put("TestHeader2", "TestValue2");

        // set custom request config
        RequestConfig requestConfig = RequestConfig.custom()
            // set timeout
            .setConnectTimeout(Timeout.of(20, TimeUnit.SECONDS))
            .setResponseTimeout(Timeout.of(20, TimeUnit.SECONDS))
            // set http proxy
//            .setProxy(HttpHost.create("http://localhost:8080"))
            // set other options in requestConfig...
            .build();

        BardClient bardClient = new BardClient(token, headers, requestConfig);

        Answer answer = bardClient.getAnswer("누구세요");
        Assertions.assertNotNull(answer.getAnswer());

        Answer answer2 = bardClient.getAnswer(Question.builder().question("あなたの名前は何ですか").build());
        Assertions.assertNotNull(answer2.getAnswer());
    }
}
