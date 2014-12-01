if [ -z "$1" ]; then
    echo Pushing master branch to Heroku
    result=$(git push heroku master)
else
    echo Pushing branch: $1 to Heroku
    result=$(git push heroku $1:master)
fi

error_condition=.*error\:\ failed\ to\ push*.
echo The result of the deploy is as follows: $result
if [[ $result =~ $error_condition ]]; then
    echo Deploy Failed.  SHA and Branch name were not updated.
else
    hash_name=LATEST_SHA
    hash=$(git rev-parse HEAD)
    branch_name=LATEST_BRANCH
    branch=$(git rev-parse --abbrev-ref HEAD)
    echo Setting $hash_name to $hash
    echo Setting $branch_name to $branch
    heroku config:set $hash_name=$hash
    heroku config:set $branch_name=$branch
fi
