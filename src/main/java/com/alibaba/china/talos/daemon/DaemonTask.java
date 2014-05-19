package com.alibaba.china.talos.daemon;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.china.shared.talos.result.model.AuthTarget;
import com.alibaba.china.shared.talos.result.service.AuthTargetService;
import com.alibaba.china.talos.file.LineAnalyzer;
import com.alibaba.china.talos.file.LineHandle;



public class DaemonTask extends AbstractFileTaskAO<String> {

    @Autowired
    private AuthTargetService authTargetService;


    @Override
    public LineAnalyzer<String> getLineAnalyzer() {
        return new LineAnalyzer<String>() {

            @Override
            public String analyse(String lineText) {

                return getMetaData(lineText);
            }

            private String getMetaData(String data) {
                return data.substring(1, data.length() - 1);
            }
        };
    }

    @Override
    public LineHandle<String> getLineHandle() {
        return new LineHandle<String>() {

            @Override
            public boolean handle(String model) {
                getLogger().info("ao begin!");
                authTargetService.getAuthTargetId(new AuthTarget(model));
                getLogger().info("ao end where userId is:" + model);
                return true;
            }

        };
    }

}
