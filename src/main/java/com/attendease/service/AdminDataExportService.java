package com.attendease.service;

import com.attendease.dto.UserSummary;
import com.attendease.entity.AdminActivityLog;
import com.attendease.entity.AdminDataExport;
import com.attendease.entity.AttendanceLocation;
import com.attendease.entity.AttendanceRecord;
import com.attendease.entity.CompensationTransaction;
import com.attendease.entity.User;
import com.attendease.entity.UserRegistration;
import com.attendease.repository.AdminActivityLogRepository;
import com.attendease.repository.AdminDataExportRepository;
import com.attendease.repository.AttendanceLocationRepository;
import com.attendease.repository.AttendanceRecordRepository;
import com.attendease.repository.CompensationTransactionRepository;
import com.attendease.repository.UserRegistrationRepository;
import com.attendease.repository.UserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminDataExportService {

    private static final String REDACTED = "[REDACTED]";
    private static final List<String> HEADERS = List.of(
            "recordType",
            "users.id", "users.fullName", "users.email", "users.password", "users.role",
            "users.active", "users.profileImage", "users.dailySalary", "users.monthlyAdvance",
            "users.createdAt",
            "user_registration.id", "user_registration.fullName", "user_registration.email",
            "user_registration.password", "user_registration.createdAt",
            "attendance_records.id", "attendance_records.userId", "attendance_records.checkInTime",
            "attendance_records.checkOutTime", "attendance_records.checkInLatitude",
            "attendance_records.checkInLongitude", "attendance_records.checkOutLatitude",
            "attendance_records.checkOutLongitude", "attendance_records.faceVerified",
            "attendance_records.locationVerified", "attendance_records.status",
            "attendance_records.createdAt",
            "compensation_transactions.id", "compensation_transactions.userId",
            "compensation_transactions.type", "compensation_transactions.amount",
            "compensation_transactions.description", "compensation_transactions.recordedByEmail",
            "compensation_transactions.createdAt",
            "attendance_locations.id", "attendance_locations.name", "attendance_locations.address",
            "attendance_locations.latitude", "attendance_locations.longitude",
            "attendance_locations.radiusMeters", "attendance_locations.active",
            "attendance_locations.createdAt", "attendance_locations.updatedAt",
            "admin_activity_logs.id", "admin_activity_logs.adminId", "admin_activity_logs.action",
            "admin_activity_logs.details", "admin_activity_logs.createdAt"
    );
    private static final List<String> COMPENSATION_HEADERS = List.of(
            "User name", "UserId", "Daily salary", "Working days", "Total earned",
            "Monthly paid", "Monthly advance", "Total salary", "Action"
    );

    private final AdminService adminService;
    private final AdminActivityLogRepository activityLogRepository;
    private final AdminDataExportRepository dataExportRepository;
    private final AttendanceLocationRepository locationRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final CompensationTransactionRepository transactionRepository;
    private final UserRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public AdminDataExportService(AdminService adminService,
                                  AdminActivityLogRepository activityLogRepository,
                                  AdminDataExportRepository dataExportRepository,
                                  AttendanceLocationRepository locationRepository,
                                  AttendanceRecordRepository attendanceRepository,
                                  CompensationTransactionRepository transactionRepository,
                                  UserRegistrationRepository registrationRepository,
                                  UserRepository userRepository) {
        this.adminService = adminService;
        this.activityLogRepository = activityLogRepository;
        this.dataExportRepository = dataExportRepository;
        this.locationRepository = locationRepository;
        this.attendanceRepository = attendanceRepository;
        this.transactionRepository = transactionRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(rollbackFor = IOException.class)
    public byte[] createWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writeCompensationSummary(workbook);
            Sheet sheet = workbook.createSheet("Database export");
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.size(); column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(HEADERS.get(column));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (User user : userRepository.findAll()) {
                rowIndex = writeRecord(sheet, rowIndex, "USER", fields(
                        "users.id", user.getId(),
                        "users.fullName", user.getFullName(),
                        "users.email", user.getEmail(),
                        "users.password", REDACTED,
                        "users.role", user.getRole(),
                        "users.active", user.isActive(),
                        "users.profileImage", user.getProfileImage() == null || user.getProfileImage().isBlank()
                                ? null : "[STORED IN DATABASE]",
                        "users.dailySalary", user.getDailySalary(),
                        "users.monthlyAdvance", user.getMonthlyAdvance(),
                        "users.createdAt", user.getCreatedAt()));
            }
            for (UserRegistration registration : registrationRepository.findAll()) {
                rowIndex = writeRecord(sheet, rowIndex, "PENDING_REGISTRATION", fields(
                        "user_registration.id", registration.getId(),
                        "user_registration.fullName", registration.getFullName(),
                        "user_registration.email", registration.getEmail(),
                        "user_registration.password", REDACTED,
                        "user_registration.createdAt", registration.getCreatedAt()));
            }
            for (AttendanceRecord record : attendanceRepository.findAll()) {
                rowIndex = writeRecord(sheet, rowIndex, "ATTENDANCE_RECORD", fields(
                        "attendance_records.id", record.getId(),
                        "attendance_records.userId", record.getUserId(),
                        "attendance_records.checkInTime", record.getCheckInTime(),
                        "attendance_records.checkOutTime", record.getCheckOutTime(),
                        "attendance_records.checkInLatitude", record.getCheckInLatitude(),
                        "attendance_records.checkInLongitude", record.getCheckInLongitude(),
                        "attendance_records.checkOutLatitude", record.getCheckOutLatitude(),
                        "attendance_records.checkOutLongitude", record.getCheckOutLongitude(),
                        "attendance_records.faceVerified", record.isFaceVerified(),
                        "attendance_records.locationVerified", record.isLocationVerified(),
                        "attendance_records.status", record.getStatus(),
                        "attendance_records.createdAt", record.getCreatedAt()));
            }
            for (CompensationTransaction transaction : transactionRepository.findAll()) {
                rowIndex = writeRecord(sheet, rowIndex, "COMPENSATION_TRANSACTION", fields(
                        "compensation_transactions.id", transaction.getId(),
                        "compensation_transactions.userId", transaction.getUserId(),
                        "compensation_transactions.type", transaction.getType(),
                        "compensation_transactions.amount", transaction.getAmount(),
                        "compensation_transactions.description", transaction.getDescription(),
                        "compensation_transactions.recordedByEmail", transaction.getRecordedByEmail(),
                        "compensation_transactions.createdAt", transaction.getCreatedAt()));
            }
            for (AttendanceLocation location : locationRepository.findAll()) {
                rowIndex = writeRecord(sheet, rowIndex, "ATTENDANCE_LOCATION", fields(
                        "attendance_locations.id", location.getId(),
                        "attendance_locations.name", location.getName(),
                        "attendance_locations.address", location.getAddress(),
                        "attendance_locations.latitude", location.getLatitude(),
                        "attendance_locations.longitude", location.getLongitude(),
                        "attendance_locations.radiusMeters", location.getRadiusMeters(),
                        "attendance_locations.active", location.isActive(),
                        "attendance_locations.createdAt", location.getCreatedAt(),
                        "attendance_locations.updatedAt", location.getUpdatedAt()));
            }
            for (AdminActivityLog activity : activityLogRepository.findAll()) {
                rowIndex = writeRecord(sheet, rowIndex, "ADMIN_ACTIVITY_LOG", fields(
                        "admin_activity_logs.id", activity.getId(),
                        "admin_activity_logs.adminId", activity.getAdminId(),
                        "admin_activity_logs.action", activity.getAction(),
                        "admin_activity_logs.details", activity.getDetails(),
                        "admin_activity_logs.createdAt", activity.getCreatedAt()));
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, Math.max(0, rowIndex - 1), 0, HEADERS.size() - 1));
            for (int column = 0; column < HEADERS.size(); column++) {
                sheet.setColumnWidth(column, Math.min(Math.max(HEADERS.get(column).length() + 2, 16) * 256, 15000));
            }

            workbook.write(output);
            byte[] generatedWorkbook = output.toByteArray();
            AdminDataExport storedExport = dataExportRepository
                    .findById(AdminDataExport.LATEST_EXPORT_ID)
                    .orElseGet(AdminDataExport::new);
            storedExport.setFileName("attendease-data.xlsx");
            storedExport.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            storedExport.setContentBase64(Base64.getEncoder().encodeToString(generatedWorkbook));
            dataExportRepository.save(storedExport);
            return generatedWorkbook;
        }
    }

    private void writeCompensationSummary(Workbook workbook) {
        Sheet sheet = workbook.createSheet("User compensation");
        CellStyle headerStyle = createHeaderStyle(workbook);
        Row header = sheet.createRow(0);
        for (int column = 0; column < COMPENSATION_HEADERS.size(); column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(COMPENSATION_HEADERS.get(column));
            cell.setCellStyle(headerStyle);
        }

        YearMonth currentMonth = YearMonth.now();
        List<UserSummary> users = adminService.getUsers(currentMonth.getYear(), currentMonth.getMonthValue());
        int rowIndex = 1;
        for (UserSummary user : users) {
            Row row = sheet.createRow(rowIndex++);
            writeCell(row.createCell(0), user.getFullName());
            writeCell(row.createCell(1), user.getUsername());
            if (user.getDailySalary() != null) {
                writeCell(row.createCell(2), user.getDailySalary());
            }
            writeCell(row.createCell(3), user.getWorkingDaysThisMonth());
            if (user.getEarnedPayThisMonth() != null) {
                writeCell(row.createCell(4), user.getEarnedPayThisMonth());
            }
            writeCell(row.createCell(5), user.getSalaryPaidThisMonth() == null
                    ? BigDecimal.ZERO : user.getSalaryPaidThisMonth());
            writeCell(row.createCell(6), user.getMonthlyAdvance() == null
                    ? BigDecimal.ZERO : user.getMonthlyAdvance());
            if (user.getTotalBalanceThisMonth() != null) {
                writeCell(row.createCell(7), user.getTotalBalanceThisMonth());
            }
            writeCell(row.createCell(8), user.isActive() ? "Approved" : "Pending");
        }

        for (int column = 0; column < COMPENSATION_HEADERS.size(); column++) {
            sheet.autoSizeColumn(column);
        }
    }

    @Transactional(readOnly = true)
    public Optional<StoredWorkbook> getLatestWorkbook() {
        return dataExportRepository.findById(AdminDataExport.LATEST_EXPORT_ID)
                .map(stored -> new StoredWorkbook(
                        stored.getFileName(),
                        stored.getContentType(),
                        Base64.getDecoder().decode(stored.getContentBase64()),
                        stored.getGeneratedAt()));
    }

    private int writeRecord(Sheet sheet, int rowIndex, String recordType, Map<String, Object> values) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(recordType);
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            int column = HEADERS.indexOf(entry.getKey());
            if (column >= 0 && entry.getValue() != null) {
                writeCell(row.createCell(column), entry.getValue());
            }
        }
        return rowIndex;
    }

    private Map<String, Object> fields(Object... keyValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            values.put((String) keyValues[index], keyValues[index + 1]);
        }
        return values;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeCell(Cell cell, Object value) {
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.toString());
        } else if (value instanceof Enum<?> enumValue) {
            cell.setCellValue(enumValue.name());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    public record StoredWorkbook(String fileName, String contentType, byte[] content,
                                 LocalDateTime generatedAt) {
    }
}
