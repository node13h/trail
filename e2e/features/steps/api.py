from urllib.parse import urljoin
import json
from subprocess import run
from urllib.parse import urlencode
from pprint import pformat

from behave import when, then, given
import requests


def sorted_leases(leases):
    return list(sorted(leases, key=lambda i: (i['ip'], i['start-date'])))


def format_unexpected(expected, got):
    return 'Expected:\n{}\nGot:\n{}\n'.format(
        pformat(expected), pformat(got))


@when('a user makes a request to get "{uri}"')
def step_impl(context, uri):  # noqa: F811
    base_url = context.config.userdata.get('app_base_url')
    url = urljoin(base_url, uri)

    context.response = requests.get(url)


@then('the response status code is {status_code:d}')
def step_impl(context, status_code):  # noqa: F811
    assert context.response.status_code == status_code


@then('the response contains valid JSON')
def step_impl(context):  # noqa: F811
    json.loads(context.response.text)


@given('the database is reset to an empty state')
def step_impl(context):  # noqa: F811
    reset_script = context.config.userdata.get('reset_script')
    reset_sql = context.config.userdata.get('reset_sql')
    pg_address = context.config.userdata.get('pg_address')
    pg_port = context.config.userdata.get('pg_port')
    pg_password = context.config.userdata.get('pg_password')

    run([reset_script, reset_sql, pg_address, pg_port, pg_password], check=True)


@when('no leases are added')
def setp_impl(context):  # noqa: F811
    pass


@when('a query for all leases between "{from_date}" and "{to_date}" "{tz}" is executed')
def step_impl(context, from_date, to_date, tz):  # noqa: F811
    base_url = context.config.userdata.get('app_base_url')

    url = urljoin(base_url, 'api/v3/leases?{}'.format(
        urlencode({'from-date': from_date, 'to-date': to_date})))
    headers = {'tz': tz}

    context.response = requests.get(url, headers=headers)


@when('a query for the "{ip}" IP between "{from_date}" and "{to_date}" "{tz}" is executed')
def step_impl(context, ip, from_date, to_date, tz):  # noqa: F811
    base_url = context.config.userdata.get('app_base_url')

    url = urljoin(base_url, 'api/v3/leases?{}'.format(
        urlencode({'ip': ip, 'from-date': from_date, 'to-date': to_date})))
    headers = {'tz': tz}

    context.response = requests.get(url, headers=headers)


@when('a query for the "{mac}" MAC between "{from_date}" and "{to_date}" "{tz}" is executed')
def step_impl(context, mac, from_date, to_date, tz):  # noqa: F811
    base_url = context.config.userdata.get('app_base_url')

    url = urljoin(base_url, 'api/v3/leases?{}'.format(
        urlencode({'mac': mac, 'from-date': from_date, 'to-date': to_date})))
    headers = {'tz': tz}

    context.response = requests.get(url, headers=headers)


@then('the response is an empty JSON array')
def step_impl(context):  # noqa: F811
    assert context.response.json()['result'] == []


@when('the following leases are added using the "{tz}" time zone')
def step_impl(context, tz):  # noqa: F811
    base_url = context.config.userdata.get('app_base_url')

    leases = []
    for row in context.table:
        lease = {
            'ip': row['ip'],
            'mac': row['mac'],
            'start-date': row['start-date'],
            'duration': int(row['duration']),
            'data': json.loads(row['data']),
        }
        leases.append(lease)

    url = urljoin(base_url, 'api/v3/leases')
    headers = {'tz': tz}

    response = requests.post(url, json=leases, headers=headers)
    response.raise_for_status()

    context.response = response


@then('the leases in the response are as follows')
def step_impl(context):  # noqa: F811
    expected_leases = []
    for row in context.table:
        lease = {
            'ip': row['ip'],
            'mac': row['mac'],
            'start-date': row['start-date'],
            'duration': int(row['duration']),
            'data': json.loads(row['data']),
        }
        expected_leases.append(lease)

    expected_leases = sorted_leases(expected_leases)
    returned_leases = sorted_leases(context.response.json()['result'])

    assert len(expected_leases) == len(returned_leases), format_unexpected(
        expected_leases, returned_leases)

    for index, expected_lease in enumerate(expected_leases):
        lease = returned_leases[index]

        lease.pop('id')

        assert lease == expected_lease, format_unexpected(expected_lease, lease)


@when('the lease for "{ip}" is released at "{end_date}" "{tz}"')
def step_impl(context, ip, end_date, tz):  # noqa: F811
    base_url = context.config.userdata.get('app_base_url')

    url = urljoin(base_url, 'api/v3/leases/released')
    headers = {'tz': tz}

    data = {
        'ip': ip,
        'end-date': end_date,
    }

    response = requests.post(url, json=data, headers=headers)
    response.raise_for_status()

    context.response = response


@when('renewals are trimmed at "{to_date}" "{tz}"')
def step_impl(context, to_date, tz):  # noqa: F811
    base_url = context.config.userdata.get('app_base_url')

    url = urljoin(base_url, 'api/v3/leases/renewals')
    headers = {'tz': tz}

    data = {
        'to-date': to_date,
    }

    response = requests.delete(url, json=data, headers=headers)
    response.raise_for_status()

    context.response = response


@when('release records are trimmed at "{to_date}" "{tz}"')
def step_impl(context, to_date, tz):  # noqa: F811
    base_url = context.config.userdata.get('app_base_url')

    url = urljoin(base_url, 'api/v3/releases')
    headers = {'tz': tz}

    data = {
        'to-date': to_date,
    }

    response = requests.delete(url, json=data, headers=headers)
    response.raise_for_status()

    context.response = response


@when('leases are trimmed at "{to_date}" "{tz}"')
def step_impl(context, to_date, tz):  # noqa: F811
    base_url = context.config.userdata.get('app_base_url')

    url = urljoin(base_url, 'api/v3/leases')
    headers = {'tz': tz}

    data = {
        'to-date': to_date,
    }

    response = requests.delete(url, json=data, headers=headers)
    response.raise_for_status()

    context.response = response
