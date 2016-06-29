#!/usr/bin/env bash

wget 'https://www.dropbox.com/s/t4jbg0poyd9rcl9/staging.backup?dl=1' -O z.txt
psql $DATABASE_URL -c 'DROP SCHEMA PUBLIC CASCADE;CREATE SCHEMA PUBLIC;'
pg_restore -d $DATABASE_URL z.txt
ls