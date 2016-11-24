package cloud.dispatcher.gateway.pay.cashier.domain.refund.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.dispatcher.gateway.pay.cashier.domain.common.dao.CommonDao;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.dao.RefundDao;

public class RefundDaoImpl extends CommonDao implements RefundDao {

    private static final String TABLE_PAYMENT_REFUND_NAME_PREFIX = "payment_refund";

    /**
     * ����֧�������Ż�ȡ�������ڱ�ı���
     */
    private String buildTableName(String id) {
        return TABLE_PAYMENT_REFUND_NAME_PREFIX + String.valueOf(
                parseTableNameSuffix(id));
    }

    /**
     * ��ȡ100��δ�����˿���б�
     *
     * @param suffix ���ݱ��׺
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<RefundEntity> selectUnoperating(int suffix) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("tableName", TABLE_PAYMENT_REFUND_NAME_PREFIX +
                String.valueOf(suffix));
        params.put("limit", 100);
        return this.getSqlMapClientTemplate().queryForList(
                "RefundDao.selectUnoperating", params);
    }

    /**
     * �����˿ID��ȡ�����˿����������
     *
     * @param payingId ֧������ID
     * @param refundId �˿��ID
     */
    @Override
    public RefundEntity select(String payingId, long refundId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("tableName", buildTableName(payingId));
        params.put("payingId", payingId);
        params.put("refundId", refundId);
        return (RefundEntity) this.getSqlMapClientTemplate().queryForObject(
                "RefundDao.selectView", params);
    }

    @Override
    public long insert(RefundEntity entity) {
        entity.setTableName(buildTableName(entity.getPayingId()));
        return (Long) this.getSqlMapClientTemplate().insert(
                "RefundDao.insert", entity);
    }

    /**
     * ����֧����ID��ȡȫ���˿����������
     *
     * @return �˿��¼�б�
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<RefundEntity> select(String payingId) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("tableName", buildTableName(payingId));
        params.put("payingId", payingId);
        return this.getSqlMapClientTemplate().queryForList(
                "RefundDao.selectList", params);
    }

    /**
     * �˿����ص���Ϣ��״̬
     *
     * �ύ�˿������Ǹ����˿����ص���Ϣ��״̬
     */
    @Override
    public void updateOnCommit(RefundEntity entity) {
        entity.setTableName(buildTableName(entity.getPayingId()));
        this.getSqlMapClientTemplate().update(
                "RefundDao.updateOnCommit", entity);
    }

    /**
     * �����˿����Ϣ
     *
     * ֧��ƽ̨�ص�ʱ�����˿����ص���Ϣ��״̬
     */
    @Override
    public void updateOnNotify(RefundEntity entity) {
        entity.setTableName(buildTableName(entity.getPayingId()));
        this.getSqlMapClientTemplate().update(
                "RefundDao.updateOnNotify", entity);
    }

    /**
     * ����ʱ�������еļ�¼�ع�ΪREG״̬
     */
    @Override
    public void updateOnRollback(RefundEntity entity) {
        entity.setTableName(buildTableName(entity.getPayingId()));
        this.getSqlMapClientTemplate().update(
                "RefundDao.updateOnRollback", entity);
    }
}
