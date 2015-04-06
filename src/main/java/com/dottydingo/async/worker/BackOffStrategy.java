package com.dottydingo.async.worker;

/**
 */
public interface BackOffStrategy
{
    long getWaitTime(int emptyPolls);
}
