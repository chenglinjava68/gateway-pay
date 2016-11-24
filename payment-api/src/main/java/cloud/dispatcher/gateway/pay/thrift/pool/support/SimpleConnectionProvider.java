package cloud.dispatcher.gateway.pay.thrift.pool.support;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.thrift.transport.TSocket;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import cloud.dispatcher.gateway.pay.thrift.pool.ConnectionProvider;

public class SimpleConnectionProvider implements ConnectionProvider, InitializingBean, DisposableBean {

    @SuppressWarnings("rawtypes")
    @Setter private ObjectPool objectPool = null;

    @Setter private boolean testOnBorrow  = GenericObjectPool.DEFAULT_TEST_ON_BORROW;

    @Setter private boolean testOnReturn  = GenericObjectPool.DEFAULT_TEST_ON_RETURN;

    @Setter private boolean testWhileIdle = GenericObjectPool.DEFAULT_TEST_WHILE_IDLE;

    @Setter @Getter private String address;

    @Setter @Getter private int port;

    @Setter @Getter private int timeout;

    @Setter private int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;

    @Setter private int minIdle  = GenericObjectPool.DEFAULT_MIN_IDLE;

    @Setter private int maxIdle  = GenericObjectPool.DEFAULT_MAX_IDLE;

    @Setter private long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void afterPropertiesSet() throws Exception {
        GenericObjectPool.Config config = new GenericObjectPool.Config();
        PoolableObjectFactory factory = new SimplePoolableObjectFactory(
                address, port, timeout);
        config.testWhileIdle = testWhileIdle;
        config.minIdle = minIdle;
        config.maxIdle = maxIdle;
        config.maxWait = maxWait;
        config.maxActive = maxActive;
        config.testOnBorrow = testOnBorrow;
        config.testOnReturn = testOnReturn;

        objectPool = new GenericObjectPool(factory, config);
    }

    @Override
    public TSocket borrowConn() {
        try {
            return (TSocket) objectPool.borrowObject();
        } catch (Exception error) {
            throw new RuntimeException("", error);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void returnConn(TSocket socket) {
        try {
            objectPool.returnObject(socket);
        } catch (Exception error) {
            throw new RuntimeException("", error);
        }
    }

    @Override
    public void destroy() throws Exception {
        objectPool.close();
    }
}
