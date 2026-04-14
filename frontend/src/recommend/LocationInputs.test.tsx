import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { LocationInputs, type LocationValue } from './LocationInputs'

const EMPTY: LocationValue = { latitude: null, longitude: null }

function StatefulHarness({
  initial = EMPTY,
  onChange,
}: {
  initial?: LocationValue
  onChange: (value: LocationValue) => void
}) {
  const [value, setValue] = useState<LocationValue>(initial)
  return (
    <LocationInputs
      value={value}
      onChange={(next) => {
        setValue(next)
        onChange(next)
      }}
    />
  )
}

describe('LocationInputs', () => {
  it('binds labels to numeric inputs with the documented ranges', () => {
    render(<LocationInputs value={EMPTY} onChange={() => {}} />)

    const latitude = screen.getByLabelText(/latitude/i) as HTMLInputElement
    const longitude = screen.getByLabelText(/longitude/i) as HTMLInputElement

    expect(latitude).toHaveAttribute('type', 'number')
    expect(latitude).toHaveAttribute('min', '-90')
    expect(latitude).toHaveAttribute('max', '90')
    expect(latitude).toHaveAttribute('step', '0.0001')

    expect(longitude).toHaveAttribute('type', 'number')
    expect(longitude).toHaveAttribute('min', '-180')
    expect(longitude).toHaveAttribute('max', '180')
    expect(longitude).toHaveAttribute('step', '0.0001')
  })

  it('emits the parsed latitude via onChange when the user types', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<StatefulHarness onChange={onChange} />)

    await user.type(screen.getByLabelText(/latitude/i), '51.523')

    const last = onChange.mock.calls.at(-1)?.[0] as LocationValue
    expect(last.latitude).toBe(51.523)
    expect(last.longitude).toBeNull()
  })

  it('emits null when the user clears a field', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(
      <StatefulHarness initial={{ latitude: 10, longitude: 20 }} onChange={onChange} />,
    )

    await user.clear(screen.getByLabelText(/latitude/i))

    expect(onChange).toHaveBeenCalledWith({ latitude: null, longitude: 20 })
  })

  it('disables both inputs while disabled=true', () => {
    render(<LocationInputs value={EMPTY} onChange={() => {}} disabled />)

    expect(screen.getByLabelText(/latitude/i)).toBeDisabled()
    expect(screen.getByLabelText(/longitude/i)).toBeDisabled()
  })
})
