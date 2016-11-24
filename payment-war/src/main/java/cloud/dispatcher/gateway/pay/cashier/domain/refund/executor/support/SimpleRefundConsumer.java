package cloud.dispatcher.gateway.pay.cashier.domain.refund.executor.support;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.executor.RefundConsumer;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.service.RefundService;

public class SimpleRefundConsumer implements RefundConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRefundConsumer.class);

    private ExecutorService executor;

    @Getter @Setter private ConcurrentLinkedQueue<RefundEntity> queue;

    @Getter @Setter private int threadPoolMax = 4;

    private List<ConsumerTask> taskList = new LinkedList<ConsumerTask>();

    @Getter @Setter private RefundService simpleRefundService;

    @Override
    public void initialize() {
        long startTime = System.nanoTime();

        executor = Executors.newFixedThreadPool(threadPoolMax);

        for (int i = 0; i < threadPoolMax; i++) {
            ConsumerTask task = new ConsumerTask();
            task.setQueue(queue);
            task.setSimpleRefundService(simpleRefundService);
            taskList.add(task);
            executor.execute(task);
        }

        LOGGER.info("[SimpleRefundConsumer] Initialized, cost: {}ms",
                (System.nanoTime() - startTime) / 1000000);
    }

    @Override
    public void destroy() {
        long startTime = System.nanoTime();
        for (ConsumerTask task : taskList)
            task.setClosedTrigger(true);

        boolean closed = true;
        while (true) {
            for (ConsumerTask task : taskList) {
                if (task.isProcessing()) {
                    closed = false;
                }
            }
            if (closed) {break;}
        }

        synchronized (queue) {
            for (int i = 0; i < queue.size(); i++) {
                RefundEntity entity = queue.poll();
                simpleRefundService.doRollback(entity);
                LOGGER.info("[SimpleRefundProducer] Rollback, entity = [" + entity + "]");
            }
        }

        executor.shutdown();
        LOGGER.info("[SimpleRefundConsumer] Closed, cost: {}ms",
                (System.nanoTime() - startTime) / 1000000);
    }
}
