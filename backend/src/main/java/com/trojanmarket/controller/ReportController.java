package com.trojanmarket.controller;

import com.trojanmarket.dto.ReportRequest;
import com.trojanmarket.security.SecurityUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Report submission endpoint. CLAUDE.md schema does not yet define a Reports table,
 * so for now we accept the report, log it, and return success. The high-risk-user
 * threshold check in AuthService.isBannedUser will start counting these once the
 * Reports table is added (see TODO there).
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    @PostMapping
    public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody ReportRequest req) {
        Integer reporterID = SecurityUtils.requireCurrentUserID();
        // TODO: persist into Reports(reportID, reporterID, type, targetID, reason, createdAt)
        //       once the migration is added. Until then, log only so the UI flow can be
        //       exercised end-to-end.
        log.info("Report received: reporter={} type={} targetID={} reason='{}'",
                reporterID, req.getType(), req.getTargetID(), req.getReason());
        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
