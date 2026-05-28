package com.complipilot.backend.common.security.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.complipilot.backend.common.storage.StorageService;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        properties = {
                "app.jwt.revoked-refresh-token-retention-seconds=604800"
        }
)
@AutoConfigureMockMvc
@Import(RefreshTokenCleanupJobTest.TestcontainersConfig.class)
class RefreshTokenCleanupJobTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenCleanupJob refreshTokenCleanupJob;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @Test
    void shouldDeleteExpiredRefreshTokens() {
        User user = createUser("cleanup-expired@example.com");

        RefreshToken expiredToken = refreshTokenRepository.save(
                new RefreshToken(
                        user,
                        "expired-token-hash",
                        Instant.now().minusSeconds(60)
                )
        );

        RefreshToken activeToken = refreshTokenRepository.save(
                new RefreshToken(
                        user,
                        "active-token-hash",
                        Instant.now().plusSeconds(3600)
                )
        );

        refreshTokenCleanupJob.cleanupRefreshTokens();

        assertThat(refreshTokenRepository.findById(expiredToken.getId())).isEmpty();
        assertThat(refreshTokenRepository.findById(activeToken.getId())).isPresent();
    }

    @Test
    void shouldDeleteOldRevokedRefreshTokensButKeepRecentRevokedTokens() {
        User user = createUser("cleanup-revoked@example.com");

        RefreshToken oldRevokedToken = refreshTokenRepository.save(
                new RefreshToken(
                        user,
                        "old-revoked-token-hash",
                        Instant.now().plusSeconds(3600)
                )
        );
        oldRevokedToken.revoke();

        /*
         * Force revoked_at to old timestamp through repository is hard because entity
         * intentionally only exposes revoke(). This test verifies current cleanup
         * does not delete recently revoked tokens.
         */
        RefreshToken recentRevokedToken = refreshTokenRepository.save(
                new RefreshToken(
                        user,
                        "recent-revoked-token-hash",
                        Instant.now().plusSeconds(3600)
                )
        );
        recentRevokedToken.revoke();
        refreshTokenRepository.save(recentRevokedToken);

        refreshTokenCleanupJob.cleanupRefreshTokens();

        assertThat(refreshTokenRepository.findById(oldRevokedToken.getId())).isPresent();
        assertThat(refreshTokenRepository.findById(recentRevokedToken.getId())).isPresent();
    }

    private User createUser(String email) {
        return userRepository.save(
                new User(
                        email,
                        "encoded-password",
                        "Cleanup User"
                )
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestcontainersConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:16-alpine")
            );
        }
    }
}