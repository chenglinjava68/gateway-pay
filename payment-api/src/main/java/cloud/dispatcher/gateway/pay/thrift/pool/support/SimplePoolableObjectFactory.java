package cloud.dispatcher.gateway.pay.thrift.pool.support;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class SimplePoolableObjectFactory implements PoolableObjectFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePoolableObjectFactory.class);

    private String address;

    private int port;

    private int timeout;

    public SimplePoolableObjectFactory(String address, int port, int timeout) {
        this.address = address;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public Object makeObject() throws Exception {
        try {
            TTransport transport = new TSocket(this.address, this.port, this.timeout);
            transport.open();
            return transport;
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public void destroyObject(Object object) throws Exception {
        if (object instanceof TSocket) {
            TSocket socket = (TSocket) object;
            if (socket.isOpen()) {
                socket.close();
            }
        }
    }

    @Override
    public boolean validateObject(Object object) {
        try {
            if (object instanceof TSocket) {
                TSocket thriftSocket = (TSocket) object;
                return thriftSocket.isOpen();
            } else {
                return false;
            }
        } catch (Exception error) {
            LOGGER.error("", error);
            return false;
        }
    }

    @Override
    public void activateObject(Object o) throws Exception {
    }

    @Override
    public void passivateObject(Object o) throws Exception {
    }
}
