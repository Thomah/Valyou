package fr.lesprojetscagnottes.core.component;

import fr.lesprojetscagnottes.core.entity.Project;
import fr.lesprojetscagnottes.core.model.ProjectModel;
import org.hobsoft.spring.resttemplatelogger.LoggingCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static io.cucumber.spring.CucumberTestContext.SCOPE_CUCUMBER_GLUE;

@Component
@Scope(SCOPE_CUCUMBER_GLUE)
public class ProjectHttpClient {

    private final String SERVER_URL = "http://localhost";
    private final String ENDPOINT = "/api/project";

    @LocalServerPort
    private int port;

    @Autowired
    private CucumberContext context;

    private final RestTemplate restTemplate;
    private HttpHeaders headers;
    private ResponseEntity<?> response;

    public ProjectHttpClient() {
        restTemplate = new RestTemplateBuilder()
                .customizers(new LoggingCustomizer())
                .build();

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private String endpoint() {
        return SERVER_URL + ":" + port + ENDPOINT;
    }

    public void post(final Project project) {
        HttpEntity<ProjectModel> entity = new HttpEntity<>(ProjectModel.fromEntity(project), headers);
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(endpoint(), entity, Void.class);
            context.setLastHttpCode(response.getStatusCodeValue());
        } catch (HttpClientErrorException ex) {
            context.setLastHttpCode(ex.getStatusCode().value());
        }
    }

    public void setBearerAuth(String token) {
        headers.setBearerAuth(token);
    }

    public ResponseEntity<?> getLastResponse() {
        return response;
    }

}
