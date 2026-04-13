import { useId, type ChangeEvent } from 'react'
import type { ApiError } from '../auth/ApiError'
import type { SpecialtyGroupOption, SpecialtyOption } from './useSpecialties'

export interface SpecialtyDropdownProps {
  specialties: SpecialtyOption[]
  loading: boolean
  error: ApiError | null
  value: number | null
  onChange: (specialtyId: number | null) => void
  disabled?: boolean
}

interface GroupedSpecialties {
  group: SpecialtyGroupOption
  options: SpecialtyOption[]
}

export function SpecialtyDropdown({
  specialties,
  loading,
  error,
  value,
  onChange,
  disabled,
}: SpecialtyDropdownProps) {
  const selectId = useId()
  const errorId = useId()

  function handleChange(event: ChangeEvent<HTMLSelectElement>) {
    const raw = event.target.value
    onChange(raw === '' ? null : Number(raw))
  }

  const grouped = groupBySpecialtyGroup(specialties)
  const selectValue = value === null ? '' : String(value)

  return (
    <div>
      <label htmlFor={selectId}>Specialty</label>
      <select
        id={selectId}
        value={selectValue}
        onChange={handleChange}
        disabled={disabled || loading || Boolean(error)}
        aria-busy={loading ? 'true' : undefined}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? errorId : undefined}
      >
        <option value="">Select a specialty</option>
        {grouped.map(({ group, options }) => (
          <optgroup key={group.id} label={group.name}>
            {options.map((specialty) => (
              <option key={specialty.id} value={specialty.id}>
                {specialty.name}
              </option>
            ))}
          </optgroup>
        ))}
      </select>
      {error && (
        <p id={errorId} role="alert">
          Could not load specialties. Please try again.
        </p>
      )}
    </div>
  )
}

function groupBySpecialtyGroup(specialties: SpecialtyOption[]): GroupedSpecialties[] {
  const map = new Map<number, GroupedSpecialties>()
  for (const specialty of specialties) {
    let entry = map.get(specialty.group.id)
    if (!entry) {
      entry = { group: specialty.group, options: [] }
      map.set(specialty.group.id, entry)
    }
    entry.options.push(specialty)
  }
  return Array.from(map.values())
}
