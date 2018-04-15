Feature: Swagger

  Scenario: User downloads Swagger JSON
    Given a user makes a request to get "swagger.json"
     Then the response status code is 200
      And the response contains valid JSON
