package com.sdm.backend.service;

import com.sdm.backend.entity.Student;
import com.sdm.backend.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentService {

    @Value("${app.student.initial-password-fallback:123123}")
    private String initialPasswordFallback;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UserService userService;

    public int createStudent(Student student) {
        return studentMapper.insert(student);
    }

    public Student findByUserId(Long userId) {
        return studentMapper.findByUserId(userId);
    }

    public Student findByStudentNumber(String studentNumber) {
        return studentMapper.findByStudentNumber(studentNumber);
    }

    public List<Student> findByCounselorId(Long counselorId) {
        return studentMapper.findByCounselorId(counselorId);
    }

    public Map<String, Object> bulkAssignCounselors(List<Long> counselorIds, boolean overwriteExisting) {
        List<com.sdm.backend.entity.User> availableCounselors = userService.findByRole("COUNSELOR").stream()
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .filter(user -> counselorIds == null || counselorIds.isEmpty() || counselorIds.contains(user.getId()))
                .sorted(Comparator.comparing(com.sdm.backend.entity.User::getId))
                .toList();

        if (availableCounselors.isEmpty()) {
            throw new IllegalArgumentException("No active counselors available for assignment");
        }

        List<Student> allStudents = studentMapper.findAll().stream()
                .sorted(Comparator
                        .comparing((Student student) -> safeText(student.getClassName()))
                        .thenComparing(student -> safeText(student.getMajor()))
                        .thenComparing(student -> safeText(student.getStudentNumber())))
                .toList();

        List<Student> candidates = allStudents.stream()
                .filter(student -> overwriteExisting || student.getCounselorId() == null)
                .toList();

        Set<Long> candidateIds = candidates.stream().map(Student::getId).collect(java.util.stream.Collectors.toSet());
        Map<Long, Integer> counselorLoads = new LinkedHashMap<>();
        for (com.sdm.backend.entity.User counselor : availableCounselors) {
            counselorLoads.put(counselor.getId(), 0);
        }
        for (Student student : allStudents) {
            if (student.getCounselorId() != null
                    && counselorLoads.containsKey(student.getCounselorId())
                    && !candidateIds.contains(student.getId())) {
                counselorLoads.computeIfPresent(student.getCounselorId(), (key, value) -> value + 1);
            }
        }

        Map<String, List<Student>> groupedStudents = new LinkedHashMap<>();
        for (Student student : candidates) {
            groupedStudents.computeIfAbsent(buildCounselorGroupKey(student), ignored -> new ArrayList<>()).add(student);
        }

        Map<Long, Integer> assignedByCounselor = new LinkedHashMap<>();
        for (com.sdm.backend.entity.User counselor : availableCounselors) {
            assignedByCounselor.put(counselor.getId(), 0);
        }

        int updatedCount = 0;
        for (List<Student> group : groupedStudents.values()) {
            Long selectedCounselorId = availableCounselors.stream()
                    .map(com.sdm.backend.entity.User::getId)
                    .min(Comparator
                            .comparing((Long counselorId) -> counselorLoads.getOrDefault(counselorId, 0))
                            .thenComparing(Long::longValue))
                    .orElseThrow(() -> new IllegalArgumentException("No counselor available"));

            for (Student student : group) {
                studentMapper.updateCounselorById(student.getId(), selectedCounselorId);
                updatedCount++;
            }

            int groupSize = group.size();
            counselorLoads.computeIfPresent(selectedCounselorId, (key, value) -> value + groupSize);
            assignedByCounselor.computeIfPresent(selectedCounselorId, (key, value) -> value + groupSize);
        }

        List<Map<String, Object>> counselorSummary = new ArrayList<>();
        for (com.sdm.backend.entity.User counselor : availableCounselors) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("counselorId", counselor.getId());
            item.put("counselorName", counselor.getRealName());
            item.put("assignedStudents", assignedByCounselor.getOrDefault(counselor.getId(), 0));
            item.put("finalLoad", counselorLoads.getOrDefault(counselor.getId(), 0));
            counselorSummary.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidateCount", candidates.size());
        result.put("updatedCount", updatedCount);
        result.put("counselorCount", availableCounselors.size());
        result.put("groupCount", groupedStudents.size());
        result.put("overwriteExisting", overwriteExisting);
        result.put("counselorAssignments", counselorSummary);
        return result;
    }

    public Long getUserIdByStudentId(Long studentId) {
        Student student = studentMapper.findById(studentId);
        return student != null ? student.getUserId() : null;
    }

    public Long getStudentIdByUserId(Long userId) {
        Student student = studentMapper.findByUserId(userId);
        return student != null ? student.getId() : null;
    }

    public int updateStudent(Student student) {
        return studentMapper.update(student);
    }

    public int deleteByUserId(Long userId) {
        return studentMapper.deleteByUserId(userId);
    }

    public List<Student> findAll() {
        return studentMapper.findAll();
    }

    public List<Student> findStudentsForExport(String role, Long currentUserId) {
        if ("COUNSELOR".equals(role) && currentUserId != null) {
            return studentMapper.findByCounselorId(currentUserId);
        }
        return studentMapper.findAll();
    }

    public List<Student> findByPage(int page, int size) {
        int offset = (page - 1) * size;
        return studentMapper.findByPage(offset, size);
    }

    public List<Student> findByPageAndFilters(int page, int size, String className, String major, Long counselorId) {
        int offset = (page - 1) * size;
        return studentMapper.findByPageAndFilters(offset, size, className, major, counselorId);
    }

    public int countAll() {
        return studentMapper.countAll();
    }

    public int countByFilters(String className, String major, Long counselorId) {
        return studentMapper.countByFilters(className, major, counselorId);
    }

    public Student findById(Long id) {
        return studentMapper.findById(id);
    }

    public Student findByStudentNumberExclude(String studentNumber, Long excludeUserId) {
        return studentMapper.findByStudentNumberExclude(studentNumber, excludeUserId);
    }

    @org.springframework.transaction.annotation.Transactional
    public int createStudentUser(Student student, String password) {
        com.sdm.backend.entity.User user = new com.sdm.backend.entity.User();
        user.setUsername(student.getStudentNumber());
        user.setPassword(password);
        user.setRealName(student.getRealName());
        user.setRole("STUDENT");
        user.setStatus(1);
        userService.insert(user);

        student.setUserId(user.getId());
        return studentMapper.insert(student);
    }

    @org.springframework.transaction.annotation.Transactional
    public int updateStudentInfo(Student student) {
        if (student.getRealName() != null) {
            com.sdm.backend.entity.User user = new com.sdm.backend.entity.User();
            user.setId(student.getUserId());
            user.setRealName(student.getRealName());
            userService.update(user);
        }

        return studentMapper.update(student);
    }

    public com.sdm.backend.entity.User findByUsername(String username) {
        return userService.findByUsername(username);
    }

    public String generateInitialPassword(String studentNumber) {
        String normalized = studentNumber == null ? "" : studentNumber.trim();
        if (normalized.isEmpty()) {
            return initialPasswordFallback;
        }
        if (normalized.length() <= 6) {
            return normalized;
        }
        return normalized.substring(normalized.length() - 6);
    }

    public Long getCounselorIdByStudentId(Long studentId) {
        Student student = studentMapper.findById(studentId);
        return student != null ? student.getCounselorId() : null;
    }

    public List<Long> findStudentsOnLeave(List<Long> studentIds, LocalDate date) {
        java.time.LocalDateTime startOfDay = date.atStartOfDay();
        java.time.LocalDateTime endOfDay = date.atTime(23, 59, 59);

        return studentMapper.findStudentsOnLeave(studentIds, startOfDay, endOfDay);
    }

    private String buildCounselorGroupKey(Student student) {
        String className = safeText(student.getClassName());
        if (!className.isEmpty()) {
            return "CLASS:" + className;
        }

        String major = safeText(student.getMajor());
        if (!major.isEmpty()) {
            return "MAJOR:" + major;
        }

        return "STUDENT:" + student.getId();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
