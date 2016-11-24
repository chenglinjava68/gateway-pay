package cloud.dispatcher.gateway.pay.cashier.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import cloud.dispatcher.gateway.pay.cashier.domain.logger.bean.entity.LoggerEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.logger.service.LoggerService;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.service.PayingService;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

public class LoggerApiServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerApiServlet.class);
    
    private LoggerService simpleLoggerService;
    
    private PayingService simplePayingService;
    
    private static final long serialVersionUID = 411159507513440868L;
    
    @Override
    public void init(ServletConfig config) {
        try {
            super.init(config);
            WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(
                    getServletContext());
            simpleLoggerService = context.getBean("simpleLoggerService", LoggerService.class);
            simplePayingService = context.getBean("simplePayingService", PayingService.class);
        } catch (ServletException error) {
            LOGGER.error("[LoggerApiServlet] Initialization failed", error);
            System.exit(-1);
        }
    }
    
    @Override
    public void destroy() {
        super.destroy();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doRequest(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doRequest(request, response);
    }
    
    private void doRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain; charset=GBK");
        PrintWriter writer = response.getWriter();
        
        String payingId = request.getParameter("payingId");
        String loggerId = request.getParameter("loggerId");
        String source = request.getParameter("source");
        
        if (StringUtils.isEmpty(payingId) || StringUtils.isEmpty(source) || 
                !simplePayingService.isPayingIdAvailable(payingId)) {
            writer.write(StringUtils.EMPTY);
            writer.flush();
            writer.close();
            return;
        } 
        
        PayingEntity paying = simplePayingService.getPayingOrder(payingId);
        if (paying == null || 
                !paying.getAppOrderSource().trim().equals(source)) {
            writer.write(StringUtils.EMPTY);
            writer.flush();
            writer.close();
            return;
        } 
        
        if (StringUtils.isEmpty(loggerId)) {
            List<LoggerEntity> logger = simpleLoggerService.getLogList(
                    payingId);
            writer.write(logger.isEmpty() ? StringUtils.EMPTY : 
                JsonUtil.encode(logger));
        } else {
            LoggerEntity logger = simpleLoggerService.getLogView(
                    payingId, Long.valueOf(loggerId));
            writer.write(logger == null ? StringUtils.EMPTY : 
                JsonUtil.encode(logger));
        }
        writer.flush();
        writer.close();
    }
}
