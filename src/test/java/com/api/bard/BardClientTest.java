package com.api.bard;

import com.api.bard.model.Answer;
import com.api.bard.model.Question;
import com.api.bard.translator.GoogleTranslatorProxy;
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

    /**
     * Advanced usage: set translator to support languages other than English, Japanese or Korean
     */
    @Test
    public void testGetAnswer_withTranslator() {
        IBardClient bardClient = BardClient.builder(token)
            // Default middleLanguage is 'en'
            .translator(new GoogleTranslatorProxy())
            .build();

        Answer answer = bardClient.getAnswer("누구세요");
        Assertions.assertNotNull(answer.getAnswer());
        // Korean is supported by Bard, so it should not use translator even set
        Assertions.assertFalse(answer.isUsedTranslator());

        Answer answer2 = bardClient.getAnswer(Question.builder().question("あなたの名前は何ですか").build());
        Assertions.assertNotNull(answer2.getAnswer());
        // Japanese is supported by Bard, so it should not use translator even set
        Assertions.assertFalse(answer2.isUsedTranslator());

        Answer answer3 = bardClient.getAnswer(Question.builder().question("你是谁？").build());
        Assertions.assertNotNull(answer3.getAnswer());
        // Chinese is not supported by Bard, so it should use the translator set, which middleLanguage is English
        // This means the question is translated to English before interact with Bard, thus the answer is also in English from Bard
        // And it will also translate the answer to Chinese before return
        Assertions.assertTrue(answer3.isUsedTranslator());

        IBardClient bardClient2 = BardClient.builder(token)
            // You can set other middleLanguage which supported by Bard, such as 'ja'
            .translator(new GoogleTranslatorProxy("ja"))
            .build();

        Answer answer4 = bardClient2.getAnswer("How are you?");
        Assertions.assertNotNull(answer4.getAnswer());
        // English is supported by Bard, so it should not use translator even set
        Assertions.assertFalse(answer4.isUsedTranslator());

        Answer answer5 = bardClient2.getAnswer(Question.builder().question("你是谁？").build());
        Assertions.assertNotNull(answer5.getAnswer());
        // Chinese is not supported by Bard, so it should use the translator set, which middleLanguage is Japanese
        // This means the question is translated to Japanese before interact with Bard, thus the answer is also in Japanese from Bard
        // And it will also translate the answer to Chinese before return
        Assertions.assertTrue(answer5.isUsedTranslator());
    }

    /**
     * Advanced usage: use advanced fields to get more information
     * such as images, sources, relatedTopics, and raw response from google bard
     */
    @Test
    public void testGetAnswer_withAdvancedFields() {
        IBardClient bardClient = BardClient.builder(token).build();

        Answer answer = bardClient.getAnswer("Give me a picture of White House");
        Assertions.assertNotNull(answer.getAnswer());
        // Korean is supported by Bard, so it should not use translator even set
        Assertions.assertFalse(answer.isUsedTranslator());

        // verification of images/sources/relatedTopics in response
        Assertions.assertEquals(answer.getImages().size(), 1);
        Assertions.assertTrue(answer.getSources().size() > 0);
        Assertions.assertTrue(answer.getRelatedTopics().size() > 0);

        // raw response from google bard, you can parse it by yourself
        Assertions.assertNotNull(answer.getRawResponse());

        // If images are available, get the decorated answer with images in markdown format
        String markdownAnswer = answer.getMarkdownAnswer();
        Assertions.assertNotNull(markdownAnswer);
    }
}
