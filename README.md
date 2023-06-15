
# Google <a href="https://bard.google.com/"><img src="https://camo.githubusercontent.com/adb54264fe2ad5067d07d0752fc32600b4e6250073b01ce8c386575b431e3f06/68747470733a2f2f7777772e677374617469632e636f6d2f6c616d64612f696d616765732f66617669636f6e5f76315f31353031363063646466663766323934636533302e737667" height="20px"></a> Java Bard API


> The java package that returns response of [Google Bard](https://bard.google.com/) through value of cookie.


**Please exercise caution and use this package responsibly.**

I referred to [this github repository(github.com/dsdanielpark/Bard-API)](https://github.com/dsdanielpark/Bard-API) where inference process of Bard was reverse engineered. Using `__Secure-1PSID`, you can ask questions and get answers from Google Bard. Please note that the bardapi is not a free service, but rather a tool provided to assist developers with testing certain functionalities due to the delayed development and release of Google Bard's API. It has been designed with a lightweight structure that can easily adapt to the emergence of an official API. This package is designed only for interest and learning purpose. Therefore, I strongly discourage using it for any other purposes.

<br>



##  [Amazing Bard Prompts](https://github.com/dsdanielpark/amazing-bard-prompts) Is All You Need!
- Helpful prompts for Google Bard

<br>

## Install
TODO: Install this pacakge to maven central repo.

Currently, it can be dependent by github maven repo.

1. Add github repo into your pom.xml `repositories` section:
```xml
  <repositories>
    <repository>
        <id>akang943578-maven-repo</id>
        <url>https://raw.githubusercontent.com/akang943578/maven-repo/master/repository</url>
    </repository>
  </repositories>
```
2. Add java-bard-api package into pom.xml `dependencies` section:
```xml
  <dependencies>
    <dependency>
      <groupId>com.api.bard</groupId>
      <artifactId>java-bard-api</artifactId>
      <version>1.2.0</version>
    </dependency>
  </dependencies>
```

3. Authentication
> **Warning** Do not expose the `__Secure-1PSID`
* Visit https://bard.google.com/
* F12 for console
* Session: Application → Cookies → Copy the value of  `__Secure-1PSID` cookie.

Note that while I referred to `__Secure-1PSID` value as an API key for convenience, it is not an officially provided API key.
Cookie value subject to frequent changes. Verify the value again if an error occurs. Most errors occur when an invalid cookie value is entered.

Set this cookie value in your profile, such as `~/.bash_profile` or `~/.zshrc` according to your shell.
```shell
export _BARD_API_KEY=XXXXXXXXX_*********************xxxxxxxxxyyyyyyyyyyyyyzz.
```

4. Then you can use it in your java code. (Note: perhaps you need to restart your IDE to apply the environment variable)
```java
import com.api.bard.BardClient;

public class BardClientMain {

    public static void main(String[] args) {
        String token = System.getenv("_BARD_API_KEY");
        IBardClient bardClient = BardClient.builder(token).build();

        String answer = bardClient.getAnswer("Who are you?").getAnswer();
        System.out.println(answer);
    }
}
```
It will show original response from Bard:
```
I am Bard, a large language model, also known as a conversational AI or chatbot trained to be informative and comprehensive. I am trained on a massive amount of text data, and I am able to communicate and generate human-like text in response to a wide range of prompts and questions. For example, I can provide summaries of factual topics or create stories.

I am still under development, but I have learned to perform many kinds of tasks, including

* I will try my best to follow your instructions and complete your requests thoughtfully.
* I will use my knowledge to answer your questions in a comprehensive and informative way, even if they are open ended, challenging, or strange.
* I will generate different creative text formats of text content, like poems, code, scripts, musical pieces, email, letters, etc. I will try my best to fulfill all your requirements.

I am not a person, and I do not have any feelings or emotions. I am a computer program, and my responses are based on the information that I have been trained on.

I am excited to see what the future holds for me, and I hope that I can continue to learn and grow. I also hope that I can be a helpful and informative resource for you.
```

5. If you need to set multiple Cookie values

- [Bard Cookies](https://github.com/dsdanielpark/Bard-API/blob/main/README_DEV.md#bard-which-can-get-cookies) - After confirming that multiple cookie values are required to receive responses reliably in certain countries, I will deploy it for testing purposes. Please debug and create a pull request


<br>

## Usage


```java
import com.api.bard.model.Answer;
import com.api.bard.model.Question;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BardClientTest {
    private String token;

    @BeforeEach
    public void setup() {
        token = System.getenv("_BARD_API_KEY");
        Assertions.assertNotNull(token);
    }

    /**
     * Simple usage
     */
    @Test
    public void testGetAnswer_happyCase() {
        IBardClient bardClient = BardClient.builder(token).build();

        // Simplest way to get answer
        Answer answer = bardClient.getAnswer("Who is current president of USA?");
        Assertions.assertNotNull(answer.getAnswer());

        // Get answer with Question object
        Answer answer2 = bardClient.getAnswer(
            Question.builder()
                .question("Who is his wife?")
                .build());
        Assertions.assertNotNull(answer2.getAnswer());

        // Reset session
        bardClient.reset();

        Answer answer3 = bardClient.getAnswer("Who is his wife?");
        Assertions.assertNotNull(answer3.getAnswer());
    }

    /**
     * Advanced usage: set custom http headers and request config
     */
    @Test
    public void testGetAnswer_customClient() throws URISyntaxException {
        // set custom headers
        Map<String, String> headers = new HashMap<>();
        headers.put("TestHeader", "TestValue");
        headers.put("TestHeader2", "TestValue2");

        // set custom request config
        RequestConfig requestConfig = RequestConfig.custom()
            // set timeout
            .setConnectTimeout(Timeout.of(20, TimeUnit.SECONDS))
            .setResponseTimeout(Timeout.of(20, TimeUnit.SECONDS))
            // set other options in requestConfig...
            .build();

        IBardClient bardClient = BardClient.builder(token)
            .headers(headers).requestConfig(requestConfig).build();


        Answer answer = bardClient.getAnswer("누구세요");
        Assertions.assertNotNull(answer.getAnswer());

        Answer answer2 = bardClient.getAnswer(Question.builder().question("あなたの名前は何ですか").build());
        Assertions.assertNotNull(answer2.getAnswer());
    }
}
```

<br>

## Further
### Support languages other than English, Japanese or Korean
As we know, Google Bard currently only support languages in ['en', 'ja', 'ko'], if you want to interact with Bard in other languages, we have to handle the translation by ourselves.

One of the most popular translation API is [Google Translate](https://cloud.google.com/translate/docs/basic/setup-basic), you can use it to translate your language to one of the supported languages, and then send the translated text to Bard. This API is not free of charge, you have to pay for it.

But there is an unofficial java google translate package we can use: [java-google-speech](https://github.com/akang943578/java-google-speech)

```java
import com.api.bard.model.Answer;
import com.api.bard.model.Question;
import com.api.bard.translator.GoogleTranslatorProxy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BardClientTest {
    private String token;

    @BeforeEach
    public void setup() {
        token = System.getenv("_BARD_API_KEY");
        Assertions.assertNotNull(token);
    }

    /**
     * Advanced usage: set translator to support languages other than English, Japanese or Korean
     */
    @Test
    public void testGetAnswer_withTranslator() {
        IBardClient bardClient = BardClient.builder(token)
            .translator(new GoogleTranslatorProxy())
            .build();

        Answer answer = bardClient.getAnswer("누구세요");
        Assertions.assertNotNull(answer.getAnswer());
        Assertions.assertFalse(answer.isUsedTranslator());

        Answer answer2 = bardClient.getAnswer(Question.builder().question("あなたの名前は何ですか").build());
        Assertions.assertNotNull(answer2.getAnswer());
        Assertions.assertFalse(answer2.isUsedTranslator());

        IBardClient bardClient2 = BardClient.builder(token)
            .translator(new GoogleTranslatorProxy("ja"))
            .build();

        Answer answer3 = bardClient2.getAnswer("How are you?");
        Assertions.assertNotNull(answer3.getAnswer());
        Assertions.assertFalse(answer3.isUsedTranslator());

        Answer answer4 = bardClient2.getAnswer(Question.builder().question("你是谁？").build());
        Assertions.assertNotNull(answer4.getAnswer());
        Assertions.assertTrue(answer4.isUsedTranslator());
    }
}
```

You can also implement your own translator by implementing the interface `IBardTranslator` and pass it to the builder.

<br><br>

## Contributors

I would like to express my sincere gratitude for the contributions made by all the contributors.

<a href="https://github.com/akang943578/java-bard-api/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=akang943578/java-bard-api" />
</a>


<br>

## License
[MIT](https://opensource.org/license/mit/) <br>
I hold no legal responsibility; for more information, please refer to the bottom of the readme file. I just want you to give me and [them](https://github.com/dsdanielpark/Bard-API) a star.
```
The MIT License (MIT)

Copyright (c) 2023 Minwoo Park

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Shifting Service Policies: Bard and Google's Dynamics
Bard's service status and Google's API interfaces are in constant flux. *The number of replies is currently limited, but certain users,* such as those utilizing VPNs or proxy servers, have reported slightly higher message caps. Adaptability is crucial in navigating these dynamic service policies. Please note that the cookie values used in this package are not official API values.

## Bugs and Issues
Sincerely grateful for any reports on new features or bugs. Your valuable feedback on the code is highly appreciated.

## Reference
[1] https://github.com/dsdanielpark/Bard-API <br>
[2] https://github.com/acheong08/Bard <br>

<br>

### Important Notice
The user assumes all legal responsibilities associated with using the BardAPI package. This Java package merely facilitates easy access to Google Bard for developers. Users are solely responsible for managing data and using the package appropriately. For further information, please consult the Google Bard Official Document.

### Caution
This Java package is not an official Google package or API service. It is not affiliated with Google and uses Google account cookies, which means that excessive or commercial usage may result in restrictions on your Google account. The package was created to support developers in testing functionalities due to delays in the official Google package. However, it should not be misused or abused. Please be cautious and refer to the Readme for more information.



*Copyright (c) 2023 akang943578*<br>
