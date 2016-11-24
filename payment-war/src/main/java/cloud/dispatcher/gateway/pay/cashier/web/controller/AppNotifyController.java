package cloud.dispatcher.gateway.pay.cashier.web.controller;


import cloud.dispatcher.gateway.pay.cashier.domain.opp.enums.OppTypeEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.service.PayingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Controller
public class AppNotifyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppNotifyController.class);

    @Autowired private PayingService simplePayingService;

    @RequestMapping(value = {"/app/notify/alipay/paying/{payingId}/{logId}"}, method = RequestMethod.POST)
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
            boolean result = simplePayingService.doNotify(params, payingId, logId, OppTypeEnum.ALIPAY, "app");
            LOGGER.info("[NotifyController] Alipay paying notify, result = [" + result + "], params = [" + params + "]");
            return result ? "success" : "fail";
        } catch (Exception error) {
            LOGGER.error("", error);
            return "fail";
        }
    }
}
