package ldes.client.requestexecutor.domain.valueobjects;

import org.apache.http.Header;

import java.util.List;
import java.util.Optional;

public class Response {

	private final int httpStatus;
	private final List<Header> headers;
	private final String body;

	public Response(List<Header> headers, int httpStatus, String body) {
		this.httpStatus = httpStatus;
		this.headers = headers;
		this.body = body;
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	public boolean hasStatus(int status) {
		return getHttpStatus() == status;
	}

	public Optional<String> getBody() {
		return Optional.ofNullable(body);
	}

	public Optional<String> getValueOfHeader(final String key) {
		return headers.stream().filter(header -> header.getName().equals(key)).map(Header::getValue).findFirst();
	}

}
