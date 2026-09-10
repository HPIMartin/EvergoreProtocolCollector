export interface SwitchOption<Value extends string> {
  readonly value: Value
  readonly label: string
}

export interface OptionSwitchProps<Value extends string> {
  readonly name: string
  readonly legend: string
  readonly options: readonly SwitchOption<Value>[]
  readonly selected: Value
  readonly onSelect: (value: Value) => void
}

export function OptionSwitch<Value extends string>({
  name,
  legend,
  options,
  selected,
  onSelect,
}: OptionSwitchProps<Value>) {
  return (
    <fieldset className="option-switch" data-testid={`switch-${name}`}>
      <legend className="option-switch__legend">{legend}</legend>
      <div className="option-switch__options">
        {options.map((option) => (
          <label
            className="option-switch__option"
            data-selected={option.value === selected ? 'true' : undefined}
            key={option.value}
          >
            <input
              checked={option.value === selected}
              name={name}
              onChange={() => {
                onSelect(option.value)
              }}
              type="radio"
              value={option.value}
            />
            {option.label}
          </label>
        ))}
      </div>
    </fieldset>
  )
}
