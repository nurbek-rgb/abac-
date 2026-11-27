package kz.abac;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;

/** Fixed worker pool; ordered results, propagated errors, explicit shutdown (Java 17). */
public final class BatchEvaluator implements AutoCloseable {
    private final ExecutorService pool;
    private final Function<AttributeContext,Decision> evaluator;
    public BatchEvaluator(Function<AttributeContext,Decision> evaluator,int threads) {
        if(threads<1 || threads>64) throw new IllegalArgumentException("Threads must be in 1..64");
        this.evaluator=Objects.requireNonNull(evaluator);
        this.pool=Executors.newFixedThreadPool(threads);
    }
    public List<Decision> evaluateAll(List<AttributeContext> requests) {
        List<AttributeContext> snapshot=List.copyOf(requests);
        List<Future<Decision>> futures=new ArrayList<>();
        try {
            for(AttributeContext request:snapshot) futures.add(pool.submit(()->evaluator.apply(request)));
            List<Decision> decisions=new ArrayList<>();
            for(Future<Decision> future:futures) decisions.add(future.get());
            return List.copyOf(decisions);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Batch interrupted",e);
        } catch(ExecutionException e) {
            if(e.getCause() instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Batch worker failed",e.getCause());
        } finally {
            // Failure can leave partial audit records; a batch is deliberately not a transaction.
            futures.stream().filter(f->!f.isDone()).forEach(f->f.cancel(true));
        }
    }
    @Override public void close() {
        pool.shutdown();
        try {
            if(!pool.awaitTermination(10,TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if(!pool.awaitTermination(10,TimeUnit.SECONDS)) throw new IllegalStateException("Workers did not stop");
            }
        } catch(InterruptedException e) { pool.shutdownNow();Thread.currentThread().interrupt(); }
    }
}
