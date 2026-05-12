package com.ccec.timer.service;

import java.util.Optional;

/**
 * 按工位与机型解析 Oracle 中的 CT 与预警阈值；无匹配配置时返回 empty（现场应进入 ABNORMAL / 未知机型，见 V1.2 §3.5）。
 */
public interface CtConfigResolver {
    Optional<CtConfig> resolve(String engineType, String stationCode);
}
