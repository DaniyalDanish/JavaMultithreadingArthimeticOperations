package com.example.springboot;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.springboot.services.SalaryService;

import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class SalaryController {

@Autowired
private SalaryService salaryService;

   @GetMapping("/salaries")
   public void calculateSalaries() {
    salaryService.calculateSalaries();
   }

}

