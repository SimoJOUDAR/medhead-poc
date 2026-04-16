import { ApiError } from '../auth/ApiError'

const NO_BEDS =
  'No hospitals have beds available right now. Please call the regional dispatch line.'
const SPECIALTY_GONE = 'The selected specialty is no longer available. Please pick another.'
const OVERLOADED =
  'The system is under heavy load and could not secure a bed. Please retry.'
const NETWORK = 'Could not reach the backend. Check your connection.'
const GENERIC = 'Something went wrong. Please retry in a moment.'
const VALIDATION_FALLBACK = 'Please check the values you entered.'

export function errorMessageFor(error: ApiError): string {
  if (error.code === 'NETWORK_ERROR') {
    return NETWORK
  }

  switch (error.status) {
    case 400: {
      const fields = error.details.map((detail) => detail.field).filter((field) => field.length > 0)
      if (fields.length === 0) {
        return VALIDATION_FALLBACK
      }
      return `Please check: ${fields.join(', ')}`
    }
    case 404:
      if (error.code === 'NO_BEDS_AVAILABLE') {
        return NO_BEDS
      }
      if (error.code === 'SPECIALTY_NOT_FOUND' || error.code === 'HOSPITAL_NOT_FOUND') {
        return SPECIALTY_GONE
      }
      return GENERIC
    case 503:
      return OVERLOADED
    default:
      return GENERIC
  }
}
