package cloud.dispatcher.gateway.pay.cashier.domain.paying.service;

import java.util.Map;

import cloud.dispatcher.gateway.pay.cashier.domain.opp.enums.OppTypeEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;

public interface PayingService {

    /**
     * ����һ��֧��������¼
     *
     * Id��DAO����UUID����, �����в��ص���setId����; ����֧������ʱ
     * , statusΪ1(PayingPhaseEnum.REGISTED), ֧����ʽΪ0(δѡ��)
     *
     * @return ֧����������
     */
    public String addPayingOrder(PayingEntity entity);

    /**
     * ��ȡ֧��������¼
     *
     * ��ȡ����PayingEntity������, status��oppGatewayType��Ҫ���ݶ�
     * Ӧ��ö������PayingPhaseEnum��OppTypeEnum����ת��
     *
     * @return ֧����������
     */
    public PayingEntity getPayingOrder(String id);

    /**
     * ���ж���֧������
     *
     * 1.������ѡ���֧����ʽ����֧������״̬��ΪPayingPhaseEnum.OPERATING(������)
     * 2.����֧����ʽѡ���Ӧ����������������������װ, ������Map<String, String>
     *
     * @return ҳ���ύ����
     */
    public Map<String, String> doPaying(String id, OppTypeEnum oppType,
           String clientIP, String clientType);

    /**
     * ����֧���ص�����
     *
     * ��֤֧��ƽ̨�ص�ʱ�ṩ�������Ƿ񱻴۸Ĳ������ص�����д��֧���������ݱ���־��
     *
     * @return �����Ƿ�ɹ�
     */
    public boolean doNotify(Map<String, String> params, String payingId,
           long logId, OppTypeEnum oppType, String clientType);

    /**
     * ����֧�����ز���
     *
     * ��֤֧��ƽ̨�ص�ʱ�ṩ�������Ƿ񱻴۸Ĳ������ص�����д��֧���������ݱ���־��
     *
     * @return ������ת��ַ
     */
    public String doReturn(Map<String, String> params, String payingId,
           long logId, OppTypeEnum oppType, String clientType);

    /**
     * ��ⶩ�����Ƿ���Ч
     *
     * ��ⶩ���Ÿ�ʽ�Ƿ�����([0-9a-f]{35})����������
     */
    public boolean isPayingIdAvailable(String id);

    /**
     * ���֧�������Ƿ���Ч
     *
     * ���֧�������Ƿ���Ч, ���֧�����������ڻ򶩵��ѹ��ڻ��Ѿ�֧����
     * ��, �򶩵�Ϊ��Ч״̬
     */
    public boolean isPayingOrderAvailable(PayingEntity entity);
}
