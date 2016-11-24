package cloud.dispatcher.gateway.pay.waiter.refund;

import java.util.List;
import java.util.Map;

import cloud.dispatcher.gateway.pay.waiter.refund.entities.RefundParamBean;

public interface RefundAssistant {

    /**
     * ע���µ��˿��
     *
     * @return ������
     */
    public long registerRefundOrder(RefundParamBean param) throws Exception;

    /**
     * ��ѯ�˿��״̬
     *
     * @return 0:���������ڻ���source��ƥ���o���M���˿����
     *         1:������ע��
     *         2:����������
     *         3:������ʧ��
     *         4:�����ѳɹ�
     */
    public int getRefundOrderPhase(String payingId, long id) throws Exception;
    
    /**
     * ��ѯ�˿������
     */
    public List<Map<String, Object>> getRefundOrderDetail(String payingId);
    
    /**
     * ��ѯ�˿������
     */
    public Map<String, Object> getRefundOrderList(String payingId, long id);
}
