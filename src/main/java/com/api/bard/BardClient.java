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
import org.apache.commons.codec.net.URLCodec;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class BardClient implements IBardClient {
    private static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom()
        .setConnectTimeout(Timeout.of(30, TimeUnit.SECONDS))
        .setResponseTimeout(Timeout.of(30, TimeUnit.SECONDS))
        .build();

    private static final String HOST = "bard.google.com";
    private static final String URL = "https://bard.google.com";
    private static final String STREAM_GENERATE_URL =
        URL + "/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate";
    private static final String X_SAME_DOMAIN = "1";
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded;charset=UTF-8";

    private String token;
    private String snim0e = "";
    private String conversationId = "";
    private String responseId = "";
    private String choiceId = "";

    private IBardTranslator translator;
    private Map<String, String> headers;
    private RequestConfig requestConfig;

    private int reqid = Integer.parseInt(String.format("%04d", new Random().nextInt(10000)));
    private URLCodec urlCodec = new URLCodec();
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

        public BardClientBuilder headers(Map<String, String> headers) {
            bardClient.headers = headers;
            return this;
        }

        public BardClientBuilder requestConfig(RequestConfig requestConfig) {
            bardClient.requestConfig = requestConfig == null ? DEFAULT_REQUEST_CONFIG : requestConfig;
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

        try (CloseableHttpClient httpClient = buildHttpClient()) {
            HttpUriRequestBase httpGet = new HttpGet(URL);
            addHeaders(httpGet);

            ClassicHttpResponse response = httpClient.execute(httpGet);
            int responseCode = response.getCode();
            if (responseCode != 200) {
                throw new BardApiException("Response code not 200. Response Status is " + responseCode);
            }

            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            EntityUtils.consume(entity);

            return extractSNlM0e(responseBody);
        } catch (IOException | ParseException e) {
            log.error("fetchSNlM0e error", e);
            throw new BardApiException("fetchSNlM0e error", e);
        }
    }

    private CloseableHttpClient buildHttpClient() {
        return HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build();
    }

    private String extractSNlM0e(String response) throws HttpResponseException {
        String pattern = "SNlM0e\":\"(.*?)\"";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new BardApiException("SNlM0e value not found in response. Check __Secure-1PSID value.");
    }

    private BardResponse sendPostRequest(String url, Map<String, String> params, Map<String, String> data)
        throws IOException, ParseException {
        try (CloseableHttpClient httpClient = buildHttpClient()) {
            String queryParameters = buildPramsOrBody(params);
            HttpPost httpPost = new HttpPost(url + "?" + queryParameters);
            addHeaders(httpPost);

            // Set request body
            String requestBody = buildPramsOrBody(data);
            HttpEntity requestEntity = new StringEntity(requestBody, ContentType.APPLICATION_FORM_URLENCODED);
            httpPost.setEntity(requestEntity);

            // Send the request
            CloseableHttpResponse response = httpClient.execute(httpPost);
            // Process the response
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                EntityUtils.consume(entity);

                // Process the responseString as needed
                return BardResponse.builder()
                    .code(response.getCode())
                    .content(responseString)
                    .build();
            }
            throw new BardApiException("response entity is null");
        }
    }

    private void addHeaders(HttpUriRequestBase requestBase) {
        // Set headers
        requestBase.addHeader(HttpHeaders.HOST, HOST);
        requestBase.addHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        requestBase.addHeader(HttpHeaders.REFERER, URL);
        requestBase.addHeader("X-Same-Domain", X_SAME_DOMAIN);
        requestBase.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        requestBase.addHeader("Origin", URL);
        requestBase.addHeader("Cookie", "__Secure-1PSID=" + token);

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBase.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    private String buildPramsOrBody(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder queryParameters = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (queryParameters.length() != 0) {
                queryParameters.append("&");
            }
            queryParameters.append(entry.getKey());
            queryParameters.append("=");
            queryParameters.append(urlEncode(entry.getValue()));
        }
        return queryParameters.toString();
    }

    private String urlEncode(String originText) throws UnsupportedEncodingException {
        // URL encode
        String encodedString = urlCodec.encode(originText, StandardCharsets.UTF_8.name());
        encodedString = encodedString.replace("+", "%20");
        return encodedString;
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
                String articleURL =  imageJson.get(1).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();

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
