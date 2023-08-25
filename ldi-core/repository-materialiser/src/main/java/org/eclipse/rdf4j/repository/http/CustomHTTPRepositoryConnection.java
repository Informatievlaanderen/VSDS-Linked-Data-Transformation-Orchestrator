package org.eclipse.rdf4j.repository.http;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.transaction.TransactionSetting;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.transaction.operations.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.repository.http.helpers.HTTPRepositorySettings;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import static org.eclipse.rdf4j.rio.RDFFormat.NTRIPLES;

/**
 * This is a copy of the HTTPRepositoryConnection
 * The only change is that we use RdfFormat.NQUADS instead of Binary.
 * This is because we use the assembly plugin to create fat jars. Rdf4j uses SPI
 * and different RDFParserFactories are provided in the META-INF.
 * However, these are overwritten by the assembly plugin.
 * We cannot use the Binary format as this is not supported by Jena, only by
 * Rdf4j.
 */
@SuppressWarnings({"java:S2177", "java:S1161", "java:S1874", "java:S6201", "java:S1155", "java:S112", "java:S1185",
		"java:S1192", "java:S6208", "java:S131", "java:S3776", "java:S125", "java:S3878", "java:S1905"})
public class CustomHTTPRepositoryConnection extends HTTPRepositoryConnection {

