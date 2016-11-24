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

import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.service.PayingService;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.service.RefundService;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

public class RefundApiServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefundApiServlet.class);
    
    private PayingService simplePayingService;
    
    private RefundService simpleRefundService;
    
    private static final long serialVersionUID = 411159507513440868L;
    
    @Override
    public void init(ServletConfig config) {
        try {
            super.init(config);
            WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(
                    getServletContext());
            simplePayingService = context.getBean("simplePayingService", PayingService.class);
            simpleRefundService = context.getBean("simpleRefundService", RefundService.class);
        } catch (ServletException error) {
            LOGGER.error("[RefundApiServlet] Initialization failed", error);
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
        String refundId = request.getParameter("refundId");
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
        
        if (StringUtils.isEmpty(refundId)) {
            List<RefundEntity> refund = simpleRefundService.getRefundOrder(
                    payingId);
            writer.write(refund.isEmpty() ? StringUtils.EMPTY : 
                JsonUtil.encode(refund));
        } else {
            RefundEntity refund = simpleRefundService.getRefundOrder(
                    payingId, Long.valueOf(refundId));
            writer.write(refund == null ? StringUtils.EMPTY : 
                JsonUtil.encode(refund));
        }
        writer.flush();
        writer.close();
    }
}
