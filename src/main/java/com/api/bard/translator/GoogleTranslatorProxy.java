package com.api.bard.translator;

import com.api.bard.exception.BardTranslateException;
import com.dark.programs.speech.translator.GoogleTranslate;
import lombok.NonNull;

import java.io.IOException;
import java.net.Proxy;

public class GoogleTranslatorProxy implements IBardTranslator {
    private static final String DEFAULT_MIDDLE_LANGUAGE = "en";

    private String middleLanguage;

    private GoogleTranslatorProxy() {
        this.middleLanguage = DEFAULT_MIDDLE_LANGUAGE;
    }

    public static Builder builder() {
        return new GoogleTranslatorProxy.Builder();
    }

    public static class Builder {
        private final GoogleTranslatorProxy googleTranslatorProxy;

        private Builder() {
            googleTranslatorProxy = new GoogleTranslatorProxy();
        }

        public Builder middleLanguage(String middleLanguage) {
            googleTranslatorProxy.middleLanguage = middleLanguage;
            return this;
        }

        public Builder proxy(@NonNull Proxy proxy) {
            GoogleTranslate.setProxy(proxy);
            return this;
        }

        public Builder auth(String authUser, String authPassword) {
            GoogleTranslate.setAuth(authUser, authPassword);
            return this;
        }

        public GoogleTranslatorProxy build() {
            return googleTranslatorProxy;
        }
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
