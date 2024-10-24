/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.jobmanager;

import java.util.HashMap;
import java.util.UUID;
import org.springframework.stereotype.Component;

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
