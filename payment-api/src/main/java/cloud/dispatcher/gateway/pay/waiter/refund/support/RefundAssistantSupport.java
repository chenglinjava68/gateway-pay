package cloud.dispatcher.gateway.pay.waiter.refund.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.thrift.ThriftRefundAPI;
import cloud.dispatcher.gateway.pay.thrift.pool.ConnectionProvider;
import cloud.dispatcher.gateway.pay.waiter.http.HttpClientFactory;
import cloud.dispatcher.gateway.pay.waiter.refund.RefundAssistant;
import cloud.dispatcher.gateway.pay.waiter.refund.entities.RefundParamBean;
import cloud.dispatcher.gateway.pay.waiter.utils.JsonUtil;

public class RefundAssistantSupport implements RefundAssistant {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefundAssistantSupport.class);
    
    @Getter @Setter private String serverHostName = "http://pay.localhost.localdomain";
    
    @Getter @Setter private HttpClientFactory httpClientFactory;

    @Getter @Setter private String source;

    @Getter @Setter private ConnectionProvider connectionProvider;

    /**
     * ע���µ��˿��
     *
     * @return ������
     */
    @Override
    public long registerRefundOrder(RefundParamBean param) throws Exception {
        if (!isPayingIdAvailable(param.getPayindId()) ||
            StringUtils.isEmpty(param.getReason()) ||
            StringUtils.isEmpty(param.getNotify()) || param.getFee() <= 0) {
            throw new IllegalArgumentException("Wrong parameter: param = " + param);
        }

        TSocket socket = connectionProvider.borrowConn();

        try {
            TFramedTransport transport = new TFramedTransport(socket);
            TProtocol protocol = new TBinaryProtocol(transport);
            ThriftRefundAPI.Client client = new ThriftRefundAPI.Client(protocol);
            if (!transport.isOpen())
                transport.open();
            LOGGER.info("[RefundAssistantSupport] Register, param = " + param);
            long result = client.registerRefundOrder(param.getPayindId(), param.getFee(),
                   param.getReason(), param.getNotify(), source);
            transport.close();
            if (result > 0) {
                return result;
            }
            throw new RuntimeException("Register failed, param = " + param);
        } catch (Exception error) {
            throw new RuntimeException(error);
        } finally {
            connectionProvider.returnConn(socket);
        }
    }

    /**
     * ��ѯ�˿��״̬
     *
     * @return 0:���������ڻ���source��ƥ���o���M���˿����
     *         1:������ע��
     *         2:����������
     *         3:������ʧ��
     *         4:�����ѳɹ�
     */
    @Override
    public int getRefundOrderPhase(String payingId, long id) throws Exception {
        if (!isPayingIdAvailable(payingId) || id <= 0) {
            throw new IllegalArgumentException("Wrong parameter: payingId = " +
                    "" + payingId + ", id = " + id);
        }

        TSocket socket = connectionProvider.borrowConn();

        try {
            TFramedTransport transport = new TFramedTransport(socket);
            TProtocol protocol = new TBinaryProtocol(transport);
            ThriftRefundAPI.Client client = new ThriftRefundAPI.Client(protocol);
            if (!transport.isOpen())
                transport.open();
            int result = client.getRefundOrderPhase(payingId, id, source);
            transport.close();
            return result;
        } catch (Exception error) {
            throw new RuntimeException(error);
        } finally {
            connectionProvider.returnConn(socket);
        }
    }

    public static boolean isPayingIdAvailable(String val) {
        if (StringUtils.isEmpty(val))
            return false;
        String regex = "([0-9a-f]{35})";
        return Pattern.compile(regex).matcher(val).matches();
    }

    /**
     * ��ѯ�˿������
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> getRefundOrderDetail(String payingId) {
        if (isPayingIdAvailable(payingId))
            throw new IllegalArgumentException("Wrong parameter: payingId = " + payingId);
        HttpClient client = httpClientFactory.getSimpleHttpClient();
        HttpGet request = new HttpGet(serverHostName + "/api/refundApiServlet?"
                + "payingId=" + payingId + "&source=" + source);
        try {
            HttpResponse response = client.execute(request);
            String result = EntityUtils.toString(response.getEntity());
            return StringUtils.isEmpty(result) ? Collections.EMPTY_LIST:
                JsonUtil.decode(result, List.class);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    /**
     * ��ѯ�˿������
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getRefundOrderList(String payingId, long id) {
        if (!isPayingIdAvailable(payingId)) 
            throw new IllegalArgumentException("Wrong parameter: payingId = " + payingId);
        HttpClient client = httpClientFactory.getSimpleHttpClient();
        HttpGet request = new HttpGet(serverHostName + "/api/refundApiServlet?"
                + "payingId=" + id + "&refundId=" + id + "&source=" + source);
        try {
            HttpResponse response = client.execute(request);
            String result = EntityUtils.toString(response.getEntity());
            return StringUtils.isEmpty(result) ? Collections.EMPTY_MAP: 
                JsonUtil.decode(result, Map.class);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}
