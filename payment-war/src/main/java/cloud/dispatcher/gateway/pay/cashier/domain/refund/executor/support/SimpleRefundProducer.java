package cloud.dispatcher.gateway.pay.cashier.domain.refund.executor.support;

import java.util.ArrayList;
import java.util.Arrays;
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
import cloud.dispatcher.gateway.pay.cashier.domain.refund.executor.RefundProducer;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.service.RefundService;
import cloud.dispatcher.gateway.pay.cashier.utils.CommonUtil;

public class SimpleRefundProducer implements RefundProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRefundProducer.class);

    @Getter @Setter private String serverPoolConfig;

    @Getter @Setter private ConcurrentLinkedQueue<RefundEntity> queue;

    private ExecutorService executor = null;

    private List<Integer> tablePool = new ArrayList<Integer>();

    private static final int REFUND_TABLE_TOTAL_NUMBER = 16;

    private List<ProducerTask> taskList = new LinkedList<ProducerTask>();

    @Getter @Setter private RefundService simpleRefundService;

    private void initTablePool() {
        List<String> serverList = Arrays.asList(serverPoolConfig.split(","));
        int indexOfThisInstance = serverList.indexOf(CommonUtil.getThisInstanceIpAddress());
        if (indexOfThisInstance != -1) {
            for (int pointer = 1; pointer <= REFUND_TABLE_TOTAL_NUMBER; pointer++) {
                if (indexOfThisInstance == pointer % serverList.size()) {
                    tablePool.add(pointer);
                }
            }
        }
        LOGGER.info("[SimpleRefundProducer] Pool loaded, Pool = [" + tablePool + "]");
    }

    @Override
    public void initialize() {
        long startTime = System.nanoTime();
        initTablePool();

        if (!tablePool.isEmpty()) {
            executor = Executors.newFixedThreadPool(tablePool.size());
            for (Integer pointer : tablePool) {
                ProducerTask task = new ProducerTask();
                task.setQueue(queue);
                task.setSimpleRefundService(simpleRefundService);
                task.setTablePointer(pointer);
                taskList.add(task);
                executor.execute(task);
            }
        }

        LOGGER.info("[SimpleRefundProducer] Initialized, cost: {}ms",
                (System.nanoTime() - startTime) / 1000000);
    }

    @Override
    public void destroy() {
        long startTime = System.nanoTime();
        for (ProducerTask task : taskList)
            task.setClosedTrigger(true);
        if (executor != null)
            executor.shutdown();
        LOGGER.info("[SimpleRefundProducer] Closed, cost: {}ms",
                (System.nanoTime() - startTime) / 1000000);
    }
}
