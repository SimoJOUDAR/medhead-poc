import { useId, type ChangeEvent } from 'react'

export interface LocationValue {
  latitude: number | null
  longitude: number | null
}

export interface LocationInputsProps {
  value: LocationValue
  onChange: (value: LocationValue) => void
  disabled?: boolean
}

export function LocationInputs({ value, onChange, disabled }: LocationInputsProps) {
  const latitudeId = useId()
  const longitudeId = useId()
  const latitudeHintId = useId()
  const longitudeHintId = useId()

  function update(field: keyof LocationValue) {
    return (event: ChangeEvent<HTMLInputElement>) => {
      const raw = event.target.value
      if (raw === '') {
        onChange({ ...value, [field]: null })
        return
      }
      const parsed = Number(raw)
      if (!Number.isFinite(parsed)) return
      onChange({ ...value, [field]: parsed })
    }
  }

  return (
    <div>
      <div>
        <label htmlFor={latitudeId}>Latitude</label>
        <input
          id={latitudeId}
          name="latitude"
          type="number"
          inputMode="decimal"
          step="0.0001"
          min={-90}
          max={90}
          value={value.latitude ?? ''}
          onChange={update('latitude')}
          aria-describedby={latitudeHintId}
          disabled={disabled}
          required
        />
        <p id={latitudeHintId}>Decimal degrees between -90 and 90.</p>
      </div>
      <div>
        <label htmlFor={longitudeId}>Longitude</label>
        <input
          id={longitudeId}
          name="longitude"
          type="number"
          inputMode="decimal"
          step="0.0001"
          min={-180}
          max={180}
          value={value.longitude ?? ''}
          onChange={update('longitude')}
          aria-describedby={longitudeHintId}
          disabled={disabled}
          required
        />
        <p id={longitudeHintId}>Decimal degrees between -180 and 180.</p>
      </div>
    </div>
  )
}
