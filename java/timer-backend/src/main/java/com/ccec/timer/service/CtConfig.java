package com.ccec.timer.service;

/**
 * 单条 CT 配置（与 T_CT_CONFIG 一行对应）。阈值表示「已用时间 / 标准 CT」达到该比例时进入黄/红预警区（V8.4.1 / V1.2 §3.5）。
 */
public record CtConfig(int standardCtSeconds, double warnThresholdElapsed, double alarmThresholdElapsed) {
    public CtConfig {
        if (standardCtSeconds <= 0) {
            throw new IllegalArgumentException("standardCtSeconds must be positive");
        }
        double w = warnThresholdElapsed;
        double a = alarmThresholdElapsed;
        if (w <= 0 || w >= 1) {
            w = 0.5;
        }
        if (a <= 0 || a > 1) {
            a = 0.8;
        }
        if (w >= a) {
            w = Math.max(0.1, a - 0.1);
        }
        warnThresholdElapsed = w;
        alarmThresholdElapsed = a;
    }
}
