Feature: Recommend the nearest hospital offering the requested specialty
  As a clinician responding to an emergency,
  I need the system to pick the nearest hospital with an available bed
  in the required specialty
  so the patient is routed to a compatible site without manual triage.

  Scenario: Nearest hospital with an available bed in the requested specialty is suggested
    Given an authenticated user
    When the user requests a recommendation for "Cardiology" near latitude 51.523 longitude -0.131
    Then the response status is 200
    And the recommended hospital is "Fred Brooks Hospital"
    And the specialty served is "Cardiology"
    And the recommendation is not marked as a fallback
    And the bed is not yet reserved
