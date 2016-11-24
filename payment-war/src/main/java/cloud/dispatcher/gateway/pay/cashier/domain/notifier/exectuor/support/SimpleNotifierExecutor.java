package cloud.dispatcher.gateway.pay.cashier.domain.notifier.exectuor.support;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.Setter;
import net.rubyeye.xmemcached.XMemcachedClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.cashier.domain.notifier.bean.entity.NoticeEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.notifier.exectuor.NotifierExecutor;
import cloud.dispatcher.gateway.pay.cashier.http.HttpClientFactory;
import cloud.dispatcher.gateway.pay.cashier.utils.CodeUtil;
import cloud.dispatcher.gateway.pay.cashier.utils.CommonUtil;

public class SimpleNotifierExecutor implements NotifierExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleNotifierExecutor.class);

    private static final String NOTIFIER_MEMCACHE_STORAGE_KEY = "NOTIFIER_" +
            CodeUtil.md5Hex(CommonUtil.getThisInstanceIpAddress(), "GBK");

    @Getter @Setter private HttpClientFactory httpClientFactory;

    @Getter @Setter private int threadPoolMax = 4;

    @Getter @Setter private XMemcachedClient xMemcachedClient;

    private ConcurrentLinkedQueue<NoticeEntity> queue = new ConcurrentLinkedQueue<NoticeEntity>();

    private ExecutorService executor;

    private List<NotifierTask> taskList = new LinkedList<NotifierTask>();

    @Override
    public void register(NoticeEntity entity) {
        synchronized (queue) {
            queue.offer(entity); queue.notify();
            LOGGER.info("[SimpleNotifierExecutor] Notify registered, entity = [" + entity + "]");
        }
    }

    @Override
    public void initialize() {
        long startTime = System.nanoTime();
        try {
            List<NoticeEntity> temporaryList = xMemcachedClient.get(
                    NOTIFIER_MEMCACHE_STORAGE_KEY);
            if (temporaryList != null && temporaryList.size() > 0) {
                synchronized (queue) {
                    for (NoticeEntity entity : temporaryList) {
                        queue.offer(entity);
                        LOGGER.info("[SimpleNotifierExecutor] Notify loaded, entity = [" + entity + "]");
                    }
                    queue.notify();
                }
            }
            xMemcachedClient.delete(NOTIFIER_MEMCACHE_STORAGE_KEY);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }

        executor = Executors.newFixedThreadPool(threadPoolMax);

        for (int i = 0; i < threadPoolMax; i++) {
            NotifierTask task = new NotifierTask();
            task.setQueue(queue);
            task.setHttpClientFactory(httpClientFactory);
            taskList.add(task);
            executor.execute(task);
        }

        LOGGER.info("[SimpleNotifierExecutor] Initialized, cost: {}ms",
                (System.nanoTime() - startTime) / 1000000);
    }

    @Override
    public void destroy() {
        long startTime = System.nanoTime();
        for (NotifierTask task : taskList) {
            task.setClosedTrigger(true);
        }

        boolean closed = true;

        while (true) {
            for (NotifierTask task : taskList) {
                if (task.isProcessing()) {
                    closed = false;
                }
            }
            if (closed) {break;}
        }

        List<NoticeEntity> tempList = new LinkedList<NoticeEntity>();

        synchronized (queue) {
            for (int i = 0; i < queue.size(); i++) {
                NoticeEntity entity = queue.poll();
                tempList.add(entity);
                LOGGER.info("[SimpleRefundProducer] Rollback, entity = [" + entity + "]");
            }
        }

        if (tempList.size() > 0) {
            try {
                xMemcachedClient.set(NOTIFIER_MEMCACHE_STORAGE_KEY, 0, tempList);
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }

        executor.shutdown();
        LOGGER.info("[SimpleNotifierExecutor] Closed, cost: {}ms",
                (System.nanoTime() - startTime) / 1000000);
    }
}
