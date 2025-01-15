package io.kestra.plugin.ollama;

import io.kestra.plugin.enums.EOllamaModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;


public class OllamaContainerTest {


    public static GenericContainer<?> ollamaContainer;
    public static String ollamaEndpoint;


    @BeforeAll
    public static void setUp() throws Exception {
        // Docker image
        DockerImageName dockerImageName = DockerImageName.parse("ollama/ollama");

        // Create the container
        ollamaContainer = new GenericContainer<>(dockerImageName)
            .withExposedPorts(11434)
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withRuntime("runc"))
            .withEnv("NVIDIA_VISIBLE_DEVICES", "");

        // Start the container
        ollamaContainer.start();

        // Get the container's endpoint for testing
        ollamaEndpoint = "http://" + ollamaContainer.getHost() + ":" + ollamaContainer.getMappedPort(11434);

        // Pull a lightweight model for testing
        ollamaContainer.execInContainer("ollama", "pull", EOllamaModel.TINY_DOLPHIN.getName());
    }


    @AfterAll
    static void tearDown() {
        if (ollamaContainer != null) {
            ollamaContainer.stop();
        }
    }
}
