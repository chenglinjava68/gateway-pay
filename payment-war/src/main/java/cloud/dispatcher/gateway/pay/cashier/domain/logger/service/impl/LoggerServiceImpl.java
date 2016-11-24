package cloud.dispatcher.gateway.pay.cashier.domain.logger.service.impl;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import cloud.dispatcher.gateway.pay.cashier.domain.logger.bean.entity.LoggerEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.logger.dao.LoggerDao;
import cloud.dispatcher.gateway.pay.cashier.domain.logger.service.LoggerService;

public class LoggerServiceImpl implements LoggerService {

    @Getter @Setter private LoggerDao simpleLoggerDao;

    @Override
    public List<LoggerEntity> getLogList(String payingId) {
        return simpleLoggerDao.select(payingId);
    }

    @Override
    public LoggerEntity getLogView(String payingId, long id) {
        return simpleLoggerDao.select(id, payingId);
    }
}
