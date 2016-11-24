package cloud.dispatcher.gateway.pay.cashier.domain.refund.dao;

import java.util.List;

import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;

public interface RefundDao {

    /**
     * �����˿ID��ȡ�����˿����������
     *
     * @param payingId ֧������ID
     * @param refundId �˿��ID
     */
    public RefundEntity select(String payingId, long refundId);

    /**
     * ����֧����ID��ȡȫ���˿����������
     *
     * @return �˿��¼�б�
     */
    public List<RefundEntity> select(String payingId);

    /**
     * ��ȡ100��δ�����˿���б�
     *
     * @param suffix ���ݱ��׺
     */
    public List<RefundEntity> selectUnoperating(int suffix);

    /**
     * �����˿����Ϣ
     *
     * �ύ�˿������Ǹ����˿����ص���Ϣ��״̬
     */
    public void updateOnCommit(RefundEntity entity);

    /**
     * �����˿����Ϣ
     *
     * ֧��ƽ̨�ص�ʱ�����˿����ص���Ϣ��״̬
     */
    public void updateOnNotify(RefundEntity entity);

    /**
     * ����һ���˿����¼
     *
     * @return �˿��ID
     */
    public long insert(RefundEntity entity);

    /**
     * ����ʱ�������еļ�¼�ع�ΪREG״̬
     */
    public void updateOnRollback(RefundEntity entity);
}
