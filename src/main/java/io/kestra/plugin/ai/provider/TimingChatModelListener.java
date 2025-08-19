package io.kestra.plugin.ai.provider;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.apache.commons.lang3.time.StopWatch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TimingChatModelListener implements ChatModelListener {
    private static Map<Integer, StopWatch> timers = new HashMap<>();
    private static Map<String, Integer> timerIdByResponseId = new HashMap<>();

    private AtomicInteger counter = new AtomicInteger(0);

    public static StopWatch getTimer(String responseId) {
        Integer timerId = timerIdByResponseId.get(responseId);
        return timers.get(timerId);
    }

    public static void clear() {
        timers.clear();
        timerIdByResponseId.clear();
    }

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        Integer timerId = counter.incrementAndGet();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        timers.put(timerId, stopWatch);
        requestContext.attributes().put("kestra.timer.id", timerId);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        Integer timerId = (Integer) responseContext.attributes().get("kestra.timer.id");
        StopWatch stopWatch = timers.get(timerId);
        stopWatch.stop();
        timerIdByResponseId.put(responseContext.chatResponse().id(), timerId );
    }
}
