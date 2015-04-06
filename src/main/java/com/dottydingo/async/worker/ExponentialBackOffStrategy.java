package com.dottydingo.async.worker;

/**
 */
public class ExponentialBackOffStrategy implements BackOffStrategy
{
    private long minWaitTime = 1000;
    private long maxWaitTime = 60000;

    public void setMinWaitTime(long minWaitTime)
    {
        this.minWaitTime = minWaitTime;
    }

    public void setMaxWaitTime(long maxWaitTime)
    {
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public long getWaitTime(int emptyPolls)
    {
        if(emptyPolls == 0)
            return minWaitTime;

        long wait = (long) Math.pow(2,emptyPolls) * minWaitTime;
        return Math.min(wait, maxWaitTime);
    }
}
