package cloud.dispatcher.gateway.pay.cashier.domain.notifier.exectuor;

import cloud.dispatcher.gateway.pay.cashier.domain.notifier.bean.entity.NoticeEntity;

public interface NotifierExecutor {

    public void register(NoticeEntity entity);

    public void destroy();

    public void initialize();
}
