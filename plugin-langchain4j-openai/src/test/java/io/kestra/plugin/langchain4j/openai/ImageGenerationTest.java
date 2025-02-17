package io.kestra.plugin.langchain4j.openai;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for ImageGeneration
 */
@KestraTest
class ImageGenerationTest {

    @Inject
    private RunContextFactory runContextFactory;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void run() throws Exception {
        // Stub the OpenAI API response
        stubFor(post(urlEqualTo("/v1/images/generations"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"data\": [{\"url\": \"https://mock-image-url.com/image.png\"}]}")));

        // GIVEN
        RunContext runContext = runContextFactory.of(Map.of(
            "prompt", "Donald Duck in New York, cartoon style",
            "apiKey", "demo",
            "modelName", "dall-e-3",
            "apiUrl", "http://localhost:" + wireMockServer.port() +"/v1"
        ));

        ImageGeneration task = ImageGeneration.builder()
            .prompt(new Property<>("{{ prompt }}"))
            .apiKey(new Property<>("{{ apiKey }}"))
            .modelName(new Property<>("{{ modelName }}"))
            .apiUrl(new Property<>("{{ apiUrl }}"))
            .build();

        // WHEN
        ImageGeneration.Output runOutput = task.run(runContext);

        // THEN
        assertThat(runOutput.getCompletion(), is("https://mock-image-url.com/image.png"));

        // Verify WireMock interaction
        verify(postRequestedFor(urlEqualTo("/v1/images/generations"))
            .withRequestBody(matchingJsonPath("$.prompt", equalTo("Donald Duck in New York, cartoon style")))
            .withHeader("Authorization", matching("Bearer demo")));
    }
}
