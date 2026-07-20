#!/bin/sh
set -eu

base="http://radicale:5232/wellness"
auth="wellness:wellness-dev-only"

create_calendar() {
  path="$1"
  name="$2"
  curl --silent --show-error --user "$auth" -X MKCALENDAR \
    -H 'Content-Type: application/xml; charset=utf-8' \
    --data "<?xml version=\"1.0\"?><c:mkcalendar xmlns:c=\"urn:ietf:params:xml:ns:caldav\" xmlns:d=\"DAV:\"><d:set><d:prop><d:displayname>${name}</d:displayname><c:supported-calendar-component-set><c:comp name=\"VEVENT\"/></c:supported-calendar-component-set></d:prop></d:set></c:mkcalendar>" \
    "$base/$path/" >/dev/null || true
}

create_calendar work "Work"
create_calendar personal "Personal"

curl --fail --silent --show-error --user "$auth" -X PUT -H 'Content-Type: text/calendar; charset=utf-8' \
  --data-binary @/fixtures/work-sample.ics "$base/work/work-sample.ics"
curl --fail --silent --show-error --user "$auth" -X PUT -H 'Content-Type: text/calendar; charset=utf-8' \
  --data-binary @/fixtures/personal-sample.ics "$base/personal/personal-sample.ics"
curl --fail --silent --show-error --user "$auth" -X PUT -H 'Content-Type: text/calendar; charset=utf-8' \
  --data-binary @/fixtures/cancelled.ics "$base/work/cancelled.ics"
curl --fail --silent --show-error --user "$auth" -X PUT -H 'Content-Type: text/calendar; charset=utf-8' \
  --data-binary @/fixtures/transparent.ics "$base/personal/transparent.ics"
