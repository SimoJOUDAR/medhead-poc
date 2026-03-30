Feature: Recommend the nearest hospital offering the requested specialty and reserve a bed
  As a clinician responding to an emergency,
  I need the system to pick the nearest hospital with an available bed
  in the required specialty, reserve that bed atomically, and announce the
  reservation so downstream systems stay in sync,
  so the patient is routed to a compatible site without manual triage
  and the bed register reflects the allocation immediately.

  Scenario: Nearest cardiology-capable hospital is recommended, its bed reserved, and a reservation event is published
    Given an authenticated user
    And "Fred Brooks Hospital" has 2 available beds for "Cardiology"
    When the user requests a recommendation for "Cardiology" near latitude 51.523 longitude -0.131
    Then the response status is 200
    And the recommended hospital is "Fred Brooks Hospital"
    And the specialty served is "Cardiology"
    And the recommendation is not marked as a fallback
    And the bed is reserved
    And "Fred Brooks Hospital" now has 1 available bed for "Cardiology"
    And a bed-reservation event was published for "Fred Brooks Hospital" and "Cardiology" with 1 remaining bed
