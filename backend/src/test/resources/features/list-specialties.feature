Feature: List available specialties
  As a clinician triaging an emergency,
  I need the NHS specialty catalogue
  so I can select the care a patient requires.

  Scenario: Authenticated user retrieves the full NHS specialty catalogue
    Given an authenticated user
    When the user requests "/api/v1/specialties"
    Then the response status is 200
    And the response lists the full NHS specialty catalogue
    And every entry carries an id, a name and a group
