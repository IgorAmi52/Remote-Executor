package com.pekara.controller.application.api.in;

public interface SubmitCommand {

    String submit(String scriptName, String scriptContent, int requiredCpus);
}
