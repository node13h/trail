          Feature: releasing leases

            Background: clean state
              Given the database is reset to an empty state

              Scenario: releasing a last lease in continuous series will result in shorter aggregated lease
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:02:40 |       20 | {}   |
                And the lease for "192.168.0.2" is released at "2000-01-01 00:02:41" "UTC"
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      161 | {}   |

              Scenario: releasing an aggregated series in the middle will break it starting on original renewal boundary
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:02:40 |       20 | {}   |
                And the lease for "192.168.0.2" is released at "2000-01-01 00:02:00" "UTC"
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      120 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:02:40 |       20 | {}   |

              Scenario: releasing in advance
                When the lease for "192.168.0.2" is released at "2000-01-01 00:01:00" "UTC"
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |

              Scenario: releasing using custom tz
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                And the lease for "192.168.0.2" is released at "2000-01-01 02:01:00" "Europe/Vilnius"
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |

              Scenario: earliest release date is applied when adding an already released lease
                When the lease for "192.168.0.2" is released at "2000-01-01 00:02:00" "UTC"
                And the lease for "192.168.0.2" is released at "2000-01-01 00:01:00" "UTC"
                And the lease for "192.168.0.2" is released at "2000-01-01 00:01:30" "UTC"
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      180 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
