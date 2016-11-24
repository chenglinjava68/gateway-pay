package cloud.dispatcher.gateway.pay.cashier.domain.refund.thrift.support;

import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.enums.PayingPhaseEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.service.PayingService;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.enums.RefundPhaseEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.service.RefundService;
import cloud.dispatcher.gateway.pay.thrift.ThriftRefundAPI;

public class SimpleThriftRefundAPI implements ThriftRefundAPI.Iface {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleThriftRefundAPI.class);

    @Getter @Setter private PayingService simplePayingService;

    @Getter @Setter private RefundService simpleRefundService;

    /**
     * ע���µ��˿��
     *
     * @param payingId ����֧����������ID
     * @param fee �˿���,��
     * @param reason �˿�����
     * @param notify ֪ͨ��ַ
     * @param source ������Դ
     *
     * @return �˿������
     * @throws TException
     */
    @Override
    public long registerRefundOrder(String payingId, int fee, String reason, String notify,
            String source) throws TException {
        if (fee < 1 || simplePayingService.isPayingIdAvailable(payingId) == false) {
            return 0;
        }

        PayingEntity payingEntity = simplePayingService.getPayingOrder(payingId);
        PayingPhaseEnum status = PayingPhaseEnum.valueOf(payingEntity.getStatus());
        if (payingEntity == null || PayingPhaseEnum.SUCCEED != status ||
                payingEntity.getAppOrderFee() < fee) {return 0;}

        List<RefundEntity> orderList = simpleRefundService.getRefundOrder(payingId);
        int balanceAmount = payingEntity.getAppOrderFee() - fee;
        for (RefundEntity order : orderList) {
            if (RefundPhaseEnum.SUCCEED.value() == order.getStatus()) {
                balanceAmount = balanceAmount - order.getAppOrderFee();
            }
        }
        if (balanceAmount < 0) {return 0;}

        RefundEntity entity = new RefundEntity();
        Date currentDate = new Date();
        entity.setCreatedAt(currentDate);
        entity.setUpdatedAt(currentDate);
        entity.setPayingId(payingId);
        entity.setAppOrderFee(fee);
        entity.setAppOrderNotify(notify);
        entity.setAppOrderSource(source);
        entity.setAppOrderNumber(payingEntity.getAppOrderNumber());
        entity.setUsrUid(payingEntity.getUsrUid());
        entity.setOppGatewayType(payingEntity.getOppGatewayType());
        entity.setUsrAccount(payingEntity.getUsrAccount());
        entity.setUsrPayment(payingEntity.getUsrPayment());

        long primaryKey = simpleRefundService.addRefundOrder(entity);
        entity.setId(primaryKey);

        LOGGER.info("[SimpleThriftRefundAPI] Register success: entity = [" + entity + "]");
        return primaryKey;
    }

    /**
     * ��ѯ�˿��״̬
     *
     * @return 0:���������ڻ���source��ƥ�� RefundPhaseEnum.NOTEXIST.value();
     *         1:������ע�� RefundPhaseEnum.CREATED.value();
     *         2:���������� RefundPhaseEnum.OPERATING.value();
     *         3:������ʧ�� RefundPhaseEnum.FAILED.value();
     *         4:�����ѳɹ� RefundPhaseEnum.SUCCEED.value();
     */
    @Override
    public int getRefundOrderPhase(String payingId, long id, String source) throws TException {
        if (!simplePayingService.isPayingIdAvailable(payingId)) {
            return RefundPhaseEnum.NOTEXIST.value();
        } else {
            RefundEntity entity = simpleRefundService.getRefundOrder(payingId, id);
            if (entity == null) {
                return RefundPhaseEnum.NOTEXIST.value();
            } else {
                int status = entity.getStatus();
                return entity.getAppOrderSource().equals(source) ? status :
                        RefundPhaseEnum.NOTEXIST.value();
            }
        }
    }
}
