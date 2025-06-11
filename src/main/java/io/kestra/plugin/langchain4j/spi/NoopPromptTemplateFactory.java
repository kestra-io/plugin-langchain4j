package io.kestra.plugin.langchain4j.spi;

import dev.langchain4j.spi.prompt.PromptTemplateFactory;

import java.util.Map;

public class NoopPromptTemplateFactory implements PromptTemplateFactory {
    @Override
    public Template create(Input input) {
        return new NoopTemplate(input.getTemplate());
    }

    static class NoopTemplate implements Template {
        private final String template;

        public NoopTemplate(String template) {
            this.template = template;
        }

        @Override
        public String render(Map<String, Object> variables) {
            return template;
        }
    }
}
