package com.braintreegateway

import _root_.org.junit.runner.RunWith
import _root_.org.scalatest.FunSpec
import _root_.org.scalatest.junit.JUnitRunner
import _root_.org.scalatest.matchers.MustMatchers

@RunWith(classOf[JUnitRunner])
class RequestBuilderSpec extends FunSpec with MustMatchers {

  class OpenTestBuilder extends RequestBuilder("open") {
    def publicBuildXmlElement(name: String, value: AnyRef): String = {
      RequestBuilder.buildXmlElementString(name, value)
    }

    def formatMap(name: String, map: Map[String, AnyRef]): String = {
      RequestBuilder.formatAsXML(name, map)
    }
  }

  describe("toXml") {
    it("produces xml from builder") {
      val builder = new RequestBuilder("myparent")
      builder.addElement("name", "value")
      val result = builder.toXmlString
      result must be === "<myparent><name>value</name></myparent>"
    }
  }

  describe("publicBuildXmlElement") {
    it("converts lists to XML") {
      val builder = new OpenTestBuilder
      val items = List("Chicken", "Rabbit")
      val element: String = builder.publicBuildXmlElement("animals", items)
      element must be === "<animals type=\"array\"><item>Chicken</item><item>Rabbit</item></animals>"
    }
  }

  describe("formatMap") {
    it("converts maps to XML") {
      val builder = new OpenTestBuilder
      val map = Map("color" -> "green", "insect" -> "bee")
      val element = builder.formatMap("examples", map)
      element must be === "<examples><color>green</color><insect>bee</insect></examples>"
    }
  }
}
