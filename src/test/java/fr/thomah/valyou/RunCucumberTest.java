package fr.thomah.valyou;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(strict = true,
        features = "classpath:features",
        plugin = {"pretty", "json:target/cucumber/report.json"},
        extraGlue = "fr.thomah.valyou.component")
public class RunCucumberTest {
}