package com.api.bard;

import com.api.bard.exception.BardApiException;
import com.api.bard.model.Answer;
import com.api.bard.model.Question;

public interface IBardClient {

    default Answer getAnswer(String question) {
        return getAnswer(Question.builder().question(question).build());
    }

    Answer getAnswer(Question question) throws BardApiException;

    void reset() throws BardApiException;
}
