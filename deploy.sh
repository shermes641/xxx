hash_name=LATEST_SHA
hash=$(git rev-parse HEAD)
branch_name=LATEST_BRANCH
branch=$(git rev-parse --abbrev-ref HEAD)
echo Setting $hash_name to $hash
echo Setting $branch_name to $branch
heroku config:set $hash_name=$hash
heroku config:set $branch_name=$branch
if [ -z "$1" ]; then
    echo Pushing master branch to Heroku
    git push heroku master
else
    echo Pushing branch: $1 to Heroku
    git push heroku $1:master
fi
