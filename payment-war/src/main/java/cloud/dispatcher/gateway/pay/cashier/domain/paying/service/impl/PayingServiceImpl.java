package cloud.dispatcher.gateway.pay.cashier.domain.paying.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;

import cloud.dispatcher.gateway.pay.cashier.domain.logger.bean.entity.LoggerEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.logger.dao.LoggerDao;
import cloud.dispatcher.gateway.pay.cashier.domain.logger.enums.LoggerResultEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.notifier.bean.entity.NoticeEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.notifier.exectuor.NotifierExecutor;
import cloud.dispatcher.gateway.pay.cashier.domain.opp.OppAssistant;
import cloud.dispatcher.gateway.pay.cashier.domain.opp.enums.OppTypeEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.dao.PayingDao;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.enums.PayingPhaseEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.service.PayingService;
import cloud.dispatcher.gateway.pay.cashier.http.HttpClientFactory;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

public class PayingServiceImpl implements PayingService {

    @Getter @Setter private NotifierExecutor simpleNotifierExecutor;

    @Getter @Setter private LoggerDao simpleLoggerDao;

    @Getter @Setter private PayingDao simplePayingDao;

    @Getter @Setter private Map<Integer, OppAssistant> simpleWebOppAssistant;

    @Getter @Setter private Map<Integer, OppAssistant> simpleWapOppAssistant;

    @Getter @Setter private Map<Integer, OppAssistant> simpleAppOppAssistant;

    @Getter @Setter private HttpClientFactory httpClientFactory;

    /**
     * ����һ��֧��������¼
     *
     * Id��DAO����UUID����, �����в��ص���setId����; ����֧������ʱ
     * , statusΪ1(PayingPhaseEnum.REGISTED), ֧����ʽΪ0(δѡ��)
     *
     * @return ֧����������
     */
    @Override
    public String addPayingOrder(PayingEntity entity) {
        return simplePayingDao.insert(entity);
    }

    /**
     * ��ȡ֧��������¼
     *
     * ��ȡ����PayingEntity������, status��oppGatewayType��Ҫ���ݶ�
     * Ӧ��ö������PayingPhaseEnum��OppTypeEnum����ת��
     *
     * @return ֧����������
     */
    @Override
    public PayingEntity getPayingOrder(String id) {
        return simplePayingDao.select(id);
    }

    /**
     * ���ж���֧������
     *
     * 1.������ѡ���֧����ʽ����֧������״̬��ΪPayingPhaseEnum.OPERATING(������)
     * 2.����֧����ʽѡ���Ӧ����������������������װ, ������Map<String, String>
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> doPaying(String id, OppTypeEnum oppType,
           String clientIP, String clientType) {
        if (!isPayingIdAvailable(id)) {
            throw new IllegalArgumentException("[PayingServiceImpl] Illegal paying order, " +
                    "id = [" + id + "]");
        }

        PayingEntity payingEntity = simplePayingDao.select(id);

        if (!isPayingOrderAvailable(payingEntity)) {
            throw new RuntimeException("[PayingServiceImpl] Illegal paying order, " +
                    "entity = [" + payingEntity + "]");
        }

        OppAssistant oppAssistant = getOppAssistant(oppType, clientType);
        Date currentTime = new Date();

        LoggerEntity logEntity = new LoggerEntity();
        logEntity.setOppType(oppType.value());
        logEntity.setOppFee(payingEntity.getAppOrderFee());
        logEntity.setPayingId(payingEntity.getId());
        logEntity.setRefundId(0);
        logEntity.setCreatedAt(currentTime);
        logEntity.setUpdatedAt(currentTime);
        long logId = simpleLoggerDao.insert(logEntity);

        Map<String, String> requestMap = oppAssistant.buildPayingRequestParamsMap(
                payingEntity, logId, clientIP);
        
        if (requestMap == null || requestMap.size() == 0) {
            return Collections.EMPTY_MAP;
        }

        logEntity.setOppCommit(JsonUtil.encode(requestMap));
        simpleLoggerDao.updateOnCommit(logEntity);

        payingEntity.setOppGatewayType(oppType.value());
        payingEntity.setStatus(PayingPhaseEnum.OPERATING.value());
        payingEntity.setUpdatedAt(currentTime);
        simplePayingDao.updateOnCommit(payingEntity);

        return requestMap;
    }

    /**
     * ����֧���ص�����
     *
     * ��֤֧��ƽ̨�ص�ʱ�ṩ�������Ƿ񱻴۸Ĳ������ص�����д��֧���������ݱ���־��
     *
     * @return �ص��Ƿ�ɹ�
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean doNotify(Map<String, String> params, String payingId,
           long logId, OppTypeEnum oppType, String clientType) {
        if (params == null || params.isEmpty()) {return false;}

        PayingEntity payingEntity = simplePayingDao.select(payingId);
        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                payingEntity.getOppGatewayAuth(), Map.class);
        OppAssistant oppAssistant = getOppAssistant(oppType, clientType);

        String feedback = JsonUtil.encode(params);
        
        Map<String, String> result = oppAssistant.parsePayingNotifyParamsMap(params, oppAuth);
        if (StringUtils.isEmpty(result.get("tradeNo"))) {
            LoggerEntity loggerEntity = simpleLoggerDao.select(
                    Long.valueOf(logId), payingId);
            loggerEntity.setStatus(LoggerResultEnum.FAILED.value());
            loggerEntity.setOppNotify(feedback);
            loggerEntity.setOppCode("");
            loggerEntity.setOppType(oppType.value());
            simpleLoggerDao.updateOnNotify(loggerEntity);

            payingEntity.setOppGatewayFeedback(feedback);
            payingEntity.setOppGatewayCode("");
            payingEntity.setUsrPayment("");
            payingEntity.setOppGatewayType(oppType.value());
            payingEntity.setStatus(PayingPhaseEnum.FAILED.value());
            simplePayingDao.updateOnNotify(payingEntity);
        } else {
            LoggerEntity loggerEntity = simpleLoggerDao.select(logId, payingId);
            loggerEntity.setStatus(LoggerResultEnum.SUCCEED.value());
            loggerEntity.setOppNotify(feedback);
            loggerEntity.setOppType(oppType.value());
            loggerEntity.setOppCode(result.get("tradeNo"));
            simpleLoggerDao.updateOnNotify(loggerEntity);

            payingEntity.setOppGatewayFeedback(feedback);
            payingEntity.setOppGatewayCode(result.get("tradeNo"));
            String account = result.get("account");
            payingEntity.setUsrPayment(
                    StringUtils.isNotEmpty(account) ? account : "");
            payingEntity.setOppGatewayType(oppType.value());
            payingEntity.setStatus(PayingPhaseEnum.SUCCEED.value());
            simplePayingDao.updateOnNotify(payingEntity);

            NoticeEntity noticeEntity = new NoticeEntity();
            noticeEntity.setResult(true);
            noticeEntity.setRequestURI(payingEntity.getAppOrderNotify());
            noticeEntity.setLoggerId(String.valueOf(logId));
            noticeEntity.setPayingId(payingId);
            noticeEntity.setRefundId("0");
            noticeEntity.setOppCode(result.get("tradeNo"));
            noticeEntity.setOppType(String.valueOf(oppType.value()));
            simpleNotifierExecutor.register(noticeEntity);
        }

        return true;
    }

    /**
     * ����֧�����ز���
     *
     * ��֤֧��ƽ̨�ص�ʱ�ṩ�������Ƿ񱻴۸Ĳ������ص�����д��֧���������ݱ���־��
     *
     * @return ������ת��ַ
     */
    @SuppressWarnings("unchecked")
    @Override
    public String doReturn(Map<String, String> params, String payingId,
           long logId, OppTypeEnum oppType, String clientType) {
        if (params == null || params.isEmpty()) {return "";}

        PayingEntity payingEntity = simplePayingDao.select(payingId);
        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                payingEntity.getOppGatewayAuth(), Map.class);
        OppAssistant oppAssistant = getOppAssistant(oppType, clientType);

