package com.api.bard.translator;

import java.util.Arrays;
import java.util.List;

public interface IBardTranslator {
    List<String> SUPPORTED_LANGUAGES = Arrays.asList("en", "ja", "ko");

    String middleLanguage();

    String detectLanguage(String rawText);

    String translate(String sourceLang, String targetLang, String rawText);
}
