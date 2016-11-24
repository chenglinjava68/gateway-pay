package cloud.dispatcher.gateway.pay.cashier.domain.logger.service;

import java.util.List;

import cloud.dispatcher.gateway.pay.cashier.domain.logger.bean.entity.LoggerEntity;

public interface LoggerService {

    public LoggerEntity getLogView(String payingId, long id);

    public List<LoggerEntity> getLogList(String payingId);
}
