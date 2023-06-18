package com.api.bard;

import com.api.bard.exception.BardApiException;
import com.api.bard.model.Answer;
import com.api.bard.model.Question;
import com.api.bard.translator.IBardTranslator;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class BardClient implements IBardClient {
    private static final String HOST = "bard.google.com";
    private static final String BARD_URL = "https://bard.google.com";
    private static final String STREAM_GENERATE_URL =
        BARD_URL + "/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate";
    private static final String X_SAME_DOMAIN = "1";
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded;charset=UTF-8";

    /**
     * Proxy to use when making requests
     */
    private Proxy proxy = Proxy.NO_PROXY;

    private String token;
    private String snim0e = "";
    private String conversationId = "";
    private String responseId = "";
    private String choiceId = "";

    private IBardTranslator translator;
    private Consumer<HttpURLConnection> connectionConfigurator;

    private int reqid = Integer.parseInt(String.format("%04d", new Random().nextInt(10000)));
    private Gson gson = new Gson();

    private BardClient(String token) {
        this.token = token;
    }

    public static BardClientBuilder builder(@NonNull String token) {
        return new BardClientBuilder(token);
    }

    public static class BardClientBuilder {
        private final BardClient bardClient;

        private BardClientBuilder(String token) {
            bardClient = new BardClient(token);
        }

        /**
         * Builder of Proxy to use when making requests
         *
         * @param proxy proxy to use when making requests
         */
        public BardClientBuilder proxy(Proxy proxy) {
            bardClient.proxy = proxy;
            return this;
        }

        /**
         * Builder of Authentication for the proxy
         *
         * @param authUser     authUser
         * @param authPassword authPassword
         */
        public BardClientBuilder auth(String authUser, String authPassword) {
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
            Authenticator.setDefault(
                new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(authUser, authPassword.toCharArray());
                    }
                }
            );
            return this;
        }

        public BardClientBuilder connectionConfigurator(Consumer<HttpURLConnection> connectionConfigurator) {
            bardClient.connectionConfigurator = connectionConfigurator;
            return this;
        }

        public BardClientBuilder translator(IBardTranslator translator) {
            bardClient.translator = translator;
            return this;
        }

        public BardClient build() {
            return bardClient;
        }
    }

    @Data
    @Builder
    private static class BardResponse {
        private int code;
        private String content;
    }

    @Override
    public Answer getAnswer(Question question) throws BardApiException {
        if (question == null || question.getQuestion().isEmpty()) {
            log.error("Question is null or empty");
            throw new IllegalArgumentException("Question is null or empty");
        }

        try {
            if (snim0e == null || snim0e.isEmpty()) {
                this.snim0e = fetchSNlM0e();
            }

            String questionInput = question.getQuestion();

            boolean needTranslate = false;
            String sourceLang = null;
            if (translator != null) {
                sourceLang = translator.detectLanguage(questionInput);
                if (!IBardTranslator.SUPPORTED_LANGUAGES.contains(sourceLang)) {
                    needTranslate = true;
                    questionInput = translator.translate(sourceLang, translator.middleLanguage(), questionInput);
                }
            }

            Map<String, String> params = new LinkedHashMap<>();
            params.put("bl", "boq_assistant-bard-web-server_20230419.00_p1");
            params.put("_reqid", String.valueOf(reqid));
            params.put("rt", "c");

            String fReq = String.format(
                "[null,\"[[\\\"%s\\\"],null,[\\\"%s\\\",\\\"%s\\\",\\\"%s\\\"]]\"]",
                questionInput, conversationId, responseId, choiceId);

            Map<String, String> data = new LinkedHashMap<>();
            data.put("f.req", fReq);
            data.put("at", snim0e);

            BardResponse bardResponse = sendPostRequest(STREAM_GENERATE_URL, params, data);

            if (bardResponse == null) {
                log.error("Response Error, bard response is null");
                throw new BardApiException("Response Error, bard response is null");
            }
            if (bardResponse.getCode() / 100 != 2) {
                throw new BardApiException("Response Error, bard response code: " + bardResponse.getCode());
            }

            Answer answer = parseBardResult(bardResponse.getContent());
            String answerOutput = answer.getAnswer();
            if (needTranslate) {
                answerOutput = translator.translate(translator.middleLanguage(), sourceLang, answerOutput);
                answer.setAnswer(answerOutput);
                answer.setUsedTranslator(true);
            }

            this.conversationId = answer.getConversationId();
            this.responseId = answer.getResponseId();
            this.choiceId = answer.getChoices().get(0).getId();

            return answer;
        } catch (Exception e) {
            log.error("Response Error, exception thrown. question: {}", question, e);
            throw new BardApiException("Response Error, exception thrown. question: " + question, e);
        }
    }

    @Override
    public void reset() throws BardApiException {
        snim0e = "";
        conversationId = "";
        responseId = "";
        choiceId = "";
    }

    private String fetchSNlM0e() {
        if (token == null || !token.endsWith(".")) {
            throw new IllegalArgumentException("token must end with a single dot. Enter correct __Secure-1PSID value.");
        }

        try {
            URL url = new URL(BARD_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            connection.setRequestMethod("GET");
            addHeaders(connection);
            if (connectionConfigurator != null) {
                connectionConfigurator.accept(connection);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new BardApiException("Response code not 200. Response Status is " + responseCode);
            }

            InputStream inputStream = connection.getInputStream();
            String responseBody = convertStreamToString(inputStream);

            return extractSNlM0e(responseBody);
        } catch (IOException e) {
            log.error("fetchSNlM0e error", e);
            throw new BardApiException("fetchSNlM0e error", e);
        }
    }

    private void addHeaders(HttpURLConnection connection) {
        // Set headers
        connection.setRequestProperty("Host", HOST);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Referer", BARD_URL);
        connection.setRequestProperty("X-Same-Domain", X_SAME_DOMAIN);
        connection.setRequestProperty("Content-Type", CONTENT_TYPE);
        connection.setRequestProperty("Origin", BARD_URL);
        connection.setRequestProperty("Cookie", "__Secure-1PSID=" + token);
    }

    private String convertStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        reader.close();
        return stringBuilder.toString();
    }


    private String extractSNlM0e(String response) {
        String pattern = "SNlM0e\":\"(.*?)\"";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new BardApiException("SNlM0e value not found in response. Check __Secure-1PSID value.");
    }

    private BardResponse sendPostRequest(String url, Map<String, String> params, Map<String, String> data)
        throws IOException {
        // Build query parameters
        StringBuilder queryParameters = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            queryParameters.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                .append("&");
        }

        // Create the URL
        URL postUrl = new URL(url + "?" + queryParameters);

        // Open a connection
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection(proxy);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        addHeaders(connection);
        if (connectionConfigurator != null) {
            connectionConfigurator.accept(connection);
        }

        // Set request body
        StringBuilder requestBody = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            requestBody.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                .append("&");
        }

        // Send the request
        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] requestBodyBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            outputStream.write(requestBodyBytes);
        }

        // Process the response
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            String responseBody = convertStreamToString(inputStream);

            return BardResponse.builder()
                .code(responseCode)
                .content(responseBody)
                .build();
        }

        throw new BardApiException("Response code: " + responseCode);
    }

    private Answer parseBardResult(String rawResponse) {
        String[] responseLines = rawResponse.split("\n");
        String rawResult = responseLines[3];

        String usefulResult =
            gson.fromJson(rawResult, JsonArray.class).get(0).getAsJsonArray().get(2).getAsString();

        JsonArray jsonElements = gson.fromJson(usefulResult, JsonArray.class);
        String content = jsonElements.get(0).getAsJsonArray().get(0).getAsString();
        String conversationId = jsonElements.get(1).getAsJsonArray().get(0).getAsString();
        String responseId = jsonElements.get(1).getAsJsonArray().get(1).getAsString();

        List<String> factualityQueries = null;
        try {
            factualityQueries = jsonElements.get(3)
                .getAsJsonArray().asList().stream()
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // pass
        }
        String textQuery = null;
        try {
            textQuery = jsonElements.get(2).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();
        } catch (Exception e) {
            // pass
        }
        List<Answer.Choice> choices = null;
        try {
            choices = jsonElements.get(4).getAsJsonArray().asList().stream()
                .map(x -> {
                    JsonArray jsonArray = x.getAsJsonArray();
                    return Answer.Choice.builder()
                        .id(jsonArray.get(0).getAsString())
                        .content(jsonArray.get(1).getAsString())
                        .build();
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            // pass
        }

        List<Answer.Image> images = null;
        try {
            images = new ArrayList<>();
            JsonArray imagesJson = jsonElements.get(4).getAsJsonArray().get(0).getAsJsonArray().get(4).getAsJsonArray();

            for (int i = 0; i < imagesJson.size(); i++) {
                JsonArray imageJson = imagesJson.get(i).getAsJsonArray();
                String url = imageJson.get(0).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();
                String markdownLabel = imageJson.get(2).getAsString();
                String articleURL = imageJson.get(1).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();

                Answer.Image image = Answer.Image.builder()
                    .imageUrl(url)
                    .imageMarker(markdownLabel)
                    .detailsLink(articleURL)
                    .build();
                images.add(image);
            }
        } catch (Exception e) {
            //pass
        }

        List<Answer.Source> sources = null;
        try {
            sources = new ArrayList<>();
            JsonArray sourceArray = jsonElements.get(3).getAsJsonArray().get(0).getAsJsonArray();

            for (int i = 0; i < sourceArray.size(); i++) {
                JsonArray imageJson = sourceArray.get(i).getAsJsonArray();
                int startIndexInAnswer = imageJson.get(0).getAsInt();
                int endIndexInAnswer = imageJson.get(1).getAsInt();
                String source = imageJson.get(2).getAsJsonArray().get(0).getAsString();

                Answer.Source sourceObj = Answer.Source.builder()
                    .startIndexInAnswer(startIndexInAnswer)
                    .endIndexInAnswer(endIndexInAnswer)
                    .rawContentInAnswer(content.substring(startIndexInAnswer, endIndexInAnswer))
                    .sourceLink(source)
                    .build();
                sources.add(sourceObj);
            }
        } catch (Exception e) {
            //pass
        }

        List<Answer.RelatedTopic> relatedTopics = null;
        try {
            relatedTopics = new ArrayList<>();
            JsonArray imagesJson = jsonElements.get(2).getAsJsonArray();

            for (int i = 0; i < imagesJson.size(); i++) {
                JsonArray imageJson = imagesJson.get(i).getAsJsonArray();
                String topic = imageJson.get(0).getAsString();
                int num = imageJson.get(1).getAsInt();

                Answer.RelatedTopic relatedTopic = Answer.RelatedTopic.builder()
                    .topic(topic)
                    .num(num)
                    .build();

                relatedTopics.add(relatedTopic);
            }
        } catch (Exception e) {
            //pass
        }

        return Answer.builder()
            .rawResponse(rawResponse)
            .answer(content)
            .conversationId(conversationId)
            .responseId(responseId)
            .choiceId(choices.get(0).getId())
            .factualityQueries(factualityQueries)
            .textQuery(textQuery)
            .choices(choices)
            .images(images)
            .sources(sources)
            .relatedTopics(relatedTopics)
            .build();
    }
}
