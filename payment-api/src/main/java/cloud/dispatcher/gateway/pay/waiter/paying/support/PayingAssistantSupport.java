package cloud.dispatcher.gateway.pay.waiter.paying.support;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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

import cloud.dispatcher.gateway.pay.thrift.ThriftPayingAPI;
import cloud.dispatcher.gateway.pay.thrift.pool.ConnectionProvider;
import cloud.dispatcher.gateway.pay.waiter.http.HttpClientFactory;
import cloud.dispatcher.gateway.pay.waiter.paying.PayingAssistant;
import cloud.dispatcher.gateway.pay.waiter.paying.entities.PayingParamBean;
import cloud.dispatcher.gateway.pay.waiter.utils.CodeUtil;
import cloud.dispatcher.gateway.pay.waiter.utils.JsonUtil;

public class PayingAssistantSupport implements PayingAssistant {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayingAssistantSupport.class);
    
    @Getter @Setter private String serverHostName = "http://pay.localhost.localdomain";
    
    @Getter @Setter private HttpClientFactory httpClientFactory;

    @Setter @Getter private String source;

    @Setter @Getter private ConnectionProvider connectionProvider;

    /**
     * ע���µ�֧������
     *
     * @return ������
     */
    @Override
    public String registerPayingOrder(PayingParamBean param) throws Exception {
        TSocket socket = connectionProvider.borrowConn();
        try {
            TFramedTransport transport = new TFramedTransport(socket);
            Map<String, String> usrMap = buildUsrParamsMap(param);
            Map<String, String> appMap = buildAppParamsMap(param);
            Map<String, String> oppMap = buildOppParamsMap(param);
            TProtocol protocol = new TBinaryProtocol(transport);
            ThriftPayingAPI.Client client = new ThriftPayingAPI.Client(protocol);
            if (!transport.isOpen())
                transport.open();
            LOGGER.info("[PayingAssistantSupport] Register, param = " + param);
            String result = client.registerPayingOrder(usrMap, appMap, oppMap, source);
            transport.close();
            if (StringUtils.isNotEmpty(result)) {
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
     * �����û���ز���Map [UID, USERNAME]
     *
     * @return usrMap
     */
    private Map<String, String> buildUsrParamsMap(PayingParamBean param) {
        Map<String, String> data = new HashMap<String, String>();

        if (param.getUid() <= 0 || StringUtils.isEmpty(param.getUsername())) {
            throw new IllegalArgumentException("Wrong parameter: uid = " +
                    "" + param.getUid() + ", username: " + param.getUsername());
        } else {
            data.put("usrUid", String.valueOf(param.getUid()));
            data.put("usrAccount", param.getUsername());
            return data;
        }
    }

    /**
     * ���ɶ�����ز���Map [ORDERNUM, ITEMNAME, ITEMDESC, ITEMLINK, NOTIFYURL,
     * RETURNURL, EXPIRE, FEE]
     *
     * @return appMap
     */
    private Map<String, String> buildAppParamsMap(PayingParamBean param) {
        Map<String, String> data = new HashMap<String, String>();

        if (StringUtils.isEmpty(param.getOrderNum())) {
            throw new IllegalArgumentException("Wrong parameter: orderNum = " +
                    param.getOrderNum());
        }
        data.put("appOrderNumber", CodeUtil.md5Hex(source + param.getOrderNum()));

        if (StringUtils.isEmpty(param.getItemName()) ||
            !isItemNameAvailable(param.getItemName())) {
            throw new IllegalArgumentException("Wrong parameter: itemName = " +
                    param.getItemName());
        }
        if (!StringUtils.isEmpty(param.getItemDesc()) &&
            !isItemDescAvailable(param.getItemDesc())) {
            throw new IllegalArgumentException("Wrong parameter: itemDesc = " +
                    param.getItemDesc());
        }
        Map<String, String> itemMap = new HashMap<String, String>();
        itemMap.put("name", param.getItemName());
        itemMap.put("desc", param.getItemDesc());
        itemMap.put("link", param.getItemLink());
        data.put("appOrderItem", JsonUtil.encode(itemMap));

        if (StringUtils.isEmpty(param.getNotifyURL())) {
            throw new IllegalArgumentException("Wrong parameter: notifyURL = " +
                    param.getNotifyURL());
        }
        data.put("appOrderNotify", param.getNotifyURL());

        if (StringUtils.isEmpty(param.getReturnURL())) {
            throw new IllegalArgumentException("Wrong parameter: returnURL = " +
                    param.getReturnURL());
        }
        data.put("appOrderReturn", param.getReturnURL());

        if (param.getExpire() == null || param.getExpire().before(new Date())) {
            throw new IllegalArgumentException("Wrong parameter: expire = " +
                    param.getExpire());
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        data.put("appOrderExpire", dateFormat.format(param.getExpire()));

        if (param.getFee() <= 0) {
            throw new IllegalArgumentException("Wrong parameter: fee = " +
                    param.getFee());
        }
        data.put("appOrderFee", String.valueOf(param.getFee()));

        return data;
    }

    /**
     * ����֧����֤����Map [ALIPAY, WEIXIN]
     *
     * @return oppMap
     */
    private Map<String, String> buildOppParamsMap(PayingParamBean param) {
        Map<String, String> data = new HashMap<String, String>();

        Map<String, String> alipayAccount = param.getAlipayAccount();
        Map<String, String> weixinAccount = param.getWeixinAccount();
        Map<String, String> wxmpayAccount = param.getWxmpayAccount();

        if (((alipayAccount == null || alipayAccount.size() == 0) &&
             (weixinAccount == null || weixinAccount.size() == 0) &&
             (wxmpayAccount == null || wxmpayAccount.size() == 0))) {
            throw new IllegalArgumentException("Wrong parameter: alipayAccount = " +
                    alipayAccount + ", weixinAccount = " +
                    weixinAccount + ", wxmpayAccount = " + wxmpayAccount);
        }

        Map<String, Map<String, String>> accountMap = new HashMap<String, Map<String, String>>();

        if (alipayAccount != null && alipayAccount.size() > 0) {
            if (StringUtils.isEmpty(alipayAccount.get("seller_email")) ||
                StringUtils.isEmpty(alipayAccount.get("partner")) ||
                StringUtils.isEmpty(alipayAccount.get("key"))) {
                throw new IllegalArgumentException("Wrong parameter: alipayAccount = " +
                        param.getAlipayAccount());
            }
            Map<String, String> account = new HashMap<String, String>();
            account.put("seller_email", alipayAccount.get("seller_email"));
            account.put("key", alipayAccount.get("key"));
            account.put("partner", alipayAccount.get("partner"));

            if (StringUtils.isNotEmpty(alipayAccount.get("rsa_pri_key"))) {
                account.put("rsa_pri_key", alipayAccount.get("rsa_pri_key"));
            }

            if (StringUtils.isNotEmpty(alipayAccount.get("rsa_pub_key"))) {
                account.put("rsa_pub_key", alipayAccount.get("rsa_pub_key"));
            }

            accountMap.put("alipay", account);
        }

        if (weixinAccount != null && weixinAccount.size() > 0) {
            if (StringUtils.isEmpty(weixinAccount.get("partkey")) ||
                StringUtils.isEmpty(weixinAccount.get("partner")) ||
                StringUtils.isEmpty(weixinAccount.get("passwd")) ||
                StringUtils.isEmpty(weixinAccount.get("app_id")) ||
                StringUtils.isEmpty(weixinAccount.get("secret")) ||
                StringUtils.isEmpty(weixinAccount.get("appkey"))) {
                throw new IllegalArgumentException("Wrong parameter: weixinAccount = " +
                        param.getWeixinAccount());
            }
            Map<String, String> account = new HashMap<String, String>();
            account.put("partner", weixinAccount.get("partner"));
            account.put("partkey", weixinAccount.get("partkey"));
            account.put("passwd", weixinAccount.get("passwd"));
            account.put("app_id", weixinAccount.get("app_id"));
            account.put("secret", weixinAccount.get("secret"));
            account.put("appkey", weixinAccount.get("appkey"));
            accountMap.put("weixin", account);
        }

        if (wxmpayAccount != null && wxmpayAccount.size() > 0) {
            if (StringUtils.isEmpty(wxmpayAccount.get("partkey")) ||
                    StringUtils.isEmpty(wxmpayAccount.get("partner")) ||
                    StringUtils.isEmpty(wxmpayAccount.get("passwd")) ||
                    StringUtils.isEmpty(wxmpayAccount.get("app_id")) ||
                    StringUtils.isEmpty(wxmpayAccount.get("secret")) ||
                    StringUtils.isEmpty(wxmpayAccount.get("appkey"))) {
                throw new IllegalArgumentException("Wrong parameter: wxmpayAccount = " +
                        param.getWxmpayAccount());
            }
            Map<String, String> account = new HashMap<String, String>();
            account.put("partner", wxmpayAccount.get("partner"));
            account.put("partkey", wxmpayAccount.get("partkey"));
            account.put("passwd", wxmpayAccount.get("passwd"));
            account.put("app_id", wxmpayAccount.get("app_id"));
            account.put("secret", wxmpayAccount.get("secret"));
            account.put("appkey", wxmpayAccount.get("appkey"));
            accountMap.put("wxmpay", account);
        }

        data.put("oppGatewayAuth", JsonUtil.encode(accountMap));

        return data;
    }

    /**
     * ��ѯ����֧���׶�
     *
     * @return 0:���������ڻ���source��ƥ��
     *         1:������ע��
     *         2:����������
     *         3:������ʧ��
     *         4:�����ѳɹ�
     *         5:�����ѽ���
     */
    @Override
    public int getPayingOrderPhase(String id) throws Exception {
        if (!isPayingIdAvailable(id))
            throw new IllegalArgumentException("Wrong parameter: id = " + id);

        TSocket socket = connectionProvider.borrowConn();
        try {
            TFramedTransport transport = new TFramedTransport(socket);
            TProtocol protocol = new TBinaryProtocol(transport);
            ThriftPayingAPI.Client client = new ThriftPayingAPI.Client(protocol);
            if (!transport.isOpen())
                transport.open();
            int result = client.getPayingOrderPhase(id, source);
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

    public static boolean isItemNameAvailable(String val) {
        if (StringUtils.isEmpty(val))
            return false;
        String regex = "^[-~#*().A-Za-z0-9\u4e00-\u9fa5\u3002\uff1b\uff0c\uff1a\u201c\u201d\uff08"
                + "\uff09\u3001\uff1f\u300a\u300b\u2014\uff01\u3010\u3011\u0020\u3000]+$";
        return Pattern.compile(regex).matcher(val).matches();
    }

    public static boolean isItemDescAvailable(String val) {
        if (StringUtils.isEmpty(val))
            return false;
        String regex = "^[-~#*().A-Za-z0-9\u4e00-\u9fa5\u3002\uff1b\uff0c\uff1a\u201c\u201d\uff08"
                + "\uff09\u3001\uff1f\u300a\u300b\u2014\uff01\u3010\u3011\u0020\u3000]+$";
        return Pattern.compile(regex).matcher(val).matches();
    }

    /**
     * ��ѯ֧����������
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getPayingOrderDetail(String id) {
        if (!isPayingIdAvailable(id)) 
            throw new IllegalArgumentException("Wrong parameter: id = " + id);
        HttpClient client = httpClientFactory.getSimpleHttpClient();
        HttpGet request = new HttpGet(serverHostName + "/api/payingApiServlet?payingId=" + id + "&source=" + source);
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
