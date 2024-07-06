package com.commrogue.solrexback.common.jobmanager;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.UUID;

@Component
public class JobManager {
    private final HashMap<UUID, StatefulJob> jobs = new HashMap<>();

    public UUID registerJob(StatefulJob job) {
        UUID uuid = UUID.randomUUID();
        this.jobs.put(uuid, job);

        return uuid;
    }

    public StatefulJob getJob(UUID uuid) {
        return this.jobs.get(uuid);
    }
}
