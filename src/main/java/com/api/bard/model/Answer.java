package com.api.bard.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Answer {

    // raw response from google bard, you can parse it by yourself
    private String rawResponse;

    // parsed answer in string
    private String answer;
    private String conversationId;
    private String responseId;
    private String choiceId;
    // if question/answer was translated before/after interact with google bard or not
    private boolean usedTranslator;
    private List<String> factualityQueries;
    private String textQuery;
    // available choices for this question
    private List<Choice> choices;
    // potential images for this question
    private List<Image> images;
    // related sources
    private List<Source> sources;
    // related topics
    private List<RelatedTopic> relatedTopics;

    @Data
    @Builder
    public static class Choice {
        private String id;
        private String content;
    }

    @Data
    @Builder
    public static class Image {
        private final String imageUrl;
        private final String imageMarker;
        private final String detailsLink;

        public String decorateMarkdown(String rawAnswer) {
            return rawAnswer.replaceFirst(
                String.format("\\[%s\\]", imageMarker.substring(1, imageMarker.length() - 1)),
                String.format("[!%s(%s)](%s)", imageMarker, imageUrl, detailsLink));
        }
    }

    @Data
    @Builder
    public static class Source {
        private int startIndexInAnswer;
        private int endIndexInAnswer;
        private String rawContentInAnswer;
        private String sourceLink;
    }

    @Data
    @Builder
    public static class RelatedTopic {
        private String topic;
        private int num;
    }

    // If images are available, get the decorated answer with images in markdown format
    public String getMarkdownAnswer() {
        String markdownAnswer = this.answer;
        if (images != null && images.size() > 0) {
            for (Image image : images) {
                markdownAnswer = image.decorateMarkdown(markdownAnswer);
            }
        }
        return markdownAnswer;
    }
}
