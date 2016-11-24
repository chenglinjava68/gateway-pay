package cloud.dispatcher.gateway.pay.thrift.pool;

import org.apache.thrift.transport.TSocket;

public interface ConnectionProvider {

    public TSocket borrowConn();

    public void returnConn(TSocket socket);
}
