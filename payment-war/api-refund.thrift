namespace java cloud.dispatcher.gateway.pay.thrift

service ThriftRefundAPI {

    i64 registerRefundOrder(1:string payingId, 2:i32 fee, 3:string reason,
                            4:string notify, 5:string source)

    i32 getRefundOrderPhase(1:string payingId, 2:i64 id, 3:string source)
}