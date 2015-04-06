package com.dottydingo.async.worker;

/**
 */
public interface Worker<T>
{
    void process(T work);
}
