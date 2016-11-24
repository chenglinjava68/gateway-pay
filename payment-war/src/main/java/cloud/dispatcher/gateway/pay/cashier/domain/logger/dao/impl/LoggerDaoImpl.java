package cloud.dispatcher.gateway.pay.cashier.domain.logger.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.dispatcher.gateway.pay.cashier.domain.common.dao.CommonDao;
import cloud.dispatcher.gateway.pay.cashier.domain.logger.bean.entity.LoggerEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.logger.dao.LoggerDao;

public class LoggerDaoImpl extends CommonDao implements LoggerDao {

    private static final String TABLE_PAYMENT_LOGGER_PREFIX = "payment_logger";

    private String buildTableName(String payingId) {
        return TABLE_PAYMENT_LOGGER_PREFIX + String.valueOf(
                parseTableNameSuffix(payingId));
    }

    @Override
    public void updateOnCommit(LoggerEntity entity) {
        entity.setTableName(buildTableName(entity.getPayingId()));
        this.getSqlMapClientTemplate().update(
                "LoggerDao.updateOnCommit", entity);
    }

    @Override
    public void updateOnReturn(LoggerEntity entity) {
        entity.setTableName(buildTableName(entity.getPayingId()));
        this.getSqlMapClientTemplate().update(
                "LoggerDao.updateOnReturn", entity);
    }

    @Override
    public void updateOnNotify(LoggerEntity entity) {
        entity.setTableName(buildTableName(entity.getPayingId()));
        this.getSqlMapClientTemplate().update(
                "LoggerDao.updateOnNotify", entity);
    }

    @Override
    public long insert(LoggerEntity entity) {
        entity.setTableName(buildTableName(entity.getPayingId()));
        return (Long) this.getSqlMapClientTemplate().insert(
                "LoggerDao.insert", entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<LoggerEntity> select(String payingId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("tableName", buildTableName(payingId));
        params.put("payingId", payingId);
        return this.getSqlMapClientTemplate().queryForList(
                "LoggerDao.selectList", params);
    }

    @Override
    public LoggerEntity select(long id, String payingId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("tableName", buildTableName(payingId));
        params.put("id", id);
        return (LoggerEntity) this.getSqlMapClientTemplate().queryForObject(
                "LoggerDao.selectView", params);
    }
}
