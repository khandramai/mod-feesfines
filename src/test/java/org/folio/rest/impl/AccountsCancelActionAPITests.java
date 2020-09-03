package org.folio.rest.impl;

import static io.restassured.http.ContentType.JSON;
import static org.folio.rest.utils.ResourceClients.accountsCancelClient;
import static org.folio.test.support.EntityBuilder.createAccount;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CancelActionRequest;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.junit.Before;
import org.junit.Test;

public class AccountsCancelActionAPITests extends ApiTests {

  private static final String ACCOUNTS_TABLE = "accounts";
  private ResourceClient  accountCancelClient;

  @Before
  public void setUp() {
    removeAllFromTable(ACCOUNTS_TABLE);
  }

  @Test
  public void cancelActionShouldCancelAccount() {
    Account accountToPost = postAccount();
    CancelActionRequest cancelActionRequest = createCancelActionRequest();
    String accountId = accountToPost.getId();

    accountCancelClient = accountsCancelClient(accountToPost.getId());
    accountCancelClient.attemptCreate(cancelActionRequest)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .body("accountId", is(accountId));

    accountsClient.getById(accountId)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("status.name", is("Closed"))
      .body("paymentStatus.name", is("Cancelled as error"))
      .body("remaining", is(0.0f));
  }

  @Test
  public void shouldReturn404WhenAccountDoesNotExist() {
    CancelActionRequest cancelActionRequest = createCancelActionRequest();

    accountCancelClient = accountsCancelClient(randomId());
    accountCancelClient.attemptCreate(cancelActionRequest)
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .body(equalTo("Fee/fine was not found"));
  }

  @Test
  public void shouldReturn422WhenAccountIsClosed() {
    Account account = createAccount();
    account.getStatus().setName(FeeFineStatus.CLOSED.getValue());
    postAccount(account);

    accountCancelClient = accountsCancelClient(account.getId());
    CancelActionRequest cancelActionRequest = createCancelActionRequest();

    accountCancelClient.attemptCreate(cancelActionRequest)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("errorMessage", is("Account is already closed"));
  }

  private CancelActionRequest createCancelActionRequest() {
    return new CancelActionRequest()
      .withComments("Comment")
      .withTransactionInfo("transactionInfo")
      .withNotifyPatron(false)
      .withServicePointId("7c5abc9f-f3d7-4856-b8d7-6712462ca007")
      .withUserName("Test User")
      .withPaymentMethod("Cash");
  }

  private Account postAccount() {
    Account accountToPost = createAccount();
    postAccount(accountToPost);
    return accountToPost;
  }

  private void postAccount(Account account) {
    accountsClient.create(account)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);
  }
}
