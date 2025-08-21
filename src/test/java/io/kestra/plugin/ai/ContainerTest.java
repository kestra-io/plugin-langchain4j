package io.kestra.plugin.ai;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;

public class ContainerTest {

    public static GenericContainer<?> ollamaContainer;
    public static String ollamaEndpoint;

    private static final List<String> models = List.of(
        "tinydolphin",
        "chroma/all-minilm-l6-v2-f32"
    );

    @BeforeAll
    public static void setUp() throws Exception {
        var dockerImageName = DockerImageName.parse("ollama/ollama:latest");

        ollamaContainer = new GenericContainer<>(dockerImageName)
            .withExposedPorts(11434)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)))
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withRuntime("runc"))
            .withEnv("NVIDIA_VISIBLE_DEVICES", "");

        ollamaContainer.start();

        ollamaEndpoint = "http://" + ollamaContainer.getHost() + ":" + ollamaContainer.getMappedPort(11434);

        waitUntilOllamaResponds();

        for (String model : models) {
            pullModel(model);
        }

        waitForModels(models);
    }

    private static void waitUntilOllamaResponds() {
        await("ollama daemon responds")
            .pollDelay(Duration.ZERO)
            .pollInterval(Duration.ofMillis(250))
            .atMost(Duration.ofSeconds(15))
            .until(() -> execOk("ollama", "list"));
    }

    private static void pullModel(String model) {
        await("pull " + model)
            .pollDelay(Duration.ZERO)
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(30))
            .until(() -> execOk("ollama", "pull", model));
    }

    private static void waitForModels(List<String> models) {
        await("models installed")
            .pollDelay(Duration.ZERO)
            .pollInterval(Duration.ofSeconds(1))
            .atMost(Duration.ofSeconds(30))
            .until(() -> models.stream().allMatch(m -> execOk("ollama", "show", m)));
    }

    private static boolean execOk(String... cmd) {
        try {
            Container.ExecResult res = ollamaContainer.execInContainer(cmd);
            return res.getExitCode() == 0;
        } catch (Exception e) {
            return false; // Awaitility will retry to the next poll
        }
    }

    @AfterAll
    static void tearDown() {
        if (ollamaContainer != null) {
            ollamaContainer.stop();
        }
    }
}
