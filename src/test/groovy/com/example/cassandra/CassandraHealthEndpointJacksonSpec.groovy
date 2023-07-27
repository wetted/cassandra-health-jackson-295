package com.example.cassandra

import io.micronaut.cassandra.health.CassandraHealthIndicator
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.testcontainers.containers.CassandraContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

@MicronautTest(rebuildContext = true)
class CassandraHealthEndpointJacksonSpec extends Specification {

    @Shared @AutoCleanup CassandraContainer cassandraContainer =
            (CassandraContainer) (new CassandraContainer(DockerImageName.parse("cassandra:latest")))
                    .withExposedPorts(9042)

    def setupSpec() {
        cassandraContainer.start()
    }

    void 'cassandra container is running'() {
        expect:
        cassandraContainer.isRunning()
    }

    void 'health call succeeds with jackson.serialization-inclusion set to the non-empty'() {
        when:
        EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer,
                MapPropertySource.of(
                        'test', [
                        'cassandra.default.basic.contact-points': ["localhost:$cassandraContainer.firstMappedPort"],
                        'cassandra.default.basic.load-balancing-policy.local-datacenter': 'datacenter1',
                        'jackson.serialization-inclusion': 'NON_EMPTY']
                ),
                "test")
        HttpClient client = embeddedServer.getApplicationContext().createBean(HttpClient, embeddedServer.getURL())

        def response = client.toBlocking().exchange("/health", CassandraHealthIndicator)

        then:
        HttpStatus.OK == response.status()
    }

    void 'health call fails with jackson.serialization-inclusion set to ALWAYS'() {

        when:
        EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer,
                MapPropertySource.of(
                        'test', [
                        'cassandra.default.basic.contact-points': ["localhost:$cassandraContainer.firstMappedPort"],
                        'cassandra.default.basic.load-balancing-policy.local-datacenter': 'datacenter1',
                        'jackson.serialization-inclusion': 'ALWAYS']
                ),
                "test")
        HttpClient client = embeddedServer.getApplicationContext().createBean(HttpClient, embeddedServer.getURL())

        def response = client.toBlocking().exchange("/health")

        then:
        HttpStatus.OK == response.status()
    }
}
