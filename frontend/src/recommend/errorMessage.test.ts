import { describe, expect, it } from 'vitest'
import { ApiError } from '../auth/ApiError'
import { errorMessageFor } from './errorMessage'

describe('errorMessageFor', () => {
  it('maps a 400 VALIDATION_ERROR with field details into a field list', () => {
    const error = new ApiError(400, 'VALIDATION_ERROR', 'Request body failed validation', [
      { field: 'latitude', message: 'must be between -90 and 90' },
      { field: 'longitude', message: 'must be between -180 and 180' },
    ])

    expect(errorMessageFor(error)).toBe('Please check: latitude, longitude')
  })

  it('maps a 400 with a single field detail into a single-item list', () => {
    const error = new ApiError(400, 'VALIDATION_ERROR', 'invalid', [
      { field: 'latitude', message: 'out of range' },
    ])

    expect(errorMessageFor(error)).toBe('Please check: latitude')
  })

  it('falls back to a generic validation message when a 400 has no field details', () => {
    const error = new ApiError(400, 'MALFORMED_REQUEST', 'Request body is missing or malformed')

    expect(errorMessageFor(error)).toBe('Please check the values you entered.')
  })

  it('maps a 404 NO_BEDS_AVAILABLE to the dispatch-line message', () => {
    const error = new ApiError(404, 'NO_BEDS_AVAILABLE', 'No beds available anywhere')

    expect(errorMessageFor(error)).toBe(
      'No hospitals have beds available right now. Please call the regional dispatch line.',
    )
  })

  it('maps a 404 SPECIALTY_NOT_FOUND to a pick-another-specialty message', () => {
    const error = new ApiError(404, 'SPECIALTY_NOT_FOUND', 'Specialty 999 not found')

    expect(errorMessageFor(error)).toBe(
      'The selected specialty is no longer available. Please pick another.',
    )
  })

  it('maps a 404 HOSPITAL_NOT_FOUND to the same pick-another-specialty message', () => {
    const error = new ApiError(404, 'HOSPITAL_NOT_FOUND', 'Hospital 12 not found')

    expect(errorMessageFor(error)).toBe(
      'The selected specialty is no longer available. Please pick another.',
    )
  })

  it('maps a 503 RESERVATION_CONFLICT to the overloaded message', () => {
    const error = new ApiError(503, 'RESERVATION_CONFLICT', 'Bed reservation could not complete')

    expect(errorMessageFor(error)).toBe(
      'The system is under heavy load and could not secure a bed. Please retry.',
    )
  })

  it('maps a 503 ROUTING_UNAVAILABLE to the same overloaded message', () => {
    const error = new ApiError(503, 'ROUTING_UNAVAILABLE', 'Routing service is unavailable')

    expect(errorMessageFor(error)).toBe(
      'The system is under heavy load and could not secure a bed. Please retry.',
    )
  })

  it('maps a network ApiError to the connection-check message', () => {
    const error = ApiError.network()

    expect(errorMessageFor(error)).toBe('Could not reach the backend. Check your connection.')
  })

  it('falls back to a generic message for unmapped statuses (e.g. 500 INTERNAL_ERROR)', () => {
    const error = new ApiError(500, 'INTERNAL_ERROR', 'An unexpected error occurred')

    expect(errorMessageFor(error)).toBe('Something went wrong. Please retry in a moment.')
  })
})
