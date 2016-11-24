package cloud.dispatcher.gateway.pay.waiter.http;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.Getter;
import lombok.Setter;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientFactory.class);

    @Getter @Setter private DefaultHttpClient simpleHttpClient;

    @Getter @Setter private int timeout = 3;

    @Getter @Setter private int poolMax = 5;

    private ThreadSafeClientConnManager clientConnManager;

    public void initialize() throws Exception {
        long startTime = System.currentTimeMillis();

        X509TrustManager trustManager = new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                    throws CertificateException {}

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                    throws CertificateException {}
        };

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, new TrustManager[] {trustManager}, null);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        registry.register(new Scheme("https", 443, new SSLSocketFactory(context)));

        clientConnManager = new ThreadSafeClientConnManager(registry, timeout, TimeUnit.SECONDS);
        clientConnManager.setDefaultMaxPerRoute(poolMax);
        clientConnManager.setMaxTotal(poolMax);
        simpleHttpClient = new DefaultHttpClient(clientConnManager, getHttpParams());
        LOGGER.info("[HttpClientFactory] Initialized, cost: " + (
                System.currentTimeMillis() - startTime) + "ms");
    }

    public void destroy() {
        long startTime = System.nanoTime();
        if (clientConnManager != null)
            clientConnManager.shutdown();

        LOGGER.info("[HttpClientFactory] Closed, cost: " + (
                System.currentTimeMillis() - startTime) + "ms");
    }

    private HttpParams getHttpParams() {
        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout * 1000);
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, timeout * 1000);
        params.setParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET, "GBK");
        return params;
    }
}
