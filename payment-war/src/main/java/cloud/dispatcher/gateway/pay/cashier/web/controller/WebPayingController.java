package cloud.dispatcher.gateway.pay.cashier.web.controller;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import cloud.dispatcher.gateway.pay.cashier.domain.opp.enums.OppTypeEnum;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.service.PayingService;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;
import cloud.dispatcher.gateway.pay.cashier.utils.ViewUtil;

@Controller
public class WebPayingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebPayingController.class);

    @Autowired private ViewUtil viewUtil;

    @Autowired private PayingService simplePayingService;

    @RequestMapping(method = RequestMethod.GET, value = {"/paying/{id}"})
    public ModelAndView payingAction(@PathVariable String id, ModelAndView modelAndView, 
            HttpServletRequest request, HttpServletResponse response) {
        modelAndView.addObject("fr_submit", request.getParameter("frSubmit"));
        modelAndView.addObject("id", id);
        modelAndView.addObject("html_header", viewUtil.getWebHeaderHTML());
        modelAndView.addObject("html_footer", viewUtil.getWebFooterHTML());

        if (!simplePayingService.isPayingIdAvailable(id)) {
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("web/message");
            return modelAndView;
        }

        PayingEntity payingEntity = simplePayingService.getPayingOrder(id);

        if (!simplePayingService.isPayingOrderAvailable(payingEntity)) {
            if (payingEntity != null) {
                return new ModelAndView("redirect:" + payingEntity.getAppOrderReturn());
            }
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("web/message");
            return modelAndView;
        }

        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                payingEntity.getOppGatewayAuth(), Map.class);
        if (oppAuth.get("alipay") == null || oppAuth.get("alipay").size() == 0) {
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("web/message");
            return modelAndView;
        }

        modelAndView.setViewName("web/paying");
        modelAndView.addObject("fee", payingEntity.getAppOrderFee() / 100.00);
        modelAndView.addObject("item", JsonUtil.decode(payingEntity.
                getAppOrderItem(), Map.class));
        Date currentTime = new Date();
        modelAndView.addObject("expire_date", (payingEntity.getAppOrderExpire()));
        modelAndView.addObject("expire_time", (payingEntity.getAppOrderExpire().getTime() -
                currentTime.getTime()) / 1000);
        modelAndView.addObject("return", payingEntity.getAppOrderReturn());
        
        Cookie cookie = new Cookie(id, "allow");
        response.addCookie(cookie);
        
        return modelAndView;
    }

    @RequestMapping(method = RequestMethod.GET, value = {"/paying/redirect/1/{id}"})
    public ModelAndView redirectAction(HttpServletRequest request, 
            ModelAndView modelAndView, @PathVariable String id) {
        modelAndView.addObject("id", id);
        modelAndView.addObject("html_header", viewUtil.getWebHeaderHTML());
        modelAndView.addObject("html_footer", viewUtil.getWebFooterHTML());
        
        if (!simplePayingService.isPayingIdAvailable(id)) {
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("web/message");
            return modelAndView;
        }

        PayingEntity payingEntity = simplePayingService.getPayingOrder(id);

        if (!simplePayingService.isPayingOrderAvailable(payingEntity)) {
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("web/message");
            return modelAndView;
        }

        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                payingEntity.getOppGatewayAuth(), Map.class);
        if (oppAuth.get("alipay") == null || oppAuth.get("alipay").size() == 0) {
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("web/message");
            return modelAndView;
        }

        modelAndView.setViewName("web/redirect_alipay");
        
        try {
            Map<String, String> requestMap = simplePayingService.doPaying(id, OppTypeEnum.ALIPAY,
                    request.getRemoteAddr(), "web");
            if (requestMap.size() == 0) {
                modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
                modelAndView.setViewName("web/message");
                return modelAndView;
            }
            modelAndView.addObject("requestMap", requestMap);
            return modelAndView;
        } catch (Exception error) {
            modelAndView.addObject("message", "֧�������쳣 UNKNOW_EXCEPTION");
            modelAndView.setViewName("web/message");
            LOGGER.error("", error);
            return modelAndView;
        }
    }
}
