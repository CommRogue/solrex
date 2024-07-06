package com.commrogue.solrexback.common.jobmanager;

public interface StatefulJob {
    State getState();

    void terminate();

    void start();

    enum State {
        RUNNING, FINISHED, TERMINATED, AWAITING
    }
}
