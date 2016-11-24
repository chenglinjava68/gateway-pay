package cloud.dispatcher.gateway.pay.cashier.domain.opp.weixin;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import lombok.Getter;
import lombok.Setter;

public class WeixinHttpsClient {

    private static final String JKS_CA_ALIAS = "tenpay";
    
    private static final String JKS_CA_PASWD = "";
    
    @Getter @Setter private String charset = "GBK";
    
    @Getter @Setter private File certPemFile;

    @Getter @Setter private File certPfxFile;
    
    @Getter @Setter private String certPfxPasswd;
    
    @Getter @Setter private int timeout = 5;
    
    public String doRequest(String requestMethod, String uri, String params) {
        try {
            File jksCAFile = new File(this.certPemFile.getParent() + "/tenpay.jks");
            if (!jksCAFile.isFile()) {
                X509Certificate certificate = (X509Certificate)getCertificate(
                        this.certPemFile);
                FileOutputStream ostream = new FileOutputStream(jksCAFile);
                storeCACert(certificate, JKS_CA_ALIAS, JKS_CA_PASWD, ostream);
                ostream.close();
            }
            
            FileInputStream jksStream = new FileInputStream(jksCAFile);
            FileInputStream keyStream = new FileInputStream(this.certPfxFile);
            
            SSLContext context = getSSLContext(jksStream, JKS_CA_PASWD, 
                    keyStream, this.certPfxPasswd);
            keyStream.close();
            jksStream.close();
            
            SSLSocketFactory factory = context.getSocketFactory();
            
            if (requestMethod.equalsIgnoreCase("POST")) {
                URL url = new URL(uri);
                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                connection.setSSLSocketFactory(factory);
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(timeout * 1000);
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                
                BufferedOutputStream stream = new BufferedOutputStream(connection
                        .getOutputStream());
                byte[] post = params.getBytes();
                int offset = 0;
                int size = post.length;
                while (offset < post.length) {
                    if (1024 >= size) {
                        stream.write(post, offset, size);
                        offset += size;
                    } else {
                        stream.write(post, offset, size);
                        offset += 1024;
                        size -= 1024;
                    }
                    stream.flush();
                }
                stream.close();
                InputStream istream = connection.getInputStream();
                ByteArrayOutputStream ostream = new ByteArrayOutputStream(); 
                byte[] response = new byte[4096];
                int count = -1;
                while((count = istream.read(response, 0, 4096)) != -1)
                    ostream.write(response, 0, count);
                response = null;
                byte[] bytes = ostream.toByteArray();
                ostream.close();
                istream.close();
                return new String(bytes, charset);
            } else {
                URL url = new URL(uri + "?" + params);
                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                connection.setSSLSocketFactory(factory);
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(timeout * 1000);
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                
                InputStream istream = connection.getInputStream();
                ByteArrayOutputStream ostream = new ByteArrayOutputStream(); 
                byte[] response = new byte[4096];
                int count = -1;
                while((count = istream.read(response, 0, 4096)) != -1)
                    ostream.write(response, 0, count);
                response = null;
                byte[] bytes = ostream.toByteArray();
                ostream.close();
                istream.close();
                return new String(bytes, charset);
            }
        } catch (Exception error) {
            throw new RuntimeException("", error);
        }
    }
    
    private Certificate getCertificate(File file) throws CertificateException, IOException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        FileInputStream istream = new FileInputStream(file);
        Certificate certificate = factory.generateCertificate(istream);
        istream.close();
        return certificate;
    }
    
    private void storeCACert(Certificate certificate, String alias, String password, 
            OutputStream ostream) throws KeyStoreException, NoSuchAlgorithmException, 
            CertificateException, IOException {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null);
        store.setCertificateEntry(alias, certificate);
        store.store(ostream, password.toCharArray());
    }
    
    private SSLContext getSSLContext(FileInputStream jksIstream, String jksPasswd,
            FileInputStream keyIstream, String keyPasswd)
            throws NoSuchAlgorithmException, KeyStoreException, CertificateException, 
            IOException, UnrecoverableKeyException, KeyManagementException {
        TrustManagerFactory jksFactory = TrustManagerFactory.getInstance("SunX509");
        KeyStore jksStore = KeyStore.getInstance("JKS");
        jksStore.load(jksIstream, jksPasswd.toCharArray());
        jksFactory.init(jksStore);

        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance("SunX509");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keyIstream, keyPasswd.toCharArray());
        keyFactory.init(keyStore, keyPasswd.toCharArray());

        SecureRandom random = new SecureRandom();
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyFactory.getKeyManagers(), 
                jksFactory.getTrustManagers(), random);
        return context;
    }
}
