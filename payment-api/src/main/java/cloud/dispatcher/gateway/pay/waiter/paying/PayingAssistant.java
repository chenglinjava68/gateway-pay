package cloud.dispatcher.gateway.pay.waiter.paying;

import java.util.Map;

import cloud.dispatcher.gateway.pay.waiter.paying.entities.PayingParamBean;

public interface PayingAssistant {

    /**
     * ע���µ�֧������
     *
     * @return ������
     */
    public String registerPayingOrder(PayingParamBean param) throws Exception;

    /**
     * ��ѯ֧������״̬
     *
     * @return 0:���������ڻ���source��ƥ��
     *         1:������ע��
     *         2:����������
     *         3:������ʧ��
     *         4:�����ѳɹ�
     *         5:�����ѽ���
     */
    public int getPayingOrderPhase(String id) throws Exception;
    
    /**
     * ��ѯ֧����������
     */
    public Map<String, Object> getPayingOrderDetail(String id);
}
