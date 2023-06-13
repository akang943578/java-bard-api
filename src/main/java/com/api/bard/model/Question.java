package com.api.bard.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class Question {

    @NonNull
    private String question;
}
