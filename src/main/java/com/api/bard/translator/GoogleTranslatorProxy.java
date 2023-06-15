package com.api.bard.translator;

import com.api.bard.exception.BardTranslateException;
import com.dark.programs.speech.translator.GoogleTranslate;

import java.io.IOException;

public class GoogleTranslatorProxy implements IBardTranslator {
    private static final String DEFAULT_MIDDLE_LANGUAGE = "en";

    private final String middleLanguage;

    public GoogleTranslatorProxy() {
        this(DEFAULT_MIDDLE_LANGUAGE);
    }

    public GoogleTranslatorProxy(String middleLanguage) {
        this.middleLanguage = middleLanguage;
    }

    @Override
    public String middleLanguage() {
        return middleLanguage;
    }

    @Override
    public String detectLanguage(String rawText) {
        try {
            return GoogleTranslate.detectLanguage(rawText);
        } catch (IOException e) {
            throw new BardTranslateException("GoogleTranslate detectLanguage failed, " +
                "rawText:{}" + rawText, e);
        }
    }

    @Override
    public String translate(String sourceLang, String targetLang, String rawText) {
        try {
            return GoogleTranslate.translate(sourceLang, targetLang, rawText);
        } catch (IOException e) {
            throw new BardTranslateException("GoogleTranslate translate error, " +
                "sourceLang: " + sourceLang + ", targetLang: " + targetLang + ", rawText:{}" + rawText, e);
        }
    }
}
