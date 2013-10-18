package com.braintreegateway.integrationtest

import _root_.org.junit.runner.RunWith
import _root_.org.scalatest.junit.JUnitRunner
import _root_.org.scalatest.matchers.MustMatchers
import com.braintreegateway._
import com.braintreegateway.SandboxValues.CreditCardNumber
import com.braintreegateway.SandboxValues.TransactionAmount
import gw.{Success, Failure}
import testhelpers.{GatewaySpec, MerchantAccountTestConstants, TestHelper}
import java.math.BigDecimal

import MerchantAccountTestConstants._

@RunWith(classOf[JUnitRunner])
class TransparentRedirectSpec extends GatewaySpec with MustMatchers {

  describe("creating transaction") {
    onGatewayIt("basically works") {
      gateway =>
        val request = new TransactionRequest().amount(TransactionAmount.AUTHORIZE.amount).creditCard.number(CreditCardNumber.VISA.number).expirationDate("05/2009").done.options.storeInVault(true).done
        val trParams = new TransactionRequest().`type`(Transaction.Type.SALE)
        val queryString = TestHelper.simulateFormPostForTR(gateway, trParams, request, gateway.transparentRedirect.url)
        val result = gateway.transparentRedirect.confirmTransaction(queryString)
        result match {
          case Success(transaction) => {
            transaction.getCreditCard.getBin must be === CreditCardNumber.VISA.number.substring(0, 6)
            transaction.getAmount must be === TransactionAmount.AUTHORIZE.amount
          }
          case _ => fail("expected success")
        }
    }

    onGatewayIt("can specify merchant id") {
      gateway =>
        val request = new TransactionRequest().amount(TransactionAmount.AUTHORIZE.amount).creditCard.number(CreditCardNumber.VISA.number).expirationDate("05/2009").done
        val trParams = new TransactionRequest().`type`(Transaction.Type.SALE).merchantAccountId(NON_DEFAULT_MERCHANT_ACCOUNT_ID)
        val queryString = TestHelper.simulateFormPostForTR(gateway, trParams, request, gateway.transparentRedirect.url)
        val result = gateway.transparentRedirect.confirmTransaction(queryString)
        result match {
          case Success(transaction) => {
            transaction.getMerchantAccountId must be === NON_DEFAULT_MERCHANT_ACCOUNT_ID
          }
          case _ => fail("expected success")
        }
    }

    onGatewayIt("can specify descriptor") {
      gateway =>
        val request = new TransactionRequest().amount(TransactionAmount.AUTHORIZE.amount).
          creditCard.number(CreditCardNumber.VISA.number).expirationDate("05/2009").done
        val trParams = new TransactionRequest().`type`(Transaction.Type.SALE).
          descriptor.name("123*123456789012345678").phone("3334445555").done
        val queryString = TestHelper.simulateFormPostForTR(gateway, trParams, request, gateway.transparentRedirect.url)
        val result = gateway.transparentRedirect.confirmTransaction(queryString)
        result match {
          case Success(transaction) => {
            transaction.getDescriptor must be === Descriptor(name="123*123456789012345678", phone="3334445555")
          }
          case _ => fail("expected success")
        }
    }

    onGatewayIt("can specify level 2 attribtues") {
      gateway =>
        val request = new TransactionRequest().amount(TransactionAmount.AUTHORIZE.amount).
          creditCard.number(CreditCardNumber.VISA.number).expirationDate("05/2009").done
        val trParams = new TransactionRequest().`type`(Transaction.Type.SALE).
          taxAmount(new BigDecimal("10.00")).taxExempt(true).purchaseOrderNumber("12345")
        val queryString = TestHelper.simulateFormPostForTR(gateway, trParams, request, gateway.transparentRedirect.url)
        val result = gateway.transparentRedirect.confirmTransaction(queryString)
        result match {
          case Success(transaction) => {
            transaction.getTaxAmount must be === new BigDecimal("10.00")
            transaction.isTaxExempt must be === true
            transaction.getPurchaseOrderNumber must be === "12345"
          }
          case _ => fail("expected success")
        }
    }
  }

