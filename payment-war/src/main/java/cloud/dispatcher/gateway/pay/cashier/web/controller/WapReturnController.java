package cloud.dispatcher.gateway.pay.cashier.web.controller;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import cloud.dispatcher.gateway.pay.cashier.domain.opp.enums.OppTypeEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.service.PayingService;
import cloud.dispatcher.gateway.pay.cashier.utils.ViewUtil;

@Controller
public class WapReturnController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WapReturnController.class);

    @Autowired private ViewUtil viewUtil;

    @Autowired private PayingService simplePayingService;

    @RequestMapping(value = {"/wap/return/alipay/paying/{payingId}/{logId}"}, method = RequestMethod.GET)
    @ResponseBody
    public ModelAndView payingReturnAction(@PathVariable String payingId,
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
            try {
                val = new String(val.getBytes("ISO-8859-1"), "GBK");
                params.put(key, val);
            } catch (UnsupportedEncodingException error) {
                LOGGER.error("", error);
            }
        }

        if (params.size() == 0) {
            return new ModelAndView("redirect:" + viewUtil.getDomain() + "/wap/paying/" + payingId);
        }

        try {
            String redirect = simplePayingService.doReturn(params, payingId, logId, OppTypeEnum.ALIPAY, "wap");
            boolean result = StringUtils.isEmpty(redirect) ? false : true;
            LOGGER.info("[ReturnController] Alipay paying return, result = [" + result + "], params = [" + params + "]");
            if (result) {
                return new ModelAndView("redirect:" + redirect);
            }
            return new ModelAndView("redirect:" + viewUtil.getDomain() + "/wap/paying/" + payingId);
        } catch (Exception error) {
            LOGGER.error("", error);
            return new ModelAndView("redirect:" + viewUtil.getDomain() + "/wap/paying/" + payingId);
        }
    }
}
