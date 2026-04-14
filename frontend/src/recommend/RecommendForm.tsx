import { useState, type FormEvent } from 'react'
import { LocationInputs, type LocationValue } from './LocationInputs'
import { RecommendationResult } from './RecommendationResult'
import { SpecialtyDropdown } from './SpecialtyDropdown'
import { useRecommendation } from './useRecommendation'
import { useSpecialties } from './useSpecialties'

const EMPTY_LOCATION: LocationValue = { latitude: null, longitude: null }

export function RecommendForm() {
  const {
    specialties,
    loading: specialtiesLoading,
    error: specialtiesError,
  } = useSpecialties()
  const { submit, result, loading: submitting } = useRecommendation()

  const [specialtyId, setSpecialtyId] = useState<number | null>(null)
  const [location, setLocation] = useState<LocationValue>(EMPTY_LOCATION)

  const canSubmit =
    specialtyId !== null &&
    location.latitude !== null &&
    location.longitude !== null &&
    !submitting

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (
      specialtyId === null ||
      location.latitude === null ||
      location.longitude === null ||
      submitting
    ) {
      return
    }
    await submit({
      specialtyId,
      latitude: location.latitude,
      longitude: location.longitude,
    })
  }

  return (
    <div>
      <form aria-label="Hospital recommendation" onSubmit={handleSubmit} noValidate>
        <SpecialtyDropdown
          specialties={specialties}
          loading={specialtiesLoading}
          error={specialtiesError}
          value={specialtyId}
          onChange={setSpecialtyId}
          disabled={submitting}
        />
        <LocationInputs value={location} onChange={setLocation} disabled={submitting} />
        <button type="submit" disabled={!canSubmit}>
          {submitting ? 'Finding the nearest hospital…' : 'Find hospital'}
        </button>
      </form>
      {submitting && !result && (
        <p role="status">Finding the nearest hospital…</p>
      )}
      {result && !submitting && <RecommendationResult result={result} />}
    </div>
  )
}
