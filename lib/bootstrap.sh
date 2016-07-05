#!/usr/bin/env bash

if [ -z "$HEROKU_PARENT_APP_NAME" ];
then
    echo "This is not a review app, HEROKU_PARENT_APP_NAME is not set";
else
    if [[ "$HEROKU_APP_NAME" =~ "-pr-" ]];
    then
        # return zero if table is empty. otherwise 1 (actually "sign ------ 1 (1 row)")
        res=$(psql $DATABASE_URL -c "select sign(count(*)) from information_schema.tables where table_schema = 'public';")
        # everything is good, it's a review app and does not have users
        if [[ "$res" =~ "0" ]];
            then
                #There is no proprietary data in the staging.backup file, so here are the DropBox credentials
                # USER:  shermes@jungroup.com   PW: jungroup
                wget 'https://www.dropbox.com/s/t4jbg0poyd9rcl9/staging.backup?dl=1' -O z.txt
                psql $DATABASE_URL -c 'DROP SCHEMA PUBLIC CASCADE;CREATE SCHEMA PUBLIC;'
                pg_restore -d $DATABASE_URL z.txt
                echo "We have restored the $HEROKU_APP_NAME review app DB"
            else
                echo "Database is not empty, there are tables in the public schema: $DATABASE_URL";
            fi
    else
        echo "This is not a review app, HEROKU_APP_NAME is not the right format: $HEROKU_APP_NAME";
    fi
fi