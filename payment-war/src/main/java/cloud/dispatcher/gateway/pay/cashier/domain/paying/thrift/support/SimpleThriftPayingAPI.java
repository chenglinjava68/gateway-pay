package cloud.dispatcher.gateway.pay.cashier.domain.paying.thrift.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.enums.PayingPhaseEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.service.PayingService;
import cloud.dispatcher.gateway.pay.thrift.ThriftPayingAPI;

public class SimpleThriftPayingAPI implements ThriftPayingAPI.Iface {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleThriftPayingAPI.class);

    @Getter @Setter private PayingService simplePayingService;

    /**
     * ע���µ�֧������
     *
     * @param usrParamsMap �û���ز���Map
     * @param appParamsMap ������ز���Map
     * @param oppParamsMap ֧����ز���Map
     *
     * @param source ������Դ
     *
     * @return ֧����������
     * @throws TException
     */
    @Override
    public String registerPayingOrder(Map<String, String> usrParamsMap, Map<String, String> appParamsMap,
            Map<String, String> oppParamsMap, String source) throws TException {
        PayingEntity entity = new PayingEntity();
        try {
            entity.setUsrUid(Long.valueOf(usrParamsMap.get("usrUid")));
            entity.setUsrAccount(usrParamsMap.get("usrAccount"));
            entity.setAppOrderSource(source);
            entity.setAppOrderNumber(appParamsMap.get("appOrderNumber"));
            entity.setAppOrderItem(appParamsMap.get("appOrderItem"));
            entity.setAppOrderNotify(appParamsMap.get("appOrderNotify"));
            entity.setAppOrderReturn(appParamsMap.get("appOrderReturn"));
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            entity.setAppOrderExpire(dateFormat.parse(
                    appParamsMap.get("appOrderExpire")));
            entity.setAppOrderFee(Integer.valueOf(appParamsMap.get("appOrderFee")));
            entity.setOppGatewayAuth(oppParamsMap.get("oppGatewayAuth"));
            entity.setStatus(PayingPhaseEnum.CREATED.value());
            Date currentDate = new Date();
            entity.setCreatedAt(currentDate);
            entity.setUpdatedAt(currentDate);
            String primaryKey = simplePayingService.addPayingOrder(entity);
            entity.setId(primaryKey);
            LOGGER.info("[SimpleThriftPayingAPI] Register success: entity = [" + entity + "]");
            return primaryKey;
        } catch (Exception error) {
            LOGGER.error("", error);
            return null;
        }
    }

    /**
     * ��ѯ֧������״̬
     *
     * @return 0:���������ڻ���source��ƥ�� PayingPhaseEnum.NOTEXIST.value();
     *         1:������ע�� PayingPhaseEnum.CREATED.value();
     *         2:���������� PayingPhaseEnum.OPERATING.value();
     *         3:������ʧ�� PayingPhaseEnum.FAILED.value();
     *         4:�����ѳɹ� PayingPhaseEnum.SUCCEED.value();
     *         5:�����ѽ��� PayingPhaseEnum.FINISHED.value();
     */
    @Override
    public int getPayingOrderPhase(String id, String source) throws TException {
        if (!simplePayingService.isPayingIdAvailable(id)) {
            return PayingPhaseEnum.NOTEXIST.value();
        } else {
            PayingEntity entity = simplePayingService.getPayingOrder(id);
            if (entity == null) {
                return PayingPhaseEnum.NOTEXIST.value();
            } else {
                int status = entity.getStatus();
                return entity.getAppOrderSource().equals(source) ? status :
                        PayingPhaseEnum.NOTEXIST.value();
            }
        }
    }
}
