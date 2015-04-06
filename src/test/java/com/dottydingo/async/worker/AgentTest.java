package com.dottydingo.async.worker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.concurrent.Semaphore;

import static org.junit.Assert.*;

/**
 */
public class AgentTest
{
    private Agent agent;
    private Poller poller;
    private Worker worker;
    private BackOffStrategy backOffStrategy;

    @Before
    public void setUp() throws Exception
    {
        agent = new Agent("test");
        agent.setMaximumWorkers(1);

        poller = Mockito.mock(Poller.class);
        agent.setPoller(poller);

        backOffStrategy = Mockito.mock(BackOffStrategy.class);
        Mockito.when(backOffStrategy.getWaitTime(Mockito.anyInt())).thenReturn(100L);

        agent.setBackOffStrategy(backOffStrategy);


    }

    @After
    public void tearDown() throws Exception
    {
        if(agent.getStatus() == AgentStatus.RUNNING)
            agent.stop();
    }

    @Test
    public void test() throws Exception
    {
        worker = Mockito.mock(Worker.class);

        agent.setWorker(worker);

        Mockito.when(poller.poll())
                .thenReturn(null)
                .thenReturn(new Object())
                .thenReturn(null);


        agent.start();

        Thread.sleep(2000L);

        agent.stop();

        Mockito.verify(backOffStrategy, Mockito.atLeast(1)).getWaitTime(1);
        Mockito.verify(worker,Mockito.times(1)).process(Mockito.anyObject());

    }



}