package org.example.licenseplatform.controller;

import org.example.licenseplatform.model.MachineInfo;
import org.example.licenseplatform.util.MachineInfoUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/machine")
public class MachineInfoController {

    @GetMapping("/info")
    public MachineInfo getMachineInfo() {
        return MachineInfoUtils.getMachineInfo();
    }
}
