package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.BulkAssignCounselorRequest;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Student;
import com.sdm.backend.service.StudentService;
import com.sdm.backend.util.ExcelExportUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<Student>> getStudentById(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().stream()
            .findFirst().get().getAuthority();

        if ("ROLE_COUNSELOR".equals(role)) {
            Student student = studentService.findById(id);
            if (student == null) {
                return ResponseEntity.ok(Result.error(404, "Student not found"));
            }
            if (!student.getCounselorId().equals(getCurrentUserId())) {
                return ResponseEntity.ok(Result.error(403, "No permission to view this student"));
            }
            return ResponseEntity.ok(Result.success(student));
        }

        Student student = studentService.findById(id);
        if (student == null) {
            return ResponseEntity.ok(Result.error(404, "Student not found"));
        }
        return ResponseEntity.ok(Result.success(student));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COUNSELOR')")
    public ResponseEntity<Result<Map<String, Object>>> getStudentList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) Long counselorId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().stream()
            .findFirst().get().getAuthority();

        if ("ROLE_COUNSELOR".equals(role)) {
            counselorId = getCurrentUserId();
        }

        List<Student> students;
        int total;

        if (className != null || major != null || counselorId != null) {
            students = studentService.findByPageAndFilters(page, size, className, major, counselorId);
            total = studentService.countByFilters(className, major, counselorId);
        } else {
            students = studentService.findByPage(page, size);
            total = studentService.countAll();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", students);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Result<Student>> getCurrentStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        com.sdm.backend.entity.User user = studentService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "Not logged in"));
        }

        Student student = studentService.findByUserId(user.getId());
        if (student == null) {
            return ResponseEntity.ok(Result.error(404, "Student profile not found"));
        }
        return ResponseEntity.ok(Result.success(student));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COUNSELOR')")
    @Log(module = "STUDENT", operation = "CREATE", description = "Create student")
    public ResponseEntity<Result<Void>> createStudent(@RequestBody Student student) {
        Student existing = studentService.findByStudentNumber(student.getStudentNumber());
        if (existing != null) {
            return ResponseEntity.ok(Result.error(400, "Student number already exists"));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().stream()
            .findFirst().get().getAuthority();

        if ("ROLE_COUNSELOR".equals(role)) {
            student.setCounselorId(getCurrentUserId());
        }

        String password = studentService.generateInitialPassword(student.getStudentNumber());
        studentService.createStudentUser(student, password);
        return ResponseEntity.ok(Result.success(null, "Student created. Initial password uses the last 6 digits of the student number."));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COUNSELOR')")
    @Log(module = "STUDENT", operation = "UPDATE", description = "Update student")
    public ResponseEntity<Result<Void>> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().stream()
            .findFirst().get().getAuthority();

        Student existing = studentService.findById(id);
        if (existing == null) {
            return ResponseEntity.ok(Result.error(404, "Student not found"));
        }

        if ("ROLE_COUNSELOR".equals(role) && !existing.getCounselorId().equals(getCurrentUserId())) {
            return ResponseEntity.ok(Result.error(403, "No permission to update this student"));
        }

        Student duplicate = studentService.findByStudentNumberExclude(student.getStudentNumber(), existing.getUserId());
        if (duplicate != null) {
            return ResponseEntity.ok(Result.error(400, "Student number already exists"));
        }

        student.setId(id);
        student.setUserId(existing.getUserId());
        studentService.updateStudentInfo(student);
        return ResponseEntity.ok(Result.success(null, "Student updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "STUDENT", operation = "DELETE", description = "Delete student")
    public ResponseEntity<Result<Void>> deleteStudent(@PathVariable Long id) {
        Student student = studentService.findById(id);
        if (student == null) {
            return ResponseEntity.ok(Result.error(404, "Student not found"));
        }
        studentService.deleteByUserId(student.getUserId());
        return ResponseEntity.ok(Result.success(null, "Student deleted successfully"));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COUNSELOR')")
    @Log(module = "STUDENT", operation = "EXPORT", description = "Export students")
    public ResponseEntity<byte[]> exportStudents() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String authority = authentication.getAuthorities().stream()
                .findFirst().map(Object::toString).orElse("");
            String role = authority.startsWith("ROLE_") ? authority.substring(5) : authority;
            List<Student> students = studentService.findStudentsForExport(role, getCurrentUserId());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ExcelExportUtil.exportExcel(students, Student.class, "Students", os);

            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"students.xlsx\"")
                .body(os.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "STUDENT", operation = "IMPORT", description = "Import students")
    public ResponseEntity<Result<Map<String, Object>>> importStudents(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "Please select a file"));
        }

        List<Student> students = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = 0;

            for (Row row : sheet) {
                if (rowCount == 0) {
                    rowCount++;
                    continue;
                }

                try {
                    Student student = new Student();
                    student.setStudentNumber(getCellValue(row.getCell(0)));
                    student.setRealName(getCellValue(row.getCell(1)));
                    student.setClassName(getCellValue(row.getCell(2)));
                    student.setMajor(getCellValue(row.getCell(3)));

                    Cell dateCell = row.getCell(4);
                    if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC) {
                        Date date = Date.from(dateCell.getLocalDateTimeCellValue().atZone(ZoneId.systemDefault()).toInstant());
                        student.setEnrollmentDate(LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()));
                    }

                    if (student.getStudentNumber() == null || student.getStudentNumber().isEmpty()) {
                        errors.add("Row " + (rowCount + 1) + ": student number is required");
                        rowCount++;
                        continue;
                    }
                    if (student.getRealName() == null || student.getRealName().isEmpty()) {
                        errors.add("Row " + (rowCount + 1) + ": name is required");
                        rowCount++;
                        continue;
                    }

                    students.add(student);
                } catch (Exception e) {
                    errors.add("Row " + (rowCount + 1) + ": parse failed - " + e.getMessage());
                }

                rowCount++;
            }
        } catch (IOException e) {
            return ResponseEntity.ok(Result.error(500, "File parse failed: " + e.getMessage()));
        }

        if (errors.isEmpty() && !students.isEmpty()) {
            int successCount = 0;
            for (Student student : students) {
                try {
                    Student existing = studentService.findByStudentNumber(student.getStudentNumber());
                    if (existing != null) {
                        errors.add("Student number " + student.getStudentNumber() + " already exists, skipped");
                        continue;
                    }

                    String password = studentService.generateInitialPassword(student.getStudentNumber());
                    studentService.createStudentUser(student, password);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Student number " + student.getStudentNumber() + " import failed: " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("errors", errors);

            if (successCount > 0 && errors.isEmpty()) {
                return ResponseEntity.ok(Result.success(result, "Imported " + successCount + " students successfully"));
            } else if (successCount > 0) {
                return ResponseEntity.ok(Result.success(result, "Partially imported " + successCount + " students"));
            } else {
                return ResponseEntity.ok(Result.error(400, "Import failed"));
            }
        } else {
            Map<String, Object> result = new HashMap<>();
            result.put("errors", errors);
            return ResponseEntity.ok(Result.error(400, "Validation failed"));
        }
    }

    @PostMapping("/bulk-assign-counselors")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "STUDENT", operation = "UPDATE", description = "Bulk assign counselors")
    public ResponseEntity<Result<Map<String, Object>>> bulkAssignCounselors(@RequestBody BulkAssignCounselorRequest request) {
        try {
            Map<String, Object> result = studentService.bulkAssignCounselors(
                    request.getCounselorIds(),
                    Boolean.TRUE.equals(request.getOverwriteExisting())
            );
            return ResponseEntity.ok(Result.success(result, "Bulk counselor assignment completed"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(Result.error(400, ex.getMessage()));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> getImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("students");

            Row headerRow = sheet.createRow(0);
            String[] templateHeaders = {"Student Number", "Name", "Class", "Major", "Enrollment Date"};

            for (int i = 0; i < templateHeaders.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(templateHeaders[i]);
            }

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("2021001");
            dataRow.createCell(1).setCellValue("Alice");
            dataRow.createCell(2).setCellValue("CS-2101");
            dataRow.createCell(3).setCellValue("Computer Science");
            dataRow.createCell(4).setCellValue(LocalDate.of(2021, 9, 1).toString());

            for (int i = 0; i < templateHeaders.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                byte[] bytes = outputStream.toByteArray();

                String encodedFileName = java.net.URLEncoder.encode("student-import-template.xlsx", "UTF-8").replace("+", "%20");

                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment().filename(encodedFileName).build());

                return ResponseEntity.ok().headers(headers).body(bytes);
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        com.sdm.backend.entity.User user = studentService.findByUsername(username);
        return user != null ? user.getId() : null;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value)) {
                    yield String.valueOf((long) value);
                }
                yield String.valueOf(value);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}
