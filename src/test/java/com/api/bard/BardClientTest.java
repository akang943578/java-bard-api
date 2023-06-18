package com.api.bard;

import com.api.bard.model.Answer;
import com.api.bard.model.Question;
import com.api.bard.translator.GoogleTranslatorAdaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class BardClientTest {
    private String token;
    private String authUser;
    private String authPassword;

    @BeforeEach
    public void setup() {
        token = System.getenv("_BARD_API_KEY");
        authUser = System.getenv("authUser");
        authPassword = System.getenv("authPassword");
        Assertions.assertNotNull(token);
        Assertions.assertNotNull(authUser);
        Assertions.assertNotNull(authPassword);
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
     * Advanced usage: customize connection properties,
     * such as set custom http headers and timeout properties
     */
    @Test
    public void testGetAnswer_customConnection() {
        IBardClient bardClient = BardClient.builder(token)
            // set configurator to customize connection properties,
            // such as timeout, headers
            .connectionConfigurator(connection -> {
                // set timeout
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(50000);

                //set customs headers
                connection.setRequestProperty("TestHeader", "TestValue");
                connection.setRequestProperty("TestHeader2", "TestValue2");

                // ... set others properties of connection
            })
            .build();

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
            .translator(GoogleTranslatorAdaptor.builder().build())
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
            .translator(GoogleTranslatorAdaptor.builder().middleLanguage("ja").build())
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

    /**
     * Advanced usage: set proxy if you can not access bard.google.com directly
     * Tested in China, http/socks5 proxy is supported. Others are not tested.
     */
    @Test
    public void test_withProxy() {
        // Set Http proxy
        IBardClient bardClient = BardClient.builder(token)
            .proxy(new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress("192.168.31.1", 7890)))
            // Set authUser and authPassword if proxy needs authentication
            .auth(authUser, authPassword)
            .build();

        Answer answer = bardClient.getAnswer("Give me a picture of White House");
        Assertions.assertNotNull(answer.getAnswer());
        Assertions.assertFalse(answer.isUsedTranslator());

        // Set Socks5 proxy
        Proxy proxy = new Proxy(Proxy.Type.SOCKS,
            new InetSocketAddress("192.168.31.1", 7890));
        IBardClient bardClient2 = BardClient.builder(token)
            // Set authUser and authPassword if proxy needs authentication
            .proxy(proxy)
            .auth(authUser, authPassword)
            // Note that if you need to set translator, you should set proxy for translator as well
            .translator(GoogleTranslatorAdaptor.builder()
                .proxy(proxy)
                .auth(authUser, authPassword)
                .build())
            .build();

        Answer answer2 = bardClient2.getAnswer("今天是星期几?");
        Assertions.assertNotNull(answer2.getAnswer());
        Assertions.assertTrue(answer2.isUsedTranslator());
    }
}