	public CustomHTTPRepositoryConnection(Repository repository) {
		this((HTTPRepository) repository, ((HTTPRepository) repository).createHTTPClient());
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	private final List<TransactionOperation> txn = Collections.synchronizedList(new ArrayList<>());

	private final RDF4JProtocolSession client;

	private boolean active;

	private Model toAdd;

	private Model toRemove;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public CustomHTTPRepositoryConnection(HTTPRepository repository, RDF4JProtocolSession client) {
		super(repository, client);

		this.client = client;

		// parser used for locally processing input data to be sent to the server
		// should be strict, and should preserve bnode ids.
		setParserConfig(new ParserConfig());
		getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
		getParserConfig().set(HTTPRepositorySettings.MAX_STATEMENT_BUFFER_SIZE,
				HTTPRepositorySettings.MAX_STATEMENT_BUFFER_SIZE.getDefaultValue());
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public HttpClient getHttpClient() {
		return client.getHttpClient();
	}

	@Override
	public void setHttpClient(HttpClient httpClient) {
		client.setHttpClient(httpClient);
	}

	@Override
	public void setParserConfig(ParserConfig parserConfig) {
		super.setParserConfig(parserConfig);
	}

	@Override
	public HTTPRepository getRepository() {
		return (HTTPRepository) super.getRepository();
	}

	@Override
	public void begin() throws RepositoryException {
		verifyIsOpen();
		verifyNotTxnActive("Connection already has an active transaction");

		if (this.getRepository().useCompatibleMode()) {
			active = true;
			return;
		}

		try {
			client.beginTransaction(this.getIsolationLevel());
			active = true;
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException | IllegalStateException | IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void begin(TransactionSetting... settings) {
		verifyIsOpen();
		verifyNotTxnActive("Connection already has an active transaction");

		if (this.getRepository().useCompatibleMode()) {
			active = true;
			return;
		}

		try {
			client.beginTransaction(settings);
			active = true;
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException | IllegalStateException | IOException e) {
			throw new RepositoryException(e);
		}

	}

	/**
	 * Prepares a {@Link Query} for evaluation on this repository. Note that the
	 * preferred way of preparing queries is
	 * to use the more specific
	 * {@link #prepareTupleQuery(QueryLanguage, String, String)},
	 * {@link #prepareBooleanQuery(QueryLanguage, String, String)}, or
	 * {@link #prepareGraphQuery(QueryLanguage, String, String)} methods instead.
	 *
	 * @throws UnsupportedOperationException
	 *             if the method is not supported for the supplied query language.
	 */
	@Override
	public Query prepareQuery(QueryLanguage ql, String queryString, String baseURI) {
		if (QueryLanguage.SPARQL.equals(ql)) {
			String strippedQuery = QueryParserUtil.removeSPARQLQueryProlog(queryString).toUpperCase();
			if (strippedQuery.startsWith("SELECT")) {
				return prepareTupleQuery(ql, queryString, baseURI);
			} else if (strippedQuery.startsWith("ASK")) {
				return prepareBooleanQuery(ql, queryString, baseURI);
			} else {
				return prepareGraphQuery(ql, queryString, baseURI);
			}
		} else {
			throw new UnsupportedOperationException("Operation not supported for query language " + ql);
		}
	}

	@Override
	public TupleQuery prepareTupleQuery(QueryLanguage ql, String queryString, String baseURI) {
		return new HTTPTupleQuery(this, ql, queryString, baseURI);
	}

	@Override
	public GraphQuery prepareGraphQuery(QueryLanguage ql, String queryString, String baseURI) {
		return new HTTPGraphQuery(this, ql, queryString, baseURI);
	}

	@Override
	public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String queryString, String baseURI) {
		return new HTTPBooleanQuery(this, ql, queryString, baseURI);
	}

	@Override
	public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
		try {
			List<Resource> contextList = new ArrayList<>();

			try (TupleQueryResult contextIDs = client.getContextIDs()) {
				while (contextIDs.hasNext()) {
					BindingSet bindingSet = contextIDs.next();
					Value context = bindingSet.getValue("contextID");

					if (context instanceof Resource) {
						contextList.add((Resource) context);
					}
				}
			}

			return createRepositoryResult(contextList);
		} catch (QueryEvaluationException | IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		try {
			StatementCollector collector = new StatementCollector();
			exportStatements(subj, pred, obj, includeInferred, collector, contexts);
			return createRepositoryResult(collector.getStatements());
		} catch (RDFHandlerException e) {
			// found a bug in StatementCollector?
			throw new RuntimeException(e);
		}
	}

	@Override
	public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, RDFHandler handler,
			Resource... contexts) throws RDFHandlerException, RepositoryException {
		flushTransactionState(Protocol.Action.GET);
		try {
			client.getStatements(subj, pred, obj, includeInferred, handler, contexts);
		} catch (IOException | QueryInterruptedException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public long size(Resource... contexts) throws RepositoryException {
		flushTransactionState(Protocol.Action.SIZE);
		try {
			return client.size(contexts);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void prepare() throws RepositoryException {
		if (this.getRepository().getServerProtocolVersion() < 12) {
			// Action.PREPARE is not supported in Servers using protocols older than version
			// 12.
			logger.warn("Prepare operation not supported by server (requires protocol version 12)");
			return;
		}

		flushTransactionState(Protocol.Action.PREPARE);
		try {
			client.prepareTransaction();
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException | IllegalStateException | IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void commit() throws RepositoryException {

		if (this.getRepository().useCompatibleMode()) {
			synchronized (txn) {
				if (txn.size() > 0) {
					try {
						client.sendTransaction(txn);
						txn.clear();
					} catch (IOException e) {
						throw new RepositoryException(e);
					}
				}
				active = false;
			}
			return;
		}

		flushTransactionState(Protocol.Action.COMMIT);
		try {
			client.commitTransaction();
			active = false;
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException | IllegalStateException | IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void rollback() throws RepositoryException {
		if (this.getRepository().useCompatibleMode()) {
			txn.clear();
			active = false;
			return;
		}

		flushTransactionState(Protocol.Action.ROLLBACK);
		try {
			client.rollbackTransaction();
			active = false;
		} catch (RepositoryException e) {
			throw e;
		} catch (RDF4JException | IllegalStateException | IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void close() throws RepositoryException {
		try {
			if (isActive()) {
				logger.warn("Rolling back transaction due to connection close", new Throwable());
				rollback();
			}
		} finally {
			super.close();
			client.close();
		}
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (baseURI == null) {
			// default baseURI to file
			baseURI = file.toURI().toString();
		}
		if (dataFormat == null) {
			dataFormat = Rio.getParserFormatForFileName(file.getName())
					.orElseThrow(Rio.unsupportedFormat(file.getName()));
		}

		try (InputStream in = new FileInputStream(file)) {
			add(in, baseURI, dataFormat, contexts);
		}
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (baseURI == null) {
			baseURI = url.toExternalForm();
		}

		URLConnection con = url.openConnection();

		// Set appropriate Accept headers
		if (dataFormat != null) {
			for (String mimeType : dataFormat.getMIMETypes()) {
				con.addRequestProperty("Accept", mimeType);
			}
		} else {
			Set<RDFFormat> rdfFormats = RDFParserRegistry.getInstance().getKeys();
			List<String> acceptParams = RDFFormat.getAcceptParams(rdfFormats, true, null);
			for (String acceptParam : acceptParams) {
				con.addRequestProperty("Accept", acceptParam);
			}
		}

		InputStream in = con.getInputStream();

		if (dataFormat == null) {
			// Try to determine the data's MIME type
			String mimeType = con.getContentType();
			int semiColonIdx = mimeType.indexOf(';');
			if (semiColonIdx >= 0) {
				mimeType = mimeType.substring(0, semiColonIdx);
			}
			dataFormat = Rio.getParserFormatForMIMEType(mimeType)
					.orElseGet(() -> Rio.getParserFormatForFileName(url.getPath())
							.orElseThrow(() -> new UnsupportedRDFormatException(
									"Could not find RDF format for URL: " + url.toExternalForm())));

		}

		try {
			add(in, baseURI, dataFormat, contexts);
		} finally {
			in.close();
		}
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		if (this.getRepository().useCompatibleMode()) {

			dataFormat = getBackwardCompatibleFormat(dataFormat);

			if (!isActive()) {
				// Send bytes directly to the server
				client.upload(in, baseURI, dataFormat, false, false, contexts);
			} else {
				// Parse files locally
				super.add(in, baseURI, dataFormat, contexts);

			}
			return;
		}

		flushTransactionState(Protocol.Action.ADD);
		// Send bytes directly to the server
		client.upload(in, baseURI, dataFormat, false, false, contexts);
	}

	private RDFFormat getBackwardCompatibleFormat(RDFFormat format) {
		// In Sesame 2.8, the default MIME-type for N-Triples changed. To stay
		// backward compatible, we 'fake' the
		// default MIME-type back to the older value (text/plain) when running in
		// compatibility mode.
		if (NTRIPLES.equals(format)) {
			// create a new format constant with identical properties as the
			// N-Triples format, just with a different
			// default MIME-type.
			return new RDFFormat(NTRIPLES.getName(), List.of("text/plain"), NTRIPLES.getCharset(),
					NTRIPLES.getFileExtensions(), NTRIPLES.supportsNamespaces(), NTRIPLES.supportsContexts(),
					NTRIPLES.supportsRDFStar());
		}

		return format;
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {

		if (this.getRepository().useCompatibleMode()) {

			dataFormat = getBackwardCompatibleFormat(dataFormat);

			if (!isActive()) {
				// Send bytes directly to the server
				client.upload(reader, baseURI, dataFormat, false, false, contexts);
			} else {
				// Parse files locally
				super.add(reader, baseURI, dataFormat, contexts);

			}
			return;
		}

		flushTransactionState(Protocol.Action.ADD);
		client.upload(reader, baseURI, dataFormat, false, false, contexts);
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		if (!isActive()) {
			// operation is not part of a transaction - just send directly
			Objects.requireNonNull(contexts,
					"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");

			final Model m = new LinkedHashModel();

			if (contexts.length == 0) {
				// if no context is specified in the method call, statement's own
				// context (if any) is used.
				m.add(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			} else {
				m.add(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
			}
			addModel(m);
		} else {
			super.add(st, contexts);
		}
	}

	@Override
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts) throws RepositoryException {
		if (!isActive()) {
			if (logger.isDebugEnabled()) {
				logger.debug("adding statement directly: {} {} {} {}", subject, predicate, object, contexts);
			}
			// operation is not part of a transaction - just send directly
			Objects.requireNonNull(contexts,
					"contexts argument may not be null; either the value should be cast to Resource or an empty array should be supplied");
			Model m = new LinkedHashModel();
			m.add(subject, predicate, object, contexts);
			addModel(m);
		} else {
			logger.debug("adding statement in txn: {} {} {} {}", new Object[] { subject, predicate, object, contexts });
			super.add(subject, predicate, object, contexts);
		}
	}

	// This is currently disabled because the implementation of removeData in
	// RDF4JProtocolSession
	// relies on an active transaction. See
	// https://github.com/eclipse/rdf4j/issues/3336
	/*
	 * @Override public void remove(Resource subject, URI predicate, Value object,
	 * Resource... contexts) throws
	 * RepositoryException { if (!isActive()) { // operation is not part of a
	 * transaction - just send directly
	 * OpenRDFUtil.verifyContextNotNull(contexts); if (subject == null) { subject =
	 * SESAME.WILDCARD; } if (predicate ==
	 * null) { predicate = SESAME.WILDCARD; } if (object == null) { object =
	 * SESAME.WILDCARD; } final Model m = new
	 * LinkedHashModel(); m.add(subject, predicate, object, contexts);
	 * removeModel(m); } else { super.remove(subject,
	 * predicate, object, contexts); } }
	 */

	@Override
	protected void addWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		if (this.getRepository().useCompatibleMode()) {
			txn.add(new AddStatementOperation(subject, predicate, object, contexts));
			return;
		}

		flushTransactionState(Protocol.Action.ADD);

		if (toAdd == null) {
			toAdd = new LinkedHashModel();
		}
		toAdd.add(subject, predicate, object, contexts);
	}

	private void addModel(Model m) throws RepositoryException {
		RDFFormat format = RDFFormat.NQUADS;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Rio.write(m, out, format);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			client.addData(in, null, format);

		} catch (RDFHandlerException e) {
			throw new RepositoryException("error while writing statement", e);
		} catch (RDFParseException | IOException e) {
			throw new RepositoryException(e);
		}
	}

	private void removeModel(Model m) throws RepositoryException {
		RDFFormat format = RDFFormat.NQUADS;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Rio.write(m, out, format);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			client.removeData(in, null, format);

		} catch (RDFHandlerException e) {
			throw new RepositoryException("error while writing statement", e);
		} catch (RDFParseException | IOException e) {
			throw new RepositoryException(e);
		}
	}

	protected void flushTransactionState(Protocol.Action action) throws RepositoryException {
		if (this.getRepository().useCompatibleMode()) {
			// no need to flush, using old-style transactions.
			return;
		}

		if (isActive()) {
			int maxBufferSize = getParserConfig().get(HTTPRepositorySettings.MAX_STATEMENT_BUFFER_SIZE);
			switch (action) {
				case ADD:
					if (toRemove != null) {
						removeModel(toRemove);
						toRemove = null;
					}
					if (toAdd != null && maxBufferSize <= toAdd.size()) {
						addModel(toAdd);
						toAdd = null;
					}
					break;
				case DELETE:
					if (toAdd != null) {
						addModel(toAdd);
						toAdd = null;
					}
					if (toRemove != null && maxBufferSize <= toRemove.size()) {
						removeModel(toRemove);
						toRemove = null;
					}
					break;
				case GET:
				case UPDATE:
				case COMMIT:
				case PREPARE:
				case QUERY:
				case SIZE:
					if (toAdd != null) {
						addModel(toAdd);
						toAdd = null;
					}
					if (toRemove != null) {
						removeModel(toRemove);
						toRemove = null;
					}
					break;
				case ROLLBACK:
					toAdd = null;
					toRemove = null;
					break;

			}
		}
	}

	@Override
	protected void removeWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		if (this.getRepository().useCompatibleMode()) {
			txn.add(new RemoveStatementsOperation(subject, predicate, object, contexts));
			return;
		}

		flushTransactionState(Protocol.Action.DELETE);

		if (toRemove == null) {
			toRemove = new LinkedHashModel();
		}

		if (subject == null) {
			subject = SESAME.WILDCARD;
		}
		if (predicate == null) {
			predicate = SESAME.WILDCARD;
		}
		if (object == null) {
			object = SESAME.WILDCARD;
		}

		if (contexts.length == 0) {
			toRemove.add(subject, predicate, object);
		} else if (contexts.length == 1) {
			toRemove.add(subject, predicate, object, contexts[0] == null ? RDF4J.NIL : contexts[0]);
		} else {
			// We shouldn't modify the array of contexts that is passed to this method, so
			// we need to mak a copy isntead
			Resource[] contextsCopy = Arrays.copyOf(contexts, contexts.length);
			for (int i = 0; i < contextsCopy.length; i++) {
				if (contextsCopy[i] == null) {
					contextsCopy[i] = RDF4J.NIL;
				}
			}
			toRemove.add(subject, predicate, object, contextsCopy);
		}

	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		boolean localTransaction = startLocalTransaction();

		if (this.getRepository().useCompatibleMode()) {
			txn.add(new ClearOperation(contexts));
		} else {
			remove(null, null, null, contexts);
		}

		conditionalCommit(localTransaction);
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}

		boolean localTransaction = startLocalTransaction();

		try {
			if (this.getRepository().useCompatibleMode()) {
				txn.add(new RemoveNamespaceOperation(prefix));
			} else {
				client.removeNamespacePrefix(prefix);
			}
			conditionalCommit(localTransaction);
		} catch (IOException e) {
			// is silently ignored. Should we throw the rollback exception or the
			// original exception (and/or should we log one of the exceptions?)
			conditionalRollback(localTransaction);
			throw new RepositoryException(e);
		}

	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		if (this.getRepository().useCompatibleMode()) {
			boolean localTransaction = startLocalTransaction();
			txn.add(new ClearNamespacesOperation());
			conditionalCommit(localTransaction);
			return;
		}

		try {
			client.clearNamespaces();
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void setNamespace(String prefix, String name) throws RepositoryException {
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}
		if (name == null) {
			throw new NullPointerException("name must not be null");
		}

		if (this.getRepository().useCompatibleMode()) {
			boolean localTransaction = startLocalTransaction();
			txn.add(new SetNamespaceOperation(prefix, name));
			conditionalCommit(localTransaction);
			return;
		}

		try {
			client.setNamespacePrefix(prefix, name);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
		try {
			List<Namespace> namespaceList = new ArrayList<>();

			try (TupleQueryResult namespaces = client.getNamespaces()) {
				while (namespaces.hasNext()) {
					BindingSet bindingSet = namespaces.next();
					Value prefix = bindingSet.getValue("prefix");
					Value namespace = bindingSet.getValue("namespace");

					if (prefix instanceof Literal && namespace instanceof Literal) {
						String prefixStr = ((Literal) prefix).getLabel();
						String namespaceStr = ((Literal) namespace).getLabel();
						namespaceList.add(new SimpleNamespace(prefixStr, namespaceStr));
					}
				}
			}

			return createRepositoryResult(namespaceList);
		} catch (QueryEvaluationException | IOException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public String getNamespace(String prefix) throws RepositoryException {
		if (prefix == null) {
			throw new NullPointerException("prefix must not be null");
		}
		try {
			return client.getNamespace(prefix);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}

	protected void scheduleUpdate(HTTPUpdate update) {
		SPARQLUpdateOperation op = new SPARQLUpdateOperation();
		op.setUpdateString(update.getQueryString());
		op.setBaseURI(update.getBaseURI());
		op.setBindings(update.getBindingsArray());
		op.setIncludeInferred(update.getIncludeInferred());
		op.setDataset(update.getDataset());
		txn.add(op);
	}

	/**
	 * Creates a RepositoryResult for the supplied element set.
	 */
	protected <E> RepositoryResult<E> createRepositoryResult(Iterable<? extends E> elements) {
		return new RepositoryResult<>(new CloseableIteratorIteration<>(elements.iterator()));
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
			throws RepositoryException, MalformedQueryException {
		return new HTTPUpdate(this, ql, update, baseURI);
	}

	/**
	 * Verifies that the connection is open, throws a {@link StoreException} if it
	 * isn't.
	 */
	protected void verifyIsOpen() throws RepositoryException {
		if (!isOpen()) {
			throw new RepositoryException("Connection has been closed");
		}
	}

	/**
	 * Verifies that the connection has an active transaction, throws a
	 * {@link StoreException} if it hasn't.
	 */
	protected void verifyTxnActive() throws RepositoryException {
		if (!isActive()) {
			throw new RepositoryException("Connection does not have an active transaction");
		}
	}

	/**
	 * Verifies that the connection does not have an active transaction, throws a
	 * {@link RepositoryException} if it has.
	 */
	protected void verifyNotTxnActive(String msg) throws RepositoryException {
		if (isActive()) {
			throw new RepositoryException(msg);
		}
	}

	@Override
	public boolean isActive() throws UnknownTransactionStateException, RepositoryException {
		return active;
	}

	/**
	 * @return
	 */
	protected RDF4JProtocolSession getSesameSession() {
		return client;
	}

}
