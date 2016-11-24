package cloud.dispatcher.gateway.pay.cashier.domain.refund.executor.support;

import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.service.RefundService;

public class ConsumerTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerTask.class);

    @Getter @Setter private volatile boolean closedTrigger = false;

    @Getter @Setter private ConcurrentLinkedQueue<RefundEntity> queue;

    @Getter @Setter private RefundService simpleRefundService;

    @Getter @Setter private volatile boolean isProcessing = false;

    @Override
    public void run() {
        while (!closedTrigger) {
            RefundEntity order = null;
            synchronized (queue) {
                order = queue.poll();
                if (order == null) {
                    try {
                        queue.wait();
                    } catch (InterruptedException error) {
                        LOGGER.error("", error);
                    }
                    order = queue.poll();
                }
            }
            isProcessing = true;
            operation(order);
            isProcessing = false;
        }
    }

    private void operation(RefundEntity order) {
        if (order == null) return;
        try {
            boolean result = simpleRefundService.doRefund(order);
            LOGGER.info("[ConsumerTask] Queue consume: result = [" + result + "], order = [" + order + "]");
        } catch (Exception error) {
            LOGGER.error("", error);
            synchronized (queue) {queue.offer(order); queue.notify();}
            LOGGER.info("[ConsumerTask] Queue produce: order = [" + order + "]");
        }
    }
}