        String feedback = JsonUtil.encode(params);
        
        Map<String, String> result = oppAssistant.parsePayingReturnParamsMap(params, oppAuth);
        if (StringUtils.isEmpty(result.get("tradeNo"))) {
            LoggerEntity loggerEntity = simpleLoggerDao.select(logId, payingId);
            loggerEntity.setStatus(LoggerResultEnum.FAILED.value());
            loggerEntity.setOppReturn(feedback);
            loggerEntity.setOppCode("");
            loggerEntity.setOppType(oppType.value());
            simpleLoggerDao.updateOnReturn(loggerEntity);

            payingEntity.setOppGatewayFeedback(feedback);
            payingEntity.setStatus(PayingPhaseEnum.FAILED.value());
            payingEntity.setUsrPayment("");
            payingEntity.setOppGatewayType(oppType.value());
            payingEntity.setOppGatewayCode("");
            simplePayingDao.updateOnNotify(payingEntity);
        } else {
            LoggerEntity loggerEntity = simpleLoggerDao.select(logId, payingId);
            loggerEntity.setStatus(LoggerResultEnum.SUCCEED.value());
            loggerEntity.setOppReturn(feedback);
            loggerEntity.setOppType(oppType.value());
            loggerEntity.setOppCode(result.get("tradeNo"));
            simpleLoggerDao.updateOnReturn(loggerEntity);

            payingEntity.setOppGatewayFeedback(feedback);
            payingEntity.setStatus(PayingPhaseEnum.SUCCEED.value());
            String account = result.get("account");
            payingEntity.setUsrPayment(
                    StringUtils.isNotEmpty(account) ? account : "");
            payingEntity.setOppGatewayType(oppType.value());
            payingEntity.setOppGatewayCode(result.get("tradeNo"));
            simplePayingDao.updateOnNotify(payingEntity);
        }

        return payingEntity.getAppOrderReturn();
    }

    /**
     * ����֧�����ͻ�ȡOppAssistant
     *
     * @return oppAssistant
     */
    private OppAssistant getOppAssistant(OppTypeEnum oppType, String clientType) {
        if (clientType.equalsIgnoreCase("web")) {
            return simpleWebOppAssistant.get(oppType.value());
        }

        if (clientType.equalsIgnoreCase("wap")) {
            return simpleWapOppAssistant.get(oppType.value());
        }

        if (clientType.equalsIgnoreCase("app")) {
            return simpleAppOppAssistant.get(oppType.value());
        }

        return null;
    }

    /**
     * ��ⶩ�����Ƿ���Ч
     *
     * ��ⶩ���Ÿ�ʽ�Ƿ�����([0-9a-f]{35})����������
     */
    public boolean isPayingIdAvailable(String id) {
        return Pattern.compile("([0-9a-f]{35})").matcher(id).matches();
    }

    /**
     * ���֧�������Ƿ���Ч
     *
     * ���֧�������Ƿ���Ч, ���֧�����������ڻ򶩵��ѹ��ڻ��Ѿ�֧����
     * ��, �򶩵�Ϊ��Ч״̬
     */
    @Override
    public boolean isPayingOrderAvailable(PayingEntity entity) {
        return (entity == null || entity.getStatus() == PayingPhaseEnum.SUCCEED.value() ||
                entity.getAppOrderExpire().before(new Date())) ? false : true;
    }
}
