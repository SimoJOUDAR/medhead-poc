import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../auth/ApiError'
import { SpecialtyDropdown } from './SpecialtyDropdown'
import type { SpecialtyOption } from './useSpecialties'

const CARDIOLOGY: SpecialtyOption = {
  id: 1,
  name: 'Cardiology',
  group: { id: 10, name: 'Medicine' },
}
const ONCOLOGY: SpecialtyOption = {
  id: 2,
  name: 'Oncology',
  group: { id: 10, name: 'Medicine' },
}
const ORTHOPAEDICS: SpecialtyOption = {
  id: 3,
  name: 'Orthopaedics',
  group: { id: 20, name: 'Surgery' },
}

describe('SpecialtyDropdown', () => {
  it('groups specialties under their specialty-group name via <optgroup>', () => {
    render(
      <SpecialtyDropdown
        specialties={[CARDIOLOGY, ONCOLOGY, ORTHOPAEDICS]}
        loading={false}
        error={null}
        value={null}
        onChange={() => {}}
      />,
    )

    const groups = screen.getAllByRole('group')
    expect(groups).toHaveLength(2)
    expect(groups[0]).toHaveAttribute('label', 'Medicine')
    expect(groups[1]).toHaveAttribute('label', 'Surgery')
  })

  it('renders a placeholder first option selected by default', () => {
    render(
      <SpecialtyDropdown
        specialties={[CARDIOLOGY]}
        loading={false}
        error={null}
        value={null}
        onChange={() => {}}
      />,
    )

    const select = screen.getByRole('combobox', { name: /specialty/i }) as HTMLSelectElement
    expect(select.value).toBe('')
    expect(screen.getByRole('option', { name: /select a specialty/i })).toBeInTheDocument()
  })

  it('emits the chosen specialty id via onChange when the user picks an option', async () => {
    const user = userEvent.setup()
    const handleChange = vi.fn()

    render(
      <SpecialtyDropdown
        specialties={[CARDIOLOGY, ONCOLOGY]}
        loading={false}
        error={null}
        value={null}
        onChange={handleChange}
      />,
    )

    await user.selectOptions(
      screen.getByRole('combobox', { name: /specialty/i }),
      'Cardiology',
    )

    expect(handleChange).toHaveBeenCalledWith(CARDIOLOGY.id)
  })

  it('marks the select aria-busy while the catalogue is loading', () => {
    render(
      <SpecialtyDropdown
        specialties={[]}
        loading={true}
        error={null}
        value={null}
        onChange={() => {}}
      />,
    )

    expect(screen.getByRole('combobox', { name: /specialty/i })).toHaveAttribute(
      'aria-busy',
      'true',
    )
  })

  it('exposes a retry-friendly message via role="alert" when the fetch fails', () => {
    render(
      <SpecialtyDropdown
        specialties={[]}
        loading={false}
        error={new ApiError(0, 'NETWORK_ERROR', 'Could not reach the backend.')}
        value={null}
        onChange={() => {}}
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent(/could not load specialties/i)
  })
})
