Feature: List hospitals with their specialties and bed availability
  As a clinician preparing to route an emergency,
  I need the network of hospitals with their current specialties and bed counts
  so I can confirm availability before choosing where to send a patient.

  Scenario: Authenticated user retrieves hospitals with specialty and bed data
    Given an authenticated user
    When the user requests "/api/v1/hospitals"
    Then the response status is 200
    And the response lists all 12 seeded hospitals
    And "Fred Brooks Hospital" offers "Cardiology" with 2 available beds
    And "Fred Brooks Hospital" offers "Immunology" with 3 available beds
