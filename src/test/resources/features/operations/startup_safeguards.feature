@wip
Feature: The service refuses to start on a setting that would break it silently
  A missing secret or a throttle setting that makes no sense does not surface as an error while the
  service runs; it surfaces as a dashboard nobody can open, a collection that never signs in, a
  throttle that never turns a client away, or timestamps an hour off. The service therefore checks
  these settings when it starts and refuses to start, naming the setting, instead of running
  without them.

  Rule: The secrets the service needs must be set

    Scenario Outline: With <what> <state> the service does not start
      Given <setting> is <state>
      When the operator starts the service
      Then the service does not start
      And the service's log says "Required configuration property '<property>' is not set or blank. Set the environment variable <setting>."

      Examples:
        | what                        | setting                       | state | property                      |
        | the dashboard's link token  | EVERGORE_SECURITY_API_TOKEN   | unset | evergore.security.api-token   |
        | the dashboard's link token  | EVERGORE_SECURITY_API_TOKEN   | blank | evergore.security.api-token   |
        | the game account's name     | EVERGORE_CREDENTIALS_USERNAME | unset | evergore.credentials.username |
        | the game account's name     | EVERGORE_CREDENTIALS_USERNAME | blank | evergore.credentials.username |
        | the game account's password | EVERGORE_CREDENTIALS_PASSWORD | unset | evergore.credentials.password |
        | the game account's password | EVERGORE_CREDENTIALS_PASSWORD | blank | evergore.credentials.password |

  Rule: The service runs only in a time zone with a fixed offset from UTC
    The ledgers store times as wall-clock text, which cannot tell the two passes of an hour apart
    when the clocks are set back.

    Scenario Outline: A time zone whose offset has ever changed is refused: "<zone>"
      Given the service's time zone is "<zone>"
      When the operator starts the service
      Then the service does not start
      And the service's log says "Effective timezone '<zone>' observes daylight saving and is not supported. Set a fixed-offset zone (e.g. UTC)."

      Examples:
        | zone          |
        | Europe/Berlin |
        | Asia/Tokyo    |

    Scenario Outline: A fixed time zone is accepted: "<zone>"
      Given the service's time zone is "<zone>"
      When the operator starts the service
      Then the service starts

      Examples:
        | zone      |
        | UTC       |
        | Etc/GMT-2 |

  Rule: The request throttle cannot be configured away

    Scenario Outline: A throttle setting of <value> for <setting> is refused
      Given the throttle setting "<setting>" is <value>
      When the operator starts the service
      Then the service does not start
      And the service's log says "Configuration property 'evergore.rate-limit.<setting>' <requirement>."

      Examples:
        | setting                   | value | requirement                                                                                |
        | max-requests-per-interval | 0     | must be at least 1, otherwise no request would ever be admitted                            |
        | interval                  | 0s    | must be a positive duration, otherwise every request would start a fresh count             |
        | block-duration            | 0s    | must be a positive duration, otherwise a blocked client would be admitted again right away |
        | max-tracked-clients       | 0     | must be at least 1, otherwise no client could be counted                                   |

    Scenario Outline: The smallest throttle setting of <value> for <setting> is accepted
      Given the throttle setting "<setting>" is <value>
      When the operator starts the service
      Then the service starts

      Examples:
        | setting                   | value |
        | max-requests-per-interval | 1     |
        | interval                  | 1s    |
        | block-duration            | 1s    |
        | max-tracked-clients       | 1     |
