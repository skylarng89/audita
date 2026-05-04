package io.audita.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audita")
public class AuditaProperties {

	private Jwt jwt = new Jwt();

	public Jwt getJwt() {
		return jwt;
	}

	public void setJwt(Jwt jwt) {
		this.jwt = jwt;
	}

	public static class Jwt {
		private String secret;
		private Integer expirySeconds;
		private Integer streamExpirySeconds;

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		public Integer getExpirySeconds() {
			return expirySeconds;
		}

		public void setExpirySeconds(Integer expirySeconds) {
			this.expirySeconds = expirySeconds;
		}

		public Integer getStreamExpirySeconds() {
			return streamExpirySeconds;
		}

		public void setStreamExpirySeconds(Integer streamExpirySeconds) {
			this.streamExpirySeconds = streamExpirySeconds;
		}
	}
}
