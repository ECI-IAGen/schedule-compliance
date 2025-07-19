package com.eci.iagen.schedule_compliance.service;

import com.eci.iagen.schedule_compliance.dto.ScheduleComplianceRequest;
import com.eci.iagen.schedule_compliance.dto.ScheduleComplianceResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScheduleComplianceService {

    @Value("${schedule.compliance.penalty.per.day:0.5}")
    private BigDecimal penaltyPerDay;

    @Value("${schedule.compliance.max.score:5.0}")
    private BigDecimal maxScore;

    @Value("${schedule.compliance.min.score:0.0}")
    private BigDecimal minScore;

    private final ObjectMapper objectMapper;

    /**
     * Evalúa el cumplimiento de horarios basado en días tardíos desde la fecha de
     * vencimiento
     * 
     * @param request Datos de la evaluación
     * @return Respuesta con penalizaciones aplicadas
     */
    public ScheduleComplianceResponse evaluateScheduleCompliance(ScheduleComplianceRequest request) {
        // Determinar la fecha de entrega efectiva
        LocalDateTime effectiveSubmissionDate = determineEffectiveSubmissionDate(
                request.getCommits(),
                request.getSubmissionDate());

        // Calcular días tardíos
        int lateDays = calculateLateDays(request.getDueDate(), effectiveSubmissionDate);

        // Calcular penalización
        BigDecimal penaltyApplied = calculatePenalty(lateDays);

        // Calcular puntuación final
        BigDecimal penalizedScore = maxScore.subtract(penaltyApplied);
        penalizedScore = penalizedScore.max(minScore);

        // Generar criterios de evaluación
        List<Map<String, Object>> commitDetails = buildCommitDetails(
                request.getCommits(),
                request.getDueDate());
        String evaluationCriteria = buildEvaluationCriteria(
                lateDays,
                penaltyApplied,
                commitDetails);

        return new ScheduleComplianceResponse(
                penalizedScore.setScale(2, RoundingMode.HALF_UP),
                maxScore,
                lateDays,
                penaltyApplied.setScale(2, RoundingMode.HALF_UP),
                lateDays > 0,
                evaluationCriteria,
                LocalDateTime.now(),
                commitDetails);
    }

    /**
     * Determina la fecha de entrega efectiva basada en commits y fecha de
     * submission
     */
    private LocalDateTime determineEffectiveSubmissionDate(
            List<ScheduleComplianceRequest.CommitInfo> commits,
            LocalDateTime submissionDate) {

        if (commits == null || commits.isEmpty()) {
            return submissionDate;
        }

        // Buscar el último commit como fecha de entrega efectiva
        Optional<LocalDateTime> lastCommitDate = commits.stream()
                .map(ScheduleComplianceRequest.CommitInfo::getDate)
                .max(LocalDateTime::compareTo);

        // Si hay commits, usar el último commit. Si no, usar la fecha de submission
        return lastCommitDate.orElse(submissionDate);
    }

    /**
     * Calcula los días tardíos entre la fecha de vencimiento y la fecha de entrega
     */
    private int calculateLateDays(LocalDateTime dueDate, LocalDateTime submissionDate) {
        if (submissionDate.isBefore(dueDate) || submissionDate.isEqual(dueDate)) {
            return 0; // Entrega a tiempo
        }

        // Calcular días completos de retraso
        long daysBetween = ChronoUnit.DAYS.between(dueDate.toLocalDate(), submissionDate.toLocalDate());

        // Si la entrega es el mismo día pero después de la hora límite, contar como 1
        // día tardío
        if (daysBetween == 0 && submissionDate.isAfter(dueDate)) {
            return 1;
        }

        return Math.max(0, (int) daysBetween);
    }

    /**
     * Calcula la penalización total basada en días tardíos
     */
    private BigDecimal calculatePenalty(int lateDays) {
        if (lateDays <= 0) {
            return BigDecimal.ZERO;
        }

        return penaltyPerDay.multiply(BigDecimal.valueOf(lateDays));
    }

    /**
     * Construye detalles de commits para el criterio de evaluación
     */
    private List<Map<String, Object>> buildCommitDetails(
            List<ScheduleComplianceRequest.CommitInfo> commits,
            LocalDateTime dueDate) {

        List<Map<String, Object>> commitDetails = new ArrayList<>();

        if (commits == null) {
            return commitDetails;
        }

        // Ordenar commits por fecha (más reciente primero)
        commits.sort(Comparator.comparing(
                ScheduleComplianceRequest.CommitInfo::getDate).reversed());

        for (ScheduleComplianceRequest.CommitInfo commit : commits) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("sha", commit.getSha());
            detail.put("message", commit.getMessage());
            detail.put("date", commit.getDate().toString());
            detail.put("onTime", !commit.getDate().isAfter(dueDate));

            commitDetails.add(detail);
        }

        return commitDetails;
    }

    /**
     * Construye el JSON de criterios de evaluación
     */
    private String buildEvaluationCriteria(
            int lateDays,
            BigDecimal penaltyApplied,
            List<Map<String, Object>> commitDetails) {

        try {
            Map<String, Object> criteria = new LinkedHashMap<>();
            criteria.put("evaluationMethod", "Days-based penalty system");
            criteria.put("lateDays", lateDays);
            criteria.put("penaltyPerDay", penaltyPerDay);
            criteria.put("totalPenalty", penaltyApplied);
            criteria.put("originalScore", maxScore);
            criteria.put("isLate", lateDays > 0);
            criteria.put("commits", commitDetails);
            criteria.put("evaluationDate", LocalDateTime.now().toString());

            return objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error generating evaluation criteria JSON", e);
        }
    }
}