  describe("customer operations") {
    onGatewayIt("creates via TR") {
      gateway =>
        val request = new CustomerRequest().firstName("John")
        val trParams = new CustomerRequest().lastName("Doe")
        val queryString = TestHelper.simulateFormPostForTR(gateway, trParams, request, gateway.transparentRedirect.url)
        val result = gateway.transparentRedirect.confirmCustomer(queryString)
        result match {
          case Success(customer) => {
            customer.getFirstName must be === "John"
            customer.getLastName must be === "Doe"
          }
          case _ => fail("expected success")
        }
    }

    onGatewayIt("updates via TR") {
      gateway =>
        val request = new CustomerRequest().firstName("John").lastName("Doe")
        val updateRequest = new CustomerRequest().firstName("Jane")

        val result = for {
          customer <- gateway.customer.create(request)
          trParams = new CustomerRequest().customerId(customer.getId).lastName("Dough")
          queryString = TestHelper.simulateFormPostForTR(gateway, trParams, updateRequest, gateway.transparentRedirect.url)
          confirm <- gateway.transparentRedirect.confirmCustomer(queryString)
        } yield (customer, confirm)

        result match {
          case Success((customer, confirm)) => {
            val updatedCustomer = gateway.customer.find(customer.getId)
            updatedCustomer.getFirstName must be === "Jane"
            updatedCustomer.getLastName must be === "Dough"
          }
          case _ => fail("expected success")
        }
    }
  }

  describe("credit card operations") {
    onGatewayIt("can create") {
      gateway =>
        val result = for {
          customer <- gateway.customer.create(new CustomerRequest)
          request = new CreditCardRequest
          trParams = new CreditCardRequest().customerId(customer.getId).number("4111111111111111").expirationDate("10/10")
          queryString = TestHelper.simulateFormPostForTR(gateway, trParams, request, gateway.transparentRedirect.url)
          confirm <- gateway.transparentRedirect.confirmCreditCard(queryString)
        } yield confirm

        result match {
          case Success(confirm) => {
            confirm.getBin must be === "411111"
            confirm.getLast4 must be === "1111"
            confirm.getExpirationDate must be === "10/2010"
          }
          case _ => fail("expected success")
        }
    }

    onGatewayIt("can update") {
      gateway =>
        val result = for {
          customer <- gateway.customer.create(new CustomerRequest)
          request = new CreditCardRequest().customerId(customer.getId).number("5105105105105100").expirationDate("05/12")
          card <- gateway.creditCard.create(request)
          updateRequest = new CreditCardRequest
          trParams = new CreditCardRequest().paymentMethodToken(card.getToken).number("4111111111111111").expirationDate("10/10")
          queryString = TestHelper.simulateFormPostForTR(gateway, trParams, updateRequest, gateway.transparentRedirect.url)
          confirm <- gateway.transparentRedirect.confirmCreditCard(queryString)
        } yield (card, confirm)

        result match {
          case Success((card, confirm)) => {
            val updatedCreditCard = gateway.creditCard.find(card.getToken)
            updatedCreditCard.getBin must be === "411111"
            updatedCreditCard.getLast4 must be === "1111"
            updatedCreditCard.getExpirationDate must be === "10/2010"
          }
          case _ => fail("expected success")
        }
    }
  }

  describe("error handling") {
    onGatewayIt("throws exception when confirming incorrect resource") {
      gateway =>
        val request = new CustomerRequest().firstName("John")
        val trParams = new CustomerRequest().lastName("Doe")
        val queryString = TestHelper.simulateFormPostForTR(gateway, trParams, request, gateway.transparentRedirect.url)
        intercept[Exception] {
          gateway.transparentRedirect.confirmTransaction(queryString)
        }.getMessage must be === "You attemped to confirm a transaction, but received a customer."
    }

    onGatewayIt("doesn't raise error when receiving API error response") {
      gateway =>
        val invalidRequest = new TransactionRequest
        val trParams = new TransactionRequest().`type`(Transaction.Type.SALE)
        val queryString = TestHelper.simulateFormPostForTR(gateway, trParams, invalidRequest, gateway.transparentRedirect.url)
        val result = gateway.transparentRedirect.confirmTransaction(queryString)
        result match {
          case Failure(errors, _, _, _, _, _) => {
            errors.deepSize must be > 0
          }
          case _ => fail("expected Failure")
        }
    }
  }
}