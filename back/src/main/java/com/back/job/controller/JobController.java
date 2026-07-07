package com.back.job.controller;

import com.back.global.exception.AppException;
import com.back.global.storage.FileStorage;
import com.back.job.dto.JobResultResponse;
import com.back.job.dto.JobStatusResponse;
import com.back.job.entity.Job;
import com.back.job.entity.JobStatus;
import com.back.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final FileStorage fileStorage;

    @GetMapping("/{id}")
    public JobStatusResponse getStatus(@PathVariable String id) {
        Job job = jobService.get(id);
        return new JobStatusResponse(job.getId(), job.getStatus().name(), job.getExpiresAt());
    }

    @GetMapping("/{id}/result")
    public JobResultResponse getResult(@PathVariable String id) {
        Job job = jobService.get(id);
        if (job.getResultKey() != null) {
            return new JobResultResponse(fileStorage.getUrl(job.getResultKey()), null);
        }
        return new JobResultResponse(null, job.getResultText());
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String id) {
        SseEmitter emitter = new SseEmitter(300_000L);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        emitter.onCompletion(exec::shutdownNow);
        emitter.onTimeout(exec::shutdownNow);

        exec.submit(() -> {
            String lastStatus = null;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Job job = jobService.get(id);
                    String status = job.getStatus().name();
                    if (!status.equals(lastStatus)) {
                        lastStatus = status;
                        emitter.send(SseEmitter.event()
                                .name("job-status-changed")
                                .data(Map.of("jobId", id, "status", status),
                                        MediaType.APPLICATION_JSON));
                    }
                    if (job.getStatus() == JobStatus.DONE || job.getStatus() == JobStatus.FAILED) {
                        emitter.complete();
                        return;
                    }
                    Thread.sleep(2000);
                }
            } catch (AppException e) {
                // Job may have been removed by TTL cleanup
                try { emitter.complete(); } catch (IllegalStateException ignored) {}
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IllegalStateException ignored) {
                // Emitter already completed (e.g. client disconnected or timeout)
            } catch (Exception e) {
                try { emitter.completeWithError(e); } catch (IllegalStateException ignored) {}
            }
        });

        return emitter;
    }
}
