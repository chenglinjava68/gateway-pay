package cloud.dispatcher.gateway.pay.cashier.domain.logger.dao;

import java.util.List;

import cloud.dispatcher.gateway.pay.cashier.domain.logger.bean.entity.LoggerEntity;

public interface LoggerDao {

    public void updateOnCommit(LoggerEntity entity);

    public void updateOnReturn(LoggerEntity entity);

    public void updateOnNotify(LoggerEntity entity);

    public long insert(LoggerEntity entity);

    public List<LoggerEntity> select(String payingId);

    public LoggerEntity select(long id, String payingId);
}
