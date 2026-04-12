package com.example.backend.report.dto;

import lombok.Data;

@Data
public class ResolveReportRequest {
    /** RESOLVED | DISMISSED */
    private String status;
    /** Ghi chú xử lý (tuỳ chọn) */
    private String resolution;
    /** NONE | BAN_1_DAY | BAN_7_DAYS | BAN_30_DAYS | BAN_PERMANENT */
    private String banAction = "NONE";
}
