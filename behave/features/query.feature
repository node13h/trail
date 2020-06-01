          Feature: querying leases

            Background: clean state
              Given the database is reset to an empty state

              Scenario: query returns no results when there are no records
                When no leases are added
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the response is an empty JSON array
                And the response status code is 200

              Scenario: query existing leases
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 01:00:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 01:00:00 |      100 | {}   |

              Scenario: query existing leases using custom timezone
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 01:00:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 02:00:00" and "2000-01-01 03:00:00" "Europe/Vilnius" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 02:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 03:00:00 |      100 | {}   |

              Scenario: query specific IP
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.3 | bB:bb:bb:bb:bb:bb | 2000-01-01 01:00:00 |      100 | {}   |
                And a query for the "192.168.0.2" IP between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |

              Scenario: query non-existing IP
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.3 | bB:bb:bb:bb:bb:bb | 2000-01-01 01:00:00 |      100 | {}   |
                And a query for the "192.168.0.4" IP between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the response is an empty JSON array

              Scenario: query specific MAC
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.3 | bB:bb:bb:bb:bb:bb | 2000-01-01 01:00:00 |      100 | {}   |
                And a query for the "aa:aa:aa:aa:aa:aa" MAC between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |

              Scenario: query non-existing MAC
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.3 | bB:bb:bb:bb:bb:bb | 2000-01-01 01:00:00 |      100 | {}   |
                And a query for the "cc:cc:cc:cc:cc:cc" MAC between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the response is an empty JSON array
