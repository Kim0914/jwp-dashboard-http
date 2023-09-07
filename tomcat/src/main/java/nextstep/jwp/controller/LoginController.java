package nextstep.jwp.controller;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import nextstep.jwp.db.InMemoryUserRepository;
import nextstep.jwp.model.User;
import org.apache.handler.AbstractController;
import org.apache.response.ContentType;
import org.apache.common.Cookie;
import org.apache.common.FileReader;
import org.apache.response.HttpStatus;
import org.apache.common.Session;
import org.apache.common.SessionManager;
import org.apache.request.HttpRequest;
import org.apache.response.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginController extends AbstractController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);
    private static final String JSESSIONID = "JSESSIONID=";
    private static final String INDEX_PAGE = "/index.html";
    private static final String LOGIN_PAGE = "/login.html";
    private static final String UNAUTHORIZED_PAGE = "/401.html";

    @Override
    protected void doPost(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        String query = httpRequest.getBody();
        String[] queries = query.split("&");
        String[] accountQuery = queries[0].split("=");
        String[] passwordQuery = queries[1].split("=");

        if (isInvalidParameter(accountQuery[0], passwordQuery[0])) {
            String content = FileReader.read(LOGIN_PAGE);
            httpResponse.setHttpStatus(HttpStatus.FOUND);
            httpResponse.setContentType(ContentType.TEXT_HTML);
            httpResponse.setBody(content);
            httpResponse.setLocation(LOGIN_PAGE);
            return;
        }

        String account = accountQuery[1];
        String password = passwordQuery[1];

        Optional<User> user = InMemoryUserRepository.findByAccount(account);

        if (user.isPresent() && user.get().checkPassword(password)) {
            LOG.info("user : {}", user);
            String content = FileReader.read(INDEX_PAGE);

            httpResponse.setHttpStatus(HttpStatus.FOUND);
            httpResponse.setContentType(ContentType.TEXT_HTML);
            httpResponse.setBody(content);

            Session session = createSession(httpResponse, user.get());
            httpResponse.setCookie(JSESSIONID + session.getId());
            httpResponse.setLocation(INDEX_PAGE);
            return;
        }

        String content = FileReader.read(UNAUTHORIZED_PAGE);
        httpResponse.setHttpStatus(HttpStatus.UNAUTHORIZED);
        httpResponse.setContentType(ContentType.TEXT_HTML);
        httpResponse.setBody(content);
        httpResponse.setLocation(UNAUTHORIZED_PAGE);
    }

    private boolean isInvalidParameter(String account, String password) {
        return !Objects.equals(account, "account") || !Objects.equals(password, "password");
    }

    @Override
    protected void doGet(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        Cookie cookie = httpRequest.getCookie();
        String sessionId = cookie.getValue("JSESSIONID");
        Session session = SessionManager.findSession(sessionId);

        if (isAuthenticatedUser(session)) {
            String content = FileReader.read(INDEX_PAGE);
            httpResponse.setHttpStatus(HttpStatus.FOUND);
            httpResponse.setContentType(ContentType.TEXT_HTML);
            httpResponse.setBody(content);
            httpResponse.setLocation(INDEX_PAGE);
            return;
        }

        String content = FileReader.read(LOGIN_PAGE);
        httpResponse.setHttpStatus(HttpStatus.OK);
        httpResponse.setContentType(ContentType.TEXT_HTML);
        httpResponse.setBody(content);
    }

    private boolean isAuthenticatedUser(Session session) {
        return session != null && session.getAttribute("user") != null;
    }

    private Session createSession(HttpResponse httpResponse, User user) {
        Session session = new Session(UUID.randomUUID().toString());
        httpResponse.setCookie(JSESSIONID + session.getId());
        session.setAttribute("user", user);
        SessionManager.add(session);
        return session;
    }
}