Feature: Reject malformed recommendation requests and surface bed-exhaustion clearly
  As a client of the recommendation API,
  I need invalid inputs to come back as 400 with field-level detail,
  and the rare case where no bed is available anywhere in the network
  to come back as 404 with a stable error code,
  so error handling on the caller side stays deterministic and human-readable.

  Scenario: Invalid coordinates are rejected with 400
    Given an authenticated user
    When the user posts a recommendation with specialty "Cardiology", latitude 999.0 and longitude 0.0
    Then the response status is 400
    And the error code is "VALIDATION_ERROR"
    And the error details include field "latitude"

  Scenario: No beds available anywhere returns 404
    Given an authenticated user
    And no hospital has any available beds
    When the user requests a recommendation for "Cardiology" near latitude 51.523 longitude -0.131
    Then the response status is 404
    And the error code is "NO_BEDS_AVAILABLE"
