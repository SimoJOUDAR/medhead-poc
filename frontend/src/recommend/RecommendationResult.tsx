import { useId } from 'react'
import type { RecommendationResponse } from './useRecommendation'

export interface RecommendationResultProps {
  result: RecommendationResponse
}

export function RecommendationResult({ result }: RecommendationResultProps) {
  const headingId = useId()
  const { hospital, specialty, requestedSpecialty, bedReserved, fallback } = result

  return (
    <article aria-labelledby={headingId}>
      <h2 id={headingId}>{hospital.name}</h2>
      {fallback && (
        <p role="status" data-testid="fallback-badge">
          Fallback match — no hospital currently has beds for the requested specialty.
        </p>
      )}
      <dl>
        <dt>Address</dt>
        <dd>{hospital.address}</dd>
        <dt>Specialty served</dt>
        <dd>{specialty.name}</dd>
        {fallback && (
          <>
            <dt>Requested specialty</dt>
            <dd>{requestedSpecialty.name}</dd>
          </>
        )}
        <dt>Distance</dt>
        <dd>{hospital.distanceKm.toFixed(1)} km</dd>
        <dt>Estimated travel time</dt>
        <dd>{Math.round(hospital.estimatedTravelTimeMinutes)} min</dd>
        <dt>Available beds</dt>
        <dd>{hospital.availableBeds}</dd>
        <dt>Bed reserved</dt>
        <dd>{bedReserved ? 'Yes' : 'No'}</dd>
      </dl>
    </article>
  )
}
