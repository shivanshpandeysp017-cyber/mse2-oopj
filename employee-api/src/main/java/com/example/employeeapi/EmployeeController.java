package com.example.employeeapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final Map<Integer, Employee> employees = new LinkedHashMap<>();

    @PostMapping
    public ResponseEntity<?> addEmployee(@RequestBody Employee employee) {
        if (isInvalid(employee)) {
            return ResponseEntity.badRequest().body("Enter valid employee details.");
        }

        if (employees.containsKey(employee.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("That employee ID already exists.");
        }

        employees.put(employee.getId(), employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return new ArrayList<>(employees.values());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable int id) {
        Employee employee = employees.get(id);

        if (employee == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(employee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable int id,
            @RequestBody Employee updatedEmployee) {

        if (!employees.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }

        if (isInvalid(updatedEmployee)) {
            return ResponseEntity.badRequest().body("Enter valid employee details.");
        }

        updatedEmployee.setId(id);
        employees.put(id, updatedEmployee);
        return ResponseEntity.ok(updatedEmployee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable int id) {
        if (employees.remove(id) == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    private boolean isInvalid(Employee employee) {
        return employee == null
                || employee.getId() <= 0
                || employee.getName() == null
                || employee.getName().isBlank()
                || employee.getDepartment() == null
                || employee.getDepartment().isBlank()
                || employee.getSalary() <= 0;
    }
}