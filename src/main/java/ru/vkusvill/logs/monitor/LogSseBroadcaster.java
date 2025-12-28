package ru.vkusvill.logs.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.vkusvill.logs.logging.LogRecord;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LogSseBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LogSseBroadcaster.class);

    private final List<SseEmitter> clients = new CopyOnWriteArrayList<SseEmitter>();

    public SseEmitter registerClient() {
        SseEmitter emitter = new SseEmitter(0L); // без таймаута
        clients.add(emitter);

        emitter.onCompletion(new Runnable() {
            @Override
            public void run() {
                clients.remove(emitter);
            }
        });
        emitter.onTimeout(new Runnable() {
            @Override
            public void run() {
                clients.remove(emitter);
            }
        });

        return emitter;
    }

    public void broadcast(LogRecord record) {
        for (SseEmitter emitter : clients) {
            try {
                emitter.send(SseEmitter.event()
                        .name("log")
                        .data(record.getColumns()));
            } catch (IOException e) {
                clients.remove(emitter);
                log.warn("Removed SSE client due to IO error", e);
            }
        }
    }
}
