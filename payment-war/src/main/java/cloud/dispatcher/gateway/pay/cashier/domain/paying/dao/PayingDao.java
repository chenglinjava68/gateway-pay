package cloud.dispatcher.gateway.pay.cashier.domain.paying.dao;

import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;

public interface PayingDao {

    /**
     * ����֧��������Ϣ
     *
     * ���ύ֧��ƽ̨ǰ����֧���������������, ����֧��������״̬Ϊ
     * PayingPhaseEnum.OPERATING
     */
    public void updateOnCommit(PayingEntity entity);

    /**
     * ����֧��������Ϣ
     *
     * ��֧��ƽ̨�ص������֧���������������, ����֧��������״̬Ϊ
     * PayingPhaseEnum.FAILED �� PayingPhaseEnum.SUCCEED
     */
    public void updateOnNotify(PayingEntity entity);
    
    /**
     * ����֧��������Ϣ
     *
     * ��֧��ƽ̨�ص������֧���������������, ����֧��������״̬Ϊ
     * PayingPhaseEnum.FAILED �� PayingPhaseEnum.SUCCEED
     */
    public void updateOnReturn(PayingEntity entity);

    /**
     * ����ID��ȡ֧����������
     *
     * ��ȡ����PayingEntity������, status��oppGatewayType��Ҫ���ݶ�
     * Ӧ��ö������PayingPhaseEnum��OppTypeEnum����ת��
     *
     * @return ֧����������
     */
    public PayingEntity select(String id);

    /**
     * ����в���֧����������
     *
     * Id��DAO����UUID����, �����в��ص���setId����; ����֧������ʱ
     * , statusΪ1(PayingPhaseEnum.REGISTED), ֧����ʽΪ0(δѡ��)
     *
     * @return ֧����������
     */
    public String insert(PayingEntity entity);
}
