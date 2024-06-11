package com.commrogue.solrexback.common.jobmanager;

import lombok.Data;

public interface StatefulJob {
    enum State {
        RUNNING, FINISHED, PAUSED, TERMINATED
    }

    State getState();
    void terminate();
    void start();
}
