package io.kestra.plugin.langchain.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConstantTest {

    public final static String OPENAI_DEMO_APIKEY = "demo";

    // TEXT COMPLETION TEST
    public final static String PROMPT_TEXT_COMPLETION = "What is the capital of France?";
    public final static String EXPECTED_RESULT = "The capital of France is Paris.";
    public final static String OPENAI_TEXT_MINI_MODEL = "gpt-4o-mini";

    // IMAGE GENERATION TEST
    public final static String TEST_PROMPT_IMAGE_GENERATION = "Donald Duck in New York, cartoon style";
    public final static String OPENAI_IMAGE_GENERATION_MODEL = "dall-e-2";
    public final static String OPENAI_IMAGE_GENERATION_URI = "/v1/images/generations";
}
