Feature: Fall back to the nearest hospital with any available bed when no site offers the requested specialty
  As a clinician responding to an emergency,
  when the requested specialty has no free bed anywhere in the network,
  I still need the system to route the patient to the nearest hospital
  that can receive them, flagged clearly as a fallback so the care team
  knows the served specialty differs from the one originally requested.

  Scenario: Fallback when no bed matches the requested specialty
    Given an authenticated user
    And no hospital has available beds for "Cardiology"
    When the user requests a recommendation for "Cardiology" near latitude 51.523 longitude -0.131
    Then the response status is 200
    And the recommendation is marked as a fallback
    And the requested specialty is "Cardiology"
    And the specialty served is not "Cardiology"
    And the bed is reserved
