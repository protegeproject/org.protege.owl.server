package org.protege.editor.owl.server.http.handlers;

import static org.protege.editor.owl.server.http.ServerProperties.CODEGEN_DELIMETER;
import static org.protege.editor.owl.server.http.ServerProperties.CODEGEN_FILE;
import static org.protege.editor.owl.server.http.ServerProperties.CODEGEN_PREFIX;
import static org.protege.editor.owl.server.http.ServerProperties.CODEGEN_SUFFIX;
import static org.protege.editor.owl.server.http.ServerProperties.EVS_HISTORY_FILE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.protege.editor.owl.server.http.ServerEndpoints;
import org.protege.editor.owl.server.http.exception.ServerException;
import org.protege.editor.owl.server.http.messages.EVSHistory;
import org.protege.editor.owl.server.security.LoginTimeoutException;
import org.protege.editor.owl.server.security.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

public class CodeGenHandler extends BaseRoutingHandler {

	private static Logger logger = LoggerFactory.getLogger(CodeGenHandler.class);

	private final ServerConfiguration serverConfiguration;
	private final SessionManager sessionManager;

	public CodeGenHandler(@Nonnull ServerConfiguration serverConfiguration, @Nonnull SessionManager sessionManager) {
		this.serverConfiguration = serverConfiguration;
		this.sessionManager = sessionManager;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) {
		try {
			String tokenKey = getTokenKey(exchange);
			AuthToken authToken = sessionManager.getAuthToken(tokenKey);
			if (sessionManager.validate(authToken, getTokenOwner(exchange))) {
				handlingRequest(authToken, exchange);
			}
			else {
				exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
				exchange.getResponseHeaders().add(new HttpString("Error-Message"), "Access denied");
			}
		}
		catch (IOException | ClassNotFoundException e) {
			internalServerErrorStatusCode(exchange, "Server failed to receive the sent data", e);
		}
		catch (LoginTimeoutException e) {
			loginTimeoutErrorStatusCode(exchange, e);
		}
		catch (ServerException e) {
			handleServerException(exchange, e);
		}
		finally {
			exchange.endExchange(); // end request
		}
	}

	private void handlingRequest(AuthToken authToken, HttpServerExchange exchange)
			throws IOException, ClassNotFoundException, ServerException {
		String requestPath = exchange.getRequestPath();
		if (requestPath.equals(ServerEndpoints.GEN_CODE)) {
			int cnt = readCountParameter(exchange);
			String p = serverConfiguration.getProperty(CODEGEN_PREFIX);
			String s = serverConfiguration.getProperty(CODEGEN_SUFFIX);
			String d = serverConfiguration.getProperty(CODEGEN_DELIMETER);
			String cfn = serverConfiguration.getProperty(CODEGEN_FILE);
			int seq = 0;
			try {
				File codeGenFile = new File(cfn);
				FileReader fileReader = new FileReader(codeGenFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				seq = Integer.parseInt(bufferedReader.readLine().trim());
				
				List<String> codes = new ArrayList<String>();
				String sseq = "0";
				for (int j = 0; j < cnt; j++) {
					sseq = (new Integer(seq++)).toString();
					String code = p + d + sseq;
					if (s != null) {
						code = code + d + s;
					}
					codes.add(code);
				}
				ObjectOutputStream os = new ObjectOutputStream(exchange.getOutputStream());
				os.writeObject(codes);
				
				try {
					fileReader.close();
				}
				catch (IOException e) {
					// Ignore the exception but report it into the log
					logger.warn("Unable to close the file reader stream used to read the code generator configuration", e);
				}
				flushCode(codeGenFile, seq);
			}
			catch (IOException e) {
				internalServerErrorStatusCode(exchange, "Server failed to read code generator configuration", e);
			}
		}
		else if (requestPath.equals(ServerEndpoints.GEN_CODES)) { 
			// NO-OP
		}
		else if (requestPath.equals(ServerEndpoints.EVS_REC)) {
			ObjectInputStream ois = new ObjectInputStream(exchange.getInputStream());
			EVSHistory hist = (EVSHistory) ois.readObject();
			recordEvsHistory(hist);
		}
	}

	private int readCountParameter(HttpServerExchange exchange) {
		int cnt = 1;
		String scnt = "";
		try {
			scnt = getQueryParameter(exchange, "count");
			cnt = Integer.parseInt(scnt);
		}
		catch (ServerException e) {
			// Ignore the exception but report it into the log
			logger.warn(e.getLocalizedMessage());
			logger.warn("... Using default value (count = " + cnt + ")");
		}
		catch (NumberFormatException e) {
			// Ignore the exception but report it into the log
			logger.warn("Unable to convert to number (count = " + scnt + ")");
			logger.warn("... Using default value (count = " + cnt + ")");
		}
		return cnt;
	}

	private void flushCode(File codeGenFile, int seq) throws ServerException {
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(codeGenFile)));
			pw.println(seq + 1);
			pw.close();
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to generate code", e);
		}
	}

	private void recordEvsHistory(EVSHistory hist) throws ServerException {
		try {
			String hisfile = serverConfiguration.getProperty(EVS_HISTORY_FILE);
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(hisfile, true)));
			pw.println(hist.toRecord());
			pw.close();
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to record EVS history", e);
		}
	}
}
