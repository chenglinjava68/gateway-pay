package cloud.dispatcher.gateway.pay.cashier.domain.refund.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import cloud.dispatcher.gateway.pay.cashier.domain.logger.bean.entity.LoggerEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.logger.dao.LoggerDao;
import cloud.dispatcher.gateway.pay.cashier.domain.logger.enums.LoggerResultEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.notifier.bean.entity.NoticeEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.notifier.exectuor.NotifierExecutor;
import cloud.dispatcher.gateway.pay.cashier.domain.opp.OppAssistant;
import cloud.dispatcher.gateway.pay.cashier.domain.opp.enums.OppTypeEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.dao.PayingDao;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.dao.RefundDao;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.enums.RefundPhaseEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.service.RefundService;
import cloud.dispatcher.gateway.pay.cashier.http.HttpClientFactory;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

public class RefundServiceImpl implements RefundService {

    @Getter @Setter private Map<Integer, OppAssistant> simpleWebOppAssistant;

    @Getter @Setter private Map<Integer, OppAssistant> simpleWapOppAssistant;

    @Getter @Setter private Map<Integer, OppAssistant> simpleAppOppAssistant;

    @Getter @Setter private LoggerDao simpleLoggerDao;

    @Getter @Setter private RefundDao simpleRefundDao;

    @Getter @Setter private PayingDao simplePayingDao;

    @Getter @Setter private NotifierExecutor simpleNotifierExecutor;

    @Getter @Setter private HttpClientFactory httpClientFactory;

    /**
     * �����˿ID��ȡ�����˿����������
     *
     * @param payingId ֧������ID
     * @param refundId �˿��ID
     */
    @Override
    public RefundEntity getRefundOrder(String payingId, long refundId) {
        return simpleRefundDao.select(payingId, refundId);
    }

    /**
     * ����һ���˿����¼
     *
     * @return �˿��ID
     */
    @Override
    public long addRefundOrder(RefundEntity entity) {
        return simpleRefundDao.insert(entity);
    }

    /**
     * ����֧����ID��ȡȫ���˿����������
     *
     * @return �˿��¼�б�
     */
    @Override
    public List<RefundEntity> getRefundOrder(String payingId) {
        return simpleRefundDao.select(payingId);
    }

    /**
     * �ɺ�̨���̽��е��˿����
     *
     * @param refundEntity �˿��
     */
    @Override
    public boolean doRefund(RefundEntity refundEntity) {
        PayingEntity payingEntity = simplePayingDao.select(refundEntity.getPayingId());
        OppAssistant oppAssistant = getOppAssistant(OppTypeEnum.valueOf(
                refundEntity.getOppGatewayType()));
        Date currentTime = new Date();
        LoggerEntity loggerEntity = new LoggerEntity();
        loggerEntity.setPayingId(refundEntity.getPayingId());
        loggerEntity.setOppType(refundEntity.getOppGatewayType());
        loggerEntity.setOppFee(refundEntity.getAppOrderFee());
        loggerEntity.setRefundId(refundEntity.getId());
        loggerEntity.setCreatedAt(currentTime);
        loggerEntity.setUpdatedAt(currentTime);
        long logId = simpleLoggerDao.insert(loggerEntity);
        loggerEntity.setId(logId);
        Map<String, String> paramsMap = oppAssistant.buildRefundRequestParamsMap(
                payingEntity, refundEntity, logId);
        loggerEntity.setOppCommit(JsonUtil.encode(paramsMap));
        simpleLoggerDao.updateOnCommit(loggerEntity);

        if (oppAssistant.commitRefundRequest(paramsMap)) {
            return true;
        }

        loggerEntity.setStatus(LoggerResultEnum.FAILED.value());
        loggerEntity.setOppNotify("");
        loggerEntity.setOppCode(payingEntity.getOppGatewayCode());
        simpleLoggerDao.updateOnNotify(loggerEntity);
        refundEntity.setStatus(RefundPhaseEnum.FAILED.value());
        refundEntity.setOppGatewayFeedback("");
        refundEntity.setOppGatewayCode(payingEntity.getOppGatewayCode());
        simpleRefundDao.updateOnNotify(refundEntity);

        NoticeEntity noticeEntity = new NoticeEntity();
        noticeEntity.setRefundId(String.valueOf(refundEntity.getId()));
        noticeEntity.setPayingId(refundEntity.getPayingId());
        noticeEntity.setOppCode(payingEntity.getOppGatewayCode());
        noticeEntity.setResult(false);
        noticeEntity.setRequestURI(refundEntity.getAppOrderNotify());
        noticeEntity.setLoggerId(String.valueOf(logId));

        noticeEntity.setOppType(String.valueOf(refundEntity.getOppGatewayType()));
        simpleNotifierExecutor.register(noticeEntity);

        return false;
    }

