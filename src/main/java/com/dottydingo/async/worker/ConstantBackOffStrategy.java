package com.dottydingo.async.worker;

/**
 */
public class ConstantBackOffStrategy implements BackOffStrategy
{
    private long waitTime;

    public void setWaitTime(long waitTime)
    {
        this.waitTime = waitTime;
    }

    public long getWaitTime()
    {
        return waitTime;
    }

    @Override
    public long getWaitTime(int i)
    {
        return waitTime;
    }
}
