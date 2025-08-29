package org.example.licenseplatform.model;

import lombok.Data;

/**
 * 代表单台机器的硬件指纹信息
 */
@Data
public class MachineInfo {

    /** CPU 序列号 */
    private String cpuSerial;

    /** MAC 地址 */
    private String macAddress;

    /** 主板序列号 */
    private String mainBoardSerial;
}
