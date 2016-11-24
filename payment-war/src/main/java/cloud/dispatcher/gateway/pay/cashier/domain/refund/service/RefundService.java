package cloud.dispatcher.gateway.pay.cashier.domain.refund.service;

import java.util.List;
import java.util.Map;

import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;

public interface RefundService {

    /**
     * �����˿ID��ȡ�����˿����������
     *
     * @param payingId ֧������ID
     * @param refundId �˿��ID
     */
    public RefundEntity getRefundOrder(String payingId, long refundId);

    /**
     * ����һ���˿����¼
     *
     * @return �˿��ID
     */
    public long addRefundOrder(RefundEntity entity);

    /**
     * ����֧����ID��ȡȫ���˿����������
     *
     * @return �˿��¼�б�
     */
    public List<RefundEntity> getRefundOrder(String payingId);

    /**
     * �ɺ�̨���̽��е��˿����
     *
     * @param entity �˿��
     */
    public boolean doRefund(RefundEntity entity);

    /**
     * ���˿�ص������ݽ��д���
     *
     * ��֤֧��ƽ̨�ص�ʱ�ṩ�������Ƿ񱻴۸Ĳ������ص�����д��
     * �˿�����ݱ���־��
     */
    public boolean doNotify(Map<String, String> params,
        String payingId, long refundId, long logId);

    /**
     * ���˿��д�봦����в����Ķ���״̬
     */
    public void doCommit(RefundEntity entity);

    /**
     * ��ȡ��δ������˿���б�
     *
     * @param suffix ���ݱ��׺
     */
    public List<RefundEntity> getUnoperating(int suffix);

    /**
     * ����ʱ�������еļ�¼�ع�ΪREG״̬
     */
    public void doRollback(RefundEntity entity);
}
