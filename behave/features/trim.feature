          Feature: trimming leases

            Background: clean state
              Given the database is reset to an empty state

              Scenario: trim lease renewals
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |       60 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |      120 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:02:00 |       60 | {}   |
                And renewals are trimmed at "2000-01-01 00:02:00" "UTC"
                And the lease for "192.168.0.2" is released at "2000-01-01 00:00:01" "UTC"
                And the lease for "192.168.0.3" is released at "2000-01-01 00:00:01" "UTC"
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |        1 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |        1 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:02:00 |       60 | {}   |

              Scenario: trim lease renewals using a non-UTC time zone
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |       60 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |      120 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:02:00 |       60 | {}   |
                And renewals are trimmed at "2000-01-01 02:02:00" "Europe/Vilnius"
                And the lease for "192.168.0.2" is released at "2000-01-01 00:00:01" "UTC"
                And the lease for "192.168.0.3" is released at "2000-01-01 00:00:01" "UTC"
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |        1 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |        1 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:02:00 |       60 | {}   |

              Scenario: trim lease release records
                When the lease for "192.168.0.2" is released at "2000-01-01 00:00:01" "UTC"
                And the lease for "192.168.0.3" is released at "2000-01-01 00:01:00" "UTC"
                And release records are trimmed at "2000-01-01 00:01:00" "UTC"
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      120 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |      120 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      120 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |       60 | {}   |

              Scenario: trim lease release records using a non-UTC time zone
                When the lease for "192.168.0.2" is released at "2000-01-01 00:00:01" "UTC"
                And the lease for "192.168.0.3" is released at "2000-01-01 00:01:00" "UTC"
                And release records are trimmed at "2000-01-01 02:01:00" "Europe/Vilnius"
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      120 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |      120 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      120 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |       60 | {}   |

              Scenario: trim leases
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      120 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:01:00 |      120 | {}   |
                | 192.168.0.4 | cc:cc:cc:cc:cc:cc | 2000-01-01 00:02:00 |      120 | {}   |
                | 192.168.0.5 | ee:ee:ee:ee:ee:ee | 2000-01-01 00:03:00 |      120 | {}   |
                And leases are trimmed at "2000-01-01 00:02:01" "UTC"
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:01:00 |      120 | {}   |
                | 192.168.0.4 | cc:cc:cc:cc:cc:cc | 2000-01-01 00:02:00 |      120 | {}   |
                | 192.168.0.5 | ee:ee:ee:ee:ee:ee | 2000-01-01 00:03:00 |      120 | {}   |

              Scenario: trim leases using a non-UTC time zone
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      120 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:01:00 |      120 | {}   |
                | 192.168.0.4 | cc:cc:cc:cc:cc:cc | 2000-01-01 00:02:00 |      120 | {}   |
                | 192.168.0.5 | ee:ee:ee:ee:ee:ee | 2000-01-01 00:03:00 |      120 | {}   |
                And leases are trimmed at "2000-01-01 02:02:01" "Europe/Vilnius"
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:01:00 |      120 | {}   |
                | 192.168.0.4 | cc:cc:cc:cc:cc:cc | 2000-01-01 00:02:00 |      120 | {}   |
                | 192.168.0.5 | ee:ee:ee:ee:ee:ee | 2000-01-01 00:03:00 |      120 | {}   |


