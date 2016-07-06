web: target/universal/stage/bin/mediationapi -Dhttp.port=${PORT} -Dplay.evolutions.autoApply=true -J-javaagent:newrelic/newrelic.jar -J-Dnewrelic.config.file=newrelic/newrelic.yml
worker: target/universal/stage/bin/mediationapi -Dhttp.port=${PORT} -Dplay.application.loader=ReportingApplicationLoader -J-javaagent:newrelic/newrelic.jar -J-Dnewrelic.config.file=newrelic/newrelic.yml
appconfigregenerator: target/universal/stage/bin/mediationapi -Dhttp.port=${PORT} -Dplay.application.loader=RegenerateAppConfigsApplicationLoader -J-javaagent:newrelic/newrelic.jar -J-Dnewrelic.config.file=newrelic/newrelic.yml
console: target/universal/stage/bin/mediationapi -main scala.tools.nsc.MainGenericRunner -usejavacp
reviewappseed: target/universal/stage/bin/mediationapi -Dhttp.port=${PORT} -Dplay.application.loader=ReviewAppSeedLoader
