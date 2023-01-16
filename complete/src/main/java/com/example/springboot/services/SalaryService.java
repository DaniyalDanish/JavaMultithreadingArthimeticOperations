package com.example.springboot.services;

import java.io.*;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.json.simple.JSONObject;
import java.net.URL;
import java.net.URLConnection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.springboot.DTO.Employee;

@Service
public class SalaryService {

    @Autowired
    private ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public void calculateSalaries() {

       String employeeString;
       List<Employee> employees;

           try {
               String resultString = callExternalApi();

               ObjectMapper mapper = new ObjectMapper();
               JsonNode root = mapper.readTree(resultString);
               JsonNode data = root.path("data");
               System.out.println(data);
               employeeString = data.toString();

               employees = objectMapper.readValue(employeeString, new TypeReference<List<Employee>>(){});

               Future<Double> sumFuture = executor.submit(() -> getSumSalary(employees));
               Future<Double> averageFuture = executor.submit(() -> getAverageSalary(employees));
               Future<Double> minFuture = executor.submit(() -> getMinSalary(employees));
               Future<Double> maxFuture = executor.submit(() -> getMaxSalary(employees));

               List<Future<Map.Entry<String, Double>>> deviationFutures = employees.stream()
                   .map(e -> executor.submit(() -> calculateDeviation(e, averageFuture.get())))
                   .collect(Collectors.toList());


               JSONObject result = new JSONObject();
               result.put("sum", sumFuture.get());
               result.put("avg", averageFuture.get());
               result.put("min", minFuture.get());
               result.put("max", maxFuture.get());

               JSONObject deviations = new JSONObject();
               for (Future<Map.Entry<String, Double>> deviationFuture : deviationFutures) {
                     Map.Entry<String, Double> deviation = deviationFuture.get();
                          deviations.put(deviation.getKey(), deviation.getValue());
                     }
                     result.put("deviations", deviations);

                         try (FileWriter file = new FileWriter("results.json")) {
                                  file.write(result.toJSONString());
                              } catch (IOException e) {
                                  System.out.println (e);
                              }
           executor.shutdown();
           }
           catch (Exception e) {
            System.out.println (e);
           }
    }


    public Double getSumSalary(List<Employee> employees) {
             return employees.stream().mapToDouble(Employee::getEmployee_salary).sum();
             }

    public Double getAverageSalary(List<Employee> employees) {
            return employees.stream().mapToDouble(Employee::getEmployee_salary).average().orElse(0);
            }

    public Double getMinSalary(List<Employee> employees) {
            return employees.stream().mapToDouble(Employee::getEmployee_salary).min().orElse(0);
            }

    public Double getMaxSalary(List<Employee> employees) {
            return employees.stream().mapToDouble(Employee::getEmployee_salary).max().orElse(0);
            }

    public Map.Entry<String, Double> calculateDeviation(Employee employee, double avg) {
           double deviation = (employee.getEmployee_salary() - avg) / avg * 100;
           return new AbstractMap.SimpleEntry<>(employee.getEmployee_name(), deviation);
           }

    private String callExternalApi() throws IOException {

          URL url = new URL("https://dummy.restapiexample.com/public/api/v1/employees");
          URLConnection connection = url.openConnection();
          BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

          StringBuilder data = new StringBuilder();
          String inputLine;
          while ((inputLine = in.readLine()) != null) {
              data.append(inputLine);
          }
          in.close();

          String jsonData = data.toString();
          return jsonData;
       }

}
