package com.ironcladrs.events.capture;

import java.util.concurrent.*;
import javax.inject.Singleton;

/** The plugin owns exactly one executor; no feature outlives its lifecycle. */
@Singleton
public class WorkExecutor
{
    private volatile ScheduledExecutorService executor;
    public void start() { executor = Executors.newSingleThreadScheduledExecutor(); }
    public ScheduledFuture<?> repeat(Runnable task) { return executor.scheduleWithFixedDelay(task, 1, 1, TimeUnit.SECONDS); }
    public ScheduledFuture<?> schedule(Runnable task, long seconds)
    {
        ScheduledExecutorService active = executor;
        if (active == null || active.isShutdown()) { return null; }
        try { return active.schedule(task, seconds, TimeUnit.SECONDS); }
        catch (RejectedExecutionException ignored) { return null; }
    }
    public void execute(Runnable task)
    {
        ScheduledExecutorService active = executor;
        if (active != null && !active.isShutdown())
        { try { active.execute(task); } catch (RejectedExecutionException ignored) { /* Stopping. */ } }
    }
    public void stop() { if (executor != null) { executor.shutdownNow(); executor = null; } }
}
