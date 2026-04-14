import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { RecommendationResult } from './RecommendationResult'
import type { RecommendationResponse } from './useRecommendation'

const BASE: RecommendationResponse = {
  hospital: {
    id: 1,
    name: 'Fred Brooks Hospital',
    latitude: 51.5237,
    longitude: -0.1311,
    address: '12 Bloomsbury Way, London',
    availableBeds: 1,
    distanceKm: 1.23,
    estimatedTravelTimeMinutes: 4.7,
  },
  specialty: { id: 7, name: 'Cardiology', group: 'Medicine' },
  requestedSpecialty: { id: 7, name: 'Cardiology', group: 'Medicine' },
  bedReserved: true,
  fallback: false,
  timestamp: '2026-04-24T10:00:00Z',
}

describe('RecommendationResult', () => {
  it('renders the recommended hospital with its route, beds, and reservation flag', () => {
    render(<RecommendationResult result={BASE} />)

    expect(screen.getByRole('heading', { level: 2, name: /fred brooks hospital/i })).toBeInTheDocument()

    const list = screen.getByRole('article').querySelector('dl')!
    const within_ = within(list as HTMLElement)
    expect(within_.getByText('Address').nextElementSibling).toHaveTextContent('12 Bloomsbury Way, London')
    expect(within_.getByText('Specialty served').nextElementSibling).toHaveTextContent('Cardiology')
    expect(within_.getByText('Distance').nextElementSibling).toHaveTextContent('1.2 km')
    expect(within_.getByText('Estimated travel time').nextElementSibling).toHaveTextContent('5 min')
    expect(within_.getByText('Available beds').nextElementSibling).toHaveTextContent('1')
    expect(within_.getByText('Bed reserved').nextElementSibling).toHaveTextContent('Yes')

    expect(screen.queryByTestId('fallback-badge')).not.toBeInTheDocument()
    expect(screen.queryByText(/requested specialty/i)).not.toBeInTheDocument()
  })

  it('surfaces the fallback badge and the requested specialty block when fallback=true', () => {
    const fallbackResult: RecommendationResponse = {
      ...BASE,
      hospital: { ...BASE.hospital, name: 'Beverly Bashir Hospital', availableBeds: 5 },
      specialty: { id: 99, name: 'Neuropathology', group: 'Medicine' },
      requestedSpecialty: { id: 7, name: 'Cardiology', group: 'Medicine' },
      fallback: true,
      bedReserved: true,
    }

    render(<RecommendationResult result={fallbackResult} />)

    expect(screen.getByTestId('fallback-badge')).toHaveTextContent(/fallback match/i)
    expect(screen.getByText('Requested specialty').nextElementSibling).toHaveTextContent('Cardiology')
    expect(screen.getByText('Specialty served').nextElementSibling).toHaveTextContent('Neuropathology')
  })

  it('shows bed reserved as No when reservation did not succeed', () => {
    render(<RecommendationResult result={{ ...BASE, bedReserved: false }} />)

    expect(screen.getByText('Bed reserved').nextElementSibling).toHaveTextContent('No')
  })
})
