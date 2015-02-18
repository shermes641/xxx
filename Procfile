web: target/universal/stage/bin/mediationapi -Dhttp.port=${PORT} -DapplyEvolutions.default=true -J-javaagent:newrelic/newrelic.jar -J-Dnewrelic.config.file=newrelic/newrelic.yml
worker: target/universal/stage/bin/mediationapi -Dhttp.port=${PORT} -Dapplication.global=RevenueDataWorkerGlobal -J-javaagent:newrelic/newrelic.jar -J-Dnewrelic.config.file=newrelic/newrelic.yml
appconfigregenerator: target/universal/stage/bin/mediationapi -Dhttp.port=${PORT} -Dapplication.global=RegenerateAppConfigsGlobal -J-javaagent:newrelic/newrelic.jar -J-Dnewrelic.config.file=newrelic/newrelic.yml
