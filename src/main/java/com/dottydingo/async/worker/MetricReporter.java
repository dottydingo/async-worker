package com.dottydingo.async.worker;

/**
 */
public interface MetricReporter
{
    void polled(long time);

    void foundWork();

    void activeWorkers(int activeWorkers);

    void workProcessed(long time);
}
