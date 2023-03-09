package ldes.client.requestexecutor.executor.noauth;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:features/default-request-executor.feature" }, glue = {
		"ldes.client.requestexecutor" })
class DefaultRequestExecutorIT {
}
