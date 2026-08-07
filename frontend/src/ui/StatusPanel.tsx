export type StatusVariant = 'loading' | 'empty' | 'error'

export type StatusPanelProps = {
  readonly variant: StatusVariant
  readonly message: string
}

const roleOf = (variant: StatusVariant): 'status' | 'alert' => {
  switch (variant) {
    case 'loading':
    case 'empty':
      return 'status'
    case 'error':
      return 'alert'
  }
}

export function StatusPanel({ variant, message }: StatusPanelProps) {
  return (
    <p
      className="status-panel"
      data-variant={variant}
      data-testid="status-panel"
      role={roleOf(variant)}
    >
      {message}
    </p>
  )
}
