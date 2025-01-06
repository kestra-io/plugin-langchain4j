package io.kestra.plugin.langchain.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConstantTest {

    public final static String TEST_PROMPT = "What is the capital of France?";
    public final static String EXPECTED_RESULT = "The capital of France is Paris.";
    public final static String OPENAI_DEMO_APIKEY = "demo";
    public final static String OPENAI_GPT4_MINI_MODEL = "gpt-4o-mini";
}
