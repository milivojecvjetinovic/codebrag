package com.softwaremill.codebrag.rest

import com.softwaremill.codebrag.AuthenticatableServletSpec
import org.scalatest.BeforeAndAfterEach
import com.softwaremill.codebrag.service.invitations.InvitationService
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.bson.types.ObjectId
import com.softwaremill.codebrag.service.user.UserJsonBuilder._
import com.softwaremill.codebrag.service.user.Authenticator
import org.scalatra.auth.Scentry
import com.softwaremill.codebrag.service.data.UserJson
import org.mockito.ArgumentCaptor


class InvitationServletSpec extends AuthenticatableServletSpec with BeforeAndAfterEach {

  var invitationService: InvitationService = _

  override def beforeEach {
    super.beforeEach()
    invitationService = mock[InvitationService]
    addServlet(new TestableInvitationServlet(fakeAuthenticator, fakeScentry, invitationService), "/*")
  }

  "GET /" should "return invitation message" in {
    //given
    userIsAuthenticatedAs(someUser())
    val invitationCode = "123abc"
    when(invitationService.generateInvitationCode(any[ObjectId])).thenReturn(invitationCode)
    //when
    get("/") {
      //then
      status should be(200)
      body should be("{\"invitationCode\":\""+invitationCode+"\"}")
    }
  }

  "POST /" should "send invitation" in {
    //given
    userIsAuthenticatedAs(someUser())

    val email = "adam@example.org"
    val invitationLink = "http://codebrag.com/#/register/123abc123"

    //when
    val json = s"""{"invitationLink": "${invitationLink}", "emails": ["${email}"]}"""
    post("/", json, defaultJsonHeaders) {
      //then
      status should be(200)
      val emailCaptor = ArgumentCaptor.forClass(classOf[List[String]])
      val invitationCaptor = ArgumentCaptor.forClass(classOf[String])
      val objectId = ArgumentCaptor.forClass(classOf[ObjectId])
      verify(invitationService).sendInvitation(emailCaptor.capture(),invitationCaptor.capture(),objectId.capture())
      emailCaptor.getValue should be(List(email))
      invitationCaptor.getValue should be (invitationLink)
    }
  }


}

class TestableInvitationServlet(fakeAuthenticator: Authenticator, fakeScentry: Scentry[UserJson], invitationService: InvitationService)
  extends InvitationServlet(fakeAuthenticator, invitationService) {
  override def scentry(implicit request: javax.servlet.http.HttpServletRequest) = fakeScentry
}

