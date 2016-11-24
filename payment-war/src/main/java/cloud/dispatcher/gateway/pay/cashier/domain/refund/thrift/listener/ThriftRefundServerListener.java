package cloud.dispatcher.gateway.pay.cashier.domain.refund.thrift.listener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.Setter;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.thrift.ThriftRefundAPI;

public class ThriftRefundServerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThriftRefundServerListener.class);

    @Getter @Setter private int listenOnPort = 35002;

    @Getter @Setter private ThriftRefundAPI.Iface simpleThriftRefundAPI;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public void initialize() {
        executor.submit(new Runnable(){
            @Override
            public void run() {listener();}
        });
    }

    public void destroy() {
        executor.shutdown();
    }

    private void listener() {
        LOGGER.info("[ThriftRefundServerListener] Initialized, port = [" + listenOnPort + "]");
        try {
            TNonblockingServerTransport transport = new TNonblockingServerSocket(listenOnPort);
            TProcessor processor = new ThriftRefundAPI.Processor(simpleThriftRefundAPI);
            TNonblockingServer.Args args = new TNonblockingServer.Args(transport);
            args.protocolFactory(new TBinaryProtocol.Factory()).transportFactory(
                    new TFramedTransport.Factory()).processor(processor);
            TServer server = new TNonblockingServer(args);
            server.serve();
        } catch (Exception error) {
            LOGGER.error("", error);
        }
    }
}
