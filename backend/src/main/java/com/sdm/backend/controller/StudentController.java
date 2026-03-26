package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.Student;
import com.sdm.backend.service.StudentService;
import com.sdm.backend.util.ExcelExportUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

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
                return ResponseEntity.ok(Result.error(404, "学生不存在"));
            }
            if (!student.getCounselorId().equals(getCurrentUserId())) {
                return ResponseEntity.ok(Result.error(403, "无权查看非本班学生"));
            }
            return ResponseEntity.ok(Result.success(student));
        }
        
        Student student = studentService.findById(id);
        if (student == null) {
            return ResponseEntity.ok(Result.error(404, "学生不存在"));
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
    @Log(module = "STUDENT", operation = "CREATE", description = "新增学生")
    public ResponseEntity<Result<Void>> createStudent(@RequestBody Student student) {
        // 验证学号是否重复
        Student existing = studentService.findByStudentNumber(student.getStudentNumber());
        if (existing != null) {
            return ResponseEntity.ok(Result.error(400, "学号已存在"));
        }

        // 如果是辅导员，自动设置辅导员 ID 为当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().stream()
            .findFirst().get().getAuthority();
        
        if ("ROLE_COUNSELOR".equals(role)) {
            student.setCounselorId(getCurrentUserId());
        }

        // 设置默认密码为 123123
        String password = "123123";

        studentService.createStudentUser(student, password);
        return ResponseEntity.ok(Result.success(null, "学生创建成功，默认密码为 123123"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COUNSELOR')")
    @Log(module = "STUDENT", operation = "UPDATE", description = "修改学生信息")
    public ResponseEntity<Result<Void>> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication.getAuthorities().stream()
            .findFirst().get().getAuthority();
        
        Student existing = studentService.findById(id);
        if (existing == null) {
            return ResponseEntity.ok(Result.error(404, "学生不存在"));
        }
        
        if ("ROLE_COUNSELOR".equals(role)) {
            if (!existing.getCounselorId().equals(getCurrentUserId())) {
                return ResponseEntity.ok(Result.error(403, "无权修改非本班学生"));
            }
        }

        // 验证学号是否重复（排除当前学生）
        Student duplicate = studentService.findByStudentNumberExclude(student.getStudentNumber(), existing.getUserId());
        if (duplicate != null) {
            return ResponseEntity.ok(Result.error(400, "学号已存在"));
        }

        student.setId(id);
        student.setUserId(existing.getUserId());
        studentService.updateStudentInfo(student);
        return ResponseEntity.ok(Result.success(null, "学生信息更新成功"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "STUDENT", operation = "DELETE", description = "删除学生")
    public ResponseEntity<Result<Void>> deleteStudent(@PathVariable Long id) {
        Student student = studentService.findById(id);
        if (student == null) {
            return ResponseEntity.ok(Result.error(404, "学生不存在"));
        }
        studentService.deleteByUserId(student.getUserId());
        return ResponseEntity.ok(Result.success(null, "学生删除成功"));
    }

    /**
     * 导出学生名单 Excel
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('COUNSELOR')")
    @Log(module = "STUDENT", operation = "EXPORT", description = "导出学生名单")
    public ResponseEntity<byte[]> exportStudents() {
        try {
            List<Student> students = studentService.findAll();
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ExcelExportUtil.exportExcel(students, Student.class, "学生名单", os);
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"学生名单.xlsx\"")
                .body(os.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Excel 批量导入学生
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Log(module = "STUDENT", operation = "IMPORT", description = "批量导入学生")
    public ResponseEntity<Result<Map<String, Object>>> importStudents(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "请选择文件"));
        }

        List<Student> students = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = 0;

            for (Row row : sheet) {
                // 跳过标题行
                if (rowCount == 0) {
                    rowCount++;
                    continue;
                }

                try {
                    Student student = new Student();
                    
                    // 读取单元格数据
                    student.setStudentNumber(getCellValue(row.getCell(0)));
                    student.setRealName(getCellValue(row.getCell(1)));
                    student.setClassName(getCellValue(row.getCell(2)));
                    student.setMajor(getCellValue(row.getCell(3)));
                    
                    // 辅导员 ID（从用户名查找，暂简化为 null）
                    // student.setCounselorId(...);
                    
                    // 入学日期
                    Cell dateCell = row.getCell(4);
                    if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC) {
                        Date date = Date.from(dateCell.getLocalDateTimeCellValue().atZone(ZoneId.systemDefault()).toInstant());
                        student.setEnrollmentDate(LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()));
                    }

                    // 验证必填字段
                    if (student.getStudentNumber() == null || student.getStudentNumber().isEmpty()) {
                        errors.add("第" + (rowCount + 1) + "行：学号不能为空");
                        rowCount++;
                        continue;
                    }
                    if (student.getRealName() == null || student.getRealName().isEmpty()) {
                        errors.add("第" + (rowCount + 1) + "行：姓名不能为空");
                        rowCount++;
                        continue;
                    }

                    students.add(student);
                } catch (Exception e) {
                    errors.add("第" + (rowCount + 1) + "行：解析失败 - " + e.getMessage());
                }
                
                rowCount++;
            }
        } catch (IOException e) {
            return ResponseEntity.ok(Result.error(500, "文件解析失败：" + e.getMessage()));
        }

        // 如果没有错误，批量导入
        if (errors.isEmpty() && !students.isEmpty()) {
            int successCount = 0;
            for (Student student : students) {
                try {
                    // 检查学号是否已存在
                    Student existing = studentService.findByStudentNumber(student.getStudentNumber());
                    if (existing != null) {
                        errors.add("学号 " + student.getStudentNumber() + " 已存在，已跳过");
                        continue;
                    }

                    // 设置默认密码为 123123
                    String password = "123123";

                    studentService.createStudentUser(student, password);
                    successCount++;
                } catch (Exception e) {
                    errors.add("学号 " + student.getStudentNumber() + " 导入失败：" + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("errors", errors);
            
            if (successCount > 0 && errors.isEmpty()) {
                return ResponseEntity.ok(Result.success(result, "导入成功 " + successCount + " 条记录"));
            } else if (successCount > 0) {
                return ResponseEntity.ok(Result.success(result, "部分导入成功：" + successCount + " 条，失败：" + errors.size() + " 条"));
            } else {
                return ResponseEntity.ok(Result.error(400, "导入失败"));
            }
        } else {
            Map<String, Object> result = new HashMap<>();
            result.put("errors", errors);
            return ResponseEntity.ok(Result.error(400, "数据验证失败"));
        }
    }

    /**
     * 获取 Excel 导入模板
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> getImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生模板");
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"学号", "姓名", "班级", "专业", "入学日期"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 示例数据
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("2021001");
            dataRow.createCell(1).setCellValue("张三");
            dataRow.createCell(2).setCellValue("计算机 2101 班");
            dataRow.createCell(3).setCellValue("计算机科学与技术");
            dataRow.createCell(4).setCellValue(LocalDate.of(2021, 9, 1).toString());

            // 设置列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 输出 Excel 文件
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                byte[] bytes = outputStream.toByteArray();
                
                // 对文件名进行 URL 编码，防止中文乱码
                String encodedFileName = java.net.URLEncoder.encode("学生导入模板.xlsx", "UTF-8").replace("+", "%20");
                
                org.springframework.http.HttpHeaders headers_map = new org.springframework.http.HttpHeaders();
                headers_map.setContentType(org.springframework.http.MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers_map.setContentDisposition(
                    org.springframework.http.ContentDisposition
                        .attachment()
                        .filename(encodedFileName)
                        .build());
                
                return ResponseEntity.ok()
                    .headers(headers_map)
                    .body(bytes);
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        com.sdm.backend.entity.User user = studentService.findByUsername(username);
        return user != null ? user.getId() : null;
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
}
