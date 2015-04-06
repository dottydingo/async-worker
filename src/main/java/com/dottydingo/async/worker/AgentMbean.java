package com.dottydingo.async.worker;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 */
@ManagedResource
public class AgentMbean
{
    private Agent agent;

    public AgentMbean(Agent agent)
    {
        this.agent = agent;
    }

    @ManagedAttribute
    public AgentStatus getStatus()
    {
        return agent.getStatus();
    }

    @ManagedAttribute
    public int getCurrentWorkers()
    {
        return agent.getCurrentWorkers();
    }

    @ManagedOperation
    public void start()
    {
        agent.start();
    }

    @ManagedOperation
    public void stop()
    {
        agent.stop();
    }

    @ManagedAttribute
    public void setMaximumWorkers(int value)
    {
        agent.setMaximumWorkers(value);
    }

    @ManagedAttribute
    public int getMaximumWorkers()
    {
        return agent.getMaximumWorkers();
    }

    @ManagedAttribute
    public String getName()
    {
        return agent.getName();
    }

    @ManagedAttribute
    public int getActiveWorkers()
    {
        return agent.getActiveWorkers();
    }
}
