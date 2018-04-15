from urllib.parse import urljoin
import json

from behave import given, then
import requests


@given('a user makes a request to get "{uri}"')
def step_impl(context, uri):
    base_url = context.config.userdata.get('app_base_url')
    url = urljoin(base_url, uri)

    context.response = requests.get(url)

@then('the response status code is {status_code:d}')
def step_impl(context, status_code):
    assert context.response.status_code == status_code


@then('the response contains valid JSON')
def step_impl(context):
    json.loads(context.response.text)
