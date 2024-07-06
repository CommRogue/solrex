package com.commrogue.solrexback.common.jobmanager;

import lombok.Data;

public interface StatefulJob {
    enum State {
        RUNNING, FINISHED, TERMINATED, AWAITING
    }

    State getState();
    void terminate();
    void start();
}
