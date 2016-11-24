package cloud.dispatcher.gateway.pay.cashier.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MonitorController {

    @RequestMapping("/monitor/live")
    @ResponseBody
    public String liveAction() {
        return "ok";
    }

    @RequestMapping("/monitor/stat")
    @ResponseBody
    public String statAction() {
        return "ok";
    }
}
