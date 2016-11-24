package cloud.dispatcher.gateway.pay.cashier.domain.notifier.exectuor.support;

import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.cashier.domain.notifier.bean.entity.NoticeEntity;
import cloud.dispatcher.gateway.pay.cashier.http.HttpClientFactory;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

public class NotifierTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierTask.class);

    private static final long NOTIFY_TIME_INTERVAL = 60000;

    @Getter @Setter private ConcurrentLinkedQueue<NoticeEntity> queue;

    @Getter @Setter private volatile boolean isProcessing = false;

    @Getter @Setter private HttpClientFactory httpClientFactory;

    @Getter @Setter private volatile boolean closedTrigger = false;

    @Override
    public void run() {
        while (!closedTrigger) {
            NoticeEntity entity = null;
            synchronized (queue) {
                entity = queue.poll();
                if (entity == null) {
                    try {
                        queue.wait();
                    } catch (InterruptedException error) {
                        LOGGER.error("", error);
                    }
                    entity = queue.poll();
                }
            }
            isProcessing = true;
            operation(entity);
            isProcessing = false;
        }
    }

    private void operation(NoticeEntity entity) {
        long currentTime = System.currentTimeMillis();
        if (entity == null) return;
        if (entity.getNotifyTime() > currentTime) {
            synchronized (queue) {
                queue.offer(entity);
            }
        } else {
            HttpClient client = httpClientFactory.getSimpleHttpClient();
            String params = StringUtils.isNotEmpty(entity.getContent())?
                    entity.getContent() : JsonUtil.encode(entity);
            HttpPost request = new HttpPost(entity.getRequestURI());

            try {
                request.setEntity(new StringEntity(params, "GBK"));
                HttpResponse response = client.execute(request);
                String result = EntityUtils.toString(response.getEntity());
                if (result.equalsIgnoreCase("success")) {
                    LOGGER.info("[NotifierTask] Notify success, entity = [" + entity + "]");
                } else {
                    entity.setNotifyTime(System.currentTimeMillis() + NOTIFY_TIME_INTERVAL);
                    synchronized (queue) {queue.offer(entity);}
                    LOGGER.error("[NotifierTask] Notify failed, entity = [" + entity + "]");
                }
            } catch (Exception error) {
                entity.setNotifyTime(System.currentTimeMillis() + NOTIFY_TIME_INTERVAL);
                synchronized (queue) {queue.offer(entity);}
                LOGGER.error("[NotifierTask] Notify failed, entity = [" + entity + "]");
            }
        }
    }
}
