package cloud.dispatcher.gateway.pay.thrift.support;

import org.apache.thrift.TException;

import cloud.dispatcher.gateway.pay.thrift.ThriftHeartBeatAPI;

public class SimpleThriftHeartBeatAPI implements ThriftHeartBeatAPI.Iface {

    @Override
    public long call() throws TException {
        return System.currentTimeMillis();
    }
}
