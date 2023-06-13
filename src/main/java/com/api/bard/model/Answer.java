package com.api.bard.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Answer {

    private String answer;
    private String conversationId;
    private String responseId;
    private String choiceId;
    private List<String> factualityQueries;
    private String textQuery;
    private List<Choice> choices;

    @Data
    @Builder
    public static class Choice {
        private String id;
        private String content;
    }
}
