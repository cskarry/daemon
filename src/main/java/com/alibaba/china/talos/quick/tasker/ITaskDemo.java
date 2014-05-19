package com.alibaba.china.talos.quick.tasker;

import com.alibaba.china.shared.talos.result.model.AuthTarget;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;



public class ITaskDemo extends ITask<String> {

    @ResourceTasker(name = "authTargetService")
    private AuthTargetService authTargetService;

    @Override
    public LineAnalyzer<String> getLineAnalyzer() {
        return new LineAnalyzer<String>() {

            @Override
            public String analyse(String lineText) {
                return (String) "12";
            }

        };
    }

    @Override
    public LineHandle<String> getLineHandle() {
        return new LineHandle<String>() {

            @Override
            public boolean handle(String model) {
                getLogger().info("ao begin!");
                authTargetService.getAuthTargetId(new AuthTarget("12"));
                getLogger().info("ao end where userId is:" + model);
                return true;
            }

        };
    }


}
