namespace java cloud.dispatcher.gateway.pay.thrift

service ThriftPayingAPI {

    string registerPayingOrder(1:map<string, string> usrParamsMap,
                               2:map<string, string> appParamsMap,
                               3:map<string, string> oppParamsMap,
                               4:string source)

    i32 getPayingOrderPhase(1:string id, 2:string source)
}