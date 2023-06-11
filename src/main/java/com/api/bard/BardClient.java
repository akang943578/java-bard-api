package com.api.bard;

import com.api.bard.exception.BardApiException;
import com.api.bard.model.Answer;
import com.api.bard.model.Question;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Data
public class BardClient {

    private static final String HOST = "bard.google.com";
    private static final String URL = "https://bard.google.com";
    private static final String STREAM_GENERATE_URL =
        URL + "/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate";
    private static final String X_SAME_DOMAIN = "1";
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded;charset=UTF-8";

    private String token;
    private Map<String, String> headers;
    private String snim0e;

    private int reqid = Integer.parseInt(String.format("%04d", new Random().nextInt(10000)));
    private URLCodec urlCodec = new URLCodec();
    private Gson gson = new Gson();
    private RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(Timeout.of(30, TimeUnit.SECONDS))
        .setResponseTimeout(Timeout.of(30, TimeUnit.SECONDS))
        .build();

    public BardClient(@NonNull String token) {
        this.token = token;
        this.snim0e = fetchSNlM0e();
    }

    public BardClient(@NonNull String token,
                      @NonNull Map<String, String> headers) {
        this.token = token;
        this.headers = headers;
        this.snim0e = fetchSNlM0e();
    }

    public BardClient(@NonNull String token,
                      @NonNull RequestConfig requestConfig) {
        this.token = token;
        this.requestConfig = requestConfig;
        this.snim0e = fetchSNlM0e();
    }

    public BardClient(@NonNull String token,
                      @NonNull Map<String, String> headers,
                      @NonNull RequestConfig requestConfig) {
        this.token = token;
        this.headers = headers;
        this.requestConfig = requestConfig;
        this.snim0e = fetchSNlM0e();
    }

    @Data
    @Builder
    private static class BardResponse {
        private int code;
        private String content;
    }

    public Answer getAnswer(String question) throws BardApiException {
        return getAnswer(Question.builder().question(question).build());
    }

    public Answer getAnswer(Question question) throws BardApiException {
        if (question == null || question.getQuestion().isEmpty()) {
            log.error("Question is null or empty");
            throw new IllegalArgumentException("Question is null or empty");
        }

        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("bl", "boq_assistant-bard-web-server_20230419.00_p1");
            params.put("_reqid", String.valueOf(reqid));
            params.put("rt", "c");

            String fReq = String.format(
                "[null,\"[[\\\"%s\\\"],null,[\\\"%s\\\",\\\"%s\\\",\\\"%s\\\"]]\"]",
                question.getQuestion(), question.getConversationId(),
                question.getResponseId(), question.getChoiceId());

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

            String response = bardResponse.getContent();
            String[] responseLines = response.split("\n");
            String jsonLine = responseLines[3];

            return parseBardResult(jsonLine);
        } catch (Exception e) {
            log.error("Response Error, exception thrown. question: {}", question, e);
            throw new BardApiException("Response Error, exception thrown. question: " + question, e);
        }
    }

    private String fetchSNlM0e() {
        if (token == null || !token.endsWith(".")) {
            throw new IllegalArgumentException("token must end with a single dot. Enter correct __Secure-1PSID value.");
        }

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
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
        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
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

    private Answer parseBardResult(String rawResult) {
        String usefulResult =
            gson.fromJson(rawResult, JsonArray.class).get(0).getAsJsonArray().get(2).getAsString();

        JsonArray jsonElements = gson.fromJson(usefulResult, JsonArray.class);
        String content = jsonElements.get(0).getAsJsonArray().get(0).getAsString();
        String conversationId = jsonElements.get(1).getAsJsonArray().get(0).getAsString();
        String responseId = jsonElements.get(1).getAsJsonArray().get(1).getAsString();

        List<String> factualityQueries = jsonElements.get(3)
            .getAsJsonArray().asList().stream()
            .map(JsonElement::getAsString)
            .collect(Collectors.toList());
        String textQuery = jsonElements.get(2).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();
        List<Answer.Choice> choices = jsonElements.get(4).getAsJsonArray().asList().stream()
            .map(x -> {
                JsonArray jsonArray = x.getAsJsonArray();
                return Answer.Choice.builder()
                    .id(jsonArray.get(0).getAsString())
                    .content(jsonArray.get(1).getAsString())
                    .build();
            })
            .collect(Collectors.toList());

        return Answer.builder()
            .answer(content)
            .conversationId(conversationId)
            .responseId(responseId)
            .factualityQueries(factualityQueries)
            .textQuery(textQuery)
            .choices(choices)
            .build();
    }
}
