package cloud.dispatcher.gateway.pay.cashier.domain.refund.executor.support;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.service.RefundService;

public class ProducerTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerTask.class);

    @Getter @Setter private volatile boolean closedTrigger = false;

    @Getter @Setter private ConcurrentLinkedQueue<RefundEntity> queue;

    @Getter @Setter private int tablePointer = 0;

    private static final int THREAD_SLEEP_TIME_SECOND_MAX = 8;

    @Getter @Setter private RefundService simpleRefundService;

    @Override
    public void run() {
        while (!closedTrigger) {
            if (queue.size() > 1000) {
                try {
                    Thread.sleep(THREAD_SLEEP_TIME_SECOND_MAX * 1000);
                } catch (InterruptedException error) {
                    LOGGER.error("", error);
                }
            }

            List<RefundEntity> orderList = simpleRefundService.getUnoperating(tablePointer);
            if (orderList == null || orderList.size() == 0) {
                try {
                    Thread.sleep(THREAD_SLEEP_TIME_SECOND_MAX * 1000);
                } catch (InterruptedException error) {
                    LOGGER.error("", error);
                }
            } else {
                synchronized (queue) {
                    for (RefundEntity order : orderList) {
                        simpleRefundService.doCommit(order);
                        queue.offer(order);
                        LOGGER.info("[ProducerTask] Queue produce: order = [" + order + "]");
                    }
                    queue.notify();
                }
            }
        }
    }
}
