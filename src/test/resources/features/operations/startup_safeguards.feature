Feature: The service refuses to start on a setting that would break it silently
  A missing secret or a throttle setting that makes no sense does not surface as an error while the
  service runs; it surfaces as a collection that never signs in, or as a throttle that admits nobody
  or never turns a client away. The service therefore checks these settings when it starts and
  refuses to start, naming the setting, instead of running without them. The service's time zone is
  not one of them: the service starts in any time zone.

  Rule: The secrets the service needs must be set

    Scenario Outline: With <what> <state> the service does not start
      Given <setting> is <state>
      When the operator starts the service
      Then the service does not start
      And the service's log says "Required configuration property '<property>' is not set or blank. Set the environment variable <setting>."

      Examples:
        | what                        | setting                       | state | property                      |
        | the game account's name     | EVERGORE_CREDENTIALS_USERNAME | blank | evergore.credentials.username |
        | the game account's password | EVERGORE_CREDENTIALS_PASSWORD | blank | evergore.credentials.password |
        | the game account's name     | EVERGORE_CREDENTIALS_USERNAME | unset | evergore.credentials.username |
        | the game account's password | EVERGORE_CREDENTIALS_PASSWORD | unset | evergore.credentials.password |

  Rule: The service starts without the guild's old token

    Scenario: The old token is no setting the service needs any more
      Given EVERGORE_SECURITY_API_TOKEN is unset
      When the operator starts the service
      Then the service starts

  Rule: The service starts in any time zone
    No time zone is refused: one with summer time, one without it and UTC alike.

    @time-zone
    Scenario Outline: The service starts in the time zone "<zone>"
      Given the service's time zone is "<zone>"
      When the operator starts the service
      Then the service starts

      Examples:
        | zone          |
        | Europe/Berlin |
        | Asia/Tokyo    |
        | UTC           |

  Rule: The request throttle cannot be configured away

    Scenario Outline: A throttle setting of <value> for <setting> is refused
      Given the throttle setting "<setting>" is <value>
      When the operator starts the service
      Then the service does not start
      And the service's log says "Configuration property 'evergore.rate-limit.<setting>' <requirement>."

      Examples:
        | setting             | value | requirement                                                                                |
        | interval            | 0s    | must be a positive duration, otherwise every request would start a fresh count             |
        | block-duration      | 0s    | must be a positive duration, otherwise a blocked client would be admitted again right away |
        | max-tracked-clients | 0     | must be at least 1, otherwise no client could be counted                                   |

      @wip
      Examples: The settings the login brings or rewords
        | setting                   | value | requirement                                                                                |
        | max-requests-per-interval | 0     | must be at least 1, otherwise no request with a login would ever be admitted               |
        | anonymous-burst           | 0     | must be at least 1, otherwise no request without a login would ever be admitted            |
        | anonymous-refill-interval | 0s    | must be a positive duration, otherwise a client without a login would never be turned away |

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

      @wip
      Examples: The settings the login brings
        | setting                   | value |
        | anonymous-burst           | 1     |
        | anonymous-refill-interval | 1s    |
