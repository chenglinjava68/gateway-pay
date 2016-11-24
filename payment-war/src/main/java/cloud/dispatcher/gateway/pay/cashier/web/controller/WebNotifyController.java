package cloud.dispatcher.gateway.pay.cashier.web.controller;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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

@Controller
public class WebNotifyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebNotifyController.class);

    @Autowired private PayingService simplePayingService;

    @Autowired private RefundService simpleRefundService;

    @RequestMapping(value = {"/notify/alipay/paying/{payingId}/{logId}"}, method = RequestMethod.POST)
    @ResponseBody
    public String payingNotifyAction(@PathVariable String payingId, 
            @PathVariable long logId, HttpServletRequest request) {
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
            boolean result = simplePayingService.doNotify(params, payingId, logId, OppTypeEnum.ALIPAY, "web");
            LOGGER.info("[NotifyController] Alipay paying notify, result = [" + result + "], params = [" + params + "]");
            return result ? "success" : "fail";
        } catch (Exception error) {
            LOGGER.error("", error);
            return "fail";
        }
    }

    @RequestMapping(value = {"/notify/alipay/refund/{payingId}/{refundId}/{logId}"},
            method = RequestMethod.POST)
    @ResponseBody
    public String refundNotifyAction(@PathVariable String payingId, 
            @PathVariable long refundId, @PathVariable long logId, 
            HttpServletRequest request) throws UnsupportedEncodingException {
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
            boolean result = simpleRefundService.doNotify(params, payingId, refundId, logId);
            params.put("payingId", payingId);
            params.put("loggerId", String.valueOf(logId));
            LOGGER.info("[NotifyController] Alipay refund notify, result = [" + result + "], params = [" + params + "]");
            return result ? "success" : "fail";
        } catch (Exception error) {
            LOGGER.error("", error);
            return "fail";
        }
    }
}
