package cloud.dispatcher.gateway.pay.cashier.web.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import cloud.dispatcher.gateway.pay.cashier.domain.opp.enums.OppTypeEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.service.PayingService;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.service.RefundService;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

@Controller
public class WapNotifyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WapNotifyController.class);

    @Autowired private PayingService simplePayingService;

    @Autowired private RefundService simpleRefundService;

    @RequestMapping(value = {"/wap/notify/{oppType}/paying/{payingId}/{logId}"}, method = RequestMethod.POST)
    @ResponseBody
    public String payingNotifyAction(@PathVariable String oppType, @PathVariable long logId,
            @PathVariable String payingId, HttpServletRequest request) {
        if (oppType.equals("alipay")) {
            return alipayPayingNotify(request, payingId, logId);
        }
        if (oppType.equals("weixin")) {
            return weixinPayingNotify(request, payingId, logId);
        }
        if (oppType.equals("wxmpay")) {
            return wxmpayPayingNotify(request, payingId, logId);
        }
        return "fail";
    }

    private String alipayPayingNotify(HttpServletRequest request, String payingId, long logId) {
        Map<String, String> params = new HashMap<String,String>();
        Map requestParamsMap = request.getParameterMap();
        for (Iterator iter = requestParamsMap.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            String[] values = (String[]) requestParamsMap.get(key);
            String val = "";
            for (int i = 0; i < values.length; i++) {
                val = (i == values.length - 1) ? val + values[i] : val + values[i] + ",";
            }
            params.put(key, val);
        }

        if (params.size() == 0) {
            return "fail";
        }

        try {
            boolean result = simplePayingService.doNotify(params, payingId, logId, OppTypeEnum.ALIPAY, "wap");
            params.put("payingId", payingId);
            params.put("loggerId", String.valueOf(logId));
            LOGGER.info("[NotifyController] Alipay paying notify, result = [" + result + "], params = [" + params + "]");
            return result ? "success" : "fail";
        } catch (Exception error) {
            LOGGER.error("", error);
            return "fail";
        }
    }

    private String weixinPayingNotify(HttpServletRequest request, String payingId, long logId) {
        Map<String, String> params = new HashMap<String,String>();
        Map requestParamsMap = request.getParameterMap();
        for (Iterator iter = requestParamsMap.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            String[] values = (String[]) requestParamsMap.get(key);
            String val = "";
            for (int i = 0; i < values.length; i++) {
                val = (i == values.length - 1) ? val + values[i] : val + values[i] + ",";
            }
            params.put(key, val);
        }

        if (params.size() == 0) {
            return "fail";
        }

        try {
            params.put("__POSTDATA__", JsonUtil.encode(parsePostData(request)));
            boolean result = simplePayingService.doNotify(params, payingId, logId, OppTypeEnum.WEIXIN, "app");
            params.put("payingId", payingId);
            params.put("loggerId", String.valueOf(logId));
            LOGGER.info("[NotifyController] Weixin paying notify, result = [" + result + "], params = [" + params + "]");
            return result ? "success" : "fail";
        } catch (Exception error) {
            LOGGER.error("", error);
            return "fail";
        }
    }

    private String wxmpayPayingNotify(HttpServletRequest request, String payingId, long logId) {
        Map<String, String> params = new HashMap<String,String>();
        Map requestParamsMap = request.getParameterMap();
        for (Iterator iter = requestParamsMap.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            String[] values = (String[]) requestParamsMap.get(key);
            String val = "";
            for (int i = 0; i < values.length; i++) {
                val = (i == values.length - 1) ? val + values[i] : val + values[i] + ",";
            }
            params.put(key, val);
        }

        if (params.size() == 0) {
            return "fail";
        }

        try {
            params.put("__POSTDATA__", JsonUtil.encode(parsePostData(request)));
            boolean result = simplePayingService.doNotify(params, payingId, logId, OppTypeEnum.WXMPAY, "wap");
            params.put("payingId", payingId);
            params.put("loggerId", String.valueOf(logId));
            LOGGER.info("[NotifyController] Weixin paying notify, result = [" + result + "], params = [" + params + "]");
            return result ? "success" : "fail";
        } catch (Exception error) {
            LOGGER.error("", error);
            return "fail";
        }
    }

    @RequestMapping(value = {"/wap/notify/{oppType}/refund/{payingId}/{refundId}/{logId}"},
            method = RequestMethod.POST)
    @ResponseBody
    public String refundNotifyAction(HttpServletRequest request, @PathVariable String payingId,
            @PathVariable long refundId, @PathVariable long logId, @PathVariable String oppType) {
        if (oppType.equals("weixin")) {
            return weixinRefundNotify(request, payingId, refundId, logId);
        }
        if (oppType.equals("wxmpay")) {
            return wxmpayRefundNotify(request, payingId, refundId, logId);
        }
        return "fail";
    }

    private String weixinRefundNotify(HttpServletRequest request,
             String payingId, long refundId, long logId) {
        try {
            Map<String, String> params = parsePostData(request);
            boolean result = simpleRefundService.doNotify(params, payingId, refundId, logId);
            LOGGER.info("[NotifyController] Weixin refund notify, result = [" + result + "], params = [" + params + "]");
            return result ? "success" : "fail";
        } catch (Exception error) {
            LOGGER.error("", error);
            return "fail";
        }
    }

    private String wxmpayRefundNotify(HttpServletRequest request,
                                      String payingId, long refundId, long logId) {
        try {
            Map<String, String> params = parsePostData(request);
            boolean result = simpleRefundService.doNotify(params, payingId, refundId, logId);
            LOGGER.info("[NotifyController] Wxmpay refund notify, result = [" + result + "], params = [" + params + "]");
            return result ? "success" : "fail";
        } catch (Exception error) {
            LOGGER.error("", error);
            return "fail";
        }
    }

    private Map<String, String> parsePostData(HttpServletRequest request) {
        Map<String, String> params = new HashMap<String, String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    request.getInputStream(), "gbk"));
            String data;
            StringBuffer stringBuffer = new StringBuffer();
            while ((data = reader.readLine()) != null) {
                stringBuffer.append(data);
            }

            if (stringBuffer.length() == 0) {return Collections.EMPTY_MAP;}

            Document document = DocumentHelper.parseText(
                    stringBuffer.toString());
            List<Element> childs = document.getRootElement().elements();
            for (Element child : childs) {
                params.put(child.getName(), child.getStringValue());
            }
            return params;
        } catch (Exception error) {
            throw new RuntimeException("", error);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException error) {
                LOGGER.error("", error);
            }
        }
    }
}