    /**
     * ���˿�ص������ݽ��д���
     *
     * ��֤֧��ƽ̨�ص�ʱ�ṩ�������Ƿ񱻴۸Ĳ������ص�����д��
     * �˿�����ݱ���־��
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean doNotify(Map<String, String> params, String payingId, long refundId, long logId) {
        if (params == null || params.isEmpty()) {return false;}

        RefundEntity refundEntity = simpleRefundDao.select(payingId, refundId);
        PayingEntity payingEntity = simplePayingDao.select(payingId);
        OppAssistant oppAssistant = getOppAssistant(OppTypeEnum.valueOf(
                payingEntity.getOppGatewayType()));
        Map<String, String> result = oppAssistant.parseRefundNotifyParamsMap(
                params, JsonUtil.decode(payingEntity.getOppGatewayAuth(), Map.class));
        String feedback = JsonUtil.encode(params);

        LoggerEntity loggerEntity = simpleLoggerDao.select(logId, payingId);
        loggerEntity.setOppNotify(feedback);
        loggerEntity.setOppCode(payingEntity.getOppGatewayCode());

        NoticeEntity noticeEntity = new NoticeEntity();

        if (!result.isEmpty()) {
            loggerEntity.setStatus(LoggerResultEnum.SUCCEED.value());
            refundEntity.setStatus(RefundPhaseEnum.SUCCEED.value());
            noticeEntity.setResult(true);
            noticeEntity.setOppCode(payingEntity.getOppGatewayCode());
        } else {
            loggerEntity.setStatus(LoggerResultEnum.FAILED.value());
            refundEntity.setStatus(RefundPhaseEnum.FAILED.value());
            noticeEntity.setResult(false);
            noticeEntity.setOppCode(payingEntity.getOppGatewayCode());
        }

        noticeEntity.setRequestURI(refundEntity.getAppOrderNotify());
        noticeEntity.setLoggerId(String.valueOf(logId));
        noticeEntity.setPayingId(payingId);
        noticeEntity.setRefundId(String.valueOf(refundId));

        noticeEntity.setOppType(String.valueOf(refundEntity.getOppGatewayType()));
        simpleNotifierExecutor.register(noticeEntity);

        simpleLoggerDao.updateOnNotify(loggerEntity);

        refundEntity.setOppGatewayFeedback(feedback);
        refundEntity.setOppGatewayCode(payingEntity.getOppGatewayCode());
        simpleRefundDao.updateOnNotify(refundEntity);

        return true;
    }

    /**
     * ���˿��д�봦����в����Ķ���״̬
     */
    @Override
    public void doCommit(RefundEntity entity) {
        simpleRefundDao.updateOnCommit(entity);
    }

    /**
     * ��ȡ��δ������˿���б�
     *
     * @param suffix ���ݱ��׺
     */
    @Override
    public List<RefundEntity> getUnoperating(int suffix) {
        return simpleRefundDao.selectUnoperating(suffix);
    }

    /**
     * ����ʱ�������еļ�¼�ع�ΪREG״̬
     */
    @Override
    public void doRollback(RefundEntity entity) {
        simpleRefundDao.updateOnRollback(entity);
    }

    /**
     * ����֧�����ͻ�ȡOppAssistant
     *
     * @return oppAssistant
     */
    private OppAssistant getOppAssistant(OppTypeEnum oppType) {
        if (oppType == OppTypeEnum.ALIPAY) {
            return simpleWebOppAssistant.get(oppType.value());
        }
        if (oppType == OppTypeEnum.WEIXIN) {
            return simpleAppOppAssistant.get(oppType.value());
        }
        if (oppType == OppTypeEnum.WXMPAY) {
            return simpleWapOppAssistant.get(oppType.value());
        }
        return null;
    }
}
