package com.api.bard;

public class BardClientMain {

    public static void main(String[] args) {
        String token = System.getenv("_BARD_API_KEY");
        IBardClient bardClient = BardClient.builder(token).build();

        String answer = bardClient.getAnswer("Who are you?").getAnswer();
        System.out.println(answer);
    }
}
