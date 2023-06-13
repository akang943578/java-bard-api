package com.api.bard;

import com.api.bard.model.Answer;
import com.api.bard.model.Question;
import org.apache.hc.client5.http.config.RequestConfig;
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
        IBardClient bardClient = BardClient.builder(token).build();

        // Simplest way to get answer
        Answer answer = bardClient.getAnswer("Who is current president of USA?");
        Assertions.assertNotNull(answer.getAnswer());

        // Get answer with Question object
        Answer answer2 = bardClient.getAnswer(
            Question.builder()
                .question("Who is his wife?")
                .build());
        Assertions.assertNotNull(answer2.getAnswer());

        // Reset session
        bardClient.reset();

        Answer answer3 = bardClient.getAnswer("Who is his wife?");
        Assertions.assertNotNull(answer3.getAnswer());
    }

    /**
     * Advanced usage: set custom http headers and request config
     */
    @Test
    public void testGetAnswer_customClient() throws URISyntaxException {
        // set custom headers
        Map<String, String> headers = new HashMap<>();
        headers.put("TestHeader", "TestValue");
        headers.put("TestHeader2", "TestValue2");

        // set custom request config
        RequestConfig requestConfig = RequestConfig.custom()
            // set timeout
            .setConnectTimeout(Timeout.of(20, TimeUnit.SECONDS))
            .setResponseTimeout(Timeout.of(20, TimeUnit.SECONDS))
            // set other options in requestConfig...
            .build();

        IBardClient bardClient = BardClient.builder(token)
            .headers(headers).requestConfig(requestConfig).build();


        Answer answer = bardClient.getAnswer("누구세요");
        Assertions.assertNotNull(answer.getAnswer());

        Answer answer2 = bardClient.getAnswer(Question.builder().question("あなたの名前は何ですか").build());
        Assertions.assertNotNull(answer2.getAnswer());
    }
}
