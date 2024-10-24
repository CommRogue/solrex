/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.jobmanager;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@Getter
public class StatefulJob {
    enum State {
        RUNNING,
        FINISHED,
        TERMINATED,
        AWAITING,
        FAILED
    }

    private Disposable jobDisposable;
    private final Job job;

    @Setter(AccessLevel.PRIVATE)
    private State state = State.AWAITING;

    @Setter(AccessLevel.PRIVATE)
    private String exitReason;

    public void start() {
        this.setState(State.RUNNING);

        this.jobDisposable = this.job
                .start()
                .doOnError((e) -> {
                    this.setState(State.FAILED);
                    this.setExitReason(e.getMessage());
                })
                .doOnSuccess((finishMessage) -> {
                    this.setState(State.FINISHED);
                    this.setExitReason(finishMessage);
                })
                .doOnEach((signal) -> this.job.cleanup())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    public void stop(String reason) {
        this.jobDisposable.dispose();
        this.setState(State.TERMINATED);
        this.setExitReason(reason);
    }

    public void stop() {
        this.stop(null);
    }

    public Optional<String> getSummary() {
        String summary = Stream.of(
                        Optional.of(this.getState()).map(s -> "State: " + s.name()),
                        this.job.getTitle().map(t -> "Title: " + t),
                        this.job.getDescription().map(d -> "Description: " + d),
                        this.job.getStateDescription().map(sd -> "State description: " + sd))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n"));

        return !summary.isBlank() ? Optional.empty() : Optional.of(summary);
    }

    public Optional<String> getTitle() {
        return this.job.getTitle();
    }

    public Optional<String> getDescription() {
        return this.job.getDescription();
    }

    public Optional<String> getStateDescription() {
        return this.job.getStateDescription();
    }
}
