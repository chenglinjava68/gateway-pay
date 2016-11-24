package cloud.dispatcher.gateway.pay.cashier.web.controller;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cloud.dispatcher.gateway.pay.cashier.utils.VersionUtil;
import org.apache.commons.lang.StringUtils;
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

@Controller
public class WapPayingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WapPayingController.class);
    
    private static final String FROM_APP_COOKIE_NAME = "fromapp";

    @Autowired private PayingService simplePayingService;

    @RequestMapping(method = RequestMethod.GET, value = {"/wap/paying/{id}"})
    public ModelAndView payingAction(@PathVariable String id, ModelAndView modelAndView,
            HttpServletRequest request, HttpServletResponse response) {
        modelAndView.addObject("fr_submit", request.getParameter("frSubmit"));

        Cookie[] cookieArray = request.getCookies();
        boolean isFromApp = false;
        modelAndView.addObject("id", id);
        
        if (cookieArray != null && cookieArray.length > 0) {
            for (int i = 0; i < cookieArray.length; i ++) {
                Cookie cookie = cookieArray[i];
                if (cookie.getName().equals(FROM_APP_COOKIE_NAME) && 
                        StringUtils.isNotEmpty(cookie.getValue())) {
                    isFromApp = true;
                    break;
                }
            }
        }
        modelAndView.addObject("is_from_app", isFromApp);
        
        if (!simplePayingService.isPayingIdAvailable(id)) {
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("wap/message");
            return modelAndView;
        }

        PayingEntity payingEntity = simplePayingService.getPayingOrder(id);

        if (!simplePayingService.isPayingOrderAvailable(payingEntity)) {
            if (payingEntity != null) {
                return new ModelAndView("redirect:" + payingEntity.getAppOrderReturn());
            }
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("wap/message");
            return modelAndView;
        }
        
        modelAndView.addObject("app_order_return", payingEntity.getAppOrderReturn());

        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                payingEntity.getOppGatewayAuth(), Map.class);

        boolean isFromWXB = StringUtils.isNotEmpty(request.getHeader("user-agent")) &&
                request.getHeader("user-agent").indexOf("MicroMessenger") >= 0 ? true : false;

        modelAndView.setViewName("wap/paying");
        if (oppAuth.get("alipay") != null && !oppAuth.get("alipay").isEmpty()) {
            modelAndView.addObject("opp_alipay", true);
        }
        if (oppAuth.get("weixin") != null && !oppAuth.get("weixin").isEmpty() && 
                isFromApp) {
            modelAndView.addObject("opp_weixin", true);
        }
        if (oppAuth.get("wxmpay") != null && !oppAuth.get("wxmpay").isEmpty() &&
                isFromWXB) {
            modelAndView.addObject("opp_wxmpay", true);
            modelAndView.addObject("isFromWXB", isFromWXB);
        }

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

    @RequestMapping(method = RequestMethod.GET, value = {"/wap/paying/redirect/{oppType}/{id}"})
    public ModelAndView redirectAction(HttpServletRequest request, ModelAndView modelAndView,
            @PathVariable String id, @PathVariable int oppType) {
        Cookie[] cookieArray = request.getCookies();
        boolean isFromApp = false;
        modelAndView.addObject("id", id);
        
        if (cookieArray != null && cookieArray.length > 0) {
            for (int i = 0; i < cookieArray.length; i ++) {
                Cookie cookie = cookieArray[i];
                if (cookie.getName().equals(FROM_APP_COOKIE_NAME) && 
                        StringUtils.isNotEmpty(cookie.getValue())) {
                    isFromApp = true;
                    break;
                }
            }
        }
        modelAndView.addObject("is_from_app", isFromApp);
        
        if (!simplePayingService.isPayingIdAvailable(id)) {
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("wap/message");
            return modelAndView;
        }

        PayingEntity payingEntity = simplePayingService.getPayingOrder(id);

        if (!simplePayingService.isPayingOrderAvailable(payingEntity)) {
            modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
            modelAndView.setViewName("wap/message");
            return modelAndView;
        }
        
        modelAndView.addObject("app_order_return", payingEntity.getAppOrderReturn());

        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                payingEntity.getOppGatewayAuth(), Map.class);

        boolean isFromWXB = StringUtils.isNotEmpty(request.getHeader("user-agent")) &&
                request.getHeader("user-agent").indexOf("MicroMessenger") >= 0 ? true : false;

        if (oppType == 1 && (oppAuth.get("alipay") == null || oppAuth.get("alipay").isEmpty())) {
            modelAndView.addObject("message", "֧����ʽ��Ч ILLEGAL_OPP_PARAM");
            modelAndView.setViewName("wap/message");
            return modelAndView;
        }

        if (oppType == 2 && (oppAuth.get("weixin") == null || oppAuth.get("weixin").isEmpty() || 
                !isFromApp)) {
            modelAndView.addObject("message", "֧����ʽ��Ч ILLEGAL_OPP_PARAM");
            modelAndView.setViewName("wap/message");
            return modelAndView;
        }

        if (oppType == 3 && (oppAuth.get("wxmpay") == null || oppAuth.get("wxmpay").isEmpty() ||
                !isFromWXB)) {
            modelAndView.addObject("message", "֧����ʽ��Ч ILLEGAL_OPP_PARAM");
            modelAndView.setViewName("wap/message");
            return modelAndView;
        }

        /**
         * ����֧����Ǯ�����õ��ж�,�����ع�ʱ�����PC,Wap,App�ĵ��û���
         */
        if (oppType == 1) {
            String[] userAgent = request.getHeader("user-agent").split("\\s+");
            for (int i = 0; i < userAgent.length; i++) {
                if (StringUtils.isNotEmpty(userAgent[i]) && userAgent[i].startsWith("gateway-pay")) {
                    String[] appVersion = userAgent[i].split("/");
                    if (VersionUtil.compare(appVersion[1], "5.5.1") || appVersion[1].equals("5.5.1")) {
                        isFromApp = true;
                    } else {
                        isFromApp = false;
                    }
                }
            }
        }

        Map<String, String> requestMap = null;

        try {
            requestMap = simplePayingService.doPaying(id, OppTypeEnum.valueOf(oppType),
                    request.getRemoteAddr(), isFromApp ? "app" : "wap");
            if (requestMap.size() == 0) {
                modelAndView.addObject("message", "֧��������Ч ILLEGAL_PAYING_PARAM");
                modelAndView.setViewName("wap/message");
                return modelAndView;
            } else {
                modelAndView.addObject("requestMap", requestMap);
                if (oppType == 1 && !isFromApp) {
                    modelAndView.setViewName("wap/redirect_alipay");
                }
                if (oppType == 1 && isFromApp) {
                    modelAndView.setViewName("wap/redirect_aliwlt");
                }
                if (oppType == 2) {
                    modelAndView.setViewName("wap/redirect_weixin");
                }
                if (oppType == 3) {
                    modelAndView.setViewName("wap/redirect_wxmpay");
                }
                return modelAndView;
            }
        } catch (Exception error) {
            modelAndView.addObject("message", "֧�������쳣 UNKNOW_EXCEPTION");
            modelAndView.setViewName("wap/message");
            LOGGER.error("", error);
            return modelAndView;
        }
    }
}
