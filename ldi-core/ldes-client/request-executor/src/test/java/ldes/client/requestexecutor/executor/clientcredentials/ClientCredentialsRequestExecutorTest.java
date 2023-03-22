package ldes.client.requestexecutor.executor.clientcredentials;

import ldes.client.requestexecutor.domain.valueobjects.Request;
import ldes.client.requestexecutor.domain.valueobjects.RequestHeaders;
import ldes.client.requestexecutor.domain.valueobjects.Response;
import ldes.client.requestexecutor.exceptions.HttpRequestException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import com.github.scribejava.core.model.OAuth2AccessToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientCredentialsRequestExecutorTest {

	@InjectMocks
	private ClientCredentialsRequestExecutor clientCredentialsRequestExecutor;

	@Mock
	private OAuth20ServiceTokenCacheWrapper oAuthService;

	@Nested
	class Apply {
		@Test
		void shouldReturnFilledResponse_whenSuccess() throws Exception {
			OAuth2AccessToken token = new OAuth2AccessToken("accessToken", "tokenType",
					3600, "refreshToken", "scope", "rawResponse");
			when(oAuthService.getAccessTokenClientCredentialsGrant()).thenReturn(token);

			Request request = new Request("url", RequestHeaders.empty());

			com.github.scribejava.core.model.Response scribeResponse = new com.github.scribejava.core.model.Response(
					200, "OK", Map.of("key", "value"), "body");
			when(oAuthService.execute(any())).thenReturn(scribeResponse);

			Response response = clientCredentialsRequestExecutor.execute(request);

			verify(oAuthService).signRequest(any(), any());
			assertEquals(scribeResponse.getCode(), response.getHttpStatus());
			assertTrue(response.getBody().isPresent());
			assertEquals(scribeResponse.getBody(), response.getBody().get());
			assertTrue(response.getValueOfHeader("key").isPresent());
			assertEquals(scribeResponse.getHeader("key"), response.getValueOfHeader("key").get());
		}

		@Test
		void shouldThrowHtppException_whenIOException() throws Exception {
			Request request = new Request("url", RequestHeaders.empty());
			when(oAuthService.execute(any())).thenThrow(IOException.class);
			assertThrows(HttpRequestException.class, () -> clientCredentialsRequestExecutor.execute(request));

		}

		@Test
		void shouldThrowHtppException_whenInterrupted() throws Exception {
			Request request = new Request("url", RequestHeaders.empty());
			when(oAuthService.execute(any())).thenThrow(InterruptedException.class);

			assertThrows(HttpRequestException.class, () -> clientCredentialsRequestExecutor.execute(request));
		}

		@Test
		void shouldNotThrowHtppException_whenInterrupted() throws Exception {
			Request request = new Request("url", RequestHeaders.empty());
			when(oAuthService.execute(any())).thenThrow(NullPointerException.class);

			assertThrows(NullPointerException.class, () -> clientCredentialsRequestExecutor.execute(request));
		}

	}
}
