          Feature: adding leases

            Background: clean state
              Given the database is reset to an empty state

              Scenario: add leases
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                Then the response status code is 200

              Scenario: query back added leases
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 01:00:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 01:00:00 |      100 | {}   |

              Scenario: add more leases
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 01:00:00 |      100 | {}   |
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 01:00:00 |      100 | {}   |
                | 192.168.0.3 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |      100 | {}   |

              Scenario: add leases using a non-UTC time zone
                When the following leases are added using the "Europe/Vilnius" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 02:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 03:00:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 01:00:00 |      100 | {}   |

              Scenario: adjacent lease added at the end of an existing one is merged into it
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      160 | {}   |


              Scenario: adjacent lease added at the front of an existing one is merged into it
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |      100 | {}   |
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      160 | {}   |

              Scenario: adjacent leases in a same request are merged together
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      160 | {}   |

              Scenario: overlapping leases are merged
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:30 |      100 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      130 | {}   |

              Scenario: adjacent leases for different IP addresses are not merged
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |      100 | {}   |
                | 192.168.0.3 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |      100 | {}   |
                | 192.168.0.3 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |

              Scenario: longer lease swallows shorter lease
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |

              Scenario: longer lease in the middle of two existing ones merges with the first and swallows the second
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:02:00 |       60 | {}   |
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |      180 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      240 | {}   |


              Scenario: adding same lease with a longer duration updates the duration of the original lease
                When the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                And the following leases are added using the "UTC" time zone
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |

              Scenario: add IPv4 and IPv6 leases
                When the following leases are added using the "UTC" time zone
                | ip                                      | mac               | start-date          | duration | data |
                | 192.168.0.2                             | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 2001:0db8:85a3:0000:0000:8a2e:0370:7334 | bb:bb:bb:bb:bb:bb | 2000-01-01 01:00:00 |      100 | {}   |
                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                | ip                              | mac               | start-date          | duration | data |
                | 192.168.0.2                     | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      100 | {}   |
                | 2001:db8:85a3:0:0:8a2e:370:7334 | bb:bb:bb:bb:bb:bb | 2000-01-01 01:00:00 |      100 | {}   |

              Scenario: different IPv4 address formats are recognized
                When the following leases are added using the "UTC" time zone
                |            ip | mac               | start-date          | duration | data |
                | 192.168.0.002 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |       60 | {}   |
                | 192.168.000.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:01:00 |       60 | {}   |
                |   192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:02:00 |       60 | {}   |

                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                |          ip | mac               | start-date          | duration | data |
                | 192.168.0.2 | aa:aa:aa:aa:aa:aa | 2000-01-01 00:00:00 |      180 | {}   |

              Scenario: different IPv6 address formats are recognized
                When the following leases are added using the "UTC" time zone
                | ip                                      | mac               | start-date          | duration | data |
                | 2001:0db8:85a3:0000:0000:8a2e:0370:7334 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |       60 | {}   |
                | 2001:0db8:85a3::8a2e:0370:7334          | bb:bb:bb:bb:bb:bb | 2000-01-01 00:01:00 |       60 | {}   |
                | 2001:db8:85a3::8a2e:370:7334            | bb:bb:bb:bb:bb:bb | 2000-01-01 00:02:00 |       60 | {}   |

                And a query for all leases between "2000-01-01 00:00:00" and "2000-01-01 01:00:00" "UTC" is executed
                Then the leases in the response are as follows
                | ip                              | mac               | start-date          | duration | data |
                | 2001:db8:85a3:0:0:8a2e:370:7334 | bb:bb:bb:bb:bb:bb | 2000-01-01 00:00:00 |      180 | {}   |
