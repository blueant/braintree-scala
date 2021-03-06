package net.bhardy.braintree.scala


class UpdateModificationRequest(parent: ModificationsRequest, existingId: String) extends ModificationRequest(parent) {

  protected override def buildRequest(root: String): RequestBuilder = {
    super.buildRequest(root).addElement("existingId", existingId)
  }
}