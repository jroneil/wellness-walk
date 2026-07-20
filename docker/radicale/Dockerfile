FROM python:3.13-alpine

RUN pip install --no-cache-dir radicale==3.7.3 && apk add --no-cache curl
COPY config /etc/radicale/config
COPY users /etc/radicale/users
RUN mkdir -p /var/lib/radicale/collections
EXPOSE 5232
CMD ["radicale", "--config", "/etc/radicale/config"]
