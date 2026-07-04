package user.service.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @GetMapping("/ping")
    public String ping() {
        return "user-service OK";
    }
}