package com.dottydingo.async.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 */
public class Agent<T>
{
    private Logger logger = LoggerFactory.getLogger(Agent.class);

    private int maximumWorkers = 10;
    private int currentWorkers;
    private String name;
    private boolean forcePollOnWorkerComplete = true;

    private final AtomicBoolean polling = new AtomicBoolean(false);
    private final AtomicReference<AgentStatus> status = new AtomicReference<>(AgentStatus.STOPPED);
    private Semaphore workerSemaphore;
    private ExecutorService executor;
    private PollerThread pollerThread;
    private Worker<T> worker;
    private MetricReporter metricReporter = new NoOpMetricReporter();

    private Poller<T> poller;
    private BackOffStrategy backOffStrategy;

    public Agent(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public int getMaximumWorkers()
    {
        return maximumWorkers;
    }

    public void setMaximumWorkers(int maximumWorkers)
    {
        if(maximumWorkers < 1)
            throw new RuntimeException("maximumWorkers must be at least 1.");

        this.maximumWorkers = maximumWorkers;
    }

    public void setPoller(Poller<T> poller)
    {
        this.poller = poller;
    }

    public void setBackOffStrategy(BackOffStrategy backOffStrategy)
    {
        this.backOffStrategy = backOffStrategy;
    }


    public void setWorker(Worker<T> worker)
    {
        this.worker = worker;
    }

    public void setMetricReporter(MetricReporter metricReporter)
    {
        this.metricReporter = metricReporter;
    }

    public void setForcePollOnWorkerComplete(boolean forcePollOnWorkerComplete)
    {
        this.forcePollOnWorkerComplete = forcePollOnWorkerComplete;
    }

    public AgentStatus getStatus()
    {
        return status.get();
    }

    public int getCurrentWorkers()
    {
        return currentWorkers;
    }

    public int getActiveWorkers()
    {
        return currentWorkers - workerSemaphore.availablePermits();
    }

    public void start()
    {
        logger.info("Staring agent {}",name);
        if(status.get() != AgentStatus.STOPPED)
            throw new RuntimeException(String.format("Agent \"%s\" is already started.",name));

        currentWorkers = maximumWorkers;
        workerSemaphore = new Semaphore(currentWorkers,true);
        executor = Executors.newFixedThreadPool(currentWorkers,new WorkerThreadFactory());
        status.set(AgentStatus.RUNNING);
        pollerThread = new PollerThread();
        pollerThread.start();

        logger.info("Agent {} started", name);
    }

    public void stop()
    {
        logger.info("Stopping agent {}",name);
        if(status.get() != AgentStatus.RUNNING)
            throw new RuntimeException(String.format("Agent \"%s\" is already stopped.",name));

        status.set(AgentStatus.SHUTTING_DOWN);
        pollerThread.interrupt();

        // complete anything being processed
        executor.shutdown();

        status.set(AgentStatus.STOPPED);

        logger.info("Agent {} stopped", name);
    }

    protected boolean poll()
    {
        if(status.get() == AgentStatus.RUNNING
                && workerSemaphore.availablePermits() > 0
                && polling.compareAndSet(false,true) )
        {
            logger.debug("Agent {} - starting poll.",name);
            try
            {
                long start = System.currentTimeMillis();
                T workItem = poller.poll();
                metricReporter.polled(System.currentTimeMillis() - start);

                if (workItem != null)
                {
                    metricReporter.foundWork();
                    logger.debug("Agent {} - found work.",name);
                    workerSemaphore.acquire();
                    executor.submit(new WorkerWrapper(workItem));
                    return true;
                }
                else
                    logger.debug("Agent {} - did not find work.",name);
            }
            catch (Throwable t)
            {
                logger.error(String.format("Error polling in agent %s", name), t);
            }
            finally
            {
                polling.set(false);
            }
        }
        else
            logger.debug("Agent {} - not eligible to poll.",name);

        return false;
    }

    private class PollerThread extends Thread
    {
        public PollerThread()
        {
            super(String.format("agent-%s-poller",name));
        }

        @Override
        public void run()
        {
            int unsuccessfulPolls = 0;
            while (status.get() == AgentStatus.RUNNING)
            {
                boolean found = poll();
                if(found)
                    unsuccessfulPolls = 0;
                else
                {
                    long sleepTime = backOffStrategy.getWaitTime(++unsuccessfulPolls);
                    logger.debug("Agent {} sleeping for {}",name,sleepTime);
                    try
                    {
                        Thread.sleep(sleepTime);
                    }
                    catch (InterruptedException ignore){}
                }

            }

            logger.debug("Agent {} poller exiting.",name);
        }
    }

    private class WorkerThreadFactory implements ThreadFactory
    {
        private AtomicLong counter = new AtomicLong(0);

        @Override
        public Thread newThread(Runnable r)
        {
            return new Thread(r,name + "-worker-"+ counter.getAndIncrement());
        }
    }

    private class WorkerWrapper implements Runnable
    {
        private T work;

        public WorkerWrapper(T work)
        {
            this.work = work;
        }

        @Override
        public void run()
        {
            metricReporter.activeWorkers(getActiveWorkers());
            long start = System.currentTimeMillis();
            try
            {
                worker.process(work);
            }
            catch (Throwable t)
            {
                logger.error(String.format("Worker for Agent %s threw an exception.",name),t);
            }
            finally
            {
                metricReporter.workProcessed(System.currentTimeMillis() - start);
                workerSemaphore.release();
                metricReporter.activeWorkers(getActiveWorkers());
                if(forcePollOnWorkerComplete)
                {
                    logger.debug("Forcing poll");
                    poll();
                }
            }
        }
    }

    private class NoOpMetricReporter implements MetricReporter
    {
        @Override
        public void polled(long time)
        {

        }

        @Override
        public void foundWork()
        {

        }

        @Override
        public void activeWorkers(int activeWorkers)
        {

        }

        @Override
        public void workProcessed(long time)
        {

        }
    }
}
