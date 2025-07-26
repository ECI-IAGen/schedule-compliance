package com.eci.iagen.schedule_compliance.controller;

import com.eci.iagen.schedule_compliance.dto.ScheduleComplianceRequest;
import com.eci.iagen.schedule_compliance.dto.ScheduleComplianceResponse;
import com.eci.iagen.schedule_compliance.service.ScheduleComplianceService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/schedule-compliance")
@RequiredArgsConstructor
public class ScheduleComplianceController {

    private final ScheduleComplianceService scheduleComplianceService;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScheduleComplianceController.class);

    /**
     * Evalúa el cumplimiento de horarios para una entrega
     * 
     * @param request Datos de la evaluación de cumplimiento
     * @return Respuesta con puntuación penalizada y detalles
     */
    @PostMapping("/evaluate")
    public ResponseEntity<ScheduleComplianceResponse> evaluateCompliance(
            @Valid @RequestBody ScheduleComplianceRequest request) {
        
        logger.info("Evaluating schedule compliance for request: {}", request);
        ScheduleComplianceResponse response = scheduleComplianceService
                .evaluateScheduleCompliance(request);
        logger.info("Compliance evaluation result: {}", response);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de health check
     * 
     * @return Estado del servicio
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Schedule Compliance Service is running");
    }
}
