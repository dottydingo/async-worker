package com.dottydingo.async.worker;


import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class ExponentialBackOffStrategyTest
{

    @Test
    public void testGetWaitTime() throws Exception
    {
        ExponentialBackOffStrategy strategy = new ExponentialBackOffStrategy();
        strategy.setMaxWaitTime(10000);
        strategy.setMinWaitTime(1000);

        assertEquals(1000, strategy.getWaitTime(0));

        assertEquals(2000,strategy.getWaitTime(1));
        assertEquals(4000, strategy.getWaitTime(2));
        assertEquals(8000,strategy.getWaitTime(3));
        assertEquals(10000,strategy.getWaitTime(4));

        assertEquals(1000,strategy.getWaitTime(0));


    }
}