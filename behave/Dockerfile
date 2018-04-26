FROM python:3
COPY requirements.txt /
RUN pip install -r /requirements.txt; apt-get update; apt-get install -y make && rm -rf /var/lib/apt/lists/*
