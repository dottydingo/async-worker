package com.dottydingo.async.worker;

/**
 */
public interface Poller<T>
{
    T poll();
}
